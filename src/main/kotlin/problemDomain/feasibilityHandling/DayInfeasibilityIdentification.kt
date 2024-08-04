/*
 * Copyright 2024 Daniel Ferring
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package problemDomain.feasibilityHandling

import problemDomain.*

// Identifies day related sources of infeasibility for a given [block]
fun identifySourcesOfInfeasibilityDay(
    solutionData: SolutionData,
    doctor: MiddleGrade,
    block: Block,
): MutableMap<Source, List<Int>> {
    val infeasibilities = mutableMapOf<Source, List<Int>>()

    if(block.items.size > 7)
        throw Exception("identifySourcesOfInfeasibility: infeasible block size reached")

    if(block.items.size == 7) {
        val (shiftsMadeInfeasible, source) = assessRowOfSevenDaysInfeasibility(solutionData, doctor.id, block)
        infeasibilities[source] = shiftsMadeInfeasible
    }

    // Checks in either direction of the block
    checkToLeft(solutionData, block, doctor, infeasibilities)
    checkToRightDay(solutionData, block, doctor, infeasibilities)

    return infeasibilities
}

private fun checkToLeft(
    solutionData: SolutionData,
    block: Block,
    doctor: MiddleGrade,
    infeasibilities: MutableMap<Source, List<Int>>
) {
    val blockSize = block.items.size
    val firstDay = block.items.min()
    val lastDay = block.items.max()
    val days = solutionData.days

    // Conditions are stored as booleans as they will be reused
    val twoToLeftInIndices = firstDay - 2 in days.indices
    // True if there exists a block to the left of [block] with a gap of one day
    val twoToLeftWorked = twoToLeftInIndices && days[firstDay-2].block.contains(doctor.id)
    val twoToRightInIndices = lastDay + 2 in days.indices
    // True if there exists a block to the right of [block] with a gap of one day
    val twoToRightWorked = twoToRightInIndices && days[lastDay+2].block.contains(doctor.id)

    // Check to left:
    // If the block of shifts is six long, overlapping shifts two days prior are infeasible
    if(twoToLeftInIndices && blockSize == 6 && days[firstDay-2].overlappingNightShifts.isNotEmpty()) {
        val source = Source.RowOfSixOverlap(block.id)
        infeasibilities[source] = days[firstDay-2].overlappingNightShifts.toList()
    }

    if(twoToLeftWorked) {
        val prevBlockID = days[firstDay-2].block[doctor.id]!!
        val prevBlock = doctor.blocksOfDays[prevBlockID]
            ?: throw Exception("addBlockInfeasibility: doctor $doctor does not have block $prevBlockID")
        val prevBlockSize = prevBlock.items.size

        /*
         * The combined length of the blocks, were the day between them to be worked, is
         * too great for any shift on that day to be feasible
         */
        if(prevBlockSize + blockSize + 1 > 7) {
            val source = when(prevBlockID < block.id) {
                true -> Source.WouldCauseRowTooLarge(Pair(prevBlockID, block.id))
                false -> Source.WouldCauseRowTooLarge(Pair(block.id, prevBlockID))
            }
            infeasibilities[source] = days[firstDay-1].getShifts()
        }

        /*
         * If there is a block two days to the right of the current one and assigning the day
         * between [block] and [prevBlock] would create a stretch of 7 days, there is insufficient
         * rest after this stretch, and shifts of that day must become infeasible
         */
        if(twoToRightWorked && prevBlockSize + blockSize + 1 == 7) {
            val nextBlockID = days[lastDay+2].block[doctor.id]!!
            val blocks = mutableListOf(prevBlockID, block.id, nextBlockID)
            blocks.sortDescending()

            val source = Source.InsufficientRestMid(Triple(blocks[0], blocks[1], blocks[2]))
            infeasibilities[source] = days[firstDay-1].getShifts()
        }

        /*
         * If block before gap of one day is 6 days long, there is insufficient rest for
         * the day prior to it to be worked.
         */
        if(prevBlockSize == 6 && prevBlock.items.min() - 1 in days.indices) {
            val source = Source.InsufficientRest(Pair(prevBlockID, block.id))
            infeasibilities[source] = days[prevBlock.items.min()-1].getShifts()
        }

        /*
         * If the [prevBlock] is 5 days long, there is insufficient rest for an
         * overlapping night shift two days before that to be worked (would cause a
         * row of 7 days)
         */
        if(prevBlockSize == 5 && prevBlock.items.min() - 2 in days.indices) {
            val source = Source.InsufficientRestOverlap(Pair(prevBlockID, block.id))
            infeasibilities[source] = days[prevBlock.items.min()-2].overlappingNightShifts.toList()
        }

        // Checks if there is insufficient rest for potential preceding stretches of seven days
        if(prevBlockSize < 6) {
            if(prevBlock.items.min() - 2 in days.indices &&
                days[prevBlock.items.min()-2].block[doctor.id] != null) {

                val twoPrevID = days[prevBlock.items.min()-2].block[doctor.id]!!
                val blockTwoPrev = doctor.blocksOfDays[twoPrevID]!!

                if(prevBlockSize + blockTwoPrev.items.size + 1 == 7) {
                    val blocks = mutableListOf(twoPrevID, prevBlockID, block.id)
                    blocks.sortDescending()

                    val source = Source.InsufficientRestMid(Triple(blocks[0], blocks[1], blocks[2]))
                    infeasibilities[source] = days[prevBlock.items.min()-1].getShifts()
                }
            }
            else if(prevBlock.items.min() - 3 in days.indices
                && days[prevBlock.items.min()-3].block[doctor.id] != null) {

                val threePrevID = days[prevBlock.items.min()-3].block[doctor.id]!!
                val blockThreePrev = doctor.blocksOfDays[threePrevID]!!

                if(prevBlockSize + blockThreePrev.items.size + 2 == 7) {
                    val blocks = mutableListOf(threePrevID, prevBlockID, block.id)
                    blocks.sortDescending()

                    val source = Source.InsufficientRestMidOverlap(Triple(blocks[0], blocks[1], blocks[2]))
                    infeasibilities[source] = days[prevBlock.items.min()-2].overlappingNightShifts.toList()
                }
            }
        }
    }

    val threeToLeftInIndices = twoToLeftInIndices && firstDay - 3 in days.indices
    val threeToLeftWorked = threeToLeftInIndices && days[firstDay-3].block.contains(doctor.id)

    if(!twoToLeftWorked && threeToLeftWorked) {
        val prevBlockID = days[firstDay-3].block[doctor.id]!!
        val prevBlock = doctor.blocksOfDays[prevBlockID]
            ?: throw Exception("addBlockInfeasibility: doctor $doctor does not have block $prevBlockID")

        /*
         * If there is a gap of two days between blocks where the size of the block, were
         * they and the gap to be combined, is greater than 7, any overlapping shifts two days
         * prior to the block being considered will be infeasible
         */
        if(prevBlock.items.size + blockSize + 2 > 7 && days[firstDay-2].overlappingNightShifts.isNotEmpty()) {
            val source = when(prevBlockID < block.id) {
                true -> Source.WouldCauseRowTooLargeOverlap(Pair(prevBlockID, block.id))
                false -> Source.WouldCauseRowTooLargeOverlap(Pair(block.id, prevBlockID))
            }
            infeasibilities[source] = days[firstDay-2].overlappingNightShifts.toList()
        }

        /*
         * Assigning an overlapping night shift would cause a stretch of 7 days for which there
         * cannot be the requisite subsequent period of rest, hence these overlapping shifts are
         * made infeasible
         */
        if(twoToRightWorked && prevBlock.items.size + blockSize + 2 == 7) {
            val nextBlockID = days[lastDay+2].block[doctor.id]!!
            val blocks = mutableListOf(prevBlockID, nextBlockID, block.id)
            blocks.sortDescending()

            val source = Source.InsufficientRestMidOverlap(Triple(blocks[0], blocks[1], blocks[2]))
            infeasibilities[source] = days[firstDay-2].overlappingNightShifts.toList()
        }

        /*
         * If the previous block is six long, there is insufficient rest for the day after
         * the block to be worked (would cause a row of 7, requiring 48 hours rest).
         * Only needs to be evaluated if the day we are considering is the first day of
         * [block]
         */
        if(prevBlock.items.size == 6) {
            val source = Source.InsufficientRest(Pair(prevBlockID, block.id))
            infeasibilities[source] = days[firstDay-2].getShifts()

            // If last day of [prevBlock] has overlapping night shifts they become infeasible
            if(days[prevBlock.items.max()].overlappingNightShifts.isNotEmpty()) {
                val overlapSource = Source.InsufficientRestOverlap(Pair(prevBlockID, block.id))
                infeasibilities[overlapSource] = days[prevBlock.items.max()].overlappingNightShifts.toList()
            }
        }
    }

    val fourToLeftWorked = threeToLeftInIndices && firstDay - 4 in days.indices
            && days[firstDay-4].block.contains(doctor.id)
    // True if there is a gap of three days followed by a block of days
    if(!twoToLeftWorked && !threeToLeftWorked && fourToLeftWorked) {
        val prevBlockID = days[firstDay-4].block[doctor.id]!!
        val prevBlock = doctor.blocksOfDays[prevBlockID]
            ?: throw Exception("checkToLeft: doctor ${doctor.id} does not have block $prevBlockID")

        /*
         * If the row is of size 5, and the following day has overlapping shifts, there
         * would be insufficient rest, were those overlapping shifts to be assigned
         */
        if(prevBlock.items.size == 5 &&
            solutionData.days[firstDay-3].overlappingNightShifts.isNotEmpty()) {
            val source = Source.InsufficientRestOverlap(Pair(prevBlockID, block.id))
            infeasibilities[source] = days[firstDay-3].overlappingNightShifts.toList()
        }
    }
}

fun checkToRightDay(
    solutionData: SolutionData,
    block: Block,
    doctor: MiddleGrade,
    infeasibilities: MutableMap<Source, List<Int>>
) {
    val blockSize = block.items.size
    val firstDay = block.items.min()
    val lastDay = block.items.max()
    val days = solutionData.days

    val twoToLeftInIndices = firstDay - 2 in days.indices
    val twoToRightInIndices = lastDay + 2 in days.indices
    // True if there exists a block to the right of [block] with a gap of one day
    val twoToRightWorked = twoToRightInIndices && days[lastDay+2].block.contains(doctor.id)

    // If block size is six, any overlapping shifts on the next day are infeasible
    if(lastDay + 1 in days.indices && blockSize == 6 &&
        days[lastDay+1].overlappingNightShifts.isNotEmpty()) {
        val source = Source.RowOfSixOverlap(block.id)
        /*
         * If shifts to the left were made infeasible by this source, we need to add to the
         * list of shifts to be made infeasible, rather than overwrite it
         */
        when(infeasibilities[source]) {
            null -> infeasibilities[source] = days[lastDay+1].overlappingNightShifts.toList()
            else -> infeasibilities[source] =
                infeasibilities[source]!! + days[lastDay+1].overlappingNightShifts.toList()
        }
    }

    if(twoToRightWorked) {
        val nextBlockID = days[lastDay+2].block[doctor.id]!!
        val nextBlock = doctor.blocksOfDays[nextBlockID]
            ?: throw Exception("addBlockInfeasibility: doctor $doctor does not have block $nextBlockID")

        /*
         * The combined length of the blocks, were the day between them to be worked, is
         * too great for any shift on that day to be feasible
         */
        if(nextBlock.items.size + blockSize + 1 > 7) {
            val source = when(nextBlockID < block.id) {
                true -> Source.WouldCauseRowTooLarge(Pair(nextBlockID, block.id))
                false -> Source.WouldCauseRowTooLarge(Pair(block.id, nextBlockID))
            }
            infeasibilities[source] = days[lastDay+1].getShifts()
        }

        /*
         * There is a block after [nextBlock] which prevents there being enough rest for a stretch
         * of 7 days were the day between [block] and [nextBlock] to be assigned; shifts of this
         * day are therefore made infeasible
         */

        if(nextBlock.items.size + blockSize + 1 == 7 && nextBlock.items.max() + 2 in days.indices
            && days[nextBlock.items.max()+2].block[doctor.id] != null) {

            val blockAfter = days[nextBlock.items.max()+2].block[doctor.id]!!
            val blocks = mutableListOf(blockAfter, nextBlockID, block.id)
            blocks.sortDescending()

            val source = Source.InsufficientRestMid(Triple(blocks[0], blocks[1], blocks[2]))
            infeasibilities[source] = days[lastDay+1].getShifts()
        }

        /*
         * If [block] is 6 days long and there is a gap of one day followed by a worked day
         * ([twoToRightIsWorked]), then shifts of the day to the left of the block must
         * be infeasible as there cannot be 48 hours of rest after the hypothetical stretch
         * of seven days
         */
        if(blockSize == 6 && firstDay - 1 in days.indices) {
            val source = Source.InsufficientRest(Pair(block.id, nextBlockID))
            infeasibilities[source] = days[firstDay-1].getShifts()
        }

        /*
         * If the block is 5 days long, and [twoToRightIsWorked] == true, overlapping
         * night shifts of the day two days before the block need to be made infeasible
         * as there is no possible 48 hours of rest after the hypothetical row of 7 days
         */
        if(blockSize == 5 && twoToLeftInIndices) {
            val source = Source.InsufficientRestOverlap(Pair(block.id, nextBlockID))
            infeasibilities[source] = days[firstDay-2].overlappingNightShifts.toList()
        }
    }

    val threeToRightInIndices = twoToRightInIndices && lastDay + 3 in days.indices
    val threeToRightWorked = threeToRightInIndices && days[lastDay+3].block.contains(doctor.id)

    if(!twoToRightWorked && threeToRightWorked) {
        val nextBlockID = days[lastDay+3].block[doctor.id]!!
        val nextBlock = doctor.blocksOfDays[nextBlockID]
            ?: throw Exception("checkToRightDay: doctor ${doctor.id} does not have block $nextBlockID")

        /*
         * The assignment of an overlapping night shift would lead to a scenario where 8 nights are
         * worked by the doctor in a row (illegal)
         */
        if(nextBlock.items.size + blockSize + 2 > 7 && days[lastDay+1].overlappingNightShifts.isNotEmpty()) {
            val source = when(nextBlockID < block.id) {
                true -> Source.WouldCauseRowTooLargeOverlap(Pair(nextBlockID, block.id))
                false -> Source.WouldCauseRowTooLargeOverlap(Pair(block.id, nextBlockID))
            }
            infeasibilities[source] = days[lastDay+1].overlappingNightShifts.toList()
        }

        /*
         * The assignment of an overlapping night shift would lead to a row of seven days
         * without the requisite subsequent rest period
         */
        if(nextBlock.items.size + blockSize + 2 == 7 && nextBlock.items.max() + 2 in days.indices
            && days[nextBlock.items.max()+2].block[doctor.id] != null) {

            val blockAfter = days[nextBlock.items.max()+2].block[doctor.id]!!
            val blocks = mutableListOf(blockAfter, nextBlockID, block.id)
            blocks.sortDescending()

            val source = Source.InsufficientRestMidOverlap(Triple(blocks[0], blocks[1], blocks[2]))
            infeasibilities[source] = days[lastDay+1].overlappingNightShifts.toList()
        }

        // Assigning the next day in this scenario would lead to a row of 7 days without the required rest
        if(blockSize == 6) {
            val source = Source.InsufficientRest(Pair(block.id, nextBlockID))
            infeasibilities[source] = days[lastDay+1].getShifts()

            // If end day of [block] has overlapping shifts, they become infeasible
            if(days[lastDay].overlappingNightShifts.isNotEmpty()) {
                val overlappingSource = Source.InsufficientRestOverlap(Pair(block.id, nextBlockID))
                infeasibilities[overlappingSource] = days[lastDay].overlappingNightShifts.toList()
            }
        }
    }

    val fourToRightWorked = threeToRightInIndices && lastDay + 4 in days.indices
            && days[lastDay+4].block.contains(doctor.id)
    if(!twoToRightWorked && !threeToRightWorked && fourToRightWorked) {
        val nextBlockID = days[lastDay+4].block[doctor.id]!!

        // Assigning an overlapping night shift would lead to insufficient rest
        if(blockSize == 5 && days[lastDay+1].overlappingNightShifts.isNotEmpty()) {
            val source = Source.InsufficientRestOverlap(Pair(block.id, nextBlockID))
            infeasibilities[source] = days[lastDay+1].overlappingNightShifts.toList()
        }
    }
}

private fun assessRowOfSevenDaysInfeasibility(
    solutionData: SolutionData,
    doctorID: Int,
    block: Block
): Pair<List<Int>, Source> {
    val shiftsMadeInfeasible = mutableListOf<Int>()

    // If the previous day is in [days.indices] all of its shifts need to be made infeasible
    val prevDay = block.items.min() - 1
    if(prevDay in solutionData.days.indices) {
        shiftsMadeInfeasible.addAll(solutionData.days[prevDay].getShifts())

        /*
         * If the day 2 days before the block is in [days.indices] any overlapping night
         * shifts need to be made infeasible
         */
        val twoBefore = prevDay - 1
        if(twoBefore in solutionData.days.indices)
            shiftsMadeInfeasible.addAll(solutionData.days[twoBefore].overlappingNightShifts)
    }

    // Shifts 48 hours after the last shift of the stretch need to be made infeasible
    val lastShift = getLastShiftOfBlock(solutionData, doctorID, block)
    shiftsMadeInfeasible.addAll(lastShift.shifts48HoursAfter)

    return Pair(shiftsMadeInfeasible, Source.RowOfSevenDays(block.id))
}

// Finds the last shift worked in a block of days so that [shifts48HoursAfter] can be used
private fun getLastShiftOfBlock(
    solutionData: SolutionData,
    doctorID: Int,
    block: Block
): Shift {
    val endDay = solutionData.days[block.items.max()]
    // It is assumed that shift IDs increase with their end time
    val lastShift = endDay.doctorsWorkingDay[doctorID]?.max()
        ?: throw Exception("getEndShiftOfBlock: Doctor $doctorID absent from [doctorsWorkingDay] of day ${endDay.id}")
    return solutionData.shifts[lastShift]
}

package problemDomain.feasibilityHandling

import problemDomain.*

// Identifies day related sources of infeasibility for a given [block]
fun identifySourcesOfInfeasibility(
    solutionData: SolutionData,
    doctor: MiddleGrade,
    block: Block,
): MutableMap<Source, List<Int>> {
    val infeasibilities = mutableMapOf<Source, List<Int>>()

    if(block.days.size > 7)
        throw Exception("identifySourcesOfInfeasibility: infeasible block size reached")

    if(block.days.size == 7) {
        val (shiftsMadeInfeasible, source) = assessRowOfSevenDaysInfeasibility(solutionData, doctor.id, block)
        infeasibilities[source] = shiftsMadeInfeasible
    }

    // Checks in either direction of the block
    checkToLeft(solutionData, block, doctor, infeasibilities)
    checkToRight(solutionData, block, doctor, infeasibilities)

    return infeasibilities
}

private fun checkToLeft(
    solutionData: SolutionData,
    block: Block,
    doctor: MiddleGrade,
    infeasibilities: MutableMap<Source, List<Int>>
) {
    val blockSize = block.days.size
    val firstDay = block.days.min()
    val lastDay = block.days.max()

    // Conditions are stored as booleans as they will be reused
    val twoToLeftInIndices = firstDay - 2 in solutionData.days.indices
    // True if there exists a block to the left of [block] with a gap of one day
    val twoToLeftWorked =
        twoToLeftInIndices && solutionData.days[firstDay-2].block.contains(doctor.id)
    val twoToRightInIndices = lastDay + 2 in solutionData.days.indices
    // True if there exists a block to the right of [block] with a gap of one day
    val twoToRightWorked =
        twoToRightInIndices && solutionData.days[lastDay+2].block.contains(doctor.id)

    // Check to left:
    // If the block of shifts is six long, overlapping shifts two days prior are infeasible
    if(twoToLeftInIndices && blockSize == 6 &&
        solutionData.days[firstDay-2].overlappingNightShifts.isNotEmpty()) {
        val source = Source.RowOfSixOverlap(block.id)
        infeasibilities[source] = solutionData.days[firstDay-2].overlappingNightShifts.toList()
    }

    if(twoToLeftWorked) {
        val prevBlockID = solutionData.days[firstDay-2].block[doctor.id]!!
        val prevBlock = doctor.blocksOfDays[prevBlockID] ?: throw Exception("addBlockInfeasibility: doctor $doctor does not have block $prevBlockID")
        val prevBlockSize = prevBlock.days.size

        /*
         * The combined length of the blocks, were the day between them to be worked, is
         * too great for any shift on that day to be feasible
         */
        if(prevBlockSize + blockSize + 1 > 7) {
            val source = when(prevBlockID < block.id) {
                true -> Source.WouldCauseRowTooLarge(Pair(prevBlockID, block.id))
                false -> Source.WouldCauseRowTooLarge(Pair(block.id, prevBlockID))
            }
            infeasibilities[source] = solutionData.days[firstDay-1].getShifts()
        }

        if(twoToRightWorked && prevBlockSize + blockSize + 1 == 7) {
            val nextBlockID = solutionData.days[lastDay+2].block[doctor.id]!!
            val blocks = mutableListOf(prevBlockID, block.id, nextBlockID)
            blocks.sortDescending()

            val source = Source.InsufficientRestMid(Triple(blocks[0], blocks[1], blocks[2]))
            infeasibilities[source] = solutionData.days[firstDay-1].getShifts()
        }

        /*
         * If block before gap of one day is 6 days long, there is insufficient rest for
         * the day prior to it to be worked.
         */
        if(prevBlockSize == 6 && prevBlock.days.min() - 1 in solutionData.days.indices) {
            val source = Source.InsufficientRest(Pair(prevBlockID, block.id))
            infeasibilities[source] = solutionData.days[prevBlock.days.min()-1].getShifts()
        }

        /*
         * If the [prevBlock] is 5 days long, there is insufficient rest for an
         * overlapping night shift two days before that to be worked (would cause a
         * row of 7 days)
         */
        if(prevBlockSize == 5 && prevBlock.days.min() - 2 in solutionData.days.indices) {
            val source = Source.InsufficientRestOverlap(Pair(prevBlockID, block.id))
            infeasibilities[source] = solutionData.days[prevBlock.days.min()-2].overlappingNightShifts.toList()
        }

        if(prevBlockSize < 6){
            if(prevBlock.days.min() - 2 in solutionData.days.indices
                && solutionData.days[prevBlock.days.min()-2].block[doctor.id] != null) {
                val twoPrevID = solutionData.days[prevBlock.days.min()-2].block[doctor.id]!!
                val blockTwoPrev = doctor.blocksOfDays[twoPrevID]!!

                if(prevBlockSize + blockTwoPrev.days.size + 1 == 7) {
                    val blocks = mutableListOf(twoPrevID, prevBlockID, block.id)
                    blocks.sortDescending()

                    val source = Source.InsufficientRestMid(Triple(blocks[0], blocks[1], blocks[2]))
                    infeasibilities[source] = solutionData.days[prevBlock.days.min()-1].getShifts()
                }
            }
            else if(prevBlock.days.min() - 3 in solutionData.days.indices
                && solutionData.days[prevBlock.days.min()-3].block[doctor.id] != null) {
                val threePrevID = solutionData.days[prevBlock.days.min()-3].block[doctor.id]!!
                val blockThreePrev = doctor.blocksOfDays[threePrevID]!!

                if(prevBlockSize + blockThreePrev.days.size + 2 == 7) {
                    val blocks = mutableListOf(threePrevID, prevBlockID, block.id)
                    blocks.sortDescending()

                    val source = Source.InsufficientRestMidOverlap(Triple(blocks[0], blocks[1], blocks[2]))
                    infeasibilities[source] = solutionData.days[prevBlock.days.min()-2].overlappingNightShifts.toList()
                }
            }
        }
    }

    val threeToLeftInIndices = twoToLeftInIndices && firstDay - 3 in solutionData.days.indices
    val threeToLeftWorked =
        threeToLeftInIndices && solutionData.days[firstDay-3].block.contains(doctor.id)

    if(!twoToLeftWorked && threeToLeftWorked) {
        val prevBlockID = solutionData.days[firstDay-3].block[doctor.id]!!
        val prevBlock = doctor.blocksOfDays[prevBlockID] ?: throw Exception("addBlockInfeasibility: doctor $doctor does not have block $prevBlockID")

        /*
         * If there is a gap of two days between blocks where the size of the block, were
         * they and the gap to be combined, is greater than 7, any overlapping shifts two days
         * prior to the block being considered will be infeasible
         */
        if(prevBlock.days.size + blockSize + 2 > 7 &&
            solutionData.days[firstDay-2].overlappingNightShifts.isNotEmpty()) {
            val source = when(prevBlockID < block.id) {
                true -> Source.WouldCauseRowTooLargeOverlap(Pair(prevBlockID, block.id))
                false -> Source.WouldCauseRowTooLargeOverlap(Pair(block.id, prevBlockID))
            }
            infeasibilities[source] = solutionData.days[firstDay-2].overlappingNightShifts.toList()
        }

        if(twoToRightWorked && prevBlock.days.size + blockSize + 2 == 7) {
            val nextBlockID = solutionData.days[lastDay+2].block[doctor.id]!!
            val blocks = mutableListOf(prevBlockID, nextBlockID, block.id)
            blocks.sortDescending()

            val source = Source.InsufficientRestMidOverlap(Triple(blocks[0], blocks[1], blocks[2]))
            infeasibilities[source] = solutionData.days[firstDay-2].overlappingNightShifts.toList()
        }

        /*
         * If the previous block is six long, there is insufficient rest for the day after
         * the block to be worked (would cause a row of 7, requiring 48 hours rest).
         * Only needs to be evaluated if the day we are considering is the first day of
         * [block]
         */
        if(prevBlock.days.size == 6) {
            val source = Source.InsufficientRest(Pair(prevBlockID, block.id))
            infeasibilities[source] = solutionData.days[firstDay-2].getShifts()

            // If last day of [prevBlock] has overlapping night shifts they become infeasible
            if(solutionData.days[prevBlock.days.max()].overlappingNightShifts.isNotEmpty()) {
                val overlapSource = Source.InsufficientRestOverlap(Pair(prevBlockID, block.id))
                infeasibilities[overlapSource] =
                    solutionData.days[prevBlock.days.max()].overlappingNightShifts.toList()
            }
        }
    }

    val fourToLeftWorked = threeToLeftInIndices && firstDay - 4 in solutionData.days.indices
            && solutionData.days[firstDay-4].block.contains(doctor.id)
    // True if there is a gap of three days followed by a block of days
    if(!twoToLeftWorked && !threeToLeftWorked && fourToLeftWorked) {
        val prevBlockID = solutionData.days[firstDay-4].block[doctor.id]!!
        val prevBlock = doctor.blocksOfDays[prevBlockID] ?: throw Exception("sdfsd")

        /*
         * If the row is of size 5, and the following day has overlapping shifts, there
         * would be insufficient rest, were those overlapping shifts to be assigned
         */
        if(prevBlock.days.size == 5 &&
            solutionData.days[firstDay-3].overlappingNightShifts.isNotEmpty()) {
            val source = Source.InsufficientRestOverlap(Pair(prevBlockID, block.id))
            infeasibilities[source] = solutionData.days[firstDay-3].overlappingNightShifts.toList()
        }
    }
}

fun checkToRight(
    solutionData: SolutionData,
    block: Block,
    doctor: MiddleGrade,
    infeasibilities: MutableMap<Source, List<Int>>
) {
    val blockSize = block.days.size
    val firstDay = block.days.min()
    val lastDay = block.days.max()
    val twoToLeftInIndices = firstDay - 2 in solutionData.days.indices
    val twoToRightInIndices = lastDay + 2 in solutionData.days.indices
    // True if there exists a block to the right of [block] with a gap of one day
    val twoToRightWorked =
        twoToRightInIndices && solutionData.days[lastDay+2].block.contains(doctor.id)

    // If block size is six, any overlapping shifts on the next day are infeasible
    if(lastDay + 1 in solutionData.days.indices && blockSize == 6 &&
        solutionData.days[lastDay+1].overlappingNightShifts.isNotEmpty()) {
        val source = Source.RowOfSixOverlap(block.id)
        /*
         * If shifts to the left were made infeasible by this source, we need to add to the
         * list of shifts to be made infeasible, rather than overwrite it
         */
        when(infeasibilities[source]) {
            null -> infeasibilities[source] = solutionData.days[lastDay+1].overlappingNightShifts.toList()
            else -> infeasibilities[source] =
                infeasibilities[source]!! + solutionData.days[lastDay+1].overlappingNightShifts.toList()
        }
    }

    if(twoToRightWorked) {
        val nextBlockID = solutionData.days[lastDay+2].block[doctor.id]!!
        val nextBlock = doctor.blocksOfDays[nextBlockID] ?: throw Exception("addBlockInfeasibility: doctor $doctor does not have block $nextBlockID")

        /*
         * The combined length of the blocks, were the day between them to be worked, is
         * too great for any shift on that day to be feasible
         */
        if(nextBlock.days.size + blockSize + 1 > 7) {
            val source = when(nextBlockID < block.id) {
                true -> Source.WouldCauseRowTooLarge(Pair(nextBlockID, block.id))
                false -> Source.WouldCauseRowTooLarge(Pair(block.id, nextBlockID))
            }
            infeasibilities[source] = solutionData.days[lastDay+1].getShifts()
        }

        if(nextBlock.days.size + blockSize + 1 == 7 && nextBlock.days.max() + 2 in solutionData.days.indices
            && solutionData.days[nextBlock.days.max()+2].block[doctor.id] != null) {
            val blockAfter = solutionData.days[nextBlock.days.max()+2].block[doctor.id]!!
            val blocks = mutableListOf(blockAfter, nextBlockID, block.id)
            blocks.sortDescending()

            val source = Source.InsufficientRestMid(Triple(blocks[0], blocks[1], blocks[2]))
            infeasibilities[source] = solutionData.days[lastDay+1].getShifts()

        }

        /*
         * If [block] is 6 days long and there is a gap of one day followed by a worked day
         * ([twoToRightIsWorked]), then shifts of the day to the left of the block must
         * be infeasible as there cannot be 48 hours of rest after the hypothetical stretch
         * of seven days
         */
        if(blockSize == 6 && firstDay - 1 in solutionData.days.indices) {
            val source = Source.InsufficientRest(Pair(block.id, nextBlockID))
            infeasibilities[source] = solutionData.days[firstDay-1].getShifts()
        }

        /*
         * If the block is 5 days long, and [twoToRightIsWorked] == true, overlapping
         * night shifts of the day two days before the block need to be made infeasible
         * as there is no possible 48 hours of rest after the hypothetical row of 7 days
         */
        if(blockSize == 5 && twoToLeftInIndices) {
            val source = Source.InsufficientRestOverlap(Pair(block.id, nextBlockID))
            infeasibilities[source] = solutionData.days[firstDay-2].overlappingNightShifts.toList()
        }
    }

    val threeToRightInIndices = twoToRightInIndices && lastDay + 3 in solutionData.days.indices
    val threeToRightWorked =
        threeToRightInIndices && solutionData.days[lastDay+3].block.contains(doctor.id)

    if(!twoToRightWorked && threeToRightWorked) {
        val nextBlockID = solutionData.days[lastDay+3].block[doctor.id]!!
        val nextBlock = doctor.blocksOfDays[nextBlockID] ?: throw Exception("sdfsd")

        if(nextBlock.days.size + blockSize + 2 > 7 &&
            solutionData.days[lastDay+1].overlappingNightShifts.isNotEmpty()) {
            val source = when(nextBlockID < block.id) {
                true -> Source.WouldCauseRowTooLargeOverlap(Pair(nextBlockID, block.id))
                false -> Source.WouldCauseRowTooLargeOverlap(Pair(block.id, nextBlockID))
            }
            infeasibilities[source] = solutionData.days[lastDay+1].overlappingNightShifts.toList()
        }

        if(nextBlock.days.size + blockSize + 2 == 7 && nextBlock.days.max() + 2 in solutionData.days.indices
            && solutionData.days[nextBlock.days.max()+2].block[doctor.id] != null) {
            val blockAfter = solutionData.days[nextBlock.days.max()+2].block[doctor.id]!!
            val blocks = mutableListOf(blockAfter, nextBlockID, block.id)
            blocks.sortDescending()

            val source = Source.InsufficientRestMidOverlap(Triple(blocks[0], blocks[1], blocks[2]))
            infeasibilities[source] = solutionData.days[lastDay+1].overlappingNightShifts.toList()
        }

        if(blockSize == 6) {
            val source = Source.InsufficientRest(Pair(block.id, nextBlockID))
            infeasibilities[source] = solutionData.days[lastDay+1].getShifts()

            // If end day of [block] has overlapping shifts, they become infeasible
            if(solutionData.days[lastDay].overlappingNightShifts.isNotEmpty()) {
                val overlappingSource =
                    Source.InsufficientRestOverlap(Pair(block.id, nextBlockID))
                infeasibilities[overlappingSource] =
                    solutionData.days[lastDay].overlappingNightShifts.toList()
            }
        }
    }

    val fourToRightWorked = threeToRightInIndices && lastDay + 4 in solutionData.days.indices
            && solutionData.days[lastDay+4].block.contains(doctor.id)
    if(!twoToRightWorked && !threeToRightWorked && fourToRightWorked) {
        val nextBlockID = solutionData.days[lastDay+4].block[doctor.id]!!

        if(blockSize == 5 && solutionData.days[lastDay+1].overlappingNightShifts.isNotEmpty()) {
            val source = Source.InsufficientRestOverlap(Pair(block.id, nextBlockID))
            infeasibilities[source] = solutionData.days[lastDay+1].overlappingNightShifts.toList()
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
    val prevDay = block.days.min() - 1
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
    val endDay = solutionData.days[block.days.max()]
    // It is assumed that shift IDs increase with their end time
    val lastShift = endDay.doctorsWorkingDay[doctorID]?.max() ?: throw Exception("getEndShiftOfBlock: Doctor $doctorID absent from [doctorsWorkingDay] of day ${endDay.id}")
    return solutionData.shifts[lastShift]
}

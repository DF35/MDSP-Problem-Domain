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

// Ascertains which checks for long shift-related feasibility need to be made
fun updateFeasibilityLongShifts(
    solutionData: SolutionData,
    shift: Shift,
    doctorID: Int,
    allocated: Boolean
) {
    when {
        // Long shift has been allocated
        allocated && shift.long -> longShiftAdded(solutionData, shift, doctorID)
        // Non-long shift has been added
        allocated && !shift.long -> impactShiftAdded(solutionData, shift, doctorID)
        // Long shift has been removed
        !allocated && shift.long -> longShiftRemoved(solutionData, shift, doctorID)
        // Non-long shift has been removed
        else -> impactShiftRemoved(solutionData, shift, doctorID)
    }
}

private fun longShiftAdded(
    solutionData: SolutionData,
    shift: Shift,
    doctorID: Int,
) {
    val dayID = shift.day
    val days = solutionData.days
    val doctor = solutionData.doctors[doctorID]

    // Days are used to track rows of long shifts
    days[dayID].doctorsWorkingLongShift[doctorID] = shift.id

    // Checks for long shifts to either side
    val workedLongShiftToLeft = dayID - 1 in days.indices &&
            days[dayID-1].longShiftBlock[doctorID] != null
    val workedLongShiftToRight = dayID + 1 in days.indices &&
            days[dayID+1].longShiftBlock[doctorID] != null

    val blockID = when {
        workedLongShiftToLeft && workedLongShiftToRight ->
            mergeBlocksLongShift(
                solutionData, doctor, days[dayID-1].longShiftBlock[doctorID]!!,
                days[dayID+1].longShiftBlock[doctorID]!!
            )

        workedLongShiftToLeft -> days[dayID-1].longShiftBlock[doctorID]!!

        workedLongShiftToRight -> days[dayID+1].longShiftBlock[doctorID]!!

        else -> doctor.nextBlockID
    }

    // If no block existed to either side, a new one is crated, [doctor] is updated accordingly
    if(blockID == doctor.nextBlockID) {
        doctor.nextBlockID++
        doctor.blocksOfLongShifts[blockID] = Block(blockID)
    }

    val block = doctor.blocksOfLongShifts[blockID]
        ?: throw Exception("longShiftAdded: Block $blockID missing from doctor $doctorID")

    // Removes all infeasibilities associated with the block of long shifts
    clearInfeasibleShiftsLongShiftBlock(solutionData, block, doctorID)

    block.addItem(dayID)
    days[dayID].longShiftBlock[doctorID] = blockID

    // Recalculates and implements infeasibilities for the block now that the long shift has been added
    val infeasibilities = identifyLongShiftInfeasibilities(solutionData, block, doctorID)
    implementLongShiftInfeasibilities(solutionData, doctor, infeasibilities)

    // Calculates the impact of the shift on preceding long day shifts
    impactShiftAdded(solutionData, shift, doctorID)
}

// Merges two blocks of long shifts
private fun mergeBlocksLongShift(
    solutionData: SolutionData,
    doctor: MiddleGrade,
    leftBlock: Int,
    rightBlock: Int
): Int {
    for(day in doctor.blocksOfLongShifts[leftBlock]!!.items) {
        doctor.blocksOfLongShifts[rightBlock]!!.addItem(day)
        solutionData.days[day].longShiftBlock[doctor.id] = rightBlock
    }

    clearInfeasibleShiftsLongShiftBlock(
        solutionData, doctor.blocksOfLongShifts[leftBlock]!!, doctor.id
    )

    doctor.blocksOfLongShifts.remove(leftBlock)
    return rightBlock
}

private fun longShiftRemoved(
    solutionData: SolutionData,
    shift: Shift,
    doctorID: Int
) {
    val days = solutionData.days
    val day = days[shift.day]
    val doctor = solutionData.doctors[doctorID]
    val block = doctor.blocksOfLongShifts[day.longShiftBlock[doctorID]]
        ?: throw Exception("longShiftRemoved: doctor $doctorID missing long shift block for shift ${shift.id}")

    clearInfeasibleShiftsLongShiftBlock(solutionData, block, doctorID)

    val (position, blocks) = block.removeItem(day.id, doctor.nextBlockID)
    day.longShiftBlock.remove(doctorID)
    day.doctorsWorkingLongShift.remove(doctorID)

    val infeasibilities = when(position) {
        // Block is now empty
        ItemRemovedPos.Final -> {
            doctor.blocksOfLongShifts.remove(block.id)
            emptyMap()
        }
        // Block split in two
        ItemRemovedPos.Middle ->
            processSplitBlockLongShift(solutionData, blocks!!, doctor)
        // Long shift removed from left end of block
        ItemRemovedPos.Start ->
            identifyLongShiftInfeasibilities(solutionData, block, doctorID)
        // Long shift removed from right end of block
        ItemRemovedPos.End ->
            identifyLongShiftInfeasibilities(solutionData, block, doctorID)
    }

    implementLongShiftInfeasibilities(solutionData, doctor, infeasibilities)

    // Removes the impact of the shift on preceding long day shifts
    impactShiftRemoved(solutionData, shift, doctorID)
}

private fun processSplitBlockLongShift(
    solutionData: SolutionData,
    blocks: Pair<Block, Block>,
    doctor: MiddleGrade
): MutableMap<Source, MutableList<Int>> {
    // Updates [doctor] with the created blocks
    doctor.blocksOfLongShifts[blocks.first.id] = blocks.first
    doctor.blocksOfLongShifts[blocks.second.id] = blocks.second

    // Updates days that are now in the newly created block
    for(day in blocks.second.items)
        solutionData.days[day].longShiftBlock[doctor.id] = blocks.second.id
    doctor.nextBlockID++

    val infeasibilities = identifyLongShiftInfeasibilities(solutionData, blocks.first, doctor.id)
    checkToRightLongShift(solutionData, blocks.second, doctor.id, infeasibilities)

    return infeasibilities
}

// Calculates and implements the impact of [shift] on preceding long day shifts
private fun impactShiftAdded(solutionData: SolutionData, shift: Shift, doctorID: Int) {
    // Search for assigned long shifts
    var workedShiftFound = false
    var currentShift = Int.MAX_VALUE
    for(longShiftID in shift.longShifts48HoursBefore) {
        currentShift = longShiftID
        if(solutionData.shifts[longShiftID].assignees.contains(doctorID)) {
            workedShiftFound = true
            break
        }
    }

    val doctor = solutionData.doctors[doctorID]
    val shifts = solutionData.shifts
    val days = solutionData.days

    val block = when(workedShiftFound) {
        // Recalculate feasibility for block that the worked shift is a part of
        true -> {
            // Identify block
            val blockID =
                solutionData.days[shifts[currentShift].day].longShiftBlock[doctorID]
            doctor.blocksOfLongShifts[blockID]!!
        }

        // Check day before for block
        false -> {
            if(shift.longShifts48HoursBefore.isEmpty()) return
            val endShift = shifts[shift.longShifts48HoursBefore.min()]
            if(endShift.day - 2 !in days.indices) return
            val priorDay = days[endShift.day-2]
            if(priorDay.longShiftBlock[doctorID] == null) return
            doctor.blocksOfLongShifts[priorDay.longShiftBlock[doctorID]!!]!!
        }
    }

    // Update sources of infeasibility
    clearInfeasibleShiftsLongShiftBlock(solutionData, block, doctorID)
    val infeasibilities =
        identifyLongShiftInfeasibilities(solutionData, block, doctorID)
    implementLongShiftInfeasibilities(solutionData, doctor, infeasibilities)
}

// Removes the infeasibilities for long shifts involving [shift]
private fun impactShiftRemoved(solutionData: SolutionData, shift: Shift, doctorID: Int) {
    if(shift.longShiftsMadeInfeasible[doctorID] == null) return

    val relevantSources = mutableMapOf<Shift, List<Source>>()
    for(shiftID in shift.longShiftsMadeInfeasible[doctorID]!!) {
        val infShift = solutionData.shifts[shiftID]
        relevantSources[infShift] = infShift.causesOfInfeasibility[doctorID]!!.sources.filter {
            sourceContainsShift(it, shift.id)
        }
    }

    for((infShift, sources) in relevantSources) {
        for(source in sources) {
            when(source) {
                is Source.InsufficientRestForRowOfFourLongShifts -> {
                    val shifts = source.blockAndShifts.second.toMutableSet()

                    when(shifts.size == 1) {
                        // If this was the only shift causing the infeasibility, it can be removed
                        true -> removeRelevantSourceLongShift(
                            solutionData, source, infShift, doctorID
                        )

                        // Shift is removed from the source of infeasibility
                        false -> {
                            val block = source.blockAndShifts.first
                            shifts.remove(shift.id)
                            shift.longShiftsMadeInfeasible[doctorID]!!.remove(infShift.id)

                            // Update source of infeasibility for [infShift]
                            infShift.removeSource(doctorID, source)
                            infShift.restInfeasibility(doctorID,
                                Source.InsufficientRestForRowOfFourLongShifts(
                                    Pair(block, shifts)
                                )
                            )
                        }
                    }
                }

                is Source.InsufficientRestForRowOfFourLongShiftsMid -> {
                    val shifts = source.blocksAndShifts.third.toMutableSet()

                    when(shifts.size == 1) {
                        // If this was the only shift causing the infeasibility, it can be removed
                        true -> removeRelevantSourceLongShift(
                            solutionData, source, infShift, doctorID
                        )

                        // Shift is removed from the source of infeasibility
                        false -> {
                            val block1 = source.blocksAndShifts.first
                            val block2 = source.blocksAndShifts.second
                            shifts.remove(shift.id)
                            shift.longShiftsMadeInfeasible[doctorID]!!.remove(infShift.id)

                            // Update source of infeasibility for [infShift]
                            infShift.removeSource(doctorID, source)
                            infShift.restInfeasibility(doctorID,
                                Source.InsufficientRestForRowOfFourLongShiftsMid(
                                    Triple(block1, block2, shifts)
                                )
                            )
                        }
                    }
                }
                else -> throw Exception("impactShiftRemoved: Invalid source selected by sourceContainsShift")
            }

        }
    }
}

// Removes all infeasibilities caused by a block of long shifts
private fun clearInfeasibleShiftsLongShiftBlock(
    solutionData: SolutionData,
    block: Block,
    doctorID: Int
) {
    val relevantSources = mutableMapOf<Shift, List<Source>>()
    for(shiftID in block.shiftsMadeInfeasible) {
        val shift = solutionData.shifts[shiftID]
        relevantSources[shift] = shift.causesOfInfeasibility[doctorID]!!.sources.filter {
            sourceContainsBlock(it, block.id)
        }
    }

    for((shift, sources) in relevantSources)
        sources.forEach { removeRelevantSourceLongShift(solutionData, it, shift, doctorID) }
}

private fun removeRelevantSourceLongShift(
    solutionData: SolutionData,
    source: Source,
    shift: Shift,
    doctorID: Int
) {
    shift.removeSource(doctorID, source)

    val relevantShifts = mutableSetOf<Int>()
    val relevantBlocks = when(source) {
        is Source.RowOfFourLongShifts -> listOf(source.block)

        is Source.WouldCauseTooLargeRowOfLongShifts ->
            listOf(source.blocks.first, source.blocks.second)

        is Source.InsufficientRestForRowOfFourLongShifts -> {
            relevantShifts.addAll(source.blockAndShifts.second)
            listOf(source.blockAndShifts.first)
        }

        is Source.InsufficientRestForRowOfFourLongShiftsMid -> {
            relevantShifts.addAll(source.blocksAndShifts.third)
            listOf(source.blocksAndShifts.first, source.blocksAndShifts.second)
        }

        else -> throw Exception("removeRelevantSourceLongShift: function can only be passed causes of infeasibility relating to long shifts")
    }

    for(block in relevantBlocks)
        if(!longShiftBlockStillPresent(shift, block, doctorID))
            solutionData.doctors[doctorID].blocksOfLongShifts[block]!!.shiftsMadeInfeasible.remove(shift.id)

    for(shiftID in relevantShifts)
        if(!shiftStillPresent(shift, shiftID, doctorID))
            solutionData.shifts[shiftID].removeInfeasibleShift(doctorID, shift.id)
}

private fun sourceContainsBlock(source: Source, blockID: Int): Boolean {
    return when(source) {
        is Source.RowOfFourLongShifts -> source.block == blockID

        is Source.WouldCauseTooLargeRowOfLongShifts ->
            source.blocks.first == blockID || source.blocks.second == blockID

        is Source.InsufficientRestForRowOfFourLongShifts ->
            source.blockAndShifts.first == blockID

        is Source.InsufficientRestForRowOfFourLongShiftsMid ->
            source.blocksAndShifts.first == blockID || source.blocksAndShifts.second == blockID

        else -> false
    }
}

// Only considers sources relating to long shift feasibility
private fun sourceContainsShift(source: Source, shiftID: Int): Boolean {
    return when(source) {
        is Source.InsufficientRestForRowOfFourLongShifts ->
            source.blockAndShifts.second.contains(shiftID)

        is Source.InsufficientRestForRowOfFourLongShiftsMid ->
            source.blocksAndShifts.third.contains(shiftID)

        else -> return false
    }
}

private fun identifyLongShiftInfeasibilities(
    solutionData: SolutionData,
    block: Block,
    doctorID: Int
): MutableMap<Source, MutableList<Int>> {
    val infeasibilities = mutableMapOf<Source, MutableList<Int>>()
    val days = solutionData.days
    val leftInIndices = block.items.min() - 1 in days.indices
    val rightInIndices = block.items.max() + 1 in days.indices

    if(block.items.size > 4) throw Exception("identifyLongShiftInfeasibilities: illegal long shift block size reached")

    if(block.items.size == 4) {
        val shiftsMadeInfeasible = mutableListOf<Int>()

        // If stretch is four long, long shifts of days before cannot be worked
        if(leftInIndices)
            shiftsMadeInfeasible.addAll(days[block.items.min()-1].longShifts)

        // If stretch is four long, 48 hours of rest must follow the final shift
        val endShiftID = days[block.items.max()].doctorsWorkingLongShift[doctorID]
            ?: throw Exception("identifyLongShiftInfeasibilities: day ${block.items.max()} in long shift block but lacks long shift for doctor $doctorID")
        shiftsMadeInfeasible.addAll(solutionData.shifts[endShiftID].shifts48HoursAfter)

        val source = Source.RowOfFourLongShifts(block.id)
        infeasibilities[source] = shiftsMadeInfeasible
    }

    if(block.items.size == 3) {
        // Check last shift to see if it is followed by 48 hours of rest
        val endShiftID = days[block.items.max()].doctorsWorkingLongShift[doctorID]
            ?: throw Exception("identifyLongShiftInfeasibilities: day ${block.items.max()} in long shift block but lacks long shift for doctor $doctorID")

        val shiftsWorkedInRestPeriod =
            findShiftsWorkedInRestPeriod(solutionData, endShiftID, doctorID)

        if(shiftsWorkedInRestPeriod.isNotEmpty() && leftInIndices) {
            val source = Source.InsufficientRestForRowOfFourLongShifts(
                Pair(block.id, shiftsWorkedInRestPeriod.toSet())
            )
            infeasibilities[source] = days[block.items.min()-1].longShifts.toMutableList()
        }

        /*
         * Check next day's long shifts to see if they have enough rest to be worked.
         * Must be checked individually as different shifts have different end times
         * so [shifts48HoursAfter] may be different for each.
         */
        if(rightInIndices) {
            for(shift in days[block.items.max()+1].longShifts) {
                val shiftsWorkedInShiftRestPeriod =
                    findShiftsWorkedInRestPeriod(solutionData, shift, doctorID)

                if(shiftsWorkedInShiftRestPeriod.isNotEmpty()) {
                    val source = Source.InsufficientRestForRowOfFourLongShifts(
                        Pair(block.id, shiftsWorkedInShiftRestPeriod.toSet())
                    )
                    // Shifts could have the same source of infeasibility
                    val shiftsMadeInfeasible = infeasibilities.getOrDefault(source, mutableListOf())
                    shiftsMadeInfeasible.add(shift)
                    infeasibilities[source] = shiftsMadeInfeasible
                }
            }
        }
    }

    // Check for block to left of [block]
    val twoToLeftInIndices = leftInIndices && block.items.min() - 2 in days.indices
    if(twoToLeftInIndices && days[block.items.min()-2].doctorsWorkingLongShift[doctorID] != null) {
        val prevBlockID = days[block.items.min()-2].longShiftBlock[doctorID]!!
        val priorBlock = solutionData.doctors[doctorID].blocksOfLongShifts[prevBlockID]!!

        // Stretches of long shifts greater than 4 cannot be assigned
        if(priorBlock.items.size + block.items.size + 1 > 4) {
            val source = when(prevBlockID < block.id) {
                true -> Source.WouldCauseTooLargeRowOfLongShifts(Pair(prevBlockID, block.id))
                false -> Source.WouldCauseTooLargeRowOfLongShifts(Pair(block.id, prevBlockID))
            }

            infeasibilities[source] = days[block.items.min()-1].longShifts.toMutableList()
        }

        // Shifts of long shifts that are 4 long must be followed by 48 hours of rest
        if(priorBlock.items.size + block.items.size + 1 == 4) {
            val endShiftID = days[block.items.max()].doctorsWorkingLongShift[doctorID]
                ?: throw Exception("identifyLongShiftInfeasibilities: day ${block.items.max()} in long shift block but lacks long shift for doctor $doctorID")

            val shiftsWorkedInRestPeriod =
                findShiftsWorkedInRestPeriod(solutionData, endShiftID, doctorID)

            if(shiftsWorkedInRestPeriod.isNotEmpty()) {
                val source = when(prevBlockID < block.id) {
                    true -> Source.InsufficientRestForRowOfFourLongShiftsMid(
                        Triple(prevBlockID, block.id, shiftsWorkedInRestPeriod.toSet())
                    )

                    false -> Source.InsufficientRestForRowOfFourLongShiftsMid(
                        Triple(block.id, prevBlockID, shiftsWorkedInRestPeriod.toSet())
                    )
                }

                infeasibilities[source] = days[block.items.min()-1].longShifts.toMutableList()
            }
        }
    }

    // Check for block to right of [block]
    checkToRightLongShift(solutionData, block, doctorID, infeasibilities)

    return infeasibilities
}

private fun checkToRightLongShift(
    solutionData: SolutionData,
    block: Block,
    doctorID: Int,
    infeasibilities: MutableMap<Source, MutableList<Int>>
) {
    val days = solutionData.days
    val rightInIndices = block.items.max() + 1 in days.indices

    val twoToRightInIndices = rightInIndices && block.items.max() + 2 in days.indices
    if(twoToRightInIndices && days[block.items.max()+2].doctorsWorkingLongShift[doctorID] != null) {
        val nextBlockID = days[block.items.max()+2].longShiftBlock[doctorID]!!
        val nextBlock = solutionData.doctors[doctorID].blocksOfLongShifts[nextBlockID]!!

        // Stretches of long shifts greater than 4 cannot be assigned
        if(nextBlock.items.size + block.items.size + 1 > 4) {
            val source = when(nextBlockID < block.id) {
                true -> Source.WouldCauseTooLargeRowOfLongShifts(Pair(nextBlockID, block.id))
                false -> Source.WouldCauseTooLargeRowOfLongShifts(Pair(block.id, nextBlockID))
            }

            infeasibilities[source] = days[block.items.max()+1].longShifts.toMutableList()
        }

        // Shifts of long shifts that are 4 long must be followed by 48 hours of rest
        if(nextBlock.items.size + block.items.size + 1 == 4) {
            val endShift = days[nextBlock.items.max()].doctorsWorkingLongShift[doctorID]
                ?: throw Exception("identifyLongShiftInfeasibilities: day ${block.items.max()} in long shift block but lacks long shift for doctor $doctorID")

            val shiftsWorkedInRestPeriod =
                findShiftsWorkedInRestPeriod(solutionData, endShift, doctorID)

            if(shiftsWorkedInRestPeriod.isNotEmpty()) {
                val source = when(nextBlockID < block.id) {
                    true -> Source.InsufficientRestForRowOfFourLongShiftsMid(
                        Triple(nextBlockID, block.id, shiftsWorkedInRestPeriod.toSet())
                    )

                    false -> Source.InsufficientRestForRowOfFourLongShiftsMid(
                        Triple(block.id, nextBlockID, shiftsWorkedInRestPeriod.toSet())
                    )
                }

                infeasibilities[source] = days[block.items.max()+1].longShifts.toMutableList()
            }
        }
    }
}

// Checks for shifts worked 48 hours after a long shift
private fun findShiftsWorkedInRestPeriod(
    solutionData: SolutionData,
    shiftID: Int,
    doctorID: Int
): List<Int> {
    val shiftsWorkedInRestPeriod = mutableListOf<Int>()

    for(shift in solutionData.shifts[shiftID].shifts48HoursAfter) {
        if(solutionData.shifts[shift].assignees.contains(doctorID))
            shiftsWorkedInRestPeriod.add(shift)
    }

    return shiftsWorkedInRestPeriod
}

private fun implementLongShiftInfeasibilities(
    solutionData: SolutionData,
    doctor: MiddleGrade,
    infeasibilities: Map<Source, MutableList<Int>>
) {
    for((source, shifts) in infeasibilities) {
        shifts.forEach { solutionData.shifts[it].restInfeasibility(doctor.id, source) }

        when(source) {
            is Source.RowOfFourLongShifts ->
                doctor.blocksOfLongShifts[source.block]!!.shiftsMadeInfeasible.addAll(shifts)

            is Source.WouldCauseTooLargeRowOfLongShifts -> {
                doctor.blocksOfLongShifts[source.blocks.first]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfLongShifts[source.blocks.second]!!.shiftsMadeInfeasible.addAll(shifts)
            }

            is Source.InsufficientRestForRowOfFourLongShifts -> {
                doctor.blocksOfLongShifts[source.blockAndShifts.first]!!.shiftsMadeInfeasible.addAll(shifts)
                for(shiftID in source.blockAndShifts.second)
                    shifts.forEach { solutionData.shifts[shiftID].addInfeasibleShift(doctor.id, it) }
            }

            is Source.InsufficientRestForRowOfFourLongShiftsMid -> {
                doctor.blocksOfLongShifts[source.blocksAndShifts.first]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfLongShifts[source.blocksAndShifts.second]!!.shiftsMadeInfeasible.addAll(shifts)
                for(shiftID in source.blocksAndShifts.third)
                    shifts.forEach { solutionData.shifts[shiftID].addInfeasibleShift(doctor.id, it) }
            }

            else -> throw Exception("implementLongShiftInfeasibilities: invalid source passed")
        }
    }
}


private fun longShiftBlockStillPresent(shift: Shift, blockID: Int, doctorID: Int): Boolean {
    val sources = shift.causesOfInfeasibility[doctorID]?.sources ?: return false

    for(source in sources) {
        when(sourceContainsBlock(source, blockID)) {
            true -> return true
            false -> continue
        }
    }

    return false
}

/*
 * [shift] - the shift being checked
 * [shiftID] - ID of the shift previously involved in the long shift related infeasibility
 */
private fun shiftStillPresent(shift: Shift, shiftID: Int, doctorID: Int): Boolean {
    val sources = shift.causesOfInfeasibility[doctorID]?.sources ?: return false

    for(source in sources) {
        when(sourceContainsShift(source, shiftID)) {
            true -> return true
            false -> continue
        }
    }

    return false
}
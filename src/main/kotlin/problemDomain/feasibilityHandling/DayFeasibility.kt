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

/******************************************************
 * Functions Relating to the addition of a worked day *
 ******************************************************/

// Adds day to block, identifies where it was added and calls relevant feasibility functions
fun updateFeasibilityDayAdded(solutionData: SolutionData, doctorID: Int, dayID: Int) {
    val days = solutionData.days
    val doctor = solutionData.doctors[doctorID]

    val leftInBlock = dayID - 1 in days.indices && days[dayID-1].block[doctorID] != null
    val rightInBlock = dayID + 1 in days.indices && days[dayID+1].block[doctorID] != null

    // Decides the block that [dayID] should be added to
    val blockID = when {
        // Right and left blocks are merged
        leftInBlock && rightInBlock -> mergeBlocksDay(
                solutionData, doctor, days[dayID-1].block[doctorID]!!, days[dayID+1].block[doctorID]!!
            )
        // Only left day is in a block
        leftInBlock -> days[dayID-1].block[doctorID]!!
        // Only right day is in a block
        rightInBlock -> days[dayID+1].block[doctorID]!!
        // Neither day is in a block
        else -> doctor.nextBlockID
    }

    // If no block existed to either side, a new one is crated, [doctor] is updated accordingly
    if(blockID == doctor.nextBlockID) {
        doctor.nextBlockID++
        doctor.blocksOfDays[blockID] = Block(blockID)
    }

    val block = doctor.blocksOfDays[blockID]
        ?: throw Exception("updateFeasibilityDaysWorked: Block $blockID missing from doctor $doctorID")

    // Remove previous sources of infeasibility
    clearInfeasibleShiftsBlock(solutionData, block, doctorID)

    // [dayID] is added to the identified block of [doctor]
    block.addItem(dayID)
    days[dayID].block[doctorID] = blockID

    // Assesses the impact that the added day has on feasibility
    val infeasibilities = identifySourcesOfInfeasibilityDay(solutionData, doctor, block)
    implementInfeasibilities(solutionData, doctor, infeasibilities)
}


/*
 * Merges two blocks, returning the ID of merged block (same as [rightBlock]), [leftBlock]
 * is removed from [blocksOfDays] of [doctor]
 */
private fun mergeBlocksDay(
    solutionData: SolutionData,
    doctor: MiddleGrade,
    leftBlock: Int,
    rightBlock: Int
): Int {
    for(day in doctor.blocksOfDays[leftBlock]!!.items) {
        doctor.blocksOfDays[rightBlock]!!.addItem(day)
        solutionData.days[day].block[doctor.id] = rightBlock
    }

    // Removes sources of infeasibility relating to the block that no longer exists
    clearInfeasibleShiftsBlock(solutionData, doctor.blocksOfDays[leftBlock]!!, doctor.id)

    doctor.blocksOfDays.remove(leftBlock)
    return rightBlock
}

/*****************************************************
 * Functions relating to the removal of a worked day *
 *****************************************************/

/*
 * Removes day from block, identifies impact on the block itself, and calls relevant
 * feasibility functions
 */
fun updateFeasibilityDayRemoved(solutionData: SolutionData, doctorID: Int, dayID: Int) {
    val days = solutionData.days
    val doctor = solutionData.doctors[doctorID]
    val block = doctor.blocksOfDays[days[dayID].block[doctorID]] ?: throw Exception("updateFeasibilityDayRemoved: Day $dayID is not allocated to a block for doctor $doctorID, despite them working on it")

    /*
     * Previous sources of infeasibility for the block are removed; the impact of the
     * block(s) on feasibility regarding days worked, will be reassessed after [dayID]
     * is removed
     */
    clearInfeasibleShiftsBlock(solutionData, block, doctorID)

    val (position, blocks) = block.removeItem(dayID, doctor.nextBlockID)
    // Reference to the block is removed from the day that is no longer worked
    days[dayID].block.remove(doctorID)

    // New sources of infeasibility are calculated
    val infeasibilities = when(position) {
        // Day was the last in the block
        ItemRemovedPos.Final -> processEmptyBlockDay(solutionData, block, doctor, dayID)
        // Block was split in two
        ItemRemovedPos.Middle -> processSplitBlockDay(solutionData, blocks!!, doctor)
        // Day was leftmost of block
        ItemRemovedPos.Start -> identifySourcesOfInfeasibilityDay(solutionData, doctor, block)
        // Day was rightmost of block
        ItemRemovedPos.End -> identifySourcesOfInfeasibilityDay(solutionData, doctor, block)
    }

    // Updates feasibility as required
    implementInfeasibilities(solutionData, doctor, infeasibilities)
}

// Deals with a block that no longer has any days in it
private fun processEmptyBlockDay(
    solutionData: SolutionData,
    block: Block,
    doctor: MiddleGrade,
    removedDay: Int
): MutableMap<Source, List<Int>> {
    val days = solutionData.days

    // Record of block is removed from [doctor]
    doctor.blocksOfDays.remove(block.id)

    // Potential sources of infeasibility for neighbouring blocks are calculated
    val infeasibilities = mutableMapOf<Source, List<Int>>()
    val twoToLeftIsWorked =
        removedDay - 2 in days.indices && days[removedDay-2].block.contains(doctor.id)
    val twoToRightIsWorked =
        removedDay + 2 in days.indices && days[removedDay+2].block.contains(doctor.id)
    if(twoToLeftIsWorked && twoToRightIsWorked)
        checkToRightDay(solutionData, doctor.blocksOfDays[days[removedDay - 2].block[doctor.id]]!!,
            doctor, infeasibilities)

    return infeasibilities
}

// Deals with a block that has been split in half by the removal of a day
private fun processSplitBlockDay(
    solutionData: SolutionData,
    blocks: Pair<Block, Block>,
    doctor: MiddleGrade,
): MutableMap<Source, List<Int>> {
    // Updates [doctor] with created blocks
    doctor.blocksOfDays[blocks.first.id] = blocks.first
    doctor.blocksOfDays[blocks.second.id] = blocks.second

    // Updates days that are now in the newly created block
    for(day in blocks.second.items)
        solutionData.days[day].block[doctor.id] = blocks.second.id
    doctor.nextBlockID++

    // Update feasibility according to new blocks
    val infeasibilities = identifySourcesOfInfeasibilityDay(solutionData, doctor, blocks.first)
    checkToRightDay(solutionData, blocks.second, doctor, infeasibilities)

    return infeasibilities
}

/*************************************************************************
 * Functions used to add and remove day related sources of infeasibility *
 *************************************************************************/

// Adds identified sources of infeasibility to relevant shifts, updates relevant blocks
private fun implementInfeasibilities(
    solutionData: SolutionData,
    doctor: MiddleGrade,
    infeasibilities: MutableMap<Source, List<Int>>
) {
    // Make relevant shifts infeasible
    for((source, shifts) in infeasibilities) {
        shifts.forEach { solutionData.shifts[it].restInfeasibility(doctor.id, source) }

        when(source) {
            is Source.RowOfSevenDays ->
                doctor.blocksOfDays[source.block]!!.shiftsMadeInfeasible.addAll(shifts)

            is Source.InsufficientRest -> {
                doctor.blocksOfDays[source.blocks.first]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfDays[source.blocks.second]!!.shiftsMadeInfeasible.addAll(shifts)
            }

            is Source.InsufficientRestOverlap -> {
                doctor.blocksOfDays[source.blocks.first]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfDays[source.blocks.second]!!.shiftsMadeInfeasible.addAll(shifts)
            }

            is Source.WouldCauseRowTooLarge -> {
                doctor.blocksOfDays[source.blocks.first]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfDays[source.blocks.second]!!.shiftsMadeInfeasible.addAll(shifts)
            }

            is Source.WouldCauseRowTooLargeOverlap -> {
                doctor.blocksOfDays[source.blocks.first]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfDays[source.blocks.second]!!.shiftsMadeInfeasible.addAll(shifts)
            }

            is Source.RowOfSixOverlap ->
                doctor.blocksOfDays[source.block]!!.shiftsMadeInfeasible.addAll(shifts)

            is Source.InsufficientRestMid -> {
                doctor.blocksOfDays[source.blocks.first]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfDays[source.blocks.second]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfDays[source.blocks.third]!!.shiftsMadeInfeasible.addAll(shifts)
            }

            is Source.InsufficientRestMidOverlap -> {
                doctor.blocksOfDays[source.blocks.first]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfDays[source.blocks.second]!!.shiftsMadeInfeasible.addAll(shifts)
                doctor.blocksOfDays[source.blocks.third]!!.shiftsMadeInfeasible.addAll(shifts)
            }

            else -> throw Exception("implementInfeasibilites: invalid source passed")
        }
    }
}

// Removes all sources of infeasibility caused by a given [block]
private fun clearInfeasibleShiftsBlock(
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
        sources.forEach { removeRelevantSource(solutionData, it, shift, doctorID) }
}

private fun sourceContainsBlock(source: Source, blockID: Int): Boolean {
    return when(source) {
        is Source.RowOfSevenDays -> source.block == blockID
        is Source.WouldCauseRowTooLarge ->
            source.blocks.first == blockID || source.blocks.second == blockID
        is Source.WouldCauseRowTooLargeOverlap ->
            source.blocks.first == blockID || source.blocks.second == blockID
        is Source.RowOfSixOverlap -> source.block == blockID
        is Source.InsufficientRest ->
            source.blocks.first == blockID || source.blocks.second == blockID
        is Source.InsufficientRestOverlap ->
            source.blocks.first == blockID || source.blocks.second == blockID
        is Source.InsufficientRestMid -> source.blocks.first == blockID ||
                source.blocks.second == blockID || source.blocks.third == blockID
        is Source.InsufficientRestMidOverlap -> source.blocks.first == blockID ||
                source.blocks.second == blockID || source.blocks.third == blockID
        else -> false
    }
}

private fun removeRelevantSource(
    solutionData: SolutionData,
    source: Source,
    shift: Shift,
    doctorID: Int
) {
    shift.removeSource(doctorID, source)

    val relevantBlocks = when(source) {
        is Source.InsufficientRest -> listOf(source.blocks.first, source.blocks.second)

        is Source.RowOfSevenDays -> listOf(source.block)

        is Source.RowOfSixOverlap -> listOf(source.block)

        is Source.InsufficientRestMid ->
                listOf(source.blocks.first, source.blocks.second, source.blocks.third)

        is Source.InsufficientRestMidOverlap ->
                listOf(source.blocks.first, source.blocks.second, source.blocks.third)

        is Source.InsufficientRestOverlap -> listOf(source.blocks.first, source.blocks.second)

        is Source.WouldCauseRowTooLarge -> listOf(source.blocks.first, source.blocks.second)

        is Source.WouldCauseRowTooLargeOverlap -> listOf(source.blocks.first, source.blocks.second)

        else -> throw Exception("processBlockCausedSource: function can only be passed sources of infeasibility relating to blocks")
    }

    for(block in relevantBlocks)
        if(!blockStillPresent(shift, block, doctorID))
            solutionData.doctors[doctorID].blocksOfDays[block]!!.shiftsMadeInfeasible.remove(shift.id)
}


fun blockStillPresent(shift: Shift, blockID: Int, doctorID: Int): Boolean {
    val sources = shift.causesOfInfeasibility[doctorID]?.sources ?: return false

    for(source in sources) {
        when(sourceContainsBlock(source, blockID)) {
            true -> return true
            false -> continue
        }
    }
    return false
}




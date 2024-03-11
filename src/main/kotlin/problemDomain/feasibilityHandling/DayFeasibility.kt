package problemDomain.feasibilityHandling

import problemDomain.Block
import problemDomain.MiddleGrade
import problemDomain.SolutionData

fun updateFeasibilityDayAdded(solutionData: SolutionData, doctorID: Int, dayID: Int) {
    val days = solutionData.days
    val doctor = solutionData.doctors[doctorID]

    val leftInBlock = dayID - 1 in days.indices && days[dayID - 1].block[doctorID] != null
    val rightInBlock = dayID + 1 in days.indices && days[dayID + 1].block[doctorID] != null

    // Decides the block that [dayID] should be added to
    val block = when {
        // Right and left blocks are merged
        leftInBlock && rightInBlock -> mergeBlocks(
            solutionData, doctor, days[dayID-1].block[doctorID]!!, days[dayID+1].block[doctorID]!!)
        // Only left day is in a block
        leftInBlock -> days[dayID-1].block[doctorID] ?: throw Exception("updateFeasibilityDayAdded: day $dayID not in any block despite being worked")
        // Only right day is in a block
        rightInBlock -> days[dayID+1].block[doctorID] ?: throw Exception("updateFeasibilityDayAdded: day $dayID not in any block despite being worked")
        // Neither day is in a block
        else -> doctor.nextBlockID
    }

    // If no block existed to either side, a new one needs to be created
    if(block == doctor.nextBlockID) {
        doctor.nextBlockID++
        doctor.blocksOfDays[block] = Block(block)
    }

    // [dayID] is added to the identified block of [doctor]
    doctor.blocksOfDays[block]!!.addDay(dayID)
    days[dayID].block[doctorID] = block
}


/*
 * Merges two blocks, returning the ID of merged block (same as [rightBlock]), [leftBlock]
 * is removed from [blocksOfDays] of [doctor]
 */
private fun mergeBlocks(
    solutionData: SolutionData,
    doctor: MiddleGrade,
    leftBlock: Int,
    rightBlock: Int
): Int {
    for(day in doctor.blocksOfDays[leftBlock]!!.days) {
        doctor.blocksOfDays[rightBlock]!!.addDay(day)
        solutionData.days[day].block[doctor.id] = rightBlock
    }
    doctor.blocksOfDays.remove(leftBlock)
    return rightBlock
}

fun assessBlockAdded() {
    TODO()
}

fun updateFeasibilityDayRemoved(solutionData: SolutionData, doctorID: Int, dayID: Int) {
    val days = solutionData.days
    val doctor = solutionData.doctors[doctorID]

    /*
     * Scenarios:
     * 1. Day is at end of block - remove day from block
     * 2. Day is last in block - remove Block
     * 3. Day is in middle of block - split into two blocks
     */
    val block = doctor.blocksOfDays[days[dayID].block[doctorID]] ?: throw Exception("updateFeasibilityDayRemoved: Day $dayID is not allocated to a block for doctor $doctorID, despite them working on it")
    when(val blocks = block.removeDay(dayID, doctor.nextBlockID)) {
        null -> if(block.days.size == 0)
                    doctor.blocksOfDays.remove(block.id)
        else -> {
            doctor.blocksOfDays[blocks.first.id] = blocks.first
            doctor.blocksOfDays[blocks.second.id] = blocks.second
            for(day in blocks.second.days)
                days[day].block[doctorID] = blocks.second.id
            doctor.nextBlockID++
        }
    }
}

fun assessBlockDayRemoved() {
    TODO()
}
package problemDomain.feasibilityHandling

import problemDomain.*

fun updateFeasibilityDayAdded(solutionData: SolutionData, doctorID: Int, dayID: Int) {
    val days = solutionData.days
    val doctor = solutionData.doctors[doctorID]

    val leftInBlock = dayID - 1 in days.indices && days[dayID - 1].block[doctorID] != null
    val rightInBlock = dayID + 1 in days.indices && days[dayID + 1].block[doctorID] != null

    // Decides the block that [dayID] should be added to
    val blockID = when {
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
    if(blockID == doctor.nextBlockID) {
        doctor.nextBlockID++
        doctor.blocksOfDays[blockID] = Block(blockID)
    }

    val block = doctor.blocksOfDays[blockID] ?: throw Exception("updateFeasibilityDaysWorked: Block $blockID missing from doctor $doctorID")

    // [dayID] is added to the identified block of [doctor]
    block.addDay(dayID)
    days[dayID].block[doctorID] = blockID

    // Assesses the impact that the added day has on feasibility
    addBlockInfeasibility(solutionData, doctor, block, dayID)
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

private fun addBlockInfeasibility(
    solutionData: SolutionData,
    doctor: MiddleGrade,
    block: Block,
    addedDay: Int
) {
    val infeasibilities = mutableMapOf<Source, List<Int>>()
    val firstDay = block.days.min()
    val lastDay = block.days.max()
    val addedIsFirst = firstDay == addedDay
    val blockSize = block.days.size

    //println(addedIsFirst)

    //println(firstDay)

    if(blockSize == 7) {
        val (shiftsMadeInfeasible, source) = assessRowOfSevenDaysInfeasibility(solutionData, doctor.id, block)
        infeasibilities[source] = shiftsMadeInfeasible
    }

    // Check to left:
    // Conditions are stored as booleans as they will be reused
    val twoToLeftInIndices = firstDay - 2 in solutionData.days.indices

    // If the block of shifts is six long, overlapping shifts two days prior are infeasible
    if(twoToLeftInIndices && blockSize == 6 &&
        solutionData.days[firstDay-2].overlappingNightShifts.isNotEmpty()) {
        val source = Source.RowOfSixOverlap(block.id)
        infeasibilities[source] = solutionData.days[firstDay-2].overlappingNightShifts.toList()
    }

    // True if there exists a block to the left of [block] with a gap of one day
    val twoToLeftWorked =
        twoToLeftInIndices && solutionData.days[firstDay-2].block.contains(doctor.id)
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

        /*
         * If block before gap of one day is 6 days long, there is insufficient rest for
         * the day prior to it to be worked.
         */
        if(prevBlockSize == 6 && addedIsFirst && prevBlock.days.min() - 1 in solutionData.days.indices) {
            val source = Source.InsufficientRest(Pair(prevBlockID, firstDay))
            infeasibilities[source] = solutionData.days[prevBlock.days.min()-1].getShifts()
        }

        /*
         * If the [prevBlock] is 5 days long, there is insufficient rest for an
         * overlapping night shift two days before that to be worked (would cause a
         * row of 7 days)
         */
        if(prevBlockSize == 5 && addedIsFirst && prevBlock.days.min() - 2 in solutionData.days.indices) {
            val source = Source.InsufficientRestOverlap(Pair(prevBlockID, firstDay))
            infeasibilities[source] = solutionData.days[prevBlock.days.min()-2].overlappingNightShifts.toList()
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

        /*
         * If the previous block is six long, there is insufficient rest for the day after
         * the block to be worked (would cause a row of 7, requiring 48 hours rest).
         * Only needs to be evaluated if the day we are considering is the first day of
         * [block]
         */
        if(prevBlock.days.size == 6 && addedIsFirst) {
            val source = Source.InsufficientRest(Pair(prevBlockID, firstDay))
            infeasibilities[source] = solutionData.days[firstDay-2].getShifts()
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
        if(prevBlock.days.size == 5 && addedIsFirst &&
            solutionData.days[firstDay-3].overlappingNightShifts.isNotEmpty()) {
            val source = Source.InsufficientRestOverlap(Pair(prevBlockID, firstDay))
            infeasibilities[source] = solutionData.days[firstDay-3].overlappingNightShifts.toList()
        }
    }


    // Check to right:
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

    val twoToRightInIndices = lastDay + 2 in solutionData.days.indices
    // True if there exists a block to the right of [block] with a gap of one day
    val twoToRightWorked =
        twoToRightInIndices && solutionData.days[lastDay+2].block.contains(doctor.id)

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

        /*
         * If [block] is 6 days long and there is a gap of one day followed by a worked day
         * ([twoToRightIsWorked]), then shifts of the day to the left of the block must
         * be infeasible as there cannot be 48 hours of rest after the hypothetical stretch
         * of seven days
         */
        if(blockSize == 6 && firstDay - 1 in solutionData.days.indices) {
            val source = Source.InsufficientRest(Pair(block.id, nextBlock.days.min()))
            infeasibilities[source] = solutionData.days[firstDay-1].getShifts()
        }

        /*
         * If the block is 5 days long, and [twoToRightIsWorked] == true, overlapping
         * night shifts of the day two days before the block need to be made infeasible
         * as there is no possible 48 hours of rest after the hypothetical row of 7 days
         */
        if(blockSize == 5 && twoToLeftInIndices) {
            val source = Source.InsufficientRestOverlap(Pair(block.id, nextBlock.days.min()))
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

        if(blockSize == 6) {
            val source = Source.InsufficientRest(Pair(block.id, nextBlock.days.min()))
            infeasibilities[source] = solutionData.days[lastDay+1].getShifts()
        }
    }

    val fourToRightWorked = threeToRightInIndices && lastDay + 4 in solutionData.days.indices
            && solutionData.days[lastDay+4].block.contains(doctor.id)
    if(!twoToRightWorked && !threeToRightWorked && fourToRightWorked) {
        val nextBlockID = solutionData.days[lastDay+4].block[doctor.id]!!
        val nextBlock = doctor.blocksOfDays[nextBlockID] ?: throw Exception("sdfsd")

        if(blockSize == 5 && solutionData.days[lastDay+1].overlappingNightShifts.isNotEmpty()) {
            val source = Source.InsufficientRestOverlap(Pair(block.id, nextBlock.days.min()))
            infeasibilities[source] = solutionData.days[lastDay+1].overlappingNightShifts.toList()
        }
    }

    // Make relevant shifts infeasible
    for((source, shifts) in infeasibilities) {
        shifts.forEach { solutionData.shifts[it].restInfeasibility(doctor.id, source) }

        when(source) {
            is Source.RowOfSevenDays ->
                doctor.blocksOfDays[source.block]!!.shiftsMadeInfeasible.addAll(shifts)
            is Source.InsufficientRest -> {
                doctor.blocksOfDays[source.blockAndDay.first]!!.shiftsMadeInfeasible.addAll(shifts)
                solutionData.days[source.blockAndDay.second].addInfeasibleShifts(doctor.id, shifts)
            }
            is Source.InsufficientRestOverlap -> {
                doctor.blocksOfDays[source.blockAndDay.first]!!.shiftsMadeInfeasible.addAll(shifts)
                solutionData.days[source.blockAndDay.second].addInfeasibleShifts(doctor.id, shifts)
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
            is Source.WeekendWorked -> TODO() // Unsure if this should be done here or not
            // These sources should not be created by this function
            is Source.RowOfNights -> throw Exception("addBlockInfeasibility: Row of nights infeasibility created")
            is Source.ShiftWorked-> throw Exception("addBlockInfeasibility: Shift worked infeasibility created")
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

fun updateFeasibilityDayRemoved(solutionData: SolutionData, doctorID: Int, dayID: Int) {
    val days = solutionData.days
    val doctor = solutionData.doctors[doctorID]
    val block = doctor.blocksOfDays[days[dayID].block[doctorID]] ?: throw Exception("updateFeasibilityDayRemoved: Day $dayID is not allocated to a block for doctor $doctorID, despite them working on it")

    /*
     * Previous sources of infeasibility for the block are removed; the impact of the
     * block(s) on feasibility regarding days worked, will be reassessed after [dayID]
     * is removed
     */
     removeBlockInfeasibility(solutionData, doctor, block)

    /*
     * Scenarios:
     * 1. Day is at end of block - remove day from block
     * 2. Day is last in block - remove Block
     * 3. Day is in middle of block - split into two blocks
     * In all scenarios the day is removed from the block - this is all that happens
     * for scenario 1
     */
    when(val blocks = block.removeDay(dayID, doctor.nextBlockID)) {
        // Scenario 2:
        null -> if(block.days.size == 0)
                    doctor.blocksOfDays.remove(block.id)
        // Scenario 3:
        else -> {
            doctor.blocksOfDays[blocks.first.id] = blocks.first
            doctor.blocksOfDays[blocks.second.id] = blocks.second
            for(day in blocks.second.days)
                days[day].block[doctorID] = blocks.second.id
            doctor.nextBlockID++
        }
    }
    // Reference to the block is removed from the day that is no longer worked
    days[dayID].block.remove(doctorID)
}

private fun removeBlockInfeasibility(solutionData: SolutionData, doctor: MiddleGrade, block: Block) {
    val (prevInfeasibleShifts, source) = when {
        block.days.size == 7 -> assessRowOfSevenDaysInfeasibility(solutionData, doctor.id, block)
        else -> Pair(emptyList(), Source.RowOfNights(emptySet()))
    }

    for(shift in prevInfeasibleShifts)
        solutionData.shifts[shift].removeSource(doctor.id, source)
}

fun assessBlockDayRemoved() {
    TODO()
}
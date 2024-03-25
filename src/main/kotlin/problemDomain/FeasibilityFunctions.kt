package problemDomain

import problemDomain.feasibilityHandling.*

data class FeasibilityInfo(val shift: Shift, val doctor: Int)

fun updateFeasibilityAllocation(solutionData: SolutionData, info: FeasibilityInfo) {
    val shift = info.shift
    val doctor = info.doctor
    shift.feasibleDoctors.remove(doctor)

    // Different actions are performed depending on the type of shift
    when(shift) {
        is DayShift -> {
            // Makes relevant shifts infeasible
            for(shiftID in shift.nightShifts48HoursBefore.union(shift.shiftsWithin11Hours))
                solutionData.shifts[shiftID].restInfeasibility(doctor, Source.ShiftWorked(shift.id))
        }
        // Makes relevant shifts infeasible and handles other night-related infeasibility
        is NightShift ->
            updateFeasibilityNightShift(solutionData, shift, doctor, allocate = true)
    }

    // Adds the shift as a source of the doctor having worked on that day
    val day = solutionData.days[shift.day]
    day.addWorkingDoctor(doctor, shift.id)
    // True if the shift added is the only one worked by the doctor on that day
    val onlyShiftWorked = day.doctorsWorkingDay[doctor]!!.size == 1

    /*
     * Checks that the next day is in indices, and if the shift assigned overlaps into
     * that day, if both are true, the doctor and shift are added to [doctorsWorkingDay]
     * of the next day.
     */
    val nextDayID = day.id + 1
    val shiftWorkedOverlaps = shift is NightShift && shift.overlaps &&
                                    nextDayID in solutionData.days.indices
    if(shiftWorkedOverlaps)
        solutionData.days[nextDayID].addWorkingDoctor(doctor, shift.id)

    /*
     * True if the shift worked overlaps into the next day, and it is the only shift
     * worked on that day
    */
    val overlapOnlyShiftWorkedNextDay = shiftWorkedOverlaps &&
            solutionData.days[nextDayID].doctorsWorkingDay[doctor]!!.size == 1

    // Relevant function(s) called, depending on which days (if any) are newly worked
    when {
        onlyShiftWorked && overlapOnlyShiftWorkedNextDay -> {
            updateFeasibilityDayAdded(solutionData, doctor, day.id)
            updateFeasibilityDayAdded(solutionData, doctor, nextDayID)
        }
        onlyShiftWorked -> updateFeasibilityDayAdded(solutionData, doctor, day.id)
        overlapOnlyShiftWorkedNextDay -> updateFeasibilityDayAdded(solutionData, doctor, nextDayID)
    }

    weekendFeasibility(solutionData, doctor, shift, allocate = true)

    return
}

fun updateFeasibilityDeallocation(solutionData: SolutionData, info: FeasibilityInfo) {
    val shift = info.shift
    val doctor = info.doctor
    shift.feasibleDoctors.add(info.doctor)

    // Different actions are performed depending on the type of shift
    when(shift) {
        is DayShift -> {
            // Removes infeasibility from relevant shifts
            for(shiftID in shift.nightShifts48HoursBefore.union(shift.shiftsWithin11Hours))
                solutionData.shifts[shiftID].removeSource(
                            info.doctor, Source.ShiftWorked(shift.id))
        }
        // Removes infeasibility from relevant shifts and handles other night-related actions
        is NightShift ->
            updateFeasibilityNightShift(solutionData, shift, info.doctor, allocate = false)
    }

    val day = solutionData.days[shift.day]
    val dayNoLongerWorked = day.doctorsWorkingDay[doctor]!!.size == 1

    val nextDayID = day.id + 1
    val removedShiftOverlaps = shift is NightShift && shift.overlaps &&
                                nextDayID in solutionData.days.indices
    val nextDayNoLongerWorked = removedShiftOverlaps &&
            solutionData.days[nextDayID].doctorsWorkingDay[doctor]!!.size == 1

    when {
        dayNoLongerWorked && nextDayNoLongerWorked -> {
            updateFeasibilityDayRemoved(solutionData, doctor, nextDayID)
            updateFeasibilityDayRemoved(solutionData, doctor, day.id)
        }
        dayNoLongerWorked -> updateFeasibilityDayRemoved(solutionData, doctor, day.id)
        nextDayNoLongerWorked -> updateFeasibilityDayRemoved(solutionData, doctor, nextDayID)
    }

    day.removeWorkingDoctor(doctor, shift.id)
    if(removedShiftOverlaps)
        solutionData.days[nextDayID].removeWorkingDoctor(doctor, shift.id)

    weekendFeasibility(solutionData, doctor, shift, allocate = false)

    return
}

// Handles the impact of weekend shifts (Midnight on Friday to Midnight on Sunday) on feasibility
private fun weekendFeasibility(
    solutionData: SolutionData,
    doctorID: Int,
    shift: Shift,
    allocate: Boolean
) {
    val dayID = shift.day
    val addedValues = when {
        // Day is a Sunday
        (dayID + 1) % 7 == 0 -> listOf(6, 7, 13, 14, -7, -8, -14, -15)
        // Day is Saturday
        (dayID + 2) % 7 == 0 -> listOf(7, 8, 14, 15, -6, -7, -13, -14)
        // Day is a Friday and shift is an overlapping night shift
        (dayID + 3) % 7 == 0 && shift is NightShift && shift.overlaps ->
            listOf(8, 9, 15, 16, -5, -6, -12, -13)
        // Day is not relevant to weekend feasibility
        else -> return
    }

    // Calculates weekendDays that are impacted by the allocated/deallocated shift
    val relevantWeekendDays = addedValues.map { it + dayID }
        .filter { it in solutionData.days.indices }

    val relevantShifts = mutableListOf<Int>()
    for(weekendDay in relevantWeekendDays) {
        val day = solutionData.days[weekendDay]
        relevantShifts.addAll(day.getShifts())

        /*
         * If [weekendDay] is a Saturday, we add any overlapping night shifts from the
         * Friday before it
         */
        if((day.id + 2) % 7 == 0)
            relevantShifts.addAll(solutionData.days[day.id-1].overlappingNightShifts)
    }

    // Adds or removes sources as required
    val feasibilityAction = when(allocate) {
        true -> { s: Shift -> s.restInfeasibility(doctorID, Source.WeekendWorked(shift.id)) }
        false -> { s: Shift -> s.removeSource(doctorID, Source.WeekendWorked(shift.id)) }
    }
    for(shiftID in relevantShifts)
        feasibilityAction(solutionData.shifts[shiftID])
}
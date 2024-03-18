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
        is NightShift -> {
            updateFeasibilityNightShift(
                solutionData, shift, doctor, allocate = true)
        }
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

    return
    // Weekend Feasibility
    TODO()
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
        is NightShift -> updateFeasibilityNightShift(
                            solutionData, shift, info.doctor, allocate = false)
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

    return
    // Both types of shifts impact the days worked by the doctor
    TODO()
}
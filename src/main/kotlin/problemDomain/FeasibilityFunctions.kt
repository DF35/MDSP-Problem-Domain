package problemDomain

import problemDomain.feasibilityHandling.*

data class FeasibilityInfo(val shift: Shift, val doctor: Int)

fun updateFeasibilityAllocation(solutionData: SolutionData, info: FeasibilityInfo) {
    val shift = info.shift
    shift.feasibleDoctors.remove(info.doctor)

    // Different actions are performed depending on the type of shift
    when(shift) {
        is DayShift -> {
            // Makes relevant shifts infeasible
            for(shiftID in shift.nightShifts48HoursBefore.union(shift.shiftsWithin11Hours))
                solutionData.shifts[shiftID].restInfeasibility(
                            info.doctor, Source.ShiftWorked(shift.id))
        }
        // Makes relevant shifts infeasible and handles other night-related infeasibility
        is NightShift -> {
            updateFeasibilityNightShift(
                solutionData, shift, info.doctor, allocate = true)
        }
    }

    return
    TODO()

}

fun updateFeasibilityDeallocation(solutionData: SolutionData, info: FeasibilityInfo) {
    val shift = info.shift
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

    return
    // Both types of shifts impact the days worked by the doctor
    TODO()
}
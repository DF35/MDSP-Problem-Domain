package problemDomain

import java.util.*
import kotlin.Exception

data class SolutionData(
    val assignments: List<Assignment>,
    val shifts: List<Shift>,
    val days: List<Day>,
    val doctors: List<MiddleGrade>
)

// Represents a single solution - interface for the problem domain
class Solution(
    val rand: Random,
    val assignments: List<Assignment>,
    val shifts: List<Shift>,
    val days: List<Day>,
    val doctors: List<MiddleGrade>,
    val averageHours: Int,
    val averageNumDayShifts: Int,
    val averageNumNightShifts: Int
) {
    // Tracks unassigned assignments for use in the objective function and heuristics
    var unassignedAssignments = mutableListOf<Int>()
    var assignedAssignments = mutableListOf<Int>()
    var objectiveValue = Double.MAX_VALUE
    var iteration = 0

    fun copy(): Solution {
        val assignments = mutableListOf<Assignment>()
        val shifts = mutableListOf<Shift>()
        val days = mutableListOf<Day>()
        val doctors = mutableListOf<MiddleGrade>()
        this.assignments.forEach { assignments.add(it.copy()) }
        this.shifts.forEach { shifts.add(it.copy()) }
        this.days.forEach { days.add(it.copy()) }
        this.doctors.forEach { doctors.add(it.copy()) }

        val solution = Solution(rand, assignments, shifts, days, doctors, averageHours, averageNumDayShifts,
                            averageNumNightShifts)
        solution.unassignedAssignments = unassignedAssignments.toMutableList()
        solution.assignedAssignments = assignedAssignments.toMutableList()
        solution.objectiveValue = objectiveValue
        solution.iteration = iteration
        return solution
    }

    fun debug() {
        assignments.forEach { it.debug() }
        shifts.forEach { it.debug() }
        days.forEach { it.debug() }
        println("\n\n\n\n")
    }

    // Generates an initial solution - has stochastic elements
    fun initialise() {
        val doctorsNeedingNightShifts = doctors.indices.filter { doctors[it].targetNightShifts > 0 }.toMutableList()
        val doctorsNeedingDayShifts = doctors.indices.filter { doctors[it].targetDayShifts > 0 }.toMutableList()
        // Sorts according to number of infeasible doctors (idea being that they will be the most difficult to assign)
        val sortedShifts = shifts.indices.filter { shifts[it].causesOfInfeasibility.isNotEmpty() }.sortedBy { shifts[it].feasibleDoctors.size }

        for(shiftID in sortedShifts) {
            val shift = shifts[shiftID]
            val assignmentId = shift.assignmentIDs[0]
            val feasibleDoctors = shift.feasibleDoctors.subtract(assignments[assignmentId].infeasibleDoctors).toList()
            if(feasibleDoctors.isEmpty()) continue
            val doctor = feasibleDoctors[rand.nextInt(feasibleDoctors.size)]
            allocateAssignment(assignmentId, doctor)
            when(shift) {
                is DayShift -> if(doctors[doctor].varianceDayShiftsWorked() == 0)
                                    doctorsNeedingDayShifts.remove(doctor)
                is NightShift -> if(doctors[doctor].varianceNightShiftsWorked() == 0)
                                    doctorsNeedingNightShifts.remove(doctor)
            }
        }

        val remainingShifts = shifts.filterNot { sortedShifts.contains(it.id) }.partition { it is DayShift }
        val remainingDayShifts = remainingShifts.first.toMutableList()
        val remainingNightShifts = remainingShifts.second.toMutableList()

        /*
         * Assigns random night shifts until either none are unassigned, or all the doctors
         * have had their required number of night shifts met
         */
        while(doctorsNeedingNightShifts.isNotEmpty() && remainingNightShifts.isNotEmpty()) {
            val shift = remainingNightShifts[rand.nextInt(remainingNightShifts.size)]
            val assignmentId = shift.assignmentIDs[0]
            val feasibleDoctors = shift.feasibleDoctors.subtract(assignments[assignmentId].infeasibleDoctors)
            val doctor = feasibleDoctors.firstOrNull { doctorsNeedingNightShifts.contains(it) }

            if(doctor == null) {
                remainingNightShifts.remove(shift)
                continue
            }

            allocateAssignment(assignmentId, doctor)
            if(doctors[doctor].varianceNightShiftsWorked() == 0)
                doctorsNeedingNightShifts.remove(doctor)
            remainingNightShifts.remove(shift)
        }

        /*
         * Assigns random day shifts until either none are unassigned, or all the doctors
         * have had their required number of day shifts met
        */
        while(doctorsNeedingDayShifts.isNotEmpty() && remainingDayShifts.isNotEmpty()) {
            val shift = remainingDayShifts[rand.nextInt(remainingDayShifts.size)]
            val assignmentId = shift.assignmentIDs[0]
            val feasibleDoctors = shift.feasibleDoctors.subtract(assignments[assignmentId].infeasibleDoctors)
            val doctor = feasibleDoctors.firstOrNull { doctorsNeedingDayShifts.contains(it) }

            if(doctor == null) {
                remainingDayShifts.remove(shift)
                continue
            }

            allocateAssignment(assignmentId, doctor)
            if(doctors[doctor].varianceDayShiftsWorked() == 0)
                doctorsNeedingDayShifts.remove(doctor)
            remainingDayShifts.remove(shift)
        }
}

    fun calculateObjectiveValue() {
        // 10 is added for each assignment without an assignee
        var value = unassignedAssignments.size.toDouble() * 10

        for(day in days) {
            // Penalises days that are entirely locum dependent
            if(day.numShiftsWithCoverage == 0)
                value += 20

            // Penalises shifts that are totally locum dependent
            val numLocumDependentShifts = day.getShifts().size - day.numShiftsWithCoverage
            value += 15 * numLocumDependentShifts
        }

        // Impact of each doctor is added to the objective value
        for(doctor in doctors)
            value += calculateDoctorContribution(doctor)

        objectiveValue = value
    }

    // Calculates the impact of a single doctor on the objective function value
    private fun calculateDoctorContribution(doctor: MiddleGrade): Double {
        var contribution = 0.00
        val varianceAverageWorked = doctor.varianceHoursWorked()
        contribution += when (varianceAverageWorked < 0) {
            false -> varianceAverageWorked * 10
            true -> -varianceAverageWorked * 20
        }
        val varianceShifts = listOf(doctor.varianceDayShiftsWorked(), doctor.varianceNightShiftsWorked())
        for(variance in varianceShifts) {
            contribution += when (variance < 0) {
                false -> variance * 10
                true -> -variance * 12
            }
        }
        return contribution
    }

    // Allocates indicated [assignment] to a given doctor and updates the feasibility of relevant shifts
    fun allocateAssignment(assignment: Int, doctor: Int): Boolean {
        val shiftID = assignments[assignment].shift
        val shift = shifts[shiftID]
        //Doctors may be infeasible for an assignment but not a shift
        val feasibleDoctors = shift.feasibleDoctors.subtract(assignments[assignment].infeasibleDoctors)

        if(!feasibleDoctors.contains(doctor)) return false

        // Subtracts previous objective value contribution of the doctor
        objectiveValue -= calculateDoctorContribution(doctors[doctor])

        // Updates feasibility and assignee/assignment information
        shift.feasibleDoctors.remove(doctor)
        shift.assignees.add(doctor)
        assignments[assignment].assign(doctor)
        unassignedAssignments.remove(assignment)
        assignedAssignments.add(assignment)
        updateFeasibility(shiftID, doctor, allocate = true)

        // Updates data of the doctor allocated to the shift
        val doc = doctors[doctor]
        doc.hoursWorked += shift.duration
        when(shift) {
            is DayShift -> doc.dayShiftsWorked++
            is NightShift -> doc.nighShiftsWorked++
        }
        doc.assignedAssignments.add(assignment)

        // Updates Objective Function value using delta evaluation
        objectiveValue -= 10 // One less unassigned assignment
        // Adds new objective contribution value of the doctor
        objectiveValue += calculateDoctorContribution(doctors[doctor])
        // Checks if the shift was previously without assignees, if so, does the same for the day
        if(shift.assignees.size == 1) {
            objectiveValue -= 15
            days[shift.day].numShiftsWithCoverage++
            if(days[shift.day].numShiftsWithCoverage == 1)
                objectiveValue -= 20
        }


        assignments[assignment].iterationAssigned = iteration
        iteration += 1

        doc.assignmentLog += "al $assignment $doctor\n"

        for(testShift in shifts) {
            for(assignee in testShift.assignees)
                if(testShift.causesOfInfeasibility[assignee] != null)
                    println("Bad news")
            for(assignmentID in testShift.assignmentIDs) {
                val a = assignments[assignmentID].assignee
                if(a != null && !testShift.assignees.contains(a))
                    println("Dreadful")
            }
        }

        return true
    }

    // Takes an [assignment] and deallocates it (if feasible), updating the feasibility of relevant shifts
    fun deallocateAssignment(assignment: Int): Boolean {
        val (doctor, shiftID) = assignments[assignment].unAssign() ?: throw Exception ("deallocateAssignment: No assignee")

        /* If the shift is a night shift, we need to check that it can be removed without creating an
         * infeasible timetable - by removing a night that is neither at the end of a block nor solitary, it would
         * be possible to end up in a situation in which there is not enough rest after a night. e.g. if the
         * second night of a four night block is removed, the first night should then have 48 hours of rest after
         * it, but this would not be the case as the third night would still be allocated.
         */
        val shift = shifts[shiftID]
        if(shift is NightShift) {
            var inMiddleOfBlock = true
            for(dayID in listOf(shift.day - 1, shift.day + 1).filter { it in days.indices })
                inMiddleOfBlock = inMiddleOfBlock && days[dayID].doctorsWorkingNight.contains(doctor)
            if(inMiddleOfBlock) {
                assignments[assignment].assign(doctor)
                return false
            }
        }

        // Subtracts previous objective value contribution of the doctor
        objectiveValue -= calculateDoctorContribution(doctors[doctor])

        // Updates feasibility and assignee/assignment information
        shift.feasibleDoctors.add(doctor)
        shift.assignees.remove(doctor)
        assignedAssignments.remove(assignment)
        unassignedAssignments.add(assignment)
        updateFeasibility(shiftID, doctor, allocate = false)

        // Updates data for doctor removed from the shift
        val doc = doctors[doctor]
        doc.hoursWorked -= shift.duration
        when(shift) {
            is DayShift -> doc.dayShiftsWorked--
            is NightShift -> doc.nighShiftsWorked--
        }
        doc.assignedAssignments.remove(assignment)

        // Updates Objective Function value using delta evaluation
        objectiveValue += 10 // One extra unassigned assignment
        // Adds new objective contribution value of the doctor
        objectiveValue += calculateDoctorContribution(doctors[doctor])
        // Checks if the shift is without assignees, if so also checks the day
        if(shift.assignees.isEmpty()) {
            objectiveValue += 15
            days[shift.day].numShiftsWithCoverage--
            if(days[shift.day].numShiftsWithCoverage == 0)
                objectiveValue += 20
        }

        doc.assignmentLog += "de $assignment\n"

        for(testShift in shifts) {
            for(assignee in testShift.assignees)
                if(testShift.causesOfInfeasibility[assignee] != null)
                    println("Awful news")
            for(assignmentID in testShift.assignmentIDs) {
                val a = assignments[assignmentID].assignee
                if (a != null && !testShift.assignees.contains(a))
                    println("God has cursed me")
            }
        }

        return true
    }

    // Returns the set of feasible doctors for a given shift
    fun getFeasibleDoctors(assignmentID: Int): Set<Int> {
        if(assignmentID !in assignments.indices)
            throw Exception("getFeasibleDoctors: invalid assignmentID given")
        val assignment = assignments[assignmentID]
        return shifts[assignment.shift].feasibleDoctors.subtract(assignment.infeasibleDoctors)
    }

    /*
     * Updates the feasibility of shifts for a given [doctor] after they are allocated ([allocate] = true) or
     * deallocated a given shift ([shiftID])
     */
    private fun updateFeasibility(shiftID: Int, doctor: Int, allocate: Boolean) {
        val shift = shifts[shiftID]
        when(shift) {
            is DayShift -> updateFeasibilityDayShift(shift, doctor, allocate)
            is NightShift -> updateFeasibilityNightShift(shift, doctor, allocate)
        }
        updateFeasibilityDaysWorked(doctor, shift.day, allocate)
    }

    // Updates the feasibility of relevant shifts after a day shift is allocated or deallocated
    private fun updateFeasibilityDayShift(shift: DayShift, doctor: Int, allocate: Boolean) {
        // The action applied to each shift depends on whether [shift] has been allocated or deallocated
        when (allocate) {
            // Shifts are made infeasible as a result of the allocation
            true -> for (shiftID in shift.nightShifts48HoursBefore.union(shift.shiftsWithin11Hours))
                        shifts[shiftID].restInfeasibility(doctor, Source.ShiftWorked(shift.id))
            // [shift] will no longer be a source of infeasibility for the relevant shifts
            false -> for (shiftID in shift.nightShifts48HoursBefore.union(shift.shiftsWithin11Hours))
                        shifts[shiftID].removeSource(doctor, Source.ShiftWorked(shift.id))
        }
    }

    // Updates the feasibility of relevant shifts after a day shift is allocated or deallocated
    private fun updateFeasibilityNightShift(shift: NightShift, doctor: Int, allocate: Boolean) {
        /*
         * The conditions for adding or removing feasibility are contingent on the same boolean checks,
         * the actions are merely reversed, depending on the value of [allocate]; defining two lambda
         * expressions allows the conditional code to be reused as the correct action can be assigned as required
         */
        val feasibilityAction1: (Shift, Int) -> Unit
        val feasibilityAction2: (Shift, Int) -> Unit

        // The action applied to each shift depends on whether [shift] has been allocated or deallocated
        when(allocate) {
            true -> {
                // Shifts are made infeasible as a result of the allocation
                for(shiftID in shift.dayShifts48HoursAfter.union(shift.shiftsWithin11Hours))
                    shifts[shiftID].restInfeasibility(doctor, Source.ShiftWorked(shift.id))

                if(shift.overlaps && shift.day + 1 in days.indices)
                    days[shift.day + 1].addWorkingDoctor(doctor)

                days[shift.day].doctorsWorkingNight.add(doctor)
                // Feasibility of relevant night shifts is assessed and updated accordingly
                consecutiveDayNightCheck(doctor, shift.day, 4, ::doctorWorksNight, ::makeNightInfeasible)

                feasibilityAction1 = { s: Shift, workedID:Int -> s.removeSource(doctor, Source.ShiftWorked(workedID)) }
                feasibilityAction2 = { s: Shift, id: Int -> s.restInfeasibility(doctor, Source.ShiftWorked(id)) }

            }
            false -> {
                // [shift] will no longer be a source of infeasibility for the relevant shifts
                for(shiftID in shift.dayShifts48HoursAfter.union(shift.shiftsWithin11Hours))
                    shifts[shiftID].removeSource(doctor, Source.ShiftWorked(shift.id))

                if(shift.overlaps && shift.day + 1 in days.indices)
                    days[shift.day + 1].removeWorkingDoctor(doctor)

                days[shift.day].doctorsWorkingNight.remove(doctor)
                removeNightRowInfeasibility(shift, doctor)

                feasibilityAction1 = { s: Shift, workedID:Int -> s.restInfeasibility(doctor, Source.ShiftWorked(workedID)) }
                feasibilityAction2 = { s: Shift, id: Int -> s.removeSource(doctor, Source.ShiftWorked(id)) }
            }
        }

        // Calculates the days that need to be checked
        data class Quadruple(val first: Int, val second: Int, val third: Int, val fourth: Int)
        val calcDays = { i: Int -> Quadruple(i - 1, i - 2, i + 1, i + 2) }
        val (prevDay, priorDay, nextDay, subsequentDay) = calcDays(shift.day)

        // Boolean conditions for the feasibility actions to be called
        val prevInIndices = prevDay in days.indices
        val workingPrevNight = if(prevInIndices) days[prevDay].doctorsWorkingNight.contains(doctor) else false
        val priorInIndices = priorDay in days.indices
        val nextInIndices = nextDay in days.indices
        val workingNextNight = if(nextInIndices) days[nextDay].doctorsWorkingNight.contains(doctor) else false
        val subInIndices = subsequentDay in days.indices

        if(prevInIndices && workingPrevNight && nextInIndices) {
            val workedShift = days[prevDay].nightShifts.first { shifts[it].assignees.contains(doctor) }
            for (shiftID in days[nextDay].nightShifts)
                feasibilityAction1(shifts[shiftID], workedShift)
        }

        if(subInIndices && !workingNextNight)
            for(shiftID in days[subsequentDay].nightShifts)
                feasibilityAction2(shifts[shiftID], shift.id)

        if(nextInIndices && workingNextNight && prevInIndices) {
            val workedShift = days[nextDay].nightShifts.first { shifts[it].assignees.contains(doctor) }
            for(shiftID in days[prevDay].nightShifts)
                feasibilityAction1(shifts[shiftID], workedShift)
        }

        if(priorInIndices && !workingPrevNight)
            for(shiftID in days[priorDay].nightShifts)
                feasibilityAction2(shifts[shiftID], shift.id)
    }

    /*
     * If a night contributed to an infeasibility stemming from 4 nights in a row, this
     * infeasibility can simply be removed
     */
    private fun removeNightRowInfeasibility(shift: NightShift, doctor: Int) {
        // The infeasibility of other nights that was contributed to by [shift] is assessed and updated accordingly
        val toRemoveMap = mutableMapOf<Int, Set<Int>>()
        for(dayID in days[shift.day].toCheckNight[doctor] ?: return) {
            val causes = days[dayID].causesOfNightInfeasibility[doctor]?.filter { it.sources.contains(shift.day) }

            val toRemove = mutableSetOf<Int>()
            for(cause in causes ?: throw Exception("removeNightRowInfeasibility: Day $dayID is missing infeasibility for $doctor")) {
                /*
                 * Nights that contributed to this night's infeasibility need to have their
                 * reference to it in [toCheck] removed
                 */
                toRemove.addAll(cause.sources)
                /*
                 * True if all causes of infeasibility for the night have been removed
                 * - shifts can be made feasible again
                 */
                if(days[dayID].removeNightInfeasibility(doctor, cause))
                    for(shiftID in days[dayID].nightShifts) shifts[shiftID].removeSource(doctor, Source.NightsWorked)
            }
            toRemoveMap[dayID] = toRemove
        }

        /*
         * Nights that are no longer infeasible have their references removed from [toCheck]
         * of nights that previously contributed to their infeasibility
         */
        for(day in toRemoveMap)
            for(dayX in day.value) days[dayX].removeToCheckNight(doctor, day.key)
    }

    private fun removeOverlappingNightInfeasibility(dayID: Int, doctor: Int) {
        val toRemoveMap = mutableMapOf<Int, Set<Int>>()
        for(day in days[dayID].toCheckOverlapping[doctor] ?: return) {
            val causes = days[day].causesOfOverlappingInfeasibility[doctor]?.filter { it.sources.contains(dayID) }

            val toRemove = mutableSetOf<Int>()
            for(cause in causes ?: throw Exception("removeOverlappingNightInfeasibility: Day $dayID is missing infeasibility for $doctor")) {
                toRemove.addAll(cause.sources)

                if(days[day].removeOverlappingInfeasibility(doctor, cause))
                    for(shift in days[day].overlappingNightShifts) shifts[shift].removeSource(doctor, Source.OverlappingNight)
            }
            toRemoveMap[day] = toRemove
        }
        for(day in toRemoveMap)
            for(dayX in day.value) days[dayX].removeToCheckOverlapping(doctor, day.key)
    }

    /*
     * Ensures the adherence to labour laws regarding stretches of days worked and weekends worked, is called whenever
     * a shift is allocated, regardless of it being a day shift or night shift
     * [doctor]: ID of the doctor that has been allocated or deallocated an assignment
     * [day]: ID of the day on which the assignment has been allocated or deallocated
     * [allocate]: true if the assignment was allocated, false if it was deallocated
     */
    private fun updateFeasibilityDaysWorked(doctor: Int, day: Int, allocate: Boolean) {
        weekendFeasibility(doctor, day, allocate)
        when(allocate) {
            true -> {
                days[day].addWorkingDoctor(doctor)
                dayFeasibility(doctor, day)
            }
            false -> {
                days[day].removeWorkingDoctor(doctor)
                if(!days[day].doctorsWorkingDay.containsKey(doctor)) {
                    checkRelatedDayInfeasibility(doctor, day)
                    removeOverlappingNightInfeasibility(day, doctor)
                }
            }
        }
    }

    /*
     * Called after a day has been assigned, in order to assess its impact on the feasibility
     * of shifts with regard to the number of days worked
     * [doctor]: ID of the doctor working on the day
     * [day]: ID of the day worked
     */
    private fun dayFeasibility(doctor: Int, day: Int) {
        // Returns the block of days that the assigned day is now a part of
        val blockOfDays = consecutiveDayNightCheck(doctor, day, 7, ::doctorWorksDay, ::makeDayInfeasible)

        /*
         * These values will be fed into functions which start at the next available index: we know that these days
         * are not worked because consecutiveDayNightCheck stops once it reaches an unassigned night
         */
        val rightStart = blockOfDays.max() + 1
        val leftStart = blockOfDays.min() - 1

        /*
         * If the block is 6 days long, an overlapping night shift could cause a row of 8
         * days, and therefore needs to be made infeasible, on either side of the block
         */
        val sixLong = blockOfDays.size == 6
        val ovAfter = rightStart in days.indices && days[rightStart].overlappingNightShifts.isNotEmpty()
        if(sixLong && ovAfter) {
            makeOverlappingNightInfeasible(rightStart, doctor,
                DayNightInfeasibility.WouldCauseRowTooLarge(blockOfDays))
        }
        val prevOv = leftStart - 1
        val ovBefore = prevOv in days.indices && days[prevOv].overlappingNightShifts.isNotEmpty()
        if(sixLong && ovBefore) {
            makeOverlappingNightInfeasible(prevOv, doctor,
                DayNightInfeasibility.WouldCauseRowTooLarge(blockOfDays))
        }

        /*
         * If the block is five days long, with a gap of three unassigned days, but the
         * first unassigned day has overlapping nights, those need to be made unfeasible
         * (would create a row of 7 without the requisite rest)
         */
        val fiveLong = blockOfDays.size == 5
        val relevantDayAfter = rightStart + 3
        val assignedAfter = relevantDayAfter in days.indices && doctorWorksDay(relevantDayAfter, doctor)
        if(fiveLong && assignedAfter && ovAfter) {
            makeOverlappingNightInfeasible(rightStart, doctor,
                DayNightInfeasibility.InsufficientRest(blockOfDays + relevantDayAfter))
        }

        // true if the day after [rightStart] is worked, false if not
        val secondDayWorked = checkRightBlock(doctor, rightStart, blockOfDays)
        /*
        * If the day after [rightStart] is worked and the block 6 days long, working on the night [leftStart] becomes
        * infeasible as it would cause a stretch of 7 nights without the requisite 48 hours rest
        */
        if(secondDayWorked && blockOfDays.size == 6 && blockOfDays.min() > 0) {
            makeDayInfeasible(
                leftStart, doctor,
                DayNightInfeasibility.InsufficientRest(blockOfDays + (rightStart + 2))
            )
        }

        /*
         * If there is an overlapping night two days before the block of 5 days and the
         * second shift after the block is worked, the overlapping shifts need to be made
         * infeasible
         */
        if(fiveLong && secondDayWorked && ovBefore) {
            makeOverlappingNightInfeasible(prevOv, doctor,
                DayNightInfeasibility.InsufficientRest(blockOfDays + (rightStart + 1)))
        }

        checkLeftBlock(doctor, leftStart, blockOfDays, secondDayWorked)
    }

    /* Called after a doctor is removed from working on a day, assesses and updates the
     * feasibility of relevant days
     * [doctor]: ID of the doctor removed from working an assignment on the day
     * [day]: ID of the day to be checked
     */
    private fun checkRelatedDayInfeasibility(doctor: Int, dayID: Int) {
        fun processDay(day: Int): Set<Int> {
            // [causes] = causes of infeasibility that need to be removed
            val (keepInToCheck, causes) = calculateToKeep(doctor, day, dayID)
            // days that no longer need to have [day] in their toCheck
            val toRemove: MutableSet<Int> = mutableSetOf()
            for(cause in causes) {
                // If a cause of infeasibility is removed, its sources no longer need to have [day] in toCheck
                toRemove.addAll(cause.sources)
                /*
                 * If a day becomes feasible as a result of the de-allocation, shifts have
                 * their infeasibility removed
                 */
                if(days[day].removeInfeasibility(doctor, cause))
                    for(shift in days[day].getShifts())
                        shifts[shift].removeSource(doctor, Source.DaysWorked)
            }

            /*
             * Some sources of removed causes might also be sources for causes of infeasibility
             * for [day] that are still valid: they need to be left out of toRemove
             */
            toRemove.removeAll(keepInToCheck)
            return toRemove
        }

        // Could be the case that the day was not part of any further infeasibility for the given doctor
        val toCheck = days[dayID].toCheck[doctor] ?: return
        val toRemoveMap: MutableMap<Int, Set<Int>> = mutableMapOf()

        // Finds the set of days that no longer need to hold reference to [day] in their toCheck
        for(day in toCheck)
            toRemoveMap[day] = processDay(day)

        // Removes toCheck entries that are no longer needed - done this way in order to avoid concurrent access exception
        for(day in toRemoveMap)
            for(dayX in day.value) days[dayX].removeToCheck(doctor, day.key)
    }

    // Used as input for other functions, to allow for other code to only be written once
    private fun doctorWorksNight(day: Int, doctor: Int): Boolean {
        return days[day].doctorsWorkingNight.contains(doctor)
    }
    private fun doctorWorksDay(day: Int, doctor: Int): Boolean {
        return days[day].doctorsWorkingDay.containsKey(doctor)
    }

    // Used to allow the use of the same code for checking different "directions"
    private val increment = { i: Int -> i + 1 }
    private val decrement = { i: Int -> i - 1 }

    // Used to allow the reuse of code for checking the feasibility of days and nights
    private fun makeDayInfeasible(dayID: Int, doctor: Int, infeasibility: DayNightInfeasibility) {
        for(shift in days[dayID].getShifts())
            shifts[shift].restInfeasibility(doctor, Source.DaysWorked)
        days[dayID].addInfeasibility(doctor, infeasibility)
        for(source in infeasibility.sources)
            days[source].addToCheck(doctor, dayID)
    }
    private fun makeNightInfeasible(dayID: Int, doctor: Int, infeasibility: DayNightInfeasibility) {
        for(shift in days[dayID].nightShifts)
            shifts[shift].restInfeasibility(doctor, Source.NightsWorked)
        days[dayID].addNightInfeasibility(doctor, infeasibility)
        for(source in infeasibility.sources)
            days[source].addToCheckNight(doctor, dayID)
    }
    private fun makeOverlappingNightInfeasible(dayID: Int, doctor: Int, infeasibility: DayNightInfeasibility) {
        if(infeasibility is DayNightInfeasibility.RestAfterRow)
            throw Exception("makeOverlappingNightInfeasible: Rest After row can only applied to full days or nights, " +
                "this function is for use with overlapping night shifts only (WouldCauseRowTooLarge, InsufficientRest")
        for(shift in days[dayID].overlappingNightShifts)
            shifts[shift].restInfeasibility(doctor, Source.OverlappingNight)
        days[dayID].addOverlappingInfeasibility(doctor, infeasibility)
        for(source in infeasibility.sources)
            days[source].addToCheckOverlapping(doctor, dayID)
    }

    // Finds an uninterrupted block of days or nights, and returns a set containing their indexes
    private inline fun checkBlock(index: Int, block: MutableSet<Int>, doctor: Int,
                                  works: (Int, Int) -> Boolean, next:(Int) -> Int) {
        var toCheck = next(index)

        while(toCheck in days.indices) {
            when(works(toCheck, doctor)) {
                false -> return
                true -> block.add(toCheck)
            }
            toCheck = next(toCheck)
        }
    }

    // Checks for a block of assigned days or nights to the right of a starting index, returns false if none are present
    private fun checkRightBlock(doctor: Int, startIndex: Int, block: MutableSet<Int>) : Boolean {
        val nextBlock: MutableSet<Int> = mutableSetOf()
        // index increments as we are "moving to the right"
        checkBlock(startIndex, nextBlock, doctor, ::doctorWorksDay, increment)
        if(nextBlock.isEmpty()) {
            checkBlock(startIndex+1, nextBlock, doctor, ::doctorWorksDay, increment)
            if(block.size == 6 && nextBlock.isNotEmpty()) {
                makeDayInfeasible(startIndex, doctor,
                    DayNightInfeasibility.InsufficientRest(block + nextBlock.min()))
            }
            return false
        }

        // Calculates the size of the stretch if [startIndex] is assigned
        val stretch = nextBlock.size + block.size + 1
        when {
            // The hypothetical stretch would be the maximum allowed before requiring 48 hours rest
            stretch == 7 -> {
                // We know that nextBlock.max() + 1 is unassigned due to checkBlock()
                val rest = nextBlock.max() + 2
                // If [rest] is worked, it would be infeasible to assign [startIndex] as there would not be enough rest
                if (rest in days.indices && doctorWorksDay(rest, doctor)) {
                    makeDayInfeasible(
                        startIndex,
                        doctor,
                        DayNightInfeasibility.InsufficientRest(nextBlock + block + rest)
                    )
                }

            }
            // If the hypothetical stretch is greater than the maximum allowed size, assigning [startIndex] is infeasible
            stretch > 7 -> makeDayInfeasible(startIndex, doctor,
                DayNightInfeasibility.WouldCauseRowTooLarge(nextBlock + block)
            )
        }
        return true
    }

    // Checks for a block of assigned days or nights to the left of a starting index
    private fun checkLeftBlock(doctor: Int, startIndex: Int, block: MutableSet<Int>, secondWorked: Boolean) {
        val prevBlock = mutableSetOf<Int>()
        // index decrements as we are "moving to the left"
        checkBlock(startIndex, prevBlock, doctor, ::doctorWorksDay, decrement)
        if(prevBlock.isEmpty()) {
            checkBlock(startIndex-1, prevBlock, doctor, ::doctorWorksDay, decrement)
            if(prevBlock.size == 6) {
                makeDayInfeasible(
                    startIndex - 1, doctor,
                    DayNightInfeasibility.InsufficientRest(prevBlock + block.min())
                )
            }
            return
        }

        if(prevBlock.size + 1 + block.size == 6 && secondWorked) {
            makeDayInfeasible(startIndex, doctor,
                DayNightInfeasibility.InsufficientRest(prevBlock.union(block) + (block.max() + 2)))
        }

        val priorToPrevBlock = prevBlock.min() - 1
        val blockPriorToPrevBlock = mutableSetOf<Int>()
        checkBlock(priorToPrevBlock, blockPriorToPrevBlock, doctor, ::doctorWorksDay, decrement)

        if(blockPriorToPrevBlock.size + prevBlock.size + 1 == 7 && priorToPrevBlock >= 0) {
            makeDayInfeasible(priorToPrevBlock, doctor,
                DayNightInfeasibility.InsufficientRest(blockPriorToPrevBlock.union(prevBlock) + block.min())
            )
        }

        // Calculates the maximum size of a stretch if [startIndex] is assigned
        val stretch = prevBlock.size + block.size + 1
        when {
            /*
             * [secondWorked] refers to a second stretch and is the returned value from checkRightBlock, it is true,
             * and the hypothetical stretch is the maximum size allowed, assigning [startIndex] would be infeasible,
             * as there would not be 48 hours of rest afterwards.
             */
            stretch == 7 -> {
                if (secondWorked) makeDayInfeasible(
                    startIndex, doctor,
                    DayNightInfeasibility.InsufficientRest(prevBlock + block + (block.max() + 2))
                )
            }
            // If the hypothetical stretch is greater than the allowed maximum, assigning [startIndex] is infeasible
            stretch > 7 -> makeDayInfeasible(
                    startIndex, doctor,
                    DayNightInfeasibility.WouldCauseRowTooLarge(prevBlock + block)
                )
        }
    }

    /*
     * Checks if [startIndex] is part of a consecutive block of days/nights that are worked,
     * adds necessary infeasibility to relevant shifts if maximum stretch is met, and returns
     * the found block as a set of IDs
     */
    private inline fun consecutiveDayNightCheck(doctor: Int, startIndex: Int, max: Int,
                                                works: (Int, Int) -> Boolean,
                                                infeasible: (Int, Int, DayNightInfeasibility) -> Unit
    ) : MutableSet<Int> {
        val block: MutableSet<Int> = mutableSetOf(startIndex)

        checkBlock(startIndex, block, doctor, works, increment)
        checkBlock(startIndex, block, doctor, works, decrement)

        // If the block is of the maximum size allowed, the necessary causes of infeasibility are added
        if(block.size == max) {
            val end = block.max()
            val toUpdate = listOf(block.min() - 1, end + 1, end + 2).filter { it in days.indices }

            for(dayID in toUpdate)
                infeasible(dayID, doctor, DayNightInfeasibility.RestAfterRow(block))
        }
        return block
    }

    // Identifies which causes of infeasibility need to be removed and which can be kept
    private fun calculateToKeep(doctor: Int, day: Int, removed: Int) : Pair<MutableSet<Int>, List<DayNightInfeasibility>> {
        // causes = causes of infeasibility that the day or night in question contributed to
        val (causes, irrelevantCauses) = when(days[day].causesOfInfeasibility[doctor]) {
            null -> throw Exception("calculateToKeep: Day $day is missing infeasibility for doctor $doctor")
            else -> days[day].causesOfInfeasibility[doctor]!!.partition { it.sources.contains(removed) }
        }
        val toKeep: MutableSet<Int> = mutableSetOf()
        // As these causes of infeasibility are still valid, the day needs to be kept in toCheck of the sources
        for(cause in irrelevantCauses) toKeep.addAll(cause.sources)

        /*
         * If an infeasibility due to the size of a hypothetical stretch exists, it needs to be recalculated; for
         * instance, a hypothetical stretch of days could have been 9 days long - after removing a day it could still
         * be 8 days long: infeasible
         */
        val tooLarge = causes.filterIsInstance<DayNightInfeasibility.WouldCauseRowTooLarge>()
        if (tooLarge.isNotEmpty()) {
            run {
                val stretch: MutableSet<Int> = mutableSetOf()
                checkBlock(day, stretch, doctor, ::doctorWorksDay, increment)
                checkBlock(day, stretch, doctor, ::doctorWorksDay, decrement)

                // If the stretch is empty, no further computation is needed
                if(stretch.isEmpty())
                    return@run

                // checkBlock would not include [day] as it is the startIndex
                if(stretch.size > 6) {
                    days[day].addInfeasibility(doctor, DayNightInfeasibility.WouldCauseRowTooLarge(stretch))
                    toKeep.addAll(stretch)
                }
                /*
                 * If the day being checked is at the end of the block, it will not be
                 * included in [stretch], due to the nature of the checkBlock function
                 */
                val dayIsEndOfBlock = stretch.max() + 1 == day
                val subsequentDayToCheck = if(dayIsEndOfBlock) day + 2 else stretch.max() + 2
                if(stretch.size == 6 && subsequentDayToCheck in days.indices
                        && doctorWorksDay(subsequentDayToCheck, doctor)) {
                    makeDayInfeasible(day, doctor, DayNightInfeasibility.InsufficientRest(stretch + subsequentDayToCheck))
                    toKeep.addAll(stretch + subsequentDayToCheck)
                }
            }
        }

        val row = causes.filterIsInstance<DayNightInfeasibility.RestAfterRow>()
        if(row.isNotEmpty() && row[0].sources.max() + 1 == day) {
            val block = mutableSetOf<Int>()
            checkBlock(day, block, doctor, ::doctorWorksDay, decrement)

            val subsequentDayToCheck = day + 2
            if(block.size == 6 && subsequentDayToCheck in days.indices &&
                    doctorWorksDay(subsequentDayToCheck, doctor)) {
                makeDayInfeasible(day, doctor, DayNightInfeasibility.InsufficientRest(block + subsequentDayToCheck))
                toKeep.addAll(block + subsequentDayToCheck)
            }
        }

        return Pair(toKeep, causes)
    }

    // Needs to be altered (weekend starts after midnight on sunday)
    private fun weekendFeasibility(doctor: Int, day: Int, allocate: Boolean) {
        val addedValues = when {
            //Day is a Sunday
            (day + 1) % 7 == 0 -> intArrayOf(6, 7, 13, 14, -7, -8, -14, -15)
            //Day is a Saturday
            (day + 2) % 7 == 0 -> intArrayOf(7, 8, 14, 15, -6, -7, -13, -14)
            else -> return
        }
        val relevantWeekendDays = addedValues.map { it + day }.filter { it in days.indices }

        val feasibilityAction = when(allocate){
            true -> { s: Shift -> s.restInfeasibility(doctor, Source.WeekendWorked(day)) }
            false -> { s: Shift -> s.removeSource(doctor, Source.WeekendWorked(day)) }
        }

        for(dayID in relevantWeekendDays)
            for(shift in days[dayID].getShifts())
                feasibilityAction(shifts[shift])
    }
}

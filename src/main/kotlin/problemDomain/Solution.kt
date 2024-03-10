package problemDomain

import java.util.*
import kotlin.Exception

data class SolutionData(
    val assignments: List<Assignment>,
    val shifts: List<Shift>,
    val doctors: List<MiddleGrade>,
    val days: List<Day>
) {
    fun copy(): SolutionData {
        val assignments = mutableListOf<Assignment>()
        val shifts = mutableListOf<Shift>()
        val days = mutableListOf<Day>()
        val doctors = mutableListOf<MiddleGrade>()
        this.assignments.forEach { assignments.add(it.copy()) }
        this.shifts.forEach { shifts.add(it.copy()) }
        this.doctors.forEach { doctors.add(it.copy()) }
        this.days.forEach { days.add(it.copy()) }
        return SolutionData(assignments, shifts, doctors, days)
    }
}

// Represents a single solution - interface for the problem domain
class Solution(
    val rand: Random,
    val data: SolutionData,
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
        val data = this.data.copy()
        val solution = Solution(rand, data, averageHours, averageNumDayShifts, averageNumNightShifts)
        solution.unassignedAssignments = unassignedAssignments.toMutableList()
        solution.assignedAssignments = assignedAssignments.toMutableList()
        solution.objectiveValue = objectiveValue
        solution.iteration = iteration
        return solution
    }

    fun debug() {
        data.assignments.forEach { it.debug() }
        data.shifts.forEach { it.debug() }
        data.days.forEach { it.debug() }
        println("\n\n\n\n")
    }

    // Generates an initial solution - has stochastic elements
    fun initialise() {
        val (assignments, shifts, doctors) = data

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

        for(day in data.days) {
            // Penalises days that are entirely locum dependent
            if(day.numShiftsWithCoverage == 0)
                value += 20

            // Penalises shifts that are totally locum dependent
            val numLocumDependentShifts = day.getShifts().size - day.numShiftsWithCoverage
            value += 15 * numLocumDependentShifts
        }

        // Impact of each doctor is added to the objective value
        for(doctor in data.doctors)
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
        val (assignments, shifts, doctors, days) = data

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
        updateFeasibilityAllocation(this.data, FeasibilityInfo(shift, doctor))

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
        val (assignments, shifts, doctors, days) = data

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
        updateFeasibilityDeallocation(this.data, FeasibilityInfo(shift, doctor))

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

    // Returns the set of feasible doctors for a given assignment
    fun getFeasibleDoctors(assignmentID: Int): Set<Int> {
        val(assignments, shifts) = data
        if(assignmentID !in assignments.indices)
            throw Exception("getFeasibleDoctors: invalid assignmentID given")
        val assignment = assignments[assignmentID]
        return shifts[assignment.shift].feasibleDoctors.subtract(assignment.infeasibleDoctors)
    }
}

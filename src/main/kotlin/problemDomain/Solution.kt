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
package problemDomain

import java.util.*
import kotlin.Exception
import kotlin.math.absoluteValue
import kotlin.math.pow


// Represents a single solution - interface for the problem domain
class Solution(
    private val rand: Random,
    val data: SolutionData,
) {
    // Tracks unassigned assignments for use in the objective function and heuristics
    var unassignedAssignments = mutableListOf<Int>()
    var assignedAssignments = mutableListOf<Int>()
    val shiftPrefsViolated = mutableMapOf<Int, Int>() // <doctorID, numViolations>
    val dayRangeViolations = mutableMapOf<Int, Int>() // <doctorID, numViolations>
    val nightRangeViolations = mutableMapOf<Int, Int>() // <doctorID, numViolations>
    var objectiveValue = Double.MAX_VALUE

    fun copy(): Solution {
        val data = this.data.copy()
        val solution = Solution(rand, data)
        solution.unassignedAssignments = unassignedAssignments.toMutableList()
        solution.assignedAssignments = assignedAssignments.toMutableList()
        shiftPrefsViolated.forEach { solution.shiftPrefsViolated[it.key] = it.value }
        dayRangeViolations.forEach { solution.dayRangeViolations[it.key] = it.value }
        nightRangeViolations.forEach { solution.nightRangeViolations[it.key] = it.value }
        solution.objectiveValue = objectiveValue
        return solution
    }

    fun debug() {
        data.assignments.forEach { it.debug() }
        data.shifts.forEach { it.debug() }
        data.days.forEach { it.debug() }
        data.doctors.forEach { it.debug() }
        println("\n\n\n\n")
    }

    // Generates an initial solution - has stochastic elements
    fun initialise() {
        val (assignments, shifts, doctors) = data

        for(doctor in doctors) {
            shiftPrefsViolated[doctor.id] = 0
            dayRangeViolations[doctor.id] = 0
            nightRangeViolations[doctor.id] = 0
        }

        val doctorsNeedingNightShifts =
            doctors.indices.filter { doctors[it].targetNightShifts > 0 }.toMutableList()
        val doctorsNeedingDayShifts =
            doctors.indices.filter { doctors[it].targetDayShifts > 0 }.toMutableList()
        // Sorts according to number of infeasible doctors (idea being that they will be the most difficult to assign)
        val sortedShifts =
            shifts.indices.filter { shifts[it].causesOfInfeasibility.isNotEmpty() }.sortedBy { shifts[it].feasibleDoctors.size }

        for(shiftID in sortedShifts) {
            val shift = shifts[shiftID]
            val assignmentId = shift.assignmentIDs[rand.nextInt(shift.assignmentIDs.size)]
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
            val assignmentId = shift.assignmentIDs[rand.nextInt(shift.assignmentIDs.size)]
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
            val assignmentId = shift.assignmentIDs[rand.nextInt(shift.assignmentIDs.size)]
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
        var value = unassignedAssignments.size.toDouble() * 15

        for(day in data.days) {
            // Penalises days that are entirely locum dependent
            if(day.numShiftsWithCoverage == 0)
                value += 30

            // Penalizes shifts that are totally locum dependent
            val numLocumDependentShifts = day.getShifts().size - day.numShiftsWithCoverage
            value += 20 * numLocumDependentShifts
        }

        // Impact of each doctor is added to the objective value
        for(doctor in data.doctors)
            value += calculateDoctorContribution(doctor)

        // Impact of disparity in preference adherence is calculated
        value += calculatePreferenceDisparity() * 500

        objectiveValue = value
    }

    // Calculates the impact of a single doctor on the objective function value
    private fun calculateDoctorContribution(doctor: MiddleGrade): Double {
        var contribution = 0.00

        // Variance compared to number of shifts and hours worked targets
        val varianceAverageWorked = doctor.varianceHoursWorked()
        contribution += when (varianceAverageWorked < 0) {
            false -> varianceAverageWorked * 5
            true -> -varianceAverageWorked * 10
        }

        // Lower weighting as unsure about correctness of this target
        val dayShiftVariance = doctor.varianceNightShiftsWorked()
        contribution += when(dayShiftVariance < 0) {
            false -> dayShiftVariance * 2
            true -> -dayShiftVariance * 2
        }

        val nightVariance = doctor.varianceNightShiftsWorked()
        contribution += when (nightVariance < 0) {
            false -> nightVariance * 10
            true -> -nightVariance * 12
        }

        // Contribution of infractions on preferences - also updates solution record of infractions
        val (shiftPrefsViolated, dayRangeViolations, nightRangeViolations) =
            calculateDoctorPreferenceViolations(doctor)

        // Add contributions of infractions
        return contribution + shiftPrefsViolated * 4 + dayRangeViolations * 4 + nightRangeViolations * 4
    }

    private fun calculateDoctorPreferenceViolations(doctor: MiddleGrade): Triple<Int, Int, Int> {
        val shiftPrefsViolated = doctor.numberOfShiftPrefsViolated()
        var dayRangeViolations = 0
        var nightRangeViolations = 0
        for(block in doctor.blocksOfDays.values) {
            if(block.items.size !in doctor.dayRange)
                dayRangeViolations++

            var numNightsInRow = 0
            // If there are night shifts in the block, they must be at the end, due to rest laws
            for(dayID in block.items.sortedDescending()) {
                /*
                 * End day of block could be from an overlapping night shift, so next day
                 * still needs to be checking if the end day does not have a worked night
                 * shift
                 */
                if(numNightsInRow == 0) {
                    when (data.days[dayID].doctorsWorkingNight[doctor.id] == null) {
                        true -> continue
                        false -> numNightsInRow++
                    }
                }
                else
                // Continue until stretch of nights ends
                    when (data.days[dayID].doctorsWorkingNight[doctor.id] == null) {
                        true -> break
                        false -> numNightsInRow++
                    }
            }

            // true if there is a row of nights and it is not in the desired range
            if(numNightsInRow > 0 && numNightsInRow !in doctor.nightRange)
                nightRangeViolations++
        }


        // Update solution record of infractions
        this.shiftPrefsViolated[doctor.id] = shiftPrefsViolated
        this.dayRangeViolations[doctor.id] = dayRangeViolations
        this.nightRangeViolations[doctor.id] = nightRangeViolations

        return Triple(shiftPrefsViolated, dayRangeViolations, nightRangeViolations)
    }

    // Uses Jain's Fairness Index
    private fun calculatePreferenceDisparity(): Double {
        // Calculate disparity in terms of shift preferences
        val withShiftPreferences = shiftPrefsViolated.filter {
            data.doctors[it.key].preferences.shiftsToAvoid
        }
        val shiftNumerator = withShiftPreferences.values.sum().toDouble().pow(2)
        var shiftSquaredTotal = 0.00
        withShiftPreferences.values.forEach { shiftSquaredTotal += it.toDouble().pow(2) }
        val shiftDenominator = shiftSquaredTotal * withShiftPreferences.size
        val shiftDisparity = shiftNumerator / shiftDenominator

        // Calculate disparity in terms of day stretch preferences
        val withDayPreferences = dayRangeViolations.filter {
            data.doctors[it.key].preferences.dayRange
        }
        val dayNumerator = withDayPreferences.values.sum().toDouble().pow(2)
        var daySquaredTotal = 0.00
        withDayPreferences.values.forEach { daySquaredTotal += it.toDouble().pow(2) }
        val dayDenominator = daySquaredTotal * withDayPreferences.size
        val dayDisparity = dayNumerator / dayDenominator

        // Calculate disparity in terms of night stretch preferences
        val withNightPreferences = nightRangeViolations.filter {
            data.doctors[it.key].preferences.nightRange
        }
        val nightNumerator = withNightPreferences.values.sum().toDouble().pow(2)
        var nightSquaredTotal = 0.00
        withNightPreferences.values.forEach { nightSquaredTotal += it.toDouble().pow(2) }
        val nightDenominator = nightSquaredTotal * withNightPreferences.size
        val nightDisparity = nightNumerator / nightDenominator

        val shiftContribution = if(!shiftDisparity.isNaN()) shiftDisparity else 1.0
        val dayContribution = if(!dayDisparity.isNaN()) dayDisparity else 1.0
        val nightContribution = if(!nightDisparity.isNaN()) nightDisparity else 1.0
        return 3 - (shiftContribution + dayContribution + nightContribution)
    }

    // Breaks down the sources of a solution's objective function score; used in result analysis
    fun descriptiveObjectiveFunction(toRead: Boolean): String {
        // Called to ensure that all values that are needed have been initialised
        calculateObjectiveValue()
        var description = ""

        val numDaysWithoutCoverage = data.days.count { it.numShiftsWithCoverage == 0 }
        val dayCoverageContribution = numDaysWithoutCoverage * 30
        val numShiftsWithoutCoverage = data.shifts.count { it.assignees.isEmpty() }
        val shiftCoverageContribution = numShiftsWithoutCoverage * 20
        val numAssignmentsWithoutCoverage = data.assignments.count { it.assignee == null }
        val assignmentCoverageContribution = numAssignmentsWithoutCoverage * 15
        val totalCoverageContribution =
            dayCoverageContribution + shiftCoverageContribution + assignmentCoverageContribution


        // Analysis of Doctor related contribution
        var totalVarianceHours = 0.0
        var varianceHoursContribution = 0.0
        var totalVarianceDayShifts = 0
        var contributionVarianceDayShifts = 0.0
        var totalVarianceNightShifts = 0
        var contributionVarianceNightShifts = 0.0
        var totalPreferenceViolations = 0
        var contributionPreferenceViolations = 0.0

        data.doctors.forEach {
            val varianceHours = it.varianceHoursWorked()
            totalVarianceHours += varianceHours.absoluteValue
            varianceHoursContribution += when(varianceHours < 0) {
                false -> varianceHours * 5
                true -> -varianceHours * 10
            }

            val varianceDays = it.varianceDayShiftsWorked()
            totalVarianceDayShifts += varianceDays.absoluteValue
            contributionVarianceDayShifts += when(varianceDays < 0) {
                false -> varianceDays * 2
                true -> -varianceDays * 2
            }

            val varianceNights = it.varianceNightShiftsWorked()
            totalVarianceNightShifts += varianceNights.absoluteValue
            contributionVarianceNightShifts += when(varianceNights < 0) {
                false -> varianceNights * 10
                true -> -varianceNights * 12
            }

            val prefsViolated = calculateDoctorPreferenceViolations(it).toList().sum()
            totalPreferenceViolations += prefsViolated
            contributionPreferenceViolations += prefsViolated * 4
        }

        val totalPreferenceContribution = contributionPreferenceViolations + 500 * calculatePreferenceDisparity()

        val totalDoctorTargetContribution =
            varianceHoursContribution + contributionVarianceDayShifts + contributionVarianceNightShifts


        when(toRead) {
            true -> {
                description += "Objective Value: $objectiveValue\n\n"

                description += "Breakdown of overall contributions:\n"
                description += "    - Coverage: $totalCoverageContribution\n"
                description += "    - Doctor Targets: $totalDoctorTargetContribution\n"
                description += "    - Doctor Preferences: $totalPreferenceContribution\n\n"

                description += "Breakdown of coverage contributions:\n"
                description += "    - Days Without Coverage: $numDaysWithoutCoverage, Contribution to Objective Function Value: $dayCoverageContribution\n"
                description += "    - Shifts Without Coverage: $numShiftsWithoutCoverage, Contribution to Objective Function Value: $shiftCoverageContribution\n"
                description += "    - Assignments Without Coverage: $numAssignmentsWithoutCoverage, Contribution to Objective Function Value: $assignmentCoverageContribution\n\n"

                description += "Breakdown of doctor target contributions:\n"
                description += "    - Total deviation from target hours: $totalVarianceHours, Contribution to Objective Function Value: $varianceHoursContribution\n"
                description += "    - Total deviation from target day shifts: $totalVarianceDayShifts, Contribution to Objective Function Value: $contributionVarianceDayShifts\n"
                description += "    - Total deviation from target night shifts: $totalVarianceNightShifts, Contribution to Objective Function Value: $contributionVarianceNightShifts\n\n"

                description += "Breakdown of doctor preference contributions:\n"
                description += "    - Total preference violations: $totalPreferenceViolations, Contribution to Objective Function Value: $contributionPreferenceViolations\n"
                description += "    - Added score for disparity in preferences being met: ${500 * calculatePreferenceDisparity()}"
            }

            false -> {
                description += "$totalCoverageContribution,$totalDoctorTargetContribution,$totalPreferenceContribution"
            }
        }

        return description
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

        // Subtracts previous objective value of preference disparity
        objectiveValue -= calculatePreferenceDisparity() * 500

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
            is NightShift -> doc.nightShiftsWorked++
        }
        doc.assignedAssignments.add(assignment)
        doc.assignedShifts.add(shift.id)

        // Update Objective Function value using delta evaluation
        objectiveValue -= 15 // One less unassigned assignment

        // Adds new objective contribution value of the doctor
        objectiveValue += calculateDoctorContribution(doctors[doctor])

        // Adds new contribution of preference disparity
        objectiveValue += calculatePreferenceDisparity() * 500

        // Checks if the shift was previously without assignees, if so, does the same for the day
        if(shift.assignees.size == 1) {
            objectiveValue -= 20
            days[shift.day].numShiftsWithCoverage++
            if(days[shift.day].numShiftsWithCoverage == 1)
                objectiveValue -= 30
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
            if(shift.day - 1 !in days.indices || shift.day + 1 !in days.indices)
                inMiddleOfBlock = false
            if(inMiddleOfBlock) {
                assignments[assignment].assign(doctor)
                return false
            }
        }

        // Subtracts previous objective value contribution of the doctor
        objectiveValue -= calculateDoctorContribution(doctors[doctor])

        // Subtracts previous objective value contribution of disparity
        objectiveValue -= calculatePreferenceDisparity() * 500

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
            is NightShift -> doc.nightShiftsWorked--
        }
        doc.assignedAssignments.remove(assignment)
        doc.assignedShifts.remove(shift.id)

        // Updates Objective Function value using delta evaluation
        objectiveValue += 15 // One extra unassigned assignment

        // Adds new objective contribution value of the doctor
        objectiveValue += calculateDoctorContribution(doctors[doctor])

        // Adds new contribution of preference disparity
        objectiveValue += calculatePreferenceDisparity() * 500

        // Checks if the shift is without assignees, if so also checks the day
        if(shift.assignees.isEmpty()) {
            objectiveValue += 20
            days[shift.day].numShiftsWithCoverage--
            if(days[shift.day].numShiftsWithCoverage == 0)
                objectiveValue += 30
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

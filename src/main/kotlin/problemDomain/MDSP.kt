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

import hyflex.ProblemDomain
import java.lang.NumberFormatException
import java.util.Scanner

class MDSP(
    seed: Long,
) : ProblemDomain(seed) {
    lateinit var solutionMemory: Array<Solution?>
    lateinit var bestSolution: Solution
    private var bestSolutionValue = Double.MAX_VALUE
    private lateinit var grades: List<String>
    private var averageHours = 0
    private var averageDayShifts = 0
    private var averageNightShifts = 0
    private var numDoctors = 0
    private var numWeeks = 0
    lateinit var doctors: List<MiddleGrade>
    private lateinit var doctorsOfGrade: Map<String, Set<Int>>
    private lateinit var doctorLeave: Map<Int, List<Int>>
    private lateinit var doctorTraining: Map<Int, List<Int>>
    lateinit var days: List<Day>
    lateinit var shifts: List<Shift>
    lateinit var shiftTimes: Map<Int, String>
    private lateinit var assignments: List<Assignment>
    private var searchDepth = 2
    private var mutationStrength = 2

    override fun toString(): String {
        return "problemDomain.MDSP"
    }

    override fun setDepthOfSearch(depthOfSearch: Double) {
        super.setDepthOfSearch(depthOfSearch)
        searchDepth = when {
            depthOfSearch <= 0.2 -> 1
            depthOfSearch <= 0.4 -> 2
            depthOfSearch <= 0.5 -> 3
            depthOfSearch <= 0.6 -> 4
            depthOfSearch <= 0.8 -> 5
            else -> 6
        }
    }

    override fun setIntensityOfMutation(intensityOfMutation: Double) {
        super.setIntensityOfMutation(intensityOfMutation)
        mutationStrength = when {
            intensityOfMutation <= 0.2 -> 1
            depthOfSearch <= 0.4 -> 2
            depthOfSearch <= 0.5 -> 3
            depthOfSearch <= 0.6 -> 4
            depthOfSearch <= 0.8 -> 5
            else -> 6
        }
    }

    override fun getHeuristicsOfType(heuristicType: HeuristicType): IntArray? {
        return when(heuristicType) {
            HeuristicType.MUTATION -> intArrayOf(0,1,2,3)
            HeuristicType.CROSSOVER -> intArrayOf(4)
            HeuristicType.RUIN_RECREATE -> intArrayOf(5,6)
            HeuristicType.LOCAL_SEARCH -> intArrayOf(7,8)
            HeuristicType.OTHER -> null
        }
    }

    override fun getHeuristicsThatUseIntensityOfMutation(): IntArray { return intArrayOf(0,1,2,3,5,6) }

    override fun getHeuristicsThatUseDepthOfSearch(): IntArray { return intArrayOf(7,8) }

    override fun loadInstance(instanceID: Int) {
        val fileName = when(instanceID) {
            0 -> "department1_baseline"
            1 -> "experiment2_baseline"
            else -> Exception("loadInstance: Invalid instanceID given")
        }

        readFile("/instances/$fileName.txt")
    }

    fun loadInstance(fileName: String) {
        readFile("/instances/$fileName.txt")
    }

    private fun readFile(filename: String) {
        val file = javaClass.getResourceAsStream(filename)!!
        val scanner = Scanner(file)
        val gradeString = scanner.nextLine()
        grades = gradeString.split(" ")
        averageHours = scanner.nextInt()
        averageDayShifts = scanner.nextInt()
        averageNightShifts = scanner.nextInt()
        numWeeks = scanner.nextInt()
        numDoctors = scanner.nextInt()
        readDoctors(scanner)
        scanner.nextLine()
        readDaysAndShifts(scanner, numWeeks*7)
        setLeaveAndTrainingInfeasibility()
    }

    private fun readDoctors(scanner: Scanner) {
        val doctors = mutableListOf<MiddleGrade>()
        val doctorsOfGrade = grades.associateWith { mutableSetOf<Int>() }
        val leave = mutableMapOf<Int, List<Int>>()
        val training = mutableMapOf<Int, List<Int>>()

        for(id in 0..<numDoctors) {
            var string = scanner.next()
            /*
             * Takes the doctor's hours of leave and works out the denominator that should be used
             * in order to provide an adjusted measure for average hours worked per week that takes
             * the aforementioned leave into account
             */
            val hoursOff = string.toDouble()

            string = scanner.next()
            leave[id] = when(string) {
                "NULL" -> emptyList()
                else -> string.split(",").map { it.toInt() }
            }

            string = scanner.next()
            val totalHoursToAdjustFor = string.toDouble() + hoursOff
            val averageHoursDenominator = (168 * numWeeks - totalHoursToAdjustFor) / (168 * numWeeks) * numWeeks

            string = scanner.next()
            training[id] = when(string) {
                "NULL" -> emptyList()
                else -> string.split(",").map { it.toInt() }
            }

            string = scanner.next()
            val dayRange = when(string) {
                "None" -> 1..7
                else -> {
                    val bounds = string.split("-")
                    bounds[0].toInt()..bounds[1].toInt()
                }
            }

            string = scanner.next()
            val nightRange = when(string) {
                "None" -> 1..4
                else -> {
                    val bounds = string.split("-")
                    bounds[0].toInt()..bounds[1].toInt()
                }
            }

            string = scanner.next()
            val shiftsToAvoid = when(string) {
                "NULL" -> emptyList()
                else -> string.split(",").map { it.toInt() }
            }

            string = scanner.next()
            val grade = when(string) {
                in grades -> string
                else -> throw Exception("readDoctors: invalid grade given")
            }

            string = scanner.next()
            val targets: Triple<Double, Int, Int> = try {
                val percentage = string.toDouble()/100
                Triple(averageHours * percentage, (averageDayShifts * percentage).toInt(),
                    (averageNightShifts * percentage).toInt())
            } catch (exception: NumberFormatException) {
                Triple(averageHours.toDouble(), averageDayShifts, averageNightShifts)
            }

            doctors.add(MiddleGrade(id, grade, targets.first, targets.second, targets.third,
                averageHoursDenominator, shiftsToAvoid.toSet(), dayRange, nightRange))
            doctorsOfGrade[grade]!!.add(id)
        }

        this.doctors = doctors
        this.doctorsOfGrade = doctorsOfGrade
        doctorLeave = leave
        doctorTraining = training
    }

    private fun readDaysAndShifts(scanner: Scanner, numDays: Int) {
        val days = mutableListOf<Day>()
        val shifts = mutableListOf<Shift>()
        val shiftTimes = mutableMapOf<Int, String>()
        val assignments = mutableListOf<Assignment>()
        var assignmentID = 0

        for(dayID in 0..<numDays) {
            val dayShifts = mutableListOf<Int>()
            val nonOverlappingNights = mutableListOf<Int>()
            val overlappingNights = mutableListOf<Int>()
            val longShifts = mutableListOf<Int>()
            var nextDay = false
            while(!nextDay) {
                val nextLine = scanner.nextLine()
                if(nextLine == "next") {
                    nextDay = true
                    continue
                }
                val (shiftIDString, assignmentGrades) = nextLine.split(" ", limit = 2)
                val shiftID = shiftIDString.toInt()
                val shiftAssignments = mutableListOf<Int>()
                for(assignmentGrade in assignmentGrades.split(" ")) {
                    assignments.add(createAssignment(assignmentID, shiftID, assignmentGrade))
                    shiftAssignments.add(assignmentID)
                    assignmentID++
                }
                val shiftsWithin11Hours = getShiftIDs(scanner)
                val shifts48HoursAfter = getShiftIDs(scanner)
                val longShifts48HoursBefore = getShiftIDs(scanner)
                val otherRelevantShifts = getShiftIDs(scanner)
                val duration = scanner.nextDouble()
                scanner.nextLine()
                shiftTimes[shiftID] = scanner.nextLine()
                val type = scanner.nextLine()
                when(type) {
                    "day" -> {
                        shifts.add(
                            DayShift(
                                shiftID, shiftAssignments.toIntArray(), shiftsWithin11Hours.toSet(),
                                shifts48HoursAfter, longShifts48HoursBefore, otherRelevantShifts.toSet(),
                                dayID, (0..<numDoctors).toMutableSet(), duration
                            )
                        )
                        dayShifts.add(shiftID)
                        if(duration > 10.0) longShifts.add(shiftID)
                    }

                    "night" -> {
                        shifts.add(
                            NightShift(
                                shiftID, shiftAssignments.toIntArray(), shiftsWithin11Hours.toSet(),
                                shifts48HoursAfter, longShifts48HoursBefore, otherRelevantShifts.toSet(),
                                dayID, (0..<numDoctors).toMutableSet(), duration, false
                            )
                        )
                        nonOverlappingNights.add(shiftID)
                        if(duration > 10.0) longShifts.add(shiftID)
                    }

                    "night overlaps" -> {
                        shifts.add(
                            NightShift(
                                shiftID, shiftAssignments.toIntArray(), shiftsWithin11Hours.toSet(),
                                shifts48HoursAfter, longShifts48HoursBefore, otherRelevantShifts.toSet(),
                                dayID, (0..<numDoctors).toMutableSet(), duration, true
                            )
                        )
                        overlappingNights.add(shiftID)
                        if(duration > 10.0) longShifts.add(shiftID)
                    }
                    else -> throw Exception("Invalid shift type given, please limit to \"day\" and \"night\"")
                }
            }
            days.add(Day(dayID, dayShifts, nonOverlappingNights, overlappingNights, longShifts))
        }
        this.days = days
        this.shifts = shifts
        this.shiftTimes = shiftTimes
        this.assignments = assignments
    }

    private fun createAssignment(id: Int, shiftID: Int, grade: String): Assignment {
        val infeasibleDoctors = when(grade) {
            "any" -> emptySet()
            in grades -> (0..<numDoctors).subtract(doctorsOfGrade.getOrDefault(grade, emptySet()))
            else -> throw Exception("createAssignment: invalid grade given")
        }
        return Assignment(id, shiftID, grade, infeasibleDoctors)
    }

    private fun getShiftIDs(scanner: Scanner): List<Int> {
        val shiftIDs = when(val nextLine = scanner.nextLine()) {
            "NULL" -> emptyList()
            else -> nextLine.split(" ").map { it.toInt() }
        }
        return shiftIDs
    }

    private fun setLeaveAndTrainingInfeasibility() {
        for((doctorID, leaveShifts) in doctorLeave)
            for(shiftId in leaveShifts) {
                shifts[shiftId].createNonRestInfeasibility(doctorID, Cause.Leave)
            }
        for((doctorID, trainingShifts) in doctorTraining)
            for(shiftId in trainingShifts.filter { shifts[it].causesOfInfeasibility[doctorID] == null })
                shifts[shiftId].createNonRestInfeasibility(doctorID, Cause.Training)
    }

    override fun setMemorySize(size: Int) {
        // If memory has not been initialised, just return empty array of nulls
        if(!this::solutionMemory.isInitialized) {
            solutionMemory = arrayOfNulls(size)
            return
        }

        // If memory has been initialised, copy solutions from old memory that will fit in the new array
        val newSolutionMemory = arrayOfNulls<Solution>(size)
        for(x in solutionMemory.indices.filter { it <= size })
            newSolutionMemory[x] = solutionMemory[x]
        solutionMemory = newSolutionMemory
    }

    override fun initialiseSolution(index: Int) {
        when {
            index !in solutionMemory.indices -> throw Exception("initialiseSolution: Invalid index passed")
            this.assignments.isEmpty() -> throw Exception("initialiseSolution: Instance has not been read")
        }
        solutionMemory[index] = blankSolution()
        solutionMemory[index]!!.initialise()
        val solutionQuality = getFunctionValue(index)
        if(solutionQuality < bestSolutionValue) {
            bestSolutionValue = solutionQuality
            bestSolution = solutionMemory[index]!!.copy()
        }
    }

    // Returns a blank solution for use in testing and the experiment interface
    fun blankSolution(): Solution {
        val assignments = mutableListOf<Assignment>()
        val shifts = mutableListOf<Shift>()
        val days = mutableListOf<Day>()
        val doctors = mutableListOf<MiddleGrade>()
        this.assignments.forEach { assignments.add(it.copy()) }
        this.shifts.forEach { shifts.add(it.copy()) }
        this.doctors.forEach { doctors.add((it.copy())) }
        this.days.forEach { days.add(it.copy()) }

        val solution = Solution(rng, SolutionData(assignments, shifts, doctors, days))
        solution.unassignedAssignments = assignments.indices.toMutableList()
        solution.calculateObjectiveValue()
        return solution
    }

    override fun getNumberOfHeuristics(): Int {
        return 11
    }

    override fun applyHeuristic(heuristicID: Int, solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        if(solutionSourceIndex !in solutionMemory.indices || solutionDestinationIndex !in solutionMemory.indices)
            throw Exception("applyHeuristic: invalid memory index passed")

        val startTime = System.currentTimeMillis()
        val objectiveValue = when(heuristicID) {
            0 -> heuristic0(solutionSourceIndex, solutionDestinationIndex)
            1 -> heuristic1(solutionSourceIndex, solutionDestinationIndex)
            2 -> heuristic2(solutionSourceIndex, solutionDestinationIndex)
            3 -> heuristic3(solutionSourceIndex, solutionDestinationIndex)
            4 -> solutionMemory[solutionSourceIndex]!!.objectiveValue
            5 -> heuristic5(solutionSourceIndex, solutionDestinationIndex)
            6 -> heuristic6(solutionSourceIndex, solutionDestinationIndex)
            7 -> heuristic7(solutionSourceIndex, solutionDestinationIndex)
            8 -> heuristic8(solutionSourceIndex, solutionDestinationIndex)
            9 -> heuristic9(solutionSourceIndex, solutionDestinationIndex)
            10 -> heuristic10(solutionSourceIndex, solutionDestinationIndex)
            else -> throw Exception("Heuristic: $heuristicID does not exist")
        }

        if(heuristicID == 4)
            solutionMemory[solutionDestinationIndex] = solutionMemory[solutionSourceIndex]!!.copy()


        heuristicCallTimeRecord[heuristicID] = (System.currentTimeMillis() - startTime).toInt()
        heuristicCallRecord[heuristicID]++

        if(objectiveValue < bestSolutionValue) {
            bestSolution = solutionMemory[solutionDestinationIndex]!!.copy()
            bestSolutionValue = objectiveValue
        }

        return objectiveValue
    }

    override fun applyHeuristic(
        heuristicID: Int,
        solutionSourceIndex1: Int,
        solutionSourceIndex2: Int,
        solutionDestinationIndex: Int
    ): Double {
        if(solutionSourceIndex1 !in solutionMemory.indices ||
            solutionSourceIndex2 !in solutionMemory.indices || solutionDestinationIndex !in solutionMemory.indices)
            throw Exception("applyHeuristic: invalid memory index passed")

        val startTime = System.currentTimeMillis()
        val objectiveValue = when(heuristicID) {
            0 -> heuristic0(solutionSourceIndex1, solutionDestinationIndex)
            1 -> heuristic1(solutionSourceIndex1, solutionDestinationIndex)
            2 -> heuristic2(solutionSourceIndex1, solutionDestinationIndex)
            3 -> heuristic3(solutionSourceIndex1, solutionDestinationIndex)
            4 -> heuristic4(solutionSourceIndex1, solutionSourceIndex2, solutionDestinationIndex)
            5 -> heuristic5(solutionSourceIndex1, solutionDestinationIndex)
            6 -> heuristic6(solutionSourceIndex1, solutionDestinationIndex)
            7 -> heuristic7(solutionSourceIndex1, solutionDestinationIndex)
            8 -> heuristic8(solutionSourceIndex1, solutionDestinationIndex)
            9 -> heuristic9(solutionSourceIndex1, solutionDestinationIndex)
            10 -> heuristic10(solutionSourceIndex1, solutionDestinationIndex)
            else -> throw Exception("applyHeuristic: Invalid heuristicID passed with arguments")
        }

        heuristicCallTimeRecord[heuristicID] = (System.currentTimeMillis() - startTime).toInt()
        heuristicCallRecord[heuristicID]++

        if(objectiveValue < bestSolutionValue) {
            bestSolution = solutionMemory[solutionDestinationIndex]!!.copy()
            bestSolutionValue = objectiveValue
        }

        return objectiveValue
    }

    // Deallocates an IOM-dependant number of random assignments for a randomly selected doctor
    private fun heuristic0(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()
        var mutationStrength = this.mutationStrength
        val doctor = tempSol.data.doctors[rng.nextInt(doctors.size)]
        if(doctor.assignedAssignments.size < mutationStrength) mutationStrength = doctor.assignedAssignments.size

        IOM@for(x in 1..mutationStrength) {
            // Will make 20 attempts to deallocate a random assignment (might not be feasible to do so)
            for(y in 0..19) {
                val numAssignedAssignments = doctor.assignedAssignments.size
                if(numAssignedAssignments == 0) break@IOM
                val assignment = doctor.assignedAssignments[rng.nextInt(numAssignedAssignments)]
                // If it succeeds in deallocating a solution in 20 attempts, we continue with the IOM loop
                if(tempSol.deallocateAssignment(assignment)) continue@IOM
            }
            // If it was not possible to remove a random assignment after 20 attempts, exit the loop
            break
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // Allocates an IOM-dependant number of random assignments for a randomly selected doctor
    private fun heuristic1(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()
        val mutationStrength = this.mutationStrength
        val doctor = rng.nextInt(doctors.size)

        IOM@for(x in 1..mutationStrength) {
            for(y in 0..19) {
                val numUnassignedAssignments = tempSol.unassignedAssignments.size
                if(numUnassignedAssignments == 0) break@IOM
                val assignment = tempSol.unassignedAssignments[rng.nextInt(numUnassignedAssignments)]
                if(tempSol.allocateAssignment(assignment, doctor)) continue@IOM
            }
            break
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // Deallocates an IOM-dependant number of random assignments
    private fun heuristic2(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()
        var mutationStrength = this.mutationStrength
        val numAssignedAssignments = tempSol.assignedAssignments.size
        if(mutationStrength > numAssignedAssignments) mutationStrength = numAssignedAssignments

        IOM@for(x in 1..mutationStrength) {
            for (y in 0..19) {
                val numAssignedAssignments = tempSol.assignedAssignments.size
                if(numAssignedAssignments == 0) break@IOM
                val assignment = tempSol.assignedAssignments[rng.nextInt(numAssignedAssignments)]
                if(tempSol.deallocateAssignment(assignment)) continue@IOM
            }
            break
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // Allocates an IOM-dependant number of random assignments to random feasible doctors
    private fun heuristic3(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()
        var mutationStrength = this.mutationStrength
        val numUnassignedAssignments = tempSol.unassignedAssignments.size
        if(mutationStrength > numUnassignedAssignments) mutationStrength = numUnassignedAssignments

        IOM@for(x in 1..mutationStrength) {
            val usableUnassignedAssignments = tempSol.unassignedAssignments.toMutableList()
            while(usableUnassignedAssignments.isNotEmpty()) {
                val assignment = usableUnassignedAssignments[rng.nextInt(usableUnassignedAssignments.size)]
                val feasibleDoctors = tempSol.getFeasibleDoctors(assignment).toList()

                when(feasibleDoctors.isEmpty()) {
                    true -> usableUnassignedAssignments.remove(assignment)
                    false -> {
                        val doctor = feasibleDoctors[rng.nextInt(feasibleDoctors.size)]
                        tempSol.allocateAssignment(assignment, doctor)
                        continue@IOM
                    }
                }
            }
            break
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // Creates a solution from common assignments of both input solutions
    private fun heuristic4(
        solutionSourceIndex: Int,
        solutionSourceIndex2: Int,
        solutionDestinationIndex: Int
    ): Double {
        val tempSol = blankSolution()
        tempSol.calculateObjectiveValue()
        /*
         * Both indexes will have been checked before this function is called - the return
         * values only exist to avoid having to use nullability operators on every call to
         * [sol1] or [sol2]
         */
        val sol1 = solutionMemory[solutionSourceIndex] ?: return Double.MAX_VALUE
        val sol2 = solutionMemory[solutionSourceIndex2] ?: return Double.MAX_VALUE

        for(assignment in tempSol.data.assignments) {
            val sol1Assignee = sol1.data.assignments[assignment.id].assignee
            if (sol1Assignee == sol2.data.assignments[assignment.id].assignee && sol1Assignee != null)
                tempSol.allocateAssignment(assignment.id, sol1Assignee)
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // Random Ruin and Recreate - Doctor based
    private fun heuristic5(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()
        var mutationStrength = this.mutationStrength
        val doctor = tempSol.data.doctors[rng.nextInt(tempSol.data.doctors.size)]
        val numAssignedAssignments = doctor.assignedAssignments.size
        if(mutationStrength > numAssignedAssignments)
            mutationStrength = numAssignedAssignments

        var numUnassigned = 0
        Ruin@for(x in 1..mutationStrength) {
            // We attempt to deallocate an assignment 20 times before giving up (might not be feasible to do so)
            for(y in 0..19) {
                val assignment = doctor.assignedAssignments[rng.nextInt(doctor.assignedAssignments.size)]
                if(tempSol.deallocateAssignment(assignment)) {
                    numUnassigned++
                    continue@Ruin
                }
            }
            break
        }

        Recreate@for(x in 1..numUnassigned) {
            // Give up if fail to find an assignable assignment for the doctor in 20 attempts
            for(y in 0..19) {
                val assignment = tempSol.unassignedAssignments[rng.nextInt(tempSol.unassignedAssignments.size)]
                val feasibleDoctors = tempSol.getFeasibleDoctors(assignment).toList()
                if(feasibleDoctors.contains(doctor.id)) {
                    tempSol.allocateAssignment(assignment, doctor.id)
                    continue@Recreate
                }
            }
            break
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // Random Ruin and Recreate - Assignment Based
    private fun heuristic6(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()
        var mutationStrength = this.mutationStrength
        val numAssignedAssignments = tempSol.assignedAssignments.size
        if(mutationStrength > numAssignedAssignments)
            mutationStrength = numAssignedAssignments

        val unassignedAssignments = mutableListOf<Int>()
        IOM@for(x in 1..mutationStrength) {
            // We attempt to deallocate an assignment 20 times before giving up (might not be feasible to do so)
            for(y in 0..19) {
                val assignment = tempSol.assignedAssignments[rng.nextInt(tempSol.assignedAssignments.size)]
                if(tempSol.deallocateAssignment(assignment)) {
                    unassignedAssignments.add(assignment)
                    continue@IOM
                }
            }
            break
        }

        while(unassignedAssignments.isNotEmpty()) {
            val assignment = unassignedAssignments[rng.nextInt(unassignedAssignments.size)]
            val feasibleDoctors = tempSol.getFeasibleDoctors(assignment).toList()
            if(feasibleDoctors.isNotEmpty()) {
                val doctor = feasibleDoctors[rng.nextInt(feasibleDoctors.size)]
                tempSol.allocateAssignment(assignment, doctor)
            }
            /*
             * If there are no feasible doctors for an assignment, remove from the list to avoid
             * an infinite loop, if the assignment is successfully allocated, it needs to be removed
             * from the list of unassigned assignments.
             */
            unassignedAssignments.remove(assignment)
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // First Improvement Hill-Climbing - assigning a random doctor
    private fun heuristic7(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()

        val numUnassignedAssignments = tempSol.unassignedAssignments.size
        if(numUnassignedAssignments == 0) {
            solutionMemory[solutionDestinationIndex] = tempSol
            return tempSol.objectiveValue
        }

        var unassignedIndex = rng.nextInt(numUnassignedAssignments)

        for(x in 1..searchDepth) {
            var improved = false
            val assignmentsToCheck = tempSol.unassignedAssignments.size
            for(y in 1..assignmentsToCheck) {
                val assignment = tempSol.unassignedAssignments[unassignedIndex]
                val feasibleDoctors = tempSol.getFeasibleDoctors(assignment).toList()

                if(feasibleDoctors.isNotEmpty()) {
                    val prevValue = tempSol.objectiveValue
                    val doctor = feasibleDoctors[rng.nextInt(feasibleDoctors.size)]
                    tempSol.allocateAssignment(assignment, doctor)

                    when(tempSol.objectiveValue < prevValue) {
                        true -> {
                            improved = true
                            unassignedIndex--
                        }
                        false -> tempSol.deallocateAssignment(assignment)
                    }
                }
                unassignedIndex++
                if(unassignedIndex !in tempSol.unassignedAssignments.indices) unassignedIndex = 0
            }
            if(!improved) break
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // Steepest Descent Hill Climbing - assigning a random doctor
    private fun heuristic8(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()

        for(x in 1..searchDepth*2) {
            val prevValue = tempSol.objectiveValue
            var bestValue = Double.MAX_VALUE
            var bestAssignment = Int.MAX_VALUE
            var bestDoctor = Int.MAX_VALUE
            val assignmentsToCheck = tempSol.unassignedAssignments.toList()
            for(assignment in assignmentsToCheck) {
                val feasibleDoctors = tempSol.getFeasibleDoctors(assignment).toList()

                if(feasibleDoctors.isNotEmpty()) {
                    val doctor = feasibleDoctors[rng.nextInt(feasibleDoctors.size)]
                    tempSol.allocateAssignment(assignment, doctor)

                    if(tempSol.objectiveValue < bestValue) {
                        bestValue = tempSol.objectiveValue
                        bestAssignment = assignment
                        bestDoctor = doctor
                    }
                    tempSol.deallocateAssignment(assignment)
                }
            }

            if(bestValue < prevValue) {
                tempSol.allocateAssignment(bestAssignment, bestDoctor)
                continue
            }
            // In the case of no improvement, we exit the loop
            break
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // First improvement hill climbing - exhaustive check of feasible doctors
    private fun heuristic9(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()

        val numUnassignedAssignments = tempSol.unassignedAssignments.size
        if(numUnassignedAssignments == 0) {
            solutionMemory[solutionDestinationIndex] = tempSol
            return tempSol.objectiveValue
        }

        var unassignedIndex = rng.nextInt(numUnassignedAssignments)

        for(x in 1..searchDepth) {
            var improved = false
            val assignmentsToCheck = tempSol.unassignedAssignments.size
            for(y in 1..assignmentsToCheck) {
                val assignment = tempSol.unassignedAssignments[unassignedIndex]
                val feasibleDoctors = tempSol.getFeasibleDoctors(assignment).toList()

                // Tries each feasible doctor to see if the objective function improves
                for(doctor in feasibleDoctors) {
                    val prevValue = tempSol.objectiveValue
                    tempSol.allocateAssignment(assignment, doctor)

                    when(tempSol.objectiveValue < prevValue) {
                        true -> {
                            improved = true
                            unassignedIndex--
                            break
                        }
                        false -> tempSol.deallocateAssignment(assignment)
                    }
                }
                unassignedIndex++
                if(unassignedIndex !in tempSol.unassignedAssignments.indices) unassignedIndex = 0
            }
            if(!improved) break
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    // Best improvement hill climbing - exhaustive check of feasible doctors
    private fun heuristic10(solutionSourceIndex: Int, solutionDestinationIndex: Int): Double {
        val tempSol = solutionMemory[solutionSourceIndex]!!.copy()

        for(x in 1..searchDepth*2) {
            val prevValue = tempSol.objectiveValue
            var bestValue = Double.MAX_VALUE
            var bestAssignment = Int.MAX_VALUE
            var bestDoctor = Int.MAX_VALUE
            val assignmentsToCheck = tempSol.unassignedAssignments.toList()
            for(assignment in assignmentsToCheck) {
                val feasibleDoctors = tempSol.getFeasibleDoctors(assignment).toList()

                for(doctor in feasibleDoctors) {
                    tempSol.allocateAssignment(assignment, doctor)

                    if(tempSol.objectiveValue < bestValue) {
                        bestValue = tempSol.objectiveValue
                        bestAssignment = assignment
                        bestDoctor = doctor
                    }
                    tempSol.deallocateAssignment(assignment)
                }
            }

            if(bestValue < prevValue) {
                tempSol.allocateAssignment(bestAssignment, bestDoctor)
                continue
            }
            // In the case of no improvement, we exit the loop
            break
        }

        solutionMemory[solutionDestinationIndex] = tempSol
        return tempSol.objectiveValue
    }

    override fun copySolution(solutionSourceIndex: Int, solutionDestinationIndex: Int) {
        if(solutionSourceIndex !in solutionMemory.indices || solutionDestinationIndex !in solutionMemory.indices)
            throw Exception("copySolution: Invalid index passed")
        solutionMemory[solutionDestinationIndex] = solutionMemory[solutionSourceIndex]!!.copy()
    }

    override fun getNumberOfInstances(): Int {
        return 4
    }

    override fun bestSolutionToString(): String {
        if(!this::bestSolution.isInitialized)
            throw Exception("bestSolutionToString: No solution has been initialised")
        return solutionString(bestSolution)
    }

    override fun getBestSolutionValue(): Double {
        return bestSolutionValue
    }

    override fun solutionToString(solutionIndex: Int): String {
        when {
            solutionIndex !in solutionMemory.indices -> throw Exception("solutionToString: invalid index passed")
            solutionMemory[solutionIndex] == null -> throw Exception("solutionToString: uninitialised solution passed")
        }
        return solutionString(solutionMemory[solutionIndex]!!)
    }

    private fun solutionString(solution: Solution): String {
        fun findDay(dayID: Int): String {
            return when (dayID % 7) {
                0 -> "Monday\n"
                1 -> "Tuesday\n"
                2 -> "Wednesday\n"
                3 -> "Thursday\n"
                4 -> "Friday\n"
                5 -> "Saturday\n"
                else -> "Sunday\n"
            }
        }

        var string = "Shifts:"
        var dayID = Int.MAX_VALUE
        for(shift in solution.data.shifts) {
            if(shift.day != dayID) {
                dayID = shift.day
                string += "\n\nDay: ${findDay(dayID)}"
            }
            string += "\nShift ${shift.id}\nTime: ${shiftTimes[shift.id]}"
            string += when(shift) {
                is DayShift -> " (Day Shift)\n"
                is NightShift -> " (Night Shift)\n"
                else -> "\n"
            }
            string += "Assignees: ${shift.assignees}\nDoctors that cannot work the shift:\n"
            shift.causesOfInfeasibility.forEach { string += "Doctor ${it.key}: ${it.value}"}
        }

        string += "\nDoctors:"
        for(doctor in solution.data.doctors) {
            string += doctor
            if(doctor.preferences.dayRange)
                string += "Preferred day stretch length: ${doctor.dayRange.min()}-${doctor.dayRange.max()}, Violations: ${solution.dayRangeViolations[doctor.id]}\n"
            if(doctor.preferences.nightRange)
                string += "Preferred night stretch length: ${doctor.nightRange.min()}-${doctor.nightRange.max()}, Violations: ${solution.nightRangeViolations[doctor.id]}\n"
            if(doctor.preferences.shiftsToAvoid)
                string += "Shifts preferably avoided: ${doctor.shiftsToAvoid}, Violations: ${solution.shiftPrefsViolated[doctor.id]}\n"
        }

        return string
    }

    override fun getFunctionValue(solutionIndex: Int): Double {
        if(solutionIndex !in solutionMemory.indices)
            throw Exception("getFunctionValue: Invalid index passed")
        val solution = solutionMemory[solutionIndex]!!
        solution.calculateObjectiveValue()
        return solution.objectiveValue
    }

    override fun compareSolutions(solutionIndex1: Int, solutionIndex2: Int): Boolean {
        if(solutionIndex1 !in solutionMemory.indices || solutionIndex2 !in solutionMemory.indices)
            throw Exception("compareSolutions: Invalid [solutionMemory] index passed")

        // If any shift has different assignees, the solutions are different
        for(x in shifts.indices) {
            if(solutionMemory[solutionIndex1]!!.data.shifts[x].assignees !=
                                solutionMemory[solutionIndex2]!!.data.shifts[x].assignees)
                return false
        }

        return true
    }
}



import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import problemDomain.*
import problemDomain.feasibilityHandling.blockStillPresent
import java.io.File
import java.util.Random
import java.util.Scanner

@DisplayName("problemDomain.Solution Tests")
class SolutionTest {
    private val pd = MDSP(24032024)
    init {
        pd.loadInstance(0)
    }
    private fun getSolution(): Solution { return pd.blankSolution() }

    @Nested
    inner class InitialiseTests {
        @Test
        fun createsSameInitialSolutionGivenTheSameRandomObject() {
            val pd1 = MDSP(24032924)
            pd1.loadInstance(0)
            pd1.initialiseSolution(0) // Calls the solutions initialise() function
            val pd2 = MDSP(24032924)
            pd2.loadInstance(0)
            pd2.initialiseSolution(0)
            pd1.solutionMemory[1] = pd2.solutionMemory[0]!!.copy()
            assertTrue(pd1.compareSolutions(0,1))
        }

        @Test
        fun createsDifferentInitialSolutionGivenADifferentRandomObject() {
            val pd1 = MDSP(24032924)
            pd1.loadInstance(0)
            pd1.initialiseSolution(0) // Calls the solutions initialise() function
            val pd2 = MDSP(30022924)
            pd2.loadInstance(0)
            pd2.initialiseSolution(0)
            pd1.solutionMemory[1] = pd2.solutionMemory[0]!!.copy()
            assertFalse(pd1.compareSolutions(0,1))
        }
    }

    @Nested
    inner class ObjectiveFunctionTests() {
        private val assignments = listOf(
            Assignment(0, 0, "any", emptySet()),
            Assignment(1, 0, "any", emptySet()),
            Assignment(2, 1, "any", emptySet()),
            Assignment(3, 1, "any", emptySet())
        )
        private val shifts = listOf(
            DayShift(0, intArrayOf(0,1), emptySet(), emptyList(), emptyList(), emptySet(), 0, mutableSetOf(0,1), 8.0),
            DayShift(1, intArrayOf(2,3), emptySet(), emptyList(), emptyList(), emptySet(), 0, mutableSetOf(0,1), 8.0)
        )
        private val day = Day(0, listOf(0,1), emptyList(), emptyList(), emptyList())
        private val doctors = listOf(
            MiddleGrade(0, "senior", 16.0, 2, 0, 1.0, setOf(1), 1..1, 1..1),
            MiddleGrade(1, "senior", 16.0, 2, 0, 1.0, setOf(0), 1..1, 1..1)
        )
        private val solutionData = SolutionData(assignments, shifts, doctors, listOf(day))
        private fun getSolutionData(): SolutionData { return solutionData.copy() }

        @Test
        fun valueDecreasesWithAssignmentAllocation() {
            val sol = Solution(Random(20023), getSolutionData())
            sol.calculateObjectiveValue()
            val initialValue = sol.objectiveValue
            sol.allocateAssignment(0, 0)
            val secondValue = sol.objectiveValue
            sol.allocateAssignment(3, 1)
            assertTrue(sol.objectiveValue < secondValue && secondValue < initialValue)
        }

        @Test
        fun valueIncreasesWithAssignmentDeallocation() {
            val sol = Solution(Random(20023), getSolutionData())
            sol.allocateAssignment(0, 0)
            sol.allocateAssignment(3, 1)
            sol.calculateObjectiveValue()
            val initialValue = sol.objectiveValue
            sol.deallocateAssignment(0)
            val secondValue = sol.objectiveValue
            sol.deallocateAssignment(3)
            assertTrue(sol.objectiveValue > secondValue && secondValue > initialValue)
        }

        @Test
        fun valueDecreasesWhenPreferencesAreMet() {
            val sol = Solution(Random(20023), getSolutionData())
            sol.allocateAssignment(0, 1)
            sol.allocateAssignment(3, 0)
            sol.calculateObjectiveValue()
            val initialValue = sol.objectiveValue
            sol.deallocateAssignment(0)
            sol.deallocateAssignment(3)
            sol.allocateAssignment(0, 0)
            sol.allocateAssignment(3, 1)
            assertTrue(sol.objectiveValue < initialValue)
        }

        @Test
        fun valueIncreasesWithPreferenceDisparity() {
            val sol = Solution(Random(20023), getSolutionData())
            sol.allocateAssignment(0, 0)
            sol.allocateAssignment(3, 1)
            sol.calculateObjectiveValue()
            val initialValue = sol.objectiveValue
            sol.allocateAssignment(1, 1)
            assertTrue(sol.objectiveValue > initialValue)
        }
    }

    @Nested
    inner class AllocateAssignmentTests {
        @Test
        fun returnsFalseForInfeasibleDoctor() {
            val solution = getSolution()
            assertFalse(solution.allocateAssignment(0, 2))
        }

        @Test
        fun updatesRelevantDataAndReturnsTrueForFeasibleDoctor() {
            val solution = getSolution()
            assertTrue(solution.allocateAssignment(0, 0))
            assertEquals(1, solution.assignedAssignments.size)
            assertEquals(0, solution.data.assignments[0].assignee)
            assertEquals(1, solution.data.doctors[0].assignedAssignments.size)
            assertEquals(1, solution.data.doctors[0].assignedShifts.size)
            assertFalse(solution.unassignedAssignments.contains(0))
            assertFalse(solution.data.shifts[0].feasibleDoctors.contains(0))
            assertTrue(solution.data.shifts[0].assignees.contains(0))
        }
    }

    @Nested
    inner class DeallocateAssignmentTests {
        @Test
        fun throwExceptionForUnassignedAssignment() {
            val solution = getSolution()
            assertThrows<Exception> { solution.deallocateAssignment(0) }
        }

        @Test
        fun returnsFalseForNightShiftInMiddleOfBlock() {
            val solution = getSolution()
            solution.allocateAssignment(3, 2)
            println(solution.allocateAssignment(7, 2))
            solution.allocateAssignment(11, 2)
            assertFalse(solution.deallocateAssignment(7))
        }

        @Test
        fun updatesRelevantDataAndReturnsTrueForValidDeallocation() {
            val solution = getSolution()
            solution.allocateAssignment(0, 0)
            assertTrue(solution.deallocateAssignment(0))
            assertEquals(0, solution.assignedAssignments.size)
            assertNull(solution.data.assignments[0].assignee)
            assertEquals(0, solution.data.doctors[0].assignedAssignments.size)
            assertEquals(0, solution.data.doctors[0].assignedShifts.size)
            assertTrue(solution.unassignedAssignments.contains(0))
            assertTrue(solution.data.shifts[0].feasibleDoctors.contains(0))
            assertFalse(solution.data.shifts[0].assignees.contains(0))
        }
    }

    @Nested
    inner class FeasibilityTests {

        @Test
        fun noErrorsForTestInstance1() {
            val problem = MDSP(20232023)
            problem.loadInstance(0)

            for(i in 1..3) {
                val solution = problem.blankSolution()
                val log = File("src/test/resources/test_instance1_log-$i.txt")
                val scanner = Scanner(log)

                var errors = 0
                while(scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    val tokens = line.split(" ")
                    val doctorID = when(tokens[0]) {
                        "al" -> {
                            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
                            tokens[2].toInt()
                        }
                        "de" -> {
                            val assignment = solution.data.assignments[tokens[1].toInt()]
                            val doctor = assignment.assignee!!
                            solution.deallocateAssignment(tokens[1].toInt())
                            doctor
                        }
                        else -> return
                    }

                    // FeasibilityChecks
                    for(testShift in solution.data.shifts) {
                        for(assignee in testShift.assignees)
                            if(testShift.causesOfInfeasibility[assignee] != null)
                                errors++
                        for(assignmentID in testShift.assignmentIDs) {
                            val a = solution.data.assignments[assignmentID].assignee
                            if (a != null && !testShift.assignees.contains(a))
                                errors++
                        }
                    }
                    val doctor = solution.data.doctors[doctorID]
                    for(block in doctor.blocksOfDays.values) {
                        for(testShift in block.shiftsMadeInfeasible) {
                            val shiftToCheck = solution.data.shifts[testShift]
                            if(shiftToCheck.causesOfInfeasibility[doctorID]!!.cause == Cause.Training ||
                                shiftToCheck.causesOfInfeasibility[doctorID]!!.cause == Cause.Leave)
                                continue
                            if(!blockStillPresent(shiftToCheck, block.id, doctorID))
                                errors++
                        }
                    }
                }
            }
        }

        @Test
        fun noErrorsForTestInstance2() {
            val problem = MDSP(20232023)
            problem.loadInstance(1)

            for(i in 1..3) {
                val solution = problem.blankSolution()
                val log = File("src/test/resources/test_instance2_log-$i.txt")
                val scanner = Scanner(log)

                var errors = 0
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    val tokens = line.split(" ")
                    val doctorID = when (tokens[0]) {
                        "al" -> {
                            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
                            tokens[2].toInt()
                        }

                        "de" -> {
                            val assignment = solution.data.assignments[tokens[1].toInt()]
                            val doctor = assignment.assignee!!
                            solution.deallocateAssignment(tokens[1].toInt())
                            doctor
                        }

                        else -> return
                    }

                    // FeasibilityChecks
                    for (testShift in solution.data.shifts) {
                        for (assignee in testShift.assignees)
                            if (testShift.causesOfInfeasibility[assignee] != null)
                                errors++
                        for (assignmentID in testShift.assignmentIDs) {
                            val a = solution.data.assignments[assignmentID].assignee
                            if (a != null && !testShift.assignees.contains(a))
                                errors++
                        }
                    }
                    val doctor = solution.data.doctors[doctorID]
                    for (block in doctor.blocksOfDays.values) {
                        for (testShift in block.shiftsMadeInfeasible) {
                            val shiftToCheck = solution.data.shifts[testShift]
                            if (shiftToCheck.causesOfInfeasibility[doctorID]!!.cause == Cause.Training ||
                                shiftToCheck.causesOfInfeasibility[doctorID]!!.cause == Cause.Leave
                            )
                                continue
                            if (!blockStillPresent(shiftToCheck, block.id, doctorID))
                                errors++
                        }
                    }
                }
            }
        }
    }

}
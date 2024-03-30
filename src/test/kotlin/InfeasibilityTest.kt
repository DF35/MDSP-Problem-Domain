import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import problemDomain.Cause
import problemDomain.DayShift
import problemDomain.NightShift
import problemDomain.Source
import java.util.*

@DisplayName("Tests for Shift Infeasibility")
class InfeasibilityTest {
    val generator = Generator()
    @Nested
    inner class BasicInfeasibilityTests {
        @Test
        fun relevantShiftsMadeInfeasibleForSingleDayShift() {
            val solution = generator.generateSolution(7)
            val shift = solution.data.shifts[2]
            solution.allocateAssignment(4, 0)

            if(shift is DayShift) for(shiftID in shift.shiftsWithin11Hours.union(shift.nightShifts48HoursBefore)) {
                val testShift = solution.data.shifts[shiftID]
                assertFalse(testShift.feasibleDoctors.contains(0))
                assertTrue(testShift.causesOfInfeasibility[0]!!.cause == Cause.Rest)
                assertTrue(testShift.causesOfInfeasibility[0]!!.sources.contains(Source.ShiftWorked(2)))
                assertEquals(1, testShift.causesOfInfeasibility[0]!!.sources.size)
            }
        }

        @Test
        fun relevantShiftsMadeFeasibleAgainForSingleDayShift() {
            val solution = generator.generateSolution(7)
            val shift = solution.data.shifts[2]
            solution.allocateAssignment(4, 0)
            solution.deallocateAssignment(4)

            if(shift is DayShift) for(shiftID in shift.shiftsWithin11Hours.union(shift.nightShifts48HoursBefore)) {
                val testShift = solution.data.shifts[shiftID]
                assertTrue(testShift.feasibleDoctors.contains(0))
                assertNull(testShift.causesOfInfeasibility[0])
            }
        }

        @Test
        fun relevantShiftsMadeInfeasibleForSingleNightShift() {
            val solution = generator.generateSolution(7)
            val shift = solution.data.shifts[3]
            solution.allocateAssignment(6, 0)

            if(shift is NightShift) for(shiftID in shift.shiftsWithin11Hours.union(shift.dayShifts48HoursAfter)) {
                val testShift = solution.data.shifts[shiftID]
                assertFalse(testShift.feasibleDoctors.contains(0))
                assertTrue(testShift.causesOfInfeasibility[0]!!.cause == Cause.Rest)
                assertTrue(testShift.causesOfInfeasibility[0]!!.sources.contains(Source.ShiftWorked(3)))
                assertEquals(1, testShift.causesOfInfeasibility[0]!!.sources.size)
            }
        }

        @Test
        fun relevantShiftsMadeFeasibleAgainForSingleNightShift() {
            val solution = generator.generateSolution(7)
            val shift = solution.data.shifts[3]
            solution.allocateAssignment(6, 0)
            solution.deallocateAssignment(6)

            if(shift is NightShift) for (shiftID in shift.shiftsWithin11Hours.union(shift.dayShifts48HoursAfter)) {
                val testShift = solution.data.shifts[shiftID]
                assertTrue(testShift.feasibleDoctors.contains(0))
                assertNull(testShift.causesOfInfeasibility[0])
            }
        }
    }

    @Nested
    inner class NightInfeasibilityTests {
        @Test
        fun doctorsWorkingNightUpdatedCorrectly() {
            val solution = generator.generateSolution(1)
            assertEquals(0, solution.data.days[0].doctorsWorkingNight.size)
            solution.allocateAssignment(2, 0)
            assertEquals(1, solution.data.days[0].doctorsWorkingNight.size)
            assertEquals(1, solution.data.days[0].doctorsWorkingNight[0])
            solution.deallocateAssignment(2)
            assertEquals(0, solution.data.days[0].doctorsWorkingNight.size)
            assertNull(solution.data.days[0].doctorsWorkingNight[0])
        }

        @Test
        fun relevantShiftsMadeInfeasibleForSingleNightWorked() {
            val solution = generator.generateSolution(7)
            solution.allocateAssignment(10, 0)
            assertFalse(solution.data.shifts[1].feasibleDoctors.contains(0))
            assertTrue(solution.data.shifts[3].feasibleDoctors.contains(0))
            assertFalse(solution.data.shifts[9].feasibleDoctors.contains(0))
            assertTrue(solution.data.shifts[7].feasibleDoctors.contains(0))
        }

        @Test
        fun relevantShiftsMadeFeasibleAgainForSingleNightWorked() {
            val solution = generator.generateSolution(7)
            solution.allocateAssignment(10, 0)
            solution.deallocateAssignment(10)
            assertTrue(solution.data.shifts[1].feasibleDoctors.contains(0))
            assertTrue(solution.data.shifts[3].feasibleDoctors.contains(0))
            assertTrue(solution.data.shifts[9].feasibleDoctors.contains(0))
            assertTrue(solution.data.shifts[7].feasibleDoctors.contains(0))
        }

        @Test
        fun feasibilityCorrectlyAdjustedWhenRowOfNightsAddedTo() {
            val solution = generator.generateSolution(7)
            solution.allocateAssignment(10, 0)
            solution.allocateAssignment(14, 0)
            assertFalse(solution.data.shifts[1].feasibleDoctors.contains(0))
            assertTrue(solution.data.shifts[3].feasibleDoctors.contains(0))
            assertTrue(solution.data.shifts[9].feasibleDoctors.contains(0))
            assertFalse(solution.data.shifts[11].feasibleDoctors.contains(0))
        }

        @Test
        fun feasibilityCorrectlyAdjustedWhenRowOfNightsRemovedFrom() {
            val solution = generator.generateSolution(7)
            solution.allocateAssignment(10, 0)
            solution.allocateAssignment(14, 0)
            solution.deallocateAssignment(10)
            assertTrue(solution.data.shifts[1].feasibleDoctors.contains(0))
            assertFalse(solution.data.shifts[3].feasibleDoctors.contains(0))
            assertTrue(solution.data.shifts[9].feasibleDoctors.contains(0))
            assertFalse(solution.data.shifts[11].feasibleDoctors.contains(0))
        }

        @Test
        fun feasibilityCorrectlyUpdatedForRowOfFourNights() {
            val solution = generator.generateSolution(7)
            solution.allocateAssignment(6, 0)
            solution.allocateAssignment(10, 0)
            solution.allocateAssignment(14, 0)
            solution.allocateAssignment(18, 0)

            val firstDay = solution.data.days[0]
            val lastShift = solution.data.shifts[9]
            for(shiftID in firstDay.getNightShifts().union(lastShift.shifts48HoursAfter))
                assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
        }

        @Test
        fun feasibilityCorrectlyUpdatedWhenRowOfFourNightsRemoved() {
            val solution = generator.generateSolution(7)
            solution.allocateAssignment(6, 0)
            solution.allocateAssignment(10, 0)
            solution.allocateAssignment(14, 0)
            solution.allocateAssignment(18, 0)
            solution.deallocateAssignment(18)

            val firstDay = solution.data.days[0]
            val lastShift = solution.data.shifts[7]
            if(lastShift !is NightShift) return
            for(shiftID in firstDay.getNightShifts())
                assertTrue(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
            for(shiftID in lastShift.shiftsWithin11Hours.union(lastShift.dayShifts48HoursAfter))
                assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
        }

    }

    @Nested
    inner class WeekendFeasibilityTests() {
        @Test
        fun correctShiftsMadeInfeasibleForWeekendWorked() {
            /*
             * Tests for working on an overlapping night shift on a Friday, any shift on a
             * Saturday, and any shift on a Sunday
             */
            for(assignment in listOf(75, 76, 77, 78, 79, 80, 81, 82, 83)) {
                val solution = generator.generateSolution(35)
                val days = solution.data.days
                solution.allocateAssignment(assignment, 0)
                // All shifts made infeasible by working on this weekend
                val infeasibleShifts = mutableListOf<Int>()
                for(weekend in listOf(0, 1, 3, 4)) {
                    infeasibleShifts.addAll(days[7*weekend+4].overlappingNightShifts)
                    infeasibleShifts.addAll(days[7*weekend+5].getShifts())
                    infeasibleShifts.addAll(days[7*weekend+6].getShifts())
                }

                for(shift in infeasibleShifts)
                    assertFalse(solution.data.shifts[shift].feasibleDoctors.contains(0))
            }
        }

        @Test
        fun shiftsMadeFeasibleAgainWhenWeekendNoLongerWorked() {
            for(assignment in listOf(75, 76, 77, 78, 79, 80, 81, 82, 83)) {
                val solution = generator.generateSolution(35)
                val days = solution.data.days
                solution.allocateAssignment(assignment, 0)
                // All shifts made infeasible by working on this weekend
                val infeasibleShifts = mutableListOf<Int>()
                for(weekend in listOf(0, 1, 3, 4)) {
                    infeasibleShifts.addAll(days[7*weekend+4].overlappingNightShifts)
                    infeasibleShifts.addAll(days[7*weekend+5].getShifts())
                    infeasibleShifts.addAll(days[7*weekend+6].getShifts())
                }

                solution.deallocateAssignment(assignment)
                for(shift in infeasibleShifts)
                    assertTrue(solution.data.shifts[shift].feasibleDoctors.contains(0))
            }
        }
    }

    @Nested
    inner class FeasibilityDayAddedTests {
        @Test
        fun successfullyCreatesNewBlock() {
            val solution = generator.generateSolution(7)
            val doctor = solution.data.doctors[0]
            assertEquals(0, doctor.blocksOfDays.size)
            solution.allocateAssignment(0, 0)
            assertEquals(1, doctor.blocksOfDays.size)
            assertTrue(doctor.blocksOfDays[0]!!.items.contains(0))
        }

        @Test
        fun addsIfBlockToLeft() {
            val solution = generator.generateSolution(7)
            val doctor = solution.data.doctors[0]
            solution.allocateAssignment(4, 0)
            solution.allocateAssignment(0, 0)
            assertEquals(1, doctor.blocksOfDays.size)
            assertEquals(2, doctor.blocksOfDays[0]!!.items.size)
            assertTrue(doctor.blocksOfDays[0]!!.items.containsAll(listOf(0,1)))
        }

        @Test
        fun addsIfBlockToRight() {
            val solution = generator.generateSolution(7)
            val doctor = solution.data.doctors[0]
            solution.allocateAssignment(0, 0)
            solution.allocateAssignment(4, 0)
            assertEquals(1, doctor.blocksOfDays.size)
            assertEquals(2, doctor.blocksOfDays[0]!!.items.size)
            assertTrue(doctor.blocksOfDays[0]!!.items.containsAll(listOf(0,1)))
        }

        @Test
        fun mergesIfDayAddedBetweenTwoBlocks() {
            val solution = generator.generateSolution(7)
            val doctor = solution.data.doctors[0]
            solution.allocateAssignment(0, 0)
            solution.allocateAssignment(8, 0)
            assertEquals(2, doctor.blocksOfDays.size)
            solution.allocateAssignment(4, 0)
            assertEquals(1, doctor.blocksOfDays.size)
            assertTrue(doctor.blocksOfDays[1]!!.items.containsAll(listOf(0,1,2)))
        }
    }

    @Nested
    inner class FeasibilityDayRemovedTests {
        @Test
        fun deletesBlockIfDayWasLastInIt() {
            val solution = generator.generateSolution(7)
            val doctor = solution.data.doctors[0]
            solution.allocateAssignment(0, 0)
            solution.deallocateAssignment(0)
            assertEquals(0, doctor.blocksOfDays.size)
        }

        @Test
        fun successfullyRemovesFirstDayOfBlock() {
            val solution = generator.generateSolution(7)
            val doctor = solution.data.doctors[0]
            solution.allocateAssignment(0, 0)
            solution.allocateAssignment(4, 0)
            solution.allocateAssignment(8, 0)

            assertEquals(3, doctor.blocksOfDays[0]!!.items.size)
            solution.deallocateAssignment(0)
            assertEquals(2, doctor.blocksOfDays[0]!!.items.size)
            assertFalse(doctor.blocksOfDays[0]!!.items.contains(0))
            solution.deallocateAssignment(4)
            assertEquals(1, doctor.blocksOfDays[0]!!.items.size)
            assertFalse(doctor.blocksOfDays[0]!!.items.contains(1))
        }

        @Test
        fun successfullyRemovesLastDayOfBlock() {
            val solution = generator.generateSolution(7)
            val doctor = solution.data.doctors[0]
            solution.allocateAssignment(0, 0)
            solution.allocateAssignment(4, 0)
            solution.allocateAssignment(8, 0)

            assertEquals(3, doctor.blocksOfDays[0]!!.items.size)
            solution.deallocateAssignment(8)
            assertEquals(2, doctor.blocksOfDays[0]!!.items.size)
            assertFalse(doctor.blocksOfDays[0]!!.items.contains(2))
            solution.deallocateAssignment(4)
            assertEquals(1, doctor.blocksOfDays[0]!!.items.size)
            assertFalse(doctor.blocksOfDays[0]!!.items.contains(1))
        }

        @Test
        fun splitsBlockIfDayRemovedInMiddle() {
            val solution = generator.generateSolution(7)
            val doctor = solution.data.doctors[0]
            solution.allocateAssignment(0, 0)
            solution.allocateAssignment(4, 0)
            solution.allocateAssignment(8, 0)
            solution.deallocateAssignment(4)

            assertEquals(2, doctor.blocksOfDays.size)
            assertTrue(doctor.blocksOfDays[0]!!.items.contains(0))
            assertFalse(doctor.blocksOfDays[0]!!.items.contains(1))
            assertFalse(doctor.blocksOfDays[0]!!.items.contains(2))

            assertTrue(doctor.blocksOfDays[1]!!.items.contains(2))
            assertFalse(doctor.blocksOfDays[1]!!.items.contains(1))
            assertFalse(doctor.blocksOfDays[1]!!.items.contains(0))
        }
    }

    @Nested
    inner class InfeasibilityIdentificationTests {
        /*
         * Used to ensure that sources of infeasibility are detected no matter the order
         * of assignment of the days in question. Credit to Juan Rada, who provided the code
         * in this blogpost: https://medium.com/@jcamilorada/recursive-permutations-calculation-algorithm-in-kotlin-86233a0a2ee1
         */
        private fun permutations(input: List<Int>): List<List<Int>> {
            val solutions = mutableListOf<List<Int>>()
            permutationsRecursive(input, 0, solutions)
            return solutions
        }
        private fun permutationsRecursive(input: List<Int>, index: Int, answers: MutableList<List<Int>>) {
            if (index == input.lastIndex) answers.add(input.toList())
            for (i in index .. input.lastIndex) {
                Collections.swap(input, index, i)
                permutationsRecursive(input, index + 1, answers)
                Collections.swap(input, i, index)
            }
        }

        /*
         * We only need to test for the detection of each scenario, as the function is called
         * before and after the addition or removal of a day worked, so there is no need to
         * test whether sources of infeasibility are converted into others by removals
         */
        @Test
        fun correctlyIdentifiesRowOfSeven() {
            val toAssign = listOf(8, 12, 16, 20, 24, 28, 32)

            for(assignments in permutations(toAssign)) {
                val solution = generator.generateSolution(11)

                for(assignment in assignments)
                    solution.allocateAssignment(assignment, 0)

                val days = solution.data.days
                val infeasibleShifts = days[0].overlappingNightShifts.union(
                    days[1].getShifts().union(solution.data.shifts[16].shifts48HoursAfter)
                )

                for (shiftID in infeasibleShifts)
                    assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
            }
        }

        @Test
        fun correctlyIdentifiesRowOfSixOverlap() {
            val toAssign = listOf(8, 12, 16, 20, 24, 28)

            for(assignments in permutations(toAssign)) {
                val solution = generator.generateSolution(9)

                for(assignment in assignments)
                    solution.allocateAssignment(assignment, 0)

                val days = solution.data.days
                val infeasibleShifts =
                    days[0].overlappingNightShifts.union(days[8].overlappingNightShifts)

                for(shiftID in infeasibleShifts)
                    assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
            }
        }

        @Test
        fun correctlyIdentifiesWouldCauseRowTooLarge() {
            val relevantAssignments = listOf(0, 4, 8, 12, 16, 20, 24, 28)

            for(indexToRemove in 1..6) {
                val toRemove = relevantAssignments[indexToRemove]
                val toAssign = relevantAssignments.filter { it != toRemove }

                for(assignments in permutations(toAssign)) {
                    val solution = generator.generateSolution(8)

                    for(assignment in assignments)
                        solution.allocateAssignment(assignment, 0)

                    for(shiftID in solution.data.days[indexToRemove].getShifts())
                        assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
                }
            }
        }

        @Test
        fun correctlyIdentifiesInsufficientRest() {
            // Covers both InsufficientRest and InsufficientRestMid
            val relevantAssignments = listOf(0, 4, 8, 12, 16, 20, 24, 32)

            for(indexToRemove in 0..6) {
                val toRemove = relevantAssignments[indexToRemove]
                val toAssign = relevantAssignments.filter { it != toRemove }

                for(assignments in permutations(toAssign)) {
                    val solution = generator.generateSolution(9)

                    for(assignment in assignments)
                        solution.allocateAssignment(assignment, 0)

                    for(shiftID in solution.data.days[indexToRemove].getShifts())
                        assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
                }
            }
        }

        @Test
        fun correctlyIdentifiesInsufficientRestOverlap() {
            val toAssign = listOf(8, 12, 16, 20, 24, 32)
            for(assignments in permutations(toAssign)) {
                val solution = generator.generateSolution(9)

                for(assignment in assignments)
                    solution.allocateAssignment(assignment, 0)

                for(shiftID in solution.data.days[0].overlappingNightShifts)
                    assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
            }

            val toAssign2 = listOf(0, 4, 8, 12, 16, 32)
            for(assignments in permutations(toAssign2)) {
                val solution = generator.generateSolution(9)

                for(assignment in assignments)
                    solution.allocateAssignment(assignment, 0)

                for(shiftID in solution.data.days[6].overlappingNightShifts)
                    assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
            }
        }

        @Test
        fun correctlyIdentifiesInsufficientRestMidOverlap() {
            val relevantAssignments = listOf(0, 4, 8, 12, 16, 20, 24, 32)

            for(indexToRemove in 1..4) {
                val toRemove = relevantAssignments[indexToRemove]
                val toAssign = relevantAssignments.filter { it != toRemove && it != toRemove+1 }

                for(assignments in permutations(toAssign)) {
                    val solution = generator.generateSolution(9)

                    for(assignment in assignments)
                        solution.allocateAssignment(assignment, 0)

                    for(shiftID in solution.data.days[indexToRemove].overlappingNightShifts)
                        assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
                }
            }
        }

        @Test
        fun correctlyIdentifiesWouldCauseRowTooLargeOverlap() {
            val relevantAssignments = listOf(0, 4, 8, 12, 16, 20, 24, 28)

            for(indexToRemove in 1..5) {
                val toRemove = relevantAssignments[indexToRemove]
                val toAssign = relevantAssignments.filter { it != toRemove && it != toRemove+1 }

                for(assignments in permutations(toAssign)) {
                    val solution = generator.generateSolution(8)

                    for(assignment in assignments)
                        solution.allocateAssignment(assignment, 0)

                    for(shiftID in solution.data.days[indexToRemove].overlappingNightShifts)
                        assertFalse(solution.data.shifts[shiftID].feasibleDoctors.contains(0))
                }
            }
        }
    }
}
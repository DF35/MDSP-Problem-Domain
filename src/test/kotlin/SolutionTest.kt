import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import problemDomain.*
import java.util.Random

@DisplayName("problemDomain.Solution Tests")
class SolutionTest {
    val generator = Generator()
    val random = Random()

    /*fun newSolution(generator: Generator): Solution {
        val (shifts, days) = generator.generateShiftsAndDays(21)
        val assignments = generator.generateAssignments(shifts)
        return Solution(random, assignments, shifts, days, listOf(),47, 15, 6)
    }

    @Nested
    inner class AllocateAssignmentTests {
        @Test
        fun throwsExceptionForDoctorAbsentFromFeasibleDoctors() {
            val solution = Solution(
                random,
                listOf(Assignment(0, 0, "Any", emptySet())),
                listOf(DayShift(0, intArrayOf(0), emptySet(), emptySet(), 0, mutableSetOf(), 10)),
                listOf(Day(0, setOf(0), emptySet())), listOf(), 47, 15, 6)
            assertThrows<Exception> { solution.allocateAssignment(1, 0) }
        }

        @Test
        fun throwsExceptionForDoctorInInfeasibleDoctors() {
            val solution = Solution(
                random,
                listOf(Assignment(0, 0, "Any", setOf(0))),
                listOf(DayShift(0, intArrayOf(0), emptySet(),  emptySet(), 0, mutableSetOf(0), 10)),
                listOf(Day(0, setOf(0), emptySet())), listOf(),47, 15, 6)
            assertThrows<Exception> { solution.allocateAssignment(1, 0) }
        }

        @Test
        fun doctorIsRemovedFromFeasibleDoctorsAndUnassignedAssignmentsUpdated() {
            val solution = Solution(
                random,
                listOf(Assignment(0, 0, "any", emptySet())),
                listOf(DayShift(0, intArrayOf(0), emptySet(), emptySet(), 0, mutableSetOf(0), 10)),
                listOf(Day(0, setOf(0), emptySet())),
                listOf(MiddleGrade(0, "senior", 47.00, 15, 6, 1.00)),
                47, 15, 6)

            assertTrue(solution.shifts[0].feasibleDoctors.contains(0))
            assertTrue(solution.unassignedAssignments.contains(0))
            solution.allocateAssignment(0, 0)
            assertFalse(solution.shifts[0].feasibleDoctors.contains(0))
            assertFalse(solution.unassignedAssignments.contains(0))
        }

        @Test
        fun correctAssigneeIsSet() {
            val solution = newSolution(generator)
            solution.allocateAssignment(0,0)
            assertEquals(0, solution.assignments[0].assignee)
        }

        @Nested
        inner class DayFeasibilityTests {
            @Test
            fun relevantShiftsUpdatedForOneAssignedShift() {
                val solution = newSolution(generator)

                solution.allocateAssignment(12, 0)
                val shift = solution.shifts[6]
                if(shift is DayShift) {
                    val infeasibleShifts = shift.shiftsWithin11Hours.union(shift.nightShifts48HoursBefore)
                    for (shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(0))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[0]!!.cause)
                    }

                    val numShifts = solution.shifts.size
                    val shiftIds = 0..<numShifts
                    for(shiftID in shiftIds.filterNot { infeasibleShifts.contains(it) || it == 6 }) {
                        assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(0))
                        assertNull(solution.shifts[shiftID].causesOfInfeasibility[0])
                    }
                }
            }

            @Test
            fun correctFeasibilityForDaysWorkedInARow() {
                val solution = newSolution(generator)

                val infeasibleShifts = mutableSetOf<Int>()
                val allocatedShifts = mutableSetOf<Int>()
                val weekendShifts = setOf(24, 25, 26, 27, 38, 39, 40, 41)

                // 1 to 6 days in a row
                for(day in 1..6){
                    val assignment = solution.assignments[4*day]
                    allocatedShifts.add(assignment.shift)
                    val shift = solution.shifts[assignment.shift]
                    solution.allocateAssignment(4*day, 3)

                    if(shift is DayShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.nightShifts48HoursBefore))

                    for (shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }

                    val numShifts = solution.shifts.size
                    val shiftIds = 0..<numShifts
                    for(shiftID in shiftIds.filterNot { infeasibleShifts.contains(it) || allocatedShifts.contains(it) || weekendShifts.contains(it) }) {
                        assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertNull(solution.shifts[shiftID].causesOfInfeasibility[3])
                    }
                }

                // 7-th day in a row
                solution.allocateAssignment(28, 3)
                allocatedShifts.add(14)
                val shift = solution.shifts[14]
                if(shift is DayShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.nightShifts48HoursBefore))

                val infeasibleDays = setOf(0, 8, 9)
                for(day in infeasibleDays) {
                    assertNotNull(solution.days[day].causesOfInfeasibility[3])
                    for (shiftId in solution.days[day].getShifts())
                        infeasibleShifts.add(shiftId)
                }

                for(shiftID in infeasibleShifts) {
                    assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                    assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                }

                val numShifts = solution.shifts.size
                val shiftIds = 0..<numShifts
                for(shiftID in shiftIds.filterNot { infeasibleShifts.contains(it) || allocatedShifts.contains(it) || weekendShifts.contains(it) }) {
                    assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(3))
                    assertNull(solution.shifts[shiftID].causesOfInfeasibility[3])
                }
            }

            @Test
            fun correctFeasibilityForRowTooLarge() {
                val days = listOf(0,1,2,3,4,5,6,7)

                // Left block assigned first
                for(day in 1..6){
                    val solution = newSolution(generator)
                    val (left, right) = days.filterNot { it == day }.partition { it < day }
                    for(l in left)
                        solution.allocateAssignment(4*l, 3)
                    for(r in right) {
                        solution.allocateAssignment(4 * r, 3)
                    }

                    for(shiftId in solution.days[day].getShifts()) {
                        assertFalse(solution.shifts[shiftId].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftId].causesOfInfeasibility[3]!!.cause)
                    }

                    assertNotNull(solution.days[day].causesOfInfeasibility[3])
                }

                // Right block assigned first
                for(day in 1..6) {
                    val solution = newSolution(generator)
                    val (left, right) = days.filterNot { it == day }.partition { it < day }
                    for(r in right) {
                        solution.allocateAssignment(4 * r, 3)
                    }
                    for(l in left)
                        solution.allocateAssignment(4*l, 3)

                    for(shiftId in solution.days[day].getShifts()) {
                        assertFalse(solution.shifts[shiftId].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftId].causesOfInfeasibility[3]!!.cause)
                    }

                    assertNotNull(solution.days[day].causesOfInfeasibility[3])
                }
            }

            @Test
            fun correctInFeasibilityForInsufficientRest() {
                val days = listOf(0,1,2,3,4,5,6)

                // Blocks are allocated first
                for(day in 0..5) {
                    val solution = newSolution(generator)
                    val (left, right) = days.filterNot { it == day }.partition { it < day }
                    for (l in left)
                        solution.allocateAssignment(4 * l, 3)
                    for (r in right)
                        solution.allocateAssignment(4 * r, 3)

                    solution.allocateAssignment(8 * 4, 3)
                    for(shiftId in solution.days[day].getShifts()) {
                        assertFalse(solution.shifts[shiftId].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftId].causesOfInfeasibility[3]!!.cause)
                    }
                }

                // Day preventing rest is allocated first
                for(day in 0..5) {
                    val solution = newSolution(generator)

                    solution.allocateAssignment(8 * 4, 3)
                    val (left, right) = days.filterNot { it == day }.partition { it < day }
                    for (l in left)
                        solution.allocateAssignment(4 * l, 3)
                    for (r in right)
                        solution.allocateAssignment(4 * r, 3)

                    for(shiftId in solution.days[day].getShifts()) {
                        assertFalse(solution.shifts[shiftId].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftId].causesOfInfeasibility[3]!!.cause)
                    }
                }

                // Day preventing rest is allocated after first block
                for(day in 0..5) {
                    val solution = newSolution(generator)

                    val (left, right) = days.filterNot { it == day }.partition { it < day }
                    for (l in left)
                        solution.allocateAssignment(4 * l, 3)
                    solution.allocateAssignment(8 * 4, 3)
                    for (r in right)
                        solution.allocateAssignment(4 * r, 3)

                    for(shiftId in solution.days[day].getShifts()) {
                        assertFalse(solution.shifts[shiftId].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftId].causesOfInfeasibility[3]!!.cause)
                    }
                }
            }

            @Test
            fun feasibilityIsCorrectlyUpdatedForWorkedWeekend() {
                val weekendDays = listOf(5,6,12,13,19,20)

                for(day in weekendDays) {
                    val addedValues = when {
                        //Day is a Sunday
                        (day + 1) % 7 == 0 -> intArrayOf(6, 7, 13, 14, -7, -8, -14, -15)
                        //Day is a Saturday
                        (day + 2) % 7 == 0 -> intArrayOf(7, 8, 14, 15, -6, -7, -13, -14)
                        else -> return
                    }

                    val solution = newSolution(generator)
                    solution.allocateAssignment(4*day, 3)
                    val relevantWeekendDays = addedValues.map { it + day }.filter { it in solution.days.indices }

                    val shift = solution.shifts[solution.assignments[4*day].shift]
                    val infeasibleShifts = mutableSetOf<Int>()
                    if(shift is DayShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.nightShifts48HoursBefore))

                    for(dayID in relevantWeekendDays)
                        for(shiftId in solution.days[dayID].getShifts())
                            infeasibleShifts.add(shiftId)

                    for(shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }

                    for(shiftId in (0..<solution.shifts.size).filterNot { infeasibleShifts.contains(it) || it == day*2 }) {
                        assertTrue(solution.shifts[shiftId].feasibleDoctors.contains(3))
                        assertNull(solution.shifts[shiftId].causesOfInfeasibility[3])
                    }
                }
            }
        }

        @Nested
        inner class NightFeasibilityTests {
            @Test
            fun relevantShiftsUpdatedForOneAssignedShift() {
                val nightsToCheck = listOf(0,1,2,18,19,20)

                for(day in nightsToCheck) {
                    val solution = newSolution(generator)
                    val infeasibleShifts = mutableSetOf<Int>()
                    val shift = solution.shifts[day * 2 + 1]
                    val twoDaysBefore = shift.day - 2
                    val twoDaysAfter = shift.day + 2

                    solution.allocateAssignment((day * 2 + 1) * 2, 3)
                    if (shift is NightShift) infeasibleShifts.addAll(shift.dayShifts48HoursAfter.union(shift.shiftsWithin11Hours))
                    if (twoDaysBefore in solution.days.indices) infeasibleShifts.addAll(solution.days[twoDaysBefore].nightShifts)
                    if (twoDaysAfter in solution.days.indices) infeasibleShifts.addAll(solution.days[twoDaysAfter].nightShifts)
                    if (day > 18) infeasibleShifts.addAll(listOf(10, 11, 12, 13, 24, 25, 26, 27))

                    for (shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }

                    for (shiftID in (0..<solution.shifts.size).filterNot { infeasibleShifts.contains(it) || it == shift.id }) {
                        assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertNull(solution.shifts[shiftID].causesOfInfeasibility[3])
                    }
                }
            }

            @Test
            fun correctFeasibilityForNightsWorkedInARow() {
                val solution = newSolution(generator)

                data class Quadruple(val first: Int, val second: Int, val third: Int, val fourth: Int)
                val calcDays = { i: Int -> Quadruple(i - 1, i - 2, i + 1, i + 2) }

                val infeasibleShifts = mutableSetOf<Int>()
                val allocatedShifts = mutableSetOf<Int>()

                // 1 to 3 nights in a row
                for(day in 1..3){
                    val assignment = solution.assignments[(day*2+1)*2]
                    allocatedShifts.add(assignment.shift)
                    val shift = solution.shifts[assignment.shift]
                    solution.allocateAssignment((day*2+1)*2, 3)

                    if(shift is NightShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.dayShifts48HoursAfter))

                    val (prevDay, priorDay, nextDay, subsequentDay) = calcDays(shift.day)

                    if(prevDay in solution.days.indices && solution.days[prevDay].doctorsWorkingNight.contains(3) && nextDay in solution.days.indices)
                        infeasibleShifts.removeAll(solution.days[nextDay].nightShifts)

                    if(subsequentDay in solution.days.indices)
                        infeasibleShifts.addAll(solution.days[subsequentDay].nightShifts)

                    if(priorDay in solution.days.indices && !solution.days[prevDay].doctorsWorkingNight.contains(3))
                        infeasibleShifts.addAll(solution.days[priorDay].nightShifts)

                    for (shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }

                    val numShifts = solution.shifts.size
                    val shiftIds = 0..<numShifts
                    for(shiftID in shiftIds.filterNot { infeasibleShifts.contains(it) || allocatedShifts.contains(it) }) {
                        assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertNull(solution.shifts[shiftID].causesOfInfeasibility[3])
                    }
                }

                // 4-th day in a row
                solution.allocateAssignment(18, 3)
                allocatedShifts.add(9)
                val shift = solution.shifts[9]
                if(shift is NightShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.dayShifts48HoursAfter))

                val infeasibleNights = setOf(0, 5, 6)
                for(day in infeasibleNights)
                    for(shiftId in solution.days[day].nightShifts)
                        infeasibleShifts.add(shiftId)

                for (shiftID in infeasibleShifts) {
                    assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                    assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                }

                val numShifts = solution.shifts.size
                val shiftIds = 0..<numShifts
                for(shiftID in shiftIds.filterNot { infeasibleShifts.contains(it) || allocatedShifts.contains(it) }) {
                    assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(3))
                    assertNull(solution.shifts[shiftID].causesOfInfeasibility[3])
                }
            }

            @Test
            fun feasibilityIsCorrectlyUpdatedForWorkedWeekend() {
                val weekendDays = listOf(5,6,12,13,19,20)

                for(day in weekendDays) {
                    val addedValues = when {
                        //Day is a Sunday
                        (day + 1) % 7 == 0 -> intArrayOf(6, 7, 13, 14, -7, -8, -14, -15)
                        //Day is a Saturday
                        (day + 2) % 7 == 0 -> intArrayOf(7, 8, 14, 15, -6, -7, -13, -14)
                        else -> return
                    }

                    val solution = newSolution(generator)
                    solution.allocateAssignment((day*2+1)*2, 3)
                    val relevantWeekendDays = addedValues.map { it + day }.filter { it in solution.days.indices }

                    val shift = solution.shifts[solution.assignments[(day*2+1)*2].shift]
                    val infeasibleShifts = mutableSetOf<Int>()
                    val twoDaysBefore = shift.day - 2
                    val twoDaysAfter = shift.day + 2

                    if(shift is NightShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.dayShifts48HoursAfter))
                    if(twoDaysBefore in solution.days.indices) infeasibleShifts.addAll(solution.days[twoDaysBefore].nightShifts)
                    if(twoDaysAfter in solution.days.indices) infeasibleShifts.addAll(solution.days[twoDaysAfter].nightShifts)

                    for(dayID in relevantWeekendDays)
                        for(shiftId in solution.days[dayID].getShifts())
                            infeasibleShifts.add(shiftId)

                    for(shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }

                    for(shiftId in (0..<solution.shifts.size).filterNot { infeasibleShifts.contains(it) || it == day*2+1 }) {
                        assertTrue(solution.shifts[shiftId].feasibleDoctors.contains(3))
                        assertNull(solution.shifts[shiftId].causesOfInfeasibility[3])
                    }
                }
            }

        }
    }

    @Nested
    inner class DeAllocateAssignmentTests {
        @Test
        fun throwsExceptionIfNoAssignee() {
            // Assignment never given an assignee
            val solution = newSolution(generator)
            assertThrows<Exception> { solution.deallocateAssignment(0) }

            // Assignment deallocated twice
            solution.allocateAssignment(0, 1)
            solution.deallocateAssignment(0)
            assertThrows<Exception> { solution.deallocateAssignment(0) }
        }

        @Test
        fun doctorIsAddedBackToFeasibleDoctors() {
            val solution = newSolution(generator)
            solution.allocateAssignment(0, 1)
            solution.deallocateAssignment(0)
            for(shift in solution.shifts) {
                assertTrue(shift.feasibleDoctors.contains(1))
                assertNull(shift.causesOfInfeasibility[1])
            }
        }

        @Test
        fun unassignedAssignmentsIsUpdated() {
            val solution = newSolution(generator)
            solution.allocateAssignment(0, 1)
            solution.deallocateAssignment(0)
            assertTrue(solution.unassignedAssignments.contains(0))
        }

        @Test
        fun assigneeIsSetToNull() {
            val solution = newSolution(generator)
            solution.allocateAssignment(0, 1)
            solution.deallocateAssignment(0)
            assertNull(solution.assignments[0].assignee)
        }

        @Nested
        inner class DayFeasibilityTests {
            /* 7-day row infeasibility is removed from relevant days if any day is deallocated
             * We know that the infeasibility is correctly created due to prior testing
             */
            @Test
            fun correctlyUpdatesForDaysInARow() {
                val days = listOf(1,2,3,3,4,5,6,7)
                for(day in days) {
                    val solution = newSolution(generator)

                    val infeasibleShifts = mutableSetOf<Int>()
                    val allocatedShifts = mutableSetOf<Int>()
                    val weekendShifts = setOf(24, 25, 26, 27, 38, 39, 40, 41)

                    for(dayID in 1..7) {
                        solution.allocateAssignment(4 * dayID, 3)
                        if(dayID != day){
                            val shiftID = solution.assignments[4*dayID].shift
                            allocatedShifts.add(shiftID)
                            val shift = solution.shifts[shiftID]
                            if(shift is DayShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.nightShifts48HoursBefore))
                        }
                    }

                    solution.deallocateAssignment(4*day)

                    // Checks that shifts, that should be, are still infeasible
                    for(shiftID in infeasibleShifts){
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }

                    // Feasible shifts are feasible
                    for(shiftID in (0..<solution.shifts.size).filterNot { infeasibleShifts.contains(it) || allocatedShifts.contains(it) || weekendShifts.contains(it) }) {
                        assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertNull(solution.shifts[shiftID].causesOfInfeasibility[3])
                    }

                    // Day related causes of infeasibility have been removed
                    for(dayID in setOf(0,8,9))
                        assertNull(solution.days[dayID].causesOfInfeasibility[3])

                    // ToCheck is cleared
                    for(dayID in days.filter { it != day })
                        assertNull(solution.days[dayID].toCheck[3])
                }
            }

            // Cases where the infeasibility should always be removed
            @Test
            fun correctlyUpdatesForWouldCauseRowTooLarge() {
                val days = listOf(0,1,2,3,4,5,6,7)

                for(dayID in 1..6) {
                    val solution = newSolution(generator)
                    val (left, right) = days.filterNot { it == dayID }.partition { it < dayID }
                    for(l in left)
                        solution.allocateAssignment(4*l, 3)
                    for(r in right)
                        solution.allocateAssignment(4*r, 3)

                    for(day in days.filterNot { it == dayID }){
                        solution.deallocateAssignment(day*4)
                        assertNull(solution.days[dayID].causesOfInfeasibility[3])
                        solution.allocateAssignment(day*4, 3)
                        assertNotNull(solution.days[dayID].causesOfInfeasibility[3])
                    }
                }
            }

            // Cases where the infeasibility should be retained if certain days are removed
            @Test
            fun correctlyHandlesEdgeCasesOfWouldCauseRowTooLarge() {
                val days = listOf(0,1,2,3,4,5,6,7,8)

                for(pivot in listOf(2,6)) {
                    val solution = newSolution(generator)
                    val (left, right) = days.filterNot { it == pivot }.partition { it < pivot }
                    for(l in left)
                        solution.allocateAssignment(4*l, 3,)
                    for(r in right)
                        solution.allocateAssignment(4*r, 3)

                    solution.deallocateAssignment(0)
                    assertNotNull(solution.days[pivot].causesOfInfeasibility[3])
                    solution.allocateAssignment(0, 3)
                    solution.deallocateAssignment(4)
                    assertNull(solution.days[pivot].causesOfInfeasibility[3])
                    solution.allocateAssignment(4,3)

                    solution.deallocateAssignment(32)
                    assertNotNull(solution.days[pivot].causesOfInfeasibility[3])
                    solution.allocateAssignment(32, 3)
                    solution.deallocateAssignment(28)
                    assertNull(solution.days[pivot].causesOfInfeasibility[3])
                }
            }

            @Test
            fun correctlyUpdatesForInsufficientRest() {
                val days = listOf(0,1,2,3,4,5,6)

                for(pivot in 0..5){
                    val solution = newSolution(generator)
                    val(left, right) = days.filterNot { it == pivot }.partition { it < pivot }
                    for(l in left)
                        solution.allocateAssignment(4*l, 3)
                    for(r in right)
                        solution.allocateAssignment(4*r, 3)

                    solution.allocateAssignment(8*4, 3)

                    for(day in days.filterNot { it == pivot }){
                        solution.deallocateAssignment(4*day)
                        assertNull(solution.days[day].causesOfInfeasibility[3])
                        solution.allocateAssignment(4*day, 3)
                    }
                }
            }

            @Test
            fun correctlyUpdatesFeasibilityForRemovalOfOnlyDayWorkedOnAWeekend() {
                val weekendDays = listOf(5,6,12,13,19,20)

                for(day in weekendDays) {
                    val solution = newSolution(generator)
                    solution.allocateAssignment(day*4, 3)
                    solution.deallocateAssignment(day*4)
                    for(shift in solution.shifts)
                        assertTrue(shift.feasibleDoctors.contains(3))
                }
            }

            @Test
            fun retainsInfeasibilityIfOneOfTwoDaysWorkedOnAWeekendIsRemoved() {
                val saturdays = listOf(5,12,19)

                for(day in saturdays){
                    val addedValues = when {
                        //Day is a Sunday
                        (day + 1) % 7 == 0 -> intArrayOf(6, 7, 13, 14, -7, -8, -14, -15)
                        //Day is a Saturday
                        (day + 2) % 7 == 0 -> intArrayOf(7, 8, 14, 15, -6, -7, -13, -14)
                        else -> return
                    }
                    val solution = newSolution(generator)
                    solution.allocateAssignment(day*4, 3)
                    solution.allocateAssignment((day+1)*4, 3)
                    val relevantWeekendDays = addedValues.map { it + day }.filter { it in solution.days.indices }
                    val infeasibleShifts = mutableSetOf<Int>()

                    for(dayID in relevantWeekendDays)
                        infeasibleShifts.addAll(solution.days[dayID].getShifts())

                    // Saturday is removed
                    solution.deallocateAssignment(day*4)
                    for(shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }
                    solution.allocateAssignment(day*4, 3)

                    // Sunday is removed
                    solution.deallocateAssignment((day+1)*4)
                    for(shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }
                }
            }

            @Test
            fun removesFeasibilityIfBothDaysWorkedOnAWeekendAreRemoved() {
                val weekendDays = listOf(5,12,19)

                for(day in weekendDays) {
                    val solution = newSolution(generator)
                    solution.allocateAssignment(day*4, 3)
                    solution.allocateAssignment((day+1)*4, 3)
                    solution.deallocateAssignment(day*4)
                    solution.deallocateAssignment((day+1)*4)
                    for(shift in solution.shifts)
                        assertTrue(shift.feasibleDoctors.contains(3))
                }
            }
        }

        @Nested
        inner class NightFeasibilityTests {
            @Test
            fun onlyRemovesEndsOfBlocksAndReturnsAppropriateBoolean() {
                val nights = listOf(1,2,3,4)
                var toTake = 1

                while(toTake < nights.size) {
                    val solution = newSolution(generator)
                    val relevantNights = mutableListOf<Int>()
                    relevantNights.addAll(nights.take(toTake))
                    for(day in relevantNights)
                        solution.allocateAssignment((day*2+1)*2, 3)

                    // Leftmost night can be removed and true is returned
                    assertTrue(solution.deallocateAssignment((relevantNights.min()*2+1)*2))
                    assertFalse(solution.days[relevantNights.min()].doctorsWorkingNight.contains(3))

                    solution.allocateAssignment((relevantNights.min()*2+1)*2, 3)

                    // Middle nights are not removed and false is returned
                    val middleNights = mutableListOf<Int>()
                    middleNights.addAll(relevantNights.filter { it != relevantNights.min() && it != relevantNights.max() })
                    for(day in middleNights){
                        assertFalse(solution.deallocateAssignment((day*2+1)*2))
                        assertTrue(solution.days[day].doctorsWorkingNight.contains(3))
                    }

                    // Rightmost night can be removed and true is returned
                    assertTrue(solution.deallocateAssignment((relevantNights.max()*2+1)*2))

                    toTake += 1
                }
            }

            @Test
            fun correctlyRemovesInfeasibilityForASingleShift() {
                val nightsToCheck = listOf(0,1,2,18,19,20)

                for(day in nightsToCheck) {
                    val solution = newSolution(generator)
                    solution.allocateAssignment((day*2+1)*2, 3)
                    solution.deallocateAssignment((day*2+1)*2)

                    for(shift in solution.shifts) {
                        assertTrue(shift.feasibleDoctors.contains(3))
                        assertNull(shift.causesOfInfeasibility[3])
                    }
                }
            }

            @Test
            fun removesAndRetainsInfeasibilityAsAppropriateForMultipleShifts() {
                val solution = newSolution(generator)
                val infeasibleShifts = mutableSetOf<Int>()
                val assignedShifts = mutableSetOf<Int>()

                for(day in 2..4) {
                    solution.allocateAssignment((day * 2 + 1) * 2, 3)
                    assignedShifts.add(day*2+1)
                }

                // Night of day 2 is deallocated
                solution.deallocateAssignment(10)
                assignedShifts.remove(5)
                for(day in listOf(3,4)){
                    val shift = solution.shifts[day*2+1]
                    if(shift is NightShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.dayShifts48HoursAfter))
                }
                infeasibleShifts.addAll(solution.days[1].nightShifts)
                infeasibleShifts.addAll(solution.days[6].nightShifts)

                for(shiftID in infeasibleShifts) {
                    assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                    assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                }

                for(shiftID in (0..<solution.shifts.size).filterNot { assignedShifts.contains(it) || infeasibleShifts.contains(it) }) {
                    assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(3))
                    assertNull(solution.shifts[shiftID].causesOfInfeasibility[3])
                }

                // Reallocating night of day 2, in order to test the removal of night of day 4
                solution.allocateAssignment(10, 3)
                infeasibleShifts.removeAll(infeasibleShifts)
                assignedShifts.add(5)

                // Night of Day 4 is deallocated
                solution.deallocateAssignment(18)
                assignedShifts.remove(9)
                for(day in listOf(2,3)){
                    val shift = solution.shifts[day*2+1]
                    if(shift is NightShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.dayShifts48HoursAfter))
                }
                infeasibleShifts.addAll(solution.days[0].nightShifts)
                infeasibleShifts.addAll(solution.days[5].nightShifts)

                for(shiftID in infeasibleShifts) {
                    assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                    assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                }

                for(shiftID in (0..<solution.shifts.size).filterNot { assignedShifts.contains(it) || infeasibleShifts.contains(it) }) {
                    assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(3))
                    assertNull(solution.shifts[shiftID].causesOfInfeasibility[3])
                }
            }

            @Test
            fun correctlyUpdatesFeasibilityWhenRowIsBroken() {
                val solution = newSolution(generator)
                val relevantNights = listOf(1,2,3,4)

                for(day in relevantNights)
                    solution.allocateAssignment((day*2+1)*2, 3)

                for(day in listOf(1,4)){
                    val assignedShifts = mutableSetOf<Int>()
                    val infeasibleShifts = mutableSetOf<Int>()
                    solution.deallocateAssignment((day*2+1)*2)

                    for(dayID in relevantNights.filterNot { it == day }) {
                        val shift = solution.shifts[dayID*2+1]
                        assignedShifts.add(shift.id)
                        if(shift is NightShift) infeasibleShifts.addAll(shift.shiftsWithin11Hours.union(shift.dayShifts48HoursAfter))
                    }

                    if(day == 1)
                        infeasibleShifts.addAll(solution.days[0].nightShifts.union(solution.days[6].nightShifts))
                    else
                        infeasibleShifts.addAll(solution.days[5].nightShifts)

                    for(shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }

                    for(shiftID in (0..<solution.shifts.size).filterNot { assignedShifts.contains(it) || infeasibleShifts.contains(it) }) {
                        assertTrue(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertNull(solution.shifts[shiftID].causesOfInfeasibility[3])
                    }
                    solution.allocateAssignment((day*2+1)*2, 3)
                }
            }

            @Test
            fun retainsInfeasibilityIfOneOfTwoDaysWorkedOnAWeekendIsRemoved() {
                val saturdays = listOf(5,12,19)

                for(day in saturdays){
                    val addedValues = when {
                        //Day is a Sunday
                        (day + 1) % 7 == 0 -> intArrayOf(6, 7, 13, 14, -7, -8, -14, -15)
                        //Day is a Saturday
                        (day + 2) % 7 == 0 -> intArrayOf(7, 8, 14, 15, -6, -7, -13, -14)
                        else -> return
                    }
                    val solution = newSolution(generator)
                    solution.allocateAssignment((day*2+1)*2, 3)
                    solution.allocateAssignment(((day+1)*2+1)*2, 3)
                    val relevantWeekendDays = addedValues.map { it + day }.filter { it in solution.days.indices }
                    val infeasibleShifts = mutableSetOf<Int>()

                    for(dayID in relevantWeekendDays)
                        infeasibleShifts.addAll(solution.days[dayID].getShifts())

                    // Saturday is removed
                    solution.deallocateAssignment((day*2+1)*2)
                    for(shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }
                    solution.allocateAssignment((day*2+1)*2, 3)

                    // Sunday is removed
                    solution.deallocateAssignment(((day+1)*2+1)*2)
                    for(shiftID in infeasibleShifts) {
                        assertFalse(solution.shifts[shiftID].feasibleDoctors.contains(3))
                        assertEquals(Cause.REST, solution.shifts[shiftID].causesOfInfeasibility[3]!!.cause)
                    }
                }
            }

            @Test
            fun removesFeasibilityIfBothDaysWorkedOnAWeekendAreRemoved() {
                val weekendDays = listOf(5,12,19)

                for(day in weekendDays) {
                    val solution = newSolution(generator)
                    solution.allocateAssignment((day*2+1)*2, 3)
                    solution.allocateAssignment(((day+1)*2+1)*2, 3)
                    solution.deallocateAssignment((day*2+1)*2)
                    solution.deallocateAssignment(((day+1)*2+1)*2)
                    for(shift in solution.shifts)
                        assertTrue(shift.feasibleDoctors.contains(3))
                }
            }

        }
    }*/
}
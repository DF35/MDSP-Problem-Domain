import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import problemDomain.Day
import problemDomain.DayNightInfeasibility

@DisplayName("Day Tests")
class DayTest {
    @Test
    fun getShiftsReturnsCorrectShifts() {
        val day = Day(0, setOf(1,3), setOf(2,4))
        assertEquals(setOf(1,2,3,4), day.getShifts())
    }

    @Test
    fun addWorkingDoctorProducesCorrectValues() {
        val day = Day(0, setOf(1,3), setOf(2,4))
        day.addWorkingDoctor(0)
        assertEquals(1, day.doctorsWorkingDay[0])
        day.addWorkingDoctor(0)
        assertEquals(2, day.doctorsWorkingDay[0])
    }

    @Nested
    inner class RemoveWorkingDoctorTests {
        @Test
        fun numberOfShiftsIsCorrectlyDecremented() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.doctorsWorkingDay[0] = 2
            day.removeWorkingDoctor(0)
            assertEquals(1, day.doctorsWorkingDay[0])
        }

        @Test
        fun doctorIsRemovedFromWorkingDayIfNoShiftsWorked() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.doctorsWorkingDay[0] = 1
            day.removeWorkingDoctor(0)
            assertNull(day.doctorsWorkingDay[0])
        }

        @Test
        fun throwsErrorIfDoctorIsNotWorkingDay() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            assertThrows<Exception> { day.removeWorkingDoctor(0) }
        }
    }

    @Nested
    inner class AddInfeasibilityTests {
        @Test
        fun createsDayInfeasibilityIfNoneExists() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.addInfeasibility(0, DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4,5,6,7)))
            assertNotNull(day.causesOfInfeasibility[0])
        }

        @Test
        fun causeIsAddedToExisitngInfeasibility() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.causesOfInfeasibility[0] = mutableSetOf(DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4,5,6,7)))
            day.addInfeasibility(0, DayNightInfeasibility.InsufficientRest(setOf(3,4,5,6)))
            assertEquals(2, day.causesOfInfeasibility[0]!!.size)
        }
    }

    @Nested
    inner class RemoveInfeasibilityTests {
        @Test
        fun throwsErrorIfFeasibilityDoesNotExist() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            // No infeasibility for the given doctor exists
            assertThrows<Exception> { day.removeInfeasibility(0, DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4,5,6,7))) }
            day.causesOfInfeasibility[0] = mutableSetOf(DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4,5,6,7)))
            // Non-existent feasibility is given for doctor with existing infeasibility
            assertThrows<Exception> { day.removeInfeasibility(0, DayNightInfeasibility.InsufficientRest(setOf(3,4,5,6))) }
        }

        @Test
        fun removesDoctorFromCausesOfInfeasibilityIfAllRemoved() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            val restRow = DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4,5,6,7))
            day.causesOfInfeasibility[0] = mutableSetOf(restRow)
            assertTrue(day.removeInfeasibility(0, restRow))
            assertNull(day.causesOfInfeasibility[0])
        }

        @Test
        fun leavesDoctorInCausesOfInfeasibilityIfOtherCausesStillExist() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            val restRow = DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4,5,6,7))
            day.causesOfInfeasibility[0] = mutableSetOf(
                restRow, DayNightInfeasibility.InsufficientRest(setOf(3,4,5,6)))
            assertFalse(day.removeInfeasibility(0, restRow))
            assertEquals(1, day.causesOfInfeasibility[0]!!.size)
        }
    }

    @Nested
    inner class AddToCheckTests {
        @Test
        fun entryForDoctorIsCreatedIfNoneExists() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.addToCheck(0, 1)
            assertEquals(1, day.toCheck[0]!!.size)
        }

        @Test
        fun dayIsCorrectlyAddedToExistingToCheckEntry() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.toCheck[0] = mutableSetOf(1)
            day.addToCheck(0, 2)
            assertEquals(2, day.toCheck[0]!!.size)
        }
    }

    @Nested
    inner class RemoveToCheckTests {
        @Test
        fun throwsExceptionIfNoEntryForDoctor() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            assertThrows<Exception> { day.removeToCheck(0, 1) }
        }

        @Test
        fun throwsExceptionIfDayNotInToCheck() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.toCheck[0] = mutableSetOf(1)
            assertThrows<Exception> { day.removeToCheck(0,2) }
        }

        @Test
        fun keepsDoctorInToCheckIfDaysStillPresent() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.toCheck[0] = mutableSetOf(1,2)
            day.removeToCheck(0, 1)
            assertEquals(1, day.toCheck[0]!!.size)
        }

        @Test
        fun removesDoctorFromToCheckIfNoDaysRemain() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.toCheck[0] = mutableSetOf(1)
            day.removeToCheck(0, 1)
            assertNull(day.toCheck[0])
        }
    }

    @Nested
    inner class AddNightInfeasibilityTests {
        @Test
        fun addsNightInfeasibilityIfNoneExists() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.addNightInfeasibility(0, DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4)))
            assertEquals(1, day.causesOfNightInfeasibility[0]!!.size)
        }

        @Test
        fun sourceIsAddedToExistingCause() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.causesOfNightInfeasibility[0] = mutableSetOf(DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4)))
            day.addNightInfeasibility(0, DayNightInfeasibility.WouldCauseRowTooLarge(setOf(6,7,8)))
            assertEquals(2, day.causesOfNightInfeasibility[0]!!.size)
        }
    }

    @Nested
    inner class RemoveNightInfeasibility {
        @Test
        fun throwsExceptionIfDoctorHasNoEntry() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            assertThrows<Exception> { day.removeNightInfeasibility(0, DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4))) }
        }

        @Test
        fun throwsExceptionIfInfeasibilityNotPresent() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.causesOfNightInfeasibility[0] = mutableSetOf(DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4)))
            assertThrows<Exception> { day.removeNightInfeasibility(0, DayNightInfeasibility.WouldCauseRowTooLarge(setOf(6,7,8))) }
        }

        @Test
        fun doctorIsLeftInCausesOfNightInfeasibilityIfOtherInfeasibilityLeft() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            val row = DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4))
            day.causesOfNightInfeasibility[0] =
                mutableSetOf(row, DayNightInfeasibility.WouldCauseRowTooLarge(setOf(6,7,8)))
            assertFalse(day.removeNightInfeasibility(0, row))
            assertEquals(1,day.causesOfNightInfeasibility[0]!!.size)
        }

        @Test
        fun doctorIsRemovedIfNoCausesOfInfeasibilityLeft() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            val row = DayNightInfeasibility.RestAfterRow(setOf(1,2,3,4))
            day.causesOfNightInfeasibility[0] = mutableSetOf(row)
            assertTrue(day.removeNightInfeasibility(0, row))
            assertNull(day.causesOfNightInfeasibility[0])
        }
    }

    @Nested
    inner class AddToCheckNightTests {
        @Test
        fun entryForDoctorIsCreatedIfNoneExists() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.addToCheckNight(0, 1)
            assertEquals(1, day.toCheckNight[0]!!.size)
        }

        @Test
        fun dayIsAddedToExistingEntry() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.toCheckNight[0] = mutableSetOf(1)
            day.addToCheckNight(0, 2)
            assertEquals(2, day.toCheckNight[0]!!.size)
        }
    }

    @Nested
    inner class RemoveToCheckNightTests {
        @Test
        fun throwsExceptionIfNoEntryForDoctor() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            assertThrows<Exception> { day.removeToCheck(0, 1) }
        }

        @Test
        fun throwsExcepctionForNonPresentDay() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.toCheckNight[0] = mutableSetOf(1)
            assertThrows<Exception> { day.removeToCheck(0, 2) }
        }

        @Test
        fun doctorNotRemovedFromToCheckNightIfDaysRemain() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.toCheckNight[0] = mutableSetOf(1,2)
            day.removeToCheckNight(0, 1)
            assertEquals(1, day.toCheckNight[0]!!.size)
        }

        @Test
        fun doctorRemovedIfNoDaysToCheck() {
            val day = Day(0, setOf(1,3), setOf(2,4))
            day.toCheckNight[0] = mutableSetOf(1)
            day.removeToCheckNight(0,1)
            assertNull(day.toCheckNight[0])
        }
    }
}
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import problemDomain.Day

@DisplayName("Day Tests")
class DayTest {
    private fun getDay(): Day {return Day(0, emptyList(), emptyList(), emptyList(), emptyList())}
    @Nested
    inner class AddWorkingDoctorTests {
        @Test
        fun createsEntryForNewDoctor() {
            val day = getDay()
            day.addWorkingDoctor(1, 1)
            assertEquals(1, day.doctorsWorkingDay.size)
            assertEquals(1, day.doctorsWorkingDay[1]!!.size)
            day.addWorkingDoctor(0, 1)
            assertEquals(2, day.doctorsWorkingDay.size)
            assertEquals(1, day.doctorsWorkingDay[0]!!.size)
        }

        @Test
        fun addsToExistingEntry() {
            val day = getDay()
            day.addWorkingDoctor(1, 1)
            day.addWorkingDoctor(1, 0)
            assertEquals(1, day.doctorsWorkingDay.size)
            assertEquals(2, day.doctorsWorkingDay[1]!!.size)
        }
    }

    @Nested
    inner class RemoveWorkingDoctor {
        @Test
        fun successfullyRemovesForValidRequest() {
            val day = getDay()
            day.addWorkingDoctor(1, 1)
            day.removeWorkingDoctor(1,1)
            assertTrue(day.doctorsWorkingDay.isEmpty())
        }

        @Test
        fun throwsExceptionIfNoEntryForDoctorExists() {
            val day = getDay()
            assertThrows<Exception> { day.removeWorkingDoctor(1, 1) }
            day.addWorkingDoctor(1,1)
            day.removeWorkingDoctor(1,1)
            assertThrows<Exception> { day.removeWorkingDoctor(1,1) }
        }

        @Test
        fun throwsExceptionIfNoRecordOfShiftWorked() {
            val day = getDay()
            day.addWorkingDoctor(1, 2)
            assertThrows<Exception> { day.removeWorkingDoctor(1,1) }
            day.removeWorkingDoctor(1,2)
            assertThrows<Exception> { day.removeWorkingDoctor(1,2) }
        }
    }
}
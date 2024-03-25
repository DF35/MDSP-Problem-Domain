import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import problemDomain.MiddleGrade

@DisplayName("MiddleGrade Tests")
class MiddleGradeTest {
    private fun getDoctor(): MiddleGrade {
        return MiddleGrade(0, "senior", 20.0, 10, 10,
            0.5, setOf(0,1,2,3), 1..7, 1..4)
    }

    @Test
    fun varianceHoursWorkedReturnsExpectedValue() {
        val doc = getDoctor()
        doc.hoursWorked = 10.0
        assertEquals(0.0, doc.varianceHoursWorked())
        doc.hoursWorked = 5.0
        assertEquals(10.0, doc.varianceHoursWorked())
        doc.hoursWorked = 20.0
        assertEquals(-20.0, doc.varianceHoursWorked())
    }

    @Test
    fun varianceDayShiftsWorkedReturnsExpectedValue() {
        val doc = getDoctor()
        doc.dayShiftsWorked = 10
        assertEquals(0, doc.varianceDayShiftsWorked())
        doc.dayShiftsWorked = 5
        assertEquals(5, doc.varianceDayShiftsWorked())
        doc.dayShiftsWorked = 20
        assertEquals(-10, doc.varianceDayShiftsWorked())
    }

    @Test
    fun varianceNightShiftsWorkedReturnsExpectedValue() {
        val doc = getDoctor()
        doc.nightShiftsWorked = 10
        assertEquals(0, doc.varianceNightShiftsWorked())
        doc.nightShiftsWorked = 5
        assertEquals(5, doc.varianceNightShiftsWorked())
        doc.nightShiftsWorked = 20
        assertEquals(-10, doc.varianceNightShiftsWorked())
    }

    @Test
    fun numberOfShiftPrefsViolatedReturnsExpectedValue() {
        val doc = getDoctor()
        assertEquals(0, doc.numberOfShiftPrefsViolated())
        doc.assignedShifts = mutableSetOf(0,3)
        assertEquals(2, doc.numberOfShiftPrefsViolated())
        doc.assignedShifts = mutableSetOf(0,1,2,3)
        assertEquals(4, doc.numberOfShiftPrefsViolated())
    }

}
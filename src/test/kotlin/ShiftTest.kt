import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import problemDomain.Cause
import problemDomain.DayShift
import problemDomain.ShiftInfeasibility
import problemDomain.Source

@DisplayName("Shift Tests")
class ShiftTest {
    fun newShift(): DayShift {
        return DayShift(1, intArrayOf(1), setOf(2), listOf(1), listOf(1), setOf(1),
            1, mutableSetOf(0), 10.00)
    }

    @Nested
    inner class RestInfeasibilityTests {
        @Test
        fun newRestInfeasibilityCreatedWhenNoneExists() {
            val shift = newShift()
            shift.restInfeasibility(0, Source.ShiftWorked(1))
            assertNotNull(shift.causesOfInfeasibility[0])
            assertEquals(shift.causesOfInfeasibility[0]!!.cause, Cause.Rest)
            assertEquals(shift.causesOfInfeasibility[0]!!.sources, setOf(Source.ShiftWorked(1)))
        }

        @Test
        fun sourceIsAddedToPreexistingRestInfeasibility() {
            val shift = newShift()
            shift.restInfeasibility(0, Source.ShiftWorked(1))
            shift.restInfeasibility(0, Source.ShiftWorked(2))
            assertNotNull(shift.causesOfInfeasibility[0])
            assertEquals(shift.causesOfInfeasibility[0]!!.cause, Cause.Rest)
            assertEquals(shift.causesOfInfeasibility[0]!!.sources, setOf(Source.ShiftWorked(1), Source.ShiftWorked(2)))
        }

        @Test
        fun doesNothingInCaseOfPreexistingNonRestInfeasibility() {
            val shift = newShift()
            shift.causesOfInfeasibility[0] = ShiftInfeasibility(Cause.Leave)
            shift.restInfeasibility(0, Source.ShiftWorked(1))
            assertEquals(shift.causesOfInfeasibility[0]!!.cause, Cause.Leave)
            assertEquals(shift.causesOfInfeasibility[0]!!.sources, setOf<Source>())
        }

        @Test
        fun doctorIsRemovedFromFeasibleDoctors() {
            val shift = newShift()
            shift.restInfeasibility(0, Source.ShiftWorked(1))
            assertEquals(shift.feasibleDoctors, setOf<Int>())
        }
    }

    @Nested
    inner class CreateNonRestInfeasibilityTests {
        @Test
        fun throwsExceptionForPreexistingInfeasibility() {
            val shift = newShift()
            shift.causesOfInfeasibility[0] = ShiftInfeasibility(Cause.Leave)
            assertThrows<Exception> { shift.createNonRestInfeasibility(0, Cause.Leave) }
        }

        @Test
        fun throwsExceptionForRestGivenAsACause() {
            val shift = newShift()
            assertThrows<Exception> { shift.createNonRestInfeasibility(0, Cause.Rest) }
        }

        @Test
        fun successFullyCreatesNewInfeasibility() {
            val shift = newShift()
            shift.createNonRestInfeasibility(0, Cause.Training)
            assertNotNull(shift.causesOfInfeasibility[0])
            assertEquals(shift.causesOfInfeasibility[0]!!.cause, Cause.Training)
        }

        @Test
        fun doctorIsRemovedFromFeasibleDoctors() {
            val shift = newShift()
            shift.createNonRestInfeasibility(0, Cause.Leave)
            assertEquals(shift.feasibleDoctors, setOf<Int>())
        }
    }

    @Nested
    inner class RemoveSourceTests {
        @Test
        fun throwsExceptionIfNoInfeasibilityExists() {
            val shift = newShift()
            assertThrows<Exception> { shift.removeSource(0, Source.RowOfNights(setOf(0,1,2,3))) }
        }

        @Test
        fun doesNothingInCaseOfNonRestInfeasibility() {
            val shift = newShift()
            shift.causesOfInfeasibility[0] = ShiftInfeasibility(Cause.Training)
            shift.removeSource(0, Source.RowOfNights(setOf(0,1,2,3)))
            assertNotNull(shift.causesOfInfeasibility[0])
            assertEquals(shift.causesOfInfeasibility[0]!!.cause, Cause.Training)
        }

        @Test
        fun rejectsRemovalOfNonExistantSource() {
            val shift = newShift()
            shift.causesOfInfeasibility[0] = ShiftInfeasibility(Cause.Rest)
            shift.causesOfInfeasibility[0]!!.sources.add(Source.ShiftWorked(1))
            assertThrows<Exception> {shift.removeSource(0, Source.ShiftWorked(2))}
        }

        @Test
        fun doctorIsNotRemovedFromFeasibleDoctorsIfAnInfeasibilityStillExists() {
            val shift = newShift()
            shift.feasibleDoctors.remove(0)
            shift.causesOfInfeasibility[0] = ShiftInfeasibility(Cause.Rest)
            shift.causesOfInfeasibility[0]!!.sources.add(Source.ShiftWorked(1))
            shift.causesOfInfeasibility[0]!!.sources.add(Source.ShiftWorked(2))
            shift.removeSource(0, Source.ShiftWorked(2))

            assertNotNull(shift.causesOfInfeasibility[0])
            assertEquals(shift.causesOfInfeasibility[0]!!.cause, Cause.Rest)
            assertEquals(shift.causesOfInfeasibility[0]!!.sources, setOf(Source.ShiftWorked(1)))
            assertEquals(setOf<Int>(), shift.feasibleDoctors)
        }

        @Test
        fun doctorIsAddedToFeasibleDoctorsIfAllInfeasibilityHasBeenRemoved() {
            val shift = newShift()
            shift.causesOfInfeasibility[0] = ShiftInfeasibility(Cause.Rest)
            shift.causesOfInfeasibility[0]!!.sources.add(Source.ShiftWorked(1))
            shift.removeSource(0, Source.ShiftWorked(1))

            assertNull(shift.causesOfInfeasibility[0])
            assertEquals(shift.feasibleDoctors, setOf(0))
        }
    }
}
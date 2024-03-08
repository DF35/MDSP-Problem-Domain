import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import problemDomain.Assignment

@DisplayName("Assignment Tests")
class AssignmentTest {
    @Test
    fun assignCorrectlyAltersAssignee() {
        val assignment = Assignment(1,1, "Any", emptySet())
        assignment.assign(1)
        assertEquals(1, assignment.assignee)
    }

    @Nested
    inner class UnAssignTests {
        @Test
        fun returnsNullIfNoAssignee() {
            val assignment = Assignment(1, 1, "Any", emptySet())
            assertEquals(null, assignment.unAssign())
        }

        @Test
        fun returnsCorrectDoctorAndShift() {
            val assignment = Assignment(1, 2, "Any", emptySet())
            assignment.assign(1)
            assertEquals(Pair(1,2), assignment.unAssign())
        }
    }
}
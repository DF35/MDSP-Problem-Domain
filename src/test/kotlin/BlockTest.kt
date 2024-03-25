import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import problemDomain.Block
import problemDomain.DayRemovedPos

@DisplayName("Block Tests")
class BlockTest {
    @Nested
    inner class AddDayTests {
        @Test
        fun successfullyAddsDay() {
            val block = Block(1)
            block.addDay(0)
            assertEquals(1, block.days.size)
            block.addDay(1)
            assertEquals(2, block.days.size)
        }

        @Test
        fun throwsExceptionIfDayAlreadyPresent() {
            val block = Block(1)
            block.addDay(0)
            assertThrows<Exception> { block.addDay(0) }
        }
    }

    @Nested
    inner class RemoveDayTests {
        @Test
        fun throwsExceptionIfDayNotPresent() {
            val block = Block(0)
            assertThrows<Exception> { block.removeDay(0, 1) }
            block.setDays(setOf(0,1,2,3))
            assertThrows<Exception> { block.removeDay(4, 1) }
        }

        @Test
        fun correctlyRemovesFinalDay() {
            val block = Block(0)
            block.addDay(1)
            val result = block.removeDay(1, 1)
            assertTrue(block.days.isEmpty())
            assertEquals(Pair(DayRemovedPos.Final, null), result)
        }

        @Test
        fun correctlyRemovesStartDay() {
            val block = Block(0)
            block.setDays(setOf(0,1,2,3,4))
            val result = block.removeDay(0, 1)
            assertEquals(4, block.days.size)
            assertEquals(Pair(DayRemovedPos.Start, null), result)
        }

        @Test
        fun correctlyRemovesEndDay() {
            val block = Block(0)
            block.setDays(setOf(0,1,2,3,4))
            val result = block.removeDay(4, 1)
            assertEquals(4, block.days.size)
            assertEquals(Pair(DayRemovedPos.End, null), result)
        }

        @Test
        fun correctlyRemovesMiddleDay() {
            val block = Block(0)
            block.setDays(setOf(0,1,2,3,4))
            val result = block.removeDay(2, 1)
            assertEquals(DayRemovedPos.Middle, result.first)
            assertEquals(2, result.second!!.first.days.size)
            assertEquals(2, result.second!!.second.days.size)
            assertEquals(0, result.second!!.first.id)
            assertEquals(1, result.second!!.second.id)
        }
    }
}
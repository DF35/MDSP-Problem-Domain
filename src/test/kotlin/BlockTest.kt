import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import problemDomain.Block
import problemDomain.ItemRemovedPos

@DisplayName("Block Tests")
class BlockTest {
    @Nested
    inner class AddDayTests {
        @Test
        fun successfullyAddsDay() {
            val block = Block(1)
            block.addItem(0)
            assertEquals(1, block.items.size)
            block.addItem(1)
            assertEquals(2, block.items.size)
        }

        @Test
        fun throwsExceptionIfDayAlreadyPresent() {
            val block = Block(1)
            block.addItem(0)
            assertThrows<Exception> { block.addItem(0) }
        }
    }

    @Nested
    inner class RemoveDayTests {
        @Test
        fun throwsExceptionIfDayNotPresent() {
            val block = Block(0)
            assertThrows<Exception> { block.removeItem(0, 1) }
            block.setItems(setOf(0,1,2,3))
            assertThrows<Exception> { block.removeItem(4, 1) }
        }

        @Test
        fun correctlyRemovesFinalDay() {
            val block = Block(0)
            block.addItem(1)
            val result = block.removeItem(1, 1)
            assertTrue(block.items.isEmpty())
            assertEquals(Pair(ItemRemovedPos.Final, null), result)
        }

        @Test
        fun correctlyRemovesStartDay() {
            val block = Block(0)
            block.setItems(setOf(0,1,2,3,4))
            val result = block.removeItem(0, 1)
            assertEquals(4, block.items.size)
            assertEquals(Pair(ItemRemovedPos.Start, null), result)
        }

        @Test
        fun correctlyRemovesEndDay() {
            val block = Block(0)
            block.setItems(setOf(0,1,2,3,4))
            val result = block.removeItem(4, 1)
            assertEquals(4, block.items.size)
            assertEquals(Pair(ItemRemovedPos.End, null), result)
        }

        @Test
        fun correctlyRemovesMiddleDay() {
            val block = Block(0)
            block.setItems(setOf(0,1,2,3,4))
            val result = block.removeItem(2, 1)
            assertEquals(ItemRemovedPos.Middle, result.first)
            assertEquals(2, result.second!!.first.items.size)
            assertEquals(2, result.second!!.second.items.size)
            assertEquals(0, result.second!!.first.id)
            assertEquals(1, result.second!!.second.id)
        }
    }
}
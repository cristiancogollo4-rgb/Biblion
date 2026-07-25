package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListBackspaceExitTrackerTest {

    @Test
    fun `consume consecutive backspace for same list`() {
        var now = 1_000L
        val tracker = ListBackspaceExitTracker(timeoutMillis = 1_200L) { now }
        val blockId = BlockId("list")

        tracker.arm(blockId)
        now += 500L

        assertTrue(tracker.consume(blockId))
        assertFalse(tracker.consume(blockId))
    }

    @Test
    fun `reject expired or different list backspace`() {
        var now = 1_000L
        val tracker = ListBackspaceExitTracker(timeoutMillis = 1_200L) { now }

        tracker.arm(BlockId("first"))
        assertFalse(tracker.consume(BlockId("second")))

        tracker.arm(BlockId("first"))
        now += 1_201L
        assertFalse(tracker.consume(BlockId("first")))
    }

    @Test
    fun `clear cancels pending exit`() {
        val blockId = BlockId("list")
        val tracker = ListBackspaceExitTracker()

        tracker.arm(blockId)
        tracker.clear()

        assertFalse(tracker.consume(blockId))
    }
}

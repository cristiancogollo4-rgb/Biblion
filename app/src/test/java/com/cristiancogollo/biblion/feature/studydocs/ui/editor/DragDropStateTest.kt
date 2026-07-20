package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DragDropStateTest {

    @Test
    fun initialState_isNotDragging() {
        val state = DragDropState()
        assertFalse(state.isDragging)
        assertNull(state.draggedBlockId)
        assertNull(state.targetIndex)
    }

    @Test
    fun startDrag_setsDraggedBlock() {
        val state = DragDropState()
        val blockId = BlockId("block_1")
        state.startDrag(blockId)
        assertTrue(state.isDragging)
        assertEquals(blockId, state.draggedBlockId)
    }

    @Test
    fun updateTarget_setsTargetIndex() {
        val state = DragDropState()
        state.updateTarget(3)
        assertEquals(3, state.targetIndex)
    }

    @Test
    fun updateTarget_canSetNull() {
        val state = DragDropState()
        state.updateTarget(3)
        state.updateTarget(null)
        assertNull(state.targetIndex)
    }

    @Test
    fun endDrag_returnsResultAndClears() {
        val state = DragDropState()
        val blockId = BlockId("block_1")
        state.startDrag(blockId)
        state.updateTarget(5)
        val result = state.endDrag()
        assertNotNull(result)
        assertEquals(blockId, result!!.blockId)
        assertEquals(5, result.targetIndex)
        assertFalse(state.isDragging)
        assertNull(state.draggedBlockId)
        assertNull(state.targetIndex)
    }

    @Test
    fun endDrag_returnsNullWhenNotDragging() {
        val state = DragDropState()
        val result = state.endDrag()
        assertNull(result)
    }

    @Test
    fun cancelDrag_clearsState() {
        val state = DragDropState()
        state.startDrag(BlockId("block_1"))
        state.updateTarget(2)
        state.cancelDrag()
        assertFalse(state.isDragging)
        assertNull(state.draggedBlockId)
        assertNull(state.targetIndex)
    }
}

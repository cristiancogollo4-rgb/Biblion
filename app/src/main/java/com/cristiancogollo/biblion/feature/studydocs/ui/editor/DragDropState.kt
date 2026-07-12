package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId

class DragDropState {
    var draggedBlockId by mutableStateOf<BlockId?>(null)
        private set
    var targetIndex by mutableStateOf<Int?>(null)
        private set

    fun startDrag(blockId: BlockId) {
        draggedBlockId = blockId
    }

    fun updateTarget(index: Int?) {
        targetIndex = index
    }

    fun endDrag(): DragDropResult? {
        val result = if (draggedBlockId != null && targetIndex != null) {
            DragDropResult(draggedBlockId!!, targetIndex!!)
        } else null

        draggedBlockId = null
        targetIndex = null
        return result
    }

    fun cancelDrag() {
        draggedBlockId = null
        targetIndex = null
    }

    val isDragging: Boolean get() = draggedBlockId != null
}

data class DragDropResult(
    val blockId: BlockId,
    val targetIndex: Int,
)

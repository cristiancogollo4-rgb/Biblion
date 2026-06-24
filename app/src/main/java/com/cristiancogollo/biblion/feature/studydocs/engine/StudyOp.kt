package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.TextStylePatch

sealed interface StudyOp {
    data class InsertBlock(val atIndex: Int, val block: StudyBlock) : StudyOp
    data class DeleteBlock(val blockId: BlockId) : StudyOp
    data class MoveBlock(val blockId: BlockId, val toIndex: Int) : StudyOp
    data class ReplaceBlock(val blockId: BlockId, val newBlock: StudyBlock) : StudyOp
    data class EditText(val blockId: BlockId, val newText: StyledText) : StudyOp
    data class ApplyStyle(val blockId: BlockId, val range: IntRange, val patch: TextStylePatch) : StudyOp
    data class ClearStyle(val blockId: BlockId, val range: IntRange, val kind: TextStyleKind) : StudyOp
    data class UpdateTitle(val newTitle: String) : StudyOp
    data class UpdateMetadata(val newMetadata: DocMetadata) : StudyOp
    data class BulkApply(val ops: List<StudyOp>) : StudyOp
}

enum class TextStyleKind { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, COLOR, BACKGROUND }

sealed interface OpResult {
    val isSuccess: Boolean

    data class Ok(val affectedBlockIds: List<BlockId> = emptyList()) : OpResult {
        override val isSuccess = true
    }
    data class Failed(val reason: String) : OpResult {
        override val isSuccess = false
    }
}

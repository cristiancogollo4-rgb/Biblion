package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import kotlinx.serialization.Serializable

sealed interface StudyOp {
    @Serializable
    data class InsertBlock(
        val block: StudyBlock,
        val afterBlockId: BlockId? = null,
    ) : StudyOp

    @Serializable
    data class DeleteBlock(
        val blockId: BlockId,
    ) : StudyOp

    @Serializable
    data class UpdateTitle(
        val newTitle: String,
    ) : StudyOp

    @Serializable
    data class UpdateMetadata(
        val metadata: DocMetadata,
    ) : StudyOp

    @Serializable
    data class ChangeBlockType(
        val blockId: BlockId,
        val newType: String,
    ) : StudyOp

    @Serializable
    data class SplitBlock(
        val splitBlockId: BlockId,
        val newBlock: StudyBlock,
    ) : StudyOp

    @Serializable
    data class MergeBlock(
        val removeBlockId: BlockId,
    ) : StudyOp

    @Serializable
    data class BulkApply(
        val ops: List<StudyOp>,
    ) : StudyOp
}

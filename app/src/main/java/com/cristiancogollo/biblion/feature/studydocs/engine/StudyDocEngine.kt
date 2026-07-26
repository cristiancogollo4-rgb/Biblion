package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.VerseBusinessRules

sealed interface OpResult {
    data object Success : OpResult
    data class Failed(val reason: String) : OpResult
    val isSuccess get() = this is Success
}

object StudyDocEngine {

    fun apply(doc: StudyDoc, op: StudyOp): Pair<StudyDoc, OpResult> = when (op) {
        is StudyOp.InsertBlock -> insertBlock(doc, op)
        is StudyOp.DeleteBlock -> deleteBlock(doc, op)
        is StudyOp.UpdateBlock -> updateBlock(doc, op)
        is StudyOp.SetVerseComparison -> setVerseComparison(doc, op)
        is StudyOp.ChangeBlockType -> changeBlockType(doc, op)
        is StudyOp.SplitBlock -> splitBlock(doc, op)
        is StudyOp.MergeBlock -> mergeBlock(doc, op)
        is StudyOp.UpdateTitle -> updateTitle(doc, op)
        is StudyOp.UpdateMetadata -> updateMetadata(doc, op)
        is StudyOp.BulkApply -> applyAll(doc, op)
    }

    private fun insertBlock(doc: StudyDoc, op: StudyOp.InsertBlock): Pair<StudyDoc, OpResult> {
        val block = normalizeForMutation(op.block)
        invalidReason(block)?.let { return doc to OpResult.Failed(it) }
        val insertAt = if (op.afterBlockId != null) {
            val idx = doc.blocks.indexOfFirst { it.id == op.afterBlockId }
            if (idx < 0) return doc to OpResult.Failed("Referenced block not found")
            idx + 1
        } else {
            doc.blocks.size
        }
        val newBlocks = doc.blocks.toMutableList()
        newBlocks.add(insertAt, block)
        return doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()) to OpResult.Success
    }

    private fun deleteBlock(doc: StudyDoc, op: StudyOp.DeleteBlock): Pair<StudyDoc, OpResult> {
        val idx = doc.blocks.indexOfFirst { it.id == op.blockId }
        if (idx < 0) return doc to OpResult.Failed("Block not found")
        if (doc.blocks.size <= 1) {
            val empty = doc.copy(
                blocks = listOf(StudyBlock.Paragraph()),
                updatedAt = System.currentTimeMillis(),
            )
            return empty to OpResult.Success
        }
        val newBlocks = doc.blocks.toMutableList()
        newBlocks.removeAt(idx)
        return doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()) to OpResult.Success
    }

    private fun updateBlock(doc: StudyDoc, op: StudyOp.UpdateBlock): Pair<StudyDoc, OpResult> {
        val idx = doc.blocks.indexOfFirst { it.id == op.block.id }
        if (idx < 0) return doc to OpResult.Failed("Block not found")
        val block = normalizeForMutation(op.block)
        invalidReason(block)?.let { return doc to OpResult.Failed(it) }
        val newBlocks = doc.blocks.toMutableList()
        newBlocks[idx] = block
        return doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()) to OpResult.Success
    }

    private fun setVerseComparison(
        doc: StudyDoc,
        op: StudyOp.SetVerseComparison,
    ): Pair<StudyDoc, OpResult> {
        val idx = doc.blocks.indexOfFirst { it.id == op.blockId }
        if (idx < 0) return doc to OpResult.Failed("Block not found")
        val verse = doc.blocks[idx] as? StudyBlock.Verse
            ?: return doc to OpResult.Failed("Only a Bible citation can be compared")
        if (
            op.version != null &&
            (
                op.version.isBlank() ||
                    op.version.equals(verse.sourceVersion, ignoreCase = true) ||
                    op.content.isNullOrBlank()
                )
        ) {
            return doc to OpResult.Failed("The comparison requires a different version and its text")
        }
        val updated = VerseBusinessRules.withComparison(verse, op.version, op.content)
        val newBlocks = doc.blocks.toMutableList().also { it[idx] = updated }
        return doc.copy(
            blocks = newBlocks,
            updatedAt = System.currentTimeMillis(),
        ) to OpResult.Success
    }

    private fun changeBlockType(doc: StudyDoc, op: StudyOp.ChangeBlockType): Pair<StudyDoc, OpResult> {
        val idx = doc.blocks.indexOfFirst { it.id == op.blockId }
        if (idx < 0) return doc to OpResult.Failed("Block not found")
        val block = doc.blocks[idx]
        if (block is StudyBlock.Verse) {
            return doc to OpResult.Failed("Bible citations are protected atomic blocks")
        }
        val text = when (block) {
            is StudyBlock.BulletList -> StyledText(block.items.joinToString("\n") { it.raw })
            is StudyBlock.OrderedList -> StyledText(block.items.joinToString("\n") { it.raw })
            else -> block.toStyledTextList().firstOrNull() ?: StyledText.Empty
        }
        val listItems = when (block) {
            is StudyBlock.BulletList -> block.items
            is StudyBlock.OrderedList -> block.items
            else -> listOf(text)
        }
        val updated: StudyBlock = when (op.newType) {
            "bullet" -> StudyBlock.BulletList(
                id = block.id, items = listItems,
                alignment = block.alignment, fontFamily = block.fontFamily, fontSize = block.fontSize,
            )
            "numbered" -> StudyBlock.OrderedList(
                id = block.id, items = listItems,
                alignment = block.alignment, fontFamily = block.fontFamily, fontSize = block.fontSize,
            )
            "heading1" -> StudyBlock.Heading(
                id = block.id, level = 1, text = text,
                alignment = block.alignment, fontFamily = block.fontFamily, fontSize = 32,
            )
            "heading2" -> StudyBlock.Heading(
                id = block.id, level = 2, text = text,
                alignment = block.alignment, fontFamily = block.fontFamily, fontSize = 24,
            )
            "heading3" -> StudyBlock.Heading(
                id = block.id, level = 3, text = text,
                alignment = block.alignment, fontFamily = block.fontFamily, fontSize = 20,
            )
            "quote" -> StudyBlock.Quote(
                id = block.id, text = text,
                alignment = block.alignment, fontFamily = block.fontFamily, fontSize = block.fontSize,
            )
            else -> StudyBlock.Paragraph(
                id = block.id, text = text,
                alignment = block.alignment, fontFamily = block.fontFamily, fontSize = block.fontSize,
            )
        }
        val newBlocks = doc.blocks.toMutableList()
        newBlocks[idx] = updated
        return doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()) to OpResult.Success
    }

    private fun splitBlock(doc: StudyDoc, op: StudyOp.SplitBlock): Pair<StudyDoc, OpResult> {
        val splitIdx = doc.blocks.indexOfFirst { it.id == op.splitBlockId }
        if (splitIdx < 0) return doc to OpResult.Failed("Block not found")
        if (doc.blocks[splitIdx] is StudyBlock.Verse) {
            return doc to OpResult.Failed("Bible citations cannot be split")
        }
        if (op.updatedSplitBlock != null && op.updatedSplitBlock.id != op.splitBlockId) {
            return doc to OpResult.Failed("Updated split block must keep the original id")
        }
        val updatedSplitBlock = op.updatedSplitBlock?.let(::normalizeForMutation)
        val newBlock = normalizeForMutation(op.newBlock)
        updatedSplitBlock?.let { invalidReason(it) }?.let {
            return doc to OpResult.Failed(it)
        }
        invalidReason(newBlock)?.let { return doc to OpResult.Failed(it) }
        val newBlocks = doc.blocks.toMutableList()
        updatedSplitBlock?.let { newBlocks[splitIdx] = it }
        newBlocks.add(splitIdx + 1, newBlock)
        return doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()) to OpResult.Success
    }

    private fun mergeBlock(doc: StudyDoc, op: StudyOp.MergeBlock): Pair<StudyDoc, OpResult> {
        val removeIdx = doc.blocks.indexOfFirst { it.id == op.removeBlockId }
        if (removeIdx <= 0) return doc to OpResult.Failed("Cannot merge first block")
        val targetIdx = removeIdx - 1
        if (op.updatedTargetBlock != null && op.updatedTargetBlock.id != doc.blocks[targetIdx].id) {
            return doc to OpResult.Failed("Updated merge target must keep the previous block id")
        }
        if (doc.blocks[removeIdx] is StudyBlock.Verse || doc.blocks[targetIdx] is StudyBlock.Verse) {
            return doc to OpResult.Failed("Bible citations cannot be merged with text blocks")
        }
        val updatedTargetBlock = op.updatedTargetBlock?.let(::normalizeForMutation)
        updatedTargetBlock?.let { invalidReason(it) }?.let {
            return doc to OpResult.Failed(it)
        }
        val newBlocks = doc.blocks.toMutableList()
        updatedTargetBlock?.let { newBlocks[targetIdx] = it }
        newBlocks.removeAt(removeIdx)
        return doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()) to OpResult.Success
    }

    private fun updateTitle(doc: StudyDoc, op: StudyOp.UpdateTitle): Pair<StudyDoc, OpResult> {
        return doc.copy(title = op.newTitle, updatedAt = System.currentTimeMillis()) to OpResult.Success
    }

    private fun updateMetadata(doc: StudyDoc, op: StudyOp.UpdateMetadata): Pair<StudyDoc, OpResult> {
        return doc.copy(metadata = op.metadata, updatedAt = System.currentTimeMillis()) to OpResult.Success
    }

    private fun applyAll(doc: StudyDoc, op: StudyOp.BulkApply): Pair<StudyDoc, OpResult> {
        var current = doc
        for (sub in op.ops) {
            val (next, result) = apply(current, sub)
            if (result !is OpResult.Success) return next to result
            current = next
        }
        return current to OpResult.Success
    }

    private fun normalizeForMutation(block: StudyBlock): StudyBlock = when (block) {
        is StudyBlock.Verse -> VerseBusinessRules.normalize(block)
        else -> block
    }

    private fun invalidReason(block: StudyBlock): String? = when (block) {
        is StudyBlock.Verse -> VerseBusinessRules.invalidReason(block)
        else -> null
    }
}

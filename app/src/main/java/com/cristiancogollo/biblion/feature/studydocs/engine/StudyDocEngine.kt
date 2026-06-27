package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

object StudyDocEngine {

    fun apply(doc: StudyDoc, op: StudyOp): Pair<StudyDoc, OpResult> = when (op) {
        is StudyOp.InsertBlock -> applyInsert(doc, op)
        is StudyOp.DeleteBlock -> applyDelete(doc, op)
        is StudyOp.MoveBlock -> applyMove(doc, op)
        is StudyOp.ReplaceBlock -> applyReplace(doc, op)
        is StudyOp.EditText -> applyEditText(doc, op)
        is StudyOp.ApplyStyle -> applyApplyStyle(doc, op)
        is StudyOp.ClearStyle -> applyClearStyle(doc, op)
        is StudyOp.UpdateTitle -> applyUpdateTitle(doc, op)
        is StudyOp.UpdateMetadata -> applyUpdateMetadata(doc, op)
        is StudyOp.BulkApply -> applyBulk(doc, op)
    }

    fun applyAll(doc: StudyDoc, ops: List<StudyOp>): Pair<StudyDoc, List<OpResult>> {
        var current = doc
        val results = mutableListOf<OpResult>()
        for (op in ops) {
            val (newDoc, result) = apply(current, op)
            current = newDoc
            results.add(result)
        }
        return current to results
    }

    fun findText(doc: StudyDoc, query: String): List<TextHit> {
        if (query.isEmpty()) return emptyList()
        val hits = mutableListOf<TextHit>()
        doc.blocks.forEachIndexed { blockIndex, block ->
            val texts: List<String> = when (block) {
                is StudyBlock.Paragraph -> listOf(block.text.plain())
                is StudyBlock.Heading -> listOf(block.text.plain())
                is StudyBlock.Quote -> listOf(block.text.plain())
                is StudyBlock.Note -> listOf(block.text.plain())
                is StudyBlock.Reflection -> listOf(block.text.plain())
                is StudyBlock.Callout -> listOf(block.text.plain())
                is StudyBlock.BulletList -> block.items.map { it.plain() }
                is StudyBlock.NumberedList -> block.items.map { it.plain() }
                is StudyBlock.Verse -> listOf(block.primaryText.plain()) + listOfNotNull(block.compareText?.plain())
                is StudyBlock.Table -> block.rows.flatMap { it.cells }.map { it.plain() }
                is StudyBlock.Divider, is StudyBlock.PageBreak -> emptyList()
                is StudyBlock.TodoList -> block.items.map { it.text.plain() }
                is StudyBlock.ColumnLayout -> block.columnBlocks.flatMap { col -> col.map { it.plainText() } }
                is StudyBlock.Comment -> listOf(block.text)
            }
            texts.forEachIndexed { textIndex, text ->
                var from = 0
                while (true) {
                    val found = text.indexOf(query, from)
                    if (found < 0) break
                    hits.add(TextHit(blockIndex, block.id, textIndex, found, found + query.length))
                    from = found + query.length
                }
            }
        }
        return hits
    }

    data class TextHit(
        val blockIndex: Int,
        val blockId: BlockId,
        val textIndex: Int,
        val start: Int,
        val endExclusive: Int,
    )

    private fun applyInsert(doc: StudyDoc, op: StudyOp.InsertBlock): Pair<StudyDoc, OpResult> {
        val safeIndex = op.atIndex.coerceIn(0, doc.blocks.size)
        if (doc.blocks.any { it.id == op.block.id }) {
            return doc to OpResult.Failed("duplicate block id")
        }
        val newBlocks = doc.blocks.toMutableList().apply { add(safeIndex, op.block) }
        return doc.copy(blocks = newBlocks, version = doc.version + 1) to
            OpResult.Ok(listOf(op.block.id))
    }

    private fun applyDelete(doc: StudyDoc, op: StudyOp.DeleteBlock): Pair<StudyDoc, OpResult> {
        val newBlocks = doc.blocks.filterNot { it.id == op.blockId }
        if (newBlocks.size == doc.blocks.size) {
            return doc to OpResult.Failed("block not found")
        }
        return doc.copy(blocks = newBlocks, version = doc.version + 1) to
            OpResult.Ok(listOf(op.blockId))
    }

    private fun applyMove(doc: StudyDoc, op: StudyOp.MoveBlock): Pair<StudyDoc, OpResult> {
        val from = doc.blocks.indexOfFirst { it.id == op.blockId }
        if (from < 0) return doc to OpResult.Failed("block not found")
        val to = op.toIndex.coerceIn(0, doc.blocks.size - 1)
        if (from == to) return doc to OpResult.Ok(listOf(op.blockId))
        val newBlocks = doc.blocks.toMutableList().apply {
            removeAt(from)
            add(to, doc.blocks[from])
        }
        return doc.copy(blocks = newBlocks, version = doc.version + 1) to
            OpResult.Ok(listOf(op.blockId))
    }

    private fun applyReplace(doc: StudyDoc, op: StudyOp.ReplaceBlock): Pair<StudyDoc, OpResult> {
        val idx = doc.blocks.indexOfFirst { it.id == op.blockId }
        if (idx < 0) return doc to OpResult.Failed("block not found")
        if (op.newBlock.id != op.blockId) {
            return doc to OpResult.Failed("newBlock id does not match target")
        }
        val newBlocks = doc.blocks.toMutableList()
        newBlocks[idx] = op.newBlock
        return doc.copy(blocks = newBlocks, version = doc.version + 1) to
            OpResult.Ok(listOf(op.blockId))
    }

    private fun applyEditText(doc: StudyDoc, op: StudyOp.EditText): Pair<StudyDoc, OpResult> {
        val idx = doc.blocks.indexOfFirst { it.id == op.blockId }
        if (idx < 0) return doc to OpResult.Failed("block not found")
        val current = doc.blocks[idx]
        val newText = op.newText
        val updated = when (current) {
            is StudyBlock.Paragraph -> current.copy(text = newText)
            is StudyBlock.Heading -> current.copy(text = newText)
            is StudyBlock.Quote -> current.copy(text = newText)
            is StudyBlock.Note -> current.copy(text = newText)
            is StudyBlock.Reflection -> current.copy(text = newText)
            is StudyBlock.Callout -> current.copy(text = newText)
            is StudyBlock.Verse -> current.copy(primaryText = newText)
            else -> return doc to OpResult.Failed("block type does not support text edit")
        }
        val newBlocks = doc.blocks.toMutableList()
        newBlocks[idx] = updated
        return doc.copy(blocks = newBlocks, version = doc.version + 1) to
            OpResult.Ok(listOf(op.blockId))
    }

    private fun applyApplyStyle(doc: StudyDoc, op: StudyOp.ApplyStyle): Pair<StudyDoc, OpResult> {
        val idx = doc.blocks.indexOfFirst { it.id == op.blockId }
        if (idx < 0) return doc to OpResult.Failed("block not found")
        val current = doc.blocks[idx]
        val updated = when (current) {
            is StudyBlock.Paragraph -> current.copy(text = current.text.withStyle(op.range, op.patch))
            is StudyBlock.Heading -> current.copy(text = current.text.withStyle(op.range, op.patch))
            is StudyBlock.Quote -> current.copy(text = current.text.withStyle(op.range, op.patch))
            is StudyBlock.Note -> current.copy(text = current.text.withStyle(op.range, op.patch))
            is StudyBlock.Reflection -> current.copy(text = current.text.withStyle(op.range, op.patch))
            is StudyBlock.Callout -> current.copy(text = current.text.withStyle(op.range, op.patch))
            is StudyBlock.Verse -> current.copy(primaryText = current.primaryText.withStyle(op.range, op.patch))
            else -> return doc to OpResult.Failed("block type does not support style")
        }
        val newBlocks = doc.blocks.toMutableList()
        newBlocks[idx] = updated
        return doc.copy(blocks = newBlocks, version = doc.version + 1) to
            OpResult.Ok(listOf(op.blockId))
    }

    private fun applyClearStyle(doc: StudyDoc, op: StudyOp.ClearStyle): Pair<StudyDoc, OpResult> {
        val idx = doc.blocks.indexOfFirst { it.id == op.blockId }
        if (idx < 0) return doc to OpResult.Failed("block not found")
        val current = doc.blocks[idx]
        val updated = when (current) {
            is StudyBlock.Paragraph -> current.copy(text = current.text.clearStyle(op.range, op.kind.name))
            is StudyBlock.Heading -> current.copy(text = current.text.clearStyle(op.range, op.kind.name))
            is StudyBlock.Quote -> current.copy(text = current.text.clearStyle(op.range, op.kind.name))
            is StudyBlock.Note -> current.copy(text = current.text.clearStyle(op.range, op.kind.name))
            is StudyBlock.Reflection -> current.copy(text = current.text.clearStyle(op.range, op.kind.name))
            is StudyBlock.Callout -> current.copy(text = current.text.clearStyle(op.range, op.kind.name))
            else -> return doc to OpResult.Failed("block type does not support clear style")
        }
        val newBlocks = doc.blocks.toMutableList()
        newBlocks[idx] = updated
        return doc.copy(blocks = newBlocks, version = doc.version + 1) to
            OpResult.Ok(listOf(op.blockId))
    }

    private fun applyUpdateTitle(doc: StudyDoc, op: StudyOp.UpdateTitle): Pair<StudyDoc, OpResult> {
        if (doc.title == op.newTitle) return doc to OpResult.Ok()
        return doc.copy(title = op.newTitle, version = doc.version + 1) to OpResult.Ok()
    }

    private fun applyUpdateMetadata(doc: StudyDoc, op: StudyOp.UpdateMetadata): Pair<StudyDoc, OpResult> {
        if (doc.metadata == op.newMetadata) return doc to OpResult.Ok()
        return doc.copy(metadata = op.newMetadata, version = doc.version + 1) to OpResult.Ok()
    }

    private fun applyBulk(doc: StudyDoc, op: StudyOp.BulkApply): Pair<StudyDoc, OpResult> {
        var current = doc
        for (inner in op.ops) {
            val (newDoc, result) = apply(current, inner)
            if (!result.isSuccess) return current to result
            current = newDoc
        }
        return current to OpResult.Ok()
    }
}

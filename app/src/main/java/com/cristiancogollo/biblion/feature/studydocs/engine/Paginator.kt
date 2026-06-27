package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

object Paginator {

    data class Layout(
        val pageHeight: Float = 36f,
        val paragraphCharsPerLine: Int = 80,
        val blockSpacing: Float = 0.5f,
    ) {
        init {
            require(pageHeight > 0) { "pageHeight debe ser positivo" }
            require(paragraphCharsPerLine > 0) { "paragraphCharsPerLine debe ser positivo" }
            require(blockSpacing >= 0) { "blockSpacing no puede ser negativo" }
        }
    }

    fun paginate(blocks: List<StudyBlock>, layout: Layout = Layout()): List<List<StudyBlock>> {
        if (blocks.isEmpty()) return listOf(emptyList())

        val pages = mutableListOf<List<StudyBlock>>()
        var current = mutableListOf<StudyBlock>()
        var currentHeight = 0f

        for (block in blocks) {
            if (block is StudyBlock.PageBreak) {
                pages.add(current)
                current = mutableListOf()
                currentHeight = 0f
                continue
            }

            val blockHeight = estimateHeight(block, layout)
            val projected = currentHeight + blockHeight + layout.blockSpacing

            if (current.isNotEmpty() && projected > layout.pageHeight) {
                pages.add(current)
                current = mutableListOf()
                currentHeight = 0f
            }

            current.add(block)
            currentHeight += blockHeight + layout.blockSpacing
        }

        if (current.isNotEmpty()) {
            pages.add(current)
        }

        if (pages.isNotEmpty() && pages.last().isEmpty()) {
            pages.removeAt(pages.lastIndex)
        }

        return pages.ifEmpty { listOf(emptyList()) }
    }

    fun estimateHeight(block: StudyBlock, layout: Layout = Layout()): Float = when (block) {
        is StudyBlock.Paragraph -> {
            val chars = block.text.plain().length.coerceAtLeast(1)
            val lines = (chars + layout.paragraphCharsPerLine - 1) / layout.paragraphCharsPerLine
            lines.toFloat().coerceAtLeast(1f)
        }
        is StudyBlock.Heading -> 1.5f
        is StudyBlock.BulletList -> (block.items.size.coerceAtLeast(1) * 1.2f)
        is StudyBlock.NumberedList -> (block.items.size.coerceAtLeast(1) * 1.2f)
        is StudyBlock.Quote -> {
            val chars = block.text.plain().length.coerceAtLeast(1)
            val lines = (chars + layout.paragraphCharsPerLine - 1) / layout.paragraphCharsPerLine
            (lines.toFloat() * 1.4f).coerceAtLeast(1.4f)
        }
        is StudyBlock.Note -> {
            val chars = block.text.plain().length.coerceAtLeast(1)
            val lines = (chars + layout.paragraphCharsPerLine - 1) / layout.paragraphCharsPerLine
            1f + lines.toFloat()
        }
        is StudyBlock.Reflection -> {
            val chars = block.text.plain().length.coerceAtLeast(1)
            val lines = (chars + layout.paragraphCharsPerLine - 1) / layout.paragraphCharsPerLine
            1f + lines.toFloat()
        }
        is StudyBlock.Callout -> {
            val chars = block.text.plain().length.coerceAtLeast(1)
            val lines = (chars + layout.paragraphCharsPerLine - 1) / layout.paragraphCharsPerLine
            1f + lines.toFloat()
        }
        is StudyBlock.Divider -> 0.5f
        is StudyBlock.PageBreak -> 0f
        is StudyBlock.Verse -> {
            val chars = block.primaryText.plain().length.coerceAtLeast(1)
            val lines = (chars + layout.paragraphCharsPerLine - 1) / layout.paragraphCharsPerLine
            val withCompare = if (block.compareText != null) lines * 2 else lines
            1f + withCompare.toFloat()
        }
        is StudyBlock.Table -> {
            val rows = block.rows.size.coerceAtLeast(1)
            val cols = (block.rows.firstOrNull()?.cells?.size ?: 1).coerceAtLeast(1)
            (rows * 1.2f) + (cols * 0.05f)
        }
        is StudyBlock.TodoList -> (block.items.size.coerceAtLeast(1) * 1.2f)
        is StudyBlock.ColumnLayout -> {
            val maxColHeight = block.columnBlocks.maxOfOrNull { col ->
                col.sumOf { estimateHeight(it, layout).toDouble() }.toFloat()
            } ?: 0f
            maxColHeight.coerceAtLeast(1f)
        }
        is StudyBlock.Comment -> 0.8f
    }

    fun totalPages(blocks: List<StudyBlock>, layout: Layout = Layout()): Int = paginate(blocks, layout).size
}

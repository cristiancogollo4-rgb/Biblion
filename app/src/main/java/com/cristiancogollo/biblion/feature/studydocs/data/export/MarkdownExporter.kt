package com.cristiancogollo.biblion.feature.studydocs.data.export

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

object MarkdownExporter {

    fun export(doc: StudyDoc): String = buildString {
        if (doc.title.isNotBlank()) {
            appendLine("# ${doc.title}")
            appendLine()
        }

        doc.blocks.forEach { block ->
            append(blockToMarkdown(block))
            appendLine()
        }
    }

    private fun blockToMarkdown(block: StudyBlock): String = when (block) {
        is StudyBlock.Paragraph -> styledTextToMarkdown(block.text)
        is StudyBlock.Heading -> {
            val prefix = "#".repeat(block.level.coerceIn(1, 6))
            "$prefix ${styledTextToMarkdown(block.text)}"
        }
        is StudyBlock.Quote -> {
            val lines = block.text.raw.lines()
            lines.joinToString("\n") { "> $it" } +
                if (block.attribution != null) "\n> — ${block.attribution}" else ""
        }
        is StudyBlock.BulletList -> block.items.joinToString("\n") {
            "- ${styledTextToMarkdown(it)}"
        }
        is StudyBlock.NumberedList -> block.items.mapIndexed { index, item ->
            "${index + 1}. ${styledTextToMarkdown(item)}"
        }.joinToString("\n")
        is StudyBlock.Verse -> {
            val ref = block.reference.displayShort()
            val text = block.primaryText.raw
            "> **$ref**\n> $text" +
                if (block.compareText != null) "\n>\n> *(${block.compareVersion}): ${block.compareText.raw}*" else ""
        }
        is StudyBlock.Note -> "> [!NOTE]\n> ${block.text.raw}"
        is StudyBlock.Reflection -> {
            val prompt = if (block.prompt != null) "**${block.prompt}**\n\n" else ""
            prompt + block.text.raw
        }
        is StudyBlock.Callout -> "> [!IMPORTANT]\n> ${block.text.raw}"
        is StudyBlock.Divider -> "---"
        is StudyBlock.PageBreak -> "\n<div style=\"page-break-after: always;\"></div>\n"
        is StudyBlock.TodoList -> block.items.joinToString("\n") { item ->
            val checkbox = if (item.checked) "[x]" else "[ ]"
            "- $checkbox ${item.text.raw}"
        }
        is StudyBlock.Table -> tableToMarkdown(block)
        is StudyBlock.ColumnLayout -> {
            block.columnBlocks.joinToString("\n\n---\n\n") { col ->
                col.joinToString("\n\n") { blockToMarkdown(it) }
            }
        }
        is StudyBlock.Comment -> "<!-- ${block.text} -->"
    }

    private fun styledTextToMarkdown(text: StyledText): String {
        if (text.ranges.isEmpty()) return text.raw

        var result = text.raw
        val sortedRanges = text.ranges.sortedByDescending { it.start }

        for (range in sortedRanges) {
            val start = range.start.coerceIn(0, result.length)
            val end = range.endExclusive.coerceIn(start, result.length)
            if (start >= end) continue

            val segment = result.substring(start, end)
            var wrapped = segment

            if (range.bold) wrapped = "**$wrapped**"
            if (range.italic) wrapped = "*$wrapped*"
            if (range.underline) wrapped = "<u>$wrapped</u>"
            if (range.strikethrough) wrapped = "~~$wrapped~~"
            if (range.link != null) wrapped = "[$wrapped](${range.link})"

            result = result.substring(0, start) + wrapped + result.substring(end)
        }

        return result
    }

    private fun tableToMarkdown(block: StudyBlock.Table): String {
        if (block.rows.isEmpty()) return ""

        val maxCols = block.rows.maxOf { it.cells.size }
        if (maxCols == 0) return ""

        val sb = StringBuilder()

        block.rows.forEachIndexed { rowIndex, row ->
            val cells = (0 until maxCols).map { colIndex ->
                row.cells.getOrNull(colIndex)?.raw ?: ""
            }
            sb.appendLine("| " + cells.joinToString(" | ") + " |")

            if (rowIndex == 0 && block.hasHeaderRow) {
                sb.appendLine("| " + cells.joinToString(" | ") { "---" } + " |")
            }
        }

        return sb.toString().trimEnd()
    }
}

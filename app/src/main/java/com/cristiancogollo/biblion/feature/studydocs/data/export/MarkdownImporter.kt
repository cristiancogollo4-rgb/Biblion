package com.cristiancogollo.biblion.feature.studydocs.data.export

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

object MarkdownImporter {

    fun import(markdown: String): StudyDoc {
        val lines = markdown.lines()
        val blocks = mutableListOf<StudyBlock>()
        var title = ""
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            when {
                line.isBlank() -> {
                    i++
                    continue
                }

                line.startsWith("# ") && title.isEmpty() -> {
                    title = line.removePrefix("# ").trim()
                    i++
                }

                line.startsWith("# ") -> {
                    val level = line.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    val text = line.dropWhile { it == '#' || it == ' ' }
                    blocks.add(StudyBlock.Heading(
                        id = BlockId.generate(),
                        level = level,
                        text = markdownToStyledText(text),
                    ))
                    i++
                }

                line.startsWith("> ") -> {
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].startsWith("> ")) {
                        quoteLines.add(lines[i].removePrefix("> "))
                        i++
                    }
                    val content = quoteLines.joinToString("\n")
                    blocks.add(StudyBlock.Quote(
                        id = BlockId.generate(),
                        text = markdownToStyledText(content),
                    ))
                }

                line.startsWith("- ") || line.startsWith("* ") -> {
                    val items = mutableListOf<StyledText>()
                    while (i < lines.size && (lines[i].startsWith("- ") || lines[i].startsWith("* "))) {
                        val itemText = lines[i].removePrefix("- ").removePrefix("* ")
                        items.add(markdownToStyledText(itemText))
                        i++
                    }
                    blocks.add(StudyBlock.BulletList(
                        id = BlockId.generate(),
                        items = items,
                    ))
                }

                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val items = mutableListOf<StyledText>()
                    while (i < lines.size && lines[i].matches(Regex("^\\d+\\.\\s.*"))) {
                        val itemText = lines[i].replaceFirst(Regex("^\\d+\\.\\s"), "")
                        items.add(markdownToStyledText(itemText))
                        i++
                    }
                    blocks.add(StudyBlock.NumberedList(
                        id = BlockId.generate(),
                        items = items,
                    ))
                }

                line.startsWith("- [ ] ") || line.startsWith("- [x] ") -> {
                    val items = mutableListOf<StudyBlock.TodoList.TodoItem>()
                    while (i < lines.size && (lines[i].startsWith("- [ ] ") || lines[i].startsWith("- [x] "))) {
                        val checked = lines[i].startsWith("- [x] ")
                        val text = lines[i].removePrefix("- [ ] ").removePrefix("- [x] ")
                        items.add(StudyBlock.TodoList.TodoItem(
                            checked = checked,
                            text = markdownToStyledText(text),
                        ))
                        i++
                    }
                    blocks.add(StudyBlock.TodoList(
                        id = BlockId.generate(),
                        items = items,
                    ))
                }

                line == "---" -> {
                    blocks.add(StudyBlock.Divider(id = BlockId.generate()))
                    i++
                }

                line.startsWith("| ") -> {
                    val rows = mutableListOf<StudyBlock.Table.TableRow>()
                    var hasHeader = false
                    while (i < lines.size && lines[i].startsWith("| ")) {
                        val rowLine = lines[i]
                        if (rowLine.matches(Regex("^\\|\\s*[-:]+.*\\|$"))) {
                            hasHeader = true
                            i++
                            continue
                        }
                        val cells = rowLine.removePrefix("| ").removeSuffix(" |")
                            .split(" | ")
                            .map { markdownToStyledText(it.trim()) }
                        rows.add(StudyBlock.Table.TableRow(cells = cells))
                        i++
                    }
                    blocks.add(StudyBlock.Table(
                        id = BlockId.generate(),
                        rows = rows,
                        hasHeaderRow = hasHeader,
                    ))
                }

                else -> {
                    blocks.add(StudyBlock.Paragraph(
                        id = BlockId.generate(),
                        text = markdownToStyledText(line),
                    ))
                    i++
                }
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(StudyBlock.Paragraph(id = BlockId.generate(), text = StyledText.Empty))
        }

        return StudyDoc(
            title = title,
            blocks = blocks,
        )
    }

    private fun markdownToStyledText(markdown: String): StyledText {
        var text = markdown
        val ranges = mutableListOf<com.cristiancogollo.biblion.feature.studydocs.model.StyleRange>()

        text = processBold(text, ranges)
        text = processItalic(text, ranges)
        text = processStrikethrough(text, ranges)
        text = processUnderline(text, ranges)

        return StyledText(raw = text, ranges = ranges)
    }

    private fun processBold(text: String, ranges: MutableList<com.cristiancogollo.biblion.feature.studydocs.model.StyleRange>): String {
        val pattern = Regex("\\*\\*(.+?)\\*\\*")
        var result = text
        var offset = 0

        pattern.findAll(text).forEach { match ->
            val start = match.range.first - offset
            val end = start + match.groupValues[1].length
            ranges.add(com.cristiancogollo.biblion.feature.studydocs.model.StyleRange(
                start = start,
                endExclusive = end,
                bold = true,
            ))
            result = result.substring(0, match.range.first - offset) +
                match.groupValues[1] +
                result.substring(match.range.last + 1 - offset)
            offset += 4
        }

        return result
    }

    private fun processItalic(text: String, ranges: MutableList<com.cristiancogollo.biblion.feature.studydocs.model.StyleRange>): String {
        val pattern = Regex("(?<!\\*)\\*(.+?)\\*(?!\\*)")
        var result = text
        var offset = 0

        pattern.findAll(text).forEach { match ->
            val start = match.range.first - offset
            val end = start + match.groupValues[1].length
            ranges.add(com.cristiancogollo.biblion.feature.studydocs.model.StyleRange(
                start = start,
                endExclusive = end,
                italic = true,
            ))
            result = result.substring(0, match.range.first - offset) +
                match.groupValues[1] +
                result.substring(match.range.last + 1 - offset)
            offset += 2
        }

        return result
    }

    private fun processStrikethrough(text: String, ranges: MutableList<com.cristiancogollo.biblion.feature.studydocs.model.StyleRange>): String {
        val pattern = Regex("~~(.+?)~~")
        var result = text
        var offset = 0

        pattern.findAll(text).forEach { match ->
            val start = match.range.first - offset
            val end = start + match.groupValues[1].length
            ranges.add(com.cristiancogollo.biblion.feature.studydocs.model.StyleRange(
                start = start,
                endExclusive = end,
                strikethrough = true,
            ))
            result = result.substring(0, match.range.first - offset) +
                match.groupValues[1] +
                result.substring(match.range.last + 1 - offset)
            offset += 4
        }

        return result
    }

    private fun processUnderline(text: String, ranges: MutableList<com.cristiancogollo.biblion.feature.studydocs.model.StyleRange>): String {
        val pattern = Regex("<u>(.+?)</u>")
        var result = text
        var offset = 0

        pattern.findAll(text).forEach { match ->
            val start = match.range.first - offset
            val end = start + match.groupValues[1].length
            ranges.add(com.cristiancogollo.biblion.feature.studydocs.model.StyleRange(
                start = start,
                endExclusive = end,
                underline = true,
            ))
            result = result.substring(0, match.range.first - offset) +
                match.groupValues[1] +
                result.substring(match.range.last + 1 - offset)
            offset += 7
        }

        return result
    }
}

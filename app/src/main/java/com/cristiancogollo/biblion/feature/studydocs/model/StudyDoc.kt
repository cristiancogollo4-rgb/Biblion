package com.cristiancogollo.biblion.feature.studydocs.model

import com.cristiancogollo.biblion.feature.studydocs.engine.plainText
import kotlinx.serialization.Serializable

@Serializable
data class StudyDoc(
    val id: DocId = DocId.generate(),
    val title: String = "",
    val blocks: List<StudyBlock> = emptyList(),
    val metadata: DocMetadata = DocMetadata.Empty,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    val isEmpty: Boolean get() = title.isBlank() && blocks.isEmpty()

    fun blockById(blockId: BlockId): StudyBlock? = blocks.firstOrNull { it.id == blockId }
    fun indexOfBlock(blockId: BlockId): Int = blocks.indexOfFirst { it.id == blockId }
    fun headings(): List<StudyBlock.Heading> = blocks.filterIsInstance<StudyBlock.Heading>()

    fun plainText(): String = buildString {
        if (title.isNotEmpty()) appendLine(title)
        for (block in blocks) {
            when (block) {
                is StudyBlock.Paragraph -> appendLine(block.text.plain())
                is StudyBlock.Heading -> appendLine(block.text.plain())
                is StudyBlock.BulletList -> block.items.forEach { appendLine("- ${it.plain()}") }
                is StudyBlock.NumberedList -> block.items.forEachIndexed { i, item -> appendLine("${i + 1}. ${item.plain()}") }
                is StudyBlock.Quote -> {
                    appendLine(block.text.plain())
                    if (block.attribution != null) appendLine("— ${block.attribution}")
                }
                is StudyBlock.Table -> block.rows.forEach { row ->
                    appendLine(row.cells.joinToString(" | ") { it.plain() })
                }
                is StudyBlock.Verse -> {
                    appendLine(block.reference.displayShort())
                    appendLine(block.primaryText.plain())
                }
                is StudyBlock.Note -> appendLine("[Nota] ${block.text.plain()}")
                is StudyBlock.Reflection -> {
                    if (block.prompt != null) appendLine("[${block.prompt}]")
                    appendLine(block.text.plain())
                }
                is StudyBlock.Callout -> appendLine("[!] ${block.text.plain()}")
                is StudyBlock.Divider -> appendLine("---")
                is StudyBlock.PageBreak -> appendLine()
                is StudyBlock.TodoList -> block.items.forEach { item ->
                    appendLine("- [${if (item.checked) "x" else " "}] ${item.text.plain()}")
                }
                is StudyBlock.ColumnLayout -> block.columnBlocks.forEach { col ->
                    appendLine(col.joinToString(" | ") { it.plainText() })
                }
                is StudyBlock.Comment -> appendLine("[Comentario] ${block.text}")
            }
        }
    }

    fun wordCount(): Int {
        var count = 0
        val ws = "\\s+".toRegex()
        if (title.isNotBlank()) count += ws.split(title).count { it.isNotBlank() }
        for (block in blocks) {
            count += when (block) {
                is StudyBlock.Paragraph -> ws.split(block.text.plain()).count { it.isNotBlank() }
                is StudyBlock.Heading -> ws.split(block.text.plain()).count { it.isNotBlank() }
                is StudyBlock.BulletList -> block.items.sumOf { ws.split(it.plain()).count { it.isNotBlank() } }
                is StudyBlock.NumberedList -> block.items.sumOf { ws.split(it.plain()).count { it.isNotBlank() } }
                is StudyBlock.Quote -> ws.split(block.text.plain()).count { it.isNotBlank() }
                is StudyBlock.Table -> block.rows.sumOf { row -> row.cells.sumOf { ws.split(it.plain()).count { it.isNotBlank() } } }
                is StudyBlock.Verse -> ws.split(block.primaryText.plain()).count { it.isNotBlank() }
                is StudyBlock.Note -> ws.split(block.text.plain()).count { it.isNotBlank() }
                is StudyBlock.Reflection -> ws.split(block.text.plain()).count { it.isNotBlank() }
                is StudyBlock.Callout -> ws.split(block.text.plain()).count { it.isNotBlank() }
                is StudyBlock.Divider, is StudyBlock.PageBreak -> 0
                is StudyBlock.TodoList -> block.items.sumOf { ws.split(it.text.plain()).count { it.isNotBlank() } }
                is StudyBlock.ColumnLayout -> block.columnBlocks.sumOf { col -> col.sumOf { ws.split(it.plainText()).count { it.isNotBlank() } } }
                is StudyBlock.Comment -> ws.split(block.text).count { it.isNotBlank() }
            }
        }
        return count
    }

    fun charCount(): Int {
        var count = title.length
        for (block in blocks) {
            count += when (block) {
                is StudyBlock.Paragraph -> block.text.length
                is StudyBlock.Heading -> block.text.length
                is StudyBlock.BulletList -> block.items.sumOf { it.length }
                is StudyBlock.NumberedList -> block.items.sumOf { it.length }
                is StudyBlock.Quote -> block.text.length + (block.attribution?.length ?: 0)
                is StudyBlock.Table -> block.rows.sumOf { row -> row.cells.sumOf { it.length } }
                is StudyBlock.Verse -> block.primaryText.length + (block.compareText?.length ?: 0)
                is StudyBlock.Note -> block.text.length
                is StudyBlock.Reflection -> block.text.length + (block.prompt?.length ?: 0)
                is StudyBlock.Callout -> block.text.length
                is StudyBlock.Divider, is StudyBlock.PageBreak -> 0
                is StudyBlock.TodoList -> block.items.sumOf { item: com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.TodoList.TodoItem -> item.text.length }
                is StudyBlock.ColumnLayout -> block.columnBlocks.sumOf { col: List<StudyBlock> -> col.sumOf { b: StudyBlock -> b.plainText().length } }
                is StudyBlock.Comment -> block.text.length
            }
        }
        return count
    }

    companion object {
        fun empty(): StudyDoc = StudyDoc(
            blocks = listOf(StudyBlock.Paragraph(text = StyledText.Empty)),
        )
    }
}

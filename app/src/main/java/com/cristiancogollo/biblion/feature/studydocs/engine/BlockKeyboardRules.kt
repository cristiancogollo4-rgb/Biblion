package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

data class FieldTarget(
    val blockIndex: Int,
    val block: StudyBlock,
    val itemIndex: Int? = null,
)

fun resolveFieldTarget(blocks: List<StudyBlock>, fieldKey: String): FieldTarget? {
    val itemMarker = ":item:"
    val itemIndex = fieldKey.indexOf(itemMarker)
    return if (itemIndex >= 0) {
        val blockKey = fieldKey.substring(0, itemIndex)
        val item = fieldKey.substring(itemIndex + itemMarker.length).toIntOrNull() ?: return null
        val blockIndex = blocks.indexOfFirst { it.id.value == blockKey }
        if (blockIndex < 0) return null
        val block = blocks[blockIndex]
        if (block !is StudyBlock.BulletList && block !is StudyBlock.NumberedList && block !is StudyBlock.TodoList) return null
        FieldTarget(blockIndex = blockIndex, block = block, itemIndex = item)
    } else {
        val blockIndex = blocks.indexOfFirst { it.id.value == fieldKey }
        if (blockIndex < 0) return null
        FieldTarget(blockIndex = blockIndex, block = blocks[blockIndex], itemIndex = null)
    }
}

fun StudyBlock.isImmutableBoundaryBlock(): Boolean = when (this) {
    is StudyBlock.Table,
    is StudyBlock.Divider,
    is StudyBlock.PageBreak,
    is StudyBlock.ColumnLayout,
    is StudyBlock.Comment -> true
    else -> false
}

fun StudyBlock.isDegradableSpecialBlock(): Boolean = when (this) {
    is StudyBlock.Heading,
    is StudyBlock.Quote,
    is StudyBlock.Note,
    is StudyBlock.Reflection,
    is StudyBlock.Callout,
    is StudyBlock.Verse,
    is StudyBlock.BulletList,
    is StudyBlock.NumberedList,
    is StudyBlock.TodoList -> true
    else -> false
}

fun StudyBlock.isTextLikeBlock(): Boolean = when (this) {
    is StudyBlock.Paragraph,
    is StudyBlock.Heading,
    is StudyBlock.Quote,
    is StudyBlock.Note,
    is StudyBlock.Reflection,
    is StudyBlock.Callout,
    is StudyBlock.Verse -> true
    else -> false
}

fun StudyBlock.plainText(): String = when (this) {
    is StudyBlock.Paragraph -> text.plain()
    is StudyBlock.Heading -> text.plain()
    is StudyBlock.Quote -> text.plain()
    is StudyBlock.Note -> text.plain()
    is StudyBlock.Reflection -> text.plain()
    is StudyBlock.Callout -> text.plain()
    is StudyBlock.Verse -> primaryText.plain()
    is StudyBlock.BulletList -> items.joinToString("\n") { it.plain() }
    is StudyBlock.NumberedList -> items.joinToString("\n") { it.plain() }
    is StudyBlock.TodoList -> items.joinToString("\n") { "- [${if (it.checked) "x" else " "}] ${it.text.plain()}" }
    is StudyBlock.Table -> rows.joinToString("\n") { row -> row.cells.joinToString(" | ") { it.plain() } }
    is StudyBlock.ColumnLayout -> columnBlocks.joinToString(" | ") { col -> col.joinToString(" ") { it.plainText() } }
    is StudyBlock.Comment -> text
    is StudyBlock.Divider,
    is StudyBlock.PageBreak -> ""
}

fun StudyBlock.isEffectivelyEmpty(): Boolean = when (this) {
    is StudyBlock.Paragraph -> text.plain().isBlank()
    is StudyBlock.Heading -> text.plain().isBlank()
    is StudyBlock.Quote -> text.plain().isBlank()
    is StudyBlock.Note -> text.plain().isBlank()
    is StudyBlock.Reflection -> text.plain().isBlank()
    is StudyBlock.Callout -> text.plain().isBlank()
    is StudyBlock.Verse -> primaryText.plain().isBlank()
    is StudyBlock.BulletList -> items.isEmpty() || items.all { it.plain().isBlank() }
    is StudyBlock.NumberedList -> items.isEmpty() || items.all { it.plain().isBlank() }
    is StudyBlock.TodoList -> items.isEmpty() || items.all { it.text.plain().isBlank() }
    is StudyBlock.Table -> rows.isEmpty() || rows.all { row -> row.cells.all { it.plain().isBlank() } }
    is StudyBlock.Comment -> text.isBlank()
    is StudyBlock.ColumnLayout -> columnBlocks.all { col -> col.all { it.isEffectivelyEmpty() } }
    is StudyBlock.Divider, is StudyBlock.PageBreak -> true
}

fun StudyBlock.toParagraphBlock(): StudyBlock.Paragraph? = when (this) {
    is StudyBlock.Paragraph -> this
    is StudyBlock.Heading -> StudyBlock.Paragraph(id = id, text = text)
    is StudyBlock.Quote -> StudyBlock.Paragraph(id = id, text = text)
    is StudyBlock.Note -> StudyBlock.Paragraph(id = id, text = text)
    is StudyBlock.Reflection -> StudyBlock.Paragraph(id = id, text = text)
    is StudyBlock.Callout -> StudyBlock.Paragraph(id = id, text = text)
    is StudyBlock.Verse -> StudyBlock.Paragraph(id = id, text = primaryText)
    is StudyBlock.BulletList -> StudyBlock.Paragraph(id = id, text = StyledText.plain(items.joinToString("\n") { it.plain() }))
    is StudyBlock.NumberedList -> StudyBlock.Paragraph(id = id, text = StyledText.plain(items.joinToString("\n") { it.plain() }))
    is StudyBlock.TodoList -> StudyBlock.Paragraph(
        id = id,
        text = StyledText.plain(items.joinToString("\n") { "- [${if (it.checked) "x" else " "}] ${it.text.plain()}" }),
    )
    else -> null
}

fun StudyBlock.appendCurrentTextAfter(previous: StudyBlock): StudyBlock? {
    val combined = previous.plainText() + plainText()
    return when (previous) {
        is StudyBlock.Paragraph -> previous.copy(text = StyledText.plain(combined))
        is StudyBlock.Heading -> previous.copy(text = StyledText.plain(combined))
        is StudyBlock.Quote -> previous.copy(text = StyledText.plain(combined))
        is StudyBlock.Note -> previous.copy(text = StyledText.plain(combined))
        is StudyBlock.Reflection -> previous.copy(text = StyledText.plain(combined))
        is StudyBlock.Callout -> previous.copy(text = StyledText.plain(combined))
        is StudyBlock.Verse -> previous.copy(primaryText = StyledText.plain(combined))
        else -> null
    }
}

fun StudyBlock.splitTextAt(cursor: Int): Pair<StudyBlock, StudyBlock.Paragraph>? {
    if (cursor !in 0..plainText().length) return null
    val raw = plainText()
    val left = raw.substring(0, cursor)
    val right = raw.substring(cursor)
    val current = when (this) {
        is StudyBlock.Paragraph -> copy(text = StyledText.plain(left))
        is StudyBlock.Heading -> copy(text = StyledText.plain(left))
        is StudyBlock.Quote -> copy(text = StyledText.plain(left))
        is StudyBlock.Note -> copy(text = StyledText.plain(left))
        is StudyBlock.Reflection -> copy(text = StyledText.plain(left))
        is StudyBlock.Callout -> copy(text = StyledText.plain(left))
        is StudyBlock.Verse -> copy(primaryText = StyledText.plain(left))
        else -> return null
    }
    val newBlock = StudyBlock.Paragraph(text = StyledText.plain(right))
    return current to newBlock
}

fun StudyBlock.emptyBlockFieldKey(): String = id.value

fun listFieldKey(blockId: BlockId, index: Int): String = "${blockId.value}:item:$index"

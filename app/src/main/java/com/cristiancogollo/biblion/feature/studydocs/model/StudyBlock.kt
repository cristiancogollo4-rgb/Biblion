package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface StudyBlock {
    val id: BlockId

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        override val id: BlockId = BlockId.generate(),
        val text: StyledText = StyledText.Empty,
    ) : StudyBlock

    @Serializable
    @SerialName("heading")
    data class Heading(
        override val id: BlockId = BlockId.generate(),
        val level: Int = 1,
        val text: StyledText = StyledText.Empty,
    ) : StudyBlock

    @Serializable
    @SerialName("bullet_list")
    data class BulletList(
        override val id: BlockId = BlockId.generate(),
        val items: List<StyledText> = emptyList(),
    ) : StudyBlock

    @Serializable
    @SerialName("numbered_list")
    data class NumberedList(
        override val id: BlockId = BlockId.generate(),
        val items: List<StyledText> = emptyList(),
    ) : StudyBlock

    @Serializable
    @SerialName("quote")
    data class Quote(
        override val id: BlockId = BlockId.generate(),
        val text: StyledText = StyledText.Empty,
        val attribution: String? = null,
    ) : StudyBlock

    @Serializable
    @SerialName("table")
    data class Table(
        override val id: BlockId = BlockId.generate(),
        val rows: List<TableRow> = emptyList(),
        val hasHeaderRow: Boolean = true,
    ) : StudyBlock {
        @Serializable
        data class TableRow(
            val cells: List<StyledText> = emptyList(),
        )
    }

    @Serializable
    @SerialName("verse")
    data class Verse(
        override val id: BlockId = BlockId.generate(),
        val reference: VerseRef,
        val primaryText: StyledText = StyledText.Empty,
        val primaryVersion: String = reference.version,
        val compareText: StyledText? = null,
        val compareVersion: String? = null,
    ) : StudyBlock {
        init {
            if (compareText != null) requireNotNull(compareVersion) {
                "compareText sin compareVersion"
            }
        }
    }

    @Serializable
    @SerialName("note")
    data class Note(
        override val id: BlockId = BlockId.generate(),
        val text: StyledText = StyledText.Empty,
    ) : StudyBlock

    @Serializable
    @SerialName("reflection")
    data class Reflection(
        override val id: BlockId = BlockId.generate(),
        val prompt: String? = null,
        val text: StyledText = StyledText.Empty,
    ) : StudyBlock

    @Serializable
    @SerialName("callout")
    data class Callout(
        override val id: BlockId = BlockId.generate(),
        val icon: String = "info",
        val color: Int? = null,
        val text: StyledText = StyledText.Empty,
    ) : StudyBlock

    @Serializable
    @SerialName("divider")
    data class Divider(
        override val id: BlockId = BlockId.generate(),
    ) : StudyBlock

    @Serializable
    @SerialName("page_break")
    data class PageBreak(
        override val id: BlockId = BlockId.generate(),
    ) : StudyBlock

    @Serializable
    @SerialName("todo_list")
    data class TodoList(
        override val id: BlockId = BlockId.generate(),
        val items: List<TodoItem> = emptyList(),
    ) : StudyBlock {
        @Serializable
        data class TodoItem(
            val checked: Boolean = false,
            val text: StyledText = StyledText.Empty,
        )
    }

    @Serializable
    @SerialName("column_layout")
    data class ColumnLayout(
        override val id: BlockId = BlockId.generate(),
        val columnBlocks: List<List<StudyBlock>> = listOf(
            listOf(Paragraph()),
            listOf(Paragraph()),
        ),
    ) : StudyBlock

    @Serializable
    @SerialName("comment")
    data class Comment(
        override val id: BlockId = BlockId.generate(),
        val text: String = "",
        val anchorBlockId: BlockId,
        val anchorStart: Int = 0,
        val anchorEnd: Int = 0,
    ) : StudyBlock
}

val StudyBlock.outlineTitle: String?
    get() = when (this) {
        is StudyBlock.Heading -> text.plain()
        else -> null
    }

val StudyBlock.isTextEditable: Boolean
    get() = when (this) {
        is StudyBlock.Paragraph -> true
        is StudyBlock.Heading -> true
        is StudyBlock.Quote -> true
        is StudyBlock.Note -> true
        is StudyBlock.Reflection -> true
        is StudyBlock.Callout -> true
        else -> false
    }

val StudyBlock.isList: Boolean
    get() = this is StudyBlock.BulletList || this is StudyBlock.NumberedList || this is StudyBlock.TodoList

val StudyBlock.displayTypeName: String
    get() = when (this) {
        is StudyBlock.Paragraph -> "Parrafo"
        is StudyBlock.Heading -> when (level) {
            1 -> "Titulo 1"
            2 -> "Titulo 2"
            3 -> "Titulo 3"
            else -> "Titulo"
        }
        is StudyBlock.Quote -> "Cita"
        is StudyBlock.BulletList -> "Lista"
        is StudyBlock.NumberedList -> "Lista numerada"
        is StudyBlock.TodoList -> "Lista de tareas"
        is StudyBlock.ColumnLayout -> "Columnas"
        else -> ""
    }

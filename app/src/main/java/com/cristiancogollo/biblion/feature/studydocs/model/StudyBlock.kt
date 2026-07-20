package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface StudyBlock {
    val id: BlockId
    val alignment: BlockAlignment
    val fontFamily: String?
    val fontSize: Int

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        override val id: BlockId = BlockId.generate(),
        val text: StyledText = StyledText.Empty,
        override val alignment: BlockAlignment = BlockAlignment.Start,
        override val fontFamily: String? = null,
        override val fontSize: Int = DocConfig.DEFAULT_FONT_SIZE,
    ) : StudyBlock

    @Serializable
    @SerialName("heading")
    data class Heading(
        override val id: BlockId = BlockId.generate(),
        val level: Int = 1,
        val text: StyledText = StyledText.Empty,
        override val alignment: BlockAlignment = BlockAlignment.Start,
        override val fontFamily: String? = null,
        override val fontSize: Int = DocConfig.HEADING1_SIZE,
    ) : StudyBlock {
        companion object {
            fun sizeForLevel(level: Int): Int = when (level) {
                1 -> DocConfig.HEADING1_SIZE
                2 -> DocConfig.HEADING2_SIZE
                3 -> DocConfig.HEADING3_SIZE
                else -> DocConfig.DEFAULT_FONT_SIZE
            }
        }
    }

    @Serializable
    @SerialName("bullet_list")
    data class BulletList(
        override val id: BlockId = BlockId.generate(),
        val items: List<StyledText> = listOf(StyledText.Empty),
        override val alignment: BlockAlignment = BlockAlignment.Start,
        override val fontFamily: String? = null,
        override val fontSize: Int = DocConfig.DEFAULT_FONT_SIZE,
    ) : StudyBlock

    @Serializable
    @SerialName("ordered_list")
    data class OrderedList(
        override val id: BlockId = BlockId.generate(),
        val items: List<StyledText> = listOf(StyledText.Empty),
        override val alignment: BlockAlignment = BlockAlignment.Start,
        override val fontFamily: String? = null,
        override val fontSize: Int = DocConfig.DEFAULT_FONT_SIZE,
    ) : StudyBlock

    @Serializable
    @SerialName("bible_verse")
    data class Verse(
        override val id: BlockId = BlockId.generate(),
        val bookId: String = "",
        val chapter: Int = 1,
        val verseStart: Int = 1,
        val verseEnd: Int = 1,
        val sourceVersion: String = "",
        val contents: Map<String, String> = emptyMap(),
        val showCompare: Boolean = false,
        val comparedVersions: List<String> = emptyList(),
        override val alignment: BlockAlignment = BlockAlignment.Start,
        override val fontFamily: String? = "serif",
        override val fontSize: Int = DocConfig.DEFAULT_FONT_SIZE,
    ) : StudyBlock

    @Serializable
    @SerialName("quote")
    data class Quote(
        override val id: BlockId = BlockId.generate(),
        val text: StyledText = StyledText.Empty,
        val attribution: String? = null,
        override val alignment: BlockAlignment = BlockAlignment.Start,
        override val fontFamily: String? = null,
        override val fontSize: Int = DocConfig.DEFAULT_FONT_SIZE,
    ) : StudyBlock

    fun plainText(): String = when (this) {
        is Paragraph -> text.plain()
        is Heading -> text.plain()
        is BulletList -> items.joinToString("\n") { it.plain() }
        is OrderedList -> items.joinToString("\n") { it.plain() }
        is Verse -> contents[sourceVersion] ?: ""
        is Quote -> text.plain()
    }

    fun toStyledTextList(): List<StyledText> = when (this) {
        is Paragraph -> listOf(text)
        is Heading -> listOf(text)
        is BulletList -> items
        is OrderedList -> items
        is Verse -> listOf(StyledText(contents[sourceVersion] ?: ""))
        is Quote -> listOf(text)
    }
}

package com.cristiancogollo.biblion.feature.studydocs.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class StyleRange(
    val start: Int,
    val endExclusive: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val color: Int? = null,
    val background: Int? = null,
    val fontSizeSp: Float? = null,
    val link: String? = null,
) {
    fun composeWith(other: StyleRange): StyleRange = StyleRange(
        start = minOf(start, other.start),
        endExclusive = maxOf(endExclusive, other.endExclusive),
        bold = bold || other.bold,
        italic = italic || other.italic,
        underline = underline || other.underline,
        strikethrough = strikethrough || other.strikethrough,
        color = color ?: other.color,
        background = background ?: other.background,
        fontSizeSp = fontSizeSp ?: other.fontSizeSp,
        link = link ?: other.link,
    )

    companion object {
        val Empty = StyleRange(start = 0, endExclusive = 0)
    }
}

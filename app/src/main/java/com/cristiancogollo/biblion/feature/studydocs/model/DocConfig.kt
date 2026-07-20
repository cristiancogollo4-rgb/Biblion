package com.cristiancogollo.biblion.feature.studydocs.model

import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
enum class BlockAlignment {
    Start,
    Center,
    End,
    Justify,
}

object DocConfig {
    const val MIN_FONT_SIZE = 8
    const val MAX_FONT_SIZE = 72
    const val DEFAULT_FONT_SIZE = 16
    const val HEADING1_SIZE = 32
    const val HEADING2_SIZE = 24
    const val HEADING3_SIZE = 20
    const val LINE_HEIGHT = 1.5f
}

object PageDimensions {
    /** Hoja carta: 8.5" × 11" a 160dpi → 850dp × 1100dp */
    val LETTER_WIDTH = 850.dp
    val LETTER_HEIGHT = 1100.dp
    /** Márgenes estándar: ~1" = 96dp */
    val PAGE_MARGIN = 48.dp
}

fun BlockAlignment.toTextAlign(): androidx.compose.ui.text.style.TextAlign = when (this) {
    BlockAlignment.Start -> androidx.compose.ui.text.style.TextAlign.Start
    BlockAlignment.Center -> androidx.compose.ui.text.style.TextAlign.Center
    BlockAlignment.End -> androidx.compose.ui.text.style.TextAlign.End
    BlockAlignment.Justify -> androidx.compose.ui.text.style.TextAlign.Justify
}

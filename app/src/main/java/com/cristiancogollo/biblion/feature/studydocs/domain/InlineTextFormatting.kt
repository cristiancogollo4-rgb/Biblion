package com.cristiancogollo.biblion.feature.studydocs.domain

import androidx.compose.ui.text.TextRange
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.fromAnnotatedString
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.syncFromStyledText
import com.mohamedrejeb.richeditor.model.RichTextState

internal fun RichTextState.applyFontSizePreservingStyles(
    textRange: TextRange,
    fontSizeSp: Float,
) {
    if (textRange.collapsed) return
    val selectionBefore = selection
    val resized = StyledText.fromAnnotatedString(annotatedString).withFontSize(
        start = textRange.min,
        endExclusive = textRange.max,
        fontSizeSp = fontSizeSp,
    )
    syncFromStyledText(resized)
    selection = TextRange(
        selectionBefore.start.coerceIn(0, annotatedString.length),
        selectionBefore.end.coerceIn(0, annotatedString.length),
    )
}

internal fun StudyBlock.withBaseFontSize(fontSize: Int): StudyBlock = when (this) {
    is StudyBlock.Paragraph -> copy(fontSize = fontSize)
    is StudyBlock.Heading -> copy(fontSize = fontSize)
    is StudyBlock.BulletList -> copy(fontSize = fontSize)
    is StudyBlock.OrderedList -> copy(fontSize = fontSize)
    is StudyBlock.Quote -> copy(fontSize = fontSize)
    is StudyBlock.Verse -> this
}

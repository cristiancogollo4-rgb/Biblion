package com.cristiancogollo.biblion.feature.studydocs.domain

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.fromAnnotatedString
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.syncFromStyledText
import com.mohamedrejeb.richeditor.model.RichTextState

internal enum class InlineColorTarget { TEXT, BACKGROUND }

internal fun RichTextState.setInlineColor(target: InlineColorTarget, color: Int?) {
    val selected = selection
    if (selected.collapsed) {
        if (color != null) {
            val style = when (target) {
                InlineColorTarget.TEXT -> SpanStyle(color = Color(color))
                InlineColorTarget.BACKGROUND -> SpanStyle(background = Color(color))
            }
            addSpanStyle(style)
            return
        }
        val current = currentSpanStyle
        clearSpanStyles()
        val preserved = when (target) {
            InlineColorTarget.TEXT -> current.copy(color = Color.Unspecified)
            InlineColorTarget.BACKGROUND -> current.copy(background = Color.Unspecified)
        }
        if (preserved != SpanStyle()) addSpanStyle(preserved)
        return
    }
    val updated = StyledText.fromAnnotatedString(annotatedString).withColor(
        start = selected.min,
        endExclusive = selected.max,
        color = color,
        background = target == InlineColorTarget.BACKGROUND,
    )
    syncFromStyledText(updated)
    selection = selected
}

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
    is StudyBlock.Verse -> copy(fontSize = fontSize)
}

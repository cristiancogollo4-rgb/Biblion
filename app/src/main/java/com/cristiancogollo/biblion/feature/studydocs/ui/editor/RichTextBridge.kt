package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDecoration.Companion.LineThrough
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.StyleRange
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.mohamedrejeb.richeditor.model.RichTextState

fun StyledText.toAnnotatedString(): AnnotatedString {
    if (raw.isEmpty()) return AnnotatedString("")
    if (ranges.isEmpty()) return AnnotatedString(raw)

    val sorted = ranges
        .filter { it.start in 0..raw.length && it.endExclusive in 0..raw.length && it.start < it.endExclusive }
        .sortedWith(compareBy({ it.start }, { it.endExclusive }))

    if (sorted.isEmpty()) return AnnotatedString(raw)

    val boundaries = buildSet {
        add(0); add(raw.length)
        sorted.forEach { add(it.start); add(it.endExclusive) }
    }.sorted()

    return buildAnnotatedString {
        append(raw)
        for (index in 0 until boundaries.size - 1) {
            val start = boundaries[index]
            val end = boundaries[index + 1]
            if (start >= end) continue
            val active = sorted.filter { it.start <= start && it.endExclusive >= end }
            if (active.isEmpty()) continue
            val merged = active.reduce { acc, r -> r.composeWith(acc) }
            addStyle(
                SpanStyle(
                    fontWeight = if (merged.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (merged.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = buildTextDecoration(merged),
                    color = merged.color?.let { Color(it) } ?: Color.Unspecified,
                    background = merged.background?.let { Color(it) } ?: Color.Unspecified,
                    fontSize = merged.fontSizeSp?.takeIf { it > 0f }?.sp ?: TextUnit.Unspecified,
                ),
                start,
                end,
            )
        }
    }
}

fun StyledText.Companion.fromAnnotatedString(annotated: AnnotatedString): StyledText {
    if (annotated.text.isEmpty()) return StyledText.Empty
    val raw = annotated.text
    val spanStyles: List<AnnotatedString.Range<SpanStyle>> = annotated.spanStyles
    val ranges = spanStyles.mapNotNull { range ->
        val start = range.start
        val end = range.end
        val s = range.item
        if (start >= end) return@mapNotNull null
        StyleRange(
            start = start,
            endExclusive = end,
            bold = s.fontWeight == FontWeight.Bold,
            italic = s.fontStyle == FontStyle.Italic,
            underline = s.textDecoration?.contains(Underline) == true,
            strikethrough = s.textDecoration?.contains(LineThrough) == true,
            color = s.color.takeIf { it != Color.Unspecified }?.toArgb(),
            background = s.background.takeIf { it != Color.Unspecified }?.toArgb(),
            fontSizeSp = s.fontSize.takeIf { it.type == TextUnitType.Sp }?.value,
        )
    }
    return StyledText(raw = raw, ranges = ranges)
}

fun RichTextState.syncFromStyledText(text: StyledText) {
    val current = StyledText.fromAnnotatedString(annotatedString)
    if (current == text) return
    val selBefore = selection
    setHtml(text.toRichHtml())
    if (selBefore.max <= annotatedString.length) {
        selection = selBefore
    }
}

fun StyledText.toRichHtml(): String = buildString {
    append("<p>")
    append(escapeHtml(raw).replace("\n", "<br/>"))
    append("</p>")
}

private fun buildTextDecoration(range: StyleRange): TextDecoration? {
    val decorations = mutableListOf<TextDecoration>()
    if (range.underline) decorations.add(Underline)
    if (range.strikethrough) decorations.add(LineThrough)
    return if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
}

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.StyleRange
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.mohamedrejeb.richeditor.model.RichTextState

fun StyledText.toRichHtml(): String {
    val ranges = ranges
        .filter { it.start in 0..raw.length && it.endExclusive in 0..raw.length && it.start < it.endExclusive }
        .sortedWith(compareBy({ it.start }, { it.endExclusive }))

    if (ranges.isEmpty()) {
        return "<p>${escapeHtml(raw).replace("\n", "<br/>")}</p>"
    }

    val boundaries = buildSet {
        add(0)
        add(raw.length)
        ranges.forEach {
            add(it.start)
            add(it.endExclusive)
        }
    }.sorted()

    val html = StringBuilder()
    html.append("<p>")
    for (index in 0 until boundaries.size - 1) {
        val start = boundaries[index]
        val end = boundaries[index + 1]
        if (start >= end) continue
        val active = ranges.filter { it.start <= start && it.endExclusive >= end }
        val segment = raw.substring(start, end)
        if (segment.isEmpty()) continue
        val opening = active.joinToString(separator = "") { openTag(it) }
        val closing = active.asReversed().joinToString(separator = "") { closeTag(it) }
        html.append(opening)
        html.append(escapeHtml(segment).replace("\n", "<br/>"))
        html.append(closing)
    }
    html.append("</p>")
    return html.toString()
}

fun StyledText.Companion.fromAnnotatedString(annotatedString: AnnotatedString): StyledText {
    val text = annotatedString.text
    if (text.isEmpty()) return StyledText.Empty

    val ranges = mutableListOf<StyleRange>()
    val boundaries = buildSet {
        add(0)
        add(text.length)
        annotatedString.spanStyles.forEach {
            add(it.start)
            add(it.end)
        }
    }.sorted()

    for (index in 0 until boundaries.size - 1) {
        val start = boundaries[index]
        val end = boundaries[index + 1]
        if (start >= end) continue
        val active = annotatedString.spanStyles.filter { it.start <= start && it.end >= end }
        if (active.isEmpty()) continue

        ranges += StyleRange(
            start = start,
            endExclusive = end,
            bold = active.any { it.item.fontWeight == FontWeight.Bold },
            italic = active.any { it.item.fontStyle == FontStyle.Italic },
            underline = active.any { it.item.textDecoration?.contains(TextDecoration.Underline) == true },
            strikethrough = active.any { it.item.textDecoration?.contains(TextDecoration.LineThrough) == true },
            color = lastSpecifiedColor(active.map { it.item.color }),
            background = lastSpecifiedColor(active.map { it.item.background }),
            fontSizeSp = lastSpecifiedFontSize(active.map { it.item.fontSize }),
            link = null,
        )
    }

    return StyledText(
        raw = text,
        ranges = mergeStyledRanges(ranges),
    )
}

fun RichTextState.syncFromStyledText(text: StyledText) {
    val current = StyledText.fromAnnotatedString(this.annotatedString)
    if (current == text) return
    val selectionBefore = selection
    setHtml(text.toRichHtml())
    if (selectionBefore.max <= annotatedString.length) {
        selection = selectionBefore
    }
}

fun richTextSelectionRangeOrNull(selectionStart: Int, selectionEnd: Int): IntRange? {
    if (selectionEnd <= selectionStart) return null
    return selectionStart until selectionEnd
}

private fun mergeStyledRanges(input: List<StyleRange>): List<StyleRange> {
    if (input.size < 2) return input
    val sorted = input.sortedWith(compareBy({ it.start }, { it.endExclusive }))
    val merged = mutableListOf(sorted.first())
    for (index in 1 until sorted.size) {
        val previous = merged.last()
        val current = sorted[index]
        if (current.start == previous.endExclusive && sameStyle(previous, current)) {
            merged[merged.lastIndex] = previous.copy(endExclusive = current.endExclusive)
        } else {
            merged.add(current)
        }
    }
    return merged
}

private fun sameStyle(a: StyleRange, b: StyleRange): Boolean =
    a.bold == b.bold &&
        a.italic == b.italic &&
        a.underline == b.underline &&
        a.strikethrough == b.strikethrough &&
        a.color == b.color &&
        a.background == b.background &&
        a.fontSizeSp == b.fontSizeSp &&
        a.link == b.link

private fun openTag(range: StyleRange): String = buildString {
    if (range.bold) append("<b>")
    if (range.italic) append("<i>")
    if (range.underline) append("<u>")
    if (range.strikethrough) append("<s>")
    range.fontSizeSp?.let { append("<span style=\"font-size:${it}px\">") }
    range.color?.let { append("<span style=\"color:${argbToHex(it)}\">") }
    range.background?.let { append("<span style=\"background-color:${argbToHex(it)}\">") }
    range.link?.let { append("<a href=\"${escapeHtmlAttribute(it)}\">") }
}

private fun closeTag(range: StyleRange): String = buildString {
    range.link?.let { append("</a>") }
    range.background?.let { append("</span>") }
    range.color?.let { append("</span>") }
    range.fontSizeSp?.let { append("</span>") }
    if (range.strikethrough) append("</s>")
    if (range.underline) append("</u>")
    if (range.italic) append("</i>")
    if (range.bold) append("</b>")
}

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun escapeHtmlAttribute(value: String): String = escapeHtml(value)
    .replace("\"", "&quot;")

private fun argbToHex(argb: Int): String {
    val hex = argb.toUInt().toString(16).padStart(8, '0')
    return "#${hex.substring(2)}"
}

private fun lastSpecifiedColor(values: List<Color>): Int? {
    val color = values.lastOrNull { it != Color.Unspecified } ?: return null
    return color.toArgb()
}

private fun lastSpecifiedFontSize(values: List<TextUnit>): Float? {
    val fontSize = values.lastOrNull { it.isSpecified } ?: return null
    return fontSize.value
}

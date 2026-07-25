package com.cristiancogollo.biblion.feature.studydocs.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDecoration.Companion.LineThrough
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import kotlinx.serialization.Serializable

@Serializable
data class StyledText(
    val raw: String = "",
    val ranges: List<StyleRange> = emptyList(),
) {
    val length: Int get() = raw.length

    fun plain(): String = raw

    /**
     * Applies an inline font size without replacing any other visual attribute.
     *
     * Ranges may overlap because each toolbar action can add one independently.
     * Flattening at every boundary first makes the resulting style deterministic
     * and prevents a font-size range from hiding an existing color range.
     */
    fun withFontSize(
        start: Int,
        endExclusive: Int,
        fontSizeSp: Float,
    ): StyledText {
        val safeStart = start.coerceIn(0, raw.length)
        val safeEnd = endExclusive.coerceIn(safeStart, raw.length)
        if (safeStart >= safeEnd || fontSizeSp <= 0f) return this

        val validRanges = ranges
            .filter { it.start in 0 until raw.length && it.endExclusive in 1..raw.length && it.start < it.endExclusive }
            .sortedWith(compareBy({ it.start }, { it.endExclusive }))
        val boundaries = buildSet {
            add(0)
            add(raw.length)
            add(safeStart)
            add(safeEnd)
            validRanges.forEach {
                add(it.start)
                add(it.endExclusive)
            }
        }.sorted()

        val flattened = buildList {
            for (index in 0 until boundaries.lastIndex) {
                val segmentStart = boundaries[index]
                val segmentEnd = boundaries[index + 1]
                if (segmentStart >= segmentEnd) continue

                val active = validRanges.filter {
                    it.start <= segmentStart && it.endExclusive >= segmentEnd
                }
                val merged = active.reduceOrNull { acc, range -> range.composeWith(acc) }
                    ?: StyleRange(segmentStart, segmentEnd)
                val resized = if (segmentStart >= safeStart && segmentEnd <= safeEnd) {
                    merged.copy(fontSizeSp = fontSizeSp)
                } else {
                    merged
                }
                if (resized.hasVisualStyle()) {
                    add(resized.copy(start = segmentStart, endExclusive = segmentEnd))
                }
            }
        }

        return copy(ranges = flattened.mergeAdjacentStyles())
    }

    /**
     * Devuelve un trozo inmutable de este texto acotado a [range] (half-open tipo `until`).
     * Los rangos de estilo que caen total o parcialmente dentro del corte se conservan
     * clamping y restando [range.first]; los externos se descartan.
     *
     * Usado por la paginacion visual para partir un [StudyBlock] entre paginas sin mutar
     * el documento original.
     */
    fun slice(range: IntRange): StyledText {
        val lo = range.first.coerceAtLeast(0)
        val hi = (range.last + 1).coerceAtMost(raw.length)
        if (lo >= hi) return StyledText.Empty
        val newRaw = raw.substring(lo, hi)
        val newRanges = ranges.mapNotNull { r ->
            val newStart = r.start.coerceIn(lo, hi) - lo
            val newEnd = r.endExclusive.coerceIn(lo, hi) - lo
            if (newStart >= newEnd) null
            else r.copy(start = newStart, endExclusive = newEnd)
        }
        return StyledText(raw = newRaw, ranges = newRanges)
    }

    fun withText(range: IntRange, replacement: String): StyledText {
        if (range.first < 0 || range.last > raw.length || range.first > range.last) return this
        val newRaw = raw.substring(0, range.first) + replacement + raw.substring(range.last + 1)
        val replacementLen = replacement.length
        val delta = replacementLen - (range.last - range.first + 1)
        val newRanges = ranges.mapNotNull { r ->
            when {
                r.endExclusive <= range.first -> r
                r.start >= range.last + 1 -> r.copy(start = r.start + delta, endExclusive = r.endExclusive + delta)
                r.start == range.first && r.endExclusive == range.last + 1 ->
                    r.copy(start = range.first, endExclusive = range.first + replacementLen)
                r.start >= range.first && r.endExclusive <= range.last + 1 -> null
                else -> {
                    val newStart = if (r.start < range.first) r.start else range.first + replacementLen
                    val newEnd = if (r.endExclusive > range.last + 1) {
                        range.first + replacementLen + (r.endExclusive - (range.last + 1))
                    } else {
                        range.first + replacementLen
                    }
                    if (newStart < newEnd) r.copy(start = newStart, endExclusive = newEnd) else null
                }
            }
        }
        return StyledText(raw = newRaw, ranges = newRanges)
    }

    companion object {
        val Empty = StyledText()
    }
}

private fun StyleRange.hasVisualStyle(): Boolean =
    bold || italic || underline || strikethrough ||
        color != null || background != null || fontSizeSp != null || link != null

private fun List<StyleRange>.mergeAdjacentStyles(): List<StyleRange> {
    if (isEmpty()) return emptyList()
    val merged = mutableListOf<StyleRange>()
    forEach { current ->
        val previous = merged.lastOrNull()
        if (previous != null &&
            previous.endExclusive == current.start &&
            previous.copy(start = current.start, endExclusive = current.endExclusive) == current
        ) {
            merged[merged.lastIndex] = previous.copy(endExclusive = current.endExclusive)
        } else {
            merged.add(current)
        }
    }
    return merged
}

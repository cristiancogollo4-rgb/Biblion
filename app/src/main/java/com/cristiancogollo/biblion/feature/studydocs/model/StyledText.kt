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

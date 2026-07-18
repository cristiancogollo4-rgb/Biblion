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

    fun withText(range: IntRange, replacement: String): StyledText {
        if (range.first < 0 || range.last > raw.length) return this
        val newRaw = raw.substring(0, range.first) + replacement + raw.substring(range.last + 1)
        val replacementLen = replacement.length
        val delta = replacementLen - (range.last - range.first + 1)
        val newRanges = ranges.mapNotNull { r ->
            when {
                r.endExclusive <= range.first -> r
                r.start >= range.last + 1 -> r.copy(start = r.start + delta, endExclusive = r.endExclusive + delta)
                r.start >= range.first && r.endExclusive <= range.last + 1 -> null
                else -> {
                    val clampedEnd = range.first + replacementLen
                    if (clampedEnd > r.start) r.copy(endExclusive = clampedEnd) else null
                }
            }
        }
        return StyledText(raw = newRaw, ranges = newRanges)
    }

    companion object {
        val Empty = StyledText()
    }
}

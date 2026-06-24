package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.Serializable

@Serializable
data class StyledText(
    val raw: String = "",
    val ranges: List<StyleRange> = emptyList(),
) {
    fun plain(): String = raw
    val length: Int get() = raw.length
    val isBlank: Boolean get() = raw.isEmpty() && ranges.isEmpty()

    fun withText(replacement: String): StyledText = StyledText(replacement, ranges)

    fun withText(range: IntRange, replacement: String): StyledText {
        if (range.first < 0 || range.last >= raw.length || range.first > range.last) return this
        val newRaw = raw.substring(0, range.first) + replacement + raw.substring(range.last + 1)
        return StyledText(newRaw, ranges)
    }

    fun withStyle(range: IntRange, patch: TextStylePatch): StyledText {
        val newRanges = ranges.toMutableList()
        newRanges.add(
            StyleRange(
                start = range.first,
                endExclusive = range.last + 1,
                bold = patch.bold ?: false,
                italic = patch.italic ?: false,
                underline = patch.underline ?: false,
                strikethrough = patch.strikethrough ?: false,
                color = patch.color,
                background = patch.background,
                fontSizeSp = patch.fontSizeSp,
                link = patch.link,
            ),
        )
        return StyledText(raw, mergeContiguous(newRanges))
    }

    private fun mergeContiguous(input: List<StyleRange>): List<StyleRange> {
        if (input.size < 2) return input
        val sorted = input.sortedBy { it.start }
        val result = mutableListOf(sorted.first())
        for (i in 1 until sorted.size) {
            val prev = result.last()
            val cur = sorted[i]
            if (cur.start == prev.endExclusive && sameStyle(prev, cur)) {
                result[result.lastIndex] = prev.copy(endExclusive = cur.endExclusive)
            } else {
                result.add(cur)
            }
        }
        return result
    }

    private fun sameStyle(a: StyleRange, b: StyleRange): Boolean =
        a.bold == b.bold && a.italic == b.italic && a.underline == b.underline &&
            a.strikethrough == b.strikethrough && a.color == b.color && a.background == b.background &&
            a.fontSizeSp == b.fontSizeSp && a.link == b.link

    fun clearStyle(range: IntRange, kind: String): StyledText = this

    companion object {
        val Empty: StyledText = StyledText()
        fun plain(text: String): StyledText = StyledText(text, emptyList())
    }
}

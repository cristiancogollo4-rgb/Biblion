package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.model.StyleRange
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

/**
 * Reconciles a plain text field edit with the persisted rich text.
 *
 * The unchanged prefix/suffix keep their original style ranges. Inserted text
 * receives the active toolbar format, matching the behavior of RichTextState.
 */
internal fun StyledText.reconcileRawText(
    nextRaw: String,
    activeFormat: ActiveFormatSnapshot,
): StyledText {
    if (raw == nextRaw) return this

    val commonPrefix = commonPrefixLength(raw, nextRaw)
    val commonSuffix = commonSuffixLength(raw, nextRaw, commonPrefix)
    val oldEndExclusive = raw.length - commonSuffix
    val newEndExclusive = nextRaw.length - commonSuffix
    val inserted = nextRaw.substring(commonPrefix, newEndExclusive)

    val prefix = sliceExclusive(0, commonPrefix)
    val replacement = StyledText(
        raw = inserted,
        ranges = activeFormat.toStyleRange(inserted.length)?.let(::listOf).orEmpty(),
    )
    val suffix = sliceExclusive(oldEndExclusive, raw.length)
    return prefix.appendStyled(replacement).appendStyled(suffix)
}

private fun StyledText.sliceExclusive(start: Int, endExclusive: Int): StyledText {
    if (start >= endExclusive) return StyledText.Empty
    return slice(start until endExclusive)
}

private fun StyledText.appendStyled(other: StyledText): StyledText {
    val offset = raw.length
    return StyledText(
        raw = raw + other.raw,
        ranges = ranges + other.ranges.map {
            it.copy(start = it.start + offset, endExclusive = it.endExclusive + offset)
        },
    )
}

private fun ActiveFormatSnapshot.toStyleRange(length: Int): StyleRange? {
    if (length <= 0) return null
    if (!bold && !italic && !underline && !strikethrough &&
        color == null && background == null && fontSize == null
    ) {
        return null
    }
    return StyleRange(
        start = 0,
        endExclusive = length,
        bold = bold,
        italic = italic,
        underline = underline,
        strikethrough = strikethrough,
        color = color,
        background = background,
        fontSizeSp = fontSize?.toFloat(),
    )
}

private fun commonPrefixLength(first: String, second: String): Int {
    val max = minOf(first.length, second.length)
    var index = 0
    while (index < max && first[index] == second[index]) index++
    return index
}

private fun commonSuffixLength(first: String, second: String, prefixLength: Int): Int {
    val max = minOf(first.length, second.length) - prefixLength
    var count = 0
    while (count < max &&
        first[first.lastIndex - count] == second[second.lastIndex - count]
    ) {
        count++
    }
    return count
}

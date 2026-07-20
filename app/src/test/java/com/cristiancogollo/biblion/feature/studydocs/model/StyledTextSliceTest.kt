package com.cristiancogollo.biblion.feature.studydocs.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StyledTextSliceTest {

    @Test
    fun slice_returns_empty_when_range_out_of_bounds() {
        val original = StyledText(raw = "abc", ranges = emptyList())
        assertEquals(StyledText.Empty, original.slice(5 until 9))
        assertEquals(StyledText.Empty, original.slice(2 until 2))
    }

    @Test
    fun slice_clamps_range_to_text_length() {
        val original = StyledText(raw = "abcdef", ranges = emptyList())
        val result = original.slice(2 until 100)
        assertEquals("cdef", result.raw)
    }

    @Test
    fun slice_keeps_fully_inside_style_unchanged_shifted() {
        val original = StyledText(
            raw = "abcdefghij",
            ranges = listOf(StyleRange(start = 3, endExclusive = 6, bold = true)),
        )
        val result = original.slice(2 until 8)
        assertEquals("cdefgh", result.raw)
        assertEquals(1, result.ranges.size)
        assertEquals(1, result.ranges[0].start)
        assertEquals(4, result.ranges[0].endExclusive)
        assertTrue(result.ranges[0].bold)
    }

    @Test
    fun slice_clamps_partially_overlapping_style() {
        val original = StyledText(
            raw = "abcdefghij",
            ranges = listOf(StyleRange(start = 2, endExclusive = 8, italic = true)),
        )
        val result = original.slice(4 until 7)
        assertEquals("efg", result.raw)
        assertEquals(1, result.ranges.size)
        assertEquals(0, result.ranges[0].start)
        assertEquals(3, result.ranges[0].endExclusive)
        assertTrue(result.ranges[0].italic)
    }

    @Test
    fun slice_drops_styles_outside_the_window() {
        val original = StyledText(
            raw = "abcdefghij",
            ranges = listOf(
                StyleRange(start = 0, endExclusive = 2, bold = true),
                StyleRange(start = 8, endExclusive = 10, italic = true),
            ),
        )
        val result = original.slice(3 until 6)
        assertEquals("def", result.raw)
        assertTrue(result.ranges.isEmpty())
    }

    @Test
    fun slice_shifts_preserves_all_style_attributes() {
        val original = StyledText(
            raw = "abcdefghij",
            ranges = listOf(
                StyleRange(
                    start = 4,
                    endExclusive = 6,
                    bold = true,
                    italic = true,
                    underline = true,
                    strikethrough = true,
                    color = 0xFF0000FF.toInt(),
                    background = 0xFFFF0000.toInt(),
                    fontSizeSp = 18f,
                    link = "https://x",
                ),
            ),
        )
        val result = original.slice(2 until 8)
        assertEquals(1, result.ranges.size)
        val r = result.ranges[0]
        assertEquals(2, r.start)
        assertEquals(4, r.endExclusive)
        assertTrue(r.bold && r.italic && r.underline && r.strikethrough)
        assertEquals(0xFF0000FF.toInt(), r.color)
        assertEquals(0xFFFF0000.toInt(), r.background)
        assertEquals(18f, r.fontSizeSp)
        assertEquals("https://x", r.link)
    }

    @Test
    fun slice_full_range_returns_equivalent_copy() {
        val original = StyledText(
            raw = "abcdef",
            ranges = listOf(StyleRange(start = 1, endExclusive = 4, bold = true)),
        )
        val result = original.slice(0 until original.raw.length)
        assertEquals(original.raw, result.raw)
        assertEquals(original.ranges, result.ranges)
    }
}

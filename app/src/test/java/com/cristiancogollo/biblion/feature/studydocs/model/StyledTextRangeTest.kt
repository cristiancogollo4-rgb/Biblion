package com.cristiancogollo.biblion.feature.studydocs.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StyledTextRangeTest {

    @Test
    fun withText_same_length_preserves_ranges() {
        val original = StyledText(
            raw = "abcdef",
            ranges = listOf(StyleRange(start = 0, endExclusive = 3, bold = true)),
        )
        val result = original.withText(0 until 3, "XYZ")
        assertEquals("XYZdef", result.raw)
        assertEquals(1, result.ranges.size)
        assertEquals(0, result.ranges[0].start)
        assertEquals(3, result.ranges[0].endExclusive)
        assertTrue(result.ranges[0].bold)
    }

    @Test
    fun withText_shorter_replacement_shifts_later_ranges() {
        val original = StyledText(
            raw = "abcdefghij",
            ranges = listOf(
                StyleRange(start = 0, endExclusive = 3, bold = true),
                StyleRange(start = 6, endExclusive = 10, italic = true),
            ),
        )
        val result = original.withText(3 until 6, "X")
        assertEquals("abcXghij", result.raw)
        assertEquals(2, result.ranges.size)
        assertEquals(0, result.ranges[0].start)
        assertEquals(3, result.ranges[0].endExclusive)
        assertTrue(result.ranges[0].bold)
        assertEquals(4, result.ranges[1].start)
        assertEquals(8, result.ranges[1].endExclusive)
        assertTrue(result.ranges[1].italic)
    }

    @Test
    fun withText_longer_replacement_shifts_later_ranges() {
        val original = StyledText(
            raw = "abcdefghij",
            ranges = listOf(
                StyleRange(start = 0, endExclusive = 3, bold = true),
                StyleRange(start = 6, endExclusive = 10, italic = true),
            ),
        )
        val result = original.withText(3 until 6, "XYZW")
        assertEquals("abcXYZWghij", result.raw)
        assertEquals(2, result.ranges.size)
        assertEquals(0, result.ranges[0].start)
        assertEquals(3, result.ranges[0].endExclusive)
        assertEquals(7, result.ranges[1].start)
        assertEquals(11, result.ranges[1].endExclusive)
    }

    @Test
    fun withText_range_inside_style_preserves_overlap() {
        val original = StyledText(
            raw = "abcdefghij",
            ranges = listOf(StyleRange(start = 2, endExclusive = 8, bold = true)),
        )
        val result = original.withText(3 until 7, "X")
        assertEquals("abcXhij", result.raw)
        assertEquals(1, result.ranges.size)
        assertEquals(2, result.ranges[0].start)
        assertEquals(5, result.ranges[0].endExclusive)
    }

    @Test
    fun withText_range_covers_entire_style_removes_it() {
        val original = StyledText(
            raw = "abcdefghij",
            ranges = listOf(StyleRange(start = 2, endExclusive = 8, bold = true)),
        )
        val result = original.withText(0 until 10, "X")
        assertEquals("X", result.raw)
        assertTrue(result.ranges.isEmpty())
    }

    @Test
    fun withText_invalid_range_returns_this() {
        val original = StyledText(
            raw = "abc",
            ranges = listOf(StyleRange(start = 0, endExclusive = 3, bold = true)),
        )
        assertEquals(original, original.withText(5 until 7, "X"))
        assertEquals(original, original.withText(-1 until 2, "X"))
        assertEquals(original, original.withText(2 until 1, "X"))
    }

    @Test
    fun withText_empty_replacement_removes_characters() {
        val original = StyledText(
            raw = "abcdefghij",
            ranges = listOf(StyleRange(start = 6, endExclusive = 10, italic = true)),
        )
        val result = original.withText(3 until 6, "")
        assertEquals("abcghij", result.raw)
        assertEquals(1, result.ranges.size)
        assertEquals(3, result.ranges[0].start)
        assertEquals(7, result.ranges[0].endExclusive)
    }
}

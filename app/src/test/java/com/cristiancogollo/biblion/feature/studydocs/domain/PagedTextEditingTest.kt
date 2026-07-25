package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.model.StyleRange
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedTextEditingTest {

    @Test
    fun `insert keeps surrounding styles and applies active format`() {
        val original = StyledText(
            raw = "hola",
            ranges = listOf(StyleRange(0, 2, italic = true)),
        )

        val result = original.reconcileRawText(
            nextRaw = "hoXXla",
            activeFormat = ActiveFormatSnapshot(bold = true),
        )

        assertEquals("hoXXla", result.raw)
        assertTrue(result.ranges.any { it.start == 0 && it.endExclusive == 2 && it.italic })
        assertTrue(result.ranges.any { it.start == 2 && it.endExclusive == 4 && it.bold })
    }

    @Test
    fun `delete joins styled prefix and suffix without losing ranges`() {
        val original = StyledText(
            raw = "abcdef",
            ranges = listOf(
                StyleRange(0, 2, bold = true),
                StyleRange(4, 6, underline = true),
            ),
        )

        val result = original.reconcileRawText(
            nextRaw = "abef",
            activeFormat = ActiveFormatSnapshot(),
        )

        assertEquals("abef", result.raw)
        assertTrue(result.ranges.any { it.start == 0 && it.endExclusive == 2 && it.bold })
        assertTrue(result.ranges.any { it.start == 2 && it.endExclusive == 4 && it.underline })
    }
}

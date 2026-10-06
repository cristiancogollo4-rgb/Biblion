package com.cristiancogollo.biblion.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPreferencesTest {

    @Test
    fun defaultsFavorComfortableAccessibleReading() {
        val preferences = ReaderPreferences()

        assertEquals(ReaderFontFamily.SERIF, preferences.fontFamily)
        assertEquals(READER_DEFAULT_LINE_SPACING, preferences.lineSpacingMultiplier, 0f)
        assertEquals(READER_DEFAULT_TEXT_WIDTH_DP, preferences.textWidthDp, 0f)
        assertEquals(ReaderTextLayout.VERSE_BLOCKS, preferences.textLayout)
        assertTrue(preferences.showSectionHeadings)
        assertTrue(preferences.showVerseNumbers)
        assertFalse(preferences.continuousScrolling)
        assertEquals("Salmos", preferences.secondaryBookName)
    }

    @Test
    fun invalidStoredValuesFallBackToSafeDefaults() {
        assertEquals(ReaderFontFamily.SERIF, ReaderFontFamily.fromStorage("unknown"))
        assertEquals(ReaderTextLayout.VERSE_BLOCKS, ReaderTextLayout.fromStorage("unknown"))
        assertEquals(ReaderTextLayout.FLOWING, ReaderTextLayout.fromStorage("flowing"))
        assertEquals(
            ReaderTextLayout.VERSE_BLOCKS,
            ReaderPreferences(textLayout = ReaderTextLayout.FLOWING).normalized().textLayout,
        )
    }

    @Test
    fun continuousValuesAreClampedToReadableRanges() {
        val preferences = ReaderPreferences(
            lineSpacingMultiplier = 8f,
            textWidthDp = 40f,
            secondaryChapter = 0,
        ).normalized()

        assertEquals(READER_MAX_LINE_SPACING, preferences.lineSpacingMultiplier, 0f)
        assertEquals(READER_MIN_TEXT_WIDTH_DP, preferences.textWidthDp, 0f)
        assertEquals(1, preferences.secondaryChapter)
    }

    @Test
    fun fontChoicesUseThreeDifferentFamilies() {
        val families = ReaderFontFamily.entries
            .map { it.asComposeFontFamily() }
            .toSet()

        assertEquals(3, families.size)
    }
}

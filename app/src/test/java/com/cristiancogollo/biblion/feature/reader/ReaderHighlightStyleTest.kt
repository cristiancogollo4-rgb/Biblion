package com.cristiancogollo.biblion.feature.reader

import androidx.compose.ui.graphics.Color
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderHighlightStyleTest {

    @Test
    fun highlightedVerseUsesDarkForegroundEvenInDarkTheme() {
        val darkThemeForeground = Color(0xFFF5E8C7)
        val highlightColors = listOf(
            Color(0xFFFFF2A8),
            Color(0xFFC8F7C5),
            Color(0xFFFFD0D0),
            Color(0xFFD8E8FF),
        )

        highlightColors.forEach { highlight ->
            assertEquals(
                BiblionBluePrimary,
                readerVerseForeground(highlight, darkThemeForeground),
            )
        }
    }

    @Test
    fun unhighlightedAndRangeSelectedVersesKeepTheirThemeForeground() {
        val normalForeground = Color(0xFFF5E8C7)
        assertEquals(
            normalForeground,
            readerVerseForeground(Color.Transparent, normalForeground),
        )
        assertEquals(
            normalForeground,
            readerVerseForeground(
                highlightColor = Color(0xFFFFF2A8),
                normalColor = normalForeground,
                isRangeSelected = true,
            ),
        )
    }
}

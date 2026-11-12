package com.cristiancogollo.biblion.feature.reader

import androidx.compose.ui.graphics.Color
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderHighlightStyleTest {

    @Test
    fun highlightedVerseUsesNavyForegroundInLightTheme() {
        val lightForeground = BiblionBluePrimary.copy(alpha = 0.9f)
        readerHighlightPaletteFor(BiblionThemeMode.LIGHT).drop(1).forEach { highlight ->
            assertEquals(
                BiblionBluePrimary,
                readerVerseForeground(highlight, lightForeground, BiblionThemeMode.LIGHT),
            )
        }
    }

    @Test
    fun highlightedVerseKeepsCreamForegroundInBlueAndDarkThemes() {
        val cream = Color(0xFFF5E8C7)
        listOf(BiblionThemeMode.BLUE, BiblionThemeMode.DARK).forEach { mode ->
            readerHighlightPaletteFor(mode).drop(1).forEach { highlight ->
                assertEquals(cream, readerVerseForeground(highlight, cream, mode))
            }
        }
    }

    @Test
    fun unhighlightedAndRangeSelectedVersesKeepTheirThemeForeground() {
        val normalForeground = Color(0xFF103250)
        assertEquals(
            normalForeground,
            readerVerseForeground(Color.Transparent, normalForeground, BiblionThemeMode.LIGHT),
        )
        assertEquals(
            normalForeground,
            readerVerseForeground(
                highlightColor = readerHighlightPaletteFor(BiblionThemeMode.LIGHT)[1],
                normalColor = normalForeground,
                mode = BiblionThemeMode.LIGHT,
                isRangeSelected = true,
            ),
        )
    }

    @Test
    fun everyThemeKeepsTheSamePersistedHighlightIndices() {
        BiblionThemeMode.entries.forEach { mode ->
            val palette = readerHighlightPaletteFor(mode)
            assertEquals(ReaderHighlightColorIndices.toList(), palette.indices.toList())
            assertEquals(0f, palette[0].alpha)
        }
    }
}

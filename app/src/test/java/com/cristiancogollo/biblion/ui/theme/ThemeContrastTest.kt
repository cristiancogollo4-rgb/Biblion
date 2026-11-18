package com.cristiancogollo.biblion.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.cristiancogollo.biblion.feature.books.BookCategory
import com.cristiancogollo.biblion.feature.books.BookCategoryColors
import com.cristiancogollo.biblion.feature.reader.readerHighlightPaletteFor
import com.cristiancogollo.biblion.feature.reader.readerVerseForeground
import com.cristiancogollo.biblion.feature.search.components.CategoryLabels
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Protege el equilibrio de color de Biblion: todo texto con >= 4.5:1 y ningun color de
 * resaltado o tarjeta que salte de forma chillona respecto al fondo del tema.
 */
class ThemeContrastTest {

    private val schemes: Map<BiblionThemeMode, ColorScheme> = mapOf(
        BiblionThemeMode.LIGHT to LightColorScheme,
        BiblionThemeMode.BLUE to BlueColorScheme,
        BiblionThemeMode.DARK to DarkColorScheme,
    )

    private fun channel(value: Float): Double =
        if (value <= 0.03928f) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    /** Compone un color translucido sobre un fondo opaco. */
    private fun over(color: Color, background: Color): Color =
        mixSrgb(color.copy(alpha = 1f), background, color.alpha)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertReadable(label: String, text: Color, background: Color, minimum: Double = 4.5) {
        val ratio = contrast(text, background)
        assertTrue("$label: contraste ${"%.2f".format(ratio)} < $minimum", ratio >= minimum)
    }

    private fun assertCalm(label: String, fill: Color, background: Color, maximum: Double = 2.1) {
        val ratio = contrast(fill, background)
        assertTrue("$label: salto ${"%.2f".format(ratio)} > $maximum (chillon)", ratio <= maximum)
    }

    @Test
    fun themeTextRolesAreReadable() {
        schemes.forEach { (mode, scheme) ->
            assertReadable("$mode onBackground", scheme.onBackground, scheme.background)
            assertReadable("$mode onSurface", scheme.onSurface, scheme.surface)
            assertReadable("$mode onSurfaceVariant/surface", scheme.onSurfaceVariant, scheme.surface)
            assertReadable("$mode onSurfaceVariant/variant", scheme.onSurfaceVariant, scheme.surfaceVariant)
            assertReadable("$mode onPrimary", scheme.onPrimary, scheme.primary)
            assertReadable("$mode onPrimaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer)
            assertReadable("$mode onSecondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer)
            assertReadable("$mode onSurface/containerHighest", scheme.onSurface, scheme.surfaceContainerHighest)
        }
    }

    @Test
    fun editorialGoldLabelsAreReadable() {
        // Etiquetas en versalitas, numeros de versiculo y enlaces "Ir": texto pequeno en dorado.
        schemes.forEach { (mode, scheme) ->
            val label = biblionLabelColor(mode)
            assertReadable("$mode etiqueta dorada/background", label, scheme.background)
            assertReadable("$mode etiqueta dorada/surface", label, scheme.surface)
        }
    }

    @Test
    fun lightThemeKeepsWarmPaperBehindWhiteCards() {
        val scheme = LightColorScheme
        assertTrue("las tarjetas deben ser mas claras que el papel", luminance(scheme.surface) > luminance(scheme.background))
        assertCalm("papel frente a blanco", scheme.background, scheme.surface, maximum = 1.1)
        assertTrue(
            "el filete de las tarjetas debe verse sobre blanco",
            contrast(scheme.outlineVariant, scheme.surface) >= 1.2,
        )
    }

    @Test
    fun highlightedVersesAreReadableAndCalm() {
        schemes.forEach { (mode, scheme) ->
            readerHighlightPaletteFor(mode).drop(1).forEachIndexed { index, highlight ->
                val text = readerVerseForeground(highlight, scheme.onSurface, mode)
                val number = readerVerseForeground(highlight, biblionLabelColor(mode), mode)
                assertReadable("$mode resaltado ${index + 1} texto", text, highlight, minimum = 7.0)
                assertReadable("$mode resaltado ${index + 1} numero", number, highlight, minimum = 3.0)
                assertCalm("$mode resaltado ${index + 1}", highlight, scheme.background)
            }
            val selection = over(scheme.primary.copy(alpha = 0.18f), scheme.background)
            assertReadable("$mode seleccion", scheme.onSurface, selection)
            assertTrue("$mode seleccion visible", contrast(selection, scheme.background) >= 1.1)
        }
    }

    @Test
    fun bookCategoriesAreReadableAndCalm() {
        schemes.forEach { (mode, scheme) ->
            BookCategory.entries.forEach { category ->
                val colors = BookCategoryColors.getColors(category, mode)
                assertReadable("$mode libro $category", colors.text, colors.bg)
                assertCalm("$mode libro $category", colors.bg, scheme.background)
                assertTrue(
                    "$mode libro $category debe separarse del fondo",
                    contrast(colors.bg, scheme.background) >= 1.15,
                )
            }
        }
    }

    @Test
    fun topicCategoryAccentsAreReadableOnSurfaces() {
        val categories = listOf(
            "BOOK", "PERSON", "PLACE", "EVENT", "ATTRIBUTE_OF_GOD", "DOCTRINE",
            "CHRISTIAN_LIFE", "CHURCH", "PROPHECY", "SIN", "COMPARATIVE_RELIGION", "OTHER",
        )
        schemes.forEach { (mode, scheme) ->
            categories.forEach { category ->
                val accent = CategoryLabels.color(category, mode)
                assertReadable("$mode tema $category/surface", accent, scheme.surface)
                assertReadable("$mode tema $category/background", accent, scheme.background)
                assertReadable(
                    "$mode tema $category sobre tarjeta tintada",
                    accent,
                    over(accent.copy(alpha = 0.20f), scheme.surface),
                )
                assertReadable("$mode inicial sobre $category", BiblionPalette.onAccent(mode), accent)
            }
        }
    }
}

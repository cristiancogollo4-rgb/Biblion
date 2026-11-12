package com.cristiancogollo.biblion.feature.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.cristiancogollo.biblion.ui.theme.BiblionBlueBackground
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import com.cristiancogollo.biblion.ui.theme.LocalBiblionThemeMode
import com.cristiancogollo.biblion.ui.theme.mixSrgb

/**
 * Indices persistidos de resaltado (Room/Firestore): 0 sin color, 1 oro, 2 salvia,
 * 3 rosa, 4 azul. El color visible depende del tema, el indice no.
 */
internal val ReaderHighlightColorIndices: IntRange = 0..4

private val LightHighlights = listOf(
    Color.Transparent,
    Color(0xFFF6E9BE),
    Color(0xFFDDEBD5),
    Color(0xFFF4DCD6),
    Color(0xFFDCE7F3),
)

// Tintes oscuros calidos: se distinguen del negro sin convertirse en bloques claros.
private val DarkHighlights = listOf(
    Color.Transparent,
    Color(0xFF352F24),
    Color(0xFF2C3129),
    Color(0xFF352A2C),
    Color(0xFF293136),
)

private val BlueHighlightInks = listOf(
    Color(0xFFF0CC70),
    Color(0xFFA8D49A),
    Color(0xFFF0A0B0),
    Color(0xFF8EC4F0),
)

private val BlueHighlights = listOf(Color.Transparent) +
    BlueHighlightInks.map { mixSrgb(it, BiblionBlueBackground, 0.22f) }

fun readerHighlightPaletteFor(mode: BiblionThemeMode): List<Color> = when (mode) {
    BiblionThemeMode.LIGHT -> LightHighlights
    BiblionThemeMode.BLUE -> BlueHighlights
    BiblionThemeMode.DARK -> DarkHighlights
}

/** Paleta de resaltado del tema actual. */
val readerHighlightPalette: List<Color>
    @Composable
    @ReadOnlyComposable
    get() = readerHighlightPaletteFor(LocalBiblionThemeMode.current)

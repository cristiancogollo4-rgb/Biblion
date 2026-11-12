package com.cristiancogollo.biblion.feature.reader

import androidx.compose.ui.graphics.Color
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode

/**
 * Color del texto de un versiculo. En Claro el resaltado es un pastel y el texto pasa a
 * azul marino; en Azul y Oscuro el resaltado es un tinte oscuro y se conserva el crema
 * del tema.
 */
internal fun readerVerseForeground(
    highlightColor: Color,
    normalColor: Color,
    mode: BiblionThemeMode,
    isRangeSelected: Boolean = false,
): Color = if (
    mode == BiblionThemeMode.LIGHT &&
    highlightColor.alpha > 0f &&
    !isRangeSelected
) {
    BiblionBluePrimary
} else {
    normalColor
}

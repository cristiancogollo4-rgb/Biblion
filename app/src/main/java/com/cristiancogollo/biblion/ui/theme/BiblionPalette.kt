package com.cristiancogollo.biblion.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Familia de matices sobrios compartida por libros, temas, diccionario y resaltados.
 *
 * Cada matiz define una tinta oscura (texto sobre blanco, >= 4.5:1), una tinta clara
 * (texto sobre negro, >= 4.5:1) y una tinta viva para las tarjetas del tema Azul: con el
 * doble de croma que la clara, porque los pasteles se confunden entre si sobre azul
 * (sobre todo los matices azulados). Los fondos tintados se derivan de la tinta clara
 * mezclandola con el fondo de cada tema, para que el color se integre sin verse chillon.
 */
enum class BiblionHue(val lightInk: Color, val darkInk: Color, val blueInk: Color) {
    SAGE(Color(0xFF4F6B42), Color(0xFFB5CCA6), Color(0xFF9ECE82)),
    SLATE(Color(0xFF3A5A7A), Color(0xFFA9C2DC), Color(0xFF8CC7FB)),
    SAND(Color(0xFF7A5C24), Color(0xFFE0C48E), Color(0xFFE8BB49)),
    WINE(Color(0xFF8A3A4A), Color(0xFFE2A9B3), Color(0xFFFF9FB7)),
    TEAL(Color(0xFF2A6658), Color(0xFF9FD3C4), Color(0xFF5FD4B8)),
    SKY(Color(0xFF2A6385), Color(0xFFA6CDE6), Color(0xFF6BCCFE)),
    LAVENDER(Color(0xFF5E4F80), Color(0xFFC6B9E6), Color(0xFFCAB3FF)),
    INDIGO(Color(0xFF434C8A), Color(0xFFB3BAEA), Color(0xFFA5BCFF)),
    TERRACOTTA(Color(0xFF954A30), Color(0xFFEDB29C), Color(0xFFFFA47A)),
    BROWN(Color(0xFF6B5242), Color(0xFFD4BCA8), Color(0xFFE5B890)),
    STEEL(Color(0xFF4A5E70), Color(0xFFB4C3D0), Color(0xFFA5C5DF)),
    CRIMSON(Color(0xFF9A2F2F), Color(0xFFF0AFA8), Color(0xFFFF9D93)),
    GOLD(Color(0xFF765B2C), Color(0xFFD9C08F), Color(0xFFE5BC56)),
}

/** Mezcla lineal en sRGB: `amount` = 1 devuelve [foreground], 0 devuelve [background]. */
internal fun mixSrgb(foreground: Color, background: Color, amount: Float): Color = Color(
    red = background.red + (foreground.red - background.red) * amount,
    green = background.green + (foreground.green - background.green) * amount,
    blue = background.blue + (foreground.blue - background.blue) * amount,
    alpha = 1f,
)

object BiblionPalette {
    private val InkOnLightAccent = Color.White
    private val InkOnDarkAccent = Color(0xFF102030)

    /** Color de acento del matiz para texto, iconos y formas sobre `surface`. */
    fun accent(hue: BiblionHue, mode: BiblionThemeMode): Color = when (mode) {
        BiblionThemeMode.LIGHT -> hue.lightInk
        BiblionThemeMode.DARK -> hue.darkInk
        // El azul Biblion es mas claro que el negro: la tinta se aclara un poco mas.
        BiblionThemeMode.BLUE -> mixSrgb(Color.White, hue.darkInk, 0.40f)
    }

    /** Texto legible sobre un relleno solido de [accent]. */
    fun onAccent(mode: BiblionThemeMode): Color =
        if (mode == BiblionThemeMode.LIGHT) InkOnLightAccent else InkOnDarkAccent

    /**
     * Base de las tarjetas tintadas del tema Azul: un azul marino mas profundo que el fondo,
     * para que el color de la categoria mande sobre el azul y no se agrise.
     */
    private val BlueCardBase = Color(0xFF071D33)

    /** Fondo de tarjeta del matiz: el mismo relleno suave en los tres temas. */
    fun tint(hue: BiblionHue, mode: BiblionThemeMode): Color = when (mode) {
        BiblionThemeMode.LIGHT -> mixSrgb(hue.darkInk, Color.White, 0.40f)
        BiblionThemeMode.DARK -> mixSrgb(hue.darkInk, Color.Black, 0.20f)
        BiblionThemeMode.BLUE -> mixSrgb(hue.blueInk, BlueCardBase, 0.34f)
    }

    /** Texto sobre [tint]. */
    fun onTint(hue: BiblionHue, mode: BiblionThemeMode): Color = when (mode) {
        BiblionThemeMode.LIGHT -> hue.lightInk
        BiblionThemeMode.DARK -> hue.darkInk
        BiblionThemeMode.BLUE -> mixSrgb(Color.White, hue.blueInk, 0.30f)
    }

    /** Borde discreto para [tint]. */
    fun border(hue: BiblionHue, mode: BiblionThemeMode): Color = when (mode) {
        BiblionThemeMode.LIGHT -> hue.lightInk.copy(alpha = 0.22f)
        BiblionThemeMode.DARK -> hue.darkInk.copy(alpha = 0.25f)
        BiblionThemeMode.BLUE -> hue.blueInk.copy(alpha = 0.35f)
    }
}

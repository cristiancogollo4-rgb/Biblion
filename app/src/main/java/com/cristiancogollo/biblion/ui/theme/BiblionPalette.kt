package com.cristiancogollo.biblion.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Familia de matices sobrios compartida por libros, temas, diccionario y resaltados.
 *
 * Cada matiz define una tinta oscura (texto sobre blanco, >= 4.5:1) y una tinta clara
 * (texto sobre negro o azul Biblion, >= 4.5:1). Los fondos tintados se derivan de la
 * tinta clara mezclandola con el fondo de cada tema, para que el color se integre en
 * Claro, Azul y Oscuro sin verse chillon.
 */
enum class BiblionHue(val lightInk: Color, val darkInk: Color) {
    SAGE(Color(0xFF4F6B42), Color(0xFFB5CCA6)),
    SLATE(Color(0xFF3A5A7A), Color(0xFFA9C2DC)),
    SAND(Color(0xFF7A5C24), Color(0xFFE0C48E)),
    WINE(Color(0xFF8A3A4A), Color(0xFFE2A9B3)),
    TEAL(Color(0xFF2A6658), Color(0xFF9FD3C4)),
    SKY(Color(0xFF2A6385), Color(0xFFA6CDE6)),
    LAVENDER(Color(0xFF5E4F80), Color(0xFFC6B9E6)),
    INDIGO(Color(0xFF434C8A), Color(0xFFB3BAEA)),
    TERRACOTTA(Color(0xFF954A30), Color(0xFFEDB29C)),
    BROWN(Color(0xFF6B5242), Color(0xFFD4BCA8)),
    STEEL(Color(0xFF4A5E70), Color(0xFFB4C3D0)),
    CRIMSON(Color(0xFF9A2F2F), Color(0xFFF0AFA8)),
    GOLD(Color(0xFF765B2C), Color(0xFFD9C08F)),
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

    /** Tarjeta azul neutra del tema Azul; el matiz va en el texto y el borde. */
    private val BlueCardSurface = Color(0xFF164672)

    /**
     * Fondo de tarjeta del matiz. En Azul no se tinta: mezclar color con azul da tonos
     * grisaceos, asi que la tarjeta es azul y la categoria se lee en texto y borde.
     */
    fun tint(hue: BiblionHue, mode: BiblionThemeMode): Color = when (mode) {
        BiblionThemeMode.LIGHT -> mixSrgb(hue.darkInk, Color.White, 0.40f)
        BiblionThemeMode.DARK -> mixSrgb(hue.darkInk, Color.Black, 0.20f)
        BiblionThemeMode.BLUE -> BlueCardSurface
    }

    /** Texto sobre [tint]. */
    fun onTint(hue: BiblionHue, mode: BiblionThemeMode): Color = when (mode) {
        BiblionThemeMode.LIGHT -> hue.lightInk
        BiblionThemeMode.DARK -> hue.darkInk
        BiblionThemeMode.BLUE -> accent(hue, mode)
    }

    /** Borde para [tint]: discreto en Claro y Oscuro, portador del color en Azul. */
    fun border(hue: BiblionHue, mode: BiblionThemeMode): Color = when (mode) {
        BiblionThemeMode.LIGHT -> hue.lightInk.copy(alpha = 0.22f)
        BiblionThemeMode.DARK -> hue.darkInk.copy(alpha = 0.25f)
        BiblionThemeMode.BLUE -> hue.darkInk.copy(alpha = 0.60f)
    }
}

package com.cristiancogollo.biblion.feature.search.components

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.cristiancogollo.biblion.ui.theme.BiblionHue
import com.cristiancogollo.biblion.ui.theme.BiblionPalette
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import com.cristiancogollo.biblion.ui.theme.LocalBiblionThemeMode

/**
 * Mapeo de categorias canonicas de la taxonomia Biblion a etiquetas
 * legibles en espanol para la UI.
 *
 * Las categorias estan definidas en tools/canonical_topics.json y replicadas
 * en la columna `category` de la tabla `topics` (schema v3).
 */
object CategoryLabels {

    fun label(category: String): String = when (category) {
        "BOOK" -> "Libro"
        "PERSON" -> "Persona"
        "PLACE" -> "Lugar"
        "EVENT" -> "Evento"
        "ATTRIBUTE_OF_GOD" -> "Atributo de Dios"
        "PROPHECY" -> "Profecia"
        "COMPARATIVE_RELIGION" -> "Religion comparada"
        "SIN" -> "Pecado"
        "DOCTRINE" -> "Doctrina"
        "CHURCH" -> "Iglesia"
        "CHRISTIAN_LIFE" -> "Vida cristiana"
        else -> category
    }

    private val sortOrder = listOf(
        "BOOK",
        "PERSON",
        "PLACE",
        "EVENT",
        "ATTRIBUTE_OF_GOD",
        "DOCTRINE",
        "CHRISTIAN_LIFE",
        "CHURCH",
        "PROPHECY",
        "SIN",
        "COMPARATIVE_RELIGION"
    )

    fun sortKey(category: String): Int =
        sortOrder.indexOf(category).let { if (it < 0) Int.MAX_VALUE else it }

    /** Matiz identitario de cada categoria, compartido con Libros y Diccionario. */
    fun hue(category: String): BiblionHue = when (category) {
        "PERSON" -> BiblionHue.BROWN
        "PLACE" -> BiblionHue.SAGE
        "EVENT" -> BiblionHue.TERRACOTTA
        "ATTRIBUTE_OF_GOD" -> BiblionHue.GOLD
        "DOCTRINE" -> BiblionHue.SLATE
        "CHRISTIAN_LIFE" -> BiblionHue.TEAL
        "CHURCH" -> BiblionHue.LAVENDER
        "PROPHECY" -> BiblionHue.INDIGO
        "SIN" -> BiblionHue.CRIMSON
        "COMPARATIVE_RELIGION" -> BiblionHue.STEEL
        "BOOK" -> BiblionHue.SAND
        else -> BiblionHue.GOLD
    }

    /** Color de acento legible (>= 4.5:1 sobre `surface`) para el tema indicado. */
    fun color(category: String, mode: BiblionThemeMode): Color =
        BiblionPalette.accent(hue(category), mode)

    /** Color de acento para el tema actual. */
    @Composable
    @ReadOnlyComposable
    fun color(category: String): Color = color(category, LocalBiblionThemeMode.current)
}

package com.cristiancogollo.biblion.feature.search.components

import androidx.compose.ui.graphics.Color
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

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

    /**
     * Color identitario de cada categoria, usado por el carrusel de
     * temas populares y las pantallas de exploracion.
     */
    fun color(category: String): Color = when (category) {
        "PERSON" -> Color(0xFF8D6E63)
        "PLACE" -> Color(0xFF4CAF50)
        "EVENT" -> Color(0xFFFF9800)
        "ATTRIBUTE_OF_GOD" -> Color(0xFFE57373)
        "DOCTRINE" -> Color(0xFF1976D2)
        "CHRISTIAN_LIFE" -> Color(0xFF66BB6A)
        "CHURCH" -> Color(0xFF7B1FA2)
        "PROPHECY" -> Color(0xFFD32F2F)
        "SIN" -> Color(0xFFC62828)
        "COMPARATIVE_RELIGION" -> Color(0xFF607D8B)
        "BOOK" -> Color(0xFF5D4037)
        else -> BiblionGoldPrimary
    }
}

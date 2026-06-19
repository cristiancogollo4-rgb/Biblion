package com.cristiancogollo.biblion.feature.search

/**
 * Versículos populares curados para mostrar en la pantalla de búsqueda.
 * Cada entrada tiene una temática, ícono y referencia bíblica.
 *
 * El text se carga dinámicamente desde la base de datos cuando el usuario
 * toca una tarjeta, usando la referencia.
 */
data class PopularVerse(
    val topic: String,
    val reference: String,
    val bookName: String,
    val chapter: Int,
    val verse: String,
    val topicColor: Long
)

object PopularVersesData {

    val popular: List<PopularVerse> = listOf(
        PopularVerse(
            topic = "Amor",
            reference = "1 Corintios 13:4-7",
            bookName = "1 Corintios",
            chapter = 13,
            verse = "4-7",
            topicColor = 0xFFE57373
        ),
        PopularVerse(
            topic = "Fe",
            reference = "Hebreos 11:1",
            bookName = "Hebreos",
            chapter = 11,
            verse = "1",
            topicColor = 0xFF1976D2
        ),
        PopularVerse(
            topic = "Gracia",
            reference = "Efesios 2:8-9",
            bookName = "Efesios",
            chapter = 2,
            verse = "8-9",
            topicColor = 0xFF66BB6A
        ),
        PopularVerse(
            topic = "Salvación",
            reference = "Romanos 10:9",
            bookName = "Romanos",
            chapter = 10,
            verse = "9",
            topicColor = 0xFF7B1FA2
        ),
        PopularVerse(
            topic = "Esperanza",
            reference = "Romanos 15:13",
            bookName = "Romanos",
            chapter = 15,
            verse = "13",
            topicColor = 0xFFFFB74D
        ),
        PopularVerse(
            topic = "Paz",
            reference = "Juan 14:27",
            bookName = "Juan",
            chapter = 14,
            verse = "27",
            topicColor = 0xFF26A69A
        )
    )
}

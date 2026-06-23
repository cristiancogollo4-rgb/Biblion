package com.cristiancogollo.biblion.feature.search

/**
 * Temas canonicos curados para el carrusel automatico de la pantalla
 * de busqueda. Reemplaza al antiguo `PopularVersesData`.
 *
 * Cada entrada corresponde a un canonico de la taxonomia v3
 * (tools/canonical_topics.json) con descripciones de la DB
 * (generadas por Bibi Worker cuando estaban disponibles).
 *
 * El `topicColor` es la identidad visual del tema en el carrusel.
 * El `verseCount` se muestra en la UI como "N versiculos" para
 * indicar cuantos versiculos vera el usuario al expandir la card.
 */
data class PopularTopic(
    val slug: String,
    val nameEs: String,
    val nameEn: String,
    val description: String,
    val category: String,
    val topicColor: Int,
    val verseCount: Int = 0
)

object PopularTopicsData {

    val popular: List<PopularTopic> = listOf(
        PopularTopic(
            slug = "amor-de-dios",
            nameEs = "Amor de Dios",
            nameEn = "Love of God",
            description = "El amor de Dios se expresa mediante entrega, obediencia, servicio y verdad.",
            category = "ATTRIBUTE_OF_GOD",
            topicColor = 0xFFE57373.toInt(),
            verseCount = 34
        ),
        PopularTopic(
            slug = "fe",
            nameEs = "Fe",
            nameEn = "Faith",
            description = "Confianza personal en Dios y su palabra; medio por el cual el pecador recibe la salvacion.",
            category = "DOCTRINE",
            topicColor = 0xFF1976D2.toInt(),
            verseCount = 25
        ),
        PopularTopic(
            slug = "gracia",
            nameEs = "Gracia",
            nameEn = "Grace",
            description = "Favor inmerecido de Dios al pecador; fundamento de la salvacion y la vida cristiana.",
            category = "DOCTRINE",
            topicColor = 0xFF66BB6A.toInt(),
            verseCount = 2
        ),
        PopularTopic(
            slug = "salvacion",
            nameEs = "Salvacion",
            nameEn = "Salvation",
            description = "Liberacion del pecado y sus consecuencias por la obra de Cristo; obra completa del Dios trino.",
            category = "DOCTRINE",
            topicColor = 0xFF7B1FA2.toInt(),
            verseCount = 20
        ),
        PopularTopic(
            slug = "esperanza",
            nameEs = "Esperanza",
            nameEn = "Hope",
            description = "Confianza firme en las promesas de Dios; ancla del alma en medio del sufrimiento.",
            category = "CHRISTIAN_LIFE",
            topicColor = 0xFFFFB74D.toInt(),
            verseCount = 4
        ),
        PopularTopic(
            slug = "paz",
            nameEs = "Paz",
            nameEn = "Peace",
            description = "Tranquilidad del corazon fundada en la justicia de Cristo y la confianza en Dios.",
            category = "CHRISTIAN_LIFE",
            topicColor = 0xFF26A69A.toInt(),
            verseCount = 15
        )
    )
}

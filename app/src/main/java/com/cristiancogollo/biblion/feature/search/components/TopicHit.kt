package com.cristiancogollo.biblion.feature.search.components

/**
 * Representa un tema canonico encontrado durante la busqueda.
 *
 * Es un DTO ligero (no la entity Room completa) que la UI consume para
 * mostrar la seccion "Temas relacionados" sobre los resultados biblicos.
 *
 * @param slug identificador unico del canonico en la taxonomia (e.g. "amor-de-dios")
 * @param nameEs nombre en espanol (curado)
 * @param nameEn nombre original en ingles
 * @param description descripcion de 1-2 oraciones generada por Bibi Worker
 * @param category categoria Biblion: ATTRIBUTE_OF_GOD, DOCTRINE, SIN, etc.
 * @param verseCount cantidad de versiculos disponibles para este tema.
 *                     Se muestra en la UI como "N versiculos" para indicar
 *                     cuantos versiculos vera el usuario al expandir la card.
 */
data class TopicHit(
    val slug: String,
    val nameEs: String,
    val nameEn: String,
    val description: String,
    val category: String,
    val verseCount: Int = 0
)

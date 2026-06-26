package com.cristiancogollo.biblion.feature.bibi.model

/**
 * DTO publico para versiculos relacionados devueltos por los engines.
 *
 * IMPORTANTE: Este DTO NO incluye el campo votes / quality_score.
 * Esos datos son INTERNOS y nunca deben exponerse al usuario en la UI
 * de Bibi. El usuario debe ver versiculos relevantes, no numeros.
 */
data class RelatedVerse(
    val reference: String,      // "Genesis 1:1" o "Juan 1:1-3"
    val book: String,           // "Genesis"
    val chapter: Int,           // 1
    val verseStart: Int,        // 1
    val verseEnd: Int,          // 1 (o 3 si es rango)
    val text: String            // "En el principio creo Dios..."
)

/**
 * DTO publico para temas.
 * El qualityScore es interno - el caller decide si mostrarlo o no.
 */
data class TopicInfo(
    val key: String,            // "10_commandments"
    val displayEs: String,      // "10 mandamientos" (curado o por defecto EN)
    val displayEn: String,      // "10 commandments"
    val qualityScore: Int       // INTERNO - filtrar antes de mostrar
) {
    /**
     * Etiqueta para mostrar al usuario. Siempre devuelve displayEs,
     * que puede ser el original en ingles si no hay traduccion.
     */
    val displayLabel: String get() = displayEs
}

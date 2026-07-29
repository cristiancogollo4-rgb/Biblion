package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.Serializable

@Serializable
data class DocMetadata(
    val tags: List<String> = emptyList(),
    val notebook: String? = null,
    val authorUid: String? = null,
    val globalVersion: Int = 0,
    val provenance: StudyProvenance? = null,
) {
    companion object {
        val Empty: DocMetadata = DocMetadata()
    }
}

/**
 * Immutable origin information carried by local copies of shared teachings.
 * Publication ids are intentionally opaque and optional for legacy documents.
 */
@Serializable
data class StudyProvenance(
    val rootPublicationId: String? = null,
    val rootRevisionId: String? = null,
    val parentPublicationId: String? = null,
    val parentRevisionId: String? = null,
    val importedAt: Long? = null,
    val sourceFormat: String? = null,
)

object DocTagGroups {
    const val PURPOSE = "purpose"
    const val AUDIENCE = "audience"
    const val TOPIC = "topic"
    const val STATE = "state"

    fun groupFor(tag: String): String = when (tag) {
        in PURPOSE_TAGS -> PURPOSE
        in AUDIENCE_TAGS -> AUDIENCE
        in TOPIC_TAGS -> TOPIC
        in STATE_TAGS -> STATE
        else -> ""
    }

    val PURPOSE_TAGS = listOf(
        "predicacion", "devocional", "estudio-biblico", "clase",
        "discipulado", "formacion",
    )
    val AUDIENCE_TAGS = listOf(
        "jovenes", "iglesia", "lideres", "universitarios", "familias",
        "simpatizantes", "ninos", "mujeres", "hombres", "ancianos",
        "grupos-especiales", "pastores",
    )
    val TOPIC_TAGS = listOf(
        "identidad", "fe", "gracia", "proposito", "oracion", "evangelismo",
        "servicio", "esperanza", "doctrina", "amor", "misiones", "adoracion",
    )
    val STATE_TAGS = listOf("borrador", "en-preparacion", "finalizado")
}

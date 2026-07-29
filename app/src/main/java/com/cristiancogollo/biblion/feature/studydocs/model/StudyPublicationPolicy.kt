package com.cristiancogollo.biblion.feature.studydocs.model

import com.cristiancogollo.biblion.BiblionUserProfile

enum class PublicationEligibility {
    ELIGIBLE,
    AUTHENTICATION_REQUIRED,
    PROFILE_INCOMPLETE,
    PUBLISHER_NOT_APPROVED,
    TITLE_REQUIRED,
    CONTENT_REQUIRED,
    TAGS_REQUIRED,
    FINAL_STATE_REQUIRED,
}

object StudyPublicationPolicy {
    fun evaluate(document: StudyDoc, profile: BiblionUserProfile?): PublicationEligibility {
        if (profile == null || profile.uid.isBlank()) return PublicationEligibility.AUTHENTICATION_REQUIRED
        if (!profile.isComplete) return PublicationEligibility.PROFILE_INCOMPLETE
        if (profile.estadoPublicador.uppercase() != "APROBADO") {
            return PublicationEligibility.PUBLISHER_NOT_APPROVED
        }
        if (profile.rol.uppercase() !in setOf("PUBLICADOR", "MODERADOR", "ADMIN")) {
            return PublicationEligibility.PUBLISHER_NOT_APPROVED
        }
        if (!document.hasPersistableTitle()) return PublicationEligibility.TITLE_REQUIRED
        if (document.blocks.none { it.plainText().isNotBlank() }) return PublicationEligibility.CONTENT_REQUIRED
        if (document.metadata.tags.isEmpty()) return PublicationEligibility.TAGS_REQUIRED
        if ("finalizado" !in document.metadata.tags) return PublicationEligibility.FINAL_STATE_REQUIRED
        return PublicationEligibility.ELIGIBLE
    }
}

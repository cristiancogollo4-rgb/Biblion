package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.BiblionUserProfile
import com.cristiancogollo.biblion.awaitCompletion
import com.cristiancogollo.biblion.feature.studydocs.model.PublicationEligibility
import com.cristiancogollo.biblion.feature.studydocs.model.StudyAuthorSnapshot
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StudyPublicationPolicy
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class StudyPublicationRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun submit(
        document: StudyDoc,
        profile: BiblionUserProfile,
    ): String {
        val eligibility = StudyPublicationPolicy.evaluate(document, profile)
        check(eligibility == PublicationEligibility.ELIGIBLE) {
            eligibility.userMessage()
        }
        val requestId = UUID.randomUUID().toString()
        val contentJson = StudyDocJson.encode(document)
        check(contentJson.toByteArray(Charsets.UTF_8).size <= MAX_CONTENT_BYTES) {
            "La ensenanza supera el tamano permitido para solicitar publicacion"
        }
        val now = System.currentTimeMillis()
        firestore.collection(REQUESTS_COLLECTION).document(requestId)
            .set(
                mapOf(
                    "requestId" to requestId,
                    "status" to "PENDING",
                    "title" to document.title,
                    "tags" to document.metadata.tags,
                    "contentJson" to contentJson,
                    "contentHash" to StudyContentHash.sha256(contentJson),
                    "author" to StudyAuthorSnapshot(
                        uid = profile.uid,
                        alias = profile.alias,
                        displayName = "${profile.nombres} ${profile.apellidos}".trim(),
                    ).toMap(),
                    "provenance" to document.metadata.provenance?.let {
                        mapOf(
                            "rootPublicationId" to it.rootPublicationId,
                            "rootRevisionId" to it.rootRevisionId,
                            "parentPublicationId" to it.parentPublicationId,
                            "parentRevisionId" to it.parentRevisionId,
                        )
                    },
                    "submittedAt" to now,
                ),
            )
            .awaitCompletion()
        return requestId
    }

    private fun StudyAuthorSnapshot.toMap(): Map<String, String> = mapOf(
        "uid" to uid,
        "alias" to alias,
        "displayName" to displayName,
    )

    private fun PublicationEligibility.userMessage(): String = when (this) {
        PublicationEligibility.AUTHENTICATION_REQUIRED -> "Inicia sesion para publicar"
        PublicationEligibility.PROFILE_INCOMPLETE -> "Completa tu perfil antes de publicar"
        PublicationEligibility.PUBLISHER_NOT_APPROVED -> "Tu cuenta aun no tiene permiso de publicador"
        PublicationEligibility.TITLE_REQUIRED -> "La ensenanza necesita un titulo"
        PublicationEligibility.CONTENT_REQUIRED -> "Agrega contenido antes de publicar"
        PublicationEligibility.TAGS_REQUIRED -> "Agrega etiquetas antes de publicar"
        PublicationEligibility.FINAL_STATE_REQUIRED -> "Marca la ensenanza como finalizada antes de publicar"
        PublicationEligibility.ELIGIBLE -> "La ensenanza puede publicarse"
    }

    private companion object {
        const val REQUESTS_COLLECTION = "publication_requests"
        const val MAX_CONTENT_BYTES = 700_000
    }
}

package com.cristiancogollo.biblion.feature.studydocs.model

import com.cristiancogollo.biblion.BiblionUserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyPublicationPolicyTest {
    private val profile = BiblionUserProfile(
        uid = "user-1",
        nombres = "Cristian",
        apellidos = "Cogollo",
        alias = "cristian",
        rol = "PUBLICADOR",
        estadoPublicador = "APROBADO",
    )

    @Test
    fun `requires final state before publication`() {
        val document = StudyDoc(
            title = "La fe",
            blocks = listOf(StudyBlock.Paragraph(text = StyledText("Contenido"))),
            metadata = DocMetadata(tags = listOf("estudio-biblico", "fe")),
        )

        assertEquals(
            PublicationEligibility.FINAL_STATE_REQUIRED,
            StudyPublicationPolicy.evaluate(document, profile),
        )
    }

    @Test
    fun `approved publisher with final teaching is eligible`() {
        val document = StudyDoc(
            title = "La fe",
            blocks = listOf(StudyBlock.Paragraph( text = StyledText("Contenido") )),
            metadata = DocMetadata(tags = listOf("estudio-biblico", "fe", "finalizado")),
        )

        assertEquals(PublicationEligibility.ELIGIBLE, StudyPublicationPolicy.evaluate(document, profile))
    }
}

package com.cristiancogollo.biblion.feature.bibi.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstitutionalDoctrinePolicyTest {
    @Test
    fun `the IPUC baptism answer is based on its articles of faith`() {
        val answer = InstitutionalDoctrinePolicy.verifiedAnswer(
            "segun la IPUC el bautismo es necesario para salvación",
        )

        assertNotNull(answer)
        assertTrue(answer!!.contains("Hechos 2:38"))
        assertTrue(answer.startsWith("Según sus Artículos de Fe"))
        assertTrue(answer.contains("distrito1.ipuc.org.co/articulosdefe/"))
        assertFalse(answer.contains("no es un requisito para la salvación"))
    }

    @Test
    fun `the institution answer does not replace unrelated baptism questions`() {
        assertNull(InstitutionalDoctrinePolicy.verifiedAnswer(
            "¿El bautismo es necesario para la salvación?",
        ))
        assertNull(InstitutionalDoctrinePolicy.verifiedAnswer(
            "¿Qué enseña la IPUC sobre la cena del Señor?",
        ))
        assertNotNull(InstitutionalDoctrinePolicy.verifiedAnswer(
            "Para la Iglesia Pentecostal Unida de Colombia, ¿hay que bautizarse para ser salvo?",
        ))
    }

    @Test
    fun `attributions without registered sources are handled without a model`() {
        assertTrue(InstitutionalDoctrinePolicy.needsOfficialSource(
            "¿Qué enseñan los adventistas sobre el bautismo?",
        ))
        assertTrue(InstitutionalDoctrinePolicy.needsOfficialSource(
            "Según los menonitas, ¿es necesaria la salvación por gracia?",
        ))
        assertTrue(InstitutionalDoctrinePolicy.needsOfficialSource(
            "Según los reformados, ¿qué dicen de la justificación?",
        ))
        assertTrue(InstitutionalDoctrinePolicy.needsOfficialSource(
            "¿Qué cree la Iglesia X sobre la predestinación?",
        ))
        assertFalse(InstitutionalDoctrinePolicy.needsOfficialSource(
            "Según la Biblia, ¿qué dice Juan 3:16 sobre la salvación?",
        ))
        assertFalse(InstitutionalDoctrinePolicy.needsOfficialSource(
            "¿Qué significa el bautismo?",
        ))
        assertFalse(InstitutionalDoctrinePolicy.needsOfficialSource(
            "¿Es necesario el bautismo para la salvación?",
        ))
        assertFalse(InstitutionalDoctrinePolicy.needsOfficialSource(
            "¿Es necesario el bautismo para salvación?",
        ))
        assertFalse(InstitutionalDoctrinePolicy.needsOfficialSource(
            "¿Para los creyentes es necesario bautizarse?",
        ))
        assertNull(InstitutionalDoctrinePolicy.verifiedAnswer(
            "Compara la IPUC y la Iglesia Católica sobre bautismo y salvación",
        ))
    }

    @Test
    fun `unsourced model attribution is not exposed`() {
        assertTrue(InstitutionalDoctrinePolicy.containsUnsupportedAttribution(
            "Según la Iglesia Bautista, el bautismo no es necesario para la salvación.",
        ))
        assertTrue(InstitutionalDoctrinePolicy.containsUnsupportedAttribution(
            "Para los menonitas, el bautismo es solo simbólico.",
        ))
        assertTrue(InstitutionalDoctrinePolicy.containsUnsupportedAttribution(
            "Los bautistas ven el bautismo como testimonio público.",
        ))
        assertTrue(InstitutionalDoctrinePolicy.containsUnsupportedAttribution(
            "La Iglesia Pentecostal Unida enseña que el bautismo es necesario.",
        ))
        assertFalse(InstitutionalDoctrinePolicy.containsUnsupportedAttribution(
            "Según Juan 3:16, Dios amó al mundo.",
        ))
        assertFalse(InstitutionalDoctrinePolicy.containsUnsupportedAttribution(
            "Algunas iglesias sostienen que el bautismo es necesario; otras lo ven como testimonio.",
        ))
    }

    @Test
    fun `general baptism questions have a safe answer without institutional claims`() {
        for (question in listOf(
            "¿Es necesario el bautismo para salvación?",
            "¿Hay que bautizarse para ser salvo?",
        )) {
            val answer = InstitutionalDoctrinePolicy.generalQuestionFallback(question)
            assertNotNull(answer)
            assertTrue(answer!!.contains("Hechos 2:38"))
            assertFalse(answer.contains("No tengo una declaración oficial"))
        }
        assertNull(InstitutionalDoctrinePolicy.generalQuestionFallback(
            "Según los adventistas, ¿es necesario el bautismo para salvación?",
        ))
    }
}

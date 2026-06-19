package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.bibi.BibiResponse
import com.cristiancogollo.biblion.feature.bibi.BibiVerse
import com.cristiancogollo.biblion.feature.bibi.Confidence
import com.cristiancogollo.biblion.feature.bibi.MetadataFact
import com.cristiancogollo.biblion.feature.bibi.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BibiResponseTest {

    @Test
    fun `buildChatText includes title and definition`() {
        val response = BibiResponse(
            title = "Gracia",
            definition = "Favor inmerecido de Dios.",
            followUp = "¿Quieres saber más?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("Gracia"))
        assertTrue(text.contains("Favor inmerecido"))
        assertTrue(text.contains("¿Quieres saber más?"))
    }

    @Test
    fun `buildChatText includes greeting when present`() {
        val response = BibiResponse(
            greeting = "¡Hola Juan! Te cuento.",
            title = "Abraham",
            definition = "Padre de multitudes.",
            followUp = "¿Quieres saber más?"
        )
        val text = response.buildChatText()
        assertTrue(text.startsWith("¡Hola Juan!"))
    }

    @Test
    fun `buildChatText includes subtitle when present`() {
        val response = BibiResponse(
            title = "Jerusalén",
            subtitle = "Ciudad de Paz",
            definition = "Capital de Israel.",
            followUp = "¿Quieres más información?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("Jerusalén"))
        assertTrue(text.contains("Ciudad de Paz"))
    }

    @Test
    fun `buildChatText includes metadata facts with label`() {
        val response = BibiResponse(
            title = "Moisés",
            definition = "Profeta de Israel.",
            metadata = listOf(
                MetadataFact("", "Género", "Masculino"),
                MetadataFact("", "Nacimiento", "1571 a.C.")
            ),
            followUp = "¿Quieres saber más?"
        )
        val text = response.buildChatText()
        assertTrue("Debe incluir label Género", text.contains("Género: Masculino"))
        assertTrue("Debe incluir label Nacimiento", text.contains("Nacimiento: 1571 a.C."))
    }

    @Test
    fun `buildChatText metadata sin label muestra solo value`() {
        val response = BibiResponse(
            title = "Test",
            definition = "Test.",
            metadata = listOf(MetadataFact("", "", "valor sin label")),
            followUp = "?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("valor sin label"))
    }

    @Test
    fun `buildChatText con icono y label muestra ambos`() {
        val response = BibiResponse(
            title = "Test",
            definition = "Test.",
            metadata = listOf(MetadataFact("👤", "Género", "Masculino")),
            followUp = "?"
        )
        val text = response.buildChatText()
        assertTrue("Debe incluir icono", text.contains("👤"))
        assertTrue("Debe incluir label", text.contains("Género:"))
        assertTrue("Debe incluir value", text.contains("Masculino"))
    }

    @Test
    fun `buildChatText does not render empty metadata`() {
        val response = BibiResponse(
            title = "Pacto",
            definition = "Acuerdo entre Dios y el hombre.",
            metadata = emptyList(),
            followUp = "¿Quieres más detalles?"
        )
        val text = response.buildChatText()
        // Should not have extra blank line from empty metadata
        assertFalse(text.contains("\n\n\n"))
    }

    @Test
    fun `buildChatText includes verses with text`() {
        val response = BibiResponse(
            title = "Fe",
            definition = "La certeza de lo que se espera.",
            verses = listOf(
                BibiVerse(ref = "Hebreos 11:1", text = "Es, pues, la fe la certeza de lo que se espera.")
            ),
            followUp = "¿Quieres más versículos?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("Hebreos 11:1"))
        assertTrue(text.contains("certeza de lo que se espera"))
    }

    @Test
    fun `buildChatText includes verse refs without text`() {
        val response = BibiResponse(
            title = "Abraham",
            definition = "Padre de la fe.",
            verses = listOf(
                BibiVerse(ref = "Génesis 12:1", text = "")
            ),
            followUp = "¿Quieres saber más?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("Génesis 12:1"))
    }

    @Test
    fun `buildChatText renders minimal response`() {
        val response = BibiResponse(
            title = "Amor",
            definition = "Dios es amor.",
            followUp = "¿Quieres que profundice?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("Amor"))
        assertTrue(text.contains("Dios es amor"))
        assertTrue(text.contains("¿Quieres que profundice?"))
    }

    @Test
    fun `confidence defaults to HIGH`() {
        val response = BibiResponse(
            title = "Test",
            definition = "Test.",
            followUp = "¿Test?"
        )
        assertEquals(Confidence.HIGH, response.confidence)
    }

    @Test
    fun `source defaults to LOCAL`() {
        val response = BibiResponse(
            title = "Test",
            definition = "Test.",
            followUp = "¿Test?"
        )
        assertEquals(Source.LOCAL, response.source)
    }
}

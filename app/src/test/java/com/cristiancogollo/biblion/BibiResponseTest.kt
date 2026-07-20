package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.bibi.model.BibiResponse
import com.cristiancogollo.biblion.feature.bibi.model.BibiVerse
import com.cristiancogollo.biblion.feature.bibi.model.Confidence
import com.cristiancogollo.biblion.feature.bibi.model.MetadataFact
import com.cristiancogollo.biblion.feature.bibi.model.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun `buildChatText includes multiple verses`() {
        val response = BibiResponse(
            title = "Abraham",
            definition = "Padre de la fe.",
            verses = listOf(
                BibiVerse(ref = "Génesis 12:1", text = ""),
                BibiVerse(ref = "Génesis 12:2", text = ""),
                BibiVerse(ref = "Hebreos 11:8", text = "")
            ),
            followUp = "¿Más?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("Génesis 12:1"))
        assertTrue(text.contains("Génesis 12:2"))
        assertTrue(text.contains("Hebreos 11:8"))
    }

    @Test
    fun `buildChatText includes multiple metadata facts`() {
        val response = BibiResponse(
            title = "Abraham",
            definition = "Padre de multitudes.",
            metadata = listOf(
                MetadataFact("", "Género", "Masculino"),
                MetadataFact("", "Nacimiento", "2001 a.C."),
                MetadataFact("", "Muerte", "1826 a.C.")
            ),
            followUp = "¿Más?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("Género: Masculino"))
        assertTrue(text.contains("Nacimiento: 2001 a.C."))
        assertTrue(text.contains("Muerte: 1826 a.C."))
    }

    @Test
    fun `buildChatText includes details list`() {
        val response = BibiResponse(
            title = "Abraham",
            definition = "Padre de multitudes.",
            details = listOf(
                "También conocido como: Abram",
                "Su nombre significa \"padre de multitudes\"."
            ),
            followUp = "¿Más?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("También conocido como: Abram"))
        assertTrue(text.contains("padre de multitudes"))
    }

    @Test
    fun `buildChatText includes suggestions`() {
        val response = BibiResponse(
            title = "Gracia",
            definition = "Favor inmerecido.",
            suggestions = listOf(
                com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion(
                    "Pedir a IA: pasajes sobre Gracia",
                    "profundiza qué pasajes hablan de Gracia",
                    isAi = true
                )
            ),
            followUp = "¿Más?"
        )
        val text = response.buildChatText()
        assertTrue(text.contains("Gracia"))
        assertTrue(text.contains("Favor inmerecido"))
    }

    @Test
    fun `buildChatText with subtitle shows it after title`() {
        val response = BibiResponse(
            title = "Jerusalén",
            subtitle = "Ciudad de Paz",
            definition = "Capital espiritual.",
            followUp = "¿Más?"
        )
        val text = response.buildChatText()
        val titleIdx = text.indexOf("Jerusalén")
        val subtitleIdx = text.indexOf("Ciudad de Paz")
        assertTrue("Subtitle should appear after title", subtitleIdx > titleIdx)
    }

    @Test
    fun `buildChatText with greeting and metadata shows greeting first`() {
        val response = BibiResponse(
            greeting = "¡Hola Juan!",
            title = "Test",
            definition = "Test def.",
            metadata = listOf(MetadataFact("", "Key", "Value")),
            followUp = "¿Más?"
        )
        val text = response.buildChatText()
        assertTrue("Greeting should be first", text.startsWith("¡Hola Juan!"))
        assertTrue(text.contains("Key: Value"))
    }

    @Test
    fun `BibiSuggestion has correct fields`() {
        val suggestion = com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion(
            label = "Comparar con Fe",
            query = "¿qué diferencia hay entre Amor y Fe?",
            isAi = true
        )
        assertEquals("Comparar con Fe", suggestion.label)
        assertEquals("¿qué diferencia hay entre Amor y Fe?", suggestion.query)
        assertTrue(suggestion.isAi)
    }

    @Test
    fun `BibiSuggestion isAi defaults to false`() {
        val suggestion = com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion(
            label = "Test",
            query = "test"
        )
        assertFalse(suggestion.isAi)
    }

    @Test
    fun `Confidence has all expected values`() {
        assertEquals(3, Confidence.values().size)
        assertNotNull(Confidence.HIGH)
        assertNotNull(Confidence.MEDIUM)
        assertNotNull(Confidence.LOW)
    }

    @Test
    fun `Source has all expected values`() {
        assertEquals(2, Source.values().size)
        assertNotNull(Source.LOCAL)
        assertNotNull(Source.AI)
    }
}

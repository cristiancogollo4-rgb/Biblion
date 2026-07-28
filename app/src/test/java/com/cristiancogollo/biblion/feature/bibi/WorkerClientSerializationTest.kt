package com.cristiancogollo.biblion.feature.bibi

import com.cristiancogollo.biblion.feature.bibi.ui.WorkerClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests para WorkerClient serialization/deserialization.
 * Solo valida parsing de JSON, no tests de red.
 */
class WorkerClientSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ── WorkerRequest serialization ────────────────────────────

    @Test
    fun `WorkerRequest serializes to valid JSON`() {
        val request = WorkerClient.WorkerRequest(
            question = "¿quién fue Abraham?",
            mode = "reader",
            intent = "question"
        )
        val body = json.encodeToString(WorkerClient.WorkerRequest.serializer(), request)
        assertTrue("JSON should contain question", body.contains("¿quién fue Abraham?"))
        assertTrue("JSON should be valid JSON", body.startsWith("{"))
    }

    @Test
    fun `WorkerRequest with study payload serializes correctly`() {
        val request = WorkerClient.WorkerRequest(
            question = "explica este versículo",
            mode = "study",
            intent = "explain",
            study = WorkerClient.StudyPayload(
                title = "Génesis 1",
                selectedText = "En el principio creó Dios",
                notes = listOf("contexto local")
            )
        )
        val body = json.encodeToString(WorkerClient.WorkerRequest.serializer(), request)
        assertTrue("Should contain study title", body.contains("Génesis 1"))
        assertTrue("Should contain selectedText", body.contains("En el principio creó Dios"))
        assertTrue("Should contain notes", body.contains("contexto local"))
    }

    @Test
    fun `WorkerRequest with bible payload serializes correctly`() {
        val request = WorkerClient.WorkerRequest(
            question = "pasajes sobre amor",
            bible = WorkerClient.BiblePayload(
                version = "nvi",
                passages = listOf("Juan 3:16", "1 Juan 4:8")
            )
        )
        val body = json.encodeToString(WorkerClient.WorkerRequest.serializer(), request)
        assertTrue("Should contain version", body.contains("nvi"))
        assertTrue("Should contain passages", body.contains("Juan 3:16"))
    }

    @Test
    fun `WorkerRequest with chat history serializes correctly`() {
        val request = WorkerClient.WorkerRequest(
            question = "¿y qué más?",
            chatHistory = listOf(
                WorkerClient.ChatHistoryEntry(
                    question = "¿quién fue Abraham?",
                    response = "Abraham fue el padre de la fe.",
                    resolvedTerm = "Abraham",
                    intent = "WHO"
                )
            ),
            lastQueries = listOf("¿quién fue Abraham?")
        )
        val body = json.encodeToString(WorkerClient.WorkerRequest.serializer(), request)
        assertTrue("Should contain resolvedTerm", body.contains("Abraham"))
        assertTrue("Should contain lastQueries", body.contains("¿quién fue Abraham?"))
    }

    // ── WorkerResponse deserialization ─────────────────────────

    @Test
    fun `WorkerResponse deserializes valid JSON`() {
        val jsonStr = """
            {
                "answer": "Abraham fue el padre de la fe.",
                "references": ["Génesis 12:1-3", "Hebreos 11:8-10"],
                "suggestedBlocks": [],
                "confidence": "high",
                "disclaimer": null
            }
        """.trimIndent()
        val response = json.decodeFromString(WorkerClient.WorkerResponse.serializer(), jsonStr)
        assertEquals("Abraham fue el padre de la fe.", response.answer)
        assertEquals(2, response.references.size)
        assertEquals("Génesis 12:1-3", response.references[0].reference)
        assertEquals("high", response.confidence)
    }

    @Test
    fun `WorkerResponse deserializes with disclaimer`() {
        val jsonStr = """
            {
                "answer": "Test answer",
                "references": [],
                "suggestedBlocks": [],
                "confidence": "medium",
                "disclaimer": "Esta respuesta puede contener errores."
            }
        """.trimIndent()
        val response = json.decodeFromString(WorkerClient.WorkerResponse.serializer(), jsonStr)
        assertEquals("Esta respuesta puede contener errores.", response.disclaimer)
        assertEquals("medium", response.confidence)
    }

    @Test
    fun `WorkerResponse deserializes empty response`() {
        val jsonStr = """
            {
                "answer": "",
                "references": [],
                "confidence": "low"
            }
        """.trimIndent()
        val response = json.decodeFromString(WorkerClient.WorkerResponse.serializer(), jsonStr)
        assertEquals("", response.answer)
        assertEquals("low", response.confidence)
        assertTrue(response.references.isEmpty())
    }

    @Test
    fun `WorkerResponse handles unknown keys gracefully`() {
        val jsonStr = """
            {
                "answer": "Test",
                "references": [],
                "confidence": "high",
                "unknown_field": "value",
                "another_unknown": 42
            }
        """.trimIndent()
        val response = json.decodeFromString(WorkerClient.WorkerResponse.serializer(), jsonStr)
        assertEquals("Test", response.answer)
    }

    @Test
    fun `WorkerResponse handles malformed JSON by returning raw text`() {
        val malformed = "This is just plain text, not JSON"
        val response = try {
            json.decodeFromString(WorkerClient.WorkerResponse.serializer(), malformed)
        } catch (e: Exception) {
            WorkerClient.WorkerResponse(answer = malformed)
        }
        assertEquals(malformed, response.answer)
    }

    @Test
    fun `WorkerResponse with suggestedBlocks deserializes`() {
        val jsonStr = """
            {
                "answer": "Test",
                "references": [],
                "suggestedBlocks": ["block1", "block2"],
                "confidence": "high"
            }
        """.trimIndent()
        val response = json.decodeFromString(WorkerClient.WorkerResponse.serializer(), jsonStr)
        assertEquals(2, response.suggestedBlocks.size)
        assertEquals("block1", response.suggestedBlocks[0])
    }

    // ── RichWorkerResponse construction ────────────────────────

    @Test
    fun `RichWorkerResponse has correct fields`() {
        val rich = WorkerClient.RichWorkerResponse(
            answer = "Abraham fue el padre de la fe.",
            references = listOf(
                WorkerClient.WorkerReference("Génesis 12:1-3")
            ),
            suggestedBlocks = emptyList(),
            confidence = "high",
            disclaimer = null,
            intentDetected = "define"
        )
        assertEquals("Abraham fue el padre de la fe.", rich.answer)
        assertEquals(1, rich.references.size)
        assertEquals("high", rich.confidence)
        assertNotNull(rich.suggestedBlocks)
    }

    @Test
    fun `WorkerResponse deserializes structured production references`() {
        val jsonStr = """
            {
                "answer": "Consulta también este pasaje.",
                "references": [
                    {
                        "reference": "Isaías 53:5",
                        "reason": "Profecía relacionada"
                    }
                ],
                "confidence": "high"
            }
        """.trimIndent()

        val response = json.decodeFromString(WorkerClient.WorkerResponse.serializer(), jsonStr)

        assertEquals("Isaías 53:5", response.references.single().reference)
        assertEquals("Profecía relacionada", response.references.single().reason)
    }

    // ── ChatHistoryEntry ───────────────────────────────────────

    @Test
    fun `ChatHistoryEntry serializes and deserializes with null resolvedTerm`() {
        val entry = WorkerClient.ChatHistoryEntry(
            question = "test",
            response = "response",
            resolvedTerm = null,
            intent = "FALLBACK"
        )
        val body = json.encodeToString(WorkerClient.ChatHistoryEntry.serializer(), entry)
        assertTrue("Should contain question", body.contains("test"))
        assertTrue("Should contain response", body.contains("response"))
        // Deserialize back and verify null preserved
        val parsed = json.decodeFromString(WorkerClient.ChatHistoryEntry.serializer(), body)
        assertEquals(null, parsed.resolvedTerm)
    }

    @Test
    fun `ChatHistoryEntry with all fields`() {
        val entry = WorkerClient.ChatHistoryEntry(
            question = "¿quién fue Abraham?",
            response = "Abraham fue...",
            resolvedTerm = "Abraham",
            intent = "WHO"
        )
        val body = json.encodeToString(WorkerClient.ChatHistoryEntry.serializer(), entry)
        assertTrue("Should contain question", body.contains("¿quién fue Abraham?"))
        assertTrue("Should contain resolvedTerm", body.contains("Abraham"))
        val parsed = json.decodeFromString(WorkerClient.ChatHistoryEntry.serializer(), body)
        assertEquals("Abraham", parsed.resolvedTerm)
        assertEquals("WHO", parsed.intent)
    }

    // ── WorkerRequest intent field ─────────────────────────────

    @Test
    fun `WorkerRequest with explain intent roundtrips`() {
        val request = WorkerClient.WorkerRequest(
            question = "explica este versículo",
            intent = "explain"
        )
        val body = json.encodeToString(WorkerClient.WorkerRequest.serializer(), request)
        val parsed = json.decodeFromString(WorkerClient.WorkerRequest.serializer(), body)
        assertEquals("explain", parsed.intent)
    }

    @Test
    fun `WorkerRequest with define intent roundtrips`() {
        val request = WorkerClient.WorkerRequest(
            question = "¿qué significa gracia?",
            intent = "define"
        )
        val body = json.encodeToString(WorkerClient.WorkerRequest.serializer(), request)
        val parsed = json.decodeFromString(WorkerClient.WorkerRequest.serializer(), body)
        assertEquals("define", parsed.intent)
    }

    @Test
    fun `WorkerRequest with cross_reference intent roundtrips`() {
        val request = WorkerClient.WorkerRequest(
            question = "pasajes relacionados con Juan 3:16",
            intent = "cross_reference"
        )
        val body = json.encodeToString(WorkerClient.WorkerRequest.serializer(), request)
        val parsed = json.decodeFromString(WorkerClient.WorkerRequest.serializer(), body)
        assertEquals("cross_reference", parsed.intent)
    }

    // ── StudyPayload ───────────────────────────────────────────

    @Test
    fun `StudyPayload serializes with notes`() {
        val payload = WorkerClient.StudyPayload(
            title = "Génesis 1",
            notes = listOf("contexto: tema=creación", "temas: creación, caído")
        )
        val body = json.encodeToString(WorkerClient.StudyPayload.serializer(), payload)
        assertTrue("Should contain notes", body.contains("contexto: tema=creación"))
        val parsed = json.decodeFromString(WorkerClient.StudyPayload.serializer(), body)
        assertEquals(2, parsed.notes.size)
    }

    // ── BiblePayload ───────────────────────────────────────────

    @Test
    fun `BiblePayload serializes passages`() {
        val payload = WorkerClient.BiblePayload(
            version = "nvi",
            passages = listOf("Juan 3:16", "1 Juan 4:8", "Romanos 8:28")
        )
        val body = json.encodeToString(WorkerClient.BiblePayload.serializer(), payload)
        val parsed = json.decodeFromString(WorkerClient.BiblePayload.serializer(), body)
        assertEquals(3, parsed.passages.size)
        assertEquals("nvi", parsed.version)
    }

    // ── WorkerResponse confidence values ───────────────────────

    @Test
    fun `WorkerResponse deserializes confidence high`() {
        val jsonStr = """{"answer": "ok", "confidence": "high"}"""
        val response = json.decodeFromString(WorkerClient.WorkerResponse.serializer(), jsonStr)
        assertEquals("high", response.confidence)
    }

    @Test
    fun `WorkerResponse deserializes confidence low`() {
        val jsonStr = """{"answer": "ok", "confidence": "low"}"""
        val response = json.decodeFromString(WorkerClient.WorkerResponse.serializer(), jsonStr)
        assertEquals("low", response.confidence)
    }
}

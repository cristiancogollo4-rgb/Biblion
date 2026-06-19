package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.bibi.BibiUserContext
import com.cristiancogollo.biblion.feature.bibi.ChatExchange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChatSessionTest {

    @Test
    fun `ChatExchange se construye correctamente`() {
        val exchange = ChatExchange(
            question = "¿quién fue Abraham?",
            response = "Abraham fue el padre de...",
            resolvedTerm = "Abraham",
            intent = "WHO"
        )
        assertEquals("¿quién fue Abraham?", exchange.question)
        assertEquals("Abraham", exchange.resolvedTerm)
        assertEquals("WHO", exchange.intent)
    }

    @Test
    fun `ChatExchange con resolvedTerm null es valido`() {
        val exchange = ChatExchange(
            question = "hola",
            response = "¡Hola! Soy Bibi"
        )
        assertNull(exchange.resolvedTerm)
        assertEquals("FALLBACK", exchange.intent)
    }

    @Test
    fun `BibiUserContext con chatHistory`() {
        val history = listOf(
            ChatExchange("¿dónde queda Galilea?", "Galilea es...", "Galilea", "WHERE"),
            ChatExchange("¿y qué más?", "...", "Galilea", "FOLLOWUP")
        )
        val ctx = BibiUserContext(
            userName = "Juan",
            currentBook = "Mateo",
            currentChapter = 5,
            chatHistory = history
        )
        assertEquals("Juan", ctx.userName)
        assertEquals(2, ctx.chatHistory.size)
        assertEquals("Galilea", ctx.chatHistory.last().resolvedTerm)
    }

    @Test
    fun `StudyAssistantRequest incluye chatHistory`() {
        val request = StudyAssistantRequest(
            question = "¿dónde vivió?",
            studyTitle = "Génesis capítulo 12",
            studyTags = emptyList(),
            selectedText = "",
            mode = StudyAssistantMode.READER,
            chatHistory = listOf(
                ChatExchange("¿quién fue Abraham?", "Abraham fue...", "Abraham", "WHO")
            )
        )
        assertEquals(1, request.chatHistory.size)
        assertEquals("Abraham", request.chatHistory.first().resolvedTerm)
    }

    @Test
    fun `StudyAssistantRequest tiene chatHistory vacio por default`() {
        val request = StudyAssistantRequest(
            question = "test",
            studyTitle = "",
            studyTags = emptyList(),
            selectedText = ""
        )
        assertNotNull(request.chatHistory)
        assertEquals(0, request.chatHistory.size)
    }

    @Test
    fun `anaphora test con multiple entries`() {
        val history = listOf(
            ChatExchange("¿qué es la gracia?", "...", "gracia", "DEFINE"),
            ChatExchange("versículos", "...", "gracia", "RELATED"),
            ChatExchange("más", "...", "gracia", "FOLLOWUP")
        )
        val last = history.lastOrNull { it.resolvedTerm != null }?.resolvedTerm
        assertEquals("gracia", last)
    }

    @Test
    fun `anaphora con termino en posision intermedia`() {
        val history = listOf(
            ChatExchange("hola", "Hola!"),
            ChatExchange("¿qué es la fe?", "...", "fe", "DEFINE"),
            ChatExchange("y ejemplos?", "...", null, "FALLBACK")
        )
        val last = history.lastOrNull { it.resolvedTerm != null }?.resolvedTerm
        assertEquals("fe", last)
    }
}

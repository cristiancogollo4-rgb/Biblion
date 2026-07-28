package com.cristiancogollo.biblion.feature.bibi

import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.feature.bibi.ui.WorkerClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibiWorkerRequestTest {
    @Test
    fun `reader request includes active verse text and version`() {
        val request = WorkerClient.buildRequest(
            question = "Explicame este versiculo",
            context = BibiContext.Reader(
                book = "Juan",
                chapter = 3,
                passages = listOf(
                    BibiPassage("Juan", 3, 16, "Porque de tal manera amo Dios al mundo")
                ),
                bibleVersion = "nvi",
            ),
            intent = "explain",
            chatHistory = emptyList(),
            lastQueries = emptyList(),
        )

        assertEquals("reader", request.mode)
        assertEquals("nvi", request.bible?.version)
        assertTrue(request.bible?.passages?.single()?.contains("Juan 3:16") == true)
        assertTrue(request.study?.selectedText?.contains("amo Dios") == true)
    }

    @Test
    fun `study request uses study mode and complete editor context`() {
        val request = WorkerClient.buildRequest(
            question = "Sugiere una aplicacion",
            context = BibiContext.Study(
                documentId = "doc-1",
                title = "La gracia",
                tags = listOf("devocional"),
                selectedText = "Por gracia sois salvos",
                outline = listOf("Introduccion", "La gracia como regalo"),
                notes = listOf("Mantener un tono pastoral"),
                passages = listOf(BibiPassage("Efesios", 2, 8, "Porque por gracia sois salvos")),
                bibleVersion = "rvr1960",
            ),
            intent = "application",
            chatHistory = emptyList(),
            lastQueries = emptyList(),
        )

        assertEquals("study", request.mode)
        assertEquals("La gracia", request.study?.title)
        assertEquals(2, request.study?.currentOutline?.size)
        assertEquals("Efesios 2:8 Porque por gracia sois salvos", request.bible?.passages?.single())
    }
}

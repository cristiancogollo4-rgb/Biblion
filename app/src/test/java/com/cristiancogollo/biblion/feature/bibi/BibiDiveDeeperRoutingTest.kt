package com.cristiancogollo.biblion.feature.bibi

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.bibi.domain.BibiOrchestrator
import com.cristiancogollo.biblion.feature.bibi.domain.RemoteBibiGateway
import com.cristiancogollo.biblion.feature.bibi.engine.KnowledgeEngine
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.Source
import com.cristiancogollo.biblion.feature.bibi.ui.WorkerClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BibiDiveDeeperRoutingTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val reader = BibiContext.Reader("Exodo", 3, bibleVersion = "rv1960")

    @Test
    fun `explicit deepen phrases win over WHO and DEFINE`() {
        listOf(
            "cuéntame más sobre Moisés",
            "cuantame mas sobre moises",
            "contame más de Moisés",
            "explica más",
            "explícame eso",
            "profundiza en la zarza ardiente",
        ).forEach { question ->
            assertEquals(
                question,
                KnowledgeEngine.BibiIntent.DIVE_DEEPER,
                KnowledgeEngine.detectIntent(question, listOf("¿quién fue Moisés?")),
            )
        }
    }

    @Test
    fun `ordinary questions that only mention later events are not deepen requests`() {
        assertNotEquals(
            KnowledgeEngine.BibiIntent.DIVE_DEEPER,
            KnowledgeEngine.detectIntent("¿quién fue Moisés?"),
        )
    }

    @Test
    fun `deepen request goes to the worker instead of repeating the local entry`() = runBlocking {
        var sentQuestion: String? = null
        val orchestrator = BibiOrchestrator(RemoteBibiGateway { request ->
            sentQuestion = request.question
            WorkerClient.RichWorkerResponse(
                "Moisés fue llamado desde la zarza (Éxodo 3) y guio al pueblo fuera de Egipto.",
                emptyList(), emptyList(), "high", null, null,
            )
        })

        val answer = orchestrator.answer(
            app, "cuéntame más sobre Moisés", reader, null, emptyList(), emptyList(),
        )

        assertEquals("cuéntame más sobre Moisés", sentQuestion)
        assertEquals(Source.AI, answer.source)
        assertTrue(answer.answer.contains("zarza"))
    }
}

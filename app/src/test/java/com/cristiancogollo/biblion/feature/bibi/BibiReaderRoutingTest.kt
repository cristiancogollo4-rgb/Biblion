package com.cristiancogollo.biblion.feature.bibi

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.bibi.domain.BibiOrchestrator
import com.cristiancogollo.biblion.feature.bibi.domain.RemoteBibiGateway
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.ui.BibiViewModel
import com.cristiancogollo.biblion.feature.bibi.ui.WorkerClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BibiReaderRoutingTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val reader = BibiContext.Reader("Juan", 3, bibleVersion = "rv1960")

    @Test
    fun `question cannot be sent before chat restoration finishes`() {
        val viewModel = BibiViewModel(app)

        viewModel.sendQuestion("¿Quién fue Moisés?", reader, null)

        assertFalse(viewModel.uiState.value.isInitialized)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `unknown intention gives the server a chance to infer it`() = runBlocking {
        var sentIntent: String? = null
        val orchestrator = BibiOrchestrator(RemoteBibiGateway { request ->
            sentIntent = request.intent
            WorkerClient.RichWorkerResponse("Respuesta", emptyList(), emptyList(), "medium", null, null)
        })

        orchestrator.answer(app, "Una pregunta sin patrón conocido", reader, null, emptyList(), emptyList())

        assertEquals("unknown", sentIntent)
    }

    @Test
    fun `application question uses its own server intention`() = runBlocking {
        var sentIntent: String? = null
        val orchestrator = BibiOrchestrator(RemoteBibiGateway { request ->
            sentIntent = request.intent
            WorkerClient.RichWorkerResponse("Respuesta", emptyList(), emptyList(), "medium", null, null)
        })

        orchestrator.answer(app, "¿Cómo aplico este versículo a mi vida?", reader, null, emptyList(), emptyList())

        assertEquals("application", sentIntent)
    }
}

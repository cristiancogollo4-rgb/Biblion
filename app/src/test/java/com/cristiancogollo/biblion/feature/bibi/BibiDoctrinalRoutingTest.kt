package com.cristiancogollo.biblion.feature.bibi

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.bibi.domain.BibiOrchestrator
import com.cristiancogollo.biblion.feature.bibi.domain.DoctrinalPerspectivePolicy
import com.cristiancogollo.biblion.feature.bibi.domain.RemoteBibiGateway
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
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
class BibiDoctrinalRoutingTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val reader = BibiContext.Reader("Juan", 10, bibleVersion = "rv1960")

    @Test
    fun `verified IPUC question does not ask the model to invent institutional doctrine`() = runBlocking {
        var remoteCalled = false
        val orchestrator = BibiOrchestrator(RemoteBibiGateway {
            remoteCalled = true
            null
        })

        val answer = orchestrator.answer(
            app, "segun la IPUC el bautismo es necesario para salvación",
            reader, null, emptyList(), emptyList(),
        )

        assertFalse(remoteCalled)
        assertTrue(answer.answer.contains("Hechos 2:38"))
        assertTrue(answer.answer.contains("Artículos de Fe de la IPUC"))
    }

    @Test
    fun `without a declared perspective Bibi does not ask the model for a verdict`() = runBlocking {
        var remoteCalled = false
        val orchestrator = BibiOrchestrator(RemoteBibiGateway {
            remoteCalled = true
            null
        })

        val answer = orchestrator.answer(
            app, "¿Es falsa la Trinidad?", reader, null, emptyList(), emptyList(),
        )

        assertFalse(remoteCalled)
        assertTrue(answer.answer.contains("No asumiré una postura"))
    }

    @Test
    fun `an explicit perspective bypasses biased local definitions and is sent to worker`() = runBlocking {
        var sentPerspective: String? = null
        val orchestrator = BibiOrchestrator(RemoteBibiGateway { request ->
            sentPerspective = request.doctrinalPerspective
            WorkerClient.RichWorkerResponse(
                "Juan 10:30 puede leerse en el contexto de la unidad divina.",
                emptyList(), emptyList(), "medium", null, null,
            )
        })

        val answer = orchestrator.answer(
            app, "Soy de la unicidad. ¿Qué es la Trinidad?", reader,
            null, emptyList(), emptyList(),
            conversationPerspective = DoctrinalPerspectivePolicy.Perspective.ONENESS,
        )

        assertEquals("oneness", sentPerspective)
        assertTrue(answer.answer.startsWith("Desde la perspectiva de la unicidad"))
    }

    @Test
    fun `a harmful claim in a secondary response field is not exposed`() = runBlocking {
        val orchestrator = BibiOrchestrator(RemoteBibiGateway {
            WorkerClient.RichWorkerResponse(
                "Consideremos el pasaje.",
                emptyList(), listOf("La Trinidad es falsa"), "high", null, null,
            )
        })

        val answer = orchestrator.answer(
            app, "Soy de la unicidad. ¿Qué es la Trinidad?", reader,
            null, emptyList(), emptyList(),
            conversationPerspective = DoctrinalPerspectivePolicy.Perspective.ONENESS,
        )

        assertFalse(answer.answer.contains("La Trinidad es falsa"))
        assertTrue(answer.answer.contains("sin juzgar la fe"))
    }
}

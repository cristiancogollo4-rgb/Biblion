package com.cristiancogollo.biblion.feature.bibi.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.ui.WorkerClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BibiInstitutionalRoutingTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val reader = BibiContext.Reader("Juan", 10, bibleVersion = "rv1960")

    @Test
    fun `unknown denominational doctrine never reaches the model`() = runBlocking {
        var remoteCalled = false
        val orchestrator = BibiOrchestrator(RemoteBibiGateway {
            remoteCalled = true
            null
        })

        val result = orchestrator.answer(
            app, "¿Qué enseñan los adventistas sobre el bautismo?",
            reader, null, emptyList(), emptyList(),
        )

        assertFalse(remoteCalled)
        assertTrue(result.answer.contains("No tengo una declaración oficial verificada"))
    }

    @Test
    fun `unsolicited institutional claim from model is not shown`() = runBlocking {
        val orchestrator = BibiOrchestrator(RemoteBibiGateway {
            WorkerClient.RichWorkerResponse(
                "Según la Iglesia Bautista, el bautismo no es necesario para la salvación.",
                emptyList(), emptyList(), "high", null, null,
            )
        })

        val result = orchestrator.answer(
            app, "Explica el bautismo", reader, null, emptyList(), emptyList(),
            forceRemote = true,
        )

        assertTrue(result.answer.contains("No tengo una declaración oficial verificada"))
        assertFalse(result.answer.contains("Iglesia Bautista"))
    }
}

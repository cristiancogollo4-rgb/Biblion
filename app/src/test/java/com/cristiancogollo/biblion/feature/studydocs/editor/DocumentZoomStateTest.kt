package com.cristiancogollo.biblion.feature.studydocs.editor

import com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocumentZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentZoomStateTest {

    @Test
    fun `set respeta el rango minimo`() {
        val state = DocumentZoomState()
        state.set(0.1f)
        assertEquals(DocumentZoomState.MIN_ZOOM, state.zoom, 0.0001f)
    }

    @Test
    fun `set respeta el rango maximo`() {
        val state = DocumentZoomState()
        state.set(10f)
        assertEquals(DocumentZoomState.MAX_ZOOM, state.zoom, 0.0001f)
    }

    @Test
    fun `step positivo aumenta el zoom`() {
        val state = DocumentZoomState()
        val initial = state.zoom
        state.step(0.25f)
        assertTrue("Esperaba zoom > $initial, fue ${state.zoom}", state.zoom > initial)
    }

    @Test
    fun `step negativo reduce el zoom`() {
        val state = DocumentZoomState(initial = 1.5f)
        val initial = state.zoom
        state.step(-0.25f)
        assertTrue("Esperaba zoom < $initial, fue ${state.zoom}", state.zoom < initial)
    }

    @Test
    fun `reset vuelve a 1_0`() {
        val state = DocumentZoomState(initial = 1.75f)
        state.reset()
        assertEquals(1.0f, state.zoom, 0.0001f)
    }

    @Test
    fun `presets estan ordenados y dentro del rango`() {
        val presets = DocumentZoomState.PRESETS
        assertEquals(listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f), presets)
        presets.forEach { p ->
            assertTrue(p in DocumentZoomState.MIN_ZOOM..DocumentZoomState.MAX_ZOOM)
        }
    }
}

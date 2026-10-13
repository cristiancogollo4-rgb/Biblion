package com.cristiancogollo.biblion.feature.studydocs.editor

import com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocumentZoomState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `fit width calcula la escala usando el ancho disponible`() {
        val state = DocumentZoomState()
        state.updateViewport(Size(600f, 1000f))
        state.updateContent(Size(400f, 1200f))

        state.fitWidth(horizontalPaddingPx = 20f)

        assertEquals(1.4f, state.zoom, 0.0001f)
    }

    @Test
    fun `fit width respeta el rango del zoom`() {
        val state = DocumentZoomState()
        state.updateViewport(Size(1000f, 1000f))
        state.updateContent(Size(100f, 1000f))
        state.fitWidth(horizontalPaddingPx = 0f)
        assertEquals(DocumentZoomState.MAX_ZOOM, state.zoom, 0.0001f)

        state.updateContent(Size(2000f, 1000f))
        state.fitWidth(horizontalPaddingPx = 0f)
        assertEquals(DocumentZoomState.MIN_ZOOM, state.zoom, 0.0001f)
    }

    @Test
    fun `gesto de zoom marca su inicio y termina sin alterar el factor`() {
        val state = DocumentZoomState(initial = 1.25f)
        assertFalse(state.isGesturing)

        state.beginGesture()
        assertTrue(state.isGesturing)
        state.endGesture()

        assertFalse(state.isGesturing)
        assertEquals(1.25f, state.zoom, 0.0001f)
    }

    @Test
    fun `el pellizco solo confirma el zoom al terminar`() {
        val state = DocumentZoomState(initial = 1f)
        state.beginGesture(Offset(0.25f, 0.75f))
        state.updateGesture(1.4f)

        assertEquals(1f, state.zoom, 0.0001f)
        assertEquals(1.4f, state.visualScale, 0.0001f)
        assertEquals(Offset(0.25f, 0.75f), state.gestureAnchorFraction)

        state.endGesture()

        assertEquals(1.4f, state.zoom, 0.0001f)
        assertEquals(1f, state.visualScale, 0.0001f)
    }

    @Test
    fun `el pellizco respeta limites y un preset cancela la vista previa`() {
        val state = DocumentZoomState(initial = 1f)
        state.beginGesture()
        state.updateGesture(10f)
        assertEquals(DocumentZoomState.MAX_ZOOM, state.gestureZoom, 0.0001f)

        state.set(1.25f)

        assertFalse(state.isGesturing)
        assertEquals(1.25f, state.zoom, 0.0001f)
        assertEquals(1f, state.visualScale, 0.0001f)
    }

    @Test
    fun `eventos invalidos o fuera del gesto no modifican el zoom`() {
        val state = DocumentZoomState(initial = 1f)
        state.updateGesture(1.5f)
        state.beginGesture()
        state.updateGesture(Float.NaN)
        state.updateGesture(0f)
        state.endGesture()
        state.set(Float.NaN)

        assertEquals(1f, state.zoom, 0.0001f)
    }
}

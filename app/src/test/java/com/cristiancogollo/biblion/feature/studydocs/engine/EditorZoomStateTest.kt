package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.ui.editor.EditorZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorZoomStateTest {

    @Test fun `initial state is 1_0 scale and 0 offset`() {
        val s = EditorZoomState.Initial
        assertEquals(1f, s.scale, 0f)
        assertEquals(0f, s.offsetY, 0f)
        assertFalse(s.isZooming)
    }

    @Test fun `clamped respects min and max scale`() {
        val below = EditorZoomState(scale = 0.1f).clamped()
        assertEquals(EditorZoomState.MIN_SCALE, below.scale, 0f)
        val above = EditorZoomState(scale = 5f).clamped()
        assertEquals(EditorZoomState.MAX_SCALE, above.scale, 0f)
    }

    @Test fun `applyPinchAndPan multiplies scale and adds offset`() {
        val s = EditorZoomState.Initial.applyPinchAndPan(scaleFactor = 1.5f, panDeltaY = 10f)
        assertEquals(1.5f, s.scale, 0f)
        assertEquals(10f, s.offsetY, 0f)
        assertTrue(s.isZooming)
    }

    @Test fun `applyPinchAndPan clamps the result to max scale`() {
        val s = EditorZoomState.Initial.applyPinchAndPan(scaleFactor = 10f, panDeltaY = 0f)
        assertEquals(EditorZoomState.MAX_SCALE, s.scale, 0f)
    }

    @Test fun `applyPinchAndPan clamps the result to min scale`() {
        val s = EditorZoomState(scale = 0.6f).applyPinchAndPan(scaleFactor = 0.1f, panDeltaY = 0f)
        assertEquals(EditorZoomState.MIN_SCALE, s.scale, 0f)
    }

    @Test fun `applyPinchAndPan chains across multiple calls`() {
        val s1 = EditorZoomState.Initial.applyPinchAndPan(1.5f, 0f)
        val s2 = s1.applyPinchAndPan(1.5f, 0f)
        // 1 * 1.5 * 1.5 = 2.25
        assertEquals(2.25f, s2.scale, 0.001f)
    }

    @Test fun `applyPan only changes offsetY`() {
        val s = EditorZoomState.Initial.applyPan(20f)
        assertEquals(1f, s.scale, 0f)
        assertEquals(20f, s.offsetY, 0f)
        assertTrue(s.isZooming)
    }

    @Test fun `endInteraction clears isZooming`() {
        val s = EditorZoomState.Initial.applyPan(20f).endInteraction()
        assertFalse(s.isZooming)
        // scale y offset no se resetean
        assertEquals(20f, s.offsetY, 0f)
    }

    @Test fun `reset returns to initial`() {
        val s = EditorZoomState(scale = 2f, offsetY = 50f, isZooming = true).reset()
        assertEquals(EditorZoomState.Initial, s)
    }

    @Test fun `displayPercent formats as integer percent`() {
        assertEquals("100%", EditorZoomState.Initial.displayPercent())
        assertEquals("150%", EditorZoomState(scale = 1.5f).displayPercent())
        assertEquals("50%", EditorZoomState(scale = 0.5f).displayPercent())
    }

    @Test fun `zoom only affects Y axis conceptually - scaleX stays at 1 by Modifier convention`() {
        // EditorZoomState solo expone scale; el Modifier.zoomGraphics aplica scaleX=1f fijo.
        // Verificamos que el state no expone un scaleX:
        val s = EditorZoomState(scale = 2f)
        // No existe propiedad scaleX en EditorZoomState; usamos reflexion ligera
        val hasScaleX = EditorZoomState::class.java.declaredFields.any { it.name == "scaleX" }
        assertFalse("EditorZoomState no debe exponer scaleX (el zoom es solo vertical)", hasScaleX)
    }

    @Test fun `MIN and MAX constants are 0_5 and 3_0`() {
        assertEquals(0.5f, EditorZoomState.MIN_SCALE, 0f)
        assertEquals(3.0f, EditorZoomState.MAX_SCALE, 0f)
        assertNotEquals(EditorZoomState.MIN_SCALE, EditorZoomState.MAX_SCALE)
    }
}

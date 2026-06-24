package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.ui.editor.EditorZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorZoomStateTest {

    @Test fun `initial state is scale 1_0 and not zooming`() {
        val s = EditorZoomState.Initial
        assertEquals(1f, s.scale, 0f)
        assertFalse(s.isZooming)
    }

    @Test fun `applyPinch multiplies scale`() {
        val s = EditorZoomState.Initial.applyPinch(1.5f)
        assertEquals(1.5f, s.scale, 0f)
        assertTrue(s.isZooming)
    }

    @Test fun `applyPinch clamps to max scale`() {
        val s = EditorZoomState.Initial.applyPinch(10f)
        assertEquals(EditorZoomState.MAX_SCALE, s.scale, 0f)
    }

    @Test fun `applyPinch clamps to min scale`() {
        val s = EditorZoomState(scale = 0.6f).applyPinch(0.1f)
        assertEquals(EditorZoomState.MIN_SCALE, s.scale, 0f)
    }

    @Test fun `applyPinch chains across multiple calls`() {
        val s1 = EditorZoomState.Initial.applyPinch(1.5f)
        val s2 = s1.applyPinch(1.5f)
        assertEquals(2.25f, s2.scale, 0.001f)
    }

    @Test fun `endInteraction clears isZooming`() {
        val s = EditorZoomState.Initial.applyPinch(1.5f).endInteraction()
        assertFalse(s.isZooming)
        assertEquals(1.5f, s.scale, 0f)
    }

    @Test fun `reset returns to initial`() {
        val s = EditorZoomState(scale = 2f, isZooming = true).reset()
        assertEquals(EditorZoomState.Initial, s)
    }

    @Test fun `displayPercent formats as integer percent`() {
        assertEquals("100%", EditorZoomState.Initial.displayPercent())
        assertEquals("150%", EditorZoomState(scale = 1.5f).displayPercent())
        assertEquals("50%", EditorZoomState(scale = 0.5f).displayPercent())
    }

    @Test fun `MIN and MAX constants are 0_5 and 3_0`() {
        assertEquals(0.5f, EditorZoomState.MIN_SCALE, 0f)
        assertEquals(3.0f, EditorZoomState.MAX_SCALE, 0f)
        assertNotEquals(EditorZoomState.MIN_SCALE, EditorZoomState.MAX_SCALE)
    }

    @Test fun `scale is uniform - state has no separate scaleX or scaleY`() {
        val hasScaleX = EditorZoomState::class.java.declaredFields.any { it.name == "scaleX" }
        val hasScaleY = EditorZoomState::class.java.declaredFields.any { it.name == "scaleY" }
        val hasOffsetY = EditorZoomState::class.java.declaredFields.any { it.name == "offsetY" }
        assertFalse(hasScaleX)
        assertFalse(hasScaleY)
        assertFalse(hasOffsetY)
    }
}

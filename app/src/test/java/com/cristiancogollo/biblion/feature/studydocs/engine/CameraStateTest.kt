package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.ui.editor.CameraState
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraStateTest {

    @Test fun `initial state is zoom 1_0`() {
        val c = CameraState.Initial
        assertEquals(1f, c.zoom)
    }

    @Test fun `stepIn advances to next zoom level`() {
        val c = CameraState(zoom = 1f).stepIn()
        assertEquals(1.25f, c.zoom)
        val c2 = c.stepIn()
        assertEquals(1.5f, c2.zoom)
        val c3 = c2.stepIn().stepIn().stepIn()
        assertEquals(CameraState.MAX_ZOOM, c3.zoom)
    }

    @Test fun `stepOut decreases to previous zoom level`() {
        val c = CameraState(zoom = 1f).stepOut()
        assertEquals(0.90f, c.zoom)
        val c2 = c.stepOut()
        assertEquals(0.75f, c2.zoom)
        val c3 = c2.stepOut()
        assertEquals(CameraState.MIN_ZOOM, c3.zoom)
    }

    @Test fun `snapZoom moves to nearest discrete level`() {
        assertEquals(1f, CameraState(zoom = 1.1f).snapZoom().zoom, 0f)
        assertEquals(1.25f, CameraState(zoom = 1.3f).snapZoom().zoom, 0f)
        assertEquals(1.5f, CameraState(zoom = 1.4f).snapZoom().zoom, 0f)
    }

    @Test fun `displayPercent formats correctly`() {
        assertEquals("100%", CameraState(zoom = 1f).displayPercent())
        assertEquals("50%", CameraState(zoom = 0.5f).displayPercent())
        assertEquals("250%", CameraState(zoom = 2.5f).displayPercent())
        assertEquals("150%", CameraState(zoom = 1.5f).displayPercent())
    }

    @Test fun `zoom levels are 7 discrete values`() {
        assertEquals(7, CameraState.ZOOM_LEVELS.size)
        assertEquals(CameraState.MIN_ZOOM, CameraState.ZOOM_LEVELS.first())
        assertEquals(CameraState.MAX_ZOOM, CameraState.ZOOM_LEVELS.last())
    }

    @Test fun `reset returns to initial`() {
        val c = CameraState(zoom = 2f).reset()
        assertEquals(CameraState.Initial, c)
    }
}

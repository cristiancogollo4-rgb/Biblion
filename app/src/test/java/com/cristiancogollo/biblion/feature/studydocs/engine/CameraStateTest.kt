package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.ui.editor.CameraState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraStateTest {

    @Test fun `initial state is zoom 1_0 and no offset`() {
        val c = CameraState.Initial
        assertEquals(1f, c.zoom)
        assertEquals(0f, c.offsetX)
        assertEquals(0f, c.offsetY)
    }

    @Test fun `focalZoom from center keeps center stable`() {
        val c = CameraState(zoom = 1f, offsetX = 0f, offsetY = 0f)
        val result = c.focalZoom(newZoom = 2f, focusX = 500f, focusY = 500f)
        assertEquals(2f, result.zoom)
        // screenToWorld at focus: (500-0)/1 = (500,500) world
        // focalZoom: offsetX = 500 - 500*2 = -500
        // screenToWorld at new: (500-(-500))/2 = 500 ✓
        val world = result.screenToWorld(500f, 500f)
        assertEquals(500f, world.x, 0.01f)
        assertEquals(500f, world.y, 0.01f)
    }

    @Test fun `focalZoom clamps to min and max`() {
        val c = CameraState(zoom = 1f)
        val minResult = c.focalZoom(newZoom = 0.1f, focusX = 0f, focusY = 0f)
        assertEquals(CameraState.MIN_ZOOM, minResult.zoom)
        val maxResult = c.focalZoom(newZoom = 10f, focusX = 0f, focusY = 0f)
        assertEquals(CameraState.MAX_ZOOM, maxResult.zoom)
    }

    @Test fun `clamp keeps offset within bounds`() {
        val c = CameraState(zoom = 1f, offsetX = -500f, offsetY = -500f)
        val docW = 560f; val docH = 900f
        val vpW = 400f; val vpH = 600f
        val clamped = c.clamp(vpW, vpH, docW, docH)
        // offsetX: -500 vs range [-docW*z+vpW, 0] = [-560+400, 0] = [-160, 0]
        assertEquals(-160f, clamped.offsetX, 0.1f)
        // offsetY: -500 vs range [-docH*z+vpH, 0] = [-900+600, 0] = [-300, 0]
        assertEquals(-300f, clamped.offsetY, 0.1f)
    }

    @Test fun `clamp at zoom 2x keeps bounds proportional`() {
        val c = CameraState(zoom = 2f, offsetX = -2000f, offsetY = -2000f)
        val clamped = c.clamp(vpW = 400f, vpH = 600f, docW = 560f, docH = 900f)
        // scaledW = 560*2 = 1120, scaledH = 900*2 = 1800
        // offsetX: [-1120+400, 0] = [-720, 0]
        assertEquals(-720f, clamped.offsetX, 0.1f)
        // offsetY: [-1800+600, 0] = [-1200, 0]
        assertEquals(-1200f, clamped.offsetY, 0.1f)
    }

    @Test fun `stepIn advances to next zoom level`() {
        val c = CameraState(zoom = 1f).stepIn()
        assertEquals(1.25f, c.zoom)
        val c2 = c.stepIn()
        assertEquals(1.5f, c2.zoom)
        val c3 = c2.stepIn().stepIn().stepIn().stepIn()
        assertEquals(CameraState.MAX_ZOOM, c3.zoom)
    }

    @Test fun `stepOut decreases to previous zoom level`() {
        val c = CameraState(zoom = 1f).stepOut()
        assertEquals(0.75f, c.zoom)
        val c2 = c.stepOut()
        assertEquals(CameraState.MIN_ZOOM, c2.zoom)
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

    @Test fun `screenToWorld and worldToScreen are inverses`() {
        val c = CameraState(zoom = 1.5f, offsetX = 100f, offsetY = 200f)
        val world = c.screenToWorld(300f, 400f)
        val screen = c.worldToScreen(world.x, world.y)
        assertEquals(300f, screen.x, 0.01f)
        assertEquals(400f, screen.y, 0.01f)
    }

    @Test fun `zoom levels are 7 discrete values`() {
        assertEquals(7, CameraState.ZOOM_LEVELS.size)
        assertEquals(0.5f, CameraState.ZOOM_LEVELS.first())
        assertEquals(2.5f, CameraState.ZOOM_LEVELS.last())
    }

    @Test fun `reset returns to initial`() {
        val c = CameraState(zoom = 2f, offsetX = 100f, offsetY = 200f).reset()
        assertEquals(CameraState.Initial, c)
    }
}

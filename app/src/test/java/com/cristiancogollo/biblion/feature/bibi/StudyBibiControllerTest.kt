package com.cristiancogollo.biblion.feature.bibi

import com.cristiancogollo.biblion.feature.bibi.ui.StudyBibiController
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyBibiControllerTest {
    @Test
    fun `reopening Bibi keeps a single open window state`() {
        val controller = StudyBibiController()

        controller.open()
        controller.open()

        assertTrue(controller.isOpen)

        controller.close()

        assertFalse(controller.isOpen)
    }
}

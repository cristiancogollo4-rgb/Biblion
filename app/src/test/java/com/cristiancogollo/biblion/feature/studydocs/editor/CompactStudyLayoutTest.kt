package com.cristiancogollo.biblion.feature.studydocs.editor

import com.cristiancogollo.biblion.feature.studydocs.ui.editor.shouldUseCompactStudyLayout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactStudyLayoutTest {

    @Test
    fun `compact height uses one pane even with expanded width`() {
        assertTrue(shouldUseCompactStudyLayout(widthDp = 964, heightDp = 434))
    }

    @Test
    fun `compact width uses one pane even with enough height`() {
        assertTrue(shouldUseCompactStudyLayout(widthDp = 839, heightDp = 600))
    }

    @Test
    fun `expanded viewport keeps split layout`() {
        assertFalse(shouldUseCompactStudyLayout(widthDp = 840, heightDp = 480))
    }
}

package com.cristiancogollo.biblion.feature.studydocs.editor

import com.cristiancogollo.biblion.feature.studydocs.ui.editor.PageGapOffsetMapping
import org.junit.Assert.assertEquals
import org.junit.Test

class PageGapOffsetMappingTest {

    @Test
    fun `cursor at page boundary maps to start of next visual page`() {
        val mapping = PageGapOffsetMapping(
            sourceLength = 20,
            breakOffsets = listOf(10),
        )

        assertEquals(13, mapping.originalToTransformed(10))
        assertEquals(10, mapping.transformedToOriginal(10))
        assertEquals(10, mapping.transformedToOriginal(11))
        assertEquals(10, mapping.transformedToOriginal(13))
    }

    @Test
    fun `multiple page gaps preserve offsets after every boundary`() {
        val mapping = PageGapOffsetMapping(
            sourceLength = 30,
            breakOffsets = listOf(10, 20),
        )

        assertEquals(5, mapping.originalToTransformed(5))
        assertEquals(13, mapping.originalToTransformed(10))
        assertEquals(26, mapping.originalToTransformed(20))
        assertEquals(36, mapping.originalToTransformed(30))
        assertEquals(25, mapping.transformedToOriginal(31))
    }
}

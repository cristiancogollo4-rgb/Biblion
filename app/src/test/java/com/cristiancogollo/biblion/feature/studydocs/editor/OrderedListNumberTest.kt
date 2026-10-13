package com.cristiancogollo.biblion.feature.studydocs.editor

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.calculateOrderedListNumber
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderedListNumberTest {

    @Test
    fun numberingContinuesAfterEveryItemInPreviousListBlock() {
        val blocks = listOf(
            StudyBlock.OrderedList(items = listOf(StyledText("uno"), StyledText("dos"))),
            StudyBlock.OrderedList(items = listOf(StyledText("tres"))),
        )

        assertEquals(1, calculateOrderedListNumber(blocks, 0))
        assertEquals(3, calculateOrderedListNumber(blocks, 1))
    }

    @Test
    fun numberingRestartsAfterAnotherBlockType() {
        val blocks = listOf(
            StudyBlock.OrderedList(items = listOf(StyledText("uno"), StyledText("dos"))),
            StudyBlock.Paragraph(text = StyledText("separador")),
            StudyBlock.OrderedList(items = listOf(StyledText("nuevo"))),
        )

        assertEquals(1, calculateOrderedListNumber(blocks, 2))
    }
}

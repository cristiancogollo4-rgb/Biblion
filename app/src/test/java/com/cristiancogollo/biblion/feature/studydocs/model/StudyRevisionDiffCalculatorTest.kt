package com.cristiancogollo.biblion.feature.studydocs.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StudyRevisionDiffCalculatorTest {
    @Test
    fun `counts changed words and blocks`() {
        val firstBlock = StudyBlock.Paragraph(
            id = BlockId("same"),
            text = StyledText("La fe produce esperanza"),
        )
        val secondBlock = firstBlock.copy(text = StyledText("La fe produce vida"))
        val before = StudyDoc(blocks = listOf(firstBlock))
        val after = StudyDoc(blocks = listOf(secondBlock, StudyBlock.Paragraph(text = StyledText("Nuevo"))))

        val diff = StudyRevisionDiffCalculator.compare(before, after)

        assertEquals(1, diff.blocksAdded)
        assertEquals(1, diff.blocksModified)
        assertEquals(0, diff.blocksRemoved)
        assertEquals(2, diff.wordsAdded)
        assertEquals(1, diff.wordsRemoved)
    }
}

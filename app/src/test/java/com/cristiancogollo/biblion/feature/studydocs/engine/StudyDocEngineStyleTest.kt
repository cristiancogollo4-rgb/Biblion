package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.TextStylePatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyDocEngineStyleTest {

    @Test
    fun apply_style_adds_bold_range() {
        val id = BlockId("p1")
        val doc = StudyDoc(blocks = listOf(
            StudyBlock.Paragraph(id = id, text = StyledText.plain("Gracia y verdad"))
        ))

        val (updated, result) = StudyDocEngine.apply(
            doc,
            StudyOp.ApplyStyle(
                blockId = id,
                range = 0..5,
                patch = TextStylePatch(bold = true),
            ),
        )

        assertTrue(result.isSuccess)
        val text = (updated.blocks.single() as StudyBlock.Paragraph).text
        assertEquals(1, text.ranges.size)
        assertEquals(0, text.ranges.single().start)
        assertEquals(6, text.ranges.single().endExclusive)
        assertTrue(text.ranges.single().bold)
    }

    @Test
    fun apply_style_unknown_block_fails() {
        val doc = StudyDoc(blocks = listOf(
            StudyBlock.Paragraph(id = BlockId("a"), text = StyledText.plain("a"))
        ))

        val (_, result) = StudyDocEngine.apply(
            doc,
            StudyOp.ApplyStyle(
                blockId = BlockId("zzz"),
                range = 0..1,
                patch = TextStylePatch(bold = true),
            ),
        )

        assertTrue(result !is OpResult.Ok)
    }

    @Test
    fun clear_style_unknown_block_fails() {
        val doc = StudyDoc(blocks = listOf(
            StudyBlock.Paragraph(id = BlockId("a"), text = StyledText.plain("a"))
        ))

        val (_, result) = StudyDocEngine.apply(
            doc,
            StudyOp.ClearStyle(
                blockId = BlockId("zzz"),
                range = 0..1,
                kind = TextStyleKind.BOLD,
            ),
        )

        assertTrue(result !is OpResult.Ok)
    }
}

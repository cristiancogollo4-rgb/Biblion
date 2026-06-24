package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyDocEngineEditTest {

    @Test
    fun edit_text_replaces_full_text_in_paragraph() {
        val id = BlockId("p1")
        val block = StudyBlock.Paragraph(id = id, text = StyledText.plain("Hola"))
        val doc = StudyDoc(blocks = listOf(block))

        val (updated, result) = StudyDocEngine.apply(
            doc,
            StudyOp.EditText(blockId = id, newText = StyledText.plain("Biblion")),
        )

        assertTrue(result.isSuccess)
        assertEquals("Biblion", (updated.blocks.single() as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun edit_text_in_heading_replaces_text_preserves_level() {
        val id = BlockId("h1")
        val heading = StudyBlock.Heading(id = id, level = 2, text = StyledText.plain("Viejo"))
        val doc = StudyDoc(blocks = listOf(heading))

        val (updated, _) = StudyDocEngine.apply(
            doc,
            StudyOp.EditText(blockId = id, newText = StyledText.plain("Nuevo")),
        )

        val h = updated.blocks.single() as StudyBlock.Heading
        assertEquals(2, h.level)
        assertEquals("Nuevo", h.text.plain())
    }

    @Test
    fun edit_text_on_unknown_block_fails() {
        val doc = StudyDoc(blocks = listOf(StudyBlock.Paragraph(id = BlockId("a"), text = StyledText.plain("a"))))

        val (unchanged, result) = StudyDocEngine.apply(
            doc,
            StudyOp.EditText(blockId = BlockId("missing"), newText = StyledText.plain("x")),
        )

        assertTrue(result !is OpResult.Ok)
        assertEquals(doc, unchanged)
    }

    @Test
    fun delete_block_removes_from_doc() {
        val a = StudyBlock.Paragraph(id = BlockId("a"), text = StyledText.plain("A"))
        val b = StudyBlock.Paragraph(id = BlockId("b"), text = StyledText.plain("B"))
        val doc = StudyDoc(blocks = listOf(a, b))

        val (updated, result) = StudyDocEngine.apply(
            doc,
            StudyOp.DeleteBlock(BlockId("a")),
        )

        assertTrue(result.isSuccess)
        assertEquals(1, updated.blocks.size)
        assertEquals("B", (updated.blocks.single() as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun move_block_reorders_blocks() {
        val a = StudyBlock.Paragraph(id = BlockId("a"), text = StyledText.plain("A"))
        val b = StudyBlock.Paragraph(id = BlockId("b"), text = StyledText.plain("B"))
        val c = StudyBlock.Paragraph(id = BlockId("c"), text = StyledText.plain("C"))
        val doc = StudyDoc(blocks = listOf(a, b, c))

        val (updated, result) = StudyDocEngine.apply(
            doc,
            StudyOp.MoveBlock(blockId = BlockId("a"), toIndex = 2),
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf("B", "C", "A"), updated.blocks.map { (it as StudyBlock.Paragraph).text.plain() })
    }
}

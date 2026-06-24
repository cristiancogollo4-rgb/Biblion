package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyDocEngineInsertTest {

    private val initial = StudyDoc.empty()

    @Test
    fun insert_block_at_beginning_pushes_existing_blocks_down() {
        val newBlock = StudyBlock.Paragraph(text = StyledText.plain("nuevo"))
        val (doc, result) = StudyDocEngine.apply(
            initial.copy(blocks = listOf(StudyBlock.Paragraph(text = StyledText.plain("viejo")))),
            StudyOp.InsertBlock(atIndex = 0, block = newBlock),
        )

        assertTrue(result.isSuccess)
        assertEquals(2, doc.blocks.size)
        assertEquals("nuevo", (doc.blocks[0] as StudyBlock.Paragraph).text.plain())
        assertEquals("viejo", (doc.blocks[1] as StudyBlock.Paragraph).text.plain())
        assertEquals(2, doc.version)
    }

    @Test
    fun insert_block_in_middle_keeps_order() {
        val a = StudyBlock.Paragraph(id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId("a"), text = StyledText.plain("A"))
        val c = StudyBlock.Paragraph(id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId("c"), text = StyledText.plain("C"))
        val b = StudyBlock.Paragraph(id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId("b"), text = StyledText.plain("B"))

        val (doc, result) = StudyDocEngine.apply(
            initial.copy(blocks = listOf(a, c)),
            StudyOp.InsertBlock(atIndex = 1, block = b),
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf("A", "B", "C"), doc.blocks.map { (it as StudyBlock.Paragraph).text.plain() })
    }

    @Test
    fun insert_block_at_end_appends() {
        val a = StudyBlock.Paragraph(id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId("a"), text = StyledText.plain("A"))
        val b = StudyBlock.Paragraph(id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId("b"), text = StyledText.plain("B"))

        val (doc, _) = StudyDocEngine.apply(
            initial.copy(blocks = listOf(a)),
            StudyOp.InsertBlock(atIndex = 5, block = b),
        )

        assertEquals(2, doc.blocks.size)
        assertEquals("A", (doc.blocks[0] as StudyBlock.Paragraph).text.plain())
        assertEquals("B", (doc.blocks[1] as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun insert_block_with_duplicate_id_fails() {
        val id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId("dup")
        val block = StudyBlock.Paragraph(id = id, text = StyledText.plain("hola"))
        val (doc, result) = StudyDocEngine.apply(
            initial.copy(blocks = listOf(block)),
            StudyOp.InsertBlock(atIndex = 0, block = block),
        )

        assertTrue(result !is OpResult.Ok)
        assertEquals(1, doc.blocks.size)
    }

    @Test
    fun insert_block_bumps_doc_version_and_updated_at() {
        val t0 = 1000L
        val (doc, _) = StudyDocEngine.apply(
            StudyDoc(createdAt = t0, updatedAt = t0, version = 5),
            StudyOp.InsertBlock(atIndex = 0, block = StudyBlock.Divider()),
        )

        assertEquals(6, doc.version)
        assertTrue(doc.updatedAt >= t0)
    }
}


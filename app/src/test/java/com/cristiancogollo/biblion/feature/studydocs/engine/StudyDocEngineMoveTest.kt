package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.VerseRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyDocEngineMoveTest {

    @Test
    fun move_block_to_same_index_is_noop() {
        val a = StudyBlock.Paragraph(id = BlockId("a"), text = StyledText.plain("A"))
        val doc = StudyDoc(blocks = listOf(a), version = 3)

        val (updated, result) = StudyDocEngine.apply(
            doc,
            StudyOp.MoveBlock(blockId = BlockId("a"), toIndex = 0),
        )

        assertTrue(result.isSuccess)
        assertEquals(3, updated.version)
        assertEquals(listOf("A"), updated.blocks.map { (it as StudyBlock.Paragraph).text.plain() })
    }

    @Test
    fun move_block_up_swaps_neighbors() {
        val a = StudyBlock.Paragraph(id = BlockId("a"), text = StyledText.plain("A"))
        val b = StudyBlock.Paragraph(id = BlockId("b"), text = StyledText.plain("B"))
        val c = StudyBlock.Paragraph(id = BlockId("c"), text = StyledText.plain("C"))
        val doc = StudyDoc(blocks = listOf(a, b, c))

        val (updated, _) = StudyDocEngine.apply(
            doc,
            StudyOp.MoveBlock(blockId = BlockId("c"), toIndex = 0),
        )

        assertEquals(listOf("C", "A", "B"), updated.blocks.map { (it as StudyBlock.Paragraph).text.plain() })
    }

    @Test
    fun move_unknown_block_fails_without_changing_doc() {
        val a = StudyBlock.Paragraph(id = BlockId("a"), text = StyledText.plain("A"))
        val doc = StudyDoc(blocks = listOf(a), version = 7)

        val (unchanged, result) = StudyDocEngine.apply(
            doc,
            StudyOp.MoveBlock(blockId = BlockId("missing"), toIndex = 0),
        )

        assertTrue(result !is OpResult.Ok)
        assertEquals(7, unchanged.version)
        assertEquals(1, unchanged.blocks.size)
    }

    @Test
    fun replace_block_with_same_id_swaps_content() {
        val id = BlockId("p1")
        val original = StudyBlock.Paragraph(id = id, text = StyledText.plain("viejo"))
        val replacement = StudyBlock.Paragraph(id = id, text = StyledText.plain("nuevo"))
        val doc = StudyDoc(blocks = listOf(original))

        val (updated, result) = StudyDocEngine.apply(
            doc,
            StudyOp.ReplaceBlock(blockId = id, newBlock = replacement),
        )

        assertTrue(result.isSuccess)
        assertEquals("nuevo", (updated.blocks.single() as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun replace_block_with_different_id_fails() {
        val id = BlockId("p1")
        val original = StudyBlock.Paragraph(id = id, text = StyledText.plain("viejo"))
        val replacement = StudyBlock.Paragraph(id = BlockId("p2"), text = StyledText.plain("nuevo"))
        val doc = StudyDoc(blocks = listOf(original))

        val (unchanged, result) = StudyDocEngine.apply(
            doc,
            StudyOp.ReplaceBlock(blockId = id, newBlock = replacement),
        )

        assertTrue(result !is OpResult.Ok)
        assertEquals(doc, unchanged)
    }
}



package com.cristiancogollo.biblion.feature.studydocs.pagination

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.compositionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PageFragmentCompositionKeyTest {

    @Test
    fun paragraph_key_does_not_change_when_text_or_slice_end_changes() {
        val blockId = BlockId("paragraph")
        val before = PageFragment.ParagraphSlice(
            originBlockId = blockId,
            block = StudyBlock.Paragraph(
                id = blockId,
                text = StyledText("a"),
            ),
            charStart = 0,
            charEndExclusive = 1,
        )
        val after = PageFragment.ParagraphSlice(
            originBlockId = blockId,
            block = StudyBlock.Paragraph(
                id = blockId,
                text = StyledText("abc"),
            ),
            charStart = 0,
            charEndExclusive = 3,
        )

        assertEquals(before.compositionKey(), after.compositionKey())
    }

    @Test
    fun list_items_have_different_composition_keys() {
        val blockId = BlockId("list")
        val block = StudyBlock.BulletList(
            id = blockId,
            items = listOf(StyledText("uno"), StyledText("dos")),
        )

        val first = PageFragment.ListItemSlice(blockId, block, 0, 0, 3)
        val second = PageFragment.ListItemSlice(blockId, block, 1, 0, 3)

        assertNotEquals(first.compositionKey(), second.compositionKey())
    }

    @Test
    fun continuation_slices_keep_independent_composition_keys() {
        val blockId = BlockId("long-paragraph")
        val block = StudyBlock.Paragraph(
            id = blockId,
            text = StyledText("texto largo"),
        )

        val owner = PageFragment.ParagraphSlice(
            blockId,
            block,
            0,
            5,
            sliceIndex = 0,
        )
        val continuation = PageFragment.ParagraphSlice(
            blockId,
            block,
            5,
            11,
            sliceIndex = 1,
        )

        assertNotEquals(owner.compositionKey(), continuation.compositionKey())
    }
}

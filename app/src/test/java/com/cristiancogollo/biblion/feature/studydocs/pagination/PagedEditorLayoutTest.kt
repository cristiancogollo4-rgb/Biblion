package com.cristiancogollo.biblion.feature.studydocs.pagination

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.Page
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.buildPagedEditorUnits
import org.junit.Assert.assertEquals
import org.junit.Test

class PagedEditorLayoutTest {

    @Test
    fun `paragraph spanning pages becomes one editor with an internal gap`() {
        val id = BlockId("paragraph")
        val block = StudyBlock.Paragraph(id = id, text = StyledText("abcdefghij"))
        val pages = listOf(
            Page(
                index = 0,
                fragments = listOf(
                    PageFragment.ParagraphSlice(
                        id,
                        block,
                        charStart = 0,
                        charEndExclusive = 5,
                        sliceIndex = 0,
                        topPx = 20f,
                        heightPx = 80f,
                    ),
                ),
            ),
            Page(
                index = 1,
                fragments = listOf(
                    PageFragment.ParagraphSlice(
                        id,
                        block,
                        charStart = 5,
                        charEndExclusive = 10,
                        sliceIndex = 1,
                        topPx = 0f,
                        heightPx = 60f,
                    ),
                ),
            ),
        )

        val unit = buildPagedEditorUnits(
            pages = pages,
            pageHeightPx = 500f,
            pageGapPx = 20f,
            pageMarginPx = 40f,
            canvasVerticalPaddingPx = 10f,
        ).single()

        assertEquals(70f, unit.topPx)
        assertEquals(560f, unit.heightPx)
        assertEquals(1, unit.gaps.size)
        assertEquals(5, unit.gaps.single().offset)
        assertEquals(80f, unit.gaps.single().startPx)
        assertEquals(420f, unit.gaps.single().heightPx)
    }
}

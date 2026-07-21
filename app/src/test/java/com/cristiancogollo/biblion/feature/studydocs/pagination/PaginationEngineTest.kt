package com.cristiancogollo.biblion.feature.studydocs.pagination

import android.content.Context
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PaginationEngineTest {

    private lateinit var textMeasurer: TextMeasurer
    private val density = Density(1f)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = androidx.compose.ui.text.font.createFontFamilyResolver(context)
        textMeasurer = TextMeasurer(
            defaultFontFamilyResolver = resolver,
            defaultDensity = density,
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
    }

    @Test
    fun empty_blocks_returns_one_empty_page() {
        val pages = PaginationEngine.paginate(
            blocks = emptyList(),
            pageWidthPx = 658f,
            pageHeightPx = 200f,
            textMeasurer = textMeasurer,
            density = density,
        )
        assertEquals(1, pages.size)
        assertTrue(pages[0].fragments.isEmpty())
    }

    @Test
    fun single_short_paragraph_returns_one_page_one_slice() {
        val pages = PaginationEngine.paginate(
            blocks = listOf(shortParagraph()),
            pageWidthPx = 658f,
            pageHeightPx = 200f,
            textMeasurer = textMeasurer,
            density = density,
        )
        assertEquals(1, pages.size)
        assertEquals(1, pages[0].fragments.size)
        val slice = pages[0].fragments[0] as PageFragment.ParagraphSlice
        assertEquals(0, slice.charStart)
        assertEquals("Hola mundo".length, slice.charEndExclusive)
    }

    @Test
    fun long_paragraph_invokes_paginate_and_preserves_char_contiguity() {
        val longText = ("Lorem ipsum dolor sit amet. ").repeat(100)
        val pages = PaginationEngine.paginate(
            blocks = listOf(
                StudyBlock.Paragraph(
                    id = BlockId("p1"),
                    text = StyledText(raw = longText),
                    fontSize = DocConfig.DEFAULT_FONT_SIZE,
                )
            ),
            pageWidthPx = 658f,
            pageHeightPx = 0.1f,
            textMeasurer = textMeasurer,
            density = density,
        )
        assertTrue("Debe generar al menos 1 pagina", pages.isNotEmpty())
        val paragraphSlices = pages.flatMap { it.fragments }
            .filterIsInstance<PageFragment.ParagraphSlice>()
        var cursor = 0
        for (slice in paragraphSlices) {
            assertEquals("Gap/overlap en offset $cursor: esperado start=$cursor, real=${slice.charStart}", cursor, slice.charStart)
            cursor = slice.charEndExclusive
        }
        assertEquals("El ultimo slice no llega al final del texto", longText.length, cursor)
    }

    @Test
    fun bullet_list_items_are_split_independently() {
        val longItem = "Texto de vineta muy largo ".repeat(50)
        val pages = PaginationEngine.paginate(
            blocks = listOf(
                StudyBlock.BulletList(
                    id = BlockId("b1"),
                    items = listOf(
                        StyledText(raw = "Item corto"),
                        StyledText(raw = longItem),
                    ),
                    fontSize = DocConfig.DEFAULT_FONT_SIZE,
                )
            ),
            pageWidthPx = 658f,
            pageHeightPx = 200f,
            textMeasurer = textMeasurer,
            density = density,
        )
        val bulletSlices = pages.flatMap { it.fragments }
            .filterIsInstance<PageFragment.ListItemSlice>()
        assertTrue("Debe haber slices de ambos items", bulletSlices.size >= 2)
        val itemIndices = bulletSlices.map { it.itemIndex }.distinct()
        assertTrue("Debe cubrir ambos items", itemIndices.contains(0) && itemIndices.contains(1))
    }

    @Test
    fun ordered_list_items_are_split_independently() {
        val longItem = "Texto de item numerado largo ".repeat(50)
        val pages = PaginationEngine.paginate(
            blocks = listOf(
                StudyBlock.OrderedList(
                    id = BlockId("o1"),
                    items = listOf(
                        StyledText(raw = "Primero"),
                        StyledText(raw = longItem),
                    ),
                    fontSize = DocConfig.DEFAULT_FONT_SIZE,
                )
            ),
            pageWidthPx = 658f,
            pageHeightPx = 200f,
            textMeasurer = textMeasurer,
            density = density,
        )
        val orderedSlices = pages.flatMap { it.fragments }
            .filterIsInstance<PageFragment.OrderedListItemSlice>()
        assertTrue("Debe haber slices de ambos items", orderedSlices.size >= 2)
        val itemIndices = orderedSlices.map { it.itemIndex }.distinct()
        assertTrue("Debe cubrir ambos items", itemIndices.contains(0) && itemIndices.contains(1))
    }

    @Test
    fun verse_block_preserves_char_contiguity() {
        val longVerse = "En el principio creo Dios los cielos y la tierra. ".repeat(30)
        val pages = PaginationEngine.paginate(
            blocks = listOf(
                StudyBlock.Verse(
                    id = BlockId("v1"),
                    bookId = "Genesis",
                    chapter = 1,
                    verseStart = 1,
                    verseEnd = 3,
                    sourceVersion = "RVR1960",
                    contents = mapOf("RVR1960" to longVerse),
                )
            ),
            pageWidthPx = 658f,
            pageHeightPx = 0.1f,
            textMeasurer = textMeasurer,
            density = density,
        )
        assertTrue("Debe generar al menos 1 pagina", pages.isNotEmpty())
        val verseSlices = pages.flatMap { it.fragments }
            .filterIsInstance<PageFragment.VerseSlice>()
        var cursor = 0
        for (slice in verseSlices) {
            assertEquals(cursor, slice.charStart)
            cursor = slice.charEndExclusive
        }
        assertEquals(longVerse.length, cursor)
    }

    @Test
    fun quote_block_preserves_char_contiguity() {
        val longQuote = "Cita de prueba que es muy larga. ".repeat(30)
        val pages = PaginationEngine.paginate(
            blocks = listOf(
                StudyBlock.Quote(
                    id = BlockId("q1"),
                    text = StyledText(raw = longQuote),
                    attribution = "Autor X",
                )
            ),
            pageWidthPx = 658f,
            pageHeightPx = 0.1f,
            textMeasurer = textMeasurer,
            density = density,
        )
        assertTrue("Debe generar al menos 1 pagina", pages.isNotEmpty())
        val quoteSlices = pages.flatMap { it.fragments }
            .filterIsInstance<PageFragment.QuoteSlice>()
        var cursor = 0
        for (slice in quoteSlices) {
            assertEquals(cursor, slice.charStart)
            cursor = slice.charEndExclusive
        }
        assertEquals(longQuote.length, cursor)
    }

    @Test
    fun multiple_block_types_fill_pages_sequentially() {
        val pages = PaginationEngine.paginate(
            blocks = listOf(
                shortParagraph(),
                StudyBlock.Heading(
                    id = BlockId("h1"),
                    level = 1,
                    text = StyledText(raw = "Titulo de prueba"),
                    fontSize = DocConfig.HEADING1_SIZE,
                ),
                StudyBlock.Quote(
                    id = BlockId("q2"),
                    text = StyledText(raw = "Una cita corta"),
                    attribution = "Autor Y",
                ),
            ),
            pageWidthPx = 658f,
            pageHeightPx = 2000f,
            textMeasurer = textMeasurer,
            density = density,
        )
        assertTrue("Debe caber todo en 1 pagina con pageHeight grande", pages.size == 1)
        val frags = pages[0].fragments
        assertEquals(3, frags.size)
        assertTrue(frags[0] is PageFragment.ParagraphSlice)
        assertTrue(frags[1] is PageFragment.HeadingSlice)
        assertTrue(frags[2] is PageFragment.QuoteSlice)
    }

    private fun shortParagraph(): StudyBlock.Paragraph = StudyBlock.Paragraph(
        id = BlockId("p_short"),
        text = StyledText(raw = "Hola mundo"),
        fontSize = DocConfig.DEFAULT_FONT_SIZE,
    )
}

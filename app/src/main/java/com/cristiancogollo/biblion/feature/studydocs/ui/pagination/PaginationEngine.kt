package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

/**
 * Motor de paginacion puro: a partir de una secuencia de [StudyBlock] y una
 * superficie de hoja (ancho y alto util en pixeles), produce una lista de [Page].
 *
 * El algoritmo recorre los bloques en orden y mide cada uno con [TextMeasurer].
 * Para bloques divisibles (parrafo, heading, item, quote, versiculo) parte por linea.
 * Para bloques indivisibles (Divider, PageBreak, Callout, Note, Reflection, Table)
 * los mantiene enteros: si no caben en la pagina actual, saltan a la siguiente.
 *
 * El motor respeta los invariantes:
 *  I1: union de charStart..charEndExclusive de los *Slice de un bloque == 0..text.length.
 *  I2: cada *Slice vive en exactamente una pagina.
 *  I3: ninguna linea se parte por la mitad entre paginas.
 *  I4: si un bloque cabe entero en el espacio restante, emite UN solo slice.
 *  I5: si ninguna linea cabe en pagina vacia, se fuerza (progreso garantizado).
 */
object PaginationEngine {

    fun paginate(
        blocks: List<StudyBlock>,
        pageWidthPx: Float,
        pageHeightPx: Float,
        textMeasurer: TextMeasurer,
        density: Density = Density(1f),
    ): List<Page> {
        if (blocks.isEmpty() || pageWidthPx <= 0f || pageHeightPx <= 0f) {
            return listOf(Page(0, emptyList()))
        }

        val pages = mutableListOf<MutableList<PageFragment>>()
        var currentFragments = mutableListOf<PageFragment>()
        var cursorY = 0f

        fun openPage() {
            if (currentFragments.isNotEmpty()) {
                pages.add(currentFragments)
            }
            currentFragments = mutableListOf()
            cursorY = 0f
        }

        for (block in blocks) {
            val style = textStyleFor(block, density)
            when (block) {
                is StudyBlock.Paragraph -> {
                    cursorY = placePartible(
                        text = block.text.raw,
                        sliceFactory = { start, end ->
                            PageFragment.ParagraphSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                            )
                        },
                        pageWidthPx = pageWidthPx,
                        pageHeightPx = pageHeightPx,
                        cursorYStart = cursorY,
                        density = density,
                        textMeasurer = textMeasurer,
                        style = style,
                        onSliceEmitted = { f -> currentFragments.add(f) },
                        onPageFilled = { openPage() },
                    )
                }
                is StudyBlock.Heading -> {
                    cursorY = placePartible(
                        text = block.text.raw,
                        sliceFactory = { start, end ->
                            PageFragment.HeadingSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                            )
                        },
                        pageWidthPx = pageWidthPx,
                        pageHeightPx = pageHeightPx,
                        cursorYStart = cursorY,
                        density = density,
                        textMeasurer = textMeasurer,
                        style = style,
                        onSliceEmitted = { f -> currentFragments.add(f) },
                        onPageFilled = { openPage() },
                    )
                }
                is StudyBlock.BulletList -> {
                    block.items.forEachIndexed { index, item ->
                        if (item.raw.isEmpty()) return@forEachIndexed
                        cursorY = placePartible(
                            text = item.raw,
                            sliceFactory = { start, end ->
                                PageFragment.ListItemSlice(
                                    originBlockId = block.id,
                                    block = block,
                                    itemIndex = index,
                                    charStart = start,
                                    charEndExclusive = end,
                                )
                            },
                            pageWidthPx = pageWidthPx,
                            pageHeightPx = pageHeightPx,
                            cursorYStart = cursorY,
                            density = density,
                            textMeasurer = textMeasurer,
                            style = style,
                            onSliceEmitted = { f -> currentFragments.add(f) },
                            onPageFilled = { openPage() },
                        )
                    }
                }
                is StudyBlock.OrderedList -> {
                    block.items.forEachIndexed { index, item ->
                        if (item.raw.isEmpty()) return@forEachIndexed
                        cursorY = placePartible(
                            text = item.raw,
                            sliceFactory = { start, end ->
                                PageFragment.OrderedListItemSlice(
                                    originBlockId = block.id,
                                    block = block,
                                    itemIndex = index,
                                    charStart = start,
                                    charEndExclusive = end,
                                )
                            },
                            pageWidthPx = pageWidthPx,
                            pageHeightPx = pageHeightPx,
                            cursorYStart = cursorY,
                            density = density,
                            textMeasurer = textMeasurer,
                            style = style,
                            onSliceEmitted = { f -> currentFragments.add(f) },
                            onPageFilled = { openPage() },
                        )
                    }
                }
                is StudyBlock.Verse -> {
                    val verseText = block.contents[block.sourceVersion] ?: ""
                    cursorY = placePartible(
                        text = verseText,
                        sliceFactory = { start, end ->
                            PageFragment.VerseSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                            )
                        },
                        pageWidthPx = pageWidthPx,
                        pageHeightPx = pageHeightPx,
                        cursorYStart = cursorY,
                        density = density,
                        textMeasurer = textMeasurer,
                        style = style,
                        onSliceEmitted = { f -> currentFragments.add(f) },
                        onPageFilled = { openPage() },
                    )
                }
                is StudyBlock.Quote -> {
                    cursorY = placePartible(
                        text = block.text.raw,
                        sliceFactory = { start, end ->
                            PageFragment.QuoteSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                            )
                        },
                        pageWidthPx = pageWidthPx,
                        pageHeightPx = pageHeightPx,
                        cursorYStart = cursorY,
                        density = density,
                        textMeasurer = textMeasurer,
                        style = style,
                        onSliceEmitted = { f -> currentFragments.add(f) },
                        onPageFilled = { openPage() },
                    )
                }
            }
        }

        if (currentFragments.isNotEmpty()) {
            pages.add(currentFragments)
        }
        if (pages.isEmpty()) {
            pages.add(mutableListOf())
        }
        return pages.mapIndexed { idx, frags -> Page(idx, frags) }
    }

    /**
     * Coloca un texto divisible en las paginas, emitiendo slices contiguos por pagina.
     * Mantiene los invariantes I1-I5.
     */
    private fun placePartible(
        text: String,
        sliceFactory: (start: Int, endExclusive: Int) -> PageFragment,
        pageWidthPx: Float,
        pageHeightPx: Float,
        cursorYStart: Float,
        density: Density,
        textMeasurer: TextMeasurer,
        style: TextStyle,
        onSliceEmitted: (PageFragment) -> Unit,
        onPageFilled: () -> Unit,
    ): Float {
        if (text.isEmpty()) {
            onSliceEmitted(sliceFactory(0, 0))
            return cursorYStart
        }
        val layout = textMeasurer.measure(
            text = text,
            style = style,
            constraints = Constraints(maxWidth = pageWidthPx.toInt()),
            density = density,
        )
        val total = text.length
        var cursorY = cursorYStart
        var blockOffset = 0
        var pageSliceStart = 0
        var guard = 0
        while (blockOffset < total) {
            val lineIndex = layout.getLineForOffset(blockOffset)
            val lineEnd = layout.getLineEnd(lineIndex, true).coerceAtMost(total)
            if (lineEnd <= blockOffset) break
            val lineHeight = (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex))
                .coerceAtLeast(1f)

            if (cursorY + lineHeight > pageHeightPx && cursorY > 0f) {
                if (blockOffset > pageSliceStart) {
                    onSliceEmitted(sliceFactory(pageSliceStart, blockOffset))
                }
                onPageFilled()
                cursorY = 0f
                pageSliceStart = blockOffset
                continue
            }
            cursorY += lineHeight
            blockOffset = lineEnd
            if (++guard > 100_000) break
        }
        if (total > pageSliceStart) {
            onSliceEmitted(sliceFactory(pageSliceStart, total))
        }
        return cursorY
    }

    private fun textStyleFor(block: StudyBlock, density: Density): TextStyle {
        val fontSizeSp = when (block) {
            is StudyBlock.Heading -> when (block.level) {
                1 -> DocConfig.HEADING1_SIZE
                2 -> DocConfig.HEADING2_SIZE
                3 -> DocConfig.HEADING3_SIZE
                else -> block.fontSize
            }
            else -> block.fontSize
        }.coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
        val fontFamily = if (block is StudyBlock.Verse || block.fontFamily == "serif") {
            FontFamily.Serif
        } else {
            FontFamily.Default
        }
        val fontWeight = if (block is StudyBlock.Heading) FontWeight.Bold else FontWeight.Normal
        val fontStyle = if (block is StudyBlock.Quote) FontStyle.Italic else FontStyle.Normal
        val lineHeightSp = fontSizeSp * DocConfig.LINE_HEIGHT
        return TextStyle(
            fontSize = with(density) { fontSizeSp.sp },
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textAlign = block.alignment.toTextAlign(),
            lineHeight = with(density) { lineHeightSp.sp },
        )
    }
}

private fun BlockAlignment.toTextAlign(): TextAlign = when (this) {
    BlockAlignment.Start -> TextAlign.Start
    BlockAlignment.Center -> TextAlign.Center
    BlockAlignment.End -> TextAlign.End
    BlockAlignment.Justify -> TextAlign.Justify
}

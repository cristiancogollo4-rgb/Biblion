package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import android.util.Log
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

/**
 * Motor de paginacion puro: a partir de una secuencia de [StudyBlock] y una
 * superficie de hoja (ancho y alto util en pixeles), produce una lista de [Page].
 *
 * El algoritmo recorre los bloques en orden y mide cada uno con [TextMeasurer].
 * Para bloques divisibles (parrafo, heading, item de lista) parte por linea.
 * Para bloques indivisibles (Quote, Table, Verse, Divider, PageBreak, etc.) los
 * mantiene enteros: si no caben en la pagina actual, saltan a la siguiente.
 *
 * El motor mide en una densidad fija (la del "papel" fisico, no la del dispositivo),
 * por lo que el zoom visual posterior (via LocalDensity) no recalcula paginas.
 *
 * Texto medido: usa [TextMeasurer.measure] con el ancho util de la pagina para
 * obtener un [TextLayoutResult] del que extrae las lineas con
 * [TextLayoutResult.getLineForOffset], [TextLayoutResult.getLineTop],
 * [TextLayoutResult.getLineBottom].
 */
object PaginationEngine {

    private val defaultTextStyle: TextStyle = TextStyle.Default

    /**
     * Pagina la lista de bloques. El resultado es siempre >= 1 pagina, incluso
     * si la lista de bloques esta vacia (se devuelve una pagina vacia).
     */
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
        var currentPage = mutableListOf<PageFragment>()
        var cursorY = 0f

        fun openPage() {
            if (currentPage.isNotEmpty()) {
                pages.add(currentPage)
            }
            currentPage = mutableListOf()
            cursorY = 0f
        }

        fun remainingHeight() = pageHeightPx - cursorY

        for (block in blocks) {
            val textPreview = block.toStyledTextList().firstOrNull()?.raw?.take(40) ?: "<empty>"
            Log.d("BIBLION_STUDY", "PaginationEngine block type=${block::class.simpleName} textPreview=[$textPreview] id=${block.id}")
            when (block) {
                is StudyBlock.Paragraph -> {
                    cursorY = paginatePartible(
                        text = block.text.raw,
                        blockId = block.id,
                        sliceFactory = { start, end ->
                            PageFragment.ParagraphSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                            )
                        },
                        pageWidthPx = pageWidthPx,
                        remainingHeight = remainingHeight(),
                        density = density,
                        textMeasurer = textMeasurer,
                        onSliceEmitted = { fragment -> currentPage.add(fragment) },
                        onPageFilled = { openPage() },
                    )
                }
                is StudyBlock.Heading -> {
                    cursorY = paginatePartible(
                        text = block.text.raw,
                        blockId = block.id,
                        sliceFactory = { start, end ->
                            PageFragment.HeadingSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                            )
                        },
                        pageWidthPx = pageWidthPx,
                        remainingHeight = remainingHeight(),
                        density = density,
                        textMeasurer = textMeasurer,
                        onSliceEmitted = { fragment -> currentPage.add(fragment) },
                        onPageFilled = { openPage() },
                    )
                }
                is StudyBlock.BulletList -> {
                    block.items.forEachIndexed { index, item ->
                        if (item.raw.isEmpty()) return@forEachIndexed
                        cursorY = paginatePartible(
                            text = item.raw,
                            blockId = block.id,
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
                            remainingHeight = remainingHeight(),
                            density = density,
                            textMeasurer = textMeasurer,
                            onSliceEmitted = { fragment -> currentPage.add(fragment) },
                            onPageFilled = { openPage() },
                        )
                    }
                }
                is StudyBlock.OrderedList -> {
                    block.items.forEachIndexed { index, item ->
                        if (item.raw.isEmpty()) return@forEachIndexed
                        cursorY = paginatePartible(
                            text = item.raw,
                            blockId = block.id,
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
                            remainingHeight = remainingHeight(),
                            density = density,
                            textMeasurer = textMeasurer,
                            onSliceEmitted = { fragment -> currentPage.add(fragment) },
                            onPageFilled = { openPage() },
                        )
                    }
                }
                is StudyBlock.Verse -> {
                    val verseText = block.contents[block.sourceVersion] ?: ""
                    cursorY = paginatePartible(
                        text = verseText,
                        blockId = block.id,
                        sliceFactory = { start, end ->
                            PageFragment.VerseSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                            )
                        },
                        pageWidthPx = pageWidthPx,
                        remainingHeight = remainingHeight(),
                        density = density,
                        textMeasurer = textMeasurer,
                        onSliceEmitted = { fragment -> currentPage.add(fragment) },
                        onPageFilled = { openPage() },
                    )
                }
                is StudyBlock.Quote -> {
                    if (cursorY >= pageHeightPx && currentPage.isNotEmpty()) {
                        openPage()
                    }
                    currentPage.add(PageFragment.Whole(originBlockId = block.id, block = block))
                    cursorY = pageHeightPx
                    openPage()
                }
            }
        }

        if (currentPage.isNotEmpty()) {
            pages.add(currentPage)
        }
        if (pages.isEmpty()) pages.add(mutableListOf<PageFragment>().also { it -> })
        Log.d("BIBLION_STUDY", "paginate END pages=${pages.size} fragments=${pages.sumOf { it.size }}")
        return pages.mapIndexed { idx, frags -> Page(idx, frags) }
    }

    /**
     * Pagina un texto divisible (parrafo, heading, item) midiendo cada linea con [TextMeasurer]
     * y partiendo en los saltos de pagina segun [remainingHeight]. Retorna el [cursorY]
     * actualizado al final del texto procesado.
     */
    private fun paginatePartible(
        text: String,
        blockId: com.cristiancogollo.biblion.feature.studydocs.model.BlockId,
        sliceFactory: (start: Int, endExclusive: Int) -> PageFragment,
        pageWidthPx: Float,
        remainingHeight: Float,
        density: Density,
        textMeasurer: TextMeasurer,
        onSliceEmitted: (PageFragment) -> Unit,
        onPageFilled: () -> Unit,
    ): Float {
        Log.d("BIBLION_STUDY", "paginatePartible start text.length=${text.length} isEmpty=${text.isEmpty()}")
        if (text.isEmpty()) {
            Log.d("BIBLION_STUDY", "paginatePartible EMPTY -> emitting slice(0,0)")
            onSliceEmitted(sliceFactory(0, 0))
            return 0f
        }

        val result = with(density) {
            textMeasurer.measure(
                text = text,
                style = defaultTextStyle,
                constraints = Constraints(maxWidth = pageWidthPx.toInt()),
            )
        }

        val totalLength = result.layoutInput.text.length
        if (totalLength == 0) return 0f

        var currentStart = 0
        var sliceStart = 0
        var cursorY = 0f
        val pageHeight = remainingHeight

        while (currentStart < totalLength) {
            val endLine = result.getLineForOffset(currentStart)
            val lineEndOffset = result.getLineEnd(endLine, false)
            if (lineEndOffset <= currentStart) break
            val lineTop = result.getLineTop(endLine)
            val lineBottom = result.getLineBottom(endLine)
            val lineHeightPx = (lineBottom - lineTop).coerceAtLeast(1f)

            if (cursorY + lineHeightPx > pageHeight) {
                if (currentStart > sliceStart) {
                    onSliceEmitted(sliceFactory(sliceStart, currentStart))
                }
                onPageFilled()
                cursorY = 0f
                sliceStart = currentStart
                continue
            }

            cursorY += lineHeightPx
            currentStart = lineEndOffset
        }

        if (sliceStart < totalLength) {
            onSliceEmitted(sliceFactory(sliceStart, totalLength))
        }

        return cursorY
    }
}

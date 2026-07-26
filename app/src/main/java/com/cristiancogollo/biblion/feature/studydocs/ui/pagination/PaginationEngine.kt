package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.debug.StudyEditorDebugLog
import com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.toAnnotatedString

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

        for ((index, block) in blocks.withIndex()) {
            // KEEP-WITH-NEXT: si es Heading y el heading + el siguiente bloque
            // no caben juntos en la pagina actual, empujamos el heading a la
            // siguiente pagina para evitar encabezados huerfanos.
            if (block is StudyBlock.Heading) {
                val nextBlock = blocks.getOrNull(index + 1)
                if (nextBlock != null && cursorY > 0f) {
                    val headingHeight = estimateBlockHeight(
                        block, pageWidthPx, density, textMeasurer,
                    )
                    val nextHeight = estimateBlockHeight(
                        nextBlock, pageWidthPx, density, textMeasurer,
                    )
                    if (headingHeight > 0f && nextHeight > 0f &&
                        cursorY + headingHeight + nextHeight > pageHeightPx
                    ) {
                        openPage()
                    }
                }
            }

            val style = textStyleFor(block, density)
            when (block) {
                is StudyBlock.Paragraph -> {
                    var sliceIndex = 0
                    cursorY = placePartible(
                        text = block.text.toAnnotatedString(),
                        sliceFactory = { start, end, top, height ->
                            PageFragment.ParagraphSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                                sliceIndex = sliceIndex++,
                                topPx = top,
                                heightPx = height,
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
                    var sliceIndex = 0
                    cursorY = placePartible(
                        text = block.text.toAnnotatedString(),
                        sliceFactory = { start, end, top, height ->
                            PageFragment.HeadingSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                                sliceIndex = sliceIndex++,
                                topPx = top,
                                heightPx = height,
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
                        var sliceIndex = 0
                        cursorY = placePartible(
                            text = item.toAnnotatedString(),
                            sliceFactory = { start, end, top, height ->
                                PageFragment.ListItemSlice(
                                    originBlockId = block.id,
                                    block = block,
                                    itemIndex = index,
                                    charStart = start,
                                    charEndExclusive = end,
                                    sliceIndex = sliceIndex++,
                                    topPx = top,
                                    heightPx = height,
                                )
                            },
                            pageWidthPx = (pageWidthPx - with(density) {
                                LIST_MARKER_GUTTER.toPx()
                            }).coerceAtLeast(1f),
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
                        var sliceIndex = 0
                        cursorY = placePartible(
                            text = item.toAnnotatedString(),
                            sliceFactory = { start, end, top, height ->
                                PageFragment.OrderedListItemSlice(
                                    originBlockId = block.id,
                                    block = block,
                                    itemIndex = index,
                                    charStart = start,
                                    charEndExclusive = end,
                                    sliceIndex = sliceIndex++,
                                    topPx = top,
                                    heightPx = height,
                                )
                            },
                            pageWidthPx = (pageWidthPx - with(density) {
                                LIST_MARKER_GUTTER.toPx()
                            }).coerceAtLeast(1f),
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
                    cursorY = placeVerseColumns(
                        block = block,
                        pageWidthPx = pageWidthPx,
                        pageHeightPx = pageHeightPx,
                        cursorYStart = cursorY,
                        density = density,
                        textMeasurer = textMeasurer,
                        style = style,
                        onSliceEmitted = { currentFragments.add(it) },
                        onPageFilled = { openPage() },
                    )
                }
                is StudyBlock.Quote -> {
                    var sliceIndex = 0
                    cursorY = placePartible(
                        text = block.text.toAnnotatedString(),
                        sliceFactory = { start, end, top, height ->
                            PageFragment.QuoteSlice(
                                originBlockId = block.id,
                                block = block,
                                charStart = start,
                                charEndExclusive = end,
                                sliceIndex = sliceIndex++,
                                topPx = top,
                                heightPx = height,
                            )
                        },
                        pageWidthPx = (pageWidthPx - with(density) {
                            QUOTE_INDENT.toPx()
                        }).coerceAtLeast(1f),
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
        text: AnnotatedString,
        sliceFactory: (
            start: Int,
            endExclusive: Int,
            topPx: Float,
            heightPx: Float,
        ) -> PageFragment,
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
            val emptyLayout = textMeasurer.measure(
                text = AnnotatedString(" "),
                style = style,
                constraints = Constraints(maxWidth = pageWidthPx.toInt()),
                density = density,
            )
            val lineHeight = emptyLayout.size.height.toFloat().coerceAtLeast(1f)
            var top = cursorYStart
            if (top + lineHeight > pageHeightPx && top > 0f) {
                onPageFilled()
                top = 0f
            }
            onSliceEmitted(sliceFactory(0, 0, top, lineHeight))
            return top + lineHeight
        }
        val layout = textMeasurer.measure(
            text = text,
            style = style,
            constraints = Constraints(maxWidth = pageWidthPx.toInt()),
            density = density,
        )
        StudyEditorDebugLog.log(
            "PAGINATION_TEXT_LAYOUT",
            "length=${text.length} widthLimit=${pageWidthPx.toInt()} " +
                "layout=${layout.size.width}x${layout.size.height} lines=${layout.lineCount} " +
                "fontSize=${style.fontSize} lineHeight=${style.lineHeight} density=$density",
        )
        val total = text.length
        var cursorY = cursorYStart
        var blockOffset = 0
        var pageSliceStart = 0
        var pageSliceTop = cursorYStart
        var pageSliceHeight = 0f
        var guard = 0
        while (blockOffset < total) {
            val lineIndex = layout.getLineForOffset(blockOffset)
            // The visible end excludes wrapping whitespace. Using it as the
            // next cursor can resolve back to the same line and truncate every
            // line after the first one.
            val lineEnd = layout.getLineEnd(lineIndex, false).coerceAtMost(total)
            if (lineEnd <= blockOffset) break
            val lineHeight = (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex))
                .coerceAtLeast(1f)

            if (cursorY + lineHeight > pageHeightPx && cursorY > 0f) {
                if (blockOffset > pageSliceStart) {
                    onSliceEmitted(
                        sliceFactory(
                            pageSliceStart,
                            blockOffset,
                            pageSliceTop,
                            pageSliceHeight,
                        ),
                    )
                }
                onPageFilled()
                cursorY = 0f
                pageSliceStart = blockOffset
                pageSliceTop = 0f
                pageSliceHeight = 0f
                continue
            }
            cursorY += lineHeight
            pageSliceHeight += lineHeight
            blockOffset = lineEnd
            if (++guard > 100_000) break
        }
        if (total > pageSliceStart) {
            onSliceEmitted(
                sliceFactory(
                    pageSliceStart,
                    total,
                    pageSliceTop,
                    pageSliceHeight,
                ),
            )
        }
        return cursorY
    }

    private fun placeVerseColumns(
        block: StudyBlock.Verse,
        pageWidthPx: Float,
        pageHeightPx: Float,
        cursorYStart: Float,
        density: Density,
        textMeasurer: TextMeasurer,
        style: TextStyle,
        onSliceEmitted: (PageFragment.VerseSlice) -> Unit,
        onPageFilled: () -> Unit,
    ): Float {
        val primaryText = block.contents[block.sourceVersion].orEmpty()
        val comparisonVersion = block.displayedVersions().drop(1).firstOrNull()
        val comparisonText = comparisonVersion?.let { block.contents[it].orEmpty() }.orEmpty()
        val hasComparison = comparisonVersion != null && comparisonText.isNotBlank()
        val contentWidth = (
            pageWidthPx - with(density) { VERSE_CONTENT_INDENT.toPx() }
            ).coerceAtLeast(1f)
        val gapPx = with(density) { VERSE_COLUMN_GAP.toPx() }
        val columnWidth = if (hasComparison) {
            ((contentWidth - gapPx) / 2f).coerceAtLeast(1f)
        } else {
            contentWidth
        }
        val headerHeight = with(density) { VERSE_HEADER_HEIGHT.toPx() }
        val labelsHeight = if (hasComparison) {
            with(density) { VERSE_VERSION_LABEL_HEIGHT.toPx() }
        } else {
            0f
        }
        val bottomReserve = with(density) { VERSE_BOTTOM_RESERVE.toPx() }
        val minimumLineHeight = textMeasurer.measure(
            text = AnnotatedString(" "),
            style = style,
            constraints = Constraints(maxWidth = columnWidth.toInt()),
            density = density,
        ).size.height.toFloat().coerceAtLeast(1f)

        var primaryOffset = 0
        var comparisonOffset = 0
        var cursorY = cursorYStart
        var sliceIndex = 0
        var firstSlice = true

        do {
            val fixedHeight = (if (firstSlice) headerHeight else 0f) +
                (if (firstSlice) labelsHeight else 0f)
            if (
                cursorY > 0f &&
                cursorY + fixedHeight + minimumLineHeight + bottomReserve > pageHeightPx
            ) {
                onPageFilled()
                cursorY = 0f
            }
            val availableBodyHeight =
                (pageHeightPx - cursorY - fixedHeight - bottomReserve)
                    .coerceAtLeast(minimumLineHeight)
            val primaryFit = fitTextSlice(
                text = primaryText,
                start = primaryOffset,
                widthPx = columnWidth,
                maxHeightPx = availableBodyHeight,
                density = density,
                textMeasurer = textMeasurer,
                style = style,
            )
            val comparisonFit = if (hasComparison) {
                fitTextSlice(
                    text = comparisonText,
                    start = comparisonOffset,
                    widthPx = columnWidth,
                    maxHeightPx = availableBodyHeight,
                    density = density,
                    textMeasurer = textMeasurer,
                    style = style,
                )
            } else {
                TextSliceFit(0, 0f)
            }
            val bodyHeight = maxOf(
                primaryFit.heightPx,
                comparisonFit.heightPx,
                if (primaryText.isEmpty() && !hasComparison) minimumLineHeight else 0f,
            )
            onSliceEmitted(
                PageFragment.VerseSlice(
                    originBlockId = block.id,
                    block = block,
                    charStart = primaryOffset,
                    charEndExclusive = primaryFit.endExclusive,
                    comparisonVersion = comparisonVersion.takeIf { hasComparison },
                    comparisonCharStart = comparisonOffset,
                    comparisonCharEndExclusive = comparisonFit.endExclusive,
                    showHeader = firstSlice,
                    sliceIndex = sliceIndex++,
                    topPx = cursorY,
                    heightPx = fixedHeight + bodyHeight + bottomReserve,
                )
            )
            primaryOffset = primaryFit.endExclusive
            comparisonOffset = comparisonFit.endExclusive
            cursorY += fixedHeight + bodyHeight + bottomReserve
            firstSlice = false

            val hasRemaining = primaryOffset < primaryText.length ||
                (hasComparison && comparisonOffset < comparisonText.length)
            if (hasRemaining) {
                onPageFilled()
                cursorY = 0f
            }
        } while (
            primaryOffset < primaryText.length ||
            (hasComparison && comparisonOffset < comparisonText.length)
        )

        return cursorY
    }

    private fun fitTextSlice(
        text: String,
        start: Int,
        widthPx: Float,
        maxHeightPx: Float,
        density: Density,
        textMeasurer: TextMeasurer,
        style: TextStyle,
    ): TextSliceFit {
        if (start >= text.length) return TextSliceFit(text.length, 0f)
        val remaining = text.substring(start)
        val layout = textMeasurer.measure(
            text = AnnotatedString(remaining),
            style = style,
            constraints = Constraints(maxWidth = widthPx.toInt()),
            density = density,
        )
        var localEnd = 0
        var height = 0f
        for (line in 0 until layout.lineCount) {
            val lineHeight = (layout.getLineBottom(line) - layout.getLineTop(line))
                .coerceAtLeast(1f)
            if (height + lineHeight > maxHeightPx && localEnd > 0) break
            height += lineHeight
            localEnd = layout.getLineEnd(line, false).coerceAtMost(remaining.length)
            if (height >= maxHeightPx) break
        }
        if (localEnd <= 0) {
            localEnd = layout.getLineEnd(0, false).coerceAtLeast(1).coerceAtMost(remaining.length)
            height = (layout.getLineBottom(0) - layout.getLineTop(0)).coerceAtLeast(1f)
        }
        return TextSliceFit(start + localEnd, height)
    }

    private data class TextSliceFit(
        val endExclusive: Int,
        val heightPx: Float,
    )

    /**
     * Estima la altura de un bloque midiendo su texto con el [TextMeasurer].
     * Se usa en el chequeo de keep-with-next para headings para evitar encabezados
     * huerfanos al final de una pagina.
     */
    internal fun estimateBlockHeight(
        block: StudyBlock,
        pageWidthPx: Float,
        density: Density,
        textMeasurer: TextMeasurer,
    ): Float {
        if (block is StudyBlock.Verse) {
            val style = textStyleFor(block, density)
            val comparisonVersion = block.displayedVersions().drop(1).firstOrNull()
            val hasComparison = comparisonVersion
                ?.let { block.contents[it].orEmpty().isNotBlank() }
                ?: false
            val contentWidth = (
                pageWidthPx - with(density) { VERSE_CONTENT_INDENT.toPx() }
                ).coerceAtLeast(1f)
            val gapPx = with(density) { VERSE_COLUMN_GAP.toPx() }
            val width = if (hasComparison) {
                ((contentWidth - gapPx) / 2f).coerceAtLeast(1f)
            } else {
                contentWidth
            }
            val primaryHeight = measureTextHeight(
                block.contents[block.sourceVersion].orEmpty(),
                width,
                density,
                textMeasurer,
                style,
            )
            val comparisonHeight = comparisonVersion?.let { version ->
                measureTextHeight(
                    block.contents[version].orEmpty(),
                    width,
                    density,
                    textMeasurer,
                    style,
                )
            } ?: 0f
            return maxOf(primaryHeight, comparisonHeight) +
                with(density) {
                    (VERSE_HEADER_HEIGHT + if (hasComparison) {
                        VERSE_VERSION_LABEL_HEIGHT
                    } else {
                        0.dp
                    } + VERSE_BOTTOM_RESERVE).toPx()
                }
        }
        val text = when (block) {
            is StudyBlock.Paragraph -> block.text.toAnnotatedString()
            is StudyBlock.Heading -> block.text.toAnnotatedString()
            is StudyBlock.Quote -> block.text.toAnnotatedString()
            is StudyBlock.Verse -> AnnotatedString(block.documentText())
            is StudyBlock.BulletList -> block.items.fold(AnnotatedString("")) { acc, item ->
                acc + item.toAnnotatedString() + AnnotatedString(" ")
            }
            is StudyBlock.OrderedList -> block.items.fold(AnnotatedString("")) { acc, item ->
                acc + item.toAnnotatedString() + AnnotatedString(" ")
            }
        }
        if (text.isEmpty()) return 0f
        val style = textStyleFor(block, density)
        val effectiveWidthPx = when (block) {
            is StudyBlock.BulletList,
            is StudyBlock.OrderedList,
            -> pageWidthPx - with(density) { LIST_MARKER_GUTTER.toPx() }
            is StudyBlock.Quote -> pageWidthPx - with(density) { QUOTE_INDENT.toPx() }
            else -> pageWidthPx
        }.coerceAtLeast(1f)
        val layout = textMeasurer.measure(
            text = text,
            style = style,
            constraints = androidx.compose.ui.unit.Constraints(
                maxWidth = effectiveWidthPx.toInt(),
            ),
            density = density,
        )
        return layout.size.height.toFloat()
    }

    private fun measureTextHeight(
        text: String,
        widthPx: Float,
        density: Density,
        textMeasurer: TextMeasurer,
        style: TextStyle,
    ): Float {
        if (text.isEmpty()) return 0f
        return textMeasurer.measure(
            text = AnnotatedString(text),
            style = style,
            constraints = Constraints(maxWidth = widthPx.toInt()),
            density = density,
        ).size.height.toFloat()
    }

    internal fun textStyleFor(block: StudyBlock, density: Density): TextStyle {
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
        val lineHeightSp = fontSizeSp * if (block is StudyBlock.Verse) {
            VERSE_LINE_HEIGHT_MULTIPLIER
        } else {
            DocConfig.LINE_HEIGHT
        }
        return TextStyle(
            fontSize = with(density) { fontSizeSp.sp },
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textAlign = block.alignment.toTextAlign(),
            lineHeight = with(density) { lineHeightSp.sp },
        )
    }

    private val LIST_MARKER_GUTTER = 40.dp
    internal val QUOTE_INDENT = 16.dp
    private const val VERSE_LINE_HEIGHT_MULTIPLIER = 1.55f
    private val VERSE_CONTENT_INDENT = 16.dp
    private val VERSE_COLUMN_GAP = 20.dp
    private val VERSE_HEADER_HEIGHT = 40.dp
    private val VERSE_VERSION_LABEL_HEIGHT = 24.dp
    private val VERSE_BOTTOM_RESERVE = 2.dp
}

private fun BlockAlignment.toTextAlign(): TextAlign = when (this) {
    BlockAlignment.Start -> TextAlign.Start
    BlockAlignment.Center -> TextAlign.Center
    BlockAlignment.End -> TextAlign.End
    BlockAlignment.Justify -> TextAlign.Justify
}

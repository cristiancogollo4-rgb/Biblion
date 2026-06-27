package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import kotlin.math.max

data class DocumentPagination(
    val pages: List<List<StudyBlock>>,
    val pageCount: Int,
    val documentHeight: Dp,
)

private data class MeasuredBlock(
    val block: StudyBlock,
    val heightPx: Int,
)

@Composable
fun rememberDocumentPagination(blocks: List<StudyBlock>): DocumentPagination {
    val density = LocalDensity.current
    val fixedDensity = remember(density.density) {
        Density(density = density.density, fontScale = 1.0f)
    }
    val textMeasurer = rememberTextMeasurer()

    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = DocConfig.FontSize,
        lineHeight = DocConfig.LineHeight,
    )
    val bodyMediumStyle = MaterialTheme.typography.bodyMedium.copy(
        lineHeight = DocConfig.LineHeight,
    )
    val bodySmallStyle = MaterialTheme.typography.bodySmall.copy(
        lineHeight = DocConfig.LineHeight,
    )
    val headline1Style = MaterialTheme.typography.headlineSmall.copy(
        fontSize = DocConfig.Heading1Size,
        lineHeight = DocConfig.LineHeight,
    )
    val headline2Style = MaterialTheme.typography.headlineSmall.copy(
        fontSize = DocConfig.Heading2Size,
        lineHeight = DocConfig.LineHeight,
    )
    val headline3Style = MaterialTheme.typography.headlineSmall.copy(
        fontSize = DocConfig.Heading3Size,
        lineHeight = DocConfig.LineHeight,
    )
    val headlineOtherStyle = MaterialTheme.typography.headlineSmall.copy(
        fontSize = DocConfig.Heading3Size,
        lineHeight = DocConfig.LineHeight,
    )
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        lineHeight = DocConfig.LineHeight,
    )

    return remember(
        blocks,
        fixedDensity,
        textMeasurer,
        bodyStyle,
        bodyMediumStyle,
        bodySmallStyle,
        headline1Style,
        headline2Style,
        headline3Style,
        headlineOtherStyle,
        labelStyle,
    ) {
        val pageWidthPx = with(fixedDensity) { DocConfig.PageWidth.roundToPx() }
        val contentWidthPx = (pageWidthPx - with(fixedDensity) { DocConfig.PagePadding.roundToPx() } * 2)
            .coerceAtLeast(1)
        val pageHeightPx = with(fixedDensity) { DocConfig.PageHeight.roundToPx() }
        val contentHeightPx = (pageHeightPx - with(fixedDensity) { DocConfig.PageContentVerticalPadding.roundToPx() } * 2)
            .coerceAtLeast(1)
        val pageGapPx = with(fixedDensity) { DocConfig.PageGap.roundToPx() }
        val blockGapPx = with(fixedDensity) { DocConfig.BlockGap.roundToPx() }

        val measuredBlocks = blocks.map { block ->
            MeasuredBlock(
                block = block,
                heightPx = estimateBlockHeightPx(
                    block = block,
                    textMeasurer = textMeasurer,
                    contentWidthPx = contentWidthPx,
                    bodyStyle = bodyStyle,
                    bodyMediumStyle = bodyMediumStyle,
                    bodySmallStyle = bodySmallStyle,
                    headline1Style = headline1Style,
                    headline2Style = headline2Style,
                    headline3Style = headline3Style,
                    headlineOtherStyle = headlineOtherStyle,
                    labelStyle = labelStyle,
                    density = fixedDensity,
                ),
            )
        }

        val pages = paginateMeasuredBlocks(
            measuredBlocks = measuredBlocks,
            contentHeightPx = contentHeightPx,
            blockGapPx = blockGapPx,
        )

        val pageCount = pages.size.coerceAtLeast(1)
        val documentHeight = with(fixedDensity) {
            val pageHeight = DocConfig.PageHeight * pageCount.toFloat()
            val gaps = DocConfig.PageGap * (pageCount - 1).coerceAtLeast(0).toFloat()
            pageHeight + gaps
        }

        DocumentPagination(
            pages = pages.ifEmpty { listOf(emptyList()) },
            pageCount = pageCount,
            documentHeight = documentHeight,
        )
    }
}

private fun paginateMeasuredBlocks(
    measuredBlocks: List<MeasuredBlock>,
    contentHeightPx: Int,
    blockGapPx: Int,
): List<List<StudyBlock>> {
    if (measuredBlocks.isEmpty()) return emptyList()

    val pages = mutableListOf<MutableList<StudyBlock>>()
    var currentPage = mutableListOf<StudyBlock>()
    var currentHeight = 0

    fun flushPage() {
        if (currentPage.isNotEmpty()) {
            pages.add(currentPage)
            currentPage = mutableListOf()
            currentHeight = 0
        }
    }

    measuredBlocks.forEach { measured ->
        if (measured.block is StudyBlock.PageBreak) {
            currentPage.add(measured.block)
            flushPage()
            return@forEach
        }

        val requiredHeight = measured.heightPx + if (currentPage.isEmpty()) 0 else blockGapPx
        val shouldWrap = currentPage.isNotEmpty() && currentHeight + requiredHeight > contentHeightPx
        if (shouldWrap) {
            flushPage()
        }

        if (currentPage.isNotEmpty()) {
            currentHeight += blockGapPx
        }

        currentPage.add(measured.block)
        currentHeight += measured.heightPx
    }

    flushPage()
    return pages
}

private fun estimateBlockHeightPx(
    block: StudyBlock,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    contentWidthPx: Int,
    bodyStyle: TextStyle,
    bodyMediumStyle: TextStyle,
    bodySmallStyle: TextStyle,
    headline1Style: TextStyle,
    headline2Style: TextStyle,
    headline3Style: TextStyle,
    headlineOtherStyle: TextStyle,
    labelStyle: TextStyle,
    density: androidx.compose.ui.unit.Density,
): Int {
    fun measureText(text: StyledText, style: TextStyle, widthPx: Int = contentWidthPx): Int {
        val layout = textMeasurer.measure(
            text = AnnotatedString(text.plain().ifBlank { " " }),
            style = style,
            constraints = Constraints(
                maxWidth = widthPx,
            ),
        )
        return layout.size.height
    }

    fun textLinePx(): Int = with(density) { DocConfig.LineHeight.roundToPx() }
    fun gapPx(dp: Dp): Int = with(density) { dp.roundToPx() }

    return when (block) {
        is StudyBlock.Paragraph -> measureText(block.text, bodyStyle) + gapPx(4.dp)
        is StudyBlock.Heading -> {
            val style = when (block.level) {
                1 -> headline1Style
                2 -> headline2Style
                3 -> headline3Style
                else -> headlineOtherStyle
            }
            measureText(block.text, style) + gapPx(2.dp)
        }
        is StudyBlock.Quote -> {
            val quoteHeight = measureText(StyledText.plain("\""), headline1Style)
            val bodyHeight = measureText(block.text, bodyStyle)
            val attributionHeight = if (block.attribution.isNullOrBlank()) 0 else textLinePx()
            quoteHeight + bodyHeight + attributionHeight + gapPx(4.dp)
        }
        is StudyBlock.BulletList -> {
            val itemHeight = block.items.sumOf { item ->
                max(measureText(item, bodyStyle), gapPx(48.dp))
            }
            val buttonHeight = if (block.items.isEmpty()) gapPx(40.dp) else gapPx(40.dp)
            itemHeight + buttonHeight + gapPx(2.dp) * max(block.items.size - 1, 0)
        }
        is StudyBlock.NumberedList -> {
            val itemHeight = block.items.sumOf { item ->
                max(measureText(item, bodyStyle), gapPx(48.dp))
            }
            val buttonHeight = if (block.items.isEmpty()) gapPx(40.dp) else gapPx(40.dp)
            itemHeight + buttonHeight + gapPx(2.dp) * max(block.items.size - 1, 0)
        }
        is StudyBlock.Note -> {
            val labelHeight = measureText(StyledText.plain("Nota"), labelStyle)
            val bodyHeight = measureText(block.text, bodyMediumStyle)
            labelHeight + bodyHeight + gapPx(4.dp)
        }
        is StudyBlock.Reflection -> {
            val promptHeight = if (block.prompt.isNullOrBlank()) 0 else measureText(StyledText.plain(block.prompt), labelStyle)
            val bodyHeight = measureText(block.text, bodyMediumStyle)
            promptHeight + bodyHeight + gapPx(4.dp)
        }
        is StudyBlock.Callout -> {
            val labelHeight = measureText(StyledText.plain("! Destacado"), labelStyle)
            val bodyHeight = measureText(block.text, bodyMediumStyle)
            labelHeight + bodyHeight + gapPx(4.dp)
        }
        is StudyBlock.Verse -> {
            val referenceHeight = measureText(StyledText.plain(block.reference.displayShort()), labelStyle)
            val primaryHeight = measureText(block.primaryText, bodyMediumStyle)
            val compareHeight = if (block.compareText != null && block.compareVersion != null) {
                measureText(StyledText.plain("${block.compareVersion}:"), labelStyle) +
                    measureText(block.compareText, bodyMediumStyle)
            } else {
                0
            }
            referenceHeight + primaryHeight + compareHeight + gapPx(4.dp)
        }
        is StudyBlock.Divider -> gapPx(24.dp)
        is StudyBlock.PageBreak -> gapPx(24.dp)
        is StudyBlock.Table -> {
            if (block.rows.isEmpty()) return gapPx(24.dp)
            val columns = block.rows.maxOfOrNull { row -> row.cells.size } ?: 0
            if (columns == 0) return gapPx(24.dp)
            val cellSpacingPx = gapPx(2.dp)
            val cellWidthPx = ((contentWidthPx - cellSpacingPx * (columns - 1)) / columns).coerceAtLeast(1)
            val rowHeights = block.rows.map { row ->
                val cellsHeight = row.cells.map { cell ->
                    measureText(cell, bodySmallStyle, cellWidthPx)
                }.maxOrNull() ?: textLinePx()
                cellsHeight + gapPx(8.dp)
            }
            rowHeights.sum() + cellSpacingPx * max(block.rows.size - 1, 0)
        }
        is StudyBlock.TodoList -> {
            val itemHeight = block.items.sumOf { item ->
                max(measureText(item.text, bodyStyle), gapPx(48.dp))
            }
            itemHeight + gapPx(4.dp)
        }
        is StudyBlock.ColumnLayout -> {
            val colWidth = contentWidthPx / block.columnBlocks.size.coerceAtLeast(1)
            val colHeights = block.columnBlocks.map { col ->
                col.sumOf { child ->
                    when (child) {
                        is StudyBlock.Paragraph -> measureText(child.text, bodyStyle, colWidth)
                        is StudyBlock.Heading -> {
                            val style = when (child.level) {
                                1 -> headline1Style
                                2 -> headline2Style
                                3 -> headline3Style
                                else -> headlineOtherStyle
                            }
                            measureText(child.text, style, colWidth)
                        }
                        is StudyBlock.Quote -> measureText(child.text, bodyStyle, colWidth)
                        else -> textLinePx()
                    }
                }
            }
            (colHeights.maxOrNull() ?: 0) + gapPx(4.dp)
        }
        is StudyBlock.Comment -> textLinePx() + gapPx(4.dp)
    }
}

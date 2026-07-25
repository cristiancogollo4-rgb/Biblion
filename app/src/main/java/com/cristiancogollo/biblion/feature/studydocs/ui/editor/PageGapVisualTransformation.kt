package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import com.cristiancogollo.biblion.feature.studydocs.debug.StudyEditorDebugLog
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageGapSpec
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PagedEditorUnit

private const val PAGE_GAP_MARKER = "\n\u00A0\n"

class PageGapVisualTransformation(
    private val styledText: StyledText,
    gaps: List<PageGapSpec>,
    private val pxToSp: (Float) -> TextUnit,
) : VisualTransformation {
    private val gaps = gaps
        .filter { it.offset in 0..styledText.length && it.heightPx > 0f }
        .sortedBy { it.offset }

    override fun filter(text: AnnotatedString): TransformedText {
        val source = StyledText(
            raw = text.text,
            ranges = styledText.ranges,
        ).toAnnotatedString()
        if (gaps.isEmpty()) return TransformedText(source, OffsetMapping.Identity)

        val transformed = buildAnnotatedString {
            var cursor = 0
            gaps.forEach { gap ->
                val offset = gap.offset.coerceIn(cursor, source.length)
                append(source.subSequence(cursor, offset))
                append('\n')
                pushStyle(
                    ParagraphStyle(
                        lineHeight = pxToSp(gap.spacerHeightPx),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Top,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    ),
                )
                pushStyle(SpanStyle(color = Color.Transparent, fontSize = pxToSp(1f)))
                // A zero-width glyph does not reliably contribute its
                // paragraph line height and lets continuation text overlap
                // the page gap. NBSP is transparent but keeps real metrics.
                append('\u00A0')
                pop()
                pop()
                append('\n')
                cursor = offset
            }
            append(source.subSequence(cursor, source.length))
        }

        return TransformedText(
            text = transformed,
            offsetMapping = PageGapOffsetMapping(
                sourceLength = source.length,
                breakOffsets = gaps.map { it.offset },
            ),
        )
    }
}

/**
 * Compose adds font metrics around the synthetic spacer paragraph. Measure
 * those metrics with the same inputs as the editor and compensate so the
 * continuation starts at the exact geometric top of the next page.
 */
internal fun calibratePageGapHeights(
    unit: PagedEditorUnit,
    styledText: StyledText,
    textStyle: TextStyle,
    widthPx: Float,
    density: Density,
    textMeasurer: TextMeasurer,
): PagedEditorUnit {
    if (unit.gaps.isEmpty() || styledText.length == 0) return unit

    val calibrated = unit.gaps.toMutableList()
    calibrated.indices.forEach { index ->
        val transformation = PageGapVisualTransformation(
            styledText = styledText,
            gaps = calibrated,
            pxToSp = { px -> with(density) { px.toSp() } },
        )
        val transformed = transformation.filter(AnnotatedString(styledText.raw))
        val layout = textMeasurer.measure(
            text = transformed.text,
            style = textStyle,
            constraints = Constraints(maxWidth = widthPx.toInt().coerceAtLeast(1)),
            density = density,
        )
        val gap = calibrated[index]
        val continuationOffset = transformed.offsetMapping
            .originalToTransformed(gap.offset)
            .coerceIn(0, transformed.text.length)
        val continuationLine = layout.getLineForOffset(continuationOffset)
        val actualTopPx = layout.getLineTop(continuationLine)
        val expectedTopPx = gap.startPx + gap.heightPx
        val correctionPx = expectedTopPx - actualTopPx
        calibrated[index] = gap.copy(
            spacerHeightPx = (gap.spacerHeightPx + correctionPx).coerceAtLeast(1f),
        )
        StudyEditorDebugLog.log(
            "PAGE_GAP_CALIBRATED",
            "block=${unit.key.blockId.value} item=${unit.key.itemIndex} " +
                "offset=${gap.offset} expected=$expectedTopPx actual=$actualTopPx " +
                "correction=$correctionPx spacer=${calibrated[index].spacerHeightPx}",
        )
    }
    return unit.copy(gaps = calibrated)
}

internal fun PagedEditorUnit.styledText(): StyledText = when (val value = block) {
    is StudyBlock.Paragraph -> value.text
    is StudyBlock.Heading -> value.text
    is StudyBlock.Quote -> value.text
    is StudyBlock.BulletList -> value.items.getOrNull(key.itemIndex ?: -1) ?: StyledText()
    is StudyBlock.OrderedList -> value.items.getOrNull(key.itemIndex ?: -1) ?: StyledText()
    is StudyBlock.Verse -> StyledText()
}

internal class PageGapOffsetMapping(
    private val sourceLength: Int,
    breakOffsets: List<Int>,
) : OffsetMapping {
    private val breaks = breakOffsets
        .map { it.coerceIn(0, sourceLength) }
        .distinct()
        .sorted()

    override fun originalToTransformed(offset: Int): Int {
        val safe = offset.coerceIn(0, sourceLength)
        val insertedBeforeOrAt = breaks.count { it <= safe }
        return safe + insertedBeforeOrAt * PAGE_GAP_MARKER.length
    }

    override fun transformedToOriginal(offset: Int): Int {
        var removed = 0
        breaks.forEach { breakOffset ->
            val markerStart = breakOffset + removed
            val markerEnd = markerStart + PAGE_GAP_MARKER.length
            if (offset <= markerStart) {
                return (offset - removed).coerceIn(0, sourceLength)
            }
            if (offset <= markerEnd) return breakOffset
            removed += PAGE_GAP_MARKER.length
        }
        return (offset - removed).coerceIn(0, sourceLength)
    }
}

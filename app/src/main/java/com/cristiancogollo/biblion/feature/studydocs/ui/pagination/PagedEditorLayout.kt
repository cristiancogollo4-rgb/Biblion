package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

data class PagedEditorUnitKey(
    val blockId: BlockId,
    val itemIndex: Int? = null,
)

data class PageGapSpec(
    val offset: Int,
    val startPx: Float,
    val heightPx: Float,
    val spacerHeightPx: Float = heightPx,
)

data class PagedEditorUnit(
    val key: PagedEditorUnitKey,
    val block: StudyBlock,
    val topPx: Float,
    val heightPx: Float,
    val gaps: List<PageGapSpec>,
)

/**
 * Converts page fragments into one editable surface per paragraph/list item.
 *
 * A unit can span several pages, but it is mounted only once. The visual
 * transformation inserts [PageGapSpec] spacers at the slice boundaries.
 */
fun buildPagedEditorUnits(
    pages: List<Page>,
    pageHeightPx: Float,
    pageGapPx: Float,
    pageMarginPx: Float,
    canvasVerticalPaddingPx: Float,
): List<PagedEditorUnit> {
    data class Positioned(
        val fragment: PageFragment,
        val globalTopPx: Float,
    )

    val positioned = pages.flatMap { page ->
        val pageTop = canvasVerticalPaddingPx + page.index * (pageHeightPx + pageGapPx)
        page.fragments.map { fragment ->
            Positioned(
                fragment = fragment,
                globalTopPx = pageTop + pageMarginPx + fragment.topPx,
            )
        }
    }

    val grouped = linkedMapOf<PagedEditorUnitKey, MutableList<Positioned>>()
    positioned.forEach { entry ->
        val fragment = entry.fragment
        if (fragment is PageFragment.VerseSlice || fragment is PageFragment.Whole) return@forEach
        val key = PagedEditorUnitKey(
            blockId = fragment.originBlockId,
            itemIndex = fragment.itemIndexOrNull(),
        )
        grouped.getOrPut(key) { mutableListOf() }.add(entry)
    }

    return grouped.mapNotNull { (key, entries) ->
        val sorted = entries.sortedWith(
            compareBy<Positioned>({ it.globalTopPx }, { it.fragment.sliceIndex }),
        )
        val first = sorted.firstOrNull() ?: return@mapNotNull null
        val gaps = sorted.zipWithNext().mapNotNull { (current, next) ->
            val offset = current.fragment.charEndExclusiveOrNull() ?: return@mapNotNull null
            val currentBottom = current.globalTopPx + current.fragment.heightPx
            val gapHeight = (next.globalTopPx - currentBottom).coerceAtLeast(0f)
            if (gapHeight <= 0.5f) {
                null
            } else {
                PageGapSpec(
                    offset = offset,
                    startPx = currentBottom - first.globalTopPx,
                    heightPx = gapHeight,
                )
            }
        }
        PagedEditorUnit(
            key = key,
            block = first.fragment.blockValue(),
            topPx = first.globalTopPx,
            heightPx = (
                sorted.last().globalTopPx +
                    sorted.last().fragment.heightPx -
                    first.globalTopPx
                ).coerceAtLeast(first.fragment.heightPx),
            gaps = gaps,
        )
    }
}

internal fun PageFragment.itemIndexOrNull(): Int? = when (this) {
    is PageFragment.ListItemSlice -> itemIndex
    is PageFragment.OrderedListItemSlice -> itemIndex
    else -> null
}

internal fun PageFragment.charEndExclusiveOrNull(): Int? = when (this) {
    is PageFragment.ParagraphSlice -> charEndExclusive
    is PageFragment.HeadingSlice -> charEndExclusive
    is PageFragment.ListItemSlice -> charEndExclusive
    is PageFragment.OrderedListItemSlice -> charEndExclusive
    is PageFragment.VerseSlice -> charEndExclusive
    is PageFragment.QuoteSlice -> charEndExclusive
    is PageFragment.Whole -> null
}

internal fun PageFragment.blockValue(): StudyBlock = when (this) {
    is PageFragment.ParagraphSlice -> block
    is PageFragment.HeadingSlice -> block
    is PageFragment.ListItemSlice -> block
    is PageFragment.OrderedListItemSlice -> block
    is PageFragment.VerseSlice -> block
    is PageFragment.QuoteSlice -> block
    is PageFragment.Whole -> block
}

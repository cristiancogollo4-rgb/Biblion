package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

/**
 * Modelo visual inmutable de un fragmento de bloque en una pagina.
 *
 * No es persistente ni serializado: la paginacion es estrictamente visual y se
 * recalcula en cada cambio estructural. Cada fragmento referencia su bloque de
 * origen (un StudyBlock) y, si el bloque es divisible (parrafo, heading, lista),
 * las coordenadas charStart..charEndExclusive delimitan el trozo visible.
 *
 * En la composicion, [originBlockId] + [blockType] + rangos determinan
 * identicamente el contenido renderizado por pagina.
 */
sealed interface PageFragment {
    val originBlockId: BlockId
    val sliceIndex: Int
    val topPx: Float
    val heightPx: Float

    data class ParagraphSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.Paragraph,
        val charStart: Int,
        val charEndExclusive: Int,
        override val sliceIndex: Int = 0,
        override val topPx: Float = 0f,
        override val heightPx: Float = 0f,
    ) : PageFragment

    data class HeadingSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.Heading,
        val charStart: Int,
        val charEndExclusive: Int,
        override val sliceIndex: Int = 0,
        override val topPx: Float = 0f,
        override val heightPx: Float = 0f,
    ) : PageFragment

    data class ListItemSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.BulletList,
        val itemIndex: Int,
        val charStart: Int,
        val charEndExclusive: Int,
        override val sliceIndex: Int = 0,
        override val topPx: Float = 0f,
        override val heightPx: Float = 0f,
    ) : PageFragment

    data class OrderedListItemSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.OrderedList,
        val itemIndex: Int,
        val charStart: Int,
        val charEndExclusive: Int,
        override val sliceIndex: Int = 0,
        override val topPx: Float = 0f,
        override val heightPx: Float = 0f,
    ) : PageFragment

    data class VerseSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.Verse,
        val charStart: Int,
        val charEndExclusive: Int,
        val comparisonVersion: String? = null,
        val comparisonCharStart: Int = 0,
        val comparisonCharEndExclusive: Int = 0,
        val showHeader: Boolean = true,
        override val sliceIndex: Int = 0,
        override val topPx: Float = 0f,
        override val heightPx: Float = 0f,
    ) : PageFragment

    data class QuoteSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.Quote,
        val charStart: Int,
        val charEndExclusive: Int,
        override val sliceIndex: Int = 0,
        override val topPx: Float = 0f,
        override val heightPx: Float = 0f,
    ) : PageFragment

    /**
     * Bloque indivisible: Divider, PageBreak, Callout, Note, Reflection, Table.
     * Se renderiza completo o salta a la siguiente pagina si no cabe.
     */
    data class Whole(
        override val originBlockId: BlockId,
        val block: StudyBlock,
        override val sliceIndex: Int = 0,
        override val topPx: Float = 0f,
        override val heightPx: Float = 0f,
    ) : PageFragment
}

data class Page(
    val index: Int,
    val fragments: List<PageFragment>,
)

/**
 * Stable Compose identity for a visual fragment.
 *
 * It deliberately excludes the block payload and charEndExclusive because both
 * change while typing. Including either value causes Compose to dispose the
 * active BasicRichTextEditor on every document synchronization.
 */
internal data class PageFragmentCompositionKey(
    val originBlockId: BlockId,
    val kind: PageFragmentKind,
    val itemIndex: Int? = null,
    val sliceIndex: Int = 0,
)

internal enum class PageFragmentKind {
    PARAGRAPH,
    HEADING,
    BULLET_ITEM,
    ORDERED_ITEM,
    VERSE,
    QUOTE,
    WHOLE,
}

internal fun PageFragment.compositionKey(): PageFragmentCompositionKey = when (this) {
    is PageFragment.ParagraphSlice -> PageFragmentCompositionKey(
        originBlockId = originBlockId,
        kind = PageFragmentKind.PARAGRAPH,
        sliceIndex = sliceIndex,
    )
    is PageFragment.HeadingSlice -> PageFragmentCompositionKey(
        originBlockId = originBlockId,
        kind = PageFragmentKind.HEADING,
        sliceIndex = sliceIndex,
    )
    is PageFragment.ListItemSlice -> PageFragmentCompositionKey(
        originBlockId = originBlockId,
        kind = PageFragmentKind.BULLET_ITEM,
        itemIndex = itemIndex,
        sliceIndex = sliceIndex,
    )
    is PageFragment.OrderedListItemSlice -> PageFragmentCompositionKey(
        originBlockId = originBlockId,
        kind = PageFragmentKind.ORDERED_ITEM,
        itemIndex = itemIndex,
        sliceIndex = sliceIndex,
    )
    is PageFragment.VerseSlice -> PageFragmentCompositionKey(
        originBlockId = originBlockId,
        kind = PageFragmentKind.VERSE,
        sliceIndex = sliceIndex,
    )
    is PageFragment.QuoteSlice -> PageFragmentCompositionKey(
        originBlockId = originBlockId,
        kind = PageFragmentKind.QUOTE,
        sliceIndex = sliceIndex,
    )
    is PageFragment.Whole -> PageFragmentCompositionKey(
        originBlockId = originBlockId,
        kind = PageFragmentKind.WHOLE,
    )
}

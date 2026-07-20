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

    data class ParagraphSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.Paragraph,
        val charStart: Int,
        val charEndExclusive: Int,
    ) : PageFragment

    data class HeadingSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.Heading,
        val charStart: Int,
        val charEndExclusive: Int,
    ) : PageFragment

    data class ListItemSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.BulletList,
        val itemIndex: Int,
        val charStart: Int,
        val charEndExclusive: Int,
    ) : PageFragment

    data class OrderedListItemSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.OrderedList,
        val itemIndex: Int,
        val charStart: Int,
        val charEndExclusive: Int,
    ) : PageFragment

    data class VerseSlice(
        override val originBlockId: BlockId,
        val block: StudyBlock.Verse,
        val charStart: Int,
        val charEndExclusive: Int,
    ) : PageFragment

    /**
     * Bloque indivisible: Divider, PageBreak, Callout, Note, Reflection, Quote, Table.
     * Se renderiza completo o salta a la siguiente pagina si no cabe.
     */
    data class Whole(
        override val originBlockId: BlockId,
        val block: StudyBlock,
    ) : PageFragment
}

data class Page(
    val index: Int,
    val fragments: List<PageFragment>,
)

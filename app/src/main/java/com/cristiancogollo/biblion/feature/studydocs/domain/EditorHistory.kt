package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

/** Estado completo necesario para restaurar una edicion del documento. */
data class EditorCheckpoint(
    val doc: StudyDoc,
    val richTexts: Map<EditorTextKey, StyledText>,
    val activeBlockId: BlockId?,
    val activeListItemIndex: Int? = null,
)

/** Identifica el editor de un bloque o el editor de un elemento de lista. */
data class EditorTextKey(
    val blockId: BlockId,
    val itemIndex: Int? = null,
)

/**
 * Historial acotado por documento. Guarda snapshots porque el texto vive en
 * RichTextState y no solo en StudyOp; asi estructura, texto y estilos viajan
 * juntos al deshacer.
 */
class EditorHistory(private val maxEntries: Int = 100) {
    private val undoStack = ArrayDeque<EditorCheckpoint>()
    private val redoStack = ArrayDeque<EditorCheckpoint>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun pushBeforeChange(checkpoint: EditorCheckpoint) {
        if (undoStack.lastOrNull() != checkpoint) {
            undoStack.addLast(checkpoint)
            while (undoStack.size > maxEntries) undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(current: EditorCheckpoint): EditorCheckpoint? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    fun redo(current: EditorCheckpoint): EditorCheckpoint? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}

/** Materializa los RichTextState actuales dentro del modelo persistible. */
fun StudyDoc.withEditorTexts(texts: Map<EditorTextKey, StyledText>): StudyDoc = copy(
    blocks = blocks.map { block ->
        when (block) {
            is StudyBlock.Paragraph -> block.copy(text = texts[EditorTextKey(block.id)] ?: block.text)
            is StudyBlock.Heading -> block.copy(text = texts[EditorTextKey(block.id)] ?: block.text)
            is StudyBlock.BulletList -> block.copy(items = replaceListItems(block.items, block.id, texts))
            is StudyBlock.OrderedList -> block.copy(items = replaceListItems(block.items, block.id, texts))
            is StudyBlock.Quote -> block.copy(text = texts[EditorTextKey(block.id)] ?: block.text)
            is StudyBlock.Verse -> block
        }
    },
)

private fun replaceListItems(
    items: List<StyledText>,
    blockId: BlockId,
    texts: Map<EditorTextKey, StyledText>,
): List<StyledText> = items.mapIndexed { index, item ->
    texts[EditorTextKey(blockId, index)] ?: item
}

internal fun StyledText.append(other: StyledText): StyledText {
    val offset = raw.length
    return StyledText(
        raw = raw + other.raw,
        ranges = ranges + other.ranges.map { range ->
            range.copy(
                start = range.start + offset,
                endExclusive = range.endExclusive + offset,
            )
        },
    )
}

package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.engine.StudyDocEngine
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyOp
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

sealed interface EditCommand {
    fun apply(doc: StudyDoc): StudyDoc
    fun invert(): EditCommand
    val description: String
}

class EditCommandStack {
    private val undoStack: ArrayDeque<EditCommand> = ArrayDeque()
    private val redoStack: ArrayDeque<EditCommand> = ArrayDeque()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoSize: Int get() = undoStack.size
    val redoSize: Int get() = redoStack.size

    fun execute(command: EditCommand, doc: StudyDoc): StudyDoc {
        val newDoc = command.apply(doc)
        undoStack.addLast(command)
        redoStack.clear()
        return newDoc
    }

    fun undo(currentDoc: StudyDoc): StudyDoc {
        if (undoStack.isEmpty()) return currentDoc
        val command = undoStack.removeLast()
        val inverse = command.invert()
        val newDoc = inverse.apply(currentDoc)
        redoStack.addLast(command)
        return newDoc
    }

    fun redo(currentDoc: StudyDoc): StudyDoc {
        if (redoStack.isEmpty()) return currentDoc
        val command = redoStack.removeLast()
        val newDoc = command.apply(currentDoc)
        undoStack.addLast(command)
        return newDoc
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun registerExternal(command: EditCommand) {
        undoStack.addLast(command)
    }

    val undoDescription: String? get() = undoStack.lastOrNull()?.description
    val redoDescription: String? get() = redoStack.lastOrNull()?.description
}

data class InsertBlockCommand(
    val atIndex: Int,
    val block: StudyBlock,
) : EditCommand {
    override val description = "Insertar bloque"
    override fun apply(doc: StudyDoc): StudyDoc {
        val safeIndex = atIndex.coerceIn(0, doc.blocks.size)
        if (doc.blocks.any { it.id == block.id }) return doc
        val newBlocks = doc.blocks.toMutableList().apply { add(safeIndex, block) }
        return doc.copy(blocks = newBlocks, version = doc.version + 1)
    }
    override fun invert(): EditCommand = DeleteBlockCommand(block.id)
}

data class DeleteBlockCommand(
    val blockId: BlockId,
) : EditCommand {
    override val description = "Borrar bloque"
    override fun apply(doc: StudyDoc): StudyDoc {
        val newBlocks = doc.blocks.filterNot { it.id == blockId }
        if (newBlocks.size == doc.blocks.size) return doc
        return doc.copy(blocks = newBlocks, version = doc.version + 1)
    }
    override fun invert(): EditCommand = RestoreBlockCommand(blockId, deletedBlock = null)
}

data class DeleteBlockCommandWithSnapshot(
    val block: StudyBlock,
    val originalIndex: Int,
) : EditCommand {
    override val description = "Borrar bloque"
    override fun apply(doc: StudyDoc): StudyDoc {
        if (doc.blocks.none { it.id == block.id }) return doc
        val newBlocks = doc.blocks.filterNot { it.id == block.id }
        return doc.copy(blocks = newBlocks, version = doc.version + 1)
    }
    override fun invert(): EditCommand = InsertBlockCommand(originalIndex, block)
}

data class RestoreBlockCommand(
    val blockId: BlockId,
    @Suppress("unused") val deletedBlock: StudyBlock?,
) : EditCommand {
    override val description = "Restaurar bloque"
    override fun apply(doc: StudyDoc): StudyDoc = doc
    override fun invert(): EditCommand = this
}

data class UpdateTitleCommand(
    val newTitle: String,
    val oldTitle: String,
) : EditCommand {
    override val description = "Cambiar titulo"
    override fun apply(doc: StudyDoc): StudyDoc =
        if (doc.title == newTitle) doc else doc.copy(title = newTitle, version = doc.version + 1)
    override fun invert(): EditCommand = UpdateTitleCommand(oldTitle, newTitle)
}

data class MoveBlockCommand(
    val fromIndex: Int,
    val toIndex: Int,
) : EditCommand {
    override val description = "Mover bloque"
    override fun apply(doc: StudyDoc): StudyDoc {
        if (fromIndex !in doc.blocks.indices) return doc
        val safeTo = toIndex.coerceIn(0, doc.blocks.size - 1)
        if (fromIndex == safeTo) return doc
        val blockId = doc.blocks[fromIndex].id
        val (newDoc, _) = StudyDocEngine.apply(doc, StudyOp.MoveBlock(blockId, safeTo))
        return newDoc
    }
    override fun invert(): EditCommand = MoveBlockCommand(toIndex, fromIndex)
}

data class ReplaceBlockCommand(
    val targetBlockId: BlockId,
    val newBlock: StudyBlock,
) : EditCommand {
    override val description = "Reemplazar bloque"
    override fun apply(doc: StudyDoc): StudyDoc {
        val idx = doc.blocks.indexOfFirst { it.id == targetBlockId }
        if (idx < 0) return doc
        val newBlocks = doc.blocks.toMutableList()
        newBlocks[idx] = newBlock
        return doc.copy(blocks = newBlocks, version = doc.version + 1)
    }
    override fun invert(): EditCommand = RestoreOriginalBlockCommand(targetBlockId, this)
}

data class RestoreOriginalBlockCommand(
    val targetBlockId: BlockId,
    val forward: ReplaceBlockCommand,
) : EditCommand {
    override val description = "Restaurar bloque"
    override fun apply(doc: StudyDoc): StudyDoc = doc
    override fun invert(): EditCommand = forward
}

data class MergeBlocksCommand(
    val targetIndex: Int,
    val targetOriginalText: StyledText,
    val sourceIndex: Int,
    val sourceOriginalText: StyledText,
) : EditCommand {
    override val description = "Fusionar bloques"
    override fun apply(doc: StudyDoc): StudyDoc {
        if (targetIndex !in doc.blocks.indices) return doc
        if (sourceIndex !in doc.blocks.indices) return doc
        if (sourceIndex == targetIndex) return doc
        val target = doc.blocks[targetIndex]
        val source = doc.blocks[sourceIndex]
        if (target !is StudyBlock.Paragraph) return doc
        if (source !is StudyBlock.Paragraph) return doc
        if (source.id == target.id) return doc
        val merged = target.copy(text = target.text.withPlainText(source.text.plain()))
        val newBlocks = doc.blocks.toMutableList()
        newBlocks[targetIndex] = merged
        newBlocks.removeAt(sourceIndex)
        return doc.copy(blocks = newBlocks, version = doc.version + 1)
    }
    override fun invert(): EditCommand = SplitBlocksCommand(
        targetIndex = targetIndex,
        targetNewText = targetOriginalText,
        insertedBlock = StudyBlock.Paragraph(id = BlockId(""), text = sourceOriginalText),
    )
}

data class SplitBlocksCommand(
    val targetIndex: Int,
    val targetNewText: StyledText,
    val insertedBlock: StudyBlock,
) : EditCommand {
    override val description = "Dividir bloque"
    override fun apply(doc: StudyDoc): StudyDoc {
        if (targetIndex !in doc.blocks.indices) return doc
        val target = doc.blocks[targetIndex]
        if (target !is StudyBlock.Paragraph) return doc
        val newTarget = target.copy(text = targetNewText)
        val newBlocks = doc.blocks.toMutableList()
        newBlocks[targetIndex] = newTarget
        newBlocks.add(targetIndex + 1, insertedBlock)
        return doc.copy(blocks = newBlocks, version = doc.version + 1)
    }
    override fun invert(): EditCommand = MergeBlocksCommand(
        targetIndex = targetIndex,
        targetOriginalText = targetNewText,
        sourceIndex = targetIndex + 1,
        sourceOriginalText = insertedBlock.let { (it as? StudyBlock.Paragraph)?.text ?: StyledText.Empty },
    )
}

private fun StyledText.withPlainText(append: String): StyledText {
    if (append.isEmpty()) return this
    val current = plain()
    return StyledText.plain(current + append)
}

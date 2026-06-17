package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.StudyBlockNode

/**
 * Operación de estudio que se puede deshacer/rehacer.
 */
sealed interface StudyOperation {
    val timestamp: Long

    data class BlocksChanged(
        val oldBlocks: List<StudyBlockNode>,
        val newBlocks: List<StudyBlockNode>,
        override val timestamp: Long = System.currentTimeMillis()
    ) : StudyOperation

    data class TextEdited(
        val blockId: String,
        val oldText: String,
        val newText: String,
        val source: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : StudyOperation

    data class BlockInserted(
        val block: StudyBlockNode,
        val position: Int,
        override val timestamp: Long = System.currentTimeMillis()
    ) : StudyOperation

    data class BlockDeleted(
        val block: StudyBlockNode,
        val position: Int,
        override val timestamp: Long = System.currentTimeMillis()
    ) : StudyOperation

    data class MetadataChanged(
        val oldTitle: String,
        val newTitle: String,
        val oldTags: List<String>,
        val newTags: List<String>,
        override val timestamp: Long = System.currentTimeMillis()
    ) : StudyOperation

    /**
     * Invierte la operación para undo.
     */
    fun invert(): StudyOperation = when (this) {
        is BlocksChanged -> copy(oldBlocks = newBlocks, newBlocks = oldBlocks)
        is TextEdited -> copy(oldText = newText, newText = oldText)
        is BlockInserted -> BlockDeleted(block = block, position = position)
        is BlockDeleted -> BlockInserted(block = block, position = position)
        is MetadataChanged -> copy(
            oldTitle = newTitle,
            newTitle = oldTitle,
            oldTags = newTags,
            newTags = oldTags
        )
    }
}

/**
 * Gestor de Undo/Redo para el modo estudio.
 * Implementa undo/redo incremental basado en operaciones, no en snapshots completos.
 */
class StudyUndoManager(private val maxSize: Int = 50) {

    private val undoStack = ArrayDeque<StudyOperation>(maxSize)
    private val redoStack = ArrayDeque<StudyOperation>(maxSize)

    /**
     * Registra una nueva operación para poder deshacerla.
     */
    fun recordOperation(operation: StudyOperation) {
        undoStack.addLast(operation)
        if (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    /**
     * Deshace la última operación.
     * @return La operación invertida, o null si no hay nada para deshacer.
     */
    fun undo(): StudyOperation? {
        if (undoStack.isEmpty()) return null
        val operation = undoStack.removeLast()
        redoStack.addLast(operation)
        return operation.invert()
    }

    /**
     * Rehace la última operación deshecha.
     * @return La operación original, o null si no hay nada para rehacer.
     */
    fun redo(): StudyOperation? {
        if (redoStack.isEmpty()) return null
        val operation = redoStack.removeLast()
        undoStack.addLast(operation)
        return operation
    }

    /**
     * Verifica si hay operaciones para deshacer.
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()

    /**
     * Verifica si hay operaciones para rehacer.
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * Obtiene el número de operaciones disponibles para deshacer.
     */
    fun undoCount(): Int = undoStack.size

    /**
     * Obtiene el número de operaciones disponibles para rehacer.
     */
    fun redoCount(): Int = redoStack.size

    /**
     * Limpia todo el historial de undo/redo.
     */
    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * Obtiene una descripción de la siguiente operación de undo (para UI).
     */
    fun peekUndoDescription(): String? {
        val operation = undoStack.lastOrNull() ?: return null
        return when (operation) {
            is StudyOperation.BlocksChanged -> "Cambios en bloques"
            is StudyOperation.TextEdited -> "Editar texto"
            is StudyOperation.BlockInserted -> "Insertar bloque"
            is StudyOperation.BlockDeleted -> "Eliminar bloque"
            is StudyOperation.MetadataChanged -> "Cambiar metadatos"
        }
    }

    /**
     * Obtiene una descripción de la siguiente operación de redo (para UI).
     */
    fun peekRedoDescription(): String? {
        val operation = redoStack.lastOrNull() ?: return null
        return when (operation) {
            is StudyOperation.BlocksChanged -> "Rehacer cambios"
            is StudyOperation.TextEdited -> "Rehacer edición"
            is StudyOperation.BlockInserted -> "Rehacer inserción"
            is StudyOperation.BlockDeleted -> "Rehacer eliminación"
            is StudyOperation.MetadataChanged -> "Rehacer metadatos"
        }
    }
}

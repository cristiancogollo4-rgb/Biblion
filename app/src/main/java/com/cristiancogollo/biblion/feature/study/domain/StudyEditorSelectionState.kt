package com.cristiancogollo.biblion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado de selección del editor de estudio.
 * Extrae el estado local pesado de StudyEditorScreen para mejor manejo y testing.
 */
class StudyEditorSelectionState {

    var activeBlockId by mutableStateOf<String?>(null)
        private set

    var activeRole by mutableStateOf("paragraph")
        private set

    var activeAlignment by mutableStateOf("start")
        private set

    var activeSource by mutableStateOf("main")
        private set

    var selectionStart by mutableIntStateOf(0)
        private set

    var selectionEnd by mutableIntStateOf(0)
        private set

    var selectedText by mutableStateOf("")
        private set

    var hasActiveSelection by mutableStateOf(false)
        private set

    /**
     * Actualiza toda la información de selección de una vez.
     */
    fun updateSelection(
        blockId: String?,
        role: String = "paragraph",
        alignment: String = "start",
        source: String = "main",
        start: Int = 0,
        end: Int = 0,
        text: String = ""
    ) {
        activeBlockId = blockId
        activeRole = role
        activeAlignment = alignment
        activeSource = source
        selectionStart = start
        selectionEnd = end
        selectedText = text
        hasActiveSelection = blockId != null && (start != end || text.isNotBlank())
    }

    /**
     * Actualiza solo el rango de selección.
     */
    fun updateRange(start: Int, end: Int, text: String = "") {
        selectionStart = start
        selectionEnd = end
        selectedText = text
        hasActiveSelection = activeBlockId != null && (start != end || text.isNotBlank())
    }

    /**
     * Actualiza solo el bloque activo y su rol.
     */
    fun updateActiveBlock(blockId: String?, role: String = "paragraph", alignment: String = "start") {
        activeBlockId = blockId
        activeRole = role
        activeAlignment = alignment
    }

    /**
     * Actualiza la fuente activa (main/parallel).
     */
    fun updateSource(source: String) {
        activeSource = source
    }

    /**
     * Actualiza el contenido de texto activo (para tracking del editor).
     */
    fun updateActiveTextContent(text: String) {
        // Este método existe para compatibilidad con el editor
        // El contenido real se mantiene en el estado local del editor
    }

    /**
     * Limpia completamente el estado de selección.
     */
    fun clear() {
        activeBlockId = null
        activeRole = "paragraph"
        activeAlignment = "start"
        activeSource = "main"
        selectionStart = 0
        selectionEnd = 0
        selectedText = ""
        hasActiveSelection = false
    }

    /**
     * Verifica si hay una selección activa válida.
     */
    fun isValidSelection(): Boolean {
        return activeBlockId != null && selectionStart < selectionEnd && selectedText.isNotBlank()
    }

    /**
     * Obtiene el rango de selección actual.
     */
    fun getSelectionRange(): Pair<Int, Int> {
        return selectionStart to selectionEnd
    }

    /**
     * Verifica si la selección es en la columna paralela.
     */
    fun isParallelSource(): Boolean {
        return activeSource == "parallel"
    }

    /**
     * Obtiene un resumen del estado actual para debugging.
     */
    fun toDebugString(): String {
        return buildString {
            append("BlockId: $activeBlockId, ")
            append("Role: $activeRole, ")
            append("Alignment: $activeAlignment, ")
            append("Source: $activeSource, ")
            append("Selection: [$selectionStart-$selectionEnd], ")
            append("Text: '${selectedText.take(20)}...', ")
            append("HasSelection: $hasActiveSelection")
        }
    }
}

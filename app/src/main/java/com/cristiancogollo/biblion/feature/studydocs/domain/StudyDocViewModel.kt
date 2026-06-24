package com.cristiancogollo.biblion.feature.studydocs.domain

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.engine.EditorMode
import com.cristiancogollo.biblion.feature.studydocs.engine.ListType
import com.cristiancogollo.biblion.feature.studydocs.engine.OpResult
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyDocEngine
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyOp
import com.cristiancogollo.biblion.feature.studydocs.engine.TextStyleKind
import com.cristiancogollo.biblion.feature.studydocs.engine.toggleList
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.TextStylePatch
import com.cristiancogollo.biblion.feature.studydocs.model.isTextEditable
import com.cristiancogollo.biblion.feature.studydocs.model.isList
import com.cristiancogollo.biblion.feature.studydocs.model.isTextEditable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

data class StudyEditorUiState(
    val doc: StudyDoc = StudyDoc.empty(),
    val selectedBlockId: String? = null,
    val isLoading: Boolean = false,
    val lastError: String? = null,
    val lastSavedAt: Long? = null,
    val isSaving: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoDescription: String? = null,
    val redoDescription: String? = null,
    val historyEntries: Int = 0,
    val currentPage: Int = 0,
    val wasJustCreated: Boolean = false,
)

class StudyDocViewModel(private val repository: StudyDocRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyEditorUiState())
    val uiState: StateFlow<StudyEditorUiState> = _uiState.asStateFlow()

    private val commandStack = EditCommandStack()
    val blockTextStates: SnapshotStateMap<BlockId, TextFieldValue> = mutableStateMapOf()

    /**
     * Alineacion por bloque (efimero, no persistido). Una sola
     * alineacion por bloque segun opcion c del plan. Se aplica
     * desde el UI via [cycleAlignment] leyendo de [blockAlignments].
     */
    val blockAlignments: SnapshotStateMap<BlockId, androidx.compose.ui.text.style.TextAlign> =
        mutableStateMapOf()

    private val _lastFocusedBlockId = MutableStateFlow<BlockId?>(null)
    val lastFocusedBlockId: StateFlow<BlockId?> = _lastFocusedBlockId.asStateFlow()

    private val _editorMode = MutableStateFlow<EditorMode>(EditorMode.Single)
    val editorMode: StateFlow<EditorMode> = _editorMode.asStateFlow()

    val activeRangeByBlock: SnapshotStateMap<BlockId, IntRange?> = mutableStateMapOf()

    fun setEditorMode(mode: EditorMode) { _editorMode.value = mode }
    fun setActiveRange(blockId: BlockId, range: IntRange?) {
        if (range == null) activeRangeByBlock.remove(blockId) else activeRangeByBlock[blockId] = range
    }
    fun onBlockFocused(blockId: BlockId) { _lastFocusedBlockId.value = blockId }

    fun newDraft() {
        // Documento de prueba con 20 parrafos para garantizar
        // que el LazyColumn tenga contenido suficiente para scrollear.
        val blocks = (1..20).map { i ->
            StudyBlock.Paragraph(
                text = StyledText.plain("Parrafo $i. Este es un texto de prueba para verificar que el scroll del editor funciona correctamente."),
            )
        }
        val doc = StudyDoc(blocks = blocks)
        blocks.forEach { block ->
            blockTextStates[block.id] = TextFieldValue(
                annotatedString = AnnotatedString(""),
                selection = TextRange(0),
            )
        }
        val firstBlockId = blocks.firstOrNull()?.id
        _uiState.value = StudyEditorUiState(
            doc = doc,
            selectedBlockId = firstBlockId?.value,
            wasJustCreated = true,
        )
    }

    fun clearJustCreatedFlag() { _uiState.update { it.copy(wasJustCreated = false) } }

    fun selectBlock(blockId: String?) { _uiState.update { it.copy(selectedBlockId = blockId) } }
    fun setCurrentPage(page: Int) { _uiState.update { it.copy(currentPage = page) } }

    fun applyOp(op: StudyOp) {
        val (newDoc, result) = StudyDocEngine.apply(_uiState.value.doc, op)
        if (result.isSuccess) {
            _uiState.update { state -> state.copy(doc = newDoc, lastError = null) }
        } else {
            _uiState.update { it.copy(lastError = (result as OpResult.Failed).reason) }
        }
    }

    fun executeCommand(command: EditCommand) {
        val current = _uiState.value
        val newDoc = commandStack.execute(command, current.doc)
        _uiState.update { it.copy(doc = newDoc, lastError = null) }
        refreshUndoRedoState()
        pruneOrphanTextStates(newDoc)
    }

    fun undo() {
        val newDoc = commandStack.undo(_uiState.value.doc)
        _uiState.update { it.copy(doc = newDoc) }
        refreshUndoRedoState()
    }

    fun redo() {
        val newDoc = commandStack.redo(_uiState.value.doc)
        _uiState.update { it.copy(doc = newDoc) }
        refreshUndoRedoState()
    }

    private fun refreshUndoRedoState() {
        _uiState.update {
            it.copy(
                canUndo = commandStack.canUndo,
                canRedo = commandStack.canRedo,
                undoDescription = commandStack.undoDescription,
                redoDescription = commandStack.redoDescription,
            )
        }
    }

    private fun pruneOrphanTextStates(doc: StudyDoc) {
        val present = doc.blocks.map { it.id }.toSet()
        val orphans = blockTextStates.keys.filter { it !in present }
        orphans.forEach { blockTextStates.remove(it) }
    }

    fun appendListItem(blockIndex: Int) {
        val current = _uiState.value.doc
        if (blockIndex !in current.blocks.indices) return
        val block = current.blocks[blockIndex]
        if (!block.isList) return
        val updated = when (block) {
            is StudyBlock.BulletList -> block.copy(items = block.items + StyledText.Empty)
            is StudyBlock.NumberedList -> block.copy(items = block.items + StyledText.Empty)
            else -> return
        }
        executeCommand(ReplaceBlockCommand(block.id, updated))
    }

    /**
     * Cicla la alineacion de un bloque: LEFT -> CENTER -> RIGHT -> JUSTIFY -> LEFT.
     * Solo aplica a bloques de texto editable (Paragraph, Heading, Quote, etc).
     * El cambio es efimero (no se persiste en el modelo); solo vive en [blockAlignments].
     */
    fun cycleAlignment(blockId: BlockId? = null) {
        val targetId = blockId
            ?: _uiState.value.selectedBlockId?.let { BlockId(it) }
            ?: _lastFocusedBlockId.value
            ?: return
        val block = _uiState.value.doc.blocks.firstOrNull { it.id == targetId } ?: return
        if (!block.isTextEditable) return
        val current = blockAlignments[targetId]
        blockAlignments[targetId] = com.cristiancogollo.biblion.feature.studydocs.engine.AlignmentCycle.next(current)
    }

    fun applyStyleAtSelection(
        patch: TextStylePatch,
        blockId: BlockId? = null,
    ) {
        val targetId = blockId
            ?: _uiState.value.selectedBlockId?.let { BlockId(it) }
            ?: _lastFocusedBlockId.value
            ?: return
        val range = activeRangeByBlock[targetId] ?: return
        applyOp(StudyOp.ApplyStyle(targetId, range, patch))
    }

    fun applyClearStyle(blockId: BlockId, range: IntRange, kind: TextStyleKind) {
        applyOp(StudyOp.ClearStyle(blockId, range, kind))
    }

    fun toggleListOnTarget(targetType: ListType, blockId: BlockId? = null) {
        val targetId = blockId
            ?: _uiState.value.selectedBlockId?.let { BlockId(it) }
            ?: _lastFocusedBlockId.value
            ?: return
        val block = _uiState.value.doc.blocks.firstOrNull { it.id == targetId } ?: return
        val cmd = toggleList(block, targetType) ?: return
        executeCommand(cmd)
    }

    fun loadByRemoteId(remoteId: String) {
        _uiState.update { it.copy(isLoading = false, wasJustCreated = false) }
    }

    /**
     * Persiste el documento actual con el titulo y tags proporcionados.
     * Usado por SaveTeachingDialog para validar antes de guardar.
     */
    fun saveNow(title: String, tags: List<String>) {
        val current = _uiState.value.doc
        val cleanTitle = title.trim()
        val nextMetadata = current.metadata.copy(tags = tags)
        viewModelScope.launch {
            applyOp(StudyOp.UpdateTitle(cleanTitle))
            applyOp(StudyOp.UpdateMetadata(nextMetadata))
            repository.save(_uiState.value.doc)
            _uiState.update { it.copy(lastSavedAt = System.currentTimeMillis()) }
        }
    }

    /**
     * Persistencia rapida sin pasar por el dialog. Usado internamente;
     * la UI debe preferir [saveNow] con titulo y tags.
     */
    fun saveNow() {
        viewModelScope.launch {
            repository.save(_uiState.value.doc)
            _uiState.update { it.copy(lastSavedAt = System.currentTimeMillis()) }
        }
    }

    fun updateTitle(newTitle: String) { applyOp(StudyOp.UpdateTitle(newTitle)) }

    class Factory(private val repository: StudyDocRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyDocViewModel(repository) as T
    }
}

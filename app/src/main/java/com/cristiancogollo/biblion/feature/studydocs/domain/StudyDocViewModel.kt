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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.cristiancogollo.biblion.feature.studydocs.model.isList

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
    val isMultiColumnEnabled: Boolean = false,
)

class StudyDocViewModel(private val repository: StudyDocRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyEditorUiState())
    val uiState: StateFlow<StudyEditorUiState> = _uiState.asStateFlow()

    private val commandStack = EditCommandStack()
    val blockTextStates: SnapshotStateMap<BlockId, TextFieldValue> = mutableStateMapOf()

    private var _autoSaveJob: Job? = null
    private var _isDocLoaded = false

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 3000L
    }

    /**
     * Alineacion por bloque (efimero, no persistido). Una sola
     * alineacion por bloque segun opcion c del plan. Se aplica
     * desde el UI via [cycleAlignment] leyendo de [blockAlignments].
     */
    val blockAlignments: SnapshotStateMap<BlockId, androidx.compose.ui.text.style.TextAlign> =
        mutableStateMapOf()

    private val _lastFocusedBlockId = MutableStateFlow<BlockId?>(null)
    val lastFocusedBlockId: StateFlow<BlockId?> = _lastFocusedBlockId.asStateFlow()

    private val _lastFocusedFieldKey = MutableStateFlow<String?>(null)
    val lastFocusedFieldKey: StateFlow<String?> = _lastFocusedFieldKey.asStateFlow()

    private val _editorMode = MutableStateFlow<EditorMode>(EditorMode.Single)
    val editorMode: StateFlow<EditorMode> = _editorMode.asStateFlow()

    val activeRangeByBlock: SnapshotStateMap<BlockId, IntRange?> = mutableStateMapOf()

    private val _fontSizeIndex = MutableStateFlow(com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig.DefaultFontSizeIndex)
    val fontSizeIndex: StateFlow<Int> = _fontSizeIndex.asStateFlow()

    val currentFontSize: Float
        get() = com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig.FontSizeLevels[_fontSizeIndex.value]

    fun increaseFontSize() {
        val current = _fontSizeIndex.value
        val levels = com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig.FontSizeLevels
        if (current < levels.lastIndex) {
            _fontSizeIndex.value = current + 1
        }
    }

    fun decreaseFontSize() {
        val current = _fontSizeIndex.value
        if (current > 0) {
            _fontSizeIndex.value = current - 1
        }
    }

    fun toggleMultiColumn() {
        _uiState.update { it.copy(isMultiColumnEnabled = !it.isMultiColumnEnabled) }
    }

    fun setEditorMode(mode: EditorMode) { _editorMode.value = mode }
    fun setActiveRange(blockId: BlockId, range: IntRange?) {
        if (range == null) activeRangeByBlock.remove(blockId) else activeRangeByBlock[blockId] = range
    }
    fun onBlockFocused(blockId: BlockId) { _lastFocusedBlockId.value = blockId }
    fun onFieldFocused(fieldKey: String) { _lastFocusedFieldKey.value = fieldKey }

    fun newDraft() {
        val blocks = listOf(
            StudyBlock.Paragraph(),
        )
        val doc = StudyDoc(blocks = blocks)
        blockTextStates.clear()
        _lastFocusedFieldKey.value = blocks.firstOrNull()?.id?.value
        blocks.forEach { block ->
            blockTextStates[block.id] = TextFieldValue(
                annotatedString = AnnotatedString(""),
                selection = TextRange(0),
            )
        }
        val firstBlockId = blocks.firstOrNull()?.id
        commandStack.clear()
        blockAlignments.clear()
        activeRangeByBlock.clear()
        _uiState.value = StudyEditorUiState(
            doc = doc,
            selectedBlockId = firstBlockId?.value,
            wasJustCreated = true,
        )
        _isDocLoaded = true
    }

    fun newFromTemplate(template: StudyTemplate) {
        val blocks = template.generateInitialBlocks()
        val metadata = template.generateMetadata()
        val doc = StudyDoc(
            title = template.name,
            blocks = blocks,
            metadata = metadata,
        )
        blockTextStates.clear()
        _lastFocusedFieldKey.value = blocks.firstOrNull()?.id?.value
        blocks.forEach { block ->
            when (block) {
                is StudyBlock.Paragraph -> {
                    blockTextStates[block.id] = TextFieldValue(
                        annotatedString = AnnotatedString(block.text.raw),
                        selection = TextRange(block.text.raw.length),
                    )
                }
                is StudyBlock.Heading -> {
                    blockTextStates[block.id] = TextFieldValue(
                        annotatedString = AnnotatedString(block.text.raw),
                        selection = TextRange(block.text.raw.length),
                    )
                }
                else -> Unit
            }
        }
        val firstBlockId = blocks.firstOrNull()?.id
        commandStack.clear()
        blockAlignments.clear()
        activeRangeByBlock.clear()
        _uiState.value = StudyEditorUiState(
            doc = doc,
            selectedBlockId = firstBlockId?.value,
            wasJustCreated = true,
        )
        _isDocLoaded = true
    }

    fun clearJustCreatedFlag() { _uiState.update { it.copy(wasJustCreated = false) } }

    fun selectBlock(blockId: String?) { _uiState.update { it.copy(selectedBlockId = blockId) } }
    fun setCurrentPage(page: Int) { _uiState.update { it.copy(currentPage = page) } }

    fun applyOp(op: StudyOp) {
        val (newDoc, result) = StudyDocEngine.apply(_uiState.value.doc, op)
        if (result.isSuccess) {
            _uiState.update { state -> state.copy(doc = newDoc, lastError = null) }
            scheduleAutoSave()
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
        scheduleAutoSave()
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
        val presentBlockIds = doc.blocks.map { it.id }.toSet()
        val validKeys = buildSet {
            addAll(presentBlockIds)
            doc.blocks.forEach { block ->
                when (block) {
                    is StudyBlock.BulletList -> block.items.indices.forEach { index ->
                        add(BlockId("${block.id.value}:item:$index"))
                    }
                    is StudyBlock.NumberedList -> block.items.indices.forEach { index ->
                        add(BlockId("${block.id.value}:item:$index"))
                    }
                    else -> Unit
                }
            }
        }
        val orphans = blockTextStates.keys.filter { it !in validKeys }
        orphans.forEach { blockTextStates.remove(it) }
        if (_lastFocusedFieldKey.value != null && BlockId(_lastFocusedFieldKey.value!!) !in validKeys) {
            _lastFocusedFieldKey.value = null
        }
    }

    fun appendListItem(blockIndex: Int) {
        val current = _uiState.value.doc
        if (blockIndex !in current.blocks.indices) return
        val block = current.blocks[blockIndex]
        if (!block.isList) return
        val updated = when (block) {
            is StudyBlock.BulletList -> block.copy(items = block.items + StyledText.Empty)
            is StudyBlock.NumberedList -> block.copy(items = block.items + StyledText.Empty)
            is StudyBlock.TodoList -> block.copy(items = block.items + StudyBlock.TodoList.TodoItem())
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
        _autoSaveJob?.cancel()
        _isDocLoaded = true
        _uiState.update { it.copy(isLoading = false, wasJustCreated = false) }
    }

    private fun scheduleAutoSave() {
        if (!_isDocLoaded) return
        _autoSaveJob?.cancel()
        _autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            val currentDoc = _uiState.value.doc
            if (currentDoc.title.isNotBlank()) {
                repository.save(currentDoc)
                _uiState.update { it.copy(lastSavedAt = System.currentTimeMillis()) }
            }
        }
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

    private val _findQuery = MutableStateFlow("")
    val findQuery: StateFlow<String> = _findQuery.asStateFlow()

    private val _findMatches = MutableStateFlow<List<StudyDocEngine.TextHit>>(emptyList())
    val findMatches: StateFlow<List<StudyDocEngine.TextHit>> = _findMatches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(0)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    fun updateFindQuery(query: String) {
        _findQuery.value = query
        if (query.isEmpty()) {
            _findMatches.value = emptyList()
            _currentMatchIndex.value = 0
        } else {
            val hits = StudyDocEngine.findText(_uiState.value.doc, query)
            _findMatches.value = hits
            _currentMatchIndex.value = 0
        }
    }

    fun findNext() {
        val matches = _findMatches.value
        if (matches.isNotEmpty()) {
            _currentMatchIndex.value = (_currentMatchIndex.value + 1) % matches.size
            scrollToMatch(matches[_currentMatchIndex.value])
        }
    }

    fun findPrevious() {
        val matches = _findMatches.value
        if (matches.isNotEmpty()) {
            _currentMatchIndex.value = if (_currentMatchIndex.value == 0) matches.size - 1 else _currentMatchIndex.value - 1
            scrollToMatch(matches[_currentMatchIndex.value])
        }
    }

    private fun scrollToMatch(hit: StudyDocEngine.TextHit) {
        selectBlock(hit.blockId.value)
    }

    fun replaceCurrent(replacement: String) {
        val matches = _findMatches.value
        val index = _currentMatchIndex.value
        if (matches.isEmpty() || index >= matches.size) return

        val hit = matches[index]
        val block = _uiState.value.doc.blocks.getOrNull(hit.blockIndex) ?: return

        val blockId = block.id
        val currentText = when (block) {
            is StudyBlock.Paragraph -> block.text
            is StudyBlock.Heading -> block.text
            is StudyBlock.Quote -> block.text
            is StudyBlock.Note -> block.text
            is StudyBlock.Reflection -> block.text
            is StudyBlock.Callout -> block.text
            is StudyBlock.Verse -> block.primaryText
            else -> return
        }

        val newText = currentText.raw.replaceRange(hit.start, hit.endExclusive, replacement)
        val newStyledText = com.cristiancogollo.biblion.feature.studydocs.model.StyledText(newText, emptyList())

        applyOp(StudyOp.EditText(blockId, newStyledText))

        updateFindQuery(_findQuery.value)
    }

    fun replaceAll(replacement: String) {
        val query = _findQuery.value
        if (query.isEmpty()) return

        var count = 0
        val blocks = _uiState.value.doc.blocks

        for (block in blocks) {
            val blockId = block.id
            val currentText = when (block) {
                is StudyBlock.Paragraph -> block.text
                is StudyBlock.Heading -> block.text
                is StudyBlock.Quote -> block.text
                is StudyBlock.Note -> block.text
                is StudyBlock.Reflection -> block.text
                is StudyBlock.Callout -> block.text
                is StudyBlock.Verse -> block.primaryText
                else -> continue
            }

            if (currentText.raw.contains(query)) {
                val newText = currentText.raw.replace(query, replacement)
                val newStyledText = com.cristiancogollo.biblion.feature.studydocs.model.StyledText(newText, emptyList())
                applyOp(StudyOp.EditText(blockId, newStyledText))
                count++
            }
        }

        updateFindQuery(_findQuery.value)
    }

    class Factory(private val repository: StudyDocRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyDocViewModel(repository) as T
    }
}

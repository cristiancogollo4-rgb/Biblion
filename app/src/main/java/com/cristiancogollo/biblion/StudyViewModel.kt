package com.cristiancogollo.biblion

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


data class CitationInsertRequest(
    val id: String,
    val reference: String,
    val text: String,
    val version: String = "rv1960",
    val includeFullText: Boolean = true
)

data class StudyUiState(
    val notebooks: List<StudyNotebookEntity> = emptyList(),
    val selectedNotebookId: Long? = null,
    val studies: List<StudyEntity> = emptyList(),
    val allStudies: List<StudyEntity> = emptyList(),
    val selectedStudyId: Long? = null,
    val title: String = "",
    val richHtml: String = "",
    val blocks: List<StudyBlockNode> = emptyList(),
    val focusMode: Boolean = false,
    val contextualMenuVisible: Boolean = false,
    val popupVisible: Boolean = false,
    val keyboardOpen: Boolean = false,
    val hasActiveSelection: Boolean = false,
    val selectionFontSizeSp: Float = 18f,
    val pendingCitations: List<CitationInsertRequest> = emptyList(),
    val globalVersion: String = "rv1960",
    val tags: List<String> = emptyList(),
    val isDraftMode: Boolean = false,
    val loadErrorMessage: String? = null
)

sealed interface StudyIntent {
    data class SelectNotebook(val notebookId: Long) : StudyIntent
    data class SelectStudy(val studyId: Long) : StudyIntent
    data class DeleteStudy(val studyId: Long) : StudyIntent
    data class UpdateTitle(val title: String) : StudyIntent
    data class UpdateRichHtml(val html: String) : StudyIntent
    data class ToggleFocusMode(val enabled: Boolean) : StudyIntent
    data class SetPopupVisible(val visible: Boolean) : StudyIntent
    data class SetContextMenuVisible(val visible: Boolean) : StudyIntent
    data class SetSelectionActive(val active: Boolean) : StudyIntent
    data class SetKeyboardOpen(val open: Boolean) : StudyIntent
    data class UpdateSelectionFontFromEditor(val currentSp: Float) : StudyIntent
    data object IncreaseSelectionFont : StudyIntent
    data object DecreaseSelectionFont : StudyIntent
    data class AddAudioBlock(val uri: String, val title: String) : StudyIntent
    data class AddImageBlock(val uri: String, val caption: String) : StudyIntent
    data class ChangeVersion(val version: String) : StudyIntent
    data object Undo : StudyIntent
    data object Redo : StudyIntent
    data object ExportPdf : StudyIntent
    data object SaveStudy : StudyIntent
    data class SaveStudyWithMetadata(val title: String, val tags: List<String>) : StudyIntent
    data object Save : StudyIntent
    data object CreateNewStudy : StudyIntent
    data object StartNewDraft : StudyIntent
}

class StudyViewModel(
    application: Application,
    private val dao: StudyDao = StudyDatabase.getInstance(application).studyDao(),
    private val autoSaveDebounceMs: Long = AUTOSAVE_DEBOUNCE_MS
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "StudyViewModel"
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; classDiscriminator = "nodeType" }

    private fun normalizeVersion(version: String): String {
        return when (version.trim().lowercase()) {
            "rvr1960" -> "rv1960"
            else -> version.trim().lowercase().ifBlank { "rv1960" }
        }
    }

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    private val notebooksFlow = dao.observeNotebooks().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val selectedNotebookFlow = MutableStateFlow<Long?>(null)

    private var lastSavedSignature: String? = null
    internal var buildSignatureOverride: ((StudyUiState) -> String)? = null

    init {
        startAutoSaveObserver()
        viewModelScope.launch {
            ensureSeedData()
        }
        viewModelScope.launch {
            notebooksFlow.collect { notebooks ->
                val selected = _state.value.selectedNotebookId ?: notebooks.firstOrNull()?.id
                _state.value = _state.value.copy(notebooks = notebooks, selectedNotebookId = selected)
                selectedNotebookFlow.value = selected
            }
        }
        viewModelScope.launch {
            selectedNotebookFlow
                .filter { it != null }
                .map { it!! }
                .distinctUntilChanged()
                .flatMapLatest { notebookId -> dao.observeStudies(notebookId) }
                .collect { studies ->
                    val currentSelected = if (_state.value.isDraftMode) {
                        null
                    } else {
                        _state.value.selectedStudyId?.takeIf { id -> studies.any { it.id == id } } ?: studies.firstOrNull()?.id
                    }
                    _state.value = _state.value.copy(studies = studies, selectedStudyId = currentSelected)
                    currentSelected?.let { loadStudy(it) }
                }
        }
        viewModelScope.launch {
            dao.observeAllStudies().collect { all ->
                _state.value = _state.value.copy(allStudies = all)
            }
        }
    }

    fun process(intent: StudyIntent) {
        when (intent) {
            is StudyIntent.SelectNotebook -> {
                _state.value = _state.value.copy(selectedNotebookId = intent.notebookId, selectedStudyId = null)
                selectedNotebookFlow.value = intent.notebookId
            }
            is StudyIntent.SelectStudy -> viewModelScope.launch { loadStudy(intent.studyId) }
            is StudyIntent.DeleteStudy -> {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        dao.deleteStudy(intent.studyId)
                        dao.deleteCitationsForStudy(intent.studyId)
                    }
                }
            }
            is StudyIntent.UpdateTitle -> {
                _state.value = _state.value.copy(title = intent.title)
            }
            is StudyIntent.UpdateRichHtml -> {
                undoStack.addLast(_state.value.richHtml)
                _state.value = _state.value.copy(
                    richHtml = intent.html,
                    blocks = rebuildBlocks(intent.html, _state.value.blocks)
                )
                redoStack.clear()
            }
            is StudyIntent.ToggleFocusMode -> _state.value = _state.value.copy(focusMode = intent.enabled)
            is StudyIntent.SetPopupVisible -> _state.value = _state.value.copy(popupVisible = intent.visible)
            is StudyIntent.SetContextMenuVisible -> _state.value = _state.value.copy(contextualMenuVisible = intent.visible)
            is StudyIntent.SetSelectionActive -> _state.value = _state.value.copy(hasActiveSelection = intent.active)
            is StudyIntent.SetKeyboardOpen -> _state.value = _state.value.copy(keyboardOpen = intent.open)
            is StudyIntent.UpdateSelectionFontFromEditor -> {
                _state.value = _state.value.copy(selectionFontSizeSp = intent.currentSp.coerceIn(12f, 46f))
            }
            StudyIntent.IncreaseSelectionFont -> {
                _state.value = _state.value.copy(selectionFontSizeSp = (_state.value.selectionFontSizeSp + 2f).coerceAtMost(46f))
            }
            StudyIntent.DecreaseSelectionFont -> {
                _state.value = _state.value.copy(selectionFontSizeSp = (_state.value.selectionFontSizeSp - 2f).coerceAtLeast(12f))
            }
            is StudyIntent.AddAudioBlock -> {
                _state.value = _state.value.copy(
                    blocks = _state.value.blocks + StudyBlockNode.Audio(intent.uri, intent.title)
                )
            }
            is StudyIntent.AddImageBlock -> {
                _state.value = _state.value.copy(
                    blocks = _state.value.blocks + StudyBlockNode.Image(intent.uri, intent.caption)
                )
            }
            is StudyIntent.ChangeVersion -> {
                _state.value = _state.value.copy(globalVersion = normalizeVersion(intent.version))
            }
            StudyIntent.Undo -> if (undoStack.isNotEmpty()) {
                val previous = undoStack.removeLast()
                redoStack.addLast(_state.value.richHtml)
                _state.value = _state.value.copy(richHtml = previous, blocks = rebuildBlocks(previous, _state.value.blocks))
            }
            StudyIntent.Redo -> if (redoStack.isNotEmpty()) {
                val next = redoStack.removeLast()
                undoStack.addLast(_state.value.richHtml)
                _state.value = _state.value.copy(richHtml = next, blocks = rebuildBlocks(next, _state.value.blocks))
            }
            StudyIntent.ExportPdf -> exportPdfStub()
            StudyIntent.SaveStudy, StudyIntent.Save -> saveStudyNow()
            is StudyIntent.SaveStudyWithMetadata -> {
                _state.value = _state.value.copy(
                    title = intent.title.trim(),
                    tags = intent.tags,
                    isDraftMode = false
                )
                saveStudyNow()
            }
            StudyIntent.CreateNewStudy -> {
                viewModelScope.launch {
                    val now = System.currentTimeMillis()
                    val notebookId = _state.value.selectedNotebookId ?: withContext(Dispatchers.IO) {
                        dao.observeNotebooks().firstOrNull()?.firstOrNull()?.id
                    } ?: return@launch
                    val emptyDoc = json.encodeToString(SerializedStudyDocument())
                    val newId = withContext(Dispatchers.IO) {
                        dao.insertStudy(StudyEntity(title = "Nueva Enseñanza", notebookId = notebookId, contentSerialized = emptyDoc, createdAt = now, updatedAt = now))
                    }
                    loadStudy(newId)
                }
            }
            StudyIntent.StartNewDraft -> {
                _state.value = _state.value.copy(
                    selectedStudyId = null,
                    title = "",
                    richHtml = "",
                    blocks = emptyList(),
                    tags = emptyList(),
                    pendingCitations = emptyList(),
                    isDraftMode = true
                )
                lastSavedSignature = null
            }
        }
    }

    fun addCitation(reference: String, text: String, includeFullText: Boolean) {
        val parsed = parseReference(reference) ?: return
        val id = UUID.randomUUID().toString()
        val citation = StudyBlockNode.Citation(
            citationId = id,
            reference = parsed,
            text = text,
            version = _state.value.globalVersion,
            includeFullText = includeFullText
        )
        _state.value = _state.value.copy(
            blocks = _state.value.blocks + citation,
            pendingCitations = _state.value.pendingCitations + CitationInsertRequest(
                id = id,
                reference = reference,
                text = text,
                version = _state.value.globalVersion,
                includeFullText = includeFullText
            )
        )
    }

    fun consumePendingCitations(): List<CitationInsertRequest> {
        val pending = _state.value.pendingCitations
        _state.value = _state.value.copy(pendingCitations = emptyList())
        return pending
    }

    fun asBlockquoteText(request: CitationInsertRequest): String {
        return if (request.includeFullText) {
            "\n    \"${request.text}\"\n    — ${request.reference}\n"
        } else {
            "\n    — ${request.reference}\n"
        }
    }

    fun updateStudyMetadata(studyId: Long, title: String, tags: List<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = dao.getStudy(studyId) ?: return@withContext
                val document = runCatching {
                    json.decodeFromString<SerializedStudyDocument>(existing.contentSerialized)
                }.getOrDefault(SerializedStudyDocument())
                val updatedDoc = document.copy(tags = tags)
                dao.updateStudy(
                    existing.copy(
                        title = title,
                        contentSerialized = json.encodeToString(updatedDoc),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            if (_state.value.selectedStudyId == studyId) {
                _state.value = _state.value.copy(title = title, tags = tags)
            }
        }
    }

    fun saveStudyNow(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val saved = persistCurrentStudy()
            onComplete(saved)
        }
    }

    private suspend fun ensureSeedData() {
        withContext(Dispatchers.IO) {
            if (dao.getNotebookCount() == 0) {
                val now = System.currentTimeMillis()
                val notebookId = dao.insertNotebook(StudyNotebookEntity(title = "Mis Notas de Estudio", createdAt = now, updatedAt = now))
                val emptyDoc = json.encodeToString(SerializedStudyDocument())
                dao.insertStudy(StudyEntity(title = "Nueva Enseñanza", notebookId = notebookId, contentSerialized = emptyDoc, createdAt = now, updatedAt = now))
            }
        }
    }

    private suspend fun loadStudy(studyId: Long) {
        val study = withContext(Dispatchers.IO) { dao.getStudy(studyId) } ?: return
        try {
            val doc = runCatching { json.decodeFromString<SerializedStudyDocument>(study.contentSerialized) }
                .getOrDefault(SerializedStudyDocument())
            val firstRich = doc.blocks.filterIsInstance<StudyBlockNode.RichText>().firstOrNull()
            val newState = _state.value.copy(
                selectedStudyId = study.id,
                title = study.title,
                richHtml = firstRich?.html ?: "",
                blocks = doc.blocks,
                globalVersion = normalizeVersion(doc.globalVersion),
                tags = doc.tags,
                isDraftMode = false,
                loadErrorMessage = null
            )
            val newSignature = buildSignature(newState)
            _state.value = newState
            lastSavedSignature = newSignature
        } catch (e: Exception) {
            Log.e(TAG, "Error loading study: $studyId", e)
            _state.value = _state.value.copy(loadErrorMessage = "No se pudo cargar el estudio.")
        }
    }

    private fun startAutoSaveObserver() {
        viewModelScope.launch {
            state
                .map { current ->
                    val selectedId = current.selectedStudyId ?: return@map ""
                    val document = SerializedStudyDocument(
                        blocks = current.blocks,
                        globalVersion = current.globalVersion,
                        tags = current.tags
                    )
                    "$selectedId|${current.title}|${json.encodeToString(document)}"
                }
                .filter { it.isNotBlank() }
                .debounce(autoSaveDebounceMs)
                .distinctUntilChanged()
                .collect { signature ->
                    if (signature != lastSavedSignature) {
                        persistCurrentStudy()
                    }
                }
        }
    }

    private suspend fun persistCurrentStudy(): Boolean {
        val s = _state.value
        val notebookId = s.selectedNotebookId ?: withContext(Dispatchers.IO) {
            dao.observeNotebooks().firstOrNull()?.firstOrNull()?.id
        } ?: return false
        val now = System.currentTimeMillis()
        val document = SerializedStudyDocument(
            blocks = s.blocks,
            globalVersion = s.globalVersion,
            tags = s.tags
        )
        val studyId = s.selectedStudyId
        if (studyId == null) {
            val newId = withContext(Dispatchers.IO) {
                dao.insertStudy(
                    StudyEntity(
                        title = s.title.ifBlank { "Nueva Enseñanza" },
                        notebookId = notebookId,
                        contentSerialized = json.encodeToString(document),
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            val citations = s.blocks.filterIsInstance<StudyBlockNode.Citation>().map {
                LinkedCitationEntity(
                    estudioId = newId,
                    book = it.reference.book,
                    chapter = it.reference.chapter,
                    verseStart = it.reference.verseStart,
                    verseEnd = it.reference.verseEnd,
                    version = it.version,
                    positionMetadata = "inline"
                )
            }
            withContext(Dispatchers.IO) { dao.replaceCitations(newId, citations) }
            _state.value = _state.value.copy(selectedStudyId = newId, selectedNotebookId = notebookId, isDraftMode = false)
            lastSavedSignature = buildSignature(_state.value)
            return true
        }

        withContext(Dispatchers.IO) {
            val existing = dao.getStudy(studyId)
            dao.updateStudy(
                StudyEntity(
                    id = studyId,
                    title = s.title.ifBlank { "Sin título" },
                    notebookId = notebookId,
                    contentSerialized = json.encodeToString(document),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
        val citations = s.blocks.filterIsInstance<StudyBlockNode.Citation>().map {
            LinkedCitationEntity(
                estudioId = studyId,
                book = it.reference.book,
                chapter = it.reference.chapter,
                verseStart = it.reference.verseStart,
                verseEnd = it.reference.verseEnd,
                version = it.version,
                positionMetadata = "inline"
            )
        }
        withContext(Dispatchers.IO) { dao.replaceCitations(studyId, citations) }
        lastSavedSignature = buildSignature(_state.value)
        return true
    }

    fun preferredBookForStudy(study: StudyEntity): String? {
        val document = runCatching { json.decodeFromString<SerializedStudyDocument>(study.contentSerialized) }
            .getOrNull() ?: return null

        val citationBook = document.blocks
            .filterIsInstance<StudyBlockNode.Citation>()
            .firstOrNull()
            ?.reference
            ?.book
        if (!citationBook.isNullOrBlank()) return citationBook

        val richText = document.blocks.filterIsInstance<StudyBlockNode.RichText>().firstOrNull()?.html.orEmpty()
        return detectReferences(richText).firstOrNull()?.book
    }

    private fun buildSignature(state: StudyUiState): String {
        buildSignatureOverride?.let { return it(state) }
        val studyId = state.selectedStudyId ?: return ""
        val document = SerializedStudyDocument(
            blocks = state.blocks,
            globalVersion = state.globalVersion,
            tags = state.tags
        )
        return "$studyId|${state.title}|${json.encodeToString(document)}"
    }

    private fun rebuildBlocks(html: String, old: List<StudyBlockNode>): List<StudyBlockNode> {
        val refs = detectReferences(html)
        val nonText = old.filterNot { it is StudyBlockNode.RichText }
        return listOf(StudyBlockNode.RichText(html = html, references = refs)) + nonText
    }

    private fun detectReferences(text: String): List<BibleReferenceNode> {
        val regex = Regex("""([1-3]?\s?[A-Za-zÁÉÍÓÚáéíóúñÑ]+)\s+(\d+):(\d+)(?:-(\d+))?""")
        return regex.findAll(text).mapNotNull { m ->
            val book = m.groupValues[1].trim()
            val chapter = m.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val start = m.groupValues[3].toIntOrNull() ?: return@mapNotNull null
            val end = m.groupValues[4].toIntOrNull() ?: start
            BibleReferenceNode(book = book, chapter = chapter, verseStart = start, verseEnd = end)
        }.toList()
    }

    private fun parseReference(reference: String): BibleReferenceNode? = detectReferences(reference).firstOrNull()

    private fun exportPdfStub() {
        // Punto de extensión: implementación de exportación PDF elegante del estudio.
    }
}

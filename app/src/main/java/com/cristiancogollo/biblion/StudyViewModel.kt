package com.cristiancogollo.biblion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


data class CitationInsertRequest(
    val id: String,
    val reference: String,
    val text: String,
    val version: String = "rvr1960",
    val includeFullText: Boolean = true
)

data class StudyUiState(
    val notebooks: List<StudyNotebookEntity> = emptyList(),
    val selectedNotebookId: Long? = null,
    val studies: List<StudyEntity> = emptyList(),
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
    val globalVersion: String = "rvr1960"
)

sealed interface StudyIntent {
    data class SelectNotebook(val notebookId: Long) : StudyIntent
    data class SelectStudy(val studyId: Long) : StudyIntent
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
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = StudyDatabase.getInstance(application).studyDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; classDiscriminator = "nodeType" }

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    private val notebooksFlow = dao.observeNotebooks().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val selectedNotebookFlow = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            ensureSeedData()
        }
        viewModelScope.launch {
            notebooksFlow.collect { notebooks ->
                val selected = _state.value.selectedNotebookId ?: notebooks.firstOrNull()?.id
                _state.value = _state.value.copy(notebooks = notebooks, selectedNotebookId = selected)
                selectedNotebookFlow.value = selected
                selected?.let { observeStudies(it) }
            }
        }
        startAutoSave()
    }

    fun process(intent: StudyIntent) {
        when (intent) {
            is StudyIntent.SelectNotebook -> {
                _state.value = _state.value.copy(selectedNotebookId = intent.notebookId, selectedStudyId = null)
                selectedNotebookFlow.value = intent.notebookId
                viewModelScope.launch { observeStudies(intent.notebookId) }
            }
            is StudyIntent.SelectStudy -> viewModelScope.launch { loadStudy(intent.studyId) }
            is StudyIntent.UpdateTitle -> _state.value = _state.value.copy(title = intent.title)
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
            is StudyIntent.AddAudioBlock -> _state.value = _state.value.copy(
                blocks = _state.value.blocks + StudyBlockNode.Audio(intent.uri, intent.title)
            )
            is StudyIntent.AddImageBlock -> _state.value = _state.value.copy(
                blocks = _state.value.blocks + StudyBlockNode.Image(intent.uri, intent.caption)
            )
            is StudyIntent.ChangeVersion -> _state.value = _state.value.copy(globalVersion = intent.version)
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

    fun saveStudyNow(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val saved = persistCurrentStudy()
            onComplete(saved)
        }
    }

    private suspend fun ensureSeedData() {
        if (notebooksFlow.value.isEmpty()) {
            val now = System.currentTimeMillis()
            val notebookId = dao.insertNotebook(StudyNotebookEntity(title = "Estudio sobre la Fe", createdAt = now, updatedAt = now))
            dao.insertNotebook(StudyNotebookEntity(title = "Serie Romanos", createdAt = now, updatedAt = now))
            dao.insertNotebook(StudyNotebookEntity(title = "Predicaciones 2026", createdAt = now, updatedAt = now))
            val emptyDoc = json.encodeToString(SerializedStudyDocument())
            dao.insertStudy(StudyEntity(title = "Nuevo estudio", notebookId = notebookId, contentSerialized = emptyDoc, createdAt = now, updatedAt = now))
        }
    }

    private suspend fun observeStudies(notebookId: Long) {
        dao.observeStudies(notebookId).collect { studies ->
            val currentSelected = _state.value.selectedStudyId?.takeIf { id -> studies.any { it.id == id } } ?: studies.firstOrNull()?.id
            _state.value = _state.value.copy(studies = studies, selectedStudyId = currentSelected)
            currentSelected?.let { loadStudy(it) }
        }
    }

    private suspend fun loadStudy(studyId: Long) {
        val study = dao.getStudy(studyId) ?: return
        val doc = runCatching { json.decodeFromString<SerializedStudyDocument>(study.contentSerialized) }
            .getOrDefault(SerializedStudyDocument())
        val firstRich = doc.blocks.filterIsInstance<StudyBlockNode.RichText>().firstOrNull()
        _state.value = _state.value.copy(
            selectedStudyId = study.id,
            title = study.title,
            richHtml = firstRich?.html ?: "",
            blocks = doc.blocks,
            globalVersion = doc.globalVersion
        )
    }

    private fun startAutoSave() {
        viewModelScope.launch {
            while (true) {
                delay(7000)
                persistCurrentStudy()
            }
        }
    }

    private suspend fun persistCurrentStudy(): Boolean {
        val s = _state.value
        val id = s.selectedStudyId ?: return false
        val notebookId = s.selectedNotebookId ?: return false
        val now = System.currentTimeMillis()
        val document = SerializedStudyDocument(blocks = s.blocks, globalVersion = s.globalVersion)
        dao.updateStudy(
            StudyEntity(
                id = id,
                title = s.title.ifBlank { "Nuevo estudio" },
                notebookId = notebookId,
                contentSerialized = json.encodeToString(document),
                createdAt = now,
                updatedAt = now
            )
        )
        val citations = s.blocks.filterIsInstance<StudyBlockNode.Citation>().map {
            LinkedCitationEntity(
                estudioId = id,
                book = it.reference.book,
                chapter = it.reference.chapter,
                verseStart = it.reference.verseStart,
                verseEnd = it.reference.verseEnd,
                version = it.version,
                positionMetadata = "inline"
            )
        }
        dao.replaceCitations(id, citations)
        return true
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

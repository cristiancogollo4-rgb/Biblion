package com.cristiancogollo.biblion

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

data class StudySearchResult(
    val studyId: Long,
    val notebookId: Long,
    val title: String,
    val snippet: String,
    val updatedAt: Long
)

data class WorkspacePanelItem(
    val label: String,
    val detail: String
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
    val globalVersion: String = "rvr1960",
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val lastSavedAt: Long? = null,
    val saveError: String? = null,
    val searchQuery: String = "",
    val searchResults: List<StudySearchResult> = emptyList(),
    val exportMessage: String? = null,
    val workspaceItems: List<WorkspacePanelItem> = emptyList(),
    val recentReferences: List<String> = emptyList()
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
    data class UpdateSearchQuery(val query: String) : StudyIntent
    data object IncreaseSelectionFont : StudyIntent
    data object DecreaseSelectionFont : StudyIntent
    data class AddAudioBlock(val uri: String, val title: String) : StudyIntent
    data class AddImageBlock(val uri: String, val caption: String) : StudyIntent
    data class ChangeVersion(val version: String) : StudyIntent
    data object Undo : StudyIntent
    data object Redo : StudyIntent
    data object ExportPdf : StudyIntent
    data object ClearExportMessage : StudyIntent
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = StudyDatabase.getInstance(application).studyDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; classDiscriminator = "nodeType" }
    private val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var observeStudiesJob: Job? = null

    private val notebooksFlow = dao.observeNotebooks().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { ensureSeedData() }
        viewModelScope.launch {
            notebooksFlow.collect { notebooks ->
                val selected = _state.value.selectedNotebookId ?: notebooks.firstOrNull()?.id
                _state.update { it.copy(notebooks = notebooks, selectedNotebookId = selected) }
                selected?.let { notebookId -> observeStudies(notebookId) }
            }
        }
        startAutoSave()
    }

    fun process(intent: StudyIntent) {
        when (intent) {
            is StudyIntent.SelectNotebook -> {
                _state.update { it.copy(selectedNotebookId = intent.notebookId, selectedStudyId = null) }
                viewModelScope.launch { observeStudies(intent.notebookId) }
            }
            is StudyIntent.SelectStudy -> viewModelScope.launch { loadStudy(intent.studyId) }
            is StudyIntent.UpdateTitle -> updateEditableState(title = intent.title)
            is StudyIntent.UpdateRichHtml -> {
                if (intent.html == _state.value.richHtml) return
                undoStack.addLast(_state.value.richHtml)
                redoStack.clear()
                val rebuilt = rebuildBlocks(intent.html, _state.value.blocks)
                _state.update {
                    it.copy(
                        richHtml = intent.html,
                        blocks = rebuilt,
                        hasUnsavedChanges = true,
                        saveError = null,
                        workspaceItems = buildWorkspaceItems(rebuilt),
                        recentReferences = buildRecentReferences(rebuilt)
                    )
                }
            }
            is StudyIntent.ToggleFocusMode -> _state.update { it.copy(focusMode = intent.enabled) }
            is StudyIntent.SetPopupVisible -> _state.update { it.copy(popupVisible = intent.visible) }
            is StudyIntent.SetContextMenuVisible -> _state.update { it.copy(contextualMenuVisible = intent.visible) }
            is StudyIntent.SetSelectionActive -> _state.update { it.copy(hasActiveSelection = intent.active) }
            is StudyIntent.SetKeyboardOpen -> _state.update { it.copy(keyboardOpen = intent.open) }
            is StudyIntent.UpdateSelectionFontFromEditor -> {
                _state.update { it.copy(selectionFontSizeSp = intent.currentSp.coerceIn(12f, 46f)) }
            }
            is StudyIntent.UpdateSearchQuery -> {
                _state.update { it.copy(searchQuery = intent.query) }
                viewModelScope.launch { performSearch(intent.query) }
            }
            StudyIntent.IncreaseSelectionFont -> {
                _state.update { it.copy(selectionFontSizeSp = (it.selectionFontSizeSp + 2f).coerceAtMost(46f)) }
            }
            StudyIntent.DecreaseSelectionFont -> {
                _state.update { it.copy(selectionFontSizeSp = (it.selectionFontSizeSp - 2f).coerceAtLeast(12f)) }
            }
            is StudyIntent.AddAudioBlock -> appendNonTextBlock(StudyBlockNode.Audio(intent.uri, intent.title))
            is StudyIntent.AddImageBlock -> appendNonTextBlock(StudyBlockNode.Image(intent.uri, intent.caption))
            is StudyIntent.ChangeVersion -> _state.update { it.copy(globalVersion = intent.version, hasUnsavedChanges = true) }
            StudyIntent.Undo -> if (undoStack.isNotEmpty()) {
                val previous = undoStack.removeLast()
                redoStack.addLast(_state.value.richHtml)
                val rebuilt = rebuildBlocks(previous, _state.value.blocks)
                _state.update {
                    it.copy(
                        richHtml = previous,
                        blocks = rebuilt,
                        hasUnsavedChanges = true,
                        workspaceItems = buildWorkspaceItems(rebuilt),
                        recentReferences = buildRecentReferences(rebuilt)
                    )
                }
            }
            StudyIntent.Redo -> if (redoStack.isNotEmpty()) {
                val next = redoStack.removeLast()
                undoStack.addLast(_state.value.richHtml)
                val rebuilt = rebuildBlocks(next, _state.value.blocks)
                _state.update {
                    it.copy(
                        richHtml = next,
                        blocks = rebuilt,
                        hasUnsavedChanges = true,
                        workspaceItems = buildWorkspaceItems(rebuilt),
                        recentReferences = buildRecentReferences(rebuilt)
                    )
                }
            }
            StudyIntent.ExportPdf -> exportStudyAsTextFile()
            StudyIntent.ClearExportMessage -> _state.update { it.copy(exportMessage = null) }
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
        val updatedBlocks = _state.value.blocks + citation
        _state.update {
            it.copy(
                blocks = updatedBlocks,
                pendingCitations = it.pendingCitations + CitationInsertRequest(
                    id = id,
                    reference = reference,
                    text = text,
                    version = it.globalVersion,
                    includeFullText = includeFullText
                ),
                hasUnsavedChanges = true,
                workspaceItems = buildWorkspaceItems(updatedBlocks),
                recentReferences = buildRecentReferences(updatedBlocks)
            )
        }
    }

    fun consumePendingCitations(): List<CitationInsertRequest> {
        val pending = _state.value.pendingCitations
        _state.update { it.copy(pendingCitations = emptyList()) }
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

    fun createStudy() {
        viewModelScope.launch {
            val notebookId = _state.value.selectedNotebookId ?: _state.value.notebooks.firstOrNull()?.id ?: return@launch
            val now = System.currentTimeMillis()
            val newStudyId = dao.insertStudy(
                StudyEntity(
                    title = "Nueva enseñanza ${state.value.studies.size + 1}",
                    notebookId = notebookId,
                    contentSerialized = json.encodeToString(SerializedStudyDocument()),
                    createdAt = now,
                    updatedAt = now
                )
            )
            observeStudies(notebookId)
            loadStudy(newStudyId)
        }
    }

    fun duplicateCurrentStudy() {
        viewModelScope.launch {
            val current = _state.value
            val currentId = current.selectedStudyId ?: return@launch
            val original = dao.getStudy(currentId) ?: return@launch
            val now = System.currentTimeMillis()
            val duplicatedId = dao.insertStudy(
                original.copy(
                    id = 0,
                    title = "${original.title} (copia)",
                    createdAt = now,
                    updatedAt = now
                )
            )
            observeStudies(original.notebookId)
            loadStudy(duplicatedId)
        }
    }

    fun deleteCurrentStudy() {
        viewModelScope.launch {
            val currentId = _state.value.selectedStudyId ?: return@launch
            val notebookId = _state.value.selectedNotebookId ?: return@launch
            dao.deleteStudy(currentId)
            observeStudies(notebookId)
        }
    }

    fun selectSearchResult(result: StudySearchResult) {
        process(StudyIntent.SelectNotebook(result.notebookId))
        process(StudyIntent.SelectStudy(result.studyId))
    }

    fun copyStudyToClipboard() {
        val title = _state.value.title.ifBlank { "Enseñanza" }
        val text = buildShareableText()
        clipboardManager.setPrimaryClip(ClipData.newPlainText(title, text))
        _state.update { it.copy(exportMessage = "Enseñanza copiada al portapapeles") }
    }

    fun shareStudy() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, _state.value.title.ifBlank { "Enseñanza" })
            putExtra(Intent.EXTRA_TEXT, buildShareableText())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(Intent.createChooser(shareIntent, "Compartir enseñanza").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    val saveStatusLabel: String
        get() = when {
            _state.value.isSaving -> "Guardando…"
            _state.value.hasUnsavedChanges -> "Sin guardar"
            _state.value.lastSavedAt != null -> "Guardado ${formatRelativeTime(_state.value.lastSavedAt)}"
            else -> "Listo"
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
        observeStudiesJob?.cancel()
        observeStudiesJob = viewModelScope.launch {
            dao.observeStudies(notebookId).collect { studies ->
                val currentSelected = _state.value.selectedStudyId?.takeIf { id -> studies.any { it.id == id } } ?: studies.firstOrNull()?.id
                _state.update { it.copy(studies = studies, selectedStudyId = currentSelected) }
                currentSelected?.let { loadStudy(it) }
            }
        }
    }

    private suspend fun loadStudy(studyId: Long) {
        val study = dao.getStudy(studyId) ?: return
        val doc = runCatching { json.decodeFromString<SerializedStudyDocument>(study.contentSerialized) }
            .getOrDefault(SerializedStudyDocument())
        val firstRich = doc.blocks.filterIsInstance<StudyBlockNode.RichText>().firstOrNull()
        _state.update {
            it.copy(
                selectedStudyId = study.id,
                title = study.title,
                richHtml = firstRich?.html ?: "",
                blocks = doc.blocks,
                globalVersion = doc.globalVersion,
                hasUnsavedChanges = false,
                isSaving = false,
                saveError = null,
                lastSavedAt = study.updatedAt,
                workspaceItems = buildWorkspaceItems(doc.blocks),
                recentReferences = buildRecentReferences(doc.blocks)
            )
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        val results = dao.searchStudies(query.trim()).map { entity ->
            StudySearchResult(
                studyId = entity.id,
                notebookId = entity.notebookId,
                title = entity.title,
                snippet = entity.contentSerialized.replace("<[^>]*>".toRegex(), " ").replace("\\s+".toRegex(), " ").take(140),
                updatedAt = entity.updatedAt
            )
        }
        _state.update { it.copy(searchResults = results) }
    }

    private fun startAutoSave() {
        viewModelScope.launch {
            while (true) {
                delay(7000)
                if (_state.value.hasUnsavedChanges && !_state.value.isSaving) {
                    persistCurrentStudy()
                }
            }
        }
    }

    private suspend fun persistCurrentStudy(): Boolean {
        val s = _state.value
        val id = s.selectedStudyId ?: return false
        val notebookId = s.selectedNotebookId ?: return false
        _state.update { it.copy(isSaving = true, saveError = null) }
        val now = System.currentTimeMillis()
        val richTextBlock = StudyBlockNode.RichText(html = s.richHtml, references = detectReferences(s.richHtml))
        val nonText = s.blocks.filterNot { it is StudyBlockNode.RichText }
        val document = SerializedStudyDocument(blocks = listOf(richTextBlock) + nonText, globalVersion = s.globalVersion)
        return runCatching {
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
            val citations = document.blocks.filterIsInstance<StudyBlockNode.Citation>().map {
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
        }.fold(
            onSuccess = {
                _state.update {
                    it.copy(
                        blocks = document.blocks,
                        hasUnsavedChanges = false,
                        isSaving = false,
                        lastSavedAt = now,
                        saveError = null,
                        workspaceItems = buildWorkspaceItems(document.blocks),
                        recentReferences = buildRecentReferences(document.blocks)
                    )
                }
                true
            },
            onFailure = { error ->
                _state.update { it.copy(isSaving = false, saveError = error.message ?: "No se pudo guardar") }
                false
            }
        )
    }

    private fun updateEditableState(title: String = _state.value.title, blocks: List<StudyBlockNode> = _state.value.blocks) {
        _state.update {
            it.copy(
                title = title,
                blocks = blocks,
                hasUnsavedChanges = true,
                saveError = null,
                workspaceItems = buildWorkspaceItems(blocks),
                recentReferences = buildRecentReferences(blocks)
            )
        }
    }

    private fun appendNonTextBlock(block: StudyBlockNode) {
        val updatedBlocks = _state.value.blocks + block
        updateEditableState(blocks = updatedBlocks)
    }

    private fun exportStudyAsTextFile() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val safeTitle = _state.value.title.ifBlank { "ensenanza" }.replace("[^A-Za-z0-9áéíóúÁÉÍÓÚñÑ_-]".toRegex(), "_")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val file = app.cacheDir.resolve("${safeTitle}_${timestamp}.txt")
            runCatching {
                file.writeText(buildShareableText())
            }.fold(
                onSuccess = {
                    _state.update { it.copy(exportMessage = "Texto exportado en ${file.absolutePath}") }
                },
                onFailure = { error ->
                    _state.update { it.copy(exportMessage = "No se pudo exportar: ${error.message}") }
                }
            )
        }
    }

    private fun buildShareableText(): String {
        val current = _state.value
        val refs = buildRecentReferences(current.blocks)
        val citations = current.blocks.filterIsInstance<StudyBlockNode.Citation>()
        return buildString {
            appendLine(current.title.ifBlank { "Enseñanza" })
            appendLine("=".repeat(current.title.ifBlank { "Enseñanza" }.length.coerceAtLeast(10)))
            if (refs.isNotEmpty()) {
                appendLine("Referencias: ${refs.joinToString()}")
                appendLine()
            }
            appendLine(current.richHtml.replace("<[^>]*>".toRegex(), " ").replace("\\s+".toRegex(), " ").trim())
            if (citations.isNotEmpty()) {
                appendLine()
                appendLine("Citas insertadas:")
                citations.forEach { appendLine("- ${it.reference.display}") }
            }
        }
    }

    private fun rebuildBlocks(html: String, old: List<StudyBlockNode>): List<StudyBlockNode> {
        val refs = detectReferences(html)
        val nonText = old.filterNot { it is StudyBlockNode.RichText }
        return listOf(StudyBlockNode.RichText(html = html, references = refs)) + nonText
    }

    private fun buildWorkspaceItems(blocks: List<StudyBlockNode>): List<WorkspacePanelItem> {
        val citations = blocks.filterIsInstance<StudyBlockNode.Citation>()
        val media = blocks.filter { it is StudyBlockNode.Audio || it is StudyBlockNode.Image }
        return buildList {
            add(WorkspacePanelItem("Citas", citations.size.toString()))
            add(WorkspacePanelItem("Bloques multimedia", media.size.toString()))
            add(WorkspacePanelItem("Referencias detectadas", buildRecentReferences(blocks).size.toString()))
            add(WorkspacePanelItem("Pendientes por insertar", _state.value.pendingCitations.size.toString()))
        }
    }

    private fun buildRecentReferences(blocks: List<StudyBlockNode>): List<String> {
        val inlineRefs = blocks.filterIsInstance<StudyBlockNode.RichText>().flatMap { it.references }
        val citedRefs = blocks.filterIsInstance<StudyBlockNode.Citation>().map { it.reference }
        return (inlineRefs + citedRefs)
            .map { it.display }
            .distinct()
            .takeLast(8)
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

    private fun formatRelativeTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        val deltaSeconds = ((System.currentTimeMillis() - timestamp) / 1000).coerceAtLeast(0)
        return when {
            deltaSeconds < 10 -> "hace unos segundos"
            deltaSeconds < 60 -> "hace ${deltaSeconds}s"
            deltaSeconds < 3600 -> "hace ${deltaSeconds / 60} min"
            else -> "${deltaSeconds / 3600} h atrás"
        }
    }
}

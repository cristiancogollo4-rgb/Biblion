package com.cristiancogollo.biblion

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
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
    val isStudiesLoading: Boolean = true,
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
    data class UpdateParagraphBlock(val blockId: String, val text: String) : StudyIntent
    data class UpdateParagraphParallelText(val blockId: String, val text: String) : StudyIntent
    data class UpdateParagraphRole(val blockId: String, val role: String) : StudyIntent
    data class UpdateParagraphAlignment(val blockId: String, val textAlign: String) : StudyIntent
    data class AddColumnEmbeddedBlock(
        val paragraphBlockId: String,
        val source: String,
        val block: ColumnEmbeddedBlock
    ) : StudyIntent
    data class UpdateColumnEmbeddedBlock(
        val paragraphBlockId: String,
        val source: String,
        val block: ColumnEmbeddedBlock
    ) : StudyIntent
    data class ToggleColumnEmbeddedBlockCollapsed(
        val paragraphBlockId: String,
        val source: String,
        val embeddedBlockId: String
    ) : StudyIntent
    data class DeleteColumnEmbeddedBlock(
        val paragraphBlockId: String,
        val source: String,
        val embeddedBlockId: String
    ) : StudyIntent
    data class SplitParagraphBlock(
        val blockId: String,
        val newBlockId: String,
        val currentText: String,
        val nextText: String,
        val nextRole: String
    ) : StudyIntent
    data class ApplyParagraphTextStyle(
        val blockId: String,
        val source: String,
        val start: Int,
        val end: Int,
        val color: Long? = null,
        val background: Long? = null,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val fontSizeSp: Float? = null
    ) : StudyIntent
    data class ClearParagraphTextStyle(
        val blockId: String,
        val source: String,
        val start: Int,
        val end: Int,
        val clearColor: Boolean = true,
        val clearBackground: Boolean = true,
        val clearBold: Boolean = true,
        val clearItalic: Boolean = true,
        val clearUnderline: Boolean = true,
        val clearFontSize: Boolean = true
    ) : StudyIntent
    data class UpdateRichTextBlock(val blockId: String, val html: String) : StudyIntent
    data class AddNoteBlock(val afterBlockId: String?, val text: String? = null) : StudyIntent
    data class AddReflectionBlock(val topic: String, val afterBlockId: String?, val text: String? = null) : StudyIntent
    data class AddQuotedVerseBlock(
        val afterBlockId: String?,
        val citation: CitationInsertRequest? = null
    ) : StudyIntent
    data class ChangeQuotedVerseVersion(val blockId: String, val version: String) : StudyIntent
    data class CompareQuotedVerseVersion(val blockId: String, val version: String) : StudyIntent
    data class AddQuestionBlock(val afterBlockId: String?) : StudyIntent
    data class UpdateBlock(val block: StudyBlockNode) : StudyIntent
    data class ToggleBlockCollapsed(val blockId: String) : StudyIntent
    data class DeleteBlock(val blockId: String) : StudyIntent
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

class StudyViewModel @JvmOverloads constructor(
    application: Application,
    private val dao: StudyDao = StudyDatabase.getInstance(application).studyDao(),
    private val autoSaveDebounceMs: Long = AUTOSAVE_DEBOUNCE_MS,
    private val seedDemoStudies: Boolean = true,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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
                _state.value = _state.value.copy(allStudies = all, isStudiesLoading = false)
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
                    withContext(ioDispatcher) {
                        val existing = dao.getStudy(intent.studyId) ?: return@withContext
                        dao.updateStudy(
                            existing.copy(
                                deletedAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    FirestoreSyncManager.requestStudiesSync()
                }
            }
            is StudyIntent.UpdateTitle -> {
                _state.value = _state.value.copy(title = intent.title)
            }
            is StudyIntent.UpdateRichHtml -> {
                undoStack.addLast(_state.value.richHtml)
                _state.value = _state.value.copy(
                    richHtml = intent.html,
                    blocks = StudyDocumentEngine.rebuildBlocks(intent.html, _state.value.blocks)
                )
                redoStack.clear()
            }
            is StudyIntent.UpdateParagraphBlock -> {
                applyDocumentBlocks(
                    StudyDocumentEngine.updateParagraphText(
                        blocks = _state.value.blocks,
                        fallbackHtml = _state.value.richHtml,
                        blockId = intent.blockId,
                        text = intent.text
                    )
                )
            }
            is StudyIntent.UpdateParagraphParallelText -> {
                applyDocumentBlocks(
                    StudyDocumentEngine.updateParagraphParallelText(
                        blocks = _state.value.blocks,
                        fallbackHtml = _state.value.richHtml,
                        blockId = intent.blockId,
                        text = intent.text
                    )
                )
            }
            is StudyIntent.SplitParagraphBlock -> {
                applyDocumentBlocks(
                    StudyDocumentEngine.splitParagraph(
                        blocks = _state.value.blocks,
                        fallbackHtml = _state.value.richHtml,
                        blockId = intent.blockId,
                        newBlockId = intent.newBlockId,
                        currentText = intent.currentText,
                        nextText = intent.nextText,
                        nextRole = intent.nextRole
                    )
                )
            }
            is StudyIntent.UpdateParagraphRole -> {
                applyDocumentBlocks(
                    StudyDocumentEngine.updateParagraphRole(
                        blocks = _state.value.blocks,
                        fallbackHtml = _state.value.richHtml,
                        blockId = intent.blockId,
                        role = intent.role
                    )
                )
            }
            is StudyIntent.UpdateParagraphAlignment -> {
                applyDocumentBlocks(
                    StudyDocumentEngine.updateParagraphAlignment(
                        blocks = _state.value.blocks,
                        fallbackHtml = _state.value.richHtml,
                        blockId = intent.blockId,
                        textAlign = intent.textAlign
                    )
                )
            }
            is StudyIntent.AddColumnEmbeddedBlock -> {
                updateColumnEmbeddedBlocks(intent.paragraphBlockId, intent.source) { blocks ->
                    blocks + intent.block
                }
            }
            is StudyIntent.UpdateColumnEmbeddedBlock -> {
                updateColumnEmbeddedBlocks(intent.paragraphBlockId, intent.source) { blocks ->
                    blocks.map { existing ->
                        if (existing.blockId == intent.block.blockId) intent.block else existing
                    }
                }
            }
            is StudyIntent.ToggleColumnEmbeddedBlockCollapsed -> {
                updateColumnEmbeddedBlocks(intent.paragraphBlockId, intent.source) { blocks ->
                    blocks.map { existing ->
                        if (existing.blockId == intent.embeddedBlockId) {
                            existing.copy(collapsed = !existing.collapsed)
                        } else {
                            existing
                        }
                    }
                }
            }
            is StudyIntent.DeleteColumnEmbeddedBlock -> {
                updateColumnEmbeddedBlocks(intent.paragraphBlockId, intent.source) { blocks ->
                    blocks.filterNot { it.blockId == intent.embeddedBlockId }
                }
            }
            is StudyIntent.ApplyParagraphTextStyle -> {
                applyDocumentBlocks(
                    StudyDocumentEngine.applyParagraphTextStyle(
                        blocks = _state.value.blocks,
                        fallbackHtml = _state.value.richHtml,
                        blockId = intent.blockId,
                        source = intent.source,
                        start = intent.start,
                        end = intent.end,
                        color = intent.color,
                        background = intent.background,
                        bold = intent.bold,
                        italic = intent.italic,
                        underline = intent.underline,
                        fontSizeSp = intent.fontSizeSp
                    )
                )
            }
            is StudyIntent.ClearParagraphTextStyle -> {
                applyDocumentBlocks(
                    StudyDocumentEngine.clearParagraphTextStyle(
                        blocks = _state.value.blocks,
                        fallbackHtml = _state.value.richHtml,
                        blockId = intent.blockId,
                        source = intent.source,
                        start = intent.start,
                        end = intent.end,
                        clearColor = intent.clearColor,
                        clearBackground = intent.clearBackground,
                        clearBold = intent.clearBold,
                        clearItalic = intent.clearItalic,
                        clearUnderline = intent.clearUnderline,
                        clearFontSize = intent.clearFontSize
                    )
                )
            }
            is StudyIntent.UpdateRichTextBlock -> {
                applyDocumentBlocks(
                    StudyDocumentEngine.updateRichTextBlock(
                        blocks = _state.value.blocks,
                        fallbackHtml = _state.value.richHtml,
                        blockId = intent.blockId,
                        html = intent.html
                    )
                )
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
            is StudyIntent.AddNoteBlock -> {
                insertInteractiveBlock(
                    block = StudyBlockNode.Note(
                        text = intent.text?.trim()?.ifBlank { null }
                            ?: "Escribe una observacion, dato curioso o aclaracion del tema."
                    ),
                    afterBlockId = intent.afterBlockId
                )
            }
            is StudyIntent.AddReflectionBlock -> {
                insertInteractiveBlock(
                    block = StudyBlockNode.Reflection(
                        topic = intent.topic.ifBlank { "Idea o palabra clave" },
                        text = intent.text?.trim()?.ifBlank { null }
                            ?: "Desarrolla aqui una mirada mas profunda para la ensenanza."
                    ),
                    afterBlockId = intent.afterBlockId
                )
            }
            is StudyIntent.AddQuotedVerseBlock -> {
                val citation = intent.citation
                insertInteractiveBlock(
                    block = StudyBlockNode.QuotedVerse(
                        reference = citation?.reference.orEmpty(),
                        primaryVersion = citation?.version ?: _state.value.globalVersion,
                        primaryText = citation?.text.orEmpty()
                    ),
                    afterBlockId = intent.afterBlockId
                )
            }
            is StudyIntent.ChangeQuotedVerseVersion -> {
                loadQuotedVerseVersion(
                    blockId = intent.blockId,
                    version = intent.version,
                    compare = false
                )
            }
            is StudyIntent.CompareQuotedVerseVersion -> {
                loadQuotedVerseVersion(
                    blockId = intent.blockId,
                    version = intent.version,
                    compare = true
                )
            }
            is StudyIntent.AddQuestionBlock -> {
                insertInteractiveBlock(
                    block = StudyBlockNode.Question(
                        question = "Escribe la pregunta que guiara esta parte de la ensenanza."
                    ),
                    afterBlockId = intent.afterBlockId
                )
            }
            is StudyIntent.UpdateBlock -> {
                applyDocumentBlocks(StudyDocumentEngine.updateBlock(_state.value.blocks, intent.block))
            }
            is StudyIntent.ToggleBlockCollapsed -> {
                applyDocumentBlocks(StudyDocumentEngine.toggleBlockCollapsed(_state.value.blocks, intent.blockId))
            }
            is StudyIntent.DeleteBlock -> {
                applyDocumentBlocks(StudyDocumentEngine.deleteBlock(_state.value.blocks, intent.blockId))
            }
            is StudyIntent.ChangeVersion -> {
                _state.value = _state.value.copy(globalVersion = normalizeVersion(intent.version))
            }
            StudyIntent.Undo -> if (undoStack.isNotEmpty()) {
                val previous = undoStack.removeLast()
                redoStack.addLast(_state.value.richHtml)
                _state.value = _state.value.copy(
                    richHtml = previous,
                    blocks = StudyDocumentEngine.rebuildBlocks(previous, _state.value.blocks)
                )
            }
            StudyIntent.Redo -> if (redoStack.isNotEmpty()) {
                val next = redoStack.removeLast()
                undoStack.addLast(_state.value.richHtml)
                _state.value = _state.value.copy(
                    richHtml = next,
                    blocks = StudyDocumentEngine.rebuildBlocks(next, _state.value.blocks)
                )
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
                    val notebookId = _state.value.selectedNotebookId ?: withContext(ioDispatcher) {
                        dao.observeNotebooks().firstOrNull()?.firstOrNull()?.id
                    } ?: return@launch
                    val notebook = withContext(ioDispatcher) { dao.getNotebook(notebookId) } ?: return@launch
                    val emptyDoc = json.encodeToString(SerializedStudyDocument())
                    val newId = withContext(ioDispatcher) {
                        dao.insertStudy(
                            StudyEntity(
                                title = "Nueva Enseñanza",
                                notebookId = notebookId,
                                notebookRemoteId = notebook.remoteId,
                                contentSerialized = emptyDoc,
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    }
                    FirestoreSyncManager.requestStudiesSync()
                    loadStudy(newId)
                }
            }
            StudyIntent.StartNewDraft -> {
                val initialBlocks = listOf(StudyBlockNode.Paragraph(text = ""))
                _state.value = _state.value.copy(
                    selectedStudyId = null,
                    title = "",
                    richHtml = "",
                    blocks = initialBlocks,
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
        val alwaysIncludeFullText = true
        val id = CuidGenerator.create()
        val citation = StudyBlockNode.Citation(
            citationId = id,
            reference = parsed,
            text = text,
            version = _state.value.globalVersion,
            includeFullText = alwaysIncludeFullText
        )
        _state.value = _state.value.copy(
            blocks = _state.value.blocks + citation,
            pendingCitations = _state.value.pendingCitations + CitationInsertRequest(
                id = id,
                reference = reference,
                text = text,
                version = _state.value.globalVersion,
                includeFullText = alwaysIncludeFullText
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

    private fun loadQuotedVerseVersion(blockId: String, version: String, compare: Boolean) {
        val normalizedVersion = normalizeVersion(version)
        val block = _state.value.blocks
            .filterIsInstance<StudyBlockNode.QuotedVerse>()
            .firstOrNull { it.blockId == blockId }
            ?: return
        val reference = parseReference(block.reference) ?: return

        viewModelScope.launch {
            val loadedText = loadReferenceText(reference, normalizedVersion)
            if (loadedText.isBlank()) return@launch
            _state.value = _state.value.copy(
                blocks = _state.value.blocks.map { current ->
                    if (current is StudyBlockNode.QuotedVerse && current.blockId == blockId) {
                        if (compare) {
                            current.copy(compareVersion = normalizedVersion, compareText = loadedText)
                        } else {
                            current.copy(primaryVersion = normalizedVersion, primaryText = loadedText)
                        }
                    } else {
                        current
                    }
                }
            )
        }
    }

    private suspend fun loadReferenceText(reference: BibleReferenceNode, version: String): String {
        val context = getApplication<Application>().applicationContext
        return (reference.verseStart..reference.verseEnd).mapNotNull { verse ->
            BibleRepository.getVerseText(
                context = context,
                versionKey = version,
                bookName = reference.book,
                chapter = reference.chapter.toString(),
                verse = verse.toString()
            ).text
                .takeIf { it.isNotBlank() }
                ?.let { text -> "$verse ${text.trim()}" }
        }.joinToString(" ")
    }

    fun updateStudyMetadata(studyId: Long, title: String, tags: List<String>) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
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
            FirestoreSyncManager.requestStudiesSync()
        }
    }

    fun saveStudyNow(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val saved = persistCurrentStudy()
            onComplete(saved)
        }
    }

    private suspend fun ensureSeedData() {
        withContext(ioDispatcher) {
            if (dao.getNotebookCount() == 0) {
                val now = System.currentTimeMillis()
                val notebook = StudyNotebookEntity(
                    title = "Mis Notas de Estudio",
                    createdAt = now,
                    updatedAt = now
                )
                val notebookId = dao.insertNotebook(notebook)
                val emptyDoc = json.encodeToString(SerializedStudyDocument())
                dao.insertStudy(
                    StudyEntity(
                        title = "Nueva Enseñanza",
                        notebookId = notebookId,
                        notebookRemoteId = notebook.remoteId,
                        contentSerialized = emptyDoc,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            if (!seedDemoStudies) return@withContext

            val sampleTitle = "Identidad Sin Filtro - Demo modo estudio"
            val existingSample = dao.getAllStudiesForSync()
                .firstOrNull { it.deletedAt == null && it.title.equals(sampleTitle, ignoreCase = true) }
            val document = SerializedStudyDocument(
                blocks = identidadSinFiltroSampleBlocks(),
                globalVersion = "rv1960",
                tags = listOf("identidad", "jovenes", "mision juvenil", "predicacion", "demo")
            )
            if (existingSample == null) {
                val now = System.currentTimeMillis()
                val notebook = dao.getAllNotebooksForSync().firstOrNull { it.deletedAt == null }
                    ?: StudyNotebookEntity(
                        title = "Mis Notas de Estudio",
                        createdAt = now,
                        updatedAt = now
                    ).let { created ->
                        val id = dao.insertNotebook(created)
                        created.copy(id = id)
                    }
                dao.insertStudy(
                    StudyEntity(
                        title = sampleTitle,
                        notebookId = notebook.id,
                        notebookRemoteId = notebook.remoteId,
                        contentSerialized = json.encodeToString(document),
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                val existingDocument = runCatching {
                    json.decodeFromString<SerializedStudyDocument>(existingSample.contentSerialized)
                }.getOrNull()
                val hasLegacyBlocks = existingDocument?.blocks?.any {
                    it is StudyBlockNode.Question || it is StudyBlockNode.TwoColumn
                } == true
                if (hasLegacyBlocks) {
                    dao.updateStudy(
                        existingSample.copy(
                        title = sampleTitle,
                        contentSerialized = json.encodeToString(document),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private fun identidadSinFiltroSampleBlocks(): List<StudyBlockNode> = listOf(
        StudyBlockNode.Paragraph(
            text = "IDENTIDAD SIN FILTRO",
            role = "heading"
        ),
        StudyBlockNode.Note(
            text = "Demo construida desde el documento Identidad Sin Filtro.docx para probar el modo estudio: citas por rango, reflexiones, columnas, listas y texto destacado."
        ),
        StudyBlockNode.QuotedVerse(
            reference = "1 Samuel 16:5-13",
            primaryVersion = "rv1960",
            primaryText = "5 El respondio: Si, vengo a ofrecer sacrificio a Jehova; santificaos, y venid conmigo al sacrificio. 6 Y acontecio que cuando ellos vinieron, el vio a Eliab, y dijo: De cierto delante de Jehova esta su ungido. 7 Y Jehova respondio a Samuel: No mires a su parecer, ni a lo grande de su estatura, porque yo lo desecho; porque Jehova no mira lo que mira el hombre; pues el hombre mira lo que esta delante de sus ojos, pero Jehova mira el corazon. 8 Entonces llamo Isai a Abinadab, y lo hizo pasar delante de Samuel, el cual dijo: Tampoco a este ha escogido Jehova. 9 Hizo luego pasar Isai a Sama. Y el dijo: Tampoco a este ha elegido Jehova. 10 E hizo pasar Isai siete hijos suyos delante de Samuel; pero Samuel dijo a Isai: Jehova no ha elegido a estos. 11 Entonces dijo Samuel a Isai: Son estos todos tus hijos? Y el respondio: Queda aun el menor, que apacienta las ovejas. Y dijo Samuel a Isai: Envia por el, porque no nos sentaremos a la mesa hasta que el venga aqui. 12 Envio, pues, por el, y le hizo entrar; y era rubio, hermoso de ojos, y de buen parecer. Entonces Jehova dijo: Levantate y ungelo, porque este es. 13 Y Samuel tomo el cuerno del aceite, y lo ungio en medio de sus hermanos; y desde aquel dia en adelante el Espiritu de Jehova vino sobre David.",
            note = "Texto base de apertura: Dios no define la identidad por apariencia, posicion o comparacion."
        ),
        StudyBlockNode.Paragraph(
            text = "Contextualizar la labor de Mision juvenil.",
            role = "heading"
        ),
        styledParagraph(
            text = "Cuando hablamos de Mision juvenil, de lo que hace en medio de las universidades, cumple dos roles: predicar el evangelio a toda la comunidad estudiantil y ser un refugio para nuestros jovenes.",
            highlight = "predicar el evangelio",
            color = 0xFF8A5A00,
            bold = true
        ),
        StudyBlockNode.Reflection(
            topic = "Mision juvenil",
            text = "La mision no solo anuncia; tambien acompana, sostiene y forma identidad en medio de la universidad."
        ),
        StudyBlockNode.Paragraph(
            text = "Predicar\nAnunciar el evangelio a la comunidad estudiantil con claridad y cercania.",
            parallelText = "Refugio\nCrear un espacio donde los jovenes puedan ser acompanados, escuchados y formados.",
            role = "columns",
            styles = listOf(TextStyleRange(start = 0, end = 8, bold = true, color = 0xFF8A5A00)),
            parallelStyles = listOf(TextStyleRange(start = 0, end = 7, bold = true, color = 0xFF0F766E))
        ),
        StudyBlockNode.Paragraph(
            text = "En la Biblia se puede encontrar el llamamiento de varios jovenes que fueron escogidos por Dios desde una temprana edad:"
        ),
        StudyBlockNode.Paragraph(
            text = "El profeta Jeremias: el profeta timido.",
            role = "bullet"
        ),
        StudyBlockNode.Paragraph(
            text = "El profeta Samuel: cuyo nombre significa Dios ha escuchado.",
            role = "bullet"
        ),
        StudyBlockNode.Paragraph(
            text = "El joven David: conocido como el hombre conforme al corazon de Dios.",
            role = "bullet"
        ),
        StudyBlockNode.QuotedVerse(
            reference = "Jeremias 1:6-7",
            primaryVersion = "rv1960",
            primaryText = "6 Y yo dije: Ah, Senor Jehova! He aqui, no se hablar, porque soy nino. 7 Y me dijo Jehova: No digas: Soy un nino; porque a todo lo que te envie iras tu, y diras todo lo que te mande."
        ),
        StudyBlockNode.Reflection(
            topic = "Pregunta guia",
            text = "Si Dios escoge a jovenes desde su aparente debilidad, que excusa debo dejar de usar? La identidad nace del llamado de Dios, no de la seguridad personal, la edad o la apariencia."
        ),
        StudyBlockNode.Paragraph(
            text = "Digale al que esta a su lado: usted ha sido escogido por Dios. Pero hemos sido escogidos para que?"
        ),
        styledParagraph(
            text = "Cuando Dios escoge a una persona, lo primero que hace es darle algo especial: le da de su gracia y le da una identidad. Cuantos de aqui tenemos la identidad de Jesucristo?",
            highlight = "le da una identidad",
            color = 0xFF0F766E,
            background = 0xFFE2F8EF,
            bold = true
        ),
        StudyBlockNode.QuotedVerse(
            reference = "Juan 15:16",
            primaryVersion = "rv1960",
            primaryText = "16 No me elegisteis vosotros a mi, sino que yo os elegi a vosotros, y os he puesto para que vayais y lleveis fruto, y vuestro fruto permanezca; para que todo lo que pidiereis al Padre en mi nombre, el os lo de."
        ),
        StudyBlockNode.Paragraph(
            text = "Esto es lo bonito: en medio de cada universidad encontramos un grupo de jovenes escogido por Dios para hacer esta mision."
        ),
        StudyBlockNode.Paragraph(
            text = "Dios escoge",
            parallelText = "Dios envia",
            role = "columns",
            styles = listOf(TextStyleRange(start = 0, end = 11, bold = true, color = 0xFF7C3AED)),
            parallelStyles = listOf(TextStyleRange(start = 0, end = 9, bold = true, color = 0xFFB45309))
        ),
        StudyBlockNode.QuotedVerse(
            reference = "1 Corintios 15:9-10",
            primaryVersion = "rv1960",
            primaryText = "9 Porque yo soy el mas pequeno de los apostoles, que no soy digno de ser llamado apostol, porque persegui a la iglesia de Dios. 10 Pero por la gracia de Dios soy lo que soy; y su gracia no ha sido en vano para conmigo, antes he trabajado mas que todos ellos; pero no yo, sino la gracia de Dios conmigo."
        ),
        StudyBlockNode.Reflection(
            topic = "Identidad sin filtro",
            text = "La identidad no se define por apariencia, comparacion o pasado. Dios mira el corazon, llama por gracia y forma fruto que permanece."
        ),
        StudyBlockNode.Note(
            text = "Cierre sugerido: invitar a los jovenes a responder desde su identidad en Cristo y no desde la presion de parecer suficientes."
        )
    )

    private fun styledParagraph(
        text: String,
        highlight: String,
        color: Long? = null,
        background: Long? = null,
        bold: Boolean = false,
        italic: Boolean = false
    ): StudyBlockNode.Paragraph {
        val start = text.indexOf(highlight).takeIf { it >= 0 } ?: return StudyBlockNode.Paragraph(text = text)
        return StudyBlockNode.Paragraph(
            text = text,
            styles = listOf(
                TextStyleRange(
                    start = start,
                    end = start + highlight.length,
                    color = color,
                    background = background,
                    bold = bold,
                    italic = italic
                )
            )
        )
    }

    private suspend fun loadStudy(studyId: Long) {
        val study = withContext(ioDispatcher) { dao.getStudy(studyId) } ?: return
        try {
            val doc = runCatching { json.decodeFromString<SerializedStudyDocument>(study.contentSerialized) }
                .getOrDefault(SerializedStudyDocument())
            val normalizedBlocks = normalizeStudyFlow(ensureTextFlow(doc.blocks, ""))
            val newState = _state.value.copy(
                selectedStudyId = study.id,
                title = study.title,
                richHtml = buildPlainTextSnapshot(normalizedBlocks),
                blocks = normalizedBlocks,
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
        val notebookId = s.selectedNotebookId ?: withContext(ioDispatcher) {
            dao.observeNotebooks().firstOrNull()?.firstOrNull()?.id
        } ?: return false
        val notebook = withContext(ioDispatcher) { dao.getNotebook(notebookId) } ?: return false
        val now = System.currentTimeMillis()
        val document = SerializedStudyDocument(
            blocks = s.blocks,
            globalVersion = s.globalVersion,
            tags = s.tags
        )
        val studyId = s.selectedStudyId
        if (studyId == null) {
            val newId = withContext(ioDispatcher) {
                dao.insertStudy(
                    StudyEntity(
                        title = s.title.ifBlank { "Nueva Enseñanza" },
                        notebookId = notebookId,
                        notebookRemoteId = notebook.remoteId,
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
            withContext(ioDispatcher) { dao.replaceCitations(newId, citations) }
            _state.value = _state.value.copy(selectedStudyId = newId, selectedNotebookId = notebookId, isDraftMode = false)
            lastSavedSignature = buildSignature(_state.value)
            FirestoreSyncManager.requestStudiesSync()
            return true
        }

        withContext(ioDispatcher) {
            val existing = dao.getStudy(studyId)
            dao.updateStudy(
                existing?.copy(
                    title = s.title.ifBlank { "Sin título" },
                    notebookId = notebookId,
                    notebookRemoteId = notebook.remoteId,
                    contentSerialized = json.encodeToString(document),
                    createdAt = existing.createdAt,
                    updatedAt = now,
                    deletedAt = null
                ) ?: StudyEntity(
                    id = studyId,
                    title = s.title.ifBlank { "Sin título" },
                    notebookId = notebookId,
                    notebookRemoteId = notebook.remoteId,
                    contentSerialized = json.encodeToString(document),
                    createdAt = now,
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
        withContext(ioDispatcher) { dao.replaceCitations(studyId, citations) }
        lastSavedSignature = buildSignature(_state.value)
        FirestoreSyncManager.requestStudiesSync()
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

        val text = buildPlainTextSnapshot(normalizeStudyFlow(ensureTextFlow(document.blocks, "")))
        return StudyDocumentEngine.detectReferences(text).firstOrNull()?.book
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

    private fun applyDocumentBlocks(blocks: List<StudyBlockNode>) {
        _state.value = _state.value.copy(
            richHtml = StudyDocumentEngine.buildPlainTextSnapshot(blocks),
            blocks = blocks
        )
    }

    private fun rebuildBlocks(html: String, old: List<StudyBlockNode>): List<StudyBlockNode> {
        return StudyDocumentEngine.rebuildBlocks(html, old)
    }

    private fun insertInteractiveBlock(block: StudyBlockNode, afterBlockId: String?) {
        applyDocumentBlocks(
            StudyDocumentEngine.insertInteractiveBlock(
                blocks = _state.value.blocks,
                fallbackHtml = _state.value.richHtml,
                block = block,
                afterBlockId = afterBlockId
            )
        )
    }

    private fun updateColumnEmbeddedBlocks(
        paragraphBlockId: String,
        source: String,
        transform: (List<ColumnEmbeddedBlock>) -> List<ColumnEmbeddedBlock>
    ) {
        applyDocumentBlocks(
            StudyDocumentEngine.updateColumnEmbeddedBlocks(
                blocks = _state.value.blocks,
                fallbackHtml = _state.value.richHtml,
                paragraphBlockId = paragraphBlockId,
                source = source,
                transform = transform
            )
        )
    }

    private fun ensureTextFlow(blocks: List<StudyBlockNode>, fallbackHtml: String): List<StudyBlockNode> =
        StudyDocumentEngine.ensureTextFlow(blocks, fallbackHtml)

    private fun normalizeStudyFlow(blocks: List<StudyBlockNode>): List<StudyBlockNode> =
        StudyDocumentEngine.normalizeStudyFlow(blocks)

    private fun buildPlainTextSnapshot(blocks: List<StudyBlockNode>): String =
        StudyDocumentEngine.buildPlainTextSnapshot(blocks)

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

    private fun parseReference(reference: String): BibleReferenceNode? =
        StudyDocumentEngine.parseReference(reference)

    private fun exportPdfStub() {
        // Punto de extensión: implementación de exportación PDF elegante del estudio.
    }
}

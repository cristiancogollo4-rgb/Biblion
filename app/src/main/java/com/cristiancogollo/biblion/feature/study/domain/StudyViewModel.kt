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
    data class AddNoteBlock(val afterBlockId: String?) : StudyIntent
    data class AddReflectionBlock(val topic: String, val afterBlockId: String?) : StudyIntent
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
                    blocks = rebuildBlocks(intent.html, _state.value.blocks)
                )
                redoStack.clear()
            }
            is StudyIntent.UpdateParagraphBlock -> {
                val currentBlocks = ensureTextFlow(_state.value.blocks, _state.value.richHtml)
                val hasTargetBlock = currentBlocks.any { block ->
                    block is StudyBlockNode.Paragraph && block.blockId == intent.blockId
                }
                val sourceBlocks = if (hasTargetBlock) {
                    currentBlocks
                } else {
                    currentBlocks + StudyBlockNode.Paragraph(blockId = intent.blockId, text = intent.text)
                }
                val updatedBlocks = normalizeStudyFlow(
                    sourceBlocks.map { block ->
                        if (block is StudyBlockNode.Paragraph && block.blockId == intent.blockId) {
                            block.copy(text = intent.text)
                        } else {
                            block
                        }
                    }
                )
                _state.value = _state.value.copy(
                    richHtml = buildPlainTextSnapshot(updatedBlocks),
                    blocks = updatedBlocks
                )
            }
            is StudyIntent.UpdateParagraphParallelText -> {
                val updatedBlocks = normalizeStudyFlow(
                    ensureTextFlow(_state.value.blocks, _state.value.richHtml).map { block ->
                        if (block is StudyBlockNode.Paragraph && block.blockId == intent.blockId) {
                            block.copy(parallelText = intent.text)
                        } else {
                            block
                        }
                    }
                )
                _state.value = _state.value.copy(
                    richHtml = buildPlainTextSnapshot(updatedBlocks),
                    blocks = updatedBlocks
                )
            }
            is StudyIntent.SplitParagraphBlock -> {
                val currentBlocks = ensureTextFlow(_state.value.blocks, _state.value.richHtml).toMutableList()
                val index = currentBlocks.indexOfFirst { block ->
                    block is StudyBlockNode.Paragraph && block.blockId == intent.blockId
                }
                if (index >= 0) {
                    val current = currentBlocks[index]
                    if (current is StudyBlockNode.Paragraph) {
                        currentBlocks[index] = current.copy(text = intent.currentText)
                        currentBlocks.add(
                            index + 1,
                            StudyBlockNode.Paragraph(
                                blockId = intent.newBlockId,
                                text = intent.nextText,
                                role = intent.nextRole
                            )
                        )
                    }
                }
                val updatedBlocks = normalizeStudyFlow(currentBlocks)
                _state.value = _state.value.copy(
                    richHtml = buildPlainTextSnapshot(updatedBlocks),
                    blocks = updatedBlocks
                )
            }
            is StudyIntent.UpdateParagraphRole -> {
                val roleUpdatedBlocks = ensureTextFlow(_state.value.blocks, _state.value.richHtml).map { block ->
                    if (block is StudyBlockNode.Paragraph && block.blockId == intent.blockId) {
                        if (intent.role == "paragraph" && block.role == "columns") {
                            block.copy(
                                text = mergePlainText(block.text, block.parallelText),
                                parallelText = "",
                                role = intent.role
                            )
                        } else {
                            block.copy(role = intent.role)
                        }
                    } else {
                        block
                    }
                }
                val updatedBlocks = if (intent.role == "columns") {
                    ensureParagraphAfter(roleUpdatedBlocks, intent.blockId)
                } else {
                    normalizeStudyFlow(roleUpdatedBlocks)
                }
                _state.value = _state.value.copy(
                    richHtml = buildPlainTextSnapshot(updatedBlocks),
                    blocks = updatedBlocks
                )
            }
            is StudyIntent.ApplyParagraphTextStyle -> {
                val updatedBlocks = ensureTextFlow(_state.value.blocks, _state.value.richHtml).map { block ->
                    if (block is StudyBlockNode.Paragraph && block.blockId == intent.blockId) {
                        block.applyTextStyle(
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
                    } else {
                        block
                    }
                }
                _state.value = _state.value.copy(
                    richHtml = buildPlainTextSnapshot(updatedBlocks),
                    blocks = updatedBlocks
                )
            }
            is StudyIntent.ClearParagraphTextStyle -> {
                val updatedBlocks = ensureTextFlow(_state.value.blocks, _state.value.richHtml).map { block ->
                    if (block is StudyBlockNode.Paragraph && block.blockId == intent.blockId) {
                        block.clearTextStyle(
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
                    } else {
                        block
                    }
                }
                _state.value = _state.value.copy(
                    richHtml = buildPlainTextSnapshot(updatedBlocks),
                    blocks = updatedBlocks
                )
            }
            is StudyIntent.UpdateRichTextBlock -> {
                val updatedBlocks = normalizeStudyFlow(
                    ensureTextFlow(_state.value.blocks, _state.value.richHtml).map { block ->
                        if (block is StudyBlockNode.RichText && block.blockId == intent.blockId) {
                            block.copy(html = intent.html, references = detectReferences(intent.html))
                        } else {
                            block
                        }
                    }
                )
                _state.value = _state.value.copy(
                    richHtml = buildPlainTextSnapshot(updatedBlocks),
                    blocks = updatedBlocks
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
                        text = "Escribe una observacion, dato curioso o aclaracion del tema."
                    ),
                    afterBlockId = intent.afterBlockId
                )
            }
            is StudyIntent.AddReflectionBlock -> {
                insertInteractiveBlock(
                    block = StudyBlockNode.Reflection(
                        topic = intent.topic.ifBlank { "Idea o palabra clave" },
                        text = "Desarrolla aqui una mirada mas profunda para la ensenanza."
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
                _state.value = _state.value.copy(
                    blocks = _state.value.blocks.map { block ->
                        if (block.interactiveBlockId() == intent.block.interactiveBlockId()) intent.block else block
                    }
                )
            }
            is StudyIntent.ToggleBlockCollapsed -> {
                _state.value = _state.value.copy(
                    blocks = _state.value.blocks.map { block ->
                        block.toggleCollapsedIfMatches(intent.blockId)
                    }
                )
            }
            is StudyIntent.DeleteBlock -> {
                val normalizedBlocks = normalizeStudyFlow(
                    _state.value.blocks.filterNot { it.interactiveBlockId() == intent.blockId }
                )
                _state.value = _state.value.copy(
                    richHtml = buildPlainTextSnapshot(normalizedBlocks),
                    blocks = normalizedBlocks
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
        return detectReferences(text).firstOrNull()?.book
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
        val nonText = old.filterNot { it is StudyBlockNode.RichText || it is StudyBlockNode.Paragraph }
        return listOf(StudyBlockNode.Paragraph(text = html.toPlainStudyText())) + nonText
    }

    private fun insertInteractiveBlock(block: StudyBlockNode, afterBlockId: String?) {
        val currentBlocks = ensureTextFlow(_state.value.blocks, _state.value.richHtml).toMutableList()
        val insertionIndex = afterBlockId
            ?.let { id -> currentBlocks.indexOfFirst { it.flowBlockId() == id } }
            ?.takeIf { it >= 0 }
            ?.plus(1)
            ?: currentBlocks.size

        currentBlocks.add(insertionIndex, block)
        val nextIndex = insertionIndex + 1
        if (currentBlocks.getOrNull(nextIndex) !is StudyBlockNode.Paragraph) {
            currentBlocks.add(nextIndex, StudyBlockNode.Paragraph(text = ""))
        }

        val normalizedBlocks = normalizeStudyFlow(currentBlocks)
        _state.value = _state.value.copy(
            richHtml = buildPlainTextSnapshot(normalizedBlocks),
            blocks = normalizedBlocks
        )
    }

    private fun ensureTextFlow(blocks: List<StudyBlockNode>, fallbackHtml: String): List<StudyBlockNode> {
        if (blocks.any { it is StudyBlockNode.Paragraph }) return blocks
        if (blocks.any { it is StudyBlockNode.RichText }) return blocks
        return listOf(StudyBlockNode.Paragraph(text = fallbackHtml.toPlainStudyText())) + blocks
    }

    private fun ensureParagraphAfter(blocks: List<StudyBlockNode>, blockId: String): List<StudyBlockNode> {
        val mutableBlocks = blocks.toMutableList()
        val index = mutableBlocks.indexOfFirst { block ->
            block is StudyBlockNode.Paragraph && block.blockId == blockId
        }
        if (index < 0) return blocks

        val next = mutableBlocks.getOrNull(index + 1)
        if (next is StudyBlockNode.Paragraph) return blocks

        mutableBlocks.add(index + 1, StudyBlockNode.Paragraph(text = ""))
        return mutableBlocks
    }

    private fun StudyBlockNode.Paragraph.applyTextStyle(
        source: String,
        start: Int,
        end: Int,
        color: Long?,
        background: Long?,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        fontSizeSp: Float?
    ): StudyBlockNode.Paragraph {
        val textLength = if (source == "parallel") parallelText.length else text.length
        val rangeStart = start.coerceIn(0, textLength).coerceAtMost(end.coerceIn(0, textLength))
        val rangeEnd = start.coerceIn(0, textLength).coerceAtLeast(end.coerceIn(0, textLength))
        if (rangeStart == rangeEnd) return this

        val style = TextStyleRange(
            start = rangeStart,
            end = rangeEnd,
            color = color,
            background = background,
            bold = bold,
            italic = italic,
            underline = underline,
            fontSizeSp = fontSizeSp
        )
        return if (source == "parallel") {
            copy(parallelStyles = parallelStyles + style)
        } else {
            copy(styles = styles + style)
        }
    }

    private fun StudyBlockNode.Paragraph.clearTextStyle(
        source: String,
        start: Int,
        end: Int,
        clearColor: Boolean,
        clearBackground: Boolean,
        clearBold: Boolean,
        clearItalic: Boolean,
        clearUnderline: Boolean,
        clearFontSize: Boolean
    ): StudyBlockNode.Paragraph {
        val textLength = if (source == "parallel") parallelText.length else text.length
        val rangeStart = start.coerceIn(0, textLength).coerceAtMost(end.coerceIn(0, textLength))
        val rangeEnd = start.coerceIn(0, textLength).coerceAtLeast(end.coerceIn(0, textLength))
        if (rangeStart == rangeEnd) return this

        fun TextStyleRange.hasAnyStyle(): Boolean =
            color != null || background != null || bold || italic || underline || fontSizeSp != null

        fun trimStyle(style: TextStyleRange): TextStyleRange = style.copy(
            color = if (clearColor) null else style.color,
            background = if (clearBackground) null else style.background,
            bold = if (clearBold) false else style.bold,
            italic = if (clearItalic) false else style.italic,
            underline = if (clearUnderline) false else style.underline,
            fontSizeSp = if (clearFontSize) null else style.fontSizeSp
        )

        fun clearStyles(styles: List<TextStyleRange>): List<TextStyleRange> = buildList {
            styles.forEach { style ->
                if (style.end <= rangeStart || style.start >= rangeEnd) {
                    add(style)
                } else {
                    if (style.start < rangeStart) {
                        add(style.copy(end = rangeStart))
                    }
                    val middle = trimStyle(
                        style.copy(
                            start = style.start.coerceAtLeast(rangeStart),
                            end = style.end.coerceAtMost(rangeEnd)
                        )
                    )
                    if (middle.start < middle.end && middle.hasAnyStyle()) {
                        add(middle)
                    }
                    if (style.end > rangeEnd) {
                        add(style.copy(start = rangeEnd))
                    }
                }
            }
        }

        return if (source == "parallel") {
            copy(parallelStyles = clearStyles(parallelStyles))
        } else {
            copy(styles = clearStyles(styles))
        }
    }

    private fun normalizeStudyFlow(blocks: List<StudyBlockNode>): List<StudyBlockNode> {
        val normalized = mutableListOf<StudyBlockNode>()
        blocks.map { it.toNativeTextBlock() }.forEach { block ->
            val last = normalized.lastOrNull()
            if (
                block is StudyBlockNode.Paragraph &&
                last is StudyBlockNode.Paragraph &&
                block.role == "paragraph" &&
                last.role == "paragraph"
            ) {
                val mergedHtml = mergePlainText(last.text, block.text)
                normalized[normalized.lastIndex] = if (last.text.isBlank() && block.text.isNotBlank()) {
                    block.copy(text = mergedHtml)
                } else {
                    last.copy(text = mergedHtml)
                }
            } else {
                normalized.add(block)
            }
        }

        val withoutDuplicateEmptyText = normalized.filterIndexed { index, block ->
            block !is StudyBlockNode.Paragraph ||
                block.text.isNotBlank() ||
                block.parallelText.isNotBlank() ||
                normalized.none { it is StudyBlockNode.Paragraph && (it.text.isNotBlank() || it.parallelText.isNotBlank()) } ||
                normalized.getOrNull(index - 1) !is StudyBlockNode.Paragraph ||
                (normalized.getOrNull(index - 1) as? StudyBlockNode.Paragraph)?.role in setOf("columns", "bullet", "numbered")
        }

        return withoutDuplicateEmptyText.ifEmpty {
            listOf(StudyBlockNode.Paragraph(text = ""))
        }
    }

    private fun mergePlainText(first: String, second: String): String {
        val firstClean = first.trim()
        val secondClean = second.trim()
        return when {
            firstClean.isBlank() -> secondClean
            secondClean.isBlank() -> firstClean
            else -> "$firstClean\n$secondClean"
        }
    }

    private fun StudyBlockNode.toNativeTextBlock(): StudyBlockNode = when (this) {
        is StudyBlockNode.RichText -> StudyBlockNode.Paragraph(
            blockId = blockId,
            text = html.toPlainStudyText(),
            role = "paragraph"
        )
        else -> this
    }

    private fun buildPlainTextSnapshot(blocks: List<StudyBlockNode>): String {
        return blocks.mapNotNull { block ->
            when (block) {
                is StudyBlockNode.Paragraph -> listOf(block.text, block.parallelText).filter { it.isNotBlank() }.joinToString("\n")
                is StudyBlockNode.RichText -> block.html.toPlainStudyText()
                is StudyBlockNode.Note -> block.text
                is StudyBlockNode.Reflection -> listOf(block.topic, block.text).filter { it.isNotBlank() }.joinToString("\n")
                is StudyBlockNode.QuotedVerse -> listOf(block.reference, block.primaryText, block.compareText, block.note).filter { it.isNotBlank() }.joinToString("\n")
                is StudyBlockNode.Question -> listOf(block.question, block.answer).filter { it.isNotBlank() }.joinToString("\n")
                is StudyBlockNode.TwoColumn -> listOf(block.leftTitle, block.leftText, block.rightTitle, block.rightText).filter { it.isNotBlank() }.joinToString("\n")
                else -> null
            }
        }.joinToString("\n\n")
    }

    private fun String.toPlainStudyText(): String {
        return replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>|</div>|</h[1-6]>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun StudyBlockNode.flowBlockId(): String? = when (this) {
        is StudyBlockNode.Paragraph -> blockId
        is StudyBlockNode.RichText -> blockId
        is StudyBlockNode.Note -> blockId
        is StudyBlockNode.Reflection -> blockId
        is StudyBlockNode.QuotedVerse -> blockId
        is StudyBlockNode.Question -> blockId
        is StudyBlockNode.TwoColumn -> blockId
        else -> null
    }

    private fun StudyBlockNode.interactiveBlockId(): String? = when (this) {
        is StudyBlockNode.Note -> blockId
        is StudyBlockNode.Reflection -> blockId
        is StudyBlockNode.QuotedVerse -> blockId
        is StudyBlockNode.Question -> blockId
        is StudyBlockNode.TwoColumn -> blockId
        else -> null
    }

    private fun StudyBlockNode.toggleCollapsedIfMatches(blockId: String): StudyBlockNode = when (this) {
        is StudyBlockNode.Note -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        is StudyBlockNode.Reflection -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        is StudyBlockNode.QuotedVerse -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        is StudyBlockNode.Question -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        is StudyBlockNode.TwoColumn -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        else -> this
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

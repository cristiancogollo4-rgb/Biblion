package com.cristiancogollo.biblion.feature.studydocs.domain

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyOp
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyDocEngine
import com.cristiancogollo.biblion.feature.studydocs.engine.OpResult
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.syncFromStyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.fromAnnotatedString
import androidx.compose.ui.graphics.toArgb
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel compartido entre ambos paneles del modo estudio.
 *
 * Reemplaza el patrón actual de ViewModel aislado en StudyDocEditorSplitContent.
 * Permite comunicación bidireccional: tap en lector izquierdo → inserta cita en editor derecho.
 */
class StudyDocSplitViewModel(
    private val repository: StudyDocRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Estado del layout split (fijo 50/50)
    private val _splitState = MutableStateFlow(SplitUiState())
    val splitState: StateFlow<SplitUiState> = _splitState.asStateFlow()

    // Estado del editor (reutiliza StudyEditorUiState existente)
    private val _editorState = MutableStateFlow(StudyEditorUiState())
    val editorState: StateFlow<StudyEditorUiState> = _editorState.asStateFlow()

    // Eventos del lector → editor (SharedFlow para one-shot events)
    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    // RichTextStates por bloque (mutableStateMapOf para que Compose observe cambios)
    val blockRichStates: SnapshotStateMap<BlockId, RichTextState> = mutableStateMapOf()

    private var _remoteId: String? = null
    private var _isDocLoaded = false
    private var _autoSaveJob: kotlinx.coroutines.Job? = null

    init {
        // Observar eventos para insertar citas
        viewModelScope.launch {
            events.collect { event ->
                when (event) {
                    is EditorEvent.InsertVerseAsQuote -> {
                        insertVerseAsQuote(
                            book = event.book,
                            chapter = event.chapter,
                            verseStart = event.verseStart,
                            verseEnd = event.verseEnd,
                            text = event.text,
                            version = event.version
                        )
                    }
                }
            }
        }
    }

    // ============ Layout Split ============

    fun setLeftPaneKind(kind: LeftPaneKind) {
        _splitState.value = _splitState.value.copy(leftPaneKind = kind)
    }

    // ============ Editor ============

    fun newDraft() {
        _remoteId = null
        blockRichStates.clear()
        val block = StudyBlock.Paragraph()
        val rs = RichTextState().apply { setHtml("<p></p>") }
        blockRichStates[block.id] = rs
        _editorState.value = StudyEditorUiState(
            doc = StudyDoc(blocks = listOf(block)),
            activeBlockId = block.id,
            hasUnsavedChanges = true
        )
        _isDocLoaded = true
    }

    fun loadByRemoteId(remoteId: String) {
        _editorState.value = _editorState.value.copy(isLoading = true)
        viewModelScope.launch {
            val doc = repository.getByRemoteId(remoteId)
            if (doc != null) {
                _remoteId = doc.remoteId
                blockRichStates.clear()
                doc.blocks.forEach { block ->
                    val rs = RichTextState()
                    val texts = block.toStyledTextList()
                    if (texts.isNotEmpty()) {
                        rs.syncFromStyledText(texts.first())
                    } else {
                        rs.setHtml("<p></p>")
                    }
                    blockRichStates[block.id] = rs
                }
                val firstId = doc.blocks.firstOrNull()?.id
                _editorState.value = StudyEditorUiState(
                    doc = doc,
                    activeBlockId = firstId,
                    isLoading = false
                )
                _isDocLoaded = true
            } else {
                _editorState.value = _editorState.value.copy(
                    isLoading = false,
                    lastError = "Documento no encontrado"
                )
            }
        }
    }

    fun setActiveBlock(blockId: BlockId) {
        _editorState.value = _editorState.value.copy(activeBlockId = blockId)
        blockRichStates[blockId]?.let { syncActiveFormat(it) }
    }

    fun syncActiveFormat(richState: RichTextState) {
        val style = richState.currentSpanStyle
        _editorState.value = _editorState.value.copy(
            activeFormat = ActiveFormatSnapshot(
                bold = style.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold,
                italic = style.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic,
                underline = style.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.Underline) == true,
                strikethrough = style.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.LineThrough) == true,
                color = style.color.takeIf { it != androidx.compose.ui.graphics.Color.Unspecified }?.toArgb(),
                background = style.background.takeIf { it != androidx.compose.ui.graphics.Color.Unspecified }?.toArgb(),
                fontSize = style.fontSize.takeIf { it.type == androidx.compose.ui.unit.TextUnitType.Sp }?.value?.toInt()
            )
        )
    }

    fun applyStyleToActive(kind: TextStyleKind) {
        val activeId = _editorState.value.activeBlockId ?: return
        val rs = blockRichStates[activeId] ?: return
        when (kind) {
            TextStyleKind.Bold -> rs.toggleSpanStyle(
                androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            )
            TextStyleKind.Italic -> rs.toggleSpanStyle(
                androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            )
            TextStyleKind.Underline -> rs.toggleSpanStyle(
                androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
            )
            TextStyleKind.Strikethrough -> rs.toggleSpanStyle(
                androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
            )
        }
        syncActiveFormat(rs)
        markDirty()
    }

    fun insertBlock(afterBlockId: BlockId?, type: String) {
        val newBlock = when (type) {
            "heading1" -> StudyBlock.Heading(level = 1)
            "heading2" -> StudyBlock.Heading(level = 2)
            "heading3" -> StudyBlock.Heading(level = 3)
            "bullet" -> StudyBlock.BulletList()
            "ordered" -> StudyBlock.OrderedList()
            "quote" -> StudyBlock.Quote()
            else -> StudyBlock.Paragraph()
        }
        val rs = RichTextState().apply { setHtml("<p></p>") }
        blockRichStates[newBlock.id] = rs
        applyOp(StudyOp.InsertBlock(newBlock, afterBlockId))
        _editorState.value = _editorState.value.copy(activeBlockId = newBlock.id)
    }

    // ============ Cross-Pane Bridge ============

    /**
     * Inserta un versículo como cita en el editor.
     *
     * Emite un evento que es capturado por el init block y procesado.
     */
    fun insertVerseAsQuote(
        book: String,
        chapter: Int,
        verseStart: Int,
        verseEnd: Int,
        text: String,
        version: String
    ) {
        viewModelScope.launch {
            _events.emit(
                EditorEvent.InsertVerseAsQuote(
                    book = book,
                    chapter = chapter,
                    verseStart = verseStart,
                    verseEnd = verseEnd,
                    text = text,
                    version = version
                )
            )
        }
    }

    private suspend fun insertVerseAsQuoteInternal(
        book: String,
        chapter: Int,
        verseStart: Int,
        verseEnd: Int,
        text: String,
        version: String
    ) {
        val reference = "$book $chapter:$verseStart" + if (verseEnd != verseStart) "-$verseEnd" else ""

        val quoteBlock = StudyBlock.Quote(
            text = StyledText.Empty,
            attribution = "$reference ($version)"
        )

        val rs = RichTextState().apply {
            setHtml("<blockquote><p>${escapeHtml(text)}</p><cite>$reference ($version)</cite></blockquote>")
        }
        blockRichStates[quoteBlock.id] = rs

        // Insertar después del bloque activo o al final
        val afterId = _editorState.value.activeBlockId
            ?: _editorState.value.doc.blocks.lastOrNull()?.id

        applyOp(StudyOp.InsertBlock(quoteBlock, afterId))
        _editorState.value = _editorState.value.copy(activeBlockId = quoteBlock.id)
    }

    private fun applyOp(op: StudyOp) {
        val (newDoc, result) = StudyDocEngine.apply(_editorState.value.doc, op)
        if (result.isSuccess) {
            _editorState.value = _editorState.value.copy(
                doc = newDoc,
                lastError = null,
                hasUnsavedChanges = true
            )
        } else {
            _editorState.value = _editorState.value.copy(
                lastError = (result as com.cristiancogollo.biblion.feature.studydocs.engine.OpResult.Failed).reason
            )
        }
    }

    private fun markDirty() {
        _editorState.value = _editorState.value.copy(hasUnsavedChanges = true)
    }

    fun saveNow(title: String, tags: List<String>) {
        viewModelScope.launch {
            val current = _editorState.value.doc
            val blocksWithHtml = current.blocks.map { block ->
                val rs = blockRichStates[block.id]
                val styled = if (rs != null) {
                    StyledText.fromAnnotatedString(rs.annotatedString)
                } else {
                    StyledText.Empty
                }
                when (block) {
                    is StudyBlock.Paragraph -> block.copy(text = styled)
                    is StudyBlock.Heading -> block.copy(text = styled)
                    is StudyBlock.BulletList -> block
                    is StudyBlock.OrderedList -> block
                    is StudyBlock.Quote -> block.copy(text = styled)
                }
            }
            val doc = current.copy(
                blocks = blocksWithHtml,
                remoteId = _remoteId ?: current.id.value,
                updatedAt = System.currentTimeMillis()
            )
            repository.save(doc)
            _editorState.value = _editorState.value.copy(lastSavedAt = System.currentTimeMillis())
        }
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    override fun onCleared() {
        super.onCleared()
        blockRichStates.clear()
    }

    fun updateTitle(newTitle: String) {
        _editorState.value = _editorState.value.copy(
            doc = _editorState.value.doc.copy(title = newTitle),
            hasUnsavedChanges = true,
        )
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        if (!_isDocLoaded) return
        _autoSaveJob?.cancel()
        _autoSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000L)
            saveNow(_editorState.value.doc.title, _editorState.value.doc.metadata.tags)
        }
    }

    // ============ Métodos adicionales requeridos por el editor ============

    fun handleEnter(blockId: BlockId) {
        val richState = blockRichStates[blockId] ?: return
        val sel = richState.selection
        val text = richState.annotatedString.text

        if (sel.start >= text.length) {
            insertBlock(blockId, "paragraph")
            return
        }

        val fullHtml = richState.toHtml()
        val cursorPos = sel.start

        richState.removeTextRange(androidx.compose.ui.text.TextRange(cursorPos, text.length))

        val temp = RichTextState()
        temp.setHtml(fullHtml)
        temp.removeTextRange(androidx.compose.ui.text.TextRange(0, cursorPos))

        val newBlock = StudyBlock.Paragraph()
        val newRS = RichTextState()
        newRS.setHtml(temp.toHtml())
        blockRichStates[newBlock.id] = newRS

        applyOp(StudyOp.SplitBlock(
            splitBlockId = blockId,
            newBlock = newBlock,
        ))
        _editorState.value = _editorState.value.copy(activeBlockId = newBlock.id)
    }

    fun handleBackspace(blockId: BlockId) {
        val idx = _editorState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (idx <= 0) return
        val prevBlock = _editorState.value.doc.blocks[idx - 1]
        val currentRS = blockRichStates[blockId] ?: return
        val prevRS = blockRichStates[prevBlock.id] ?: return

        val sel = currentRS.selection
        if (!sel.collapsed || sel.start > 0) return

        val joinOffset = prevRS.annotatedString.text.length
        val currentHtml = currentRS.toHtml()

        prevRS.setHtml(prevRS.toHtml() + currentHtml)

        blockRichStates.remove(blockId)
        applyOp(StudyOp.MergeBlock(removeBlockId = blockId))

        prevRS.selection = androidx.compose.ui.text.TextRange(joinOffset)
        _editorState.value = _editorState.value.copy(activeBlockId = prevBlock.id)
    }

    fun moveCursorToPrevBlock(blockId: BlockId) {
        val blocks = _editorState.value.doc.blocks
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx <= 0) return
        val prevId = blocks[idx - 1].id
        val prevRS = blockRichStates[prevId] ?: return
        prevRS.selection = androidx.compose.ui.text.TextRange(prevRS.annotatedString.text.length)
        _editorState.value = _editorState.value.copy(activeBlockId = prevId)
    }

    fun moveCursorToNextBlock(blockId: BlockId) {
        val blocks = _editorState.value.doc.blocks
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx < 0 || idx >= blocks.size - 1) return
        val nextId = blocks[idx + 1].id
        val nextRS = blockRichStates[nextId] ?: return
        nextRS.selection = androidx.compose.ui.text.TextRange(0)
        _editorState.value = _editorState.value.copy(activeBlockId = nextId)
    }

    fun setActiveTextColor(argb: Int) {
        val activeId = _editorState.value.activeBlockId ?: return
        val rs = blockRichStates[activeId] ?: return
        if (rs.selection.collapsed) {
            rs.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color(argb)))
        } else {
            rs.addSpanStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color(argb)))
        }
        syncActiveFormat(rs)
        markDirty()
    }

    fun setActiveBackgroundColor(argb: Int) {
        val activeId = _editorState.value.activeBlockId ?: return
        val rs = blockRichStates[activeId] ?: return
        if (rs.selection.collapsed) {
            rs.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(background = androidx.compose.ui.graphics.Color(argb)))
        } else {
            rs.addSpanStyle(androidx.compose.ui.text.SpanStyle(background = androidx.compose.ui.graphics.Color(argb)))
        }
        syncActiveFormat(rs)
        markDirty()
    }

    fun clearActiveColor() {
        val activeId = _editorState.value.activeBlockId ?: return
        val rs = blockRichStates[activeId] ?: return
        rs.removeSpanStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Unspecified))
        rs.removeSpanStyle(androidx.compose.ui.text.SpanStyle(background = androidx.compose.ui.graphics.Color.Unspecified))
        syncActiveFormat(rs)
        markDirty()
    }

    fun stepFontSizeActive(delta: Int) {
        val activeId = _editorState.value.activeBlockId ?: return
        val idx = _editorState.value.doc.blocks.indexOfFirst { it.id == activeId }
        if (idx < 0) return
        val block = _editorState.value.doc.blocks[idx]
        val newSize = (block.fontSize + delta).coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
        val updated = when (block) {
            is StudyBlock.Paragraph -> block.copy(fontSize = newSize)
            is StudyBlock.Heading -> block.copy(fontSize = newSize)
            is StudyBlock.BulletList -> block.copy(fontSize = newSize)
            is StudyBlock.OrderedList -> block.copy(fontSize = newSize)
            is StudyBlock.Quote -> block.copy(fontSize = newSize)
        }
        val newBlocks = _editorState.value.doc.blocks.toMutableList()
        newBlocks[idx] = updated
        _editorState.value = _editorState.value.copy(
            doc = _editorState.value.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
            hasUnsavedChanges = true
        )
    }

    fun setActiveFontFamily(family: String) {
        val activeId = _editorState.value.activeBlockId ?: return
        val blocks = _editorState.value.doc.blocks
        val idx = blocks.indexOfFirst { it.id == activeId }
        if (idx < 0) return
        val block = blocks[idx]
        val updated = when (block) {
            is StudyBlock.Paragraph -> block.copy(fontFamily = family)
            is StudyBlock.Heading -> block.copy(fontFamily = family)
            is StudyBlock.BulletList -> block.copy(fontFamily = family)
            is StudyBlock.OrderedList -> block.copy(fontFamily = family)
            is StudyBlock.Quote -> block.copy(fontFamily = family)
        }
        val newBlocks = blocks.toMutableList()
        newBlocks[idx] = updated
        _editorState.value = _editorState.value.copy(
            doc = _editorState.value.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
            hasUnsavedChanges = true
        )
    }

    fun cycleBlockAlignment(blockId: BlockId) {
        val idx = _editorState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return
        val block = _editorState.value.doc.blocks[idx]
        val next = when (block.alignment) {
            com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Start ->
                com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Center
            com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Center ->
                com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.End
            com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.End ->
                com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Justify
            com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Justify ->
                com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Start
        }
        val updated = when (block) {
            is StudyBlock.Paragraph -> block.copy(alignment = next)
            is StudyBlock.Heading -> block.copy(alignment = next)
            is StudyBlock.BulletList -> block.copy(alignment = next)
            is StudyBlock.OrderedList -> block.copy(alignment = next)
            is StudyBlock.Quote -> block.copy(alignment = next)
        }
        val newBlocks = _editorState.value.doc.blocks.toMutableList()
        newBlocks[idx] = updated
        _editorState.value = _editorState.value.copy(
            doc = _editorState.value.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
            hasUnsavedChanges = true
        )
    }

    class Factory(private val repository: StudyDocRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return StudyDocSplitViewModel(repository, androidx.lifecycle.SavedStateHandle()) as T
        }
    }

    companion object
}

/**
 * Estado del layout split.
 */
data class SplitUiState(
    val leftPaneKind: LeftPaneKind = LeftPaneKind.BibleReader
)

/**
 * Tipo de contenido en el panel izquierdo.
 */
enum class LeftPaneKind {
    BibleReader,
    DocsLibrary,
    Dictionary,
    BibiChat
}

/**
 * Eventos del lector → editor.
 */
sealed interface EditorEvent {
    data class InsertVerseAsQuote(
        val book: String,
        val chapter: Int,
        val verseStart: Int,
        val verseEnd: Int,
        val text: String,
        val version: String
    ) : EditorEvent
}

package com.cristiancogollo.biblion.feature.studydocs.domain

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.debug.StudyEditorDebugLog
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
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
    private val _events = MutableSharedFlow<EditorEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    // RichTextStates por bloque (mutableStateMapOf para que Compose observe cambios)
    val blockRichStates: SnapshotStateMap<BlockId, RichTextState> = mutableStateMapOf()
    val listItemRichStates: SnapshotStateMap<EditorTextKey, RichTextState> = mutableStateMapOf()

    private var _remoteId: String? = null
    private var _isDocLoaded = false
    private var _autoSaveJob: kotlinx.coroutines.Job? = null
    private val history = EditorHistory()
    private val lastRichTexts = mutableMapOf<EditorTextKey, StyledText>()
    private var lastTextHistoryAt = 0L
    private var focusRequestSequence = 0L

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 3000L
        private const val TEXT_HISTORY_GROUP_MS = 500L
    }

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
        listItemRichStates.clear()
        lastRichTexts.clear()
        history.clear()
        val block = StudyBlock.Paragraph()
        val rs = RichTextState().apply { setHtml("<p></p>") }
        blockRichStates[block.id] = rs
        lastRichTexts[EditorTextKey(block.id)] = StyledText.fromAnnotatedString(rs.annotatedString)
        val initialFocusRequest = nextFocusRequest(EditorTextKey(block.id))
        Log.d("BIBLION_STUDY", "StudyDocSplitViewModel.newDraft blockId=${block.id} richState added, size=${blockRichStates.size}")
        _editorState.value = StudyEditorUiState(
            doc = StudyDoc(blocks = listOf(block)),
            activeBlockId = block.id,
            focusRequest = initialFocusRequest,
            hasUnsavedChanges = true
        )
        _isDocLoaded = true
    }

    fun loadByRemoteId(remoteId: String) {
        Log.d("BIBLION_STUDY", "StudyDocSplitViewModel.loadByRemoteId start remoteId=$remoteId")
        _editorState.value = _editorState.value.copy(isLoading = true)
        viewModelScope.launch {
            val doc = repository.getByRemoteId(remoteId)
            Log.d("BIBLION_STUDY", "StudyDocSplitViewModel.loadByRemoteId repo returned doc=${doc != null} blocks=${doc?.blocks?.size}")
            if (doc != null) {
                _remoteId = doc.remoteId
                blockRichStates.clear()
                listItemRichStates.clear()
                lastRichTexts.clear()
                doc.blocks.forEach { block ->
                    initializeRichStates(block)
                }
                history.clear()
                Log.d("BIBLION_STUDY", "StudyDocSplitViewModel.loadByRemoteId blockRichStates size=${blockRichStates.size}")
                val firstBlock = doc.blocks.firstOrNull()
                val firstId = firstBlock?.id
                val firstItemIndex = firstBlock?.let { navigationItemIndex(it, toEnd = false) }
                val initialFocusRequest = firstId?.let { nextFocusRequest(EditorTextKey(it, firstItemIndex)) }
                _editorState.value = StudyEditorUiState(
                    doc = doc,
                    activeBlockId = firstId,
                    activeListItemIndex = firstItemIndex,
                    focusRequest = initialFocusRequest,
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
        setActiveBlock(blockId, null)
    }

    fun setActiveBlock(blockId: BlockId, itemIndex: Int?) {
        activateEditor(blockId, itemIndex, requestFocus = true)
    }

    /** Called only after the real BasicRichTextEditor reports physical focus. */
    fun onEditorFocused(blockId: BlockId, itemIndex: Int? = null) {
        val previousBlock = _editorState.value.activeBlockId
        val previousItem = _editorState.value.activeListItemIndex
        _editorState.value = _editorState.value.copy(
            activeBlockId = blockId,
            activeListItemIndex = itemIndex,
            focusRequest = null,
        )
        StudyEditorDebugLog.log(
            "FOCUS_CONFIRMED",
            "vm=split from=${previousBlock?.value}.$previousItem " +
                "to=${blockId.value}.$itemIndex physical=true",
        )
        richStateFor(blockId, itemIndex)?.let { syncActiveFormat(it) }
    }

    private fun nextFocusRequest(target: EditorTextKey): EditorFocusRequest =
        EditorFocusRequest(target = target, sequence = ++focusRequestSequence)

    private fun activateEditor(
        blockId: BlockId,
        itemIndex: Int?,
        requestFocus: Boolean,
    ) {
        val previousBlock = _editorState.value.activeBlockId
        val previousItem = _editorState.value.activeListItemIndex
        val request = if (requestFocus) nextFocusRequest(EditorTextKey(blockId, itemIndex)) else null
        _editorState.value = _editorState.value.copy(
            activeBlockId = blockId,
            activeListItemIndex = itemIndex,
            focusRequest = request ?: _editorState.value.focusRequest,
        )
        StudyEditorDebugLog.log(
            "FOCUS_STATE",
            "vm=split from=${previousBlock?.value}.$previousItem to=${blockId.value}.$itemIndex " +
                "request=${request?.sequence ?: "none"}",
        )
        richStateFor(blockId, itemIndex)?.let { syncActiveFormat(it) }
    }

    fun richStateFor(blockId: BlockId, itemIndex: Int? = null): RichTextState? =
        if (itemIndex == null) blockRichStates[blockId]
        else listItemRichStates[EditorTextKey(blockId, itemIndex)]

    private fun activeRichState(): RichTextState? {
        val blockId = _editorState.value.activeBlockId ?: return null
        return richStateFor(blockId, _editorState.value.activeListItemIndex)
    }

    private fun initializeRichStates(block: StudyBlock) {
        when (block) {
            is StudyBlock.BulletList -> block.items.forEachIndexed { index, text ->
                val rs = RichTextState().apply { syncFromStyledText(text) }
                listItemRichStates[EditorTextKey(block.id, index)] = rs
                lastRichTexts[EditorTextKey(block.id, index)] = StyledText.fromAnnotatedString(rs.annotatedString)
            }
            is StudyBlock.OrderedList -> block.items.forEachIndexed { index, text ->
                val rs = RichTextState().apply { syncFromStyledText(text) }
                listItemRichStates[EditorTextKey(block.id, index)] = rs
                lastRichTexts[EditorTextKey(block.id, index)] = StyledText.fromAnnotatedString(rs.annotatedString)
            }
            else -> {
                val text = block.toStyledTextList().firstOrNull() ?: StyledText.Empty
                val rs = RichTextState().apply { syncFromStyledText(text) }
                blockRichStates[block.id] = rs
                lastRichTexts[EditorTextKey(block.id)] = StyledText.fromAnnotatedString(rs.annotatedString)
            }
        }
    }

    private fun resetRichStatesForBlock(blockId: BlockId) {
        blockRichStates.remove(blockId)
        listItemRichStates.keys.toList()
            .filter { it.blockId == blockId }
            .forEach { listItemRichStates.remove(it) }
        lastRichTexts.keys.toList()
            .filter { it.blockId == blockId }
            .forEach { lastRichTexts.remove(it) }
        _editorState.value.doc.blocks.firstOrNull { it.id == blockId }?.let(::initializeRichStates)
    }

    fun syncActiveFormat(richState: RichTextState) {
        val sel = richState.selection
        val fontSize: Int? = if (sel.collapsed) {
            // Cursor sin selección: leer fontSize del carácter actual
            val ts = richState.currentSpanStyle.fontSize
            if (ts.type == TextUnitType.Sp) ts.value.toInt() else null
        } else {
            // Selección: detectar si todos los caracteres tienen el mismo fontSize
            val sizes = mutableSetOf<Int>()
            for (offset in sel.start until sel.end.coerceAtMost(richState.annotatedString.text.length)) {
                val s = richState.getSpanStyle(androidx.compose.ui.text.TextRange(offset, offset + 1))
                val sz = s.fontSize?.let { if (it.type == TextUnitType.Sp) it.value.toInt() else null }
                if (sz != null) sizes.add(sz)
            }
            when {
                sizes.isEmpty() -> null
                sizes.size == 1 -> sizes.first()
                else -> -1  // mezcla → mostrar "-"
            }
        }
        val style = richState.currentSpanStyle
        _editorState.value = _editorState.value.copy(
            activeFormat = ActiveFormatSnapshot(
                bold = style.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold,
                italic = style.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic,
                underline = style.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.Underline) == true,
                strikethrough = style.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.LineThrough) == true,
                color = style.color.takeIf { it != androidx.compose.ui.graphics.Color.Unspecified }?.toArgb(),
                background = style.background.takeIf { it != androidx.compose.ui.graphics.Color.Unspecified }?.toArgb(),
                fontSize = fontSize,
            )
        )
    }

    fun applyStyleToActive(kind: TextStyleKind) {
        val activeId = _editorState.value.activeBlockId ?: return
        val rs = activeRichState() ?: return
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
        flushRichTextForStructuralEdit(afterBlockId)
        val newBlock = when (type) {
            "heading1" -> StudyBlock.Heading(level = 1)
            "heading2" -> StudyBlock.Heading(level = 2)
            "heading3" -> StudyBlock.Heading(level = 3)
            "bullet" -> StudyBlock.BulletList()
            "ordered" -> StudyBlock.OrderedList()
            "quote" -> StudyBlock.Quote()
            else -> StudyBlock.Paragraph()
        }
        initializeRichStates(newBlock)
        applyOp(StudyOp.InsertBlock(newBlock, afterBlockId))
        val activeItemIndex = if (newBlock is StudyBlock.BulletList || newBlock is StudyBlock.OrderedList) 0 else null
        StudyEditorDebugLog.log(
            "BLOCK_INSERT",
            "vm=split type=$type block=${newBlock.id.value} activeItem=$activeItemIndex",
        )
        activateEditor(newBlock.id, activeItemIndex, requestFocus = true)
    }

    private fun flushRichTextForStructuralEdit(blockId: BlockId?) {
        blockId ?: return
        val block = _editorState.value.doc.blocks.firstOrNull { it.id == blockId }
        val itemIndex = when (block) {
            is StudyBlock.BulletList, is StudyBlock.OrderedList ->
                _editorState.value.activeListItemIndex ?: 0
            else -> null
        }
        richStateFor(blockId, itemIndex)?.let { state ->
            StudyEditorDebugLog.log(
                "STRUCTURAL_FLUSH",
                "vm=split block=${blockId.value} item=$itemIndex " +
                    "stateLen=${state.annotatedString.length} " +
                    "docLen=${block?.plainText()?.length}",
            )
            onRichTextChanged(blockId, state, itemIndex)
        }
    }

    fun changeBlockType(blockId: BlockId, newType: String) {
        val oldType = _editorState.value.doc.blocks.firstOrNull { it.id == blockId }?.let { it::class.simpleName } ?: "?"
        StudyEditorDebugLog.log(
            "BLOCK_TYPE_START",
            "vm=split block=${blockId.value} old=$oldType new=$newType " +
                "active=${_editorState.value.activeBlockId?.value} " +
                "activeItem=${_editorState.value.activeListItemIndex}",
        )
        Log.d("LIST_DEBUG", "changeBlockType id=${blockId.value} old=$oldType -> new=$newType")
        flushRichTextForStructuralEdit(blockId)
        val applied = applyOp(StudyOp.ChangeBlockType(blockId, newType))
        if (!applied) {
            StudyEditorDebugLog.log(
                "BLOCK_TYPE_RESULT",
                "vm=split block=${blockId.value} applied=false error=${_editorState.value.lastError}",
            )
            return
        }
        resetRichStatesForBlock(blockId)
        val updatedBlock = _editorState.value.doc.blocks.firstOrNull { it.id == blockId }
        val activeItemIndex = if (updatedBlock is StudyBlock.BulletList || updatedBlock is StudyBlock.OrderedList) 0 else null
        StudyEditorDebugLog.log(
            "BLOCK_TYPE_APPLIED",
            "vm=split block=${blockId.value} result=${updatedBlock?.let { it::class.simpleName }} " +
                "activeItem=$activeItemIndex textLen=${updatedBlock?.plainText()?.length}",
        )
        // Forzar re-focus: sacar foco ahora, restaurar en el próximo frame
        activateEditor(blockId, activeItemIndex, requestFocus = true)
        StudyEditorDebugLog.log(
            "BLOCK_TYPE_FOCUS_READY",
            "vm=split block=${blockId.value} activeItem=$activeItemIndex",
        )
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
        Log.d("BIBLION_CRASH", "insertVerseAsQuote CALLED book=$book chapter=$chapter verse=$verseStart-$verseEnd version=$version text.length=${text.length}")
        viewModelScope.launch {
            Log.d("BIBLION_CRASH", "insertVerseAsQuote launching direct")
            insertVerseAsQuoteInternal(
                book = book, chapter = chapter,
                verseStart = verseStart, verseEnd = verseEnd,
                text = text, version = version,
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
        val contents = mapOf(version to text)

        val verseBlock = StudyBlock.Verse(
            bookId = book,
            chapter = chapter,
            verseStart = verseStart,
            verseEnd = verseEnd,
            sourceVersion = version,
            contents = contents,
            showCompare = false,
        )

        val afterId = _editorState.value.activeBlockId
            ?: _editorState.value.doc.blocks.lastOrNull()?.id

        applyOp(StudyOp.InsertBlock(verseBlock, afterId))

        // Insertar un párrafo vacío debajo de la cita para continuar escribiendo
        val nextBlock = StudyBlock.Paragraph()
        val nextRS = RichTextState().apply { setHtml("<p></p>") }
        blockRichStates[nextBlock.id] = nextRS
        applyOp(StudyOp.InsertBlock(nextBlock, afterBlockId = verseBlock.id))
        activateEditor(nextBlock.id, null, requestFocus = true)
    }

    private fun applyOp(op: StudyOp): Boolean {
        val before = currentCheckpoint()
        val (newDoc, result) = StudyDocEngine.apply(_editorState.value.doc, op)
        if (result.isSuccess) {
            history.pushBeforeChange(before)
            _editorState.value = _editorState.value.copy(
                doc = newDoc,
                lastError = null,
                hasUnsavedChanges = true
            )
            refreshHistoryState()
            scheduleAutoSave()
            StudyEditorDebugLog.log(
                "OP_APPLIED",
                "vm=split op=${op::class.simpleName} blocks=${StudyEditorDebugLog.blocksSummary(newDoc.blocks)}",
            )
            return true
        } else {
            _editorState.value = _editorState.value.copy(
                lastError = (result as com.cristiancogollo.biblion.feature.studydocs.engine.OpResult.Failed).reason
            )
            StudyEditorDebugLog.log(
                "OP_FAILED",
                "vm=split op=${op::class.simpleName} error=${_editorState.value.lastError}",
            )
            return false
        }
    }

    private fun markDirty() {
        _editorState.value = _editorState.value.copy(hasUnsavedChanges = true)
        scheduleAutoSave()
    }

    private fun currentRichTexts(
        overrides: Map<EditorTextKey, StyledText> = emptyMap(),
    ): Map<EditorTextKey, StyledText> = buildMap {
        _editorState.value.doc.blocks.forEach { block ->
            when (block) {
                is StudyBlock.BulletList -> block.items.forEachIndexed { index, item ->
                    val key = EditorTextKey(block.id, index)
                    put(key, overrides[key] ?: listItemRichStates[key]?.let {
                        StyledText.fromAnnotatedString(it.annotatedString)
                    } ?: item)
                }
                is StudyBlock.OrderedList -> block.items.forEachIndexed { index, item ->
                    val key = EditorTextKey(block.id, index)
                    put(key, overrides[key] ?: listItemRichStates[key]?.let {
                        StyledText.fromAnnotatedString(it.annotatedString)
                    } ?: item)
                }
                else -> {
                    val key = EditorTextKey(block.id)
                    put(key, overrides[key] ?: blockRichStates[block.id]?.let {
                        StyledText.fromAnnotatedString(it.annotatedString)
                    } ?: block.toStyledTextList().firstOrNull() ?: StyledText.Empty)
                }
            }
        }
    }

    private fun currentCheckpoint(
        overrides: Map<EditorTextKey, StyledText> = emptyMap(),
    ): EditorCheckpoint {
        val texts = currentRichTexts(overrides)
        return EditorCheckpoint(
            doc = _editorState.value.doc.withEditorTexts(texts),
            richTexts = texts,
            activeBlockId = _editorState.value.activeBlockId,
            activeListItemIndex = _editorState.value.activeListItemIndex,
        )
    }

    private fun refreshHistoryState() {
        _editorState.value = _editorState.value.copy(
            canUndo = history.canUndo,
            canRedo = history.canRedo,
        )
    }

    fun onRichTextChanged(blockId: BlockId, richState: RichTextState, itemIndex: Int? = null) {
        val key = EditorTextKey(blockId, itemIndex)
        val next = StyledText.fromAnnotatedString(richState.annotatedString)
        val previous = lastRichTexts[key]
        StudyEditorDebugLog.log(
            "RICH_SYNC",
            "vm=split block=${blockId.value} item=$itemIndex " +
                "previousLen=${previous?.length} nextLen=${next.length} " +
                "nextPreview=${StudyEditorDebugLog.textPreview(next.raw)} " +
                "doc=${StudyEditorDebugLog.blocksSummary(_editorState.value.doc.blocks)}",
        )
        if (previous == null) {
            StudyEditorDebugLog.log("RICH_SYNC_INIT", "vm=split block=${blockId.value} item=$itemIndex")
            lastRichTexts[key] = next
            return
        }
        if (previous == next) {
            StudyEditorDebugLog.log("RICH_SYNC_IGNORE", "vm=split block=${blockId.value} reason=equal")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastTextHistoryAt > TEXT_HISTORY_GROUP_MS) {
            history.pushBeforeChange(currentCheckpoint(mapOf(key to previous)))
            refreshHistoryState()
        }
        lastTextHistoryAt = now
        lastRichTexts[key] = next
        val texts = currentRichTexts(mapOf(key to next))
        _editorState.value = _editorState.value.copy(
            doc = _editorState.value.doc.withEditorTexts(texts).copy(updatedAt = now),
            hasUnsavedChanges = true,
        )
        StudyEditorDebugLog.log(
            "RICH_SYNC_APPLIED",
            "vm=split block=${blockId.value} item=$itemIndex " +
                "doc=${StudyEditorDebugLog.blocksSummary(_editorState.value.doc.blocks)}",
        )
        scheduleAutoSave()
    }

    private fun restoreCheckpoint(checkpoint: EditorCheckpoint) {
        val validIds = checkpoint.doc.blocks.map { it.id }.toSet()
        blockRichStates.keys.toList()
            .filter { it !in validIds }
            .forEach { blockRichStates.remove(it) }
        val validKeys = checkpoint.richTexts.keys
        listItemRichStates.keys.toList()
            .filter { it !in validKeys }
            .forEach { listItemRichStates.remove(it) }
        lastRichTexts.keys.toList().filter { it !in validKeys }.forEach { lastRichTexts.remove(it) }
        checkpoint.richTexts.forEach { (key, styled) ->
            val state = if (key.itemIndex == null) {
                blockRichStates[key.blockId] ?: RichTextState().also { blockRichStates[key.blockId] = it }
            } else {
                listItemRichStates[key] ?: RichTextState().also { listItemRichStates[key] = it }
            }
            lastRichTexts[key] = styled
            state.syncFromStyledText(styled)
        }
        _editorState.value = _editorState.value.copy(
            doc = checkpoint.doc,
            activeBlockId = checkpoint.activeBlockId,
            activeListItemIndex = checkpoint.activeListItemIndex,
            focusRequest = null,
            hasUnsavedChanges = true,
            lastError = null,
        )
        checkpoint.activeBlockId?.let {
            activateEditor(it, checkpoint.activeListItemIndex, requestFocus = true)
        }
        refreshHistoryState()
        scheduleAutoSave()
    }

    fun undo() {
        val previous = history.undo(currentCheckpoint()) ?: return
        restoreCheckpoint(previous)
    }

    fun redo() {
        val next = history.redo(currentCheckpoint()) ?: return
        restoreCheckpoint(next)
    }

    fun saveNow(title: String, tags: List<String>) {
        _autoSaveJob?.cancel()
        val current = _editorState.value.doc
            .copy(
                title = title.trim(),
                metadata = com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata(tags = tags),
            )
        val texts = currentRichTexts()
        val doc = current
            .withEditorTexts(texts)
            .copy(
                remoteId = _remoteId ?: current.id.value,
                updatedAt = System.currentTimeMillis(),
            )
        _editorState.value = _editorState.value.copy(
            doc = doc,
            isSaving = true,
            lastError = null,
        )
        viewModelScope.launch {
            try {
                repository.save(doc)
                _editorState.value = _editorState.value.copy(
                    lastSavedAt = System.currentTimeMillis(),
                    hasUnsavedChanges = false,
                    isSaving = false,
                )
            } catch (error: Throwable) {
                _editorState.value = _editorState.value.copy(
                    isSaving = false,
                    hasUnsavedChanges = true,
                    lastError = error.message ?: "No se pudo guardar el documento",
                )
            }
        }
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    override fun onCleared() {
        _autoSaveJob?.cancel()
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
            kotlinx.coroutines.delay(AUTO_SAVE_DELAY_MS)
            saveNow(_editorState.value.doc.title, _editorState.value.doc.metadata.tags)
        }
    }

    // ============ Métodos adicionales requeridos por el editor ============

    fun handleEnter(blockId: BlockId): Boolean {
        val block = _editorState.value.doc.blocks.firstOrNull { it.id == blockId } ?: return false
        val currentState = richStateFor(blockId, _editorState.value.activeListItemIndex)
        StudyEditorDebugLog.log(
            "VM_ENTER_START",
            "vm=split block=${blockId.value} type=${block::class.simpleName} " +
                "cursor=${currentState?.selection} stateLen=${currentState?.annotatedString?.length} " +
                "doc=${StudyEditorDebugLog.blocksSummary(_editorState.value.doc.blocks)}",
        )
        if (block is StudyBlock.BulletList || block is StudyBlock.OrderedList) {
            return handleListEnter(blockId, block)
        }
        val rs = blockRichStates[blockId] ?: return false
        val sel = rs.selection
        val text = rs.annotatedString.text
        Log.d("LIST_DEBUG", "handleEnter blockType=${block::class.simpleName} id=${blockId.value} text=[$text] len=${text.length} cursor=${sel.start} collapsed=${sel.collapsed}")

        when (block) {
            is StudyBlock.BulletList, is StudyBlock.OrderedList -> {
                if (text.isBlank()) {
                    // Lista vacía: Enter crea otro ítem vacío del mismo tipo (no convierte a paragraph)
                    Log.d("LIST_DEBUG", "handleEnter -> LIST EMPTY: creating new empty ${block::class.simpleName}")
                    val newBlock = if (block is StudyBlock.BulletList) {
                        StudyBlock.BulletList(fontFamily = block.fontFamily, fontSize = block.fontSize)
                    } else {
                        StudyBlock.OrderedList(fontFamily = block.fontFamily, fontSize = block.fontSize)
                    }
                    val newRS = RichTextState().apply { setHtml("<p></p>") }
                    blockRichStates[newBlock.id] = newRS
                    applyOp(StudyOp.SplitBlock(blockId, newBlock))
                      activateEditor(newBlock.id, 0, requestFocus = true)
                    return true
                }
                // Lista con texto: dividir texto en el cursor como párrafo normal
                val fullHtml = rs.toHtml()
                val cursorPos = sel.start

                if (cursorPos >= text.length) {
                    Log.d("LIST_DEBUG", "handleEnter -> LIST AT END: cursor=$cursorPos textLen=${text.length} creating new empty ${block::class.simpleName}")
                    // Cursor al final: crear nuevo bloque del mismo tipo vacío
                    val newBlock = if (block is StudyBlock.BulletList) {
                        StudyBlock.BulletList(fontFamily = block.fontFamily, fontSize = block.fontSize)
                    } else {
                        StudyBlock.OrderedList(fontFamily = block.fontFamily, fontSize = block.fontSize)
                    }
                    val newRS = RichTextState().apply { setHtml("<p></p>") }
                    blockRichStates[newBlock.id] = newRS
                    applyOp(StudyOp.SplitBlock(blockId, newBlock))
                    activateEditor(newBlock.id, 0, requestFocus = true)
                } else {
                    Log.d("LIST_DEBUG", "handleEnter -> LIST MIDDLE: cursor=$cursorPos textLen=${text.length} splitting text")
                    // Cursor en medio: dividir texto, nuevo bloque hereda tipo
                    rs.removeTextRange(androidx.compose.ui.text.TextRange(cursorPos, text.length))
                    val temp = RichTextState()
                    temp.setHtml(fullHtml)
                    temp.removeTextRange(androidx.compose.ui.text.TextRange(0, cursorPos))
                    val leftText = StyledText.fromAnnotatedString(rs.annotatedString)
                    val rightText = StyledText.fromAnnotatedString(temp.annotatedString)
                    val updatedSplitBlock = when (block) {
                        is StudyBlock.BulletList -> block.copy(
                            items = block.items.toMutableList().apply {
                                if (isNotEmpty()) this[0] = leftText
                            },
                        )
                        is StudyBlock.OrderedList -> block.copy(
                            items = block.items.toMutableList().apply {
                                if (isNotEmpty()) this[0] = leftText
                            },
                        )
                        else -> block
                    }
                    val newBlock = if (block is StudyBlock.BulletList) {
                        StudyBlock.BulletList(
                            items = listOf(rightText),
                            alignment = block.alignment,
                            fontFamily = block.fontFamily,
                            fontSize = block.fontSize,
                        )
                    } else {
                        StudyBlock.OrderedList(
                            items = listOf(rightText),
                            alignment = block.alignment,
                            fontFamily = block.fontFamily,
                            fontSize = block.fontSize,
                        )
                    }
                    val newRS = RichTextState().apply { syncFromStyledText(rightText) }
                    blockRichStates[newBlock.id] = newRS
                    lastRichTexts[EditorTextKey(newBlock.id)] = rightText
                    applyOp(StudyOp.SplitBlock(blockId, newBlock, updatedSplitBlock))
                      activateEditor(newBlock.id, null, requestFocus = true)
                }
            }
            else -> {
                // Comportamiento estándar existente
                if (sel.start >= text.length) {
                    insertBlock(blockId, "paragraph")
                    return true
                }
                val fullHtml = rs.toHtml()
                val cursorPos = sel.start
                rs.removeTextRange(androidx.compose.ui.text.TextRange(cursorPos, text.length))
                val temp = RichTextState()
                temp.setHtml(fullHtml)
                temp.removeTextRange(androidx.compose.ui.text.TextRange(0, cursorPos))
                val leftText = StyledText.fromAnnotatedString(rs.annotatedString)
                val rightText = StyledText.fromAnnotatedString(temp.annotatedString)
                val updatedSplitBlock = when (block) {
                    is StudyBlock.Paragraph -> block.copy(text = leftText)
                    is StudyBlock.Heading -> block.copy(text = leftText)
                    is StudyBlock.Quote -> block.copy(text = leftText)
                    else -> block
                }
                StudyEditorDebugLog.log(
                    "VM_ENTER_SPLIT",
                    "vm=split block=${blockId.value} cursor=$cursorPos " +
                        "leftLen=${leftText.length} rightLen=${rightText.length} " +
                        "leftPreview=${StudyEditorDebugLog.textPreview(leftText.raw)} " +
                        "rightPreview=${StudyEditorDebugLog.textPreview(rightText.raw)}",
                )
                val newBlock = StudyBlock.Paragraph(
                    text = rightText,
                    alignment = block.alignment,
                    fontFamily = block.fontFamily,
                    fontSize = block.fontSize,
                )
                val newRS = RichTextState().apply { syncFromStyledText(rightText) }
                blockRichStates[newBlock.id] = newRS
                lastRichTexts[EditorTextKey(newBlock.id)] = rightText
                StudyEditorDebugLog.log(
                    "VM_ENTER_APPLY",
                    "vm=split old=${blockId.value} new=${newBlock.id.value} " +
                        "updatedLeftLen=${updatedSplitBlock.plainText().length}",
                )
                applyOp(StudyOp.SplitBlock(blockId, newBlock, updatedSplitBlock))
                StudyEditorDebugLog.log(
                    "VM_ENTER_DONE",
                    "vm=split active=${newBlock.id.value} " +
                        "doc=${StudyEditorDebugLog.blocksSummary(_editorState.value.doc.blocks)}",
                )
                  activateEditor(newBlock.id, 0, requestFocus = true)
            }
        }
        return true
    }

    fun handleBackspace(blockId: BlockId): Boolean {
        val idx = _editorState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return false
        val block = _editorState.value.doc.blocks[idx]
        if (block is StudyBlock.BulletList || block is StudyBlock.OrderedList) {
            return handleListBackspace(blockId, block)
        }
        val currentRS = blockRichStates[blockId] ?: return false

        val sel = currentRS.selection
        Log.d("LIST_DEBUG", "handleBackspace blockType=${block::class.simpleName} id=${blockId.value} cursor=${sel.start} collapsed=${sel.collapsed}")
        if (!sel.collapsed || sel.start > 0) {
            Log.d("LIST_DEBUG", "handleBackspace -> SKIP: not at start or has selection")
            return false
        }

        when (block) {
            is StudyBlock.BulletList, is StudyBlock.OrderedList -> {
                Log.d("LIST_DEBUG", "handleBackspace -> LIST: mutating to paragraph")
                // Primer Backspace: mutar a paragraph (desvincular)
                changeBlockType(blockId, "paragraph")
                return true
            }
            else -> {
                if (idx <= 0) return false
                val prevBlock = _editorState.value.doc.blocks[idx - 1]
                val prevRS = blockRichStates[prevBlock.id] ?: return false
                val joinOffset = prevRS.annotatedString.text.length
                val currentHtml = currentRS.toHtml()
                prevRS.setHtml(prevRS.toHtml() + currentHtml)
                blockRichStates.remove(blockId)
                applyOp(StudyOp.MergeBlock(removeBlockId = blockId))
                prevRS.selection = androidx.compose.ui.text.TextRange(joinOffset)
                  activateEditor(prevBlock.id, null, requestFocus = true)
            }
        }
        return true
    }

    private fun removeListItemState(blockId: BlockId, itemIndex: Int) {
        listItemRichStates.remove(EditorTextKey(blockId, itemIndex))
        val remainingIndexes = listItemRichStates.keys
            .filter { it.blockId == blockId && it.itemIndex != null && it.itemIndex > itemIndex }
            .mapNotNull { it.itemIndex }
            .sorted()
        remainingIndexes.forEach { oldIndex ->
            val state = listItemRichStates.remove(EditorTextKey(blockId, oldIndex))
            if (state != null) listItemRichStates[EditorTextKey(blockId, oldIndex - 1)] = state
        }
        lastRichTexts.keys.toList()
            .filter { it.blockId == blockId && it.itemIndex != null }
            .forEach { lastRichTexts.remove(it) }
    }

    private fun insertListItemState(blockId: BlockId, itemIndex: Int, state: RichTextState) {
        val indexes = listItemRichStates.keys
            .filter { it.blockId == blockId && it.itemIndex != null && it.itemIndex >= itemIndex }
            .mapNotNull { it.itemIndex }
            .sortedDescending()
        indexes.forEach { oldIndex ->
            val moved = listItemRichStates.remove(EditorTextKey(blockId, oldIndex))
            if (moved != null) listItemRichStates[EditorTextKey(blockId, oldIndex + 1)] = moved
        }
        listItemRichStates[EditorTextKey(blockId, itemIndex)] = state
    }

    private fun commitListItems(
        blockId: BlockId,
        items: List<StyledText>,
        activeItemIndex: Int,
        before: EditorCheckpoint,
    ) {
        val index = _editorState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return
        val block = _editorState.value.doc.blocks[index]
        val updated = when (block) {
            is StudyBlock.BulletList -> block.copy(items = items)
            is StudyBlock.OrderedList -> block.copy(items = items)
            else -> return
        }
        val blocks = _editorState.value.doc.blocks.toMutableList().also { it[index] = updated }
        items.forEachIndexed { itemIndex, text ->
            lastRichTexts[EditorTextKey(blockId, itemIndex)] = text
        }
        history.pushBeforeChange(before)
        val normalizedActiveItem = activeItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        _editorState.value = _editorState.value.copy(
            doc = _editorState.value.doc.copy(blocks = blocks, updatedAt = System.currentTimeMillis()),
            activeBlockId = blockId,
            activeListItemIndex = normalizedActiveItem,
            hasUnsavedChanges = true,
        )
        activateEditor(blockId, normalizedActiveItem, requestFocus = true)
        refreshHistoryState()
        scheduleAutoSave()
    }

    private fun handleListEnter(blockId: BlockId, block: StudyBlock): Boolean {
        val itemIndex = (_editorState.value.activeListItemIndex ?: 0).coerceIn(0, block.toStyledTextList().lastIndex)
        val state = richStateFor(blockId, itemIndex) ?: return false
        val current = StyledText.fromAnnotatedString(state.annotatedString)
        if (current.raw.isBlank() && block.toStyledTextList().size == 1) {
            changeBlockType(blockId, "paragraph")
            return true
        }
        val splitAt = state.selection.start.coerceIn(0, current.length)
        val left = current.slice(0 until splitAt)
        val right = current.slice(splitAt until current.length)
        val before = currentCheckpoint()
        val items = block.toStyledTextList().toMutableList().apply {
            this[itemIndex] = left
            add(itemIndex + 1, right)
        }
        state.syncFromStyledText(left)
        insertListItemState(blockId, itemIndex + 1, RichTextState().apply {
            syncFromStyledText(right)
        })
        commitListItems(blockId, items, itemIndex + 1, before)
        return true
    }

    private fun handleListBackspace(blockId: BlockId, block: StudyBlock): Boolean {
        val itemIndex = (_editorState.value.activeListItemIndex ?: 0).coerceIn(0, block.toStyledTextList().lastIndex)
        val currentState = richStateFor(blockId, itemIndex) ?: return false
        if (!currentState.selection.collapsed || currentState.selection.start > 0) return false
        val items = block.toStyledTextList()
        if (itemIndex == 0 && items.size == 1) {
            changeBlockType(blockId, "paragraph")
            return true
        }
        val before = currentCheckpoint()
        val nextItems = items.toMutableList()
        val nextIndex = if (itemIndex > 0) {
            val previous = items[itemIndex - 1].append(StyledText.fromAnnotatedString(currentState.annotatedString))
            nextItems[itemIndex - 1] = previous
            nextItems.removeAt(itemIndex)
            richStateFor(blockId, itemIndex - 1)?.apply {
                syncFromStyledText(previous)
                selection = androidx.compose.ui.text.TextRange(previous.length)
            }
            itemIndex - 1
        } else {
            nextItems.removeAt(0)
            0
        }
        removeListItemState(blockId, itemIndex)
        commitListItems(blockId, nextItems, nextIndex, before)
        return true
    }

    fun moveCursorToPrevBlock(blockId: BlockId): Boolean {
        val blocks = _editorState.value.doc.blocks
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx <= 0) {
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=split direction=prev from=${blockId.value} handled=false reason=boundary index=$idx",
            )
            return false
        }
        for (targetIndex in (idx - 1) downTo 0) {
            val target = blocks[targetIndex]
            val targetItemIndex = navigationItemIndex(target, toEnd = true)
            val targetState = richStateFor(target.id, targetItemIndex) ?: continue
            targetState.selection = androidx.compose.ui.text.TextRange(targetState.annotatedString.text.length)
            activateEditor(target.id, targetItemIndex, requestFocus = true)
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=split direction=prev from=${blockId.value} to=${target.id.value} " +
                    "targetItem=$targetItemIndex selection=${targetState.selection} handled=true",
            )
            return true
        }
        StudyEditorDebugLog.log(
            "FOCUS_MOVE_RESULT",
            "vm=split direction=prev from=${blockId.value} handled=false reason=no-target-state",
        )
        return false
    }

    fun moveCursorToNextBlock(blockId: BlockId): Boolean {
        val blocks = _editorState.value.doc.blocks
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx < 0 || idx >= blocks.size - 1) {
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=split direction=next from=${blockId.value} handled=false reason=boundary index=$idx total=${blocks.size}",
            )
            return false
        }
        for (targetIndex in (idx + 1) until blocks.size) {
            val target = blocks[targetIndex]
            val targetItemIndex = navigationItemIndex(target, toEnd = false)
            val targetState = richStateFor(target.id, targetItemIndex) ?: continue
            targetState.selection = androidx.compose.ui.text.TextRange(0)
            activateEditor(target.id, targetItemIndex, requestFocus = true)
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=split direction=next from=${blockId.value} to=${target.id.value} " +
                    "targetItem=$targetItemIndex selection=${targetState.selection} handled=true",
            )
            return true
        }
        StudyEditorDebugLog.log(
            "FOCUS_MOVE_RESULT",
            "vm=split direction=next from=${blockId.value} handled=false reason=no-target-state",
        )
        return false
    }

    fun moveCursorToPrevListItem(blockId: BlockId, itemIndex: Int): Boolean {
        if (itemIndex > 0) {
            val targetIndex = itemIndex - 1
            val targetState = richStateFor(blockId, targetIndex)
            if (targetState == null) {
                StudyEditorDebugLog.log(
                    "FOCUS_MOVE_RESULT",
                    "vm=split direction=prev-item block=${blockId.value} fromItem=$itemIndex " +
                        "toItem=$targetIndex handled=false reason=no-state",
                )
                return false
            }
            targetState.selection = androidx.compose.ui.text.TextRange(targetState.annotatedString.text.length)
            activateEditor(blockId, targetIndex, requestFocus = true)
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=split direction=prev-item block=${blockId.value} fromItem=$itemIndex " +
                    "toItem=$targetIndex selection=${targetState.selection} handled=true",
            )
            return true
        }
        return moveCursorToPrevBlock(blockId)
    }

    fun moveCursorToNextListItem(blockId: BlockId, itemIndex: Int): Boolean {
        val block = _editorState.value.doc.blocks.firstOrNull { it.id == blockId }
        val lastIndex = when (block) {
            is StudyBlock.BulletList -> block.items.lastIndex
            is StudyBlock.OrderedList -> block.items.lastIndex
            else -> -1
        }
        if (itemIndex < lastIndex) {
            val targetIndex = itemIndex + 1
            val targetState = richStateFor(blockId, targetIndex)
            if (targetState == null) {
                StudyEditorDebugLog.log(
                    "FOCUS_MOVE_RESULT",
                    "vm=split direction=next-item block=${blockId.value} fromItem=$itemIndex " +
                        "toItem=$targetIndex handled=false reason=no-state",
                )
                return false
            }
            targetState.selection = androidx.compose.ui.text.TextRange(0)
            activateEditor(blockId, targetIndex, requestFocus = true)
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=split direction=next-item block=${blockId.value} fromItem=$itemIndex " +
                    "toItem=$targetIndex selection=${targetState.selection} handled=true",
            )
            return true
        }
        return moveCursorToNextBlock(blockId)
    }

    private fun navigationItemIndex(block: StudyBlock, toEnd: Boolean): Int? = when (block) {
        is StudyBlock.BulletList -> if (toEnd) block.items.lastIndex.coerceAtLeast(0) else 0
        is StudyBlock.OrderedList -> if (toEnd) block.items.lastIndex.coerceAtLeast(0) else 0
        else -> null
    }

    fun setActiveTextColor(argb: Int) {
        val activeId = _editorState.value.activeBlockId ?: return
        val rs = activeRichState() ?: return
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
        val rs = activeRichState() ?: return
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
        val rs = activeRichState() ?: return
        val sel = rs.selection

        if (sel.collapsed) {
            // Sin selección: limpiar color pendiente
            rs.clearSpanStyles()
        } else {
            // Con selección: preservar bold/italic/underline/strikethrough, quitar color/background
            val keepStyle = androidx.compose.ui.text.SpanStyle(
                fontWeight = rs.currentSpanStyle.fontWeight,
                fontStyle = rs.currentSpanStyle.fontStyle,
                textDecoration = rs.currentSpanStyle.textDecoration,
                fontSize = rs.currentSpanStyle.fontSize,
            )
            rs.clearSpanStyles(sel)
            if (keepStyle != androidx.compose.ui.text.SpanStyle()) {
                rs.addSpanStyle(keepStyle, sel)
            }
        }
        syncActiveFormat(rs)
        markDirty()
    }

    fun stepFontSizeActive(delta: Int) {
        val activeId = _editorState.value.activeBlockId ?: return
        val rs = activeRichState() ?: return
        val sel = rs.selection
        val textLength = rs.annotatedString.text.length
        val isFullSelection = !sel.collapsed && sel.start == 0 && sel.end >= textLength

        if (sel.collapsed || isFullSelection) {
            // Reglas de negocio para cursor colapsado (o selección que cubre todo):
            //   1) textLen == 0 (bloque vacío): cambiar block.fontSize.
            //   2) textLen > 0 (cursor en texto): NO tocar block.fontSize.
            //      Registrar Estilo de Escritura Pendiente (Pending Font Size)
            //      en el RichTextState para que el próximo carácter se escriba
            //      con SpanStyle(fontSize = newSize). El texto existente conserva
            //      su tamaño actual.
            val idx = _editorState.value.doc.blocks.indexOfFirst { it.id == activeId }
            if (idx < 0) return
            val block = _editorState.value.doc.blocks[idx]
            val baseSize = block.fontSize
            val newSize = (baseSize + delta).coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)

            if (textLength == 0) {
                val before = currentCheckpoint()
                // Regla 1: bloque vacío -> modificar tamaño base del bloque.
                val updated = when (block) {
                    is StudyBlock.Paragraph -> block.copy(fontSize = newSize)
                    is StudyBlock.Heading -> block.copy(fontSize = newSize)
                    is StudyBlock.BulletList -> block.copy(fontSize = newSize)
                    is StudyBlock.OrderedList -> block.copy(fontSize = newSize)
                    is StudyBlock.Quote -> block.copy(fontSize = newSize)
                    is StudyBlock.Verse -> block
                }
                val newBlocks = _editorState.value.doc.blocks.toMutableList()
                newBlocks[idx] = updated
                _editorState.value = _editorState.value.copy(
                    doc = _editorState.value.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
                    hasUnsavedChanges = true,
                )
                history.pushBeforeChange(before)
                refreshHistoryState()
            } else {
                // Regla 2: cursor en texto -> no tocar el bloque, registrar tamaño
                // pendiente en el RichTextState. El próximo carácter que se escriba
                // quedará envuelto en SpanStyle(fontSize = newSize).
                // El base se toma del currentSpanStyle (si está definido) para que
                // cada click acumule: +1 desde 16 -> 17, +1 desde 17 -> 18, etc.
                val currentFontSizeSp = rs.currentSpanStyle.fontSize
                val currentFontSize = if (currentFontSizeSp.isSpecified) {
                    currentFontSizeSp.value.toInt()
                } else {
                    block.fontSize
                }
                val baseSize = currentFontSize
                val newSize = (baseSize + delta).coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
                val newSizeSp = newSize.sp
                if (currentFontSizeSp != newSizeSp) {
                    rs.toggleSpanStyle(
                        androidx.compose.ui.text.SpanStyle(fontSize = newSizeSp),
                    )
                }
            }
        } else {
            // Con selección parcial: detectar tamaño base y normalizar SOLO la selección
            val baseSize = detectBaseFontSize(rs, sel)
            val newSize = (baseSize + delta).coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
            rs.addSpanStyle(
                androidx.compose.ui.text.SpanStyle(fontSize = newSize.sp),
                sel,
            )
        }
        syncActiveFormat(rs)
        markDirty()
    }

    /**
     * Detecta el tamaño de fuente base de una selección.
     * Si todos los caracteres tienen el mismo tamaño, retorna ese tamaño.
     * Si tienen tamaños distintos, retorna el del primer carácter.
     */
    private fun detectBaseFontSize(rs: RichTextState, sel: androidx.compose.ui.text.TextRange): Int {
        val sizes = mutableListOf<Int>()
        for (offset in sel.start until sel.end.coerceAtMost(rs.annotatedString.text.length)) {
            val style = rs.getSpanStyle(androidx.compose.ui.text.TextRange(offset, offset + 1))
            val size = style.fontSize?.let {
                if (it.type == TextUnitType.Sp) it.value.toInt() else null
            }
            if (size != null) sizes.add(size)
        }
        return sizes.firstOrNull() ?: 16
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
            is StudyBlock.Verse -> block
        }
        val before = currentCheckpoint()
        val newBlocks = blocks.toMutableList()
        newBlocks[idx] = updated
        _editorState.value = _editorState.value.copy(
            doc = _editorState.value.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
            hasUnsavedChanges = true
        )
        history.pushBeforeChange(before)
        refreshHistoryState()
        scheduleAutoSave()
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
            is StudyBlock.Verse -> block
        }
        val before = currentCheckpoint()
        val newBlocks = _editorState.value.doc.blocks.toMutableList()
        newBlocks[idx] = updated
        _editorState.value = _editorState.value.copy(
            doc = _editorState.value.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
            hasUnsavedChanges = true
        )
        history.pushBeforeChange(before)
        refreshHistoryState()
        scheduleAutoSave()
    }

    fun setBlockAlignment(blockId: BlockId, alignment: com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment) {
        val idx = _editorState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return
        val block = _editorState.value.doc.blocks[idx]
        val updated = when (block) {
            is StudyBlock.Paragraph -> block.copy(alignment = alignment)
            is StudyBlock.Heading -> block.copy(alignment = alignment)
            is StudyBlock.BulletList -> block.copy(alignment = alignment)
            is StudyBlock.OrderedList -> block.copy(alignment = alignment)
            is StudyBlock.Quote -> block.copy(alignment = alignment)
            is StudyBlock.Verse -> block.copy(alignment = alignment)
        }
        val before = currentCheckpoint()
        val newBlocks = _editorState.value.doc.blocks.toMutableList()
        newBlocks[idx] = updated
        _editorState.value = _editorState.value.copy(
            doc = _editorState.value.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
            hasUnsavedChanges = true,
        )
        history.pushBeforeChange(before)
        refreshHistoryState()
        scheduleAutoSave()
    }

    class Factory(private val repository: StudyDocRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return StudyDocSplitViewModel(repository, androidx.lifecycle.SavedStateHandle()) as T
        }
    }

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

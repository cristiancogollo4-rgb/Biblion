package com.cristiancogollo.biblion.feature.studydocs.domain

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.debug.StudyEditorDebugLog
import com.cristiancogollo.biblion.feature.studydocs.engine.OpResult
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyOp
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyDocEngine
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.capabilities
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.hasPersistableTitle
import com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.syncFromStyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.fromAnnotatedString
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveFormatSnapshot(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val color: Int? = null,
    val background: Int? = null,
    val fontSize: Int? = null,
    val alignment: BlockAlignment = BlockAlignment.Start,
)

data class StudyEditorUiState(
    val doc: StudyDoc = StudyDoc.empty(),
    val activeBlockId: BlockId? = null,
    val activeListItemIndex: Int? = null,
    val focusRequest: EditorFocusRequest? = null,
    val activeFormat: ActiveFormatSnapshot = ActiveFormatSnapshot(),
    val isLoading: Boolean = false,
    val lastError: String? = null,
    val lastSavedAt: Long? = null,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

enum class TextStyleKind { Bold, Italic, Underline, Strikethrough }

class StudyDocViewModel(private val repository: StudyDocRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyEditorUiState(isLoading = true))
    val uiState: StateFlow<StudyEditorUiState> = _uiState.asStateFlow()

    val blockRichStates: SnapshotStateMap<BlockId, RichTextState> =
        androidx.compose.runtime.mutableStateMapOf()
    val listItemRichStates: SnapshotStateMap<EditorTextKey, RichTextState> =
        androidx.compose.runtime.mutableStateMapOf()

    private var _autoSaveJob: Job? = null
    private var _remoteId: String? = null
    private var _isDocLoaded = false
    private val history = EditorHistory()
    private val listBackspaceExitTracker = ListBackspaceExitTracker()
    private val lastRichTexts = mutableMapOf<EditorTextKey, StyledText>()
    private var lastTextHistoryAt = 0L
    private var focusRequestSequence = 0L

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 3000L
        private const val TEXT_HISTORY_GROUP_MS = 500L
    }

    fun newDraft() {
        _remoteId = null
        blockRichStates.clear()
        listItemRichStates.clear()
        lastRichTexts.clear()
        history.clear()
        listBackspaceExitTracker.clear()
        val block = StudyBlock.Paragraph()
        val rs = RichTextState().apply { setHtml("<p></p>") }
        blockRichStates[block.id] = rs
        lastRichTexts[EditorTextKey(block.id)] = StyledText.fromAnnotatedString(rs.annotatedString)
        val initialFocusRequest = nextFocusRequest(EditorTextKey(block.id))
        _uiState.value = StudyEditorUiState(
            doc = StudyDoc(blocks = listOf(block)),
            activeBlockId = block.id,
            focusRequest = initialFocusRequest,
            hasUnsavedChanges = false,
        )
        _isDocLoaded = true
    }

    fun loadByRemoteId(remoteId: String) {
        Log.d("BIBLION_STUDY", "StudyDocViewModel.loadByRemoteId start remoteId=$remoteId")
        _autoSaveJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val doc = repository.getByRemoteId(remoteId)
            Log.d("BIBLION_STUDY", "StudyDocViewModel.loadByRemoteId repo returned doc=${doc != null} blocks=${doc?.blocks?.size}")
            if (doc != null) {
                _remoteId = doc.remoteId
                blockRichStates.clear()
                listItemRichStates.clear()
                lastRichTexts.clear()
                doc.blocks.forEach { block ->
                    initializeRichStates(block)
                }
                history.clear()
                Log.d("BIBLION_STUDY", "StudyDocViewModel.loadByRemoteId blockRichStates populated size=${blockRichStates.size}")
                val firstBlock = doc.blocks.firstOrNull()
                val firstId = firstBlock?.id
                val firstItemIndex = firstBlock?.let { navigationItemIndex(it, toEnd = false) }
                val initialFocusRequest = firstId?.let { nextFocusRequest(EditorTextKey(it, firstItemIndex)) }
                _uiState.value = StudyEditorUiState(
                    doc = doc,
                    activeBlockId = firstId,
                    activeListItemIndex = firstItemIndex,
                    focusRequest = initialFocusRequest,
                    isLoading = false,
                )
                _isDocLoaded = true
            } else {
                Log.d("BIBLION_STUDY", "StudyDocViewModel.loadByRemoteId doc NOT FOUND remoteId=$remoteId")
                _uiState.update { it.copy(isLoading = false, lastError = "Documento no encontrado") }
            }
        }
    }

    fun setActiveBlock(blockId: BlockId) {
        setActiveBlock(blockId, null)
    }

    fun setActiveBlock(blockId: BlockId, itemIndex: Int?) {
        val block = _uiState.value.doc.blocks.firstOrNull { it.id == blockId } ?: return
        activateEditor(blockId, itemIndex, requestFocus = block !is StudyBlock.Verse)
        if (block is StudyBlock.Verse) {
            _uiState.update {
                it.copy(activeFormat = ActiveFormatSnapshot(fontSize = block.fontSize))
            }
        }
    }

    /** Called only after the real BasicRichTextEditor reports physical focus. */
    fun onEditorFocused(blockId: BlockId, itemIndex: Int? = null) {
        val previousBlock = _uiState.value.activeBlockId
        val previousItem = _uiState.value.activeListItemIndex
        _uiState.update {
            it.copy(
                activeBlockId = blockId,
                activeListItemIndex = itemIndex,
                focusRequest = null,
            )
        }
        StudyEditorDebugLog.log(
            "FOCUS_CONFIRMED",
            "vm=single from=${previousBlock?.value}.$previousItem " +
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
        val previousBlock = _uiState.value.activeBlockId
        val previousItem = _uiState.value.activeListItemIndex
        val request = if (requestFocus) nextFocusRequest(EditorTextKey(blockId, itemIndex)) else null
        _uiState.update {
            it.copy(
                activeBlockId = blockId,
                activeListItemIndex = itemIndex,
                focusRequest = request ?: it.focusRequest,
            )
        }
        StudyEditorDebugLog.log(
            "FOCUS_STATE",
            "vm=single from=${previousBlock?.value}.$previousItem to=${blockId.value}.$itemIndex " +
                "request=${request?.sequence ?: "none"}",
        )
        richStateFor(blockId, itemIndex)?.let { syncActiveFormat(it) }
    }

    fun richStateFor(blockId: BlockId, itemIndex: Int? = null): RichTextState? =
        if (itemIndex == null) blockRichStates[blockId]
        else listItemRichStates[EditorTextKey(blockId, itemIndex)]

    private fun activeRichState(): RichTextState? {
        val blockId = _uiState.value.activeBlockId ?: return null
        return richStateFor(blockId, _uiState.value.activeListItemIndex)
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
        _uiState.value.doc.blocks.firstOrNull { it.id == blockId }?.let(::initializeRichStates)
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
        _uiState.update {
            it.copy(
                activeFormat = ActiveFormatSnapshot(
                    bold = style.fontWeight == FontWeight.Bold,
                    italic = style.fontStyle == FontStyle.Italic,
                    underline = style.textDecoration?.contains(TextDecoration.Underline) == true,
                    strikethrough = style.textDecoration?.contains(TextDecoration.LineThrough) == true,
                    color = style.color.takeIf { it != Color.Unspecified }?.toArgb(),
                    background = style.background.takeIf { it != Color.Unspecified }?.toArgb(),
                    fontSize = fontSize,
                )
            )
        }
    }

    fun applyStyleToActive(kind: TextStyleKind) {
        val activeId = _uiState.value.activeBlockId ?: return
        if (_uiState.value.doc.blocks.firstOrNull { it.id == activeId }
                ?.capabilities()?.supportsInlineFormatting == false
        ) return
        val rs = activeRichState() ?: return
        when (kind) {
            TextStyleKind.Bold -> rs.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            TextStyleKind.Italic -> rs.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
            TextStyleKind.Underline -> rs.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            TextStyleKind.Strikethrough -> rs.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        }
        onRichTextChanged(activeId, rs, _uiState.value.activeListItemIndex)
        syncActiveFormat(rs)
    }

    fun setActiveTextColor(argb: Int) {
        val activeId = _uiState.value.activeBlockId ?: return
        if (_uiState.value.doc.blocks.firstOrNull { it.id == activeId }
                ?.capabilities()?.supportsInlineFormatting == false
        ) return
        val rs = activeRichState() ?: return
        if (rs.selection.collapsed) {
            rs.toggleSpanStyle(SpanStyle(color = Color(argb)))
        } else {
            rs.addSpanStyle(SpanStyle(color = Color(argb)))
        }
        onRichTextChanged(activeId, rs, _uiState.value.activeListItemIndex)
        syncActiveFormat(rs)
    }

    fun setActiveBackgroundColor(argb: Int) {
        val activeId = _uiState.value.activeBlockId ?: return
        if (_uiState.value.doc.blocks.firstOrNull { it.id == activeId }
                ?.capabilities()?.supportsInlineFormatting == false
        ) return
        val rs = activeRichState() ?: return
        if (rs.selection.collapsed) {
            rs.toggleSpanStyle(SpanStyle(background = Color(argb)))
        } else {
            rs.addSpanStyle(SpanStyle(background = Color(argb)))
        }
        onRichTextChanged(activeId, rs, _uiState.value.activeListItemIndex)
        syncActiveFormat(rs)
    }

    fun clearActiveColor() {
        val activeId = _uiState.value.activeBlockId ?: return
        if (_uiState.value.doc.blocks.firstOrNull { it.id == activeId }
                ?.capabilities()?.supportsInlineFormatting == false
        ) return
        val rs = activeRichState() ?: return
        val sel = rs.selection

        if (sel.collapsed) {
            rs.clearSpanStyles()
        } else {
            val keepStyle = SpanStyle(
                fontWeight = rs.currentSpanStyle.fontWeight,
                fontStyle = rs.currentSpanStyle.fontStyle,
                textDecoration = rs.currentSpanStyle.textDecoration,
                fontSize = rs.currentSpanStyle.fontSize,
            )
            rs.clearSpanStyles(sel)
            if (keepStyle != SpanStyle()) {
                rs.addSpanStyle(keepStyle, sel)
            }
        }
        onRichTextChanged(activeId, rs, _uiState.value.activeListItemIndex)
        syncActiveFormat(rs)
    }

    fun insertBlock(afterBlockId: BlockId?, type: String) {
        flushRichTextForStructuralEdit(afterBlockId)
        val newBlock = when (type) {
            "heading1" -> StudyBlock.Heading(level = 1, fontSize = DocConfig.HEADING1_SIZE)
            "heading2" -> StudyBlock.Heading(level = 2, fontSize = DocConfig.HEADING2_SIZE)
            "heading3" -> StudyBlock.Heading(level = 3, fontSize = DocConfig.HEADING3_SIZE)
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
            "vm=single type=$type block=${newBlock.id.value} activeItem=$activeItemIndex",
        )
        activateEditor(newBlock.id, activeItemIndex, requestFocus = true)
    }

    private fun flushRichTextForStructuralEdit(blockId: BlockId?) {
        blockId ?: return
        val block = _uiState.value.doc.blocks.firstOrNull { it.id == blockId }
        val itemIndex = when (block) {
            is StudyBlock.BulletList, is StudyBlock.OrderedList ->
                _uiState.value.activeListItemIndex ?: 0
            else -> null
        }
        richStateFor(blockId, itemIndex)?.let { state ->
            StudyEditorDebugLog.log(
                "STRUCTURAL_FLUSH",
                "vm=single block=${blockId.value} item=$itemIndex " +
                    "stateLen=${state.annotatedString.length} " +
                    "docLen=${block?.plainText()?.length}",
            )
            onRichTextChanged(blockId, state, itemIndex)
        }
    }

    fun changeBlockType(blockId: BlockId, newType: String) {
        val oldType = _uiState.value.doc.blocks.firstOrNull { it.id == blockId }?.let { it::class.simpleName } ?: "?"
        StudyEditorDebugLog.log(
            "BLOCK_TYPE_START",
            "vm=single block=${blockId.value} old=$oldType new=$newType " +
                "active=${_uiState.value.activeBlockId?.value} " +
                "activeItem=${_uiState.value.activeListItemIndex}",
        )
        flushRichTextForStructuralEdit(blockId)
        val applied = applyOp(StudyOp.ChangeBlockType(blockId, newType))
        if (!applied) {
            StudyEditorDebugLog.log(
                "BLOCK_TYPE_RESULT",
                "vm=single block=${blockId.value} applied=false error=${_uiState.value.lastError}",
            )
            return
        }
        resetRichStatesForBlock(blockId)
        val updatedBlock = _uiState.value.doc.blocks.firstOrNull { it.id == blockId }
        val activeItemIndex = if (updatedBlock is StudyBlock.BulletList || updatedBlock is StudyBlock.OrderedList) 0 else null
        StudyEditorDebugLog.log(
            "BLOCK_TYPE_APPLIED",
            "vm=single block=${blockId.value} result=${updatedBlock?.let { it::class.simpleName }} " +
                "activeItem=$activeItemIndex textLen=${updatedBlock?.plainText()?.length}",
        )
        activateEditor(blockId, activeItemIndex, requestFocus = true)
        StudyEditorDebugLog.log(
            "BLOCK_TYPE_FOCUS_READY",
            "vm=single block=${blockId.value} activeItem=$activeItemIndex",
        )
    }

    fun insertVerseCitation(
        book: String,
        chapter: Int,
        verseStart: Int,
        verseEnd: Int,
        text: String,
        version: String,
        verseNumbers: List<Int> = emptyList(),
    ) {
        val verseBlock = StudyBlock.Verse(
            bookId = book,
            chapter = chapter,
            verseStart = verseStart,
            verseEnd = verseEnd,
            verseNumbers = verseNumbers,
            sourceVersion = version,
            contents = mapOf(version to text),
        )
        val afterId = _uiState.value.activeBlockId
            ?: _uiState.value.doc.blocks.lastOrNull()?.id
        if (!applyOp(StudyOp.InsertBlock(verseBlock, afterId))) return

        val nextBlock = StudyBlock.Paragraph()
        blockRichStates[nextBlock.id] = RichTextState().apply { setHtml("<p></p>") }
        applyOp(StudyOp.InsertBlock(nextBlock, afterBlockId = verseBlock.id))
        activateEditor(nextBlock.id, null, requestFocus = true)
    }

    fun updateVerseComparisons(
        blockId: BlockId,
        comparedVersions: List<String>,
        comparedContents: Map<String, String>,
    ) {
        val version = comparedVersions.firstOrNull()
        applyOp(
            StudyOp.SetVerseComparison(
                blockId = blockId,
                version = version,
                content = version?.let(comparedContents::get),
            )
        )
    }

    fun deleteBlock(blockId: BlockId) {
        val index = _uiState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return
        blockRichStates.remove(blockId)
        listItemRichStates.keys.toList()
            .filter { it.blockId == blockId }
            .forEach { listItemRichStates.remove(it) }
        lastRichTexts.keys.toList()
            .filter { it.blockId == blockId }
            .forEach { lastRichTexts.remove(it) }
        if (!applyOp(StudyOp.DeleteBlock(blockId))) return

        val blocks = _uiState.value.doc.blocks
        val target = blocks.getOrNull(index.coerceAtMost(blocks.lastIndex)) ?: return
        val itemIndex = navigationItemIndex(target, toEnd = false)
        if (richStateFor(target.id, itemIndex) == null) initializeRichStates(target)
        setActiveBlock(target.id, itemIndex)
    }

    fun cycleBlockAlignment(blockId: BlockId) {
        val idx = _uiState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return
        val block = _uiState.value.doc.blocks[idx]
        val next = when (block.alignment) {
            BlockAlignment.Start -> BlockAlignment.Center
            BlockAlignment.Center -> BlockAlignment.End
            BlockAlignment.End -> BlockAlignment.Justify
            BlockAlignment.Justify -> BlockAlignment.Start
        }
        val updated = when (block) {
            is StudyBlock.Paragraph -> block.copy(alignment = next)
            is StudyBlock.Heading -> block.copy(alignment = next)
            is StudyBlock.BulletList -> block.copy(alignment = next)
            is StudyBlock.OrderedList -> block.copy(alignment = next)
            is StudyBlock.Quote -> block.copy(alignment = next)
            is StudyBlock.Verse -> block.copy(alignment = next)
        }
        val before = currentCheckpoint()
        val newBlocks = _uiState.value.doc.blocks.toMutableList()
        newBlocks[idx] = updated
        val newDoc = _uiState.value.doc.copy(
            blocks = newBlocks,
            updatedAt = System.currentTimeMillis(),
        )
        history.pushBeforeChange(before)
        refreshHistoryState()
        _uiState.update { it.copy(doc = newDoc, hasUnsavedChanges = true) }
        scheduleAutoSave()
    }

    fun stepFontSizeActive(delta: Int) {
        val activeId = _uiState.value.activeBlockId ?: return
        val activeBlock = _uiState.value.doc.blocks.firstOrNull { it.id == activeId } ?: return
        if (activeBlock is StudyBlock.Verse) {
            val size = (activeBlock.fontSize + delta)
                .coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
            applyOp(StudyOp.UpdateBlock(activeBlock.copy(fontSize = size)))
            _uiState.update {
                it.copy(activeFormat = ActiveFormatSnapshot(fontSize = size))
            }
            return
        }
        val rs = activeRichState() ?: return
        val sel = rs.selection

        if (sel.collapsed) {
            val idx = _uiState.value.doc.blocks.indexOfFirst { it.id == activeId }
            if (idx < 0) return
            val block = _uiState.value.doc.blocks[idx]
            val newSize = (block.fontSize + delta).coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
            val updated = when (block) {
                is StudyBlock.Paragraph -> block.copy(fontSize = newSize)
                is StudyBlock.Heading -> block.copy(fontSize = newSize)
                is StudyBlock.BulletList -> block.copy(fontSize = newSize)
                is StudyBlock.OrderedList -> block.copy(fontSize = newSize)
                is StudyBlock.Quote -> block.copy(fontSize = newSize)
                is StudyBlock.Verse -> block.copy(fontSize = newSize)
            }
            val before = currentCheckpoint()
            val newBlocks = _uiState.value.doc.blocks.toMutableList()
            newBlocks[idx] = updated
            _uiState.update {
                it.copy(
                    doc = it.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
                    hasUnsavedChanges = true,
                )
            }
        } else {
            // Con selección: modificar solo el tamaño y conservar color, fondo y énfasis.
            val baseSize = detectBaseFontSize(rs, sel)
            val newSize = (baseSize + delta).coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
            rs.applyFontSizePreservingStyles(sel, newSize.toFloat())
        }
        onRichTextChanged(activeId, rs, _uiState.value.activeListItemIndex)
        syncActiveFormat(rs)
        if (rs.annotatedString.isEmpty()) scheduleAutoSave()
    }

    fun setFontSizeActive(fontSize: Int) {
        val activeId = _uiState.value.activeBlockId ?: return
        val activeBlock = _uiState.value.doc.blocks.firstOrNull { it.id == activeId } ?: return
        val normalizedSize = fontSize.coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
        if (activeBlock is StudyBlock.Verse) {
            applyOp(StudyOp.UpdateBlock(activeBlock.copy(fontSize = normalizedSize)))
            _uiState.update {
                it.copy(activeFormat = ActiveFormatSnapshot(fontSize = normalizedSize))
            }
            return
        }
        val rs = activeRichState() ?: return
        val selection = rs.selection
        val textLength = rs.annotatedString.length

        if (selection.collapsed && textLength == 0) {
            val index = _uiState.value.doc.blocks.indexOfFirst { it.id == activeId }
            if (index < 0) return
            val before = currentCheckpoint()
            val blocks = _uiState.value.doc.blocks.toMutableList()
            blocks[index] = blocks[index].withBaseFontSize(normalizedSize)
            _uiState.update {
                it.copy(
                    doc = it.doc.copy(
                        blocks = blocks,
                        updatedAt = System.currentTimeMillis(),
                    ),
                    hasUnsavedChanges = true,
                )
            }
            history.pushBeforeChange(before)
            refreshHistoryState()
            scheduleAutoSave()
        } else if (selection.collapsed) {
            rs.addSpanStyle(SpanStyle(fontSize = normalizedSize.sp))
        } else {
            rs.applyFontSizePreservingStyles(selection, normalizedSize.toFloat())
        }

        onRichTextChanged(activeId, rs, _uiState.value.activeListItemIndex)
        syncActiveFormat(rs)
    }

    /**
     * Detecta el tamaño de fuente base de una selección.
     * Si todos los caracteres tienen el mismo tamaño, retorna ese tamaño.
     * Si tienen tamaños distintos, retorna el del primer carácter.
     */
    private fun detectBaseFontSize(rs: com.mohamedrejeb.richeditor.model.RichTextState, sel: androidx.compose.ui.text.TextRange): Int {
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

    fun handleEnter(blockId: BlockId): Boolean {
        val block = _uiState.value.doc.blocks.firstOrNull { it.id == blockId } ?: return false
        val currentState = richStateFor(blockId, _uiState.value.activeListItemIndex)
        StudyEditorDebugLog.log(
            "VM_ENTER_START",
            "vm=single block=${blockId.value} type=${block::class.simpleName} " +
                "cursor=${currentState?.selection} stateLen=${currentState?.annotatedString?.length} " +
                "doc=${StudyEditorDebugLog.blocksSummary(_uiState.value.doc.blocks)}",
        )
        if (block is StudyBlock.BulletList || block is StudyBlock.OrderedList) {
            return handleListEnter(blockId, block)
        }
        val itemIndex = _uiState.value.activeListItemIndex ?: 0
        val rs = richStateFor(blockId, itemIndex.takeIf { block is StudyBlock.BulletList || block is StudyBlock.OrderedList })
            ?: return false
        val sel = rs.selection
        val text = rs.annotatedString.text

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
            "vm=single block=${blockId.value} cursor=$cursorPos " +
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
            "vm=single old=${blockId.value} new=${newBlock.id.value} " +
                "updatedLeftLen=${updatedSplitBlock.plainText().length}",
        )
        applyOp(StudyOp.SplitBlock(blockId, newBlock, updatedSplitBlock))
        StudyEditorDebugLog.log(
            "VM_ENTER_DONE",
            "vm=single active=${newBlock.id.value} " +
                "doc=${StudyEditorDebugLog.blocksSummary(_uiState.value.doc.blocks)}",
        )
        activateEditor(newBlock.id, null, requestFocus = true)
        return true
    }

    fun handleBackspace(blockId: BlockId): Boolean {
        val idx = _uiState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return false
        val block = _uiState.value.doc.blocks[idx]
        if (block is StudyBlock.BulletList || block is StudyBlock.OrderedList) {
            return handleListBackspace(blockId, block)
        }
        val itemIndex = _uiState.value.activeListItemIndex ?: 0
        val currentRS = richStateFor(
            blockId,
            itemIndex.takeIf { block is StudyBlock.BulletList || block is StudyBlock.OrderedList },
        ) ?: return false

        val sel = currentRS.selection
        if (!sel.collapsed || sel.start > 0) return false

        if (idx <= 0) return false
        val prevBlock = _uiState.value.doc.blocks[idx - 1]
        val prevRS = blockRichStates[prevBlock.id] ?: return false
        val joinOffset = prevRS.annotatedString.text.length
        val mergedText = StyledText.fromAnnotatedString(prevRS.annotatedString)
            .append(StyledText.fromAnnotatedString(currentRS.annotatedString))
        val updatedPreviousBlock = prevBlock.withText(mergedText) ?: return false
        applyOp(
            StudyOp.MergeBlock(
                removeBlockId = blockId,
                updatedTargetBlock = updatedPreviousBlock,
            ),
        )
        prevRS.syncFromStyledText(mergedText)
        lastRichTexts[EditorTextKey(prevBlock.id)] =
            StyledText.fromAnnotatedString(prevRS.annotatedString)
        blockRichStates.remove(blockId)
        lastRichTexts.remove(EditorTextKey(blockId))
        prevRS.selection = androidx.compose.ui.text.TextRange(joinOffset)
        activateEditor(prevBlock.id, null, requestFocus = true)
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

    private fun handleListEnter(blockId: BlockId, block: StudyBlock): Boolean {
        listBackspaceExitTracker.clear()
        val itemIndex = (_uiState.value.activeListItemIndex ?: 0).coerceIn(0, block.toStyledTextList().lastIndex)
        val state = richStateFor(blockId, itemIndex) ?: return false
        val current = StyledText.fromAnnotatedString(state.annotatedString)
        val currentItems = block.toStyledTextList()
        if (current.raw.isBlank()) {
            if (currentItems.size == 1) {
                changeBlockType(blockId, "paragraph")
                return true
            }
            if (itemIndex == currentItems.lastIndex) {
                return exitListFromEmptyItem(blockId, block, itemIndex, "enter")
            }
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
        val itemIndex = (_uiState.value.activeListItemIndex ?: 0).coerceIn(0, block.toStyledTextList().lastIndex)
        val currentState = richStateFor(blockId, itemIndex) ?: return false
        if (!currentState.selection.collapsed) {
            listBackspaceExitTracker.clear()
            return false
        }
        val cursorAtEnd = currentState.selection.start == currentState.annotatedString.length
        if (cursorAtEnd && listBackspaceExitTracker.consume(blockId)) {
            StudyEditorDebugLog.log(
                "LIST_BACKSPACE_EXIT",
                "vm=single block=${blockId.value}",
            )
            insertBlock(blockId, "paragraph")
            return true
        }
        if (!cursorAtEnd) listBackspaceExitTracker.clear()
        if (currentState.selection.start > 0) return false
        val items = block.toStyledTextList()
        if (itemIndex == 0 && items.size == 1) {
            changeBlockType(blockId, "paragraph")
            return true
        }
        val before = currentCheckpoint()
        val currentText = StyledText.fromAnnotatedString(currentState.annotatedString)
        val armListExit = itemIndex == items.lastIndex &&
            itemIndex > 0 &&
            currentText.raw.isBlank()
        val nextItems = items.toMutableList()
        val nextIndex = if (itemIndex > 0) {
            val previous = if (armListExit) {
                items[itemIndex - 1]
            } else {
                items[itemIndex - 1].append(currentText)
            }
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
        if (armListExit) {
            listBackspaceExitTracker.arm(blockId)
            StudyEditorDebugLog.log(
                "LIST_BACKSPACE_ARM",
                "vm=single block=${blockId.value} item=$itemIndex nextItem=$nextIndex",
            )
        }
        return true
    }

    private fun exitListFromEmptyItem(
        blockId: BlockId,
        block: StudyBlock,
        itemIndex: Int,
        source: String,
    ): Boolean {
        val remainingItems = block.toStyledTextList().toMutableList().apply {
            removeAt(itemIndex)
        }
        val updatedList = when (block) {
            is StudyBlock.BulletList -> block.copy(items = remainingItems)
            is StudyBlock.OrderedList -> block.copy(items = remainingItems)
            else -> return false
        }
        val paragraph = StudyBlock.Paragraph(
            alignment = block.alignment,
            fontFamily = block.fontFamily,
            fontSize = block.fontSize,
        )
        removeListItemState(blockId, itemIndex)
        initializeRichStates(paragraph)
        val applied = applyOp(StudyOp.SplitBlock(blockId, paragraph, updatedList))
        if (applied) {
            StudyEditorDebugLog.log(
                "LIST_EXIT",
                "vm=single source=$source block=${blockId.value} paragraph=${paragraph.id.value}",
            )
            activateEditor(paragraph.id, null, requestFocus = true)
        }
        return applied
    }

    private fun commitListItems(
        blockId: BlockId,
        items: List<StyledText>,
        activeItemIndex: Int,
        before: EditorCheckpoint,
    ) {
        val index = _uiState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return
        val block = _uiState.value.doc.blocks[index]
        val updated = when (block) {
            is StudyBlock.BulletList -> block.copy(items = items)
            is StudyBlock.OrderedList -> block.copy(items = items)
            else -> return
        }
        val blocks = _uiState.value.doc.blocks.toMutableList().also { it[index] = updated }
        items.forEachIndexed { itemIndex, text ->
            lastRichTexts[EditorTextKey(blockId, itemIndex)] = text
        }
        history.pushBeforeChange(before)
        val normalizedActiveItem = activeItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        _uiState.update {
            it.copy(
                doc = it.doc.copy(blocks = blocks, updatedAt = System.currentTimeMillis()),
                activeBlockId = blockId,
                activeListItemIndex = normalizedActiveItem,
                hasUnsavedChanges = true,
            )
        }
        activateEditor(blockId, normalizedActiveItem, requestFocus = true)
        refreshHistoryState()
        scheduleAutoSave()
    }

    fun moveCursorToPrevBlock(blockId: BlockId): Boolean {
        val blocks = _uiState.value.doc.blocks
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx <= 0) {
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=single direction=prev from=${blockId.value} handled=false reason=boundary index=$idx",
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
                "vm=single direction=prev from=${blockId.value} to=${target.id.value} " +
                    "targetItem=$targetItemIndex selection=${targetState.selection} handled=true",
            )
            return true
        }
        StudyEditorDebugLog.log(
            "FOCUS_MOVE_RESULT",
            "vm=single direction=prev from=${blockId.value} handled=false reason=no-target-state",
        )
        return false
    }

    fun moveCursorToNextBlock(blockId: BlockId): Boolean {
        val blocks = _uiState.value.doc.blocks
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx < 0 || idx >= blocks.size - 1) {
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=single direction=next from=${blockId.value} handled=false reason=boundary index=$idx total=${blocks.size}",
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
                "vm=single direction=next from=${blockId.value} to=${target.id.value} " +
                    "targetItem=$targetItemIndex selection=${targetState.selection} handled=true",
            )
            return true
        }
        StudyEditorDebugLog.log(
            "FOCUS_MOVE_RESULT",
            "vm=single direction=next from=${blockId.value} handled=false reason=no-target-state",
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
                    "vm=single direction=prev-item block=${blockId.value} fromItem=$itemIndex " +
                        "toItem=$targetIndex handled=false reason=no-state",
                )
                return false
            }
            targetState.selection = androidx.compose.ui.text.TextRange(targetState.annotatedString.text.length)
            activateEditor(blockId, targetIndex, requestFocus = true)
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=single direction=prev-item block=${blockId.value} fromItem=$itemIndex " +
                    "toItem=$targetIndex selection=${targetState.selection} handled=true",
            )
            return true
        }
        return moveCursorToPrevBlock(blockId)
    }

    fun moveCursorToNextListItem(blockId: BlockId, itemIndex: Int): Boolean {
        val block = _uiState.value.doc.blocks.firstOrNull { it.id == blockId }
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
                    "vm=single direction=next-item block=${blockId.value} fromItem=$itemIndex " +
                        "toItem=$targetIndex handled=false reason=no-state",
                )
                return false
            }
            targetState.selection = androidx.compose.ui.text.TextRange(0)
            activateEditor(blockId, targetIndex, requestFocus = true)
            StudyEditorDebugLog.log(
                "FOCUS_MOVE_RESULT",
                "vm=single direction=next-item block=${blockId.value} fromItem=$itemIndex " +
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

    fun setActiveFontFamily(family: String) {
        val activeId = _uiState.value.activeBlockId ?: return
        val blocks = _uiState.value.doc.blocks
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
        _uiState.update { it.copy(
            doc = it.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
            hasUnsavedChanges = true,
        )}
        history.pushBeforeChange(before)
        refreshHistoryState()
        scheduleAutoSave()
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(doc = it.doc.copy(title = newTitle), hasUnsavedChanges = true) }
        scheduleAutoSave()
    }

    fun saveNow(title: String, tags: List<String>) {
        if (title.trim().isEmpty()) return
        _uiState.update {
            it.copy(
                doc = it.doc.copy(title = title.trim(), metadata = DocMetadata(tags = tags)),
                hasUnsavedChanges = true,
            )
        }
        saveNow()
    }

    fun saveNow() {
        _autoSaveJob?.cancel()
        if (!_uiState.value.doc.hasPersistableTitle()) return
        val current = _uiState.value.doc.withEditorTexts(currentRichTexts()).copy(
            remoteId = _remoteId ?: _uiState.value.doc.id.value,
            updatedAt = System.currentTimeMillis(),
        )
        _uiState.update { it.copy(doc = current, isSaving = true, lastError = null) }
        viewModelScope.launch {
            try {
                repository.save(current)
                _uiState.update {
                    it.copy(
                        lastSavedAt = System.currentTimeMillis(),
                        hasUnsavedChanges = false,
                        isSaving = false,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasUnsavedChanges = true,
                        lastError = error.message ?: "No se pudo guardar el documento",
                    )
                }
            }
        }
    }

    fun discardDraft() {
        val remoteId = _uiState.value.doc.remoteId ?: _uiState.value.doc.id.value
        viewModelScope.launch { repository.discardDraft(remoteId) }
    }

    private fun applyOp(op: StudyOp): Boolean {
        val before = currentCheckpoint()
        val (newDoc, result) = StudyDocEngine.apply(_uiState.value.doc, op)
        if (result.isSuccess) {
            history.pushBeforeChange(before)
            _uiState.update {
                it.copy(
                    doc = newDoc,
                    lastError = null,
                    hasUnsavedChanges = true,
                )
            }
            refreshHistoryState()
            scheduleAutoSave()
            StudyEditorDebugLog.log(
                "OP_APPLIED",
                "vm=single op=${op::class.simpleName} blocks=${StudyEditorDebugLog.blocksSummary(newDoc.blocks)}",
            )
            return true
        } else {
            _uiState.update { it.copy(lastError = (result as OpResult.Failed).reason) }
            StudyEditorDebugLog.log(
                "OP_FAILED",
                "vm=single op=${op::class.simpleName} error=${_uiState.value.lastError}",
            )
            return false
        }
    }

    private fun currentRichTexts(
        overrides: Map<EditorTextKey, StyledText> = emptyMap(),
    ): Map<EditorTextKey, StyledText> = buildMap {
        _uiState.value.doc.blocks.forEach { block ->
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
            doc = _uiState.value.doc.withEditorTexts(texts),
            richTexts = texts,
            activeBlockId = _uiState.value.activeBlockId,
            activeListItemIndex = _uiState.value.activeListItemIndex,
        )
    }

    private fun refreshHistoryState() {
        _uiState.update {
            it.copy(canUndo = history.canUndo, canRedo = history.canRedo)
        }
    }

    fun updatePagedSelection(
        blockId: BlockId,
        itemIndex: Int?,
        selection: androidx.compose.ui.text.TextRange,
    ) {
        val state = richStateFor(blockId, itemIndex) ?: return
        val safeSelection = androidx.compose.ui.text.TextRange(
            selection.start.coerceIn(0, state.annotatedString.length),
            selection.end.coerceIn(0, state.annotatedString.length),
        )
        if (state.selection != safeSelection) state.selection = safeSelection
        if (_uiState.value.activeBlockId != blockId ||
            _uiState.value.activeListItemIndex != itemIndex
        ) {
            onEditorFocused(blockId, itemIndex)
        }
        syncActiveFormat(state)
    }

    fun replacePagedText(
        blockId: BlockId,
        itemIndex: Int?,
        rawText: String,
        selection: androidx.compose.ui.text.TextRange,
    ) {
        val state = richStateFor(blockId, itemIndex) ?: return
        val previous = StyledText.fromAnnotatedString(state.annotatedString)
        val next = previous.reconcileRawText(rawText, _uiState.value.activeFormat)
        state.syncFromStyledText(next)
        state.selection = androidx.compose.ui.text.TextRange(
            selection.start.coerceIn(0, next.length),
            selection.end.coerceIn(0, next.length),
        )
        onRichTextChanged(blockId, state, itemIndex)
        syncActiveFormat(state)
    }

    fun onRichTextChanged(blockId: BlockId, richState: RichTextState, itemIndex: Int? = null) {
        val key = EditorTextKey(blockId, itemIndex)
        val next = StyledText.fromAnnotatedString(richState.annotatedString)
        val previous = lastRichTexts[key]
        StudyEditorDebugLog.log(
            "RICH_SYNC",
            "vm=single block=${blockId.value} item=$itemIndex " +
                "previousLen=${previous?.length} nextLen=${next.length} " +
                "nextPreview=${StudyEditorDebugLog.textPreview(next.raw)} " +
                "doc=${StudyEditorDebugLog.blocksSummary(_uiState.value.doc.blocks)}",
        )
        if (previous == null) {
            StudyEditorDebugLog.log("RICH_SYNC_INIT", "vm=single block=${blockId.value} item=$itemIndex")
            lastRichTexts[key] = next
            return
        }
        if (previous == next) {
            StudyEditorDebugLog.log("RICH_SYNC_IGNORE", "vm=single block=${blockId.value} reason=equal")
            return
        }

        listBackspaceExitTracker.clear()
        val now = System.currentTimeMillis()
        if (now - lastTextHistoryAt > TEXT_HISTORY_GROUP_MS) {
            history.pushBeforeChange(currentCheckpoint(mapOf(key to previous)))
            refreshHistoryState()
        }
        lastTextHistoryAt = now
        lastRichTexts[key] = next
        val texts = currentRichTexts(mapOf(key to next))
        _uiState.update {
            it.copy(
                doc = it.doc.withEditorTexts(texts).copy(updatedAt = now),
                hasUnsavedChanges = true,
            )
        }
        StudyEditorDebugLog.log(
            "RICH_SYNC_APPLIED",
            "vm=single block=${blockId.value} item=$itemIndex " +
                "doc=${StudyEditorDebugLog.blocksSummary(_uiState.value.doc.blocks)}",
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
            state.syncFromStyledText(styled)
            lastRichTexts[key] = StyledText.fromAnnotatedString(state.annotatedString)
        }
        _uiState.update {
            it.copy(
                doc = checkpoint.doc,
                activeBlockId = checkpoint.activeBlockId,
                activeListItemIndex = checkpoint.activeListItemIndex,
                focusRequest = null,
                hasUnsavedChanges = true,
                lastError = null,
            )
        }
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

    private fun scheduleAutoSave() {
        if (!_isDocLoaded) return
        _autoSaveJob?.cancel()
        _autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            val current = _uiState.value.doc.withEditorTexts(currentRichTexts()).copy(
                remoteId = _remoteId ?: _uiState.value.doc.remoteId ?: _uiState.value.doc.id.value,
                updatedAt = System.currentTimeMillis(),
            )
            try {
                repository.saveDraft(current)
            } catch (error: Throwable) {
                _uiState.update { it.copy(lastError = error.message ?: "No se pudo guardar el borrador") }
            }
        }
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    fun setBlockAlignment(blockId: BlockId, alignment: com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment) {
        val idx = _uiState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return
        val block = _uiState.value.doc.blocks[idx]
        val updated = when (block) {
            is StudyBlock.Paragraph -> block.copy(alignment = alignment)
            is StudyBlock.Heading -> block.copy(alignment = alignment)
            is StudyBlock.BulletList -> block.copy(alignment = alignment)
            is StudyBlock.OrderedList -> block.copy(alignment = alignment)
            is StudyBlock.Quote -> block.copy(alignment = alignment)
            is StudyBlock.Verse -> block.copy(alignment = alignment)
        }
        val before = currentCheckpoint()
        val newBlocks = _uiState.value.doc.blocks.toMutableList()
        newBlocks[idx] = updated
        _uiState.update { it.copy(doc = it.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()), hasUnsavedChanges = true) }
        history.pushBeforeChange(before)
        refreshHistoryState()
        scheduleAutoSave()
    }

    override fun onCleared() {
        _autoSaveJob?.cancel()
        blockRichStates.clear()
        listItemRichStates.clear()
        super.onCleared()
    }

    class Factory(private val repository: StudyDocRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyDocViewModel(repository) as T
    }
}

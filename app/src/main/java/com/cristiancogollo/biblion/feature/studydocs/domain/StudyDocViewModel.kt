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
import com.cristiancogollo.biblion.feature.studydocs.engine.OpResult
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyOp
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyDocEngine
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
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
    val pendingFocusBlockId: String? = null,
    val activeFormat: ActiveFormatSnapshot = ActiveFormatSnapshot(),
    val isLoading: Boolean = false,
    val lastError: String? = null,
    val lastSavedAt: Long? = null,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)

enum class TextStyleKind { Bold, Italic, Underline, Strikethrough }

class StudyDocViewModel(private val repository: StudyDocRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyEditorUiState())
    val uiState: StateFlow<StudyEditorUiState> = _uiState.asStateFlow()

    val blockRichStates: SnapshotStateMap<BlockId, RichTextState> =
        androidx.compose.runtime.mutableStateMapOf()

    private var _autoSaveJob: Job? = null
    private var _remoteId: String? = null
    private var _isDocLoaded = false

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 3000L
    }

    fun newDraft() {
        _remoteId = null
        blockRichStates.clear()
        val block = StudyBlock.Paragraph()
        val rs = RichTextState().apply { setHtml("<p></p>") }
        blockRichStates[block.id] = rs
        _uiState.value = StudyEditorUiState(
            doc = StudyDoc(blocks = listOf(block)),
            activeBlockId = block.id,
            hasUnsavedChanges = true,
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
                Log.d("BIBLION_STUDY", "StudyDocViewModel.loadByRemoteId blockRichStates populated size=${blockRichStates.size}")
                val firstId = doc.blocks.firstOrNull()?.id
                _uiState.value = StudyEditorUiState(
                    doc = doc,
                    activeBlockId = firstId,
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
        _uiState.update { it.copy(activeBlockId = blockId) }
        blockRichStates[blockId]?.let { syncActiveFormat(it) }
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
        val rs = blockRichStates[activeId] ?: return
        when (kind) {
            TextStyleKind.Bold -> rs.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            TextStyleKind.Italic -> rs.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
            TextStyleKind.Underline -> rs.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            TextStyleKind.Strikethrough -> rs.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        }
        syncActiveFormat(rs)
        markDirty()
        scheduleAutoSave()
    }

    fun setActiveTextColor(argb: Int) {
        val activeId = _uiState.value.activeBlockId ?: return
        val rs = blockRichStates[activeId] ?: return
        if (rs.selection.collapsed) {
            rs.toggleSpanStyle(SpanStyle(color = Color(argb)))
        } else {
            rs.addSpanStyle(SpanStyle(color = Color(argb)))
        }
        syncActiveFormat(rs)
        markDirty()
        scheduleAutoSave()
    }

    fun setActiveBackgroundColor(argb: Int) {
        val activeId = _uiState.value.activeBlockId ?: return
        val rs = blockRichStates[activeId] ?: return
        if (rs.selection.collapsed) {
            rs.toggleSpanStyle(SpanStyle(background = Color(argb)))
        } else {
            rs.addSpanStyle(SpanStyle(background = Color(argb)))
        }
        syncActiveFormat(rs)
        markDirty()
        scheduleAutoSave()
    }

    fun clearActiveColor() {
        val activeId = _uiState.value.activeBlockId ?: return
        val rs = blockRichStates[activeId] ?: return
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
        syncActiveFormat(rs)
        scheduleAutoSave()
    }

    fun insertBlock(afterBlockId: BlockId?, type: String) {
        val newBlock = when (type) {
            "heading1" -> StudyBlock.Heading(level = 1, fontSize = DocConfig.HEADING1_SIZE)
            "heading2" -> StudyBlock.Heading(level = 2, fontSize = DocConfig.HEADING2_SIZE)
            "heading3" -> StudyBlock.Heading(level = 3, fontSize = DocConfig.HEADING3_SIZE)
            "bullet" -> StudyBlock.BulletList()
            "ordered" -> StudyBlock.OrderedList()
            "quote" -> StudyBlock.Quote()
            else -> StudyBlock.Paragraph()
        }
        val rs = RichTextState().apply { setHtml("<p></p>") }
        blockRichStates[newBlock.id] = rs
        applyOp(StudyOp.InsertBlock(newBlock, afterBlockId))
        _uiState.update { it.copy(activeBlockId = newBlock.id) }
    }

    fun changeBlockType(blockId: BlockId, newType: String) {
        applyOp(StudyOp.ChangeBlockType(blockId, newType))
        _uiState.update { it.copy(activeBlockId = null) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(80L)
            _uiState.update { it.copy(activeBlockId = blockId) }
        }
    }

    fun deleteBlock(blockId: BlockId) {
        if (_uiState.value.doc.blocks.size <= 1) return
        blockRichStates.remove(blockId)
        applyOp(StudyOp.DeleteBlock(blockId))
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
            is StudyBlock.Verse -> block
        }
        val newBlocks = _uiState.value.doc.blocks.toMutableList()
        newBlocks[idx] = updated
        val newDoc = _uiState.value.doc.copy(
            blocks = newBlocks,
            updatedAt = System.currentTimeMillis(),
        )
        _uiState.update { it.copy(doc = newDoc, hasUnsavedChanges = true) }
        scheduleAutoSave()
    }

    fun stepFontSizeActive(delta: Int) {
        val activeId = _uiState.value.activeBlockId ?: return
        val rs = blockRichStates[activeId] ?: return
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
            is StudyBlock.Verse -> block
            }
            val newBlocks = _uiState.value.doc.blocks.toMutableList()
            newBlocks[idx] = updated
            _uiState.update {
                it.copy(
                    doc = it.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
                    hasUnsavedChanges = true,
                )
            }
        } else {
            // Con selección: detectar tamaño base y normalizar TODA la selección
            val baseSize = detectBaseFontSize(rs, sel)
            val newSize = (baseSize + delta).coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
            rs.addSpanStyle(SpanStyle(fontSize = newSize.sp), sel)
        }
        syncActiveFormat(rs)
        scheduleAutoSave()
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

    fun handleEnter(blockId: BlockId) {
        val block = _uiState.value.doc.blocks.firstOrNull { it.id == blockId } ?: return
        val rs = blockRichStates[blockId] ?: return
        val sel = rs.selection
        val text = rs.annotatedString.text

        when (block) {
            is StudyBlock.BulletList, is StudyBlock.OrderedList -> {
                if (text.isBlank()) {
                    changeBlockType(blockId, "paragraph")
                    return
                }
                val fullHtml = rs.toHtml()
                val cursorPos = sel.start
                if (cursorPos >= text.length) {
                    val newBlock = if (block is StudyBlock.BulletList) {
                        StudyBlock.BulletList(fontFamily = block.fontFamily, fontSize = block.fontSize)
                    } else {
                        StudyBlock.OrderedList(fontFamily = block.fontFamily, fontSize = block.fontSize)
                    }
                    val newRS = RichTextState().apply { setHtml("<p></p>") }
                    blockRichStates[newBlock.id] = newRS
                    applyOp(StudyOp.SplitBlock(blockId, newBlock))
                    _uiState.update { it.copy(activeBlockId = newBlock.id) }
                } else {
                    rs.removeTextRange(androidx.compose.ui.text.TextRange(cursorPos, text.length))
                    val temp = RichTextState()
                    temp.setHtml(fullHtml)
                    temp.removeTextRange(androidx.compose.ui.text.TextRange(0, cursorPos))
                    val newBlock = if (block is StudyBlock.BulletList) {
                        StudyBlock.BulletList(fontFamily = block.fontFamily, fontSize = block.fontSize)
                    } else {
                        StudyBlock.OrderedList(fontFamily = block.fontFamily, fontSize = block.fontSize)
                    }
                    val newRS = RichTextState()
                    newRS.setHtml(temp.toHtml())
                    blockRichStates[newBlock.id] = newRS
                    applyOp(StudyOp.SplitBlock(blockId, newBlock))
                    _uiState.update { it.copy(activeBlockId = newBlock.id) }
                }
            }
            else -> {
                if (sel.start >= text.length) {
                    insertBlock(blockId, "paragraph")
                    return
                }
                val fullHtml = rs.toHtml()
                val cursorPos = sel.start
                rs.removeTextRange(androidx.compose.ui.text.TextRange(cursorPos, text.length))
                val temp = RichTextState()
                temp.setHtml(fullHtml)
                temp.removeTextRange(androidx.compose.ui.text.TextRange(0, cursorPos))
                val newBlock = StudyBlock.Paragraph()
                val newRS = RichTextState()
                newRS.setHtml(temp.toHtml())
                blockRichStates[newBlock.id] = newRS
                applyOp(StudyOp.SplitBlock(blockId, newBlock))
                _uiState.update { it.copy(activeBlockId = newBlock.id) }
            }
        }
    }

    fun handleBackspace(blockId: BlockId) {
        val idx = _uiState.value.doc.blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return
        val block = _uiState.value.doc.blocks[idx]
        val currentRS = blockRichStates[blockId] ?: return

        val sel = currentRS.selection
        if (!sel.collapsed || sel.start > 0) return

        when (block) {
            is StudyBlock.BulletList, is StudyBlock.OrderedList -> {
                changeBlockType(blockId, "paragraph")
                return
            }
            else -> {
                if (idx <= 0) return
                val prevBlock = _uiState.value.doc.blocks[idx - 1]
                val prevRS = blockRichStates[prevBlock.id] ?: return
                val joinOffset = prevRS.annotatedString.text.length
                val currentHtml = currentRS.toHtml()
                prevRS.setHtml(prevRS.toHtml() + currentHtml)
                blockRichStates.remove(blockId)
                applyOp(StudyOp.MergeBlock(removeBlockId = blockId))
                prevRS.selection = androidx.compose.ui.text.TextRange(joinOffset)
                _uiState.update { it.copy(activeBlockId = prevBlock.id) }
            }
        }
    }

    fun moveCursorToPrevBlock(blockId: BlockId) {
        val blocks = _uiState.value.doc.blocks
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx <= 0) return
        val prevId = blocks[idx - 1].id
        val prevRS = blockRichStates[prevId] ?: return
        prevRS.selection = androidx.compose.ui.text.TextRange(prevRS.annotatedString.text.length)
        _uiState.update { it.copy(activeBlockId = prevId) }
    }

    fun moveCursorToNextBlock(blockId: BlockId) {
        val blocks = _uiState.value.doc.blocks
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx < 0 || idx >= blocks.size - 1) return
        val nextId = blocks[idx + 1].id
        val nextRS = blockRichStates[nextId] ?: return
        nextRS.selection = androidx.compose.ui.text.TextRange(0)
        _uiState.update { it.copy(activeBlockId = nextId) }
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
        val newBlocks = blocks.toMutableList()
        newBlocks[idx] = updated
        _uiState.update { it.copy(
            doc = it.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()),
            hasUnsavedChanges = true,
        )}
        scheduleAutoSave()
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(doc = it.doc.copy(title = newTitle), hasUnsavedChanges = true) }
        scheduleAutoSave()
    }

    fun saveNow(title: String, tags: List<String>) {
        _uiState.update {
            it.copy(
                doc = it.doc.copy(title = title.trim(), metadata = DocMetadata(tags = tags)),
                hasUnsavedChanges = false,
            )
        }
        saveNow()
    }

    fun saveNow() {
        viewModelScope.launch {
            val current = _uiState.value.doc
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
            is StudyBlock.Verse -> block
                }
            }
            val doc = current.copy(
                blocks = blocksWithHtml,
                remoteId = _remoteId ?: current.id.value,
                updatedAt = System.currentTimeMillis(),
            )
            repository.save(doc)
            _uiState.update { it.copy(lastSavedAt = System.currentTimeMillis(), hasUnsavedChanges = false) }
        }
    }

    private fun applyOp(op: StudyOp) {
        val (newDoc, result) = StudyDocEngine.apply(_uiState.value.doc, op)
        if (result.isSuccess) {
            _uiState.update { it.copy(doc = newDoc, lastError = null, hasUnsavedChanges = true) }
            scheduleAutoSave()
        } else {
            _uiState.update { it.copy(lastError = (result as OpResult.Failed).reason) }
        }
    }

    private fun markDirty() {
        _uiState.update { it.copy(hasUnsavedChanges = true) }
    }

    private fun scheduleAutoSave() {
        if (!_isDocLoaded) return
        _autoSaveJob?.cancel()
        _autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            saveNow()
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
        val newBlocks = _uiState.value.doc.blocks.toMutableList()
        newBlocks[idx] = updated
        _uiState.update { it.copy(doc = it.doc.copy(blocks = newBlocks, updatedAt = System.currentTimeMillis()), hasUnsavedChanges = true) }
    }

    class Factory(private val repository: StudyDocRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyDocViewModel(repository) as T
    }
}

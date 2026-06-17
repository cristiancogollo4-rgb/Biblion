package com.cristiancogollo.biblion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyEditorScreen(
    viewModel: StudyViewModel,
    onClose: () -> Unit,
    onFocusModeChanged: (Boolean) -> Unit,
    currentUserName: String? = null
) {
    val ui by viewModel.state.collectAsState()
    val selectionState = viewModel.selectionState
    val context = LocalContext.current
    var availableBibleVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    var activeTextContent by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editorContainerWidthPx by remember { mutableStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }
    var saveTagsInput by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var pendingFocusBlockId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    fun openMetadataDialog() {
        saveTitle = ui.title
        saveTagsInput = ui.tags.joinToString(", ")
        saveError = null
        showSaveDialog = true
    }

    fun validateNonEmptyContent(): Boolean {
        return StudyDocumentValidator.validateContent(ui.blocks).isValid
    }

    fun handleSave() {
        val validation = StudyDocumentValidator.validateForSave(ui.title, ui.tags, ui.blocks)
        if (!validation.isValid) {
            val hasMetadataErrors = validation.errors.any { err ->
                err is ValidationError.MissingTitle ||
                    err is ValidationError.MissingPurposeTag ||
                    err is ValidationError.MissingAudienceTag ||
                    err is ValidationError.MissingTopicTag ||
                    err is ValidationError.MissingStateTag ||
                    err is ValidationError.MultipleStateTags
            }
            if (hasMetadataErrors) {
                openMetadataDialog()
            } else {
                scope.launch { snackbarHostState.showSnackbar(validation.displayMessage) }
            }
            return
        }
        viewModel.process(StudyIntent.SaveStudy)
    }

    fun selectedText(): String = selectionState.selectedText.trim()

    fun assistantCurrentOutline(): List<String> = ui.blocks.mapNotNull { block ->
        when (block) {
            is StudyBlockNode.Paragraph -> listOf(block.text, block.parallelText)
                .filter { it.isNotBlank() }
                .joinToString(" | ")
                .ifBlank { null }
            is StudyBlockNode.RichText -> block.html.takeIf { it.isNotBlank() }
            is StudyBlockNode.Citation -> block.reference.display
            is StudyBlockNode.QuotedVerse -> listOf(block.reference, block.primaryText)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { null }
            is StudyBlockNode.Question -> block.question.takeIf { it.isNotBlank() }
            is StudyBlockNode.TwoColumn -> listOf(block.leftTitle, block.leftText, block.rightTitle, block.rightText)
                .filter { it.isNotBlank() }
                .joinToString(" | ")
                .ifBlank { null }
            else -> null
        }
    }.take(12)

    fun assistantNotes(): List<String> = ui.blocks.mapNotNull { block ->
        when (block) {
            is StudyBlockNode.Note -> block.text.takeIf { it.isNotBlank() }
            is StudyBlockNode.Reflection -> listOf(block.topic, block.text)
                .filter { it.isNotBlank() }
                .joinToString(": ")
                .ifBlank { null }
            else -> null
        }
    }.take(8)

    fun updateActiveParagraphRole(role: String) {
        val blockId = selectionState.activeBlockId ?: return
        val start = selectionState.selectionStart
        val end = selectionState.selectionEnd
        if (start != end && start >= 0 && end > start) {
            selectionState.clear()
            viewModel.process(StudyIntent.ApplyRoleToSelection(blockId, role, start, end))
        } else {
            selectionState.updateActiveBlock(blockId, role, selectionState.activeAlignment)
            viewModel.process(StudyIntent.UpdateParagraphRole(blockId, role))
        }
    }

    fun updateActiveParagraphAlignment(textAlign: String) {
        val blockId = selectionState.activeBlockId ?: return
        val start = selectionState.selectionStart
        val end = selectionState.selectionEnd
        if (start != end && start >= 0 && end > start) {
            selectionState.clear()
            viewModel.process(StudyIntent.ApplyAlignmentToSelection(blockId, textAlign, start, end))
        } else {
            selectionState.updateActiveBlock(blockId, selectionState.activeRole, textAlign)
            viewModel.process(StudyIntent.UpdateParagraphAlignment(blockId, textAlign))
        }
    }

    fun insertNoteBlock() = viewModel.process(StudyIntent.AddNoteBlock(selectionState.activeBlockId))

    fun insertReflectionBlock() = viewModel.process(StudyIntent.AddReflectionBlock(selectedText(), selectionState.activeBlockId))

    fun toggleParallelText() {
        updateActiveParagraphRole(if (selectionState.activeRole == "columns") "paragraph" else "columns")
    }

    fun updateActiveSelection(
        source: String,
        text: String,
        selectionStart: Int,
        selectionEnd: Int
    ) {
        activeTextContent = text
        selectionState.updateSelection(
            blockId = selectionState.activeBlockId,
            role = selectionState.activeRole,
            alignment = selectionState.activeAlignment,
            source = source,
            start = selectionStart.coerceIn(0, text.length),
            end = selectionEnd.coerceIn(0, text.length),
            text = if (selectionStart != selectionEnd) {
                text.substring(
                    selectionStart.coerceAtMost(selectionEnd),
                    selectionStart.coerceAtLeast(selectionEnd)
                )
            } else ""
        )
    }

    fun replaceActiveTextRange(start: Int, end: Int, replacement: String) {
        val blockId = selectionState.activeBlockId ?: return
        val updated = activeTextContent.replaceRange(start, end, replacement)
        if (selectionState.isParallelSource()) {
            viewModel.process(StudyIntent.UpdateParagraphParallelText(blockId, updated))
        } else {
            viewModel.process(StudyIntent.UpdateParagraphBlock(blockId, updated))
        }
        activeTextContent = updated
        selectionState.updateRange(start, start + replacement.length, replacement)
    }

    fun transformSelectedText(transform: (String) -> String) {
        val start = selectionState.selectionStart.coerceAtMost(selectionState.selectionEnd)
        val end = selectionState.selectionStart.coerceAtLeast(selectionState.selectionEnd)
        if (start == end || activeTextContent.isEmpty()) return
        replaceActiveTextRange(start, end, transform(activeTextContent.substring(start, end)))
    }

    fun insertColumnEmbeddedBlock(block: ColumnEmbeddedBlock) {
        val blockId = selectionState.activeBlockId ?: return
        val insertionPosition = selectionState.selectionEnd
            .coerceIn(0, activeTextContent.length)
        viewModel.process(
            StudyIntent.AddColumnEmbeddedBlock(
                paragraphBlockId = blockId,
                source = selectionState.activeSource,
                block = block.copy(position = insertionPosition)
            )
        )
    }

    fun transformSelectedLines(transform: (List<String>) -> List<String>) {
        if (activeTextContent.isEmpty()) return
        val selectedStart = selectionState.selectionStart.coerceAtMost(selectionState.selectionEnd)
        val selectedEnd = selectionState.selectionStart.coerceAtLeast(selectionState.selectionEnd)
        val hasSelectionRange = selectedStart != selectedEnd
        val start = if (hasSelectionRange) {
            selectedStart
        } else {
            activeTextContent.lastIndexOf('\n', (selectedStart - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        }
        val end = if (hasSelectionRange) {
            selectedEnd
        } else {
            activeTextContent.indexOf('\n', selectedStart).let { if (it < 0) activeTextContent.length else it }
        }
        replaceActiveTextRange(
            start = start,
            end = end,
            replacement = transform(activeTextContent.substring(start, end).lines()).joinToString("\n")
        )
    }

    fun applySelectedStyle(
        color: Color? = null,
        background: Color? = null,
        bold: Boolean = false,
        italic: Boolean = false,
        underline: Boolean = false,
        fontSizeSp: Float? = null
    ) {
        val blockId = selectionState.activeBlockId ?: return
        val start = selectionState.selectionStart.coerceAtMost(selectionState.selectionEnd)
        val end = selectionState.selectionStart.coerceAtLeast(selectionState.selectionEnd)
        if (start == end) return
        viewModel.process(
            StudyIntent.ApplyParagraphTextStyle(
                blockId = blockId,
                source = selectionState.activeSource,
                start = start,
                end = end,
                color = color?.toStudyColorLong(),
                background = background?.toStudyColorLong(),
                bold = bold,
                italic = italic,
                underline = underline,
                fontSizeSp = fontSizeSp
            )
        )
    }

    fun clearSelectedStyle() {
        val blockId = selectionState.activeBlockId ?: return
        val start = selectionState.selectionStart.coerceAtMost(selectionState.selectionEnd)
        val end = selectionState.selectionStart.coerceAtLeast(selectionState.selectionEnd)
        if (start == end) return
        viewModel.process(StudyIntent.ClearParagraphTextStyle(blockId, selectionState.activeSource, start, end))
    }

    fun clearSelectedTextColor() {
        val blockId = selectionState.activeBlockId ?: return
        val start = selectionState.selectionStart.coerceAtMost(selectionState.selectionEnd)
        val end = selectionState.selectionStart.coerceAtLeast(selectionState.selectionEnd)
        if (start == end) return
        viewModel.process(
            StudyIntent.ClearParagraphTextStyle(
                blockId = blockId,
                source = selectionState.activeSource,
                start = start,
                end = end,
                clearColor = true,
                clearBackground = false,
                clearBold = false,
                clearItalic = false,
                clearUnderline = false,
                clearFontSize = false
            )
        )
    }

    fun clearSelectedBackground() {
        val blockId = selectionState.activeBlockId ?: return
        val start = selectionState.selectionStart.coerceAtMost(selectionState.selectionEnd)
        val end = selectionState.selectionStart.coerceAtLeast(selectionState.selectionEnd)
        if (start == end) return
        viewModel.process(
            StudyIntent.ClearParagraphTextStyle(
                blockId = blockId,
                source = selectionState.activeSource,
                start = start,
                end = end,
                clearColor = false,
                clearBackground = true,
                clearBold = false,
                clearItalic = false,
                clearUnderline = false,
                clearFontSize = false
            )
        )
    }

    fun clearSelectionFormatting() {
        updateActiveParagraphRole("paragraph")
        clearSelectedStyle()
    }

    fun clearSelectionTextColor() = clearSelectedTextColor()

    fun clearSelectionBackground() = clearSelectedBackground()

    val hasSelection = selectionState.selectedText.isNotBlank()
    val hasTextTarget = selectionState.activeBlockId != null
    val starterTextBlock = remember { StudyBlockNode.Paragraph(text = "") }
    val documentBlocks = if (ui.blocks.isNotEmpty()) {
        ui.blocks
    } else {
        listOf(starterTextBlock)
    }
    val editorItems = remember(documentBlocks) { documentBlocks.toEditorItems() }

    LaunchedEffect(hasSelection) {
        viewModel.process(StudyIntent.SetSelectionActive(hasSelection))
    }

    LaunchedEffect(ui.pendingCitations.size) {
        if (ui.pendingCitations.isNotEmpty()) {
            snackbarHostState.showSnackbar("${ui.pendingCitations.size} cita(s) lista(s) para insertar")
        }
    }

    LaunchedEffect(ui.focusMode) { onFocusModeChanged(ui.focusMode) }

    LaunchedEffect(Unit) {
        availableBibleVersions = BibleRepository.getAvailableVersions(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar Modo Estudio", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Editor de Estudio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón de Guardar Enseñanza
                        IconButton(onClick = {
                            handleSave()
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar Enseñanza", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        // Botón de Modo Enfoque (Ampliar)
                        IconButton(onClick = { viewModel.process(StudyIntent.ToggleFocusMode(!ui.focusMode)) }) {
                            Icon(
                                imageVector = if (ui.focusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Modo Enfoque",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            StudyEditorBottomBar(
                hasSelection = hasSelection || hasTextTarget,
                isParallelTextMode = selectionState.activeRole == "columns",
                isBulletMode = selectionState.activeRole == "bullet",
                isNumberedMode = selectionState.activeRole == "numbered",
                pendingCitations = ui.pendingCitations.size,
                onHeadlineUp = { updateActiveParagraphRole("heading") },
                onHeadlineDown = { updateActiveParagraphRole("paragraph") },
                onBold = { applySelectedStyle(bold = true) },
                onItalic = { applySelectedStyle(italic = true) },
                onUnderline = { applySelectedStyle(underline = true) },
                onIncreaseSize = {
                    viewModel.process(StudyIntent.IncreaseSelectionFont)
                    applySelectedStyle(fontSizeSp = (ui.selectionFontSizeSp + 2f).coerceAtMost(46f))
                },
                onDecreaseSize = {
                    viewModel.process(StudyIntent.DecreaseSelectionFont)
                    applySelectedStyle(fontSizeSp = (ui.selectionFontSizeSp - 2f).coerceAtLeast(12f))
                },
                onBulletList = {
                    if (selectionState.activeRole == "columns") {
                        transformSelectedLines { lines ->
                            lines.map { line ->
                                if (line.trim().startsWith("- ")) line else "- ${line.trimStart()}"
                            }
                        }
                    } else {
                        updateActiveParagraphRole(if (selectionState.activeRole == "bullet") "paragraph" else "bullet")
                    }
                },
                onOrderedList = {
                    if (selectionState.activeRole == "columns") {
                        transformSelectedLines { lines ->
                            lines.mapIndexed { index, line ->
                                val clean = line.trimStart().replace(orderedListPrefixRegex, "")
                                "${index + 1}. $clean"
                            }
                        }
                    } else {
                        updateActiveParagraphRole(if (selectionState.activeRole == "numbered") "paragraph" else "numbered")
                    }
                },
                onInsertPendingCitations = {
                    val pending = viewModel.consumePendingCitations()
                    pending.forEach { request ->
                        if (selectionState.activeRole == "columns" && selectionState.activeBlockId != null) {
                            insertColumnEmbeddedBlock(
                                ColumnEmbeddedBlock(
                                    type = "quote",
                                    title = request.reference,
                                    text = request.text
                                )
                            )
                        } else {
                            viewModel.process(StudyIntent.AddQuotedVerseBlock(selectionState.activeBlockId, request))
                        }
                    }
                },
                onInsertNote = {
                    if (selectionState.activeRole == "columns" && selectionState.activeBlockId != null) {
                        insertColumnEmbeddedBlock(
                            ColumnEmbeddedBlock(
                                type = "note",
                                title = "Nota",
                                text = "Escribe una observacion, dato curioso o aclaracion del tema."
                            )
                        )
                    } else {
                        insertNoteBlock()
                    }
                },
                onInsertReflection = {
                    if (selectionState.activeRole == "columns" && selectionState.activeBlockId != null) {
                        insertColumnEmbeddedBlock(
                            ColumnEmbeddedBlock(
                                type = "reflection",
                                title = selectedText().ifBlank { "Reflexion" },
                                text = "Desarrolla aqui una mirada mas profunda para la ensenanza."
                            )
                        )
                    } else {
                        insertReflectionBlock()
                    }
                },
                onInsertTwoColumn = { toggleParallelText() },
                onTextColor = { color -> applySelectedStyle(color = color) },
                onBackgroundColor = { color -> applySelectedStyle(background = color) },
                onClearTextColor = { clearSelectionTextColor() },
                onClearBackground = { clearSelectionBackground() },
                onClearFormatting = { clearSelectionFormatting() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = if (ui.focusMode) 64.dp else 24.dp)
                .onGloballyPositioned { coordinates ->
                    editorContainerWidthPx = coordinates.size.width
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.B -> {
                            updateActiveParagraphRole("heading")
                            true
                        }
                        Key.I -> {
                            updateActiveParagraphRole("paragraph")
                            true
                        }
                        Key.S -> {
                            handleSave()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = editorItems,
                    key = { item -> item.block.editorBlockKey() },
                    contentType = { item -> item.block::class.simpleName.orEmpty() }
                ) { item ->
                    val block = item.block
                    when (block) {
                        is StudyBlockNode.Paragraph -> {
                            StudyParagraphBlockEditor(
                                block = block,
                                listNumber = item.listNumber.coerceAtLeast(1),
                                focusOnAppear = pendingFocusBlockId == block.blockId,
                                onFocusHandled = { pendingFocusBlockId = null },
                                onActive = { source, text, start, end ->
                                    selectionState.updateActiveBlock(block.blockId, block.role, block.textAlign)
                                    selectionState.updateSource(source)
                                    updateActiveSelection(source, text, start, end)
                                },
                                onTextChanged = { text ->
                                    viewModel.process(StudyIntent.UpdateParagraphBlock(block.blockId, text))
                                },
                                onParallelTextChanged = { text ->
                                    viewModel.process(StudyIntent.UpdateParagraphParallelText(block.blockId, text))
                                },
                                onRoleChanged = { role ->
                                    selectionState.updateActiveBlock(selectionState.activeBlockId, role, selectionState.activeAlignment)
                                    viewModel.process(StudyIntent.UpdateParagraphRole(block.blockId, role))
                                },
                                onColumnEmbeddedBlockUpdate = { source, embedded ->
                                    viewModel.process(StudyIntent.UpdateColumnEmbeddedBlock(block.blockId, source, embedded))
                                },
                                onColumnEmbeddedBlockToggle = { source, embeddedId ->
                                    viewModel.process(StudyIntent.ToggleColumnEmbeddedBlockCollapsed(block.blockId, source, embeddedId))
                                },
                                onColumnEmbeddedBlockDelete = { source, embeddedId ->
                                    viewModel.process(StudyIntent.DeleteColumnEmbeddedBlock(block.blockId, source, embeddedId))
                                },
                                onSplitText = { currentText, nextText, nextRole ->
                                    val newBlockId = CuidGenerator.create()
                                    pendingFocusBlockId = newBlockId
                                    viewModel.process(StudyIntent.SplitParagraphBlock(block.blockId, newBlockId, currentText, nextText, nextRole))
                                }
                            )
                        }
                        is StudyBlockNode.RichText -> {
                            val legacyText = block.html.toPlainEditorText()
                            StudyParagraphBlockEditor(
                                block = StudyBlockNode.Paragraph(blockId = block.blockId, text = legacyText),
                                listNumber = 1,
                                focusOnAppear = pendingFocusBlockId == block.blockId,
                                onFocusHandled = { pendingFocusBlockId = null },
                                onActive = { source, text, start, end ->
                                    selectionState.updateActiveBlock(block.blockId, "paragraph", "start")
                                    selectionState.updateSource(source)
                                    updateActiveSelection(source, text, start, end)
                                },
                                onTextChanged = { text ->
                                    viewModel.process(StudyIntent.UpdateParagraphBlock(block.blockId, text))
                                },
                                onParallelTextChanged = { text ->
                                    viewModel.process(StudyIntent.UpdateParagraphParallelText(block.blockId, text))
                                },
                                onRoleChanged = { role ->
                                    selectionState.updateActiveBlock(selectionState.activeBlockId, role, selectionState.activeAlignment)
                                    viewModel.process(StudyIntent.UpdateParagraphRole(block.blockId, role))
                                },
                                onColumnEmbeddedBlockUpdate = { source, embedded ->
                                    viewModel.process(StudyIntent.UpdateColumnEmbeddedBlock(block.blockId, source, embedded))
                                },
                                onColumnEmbeddedBlockToggle = { source, embeddedId ->
                                    viewModel.process(StudyIntent.ToggleColumnEmbeddedBlockCollapsed(block.blockId, source, embeddedId))
                                },
                                onColumnEmbeddedBlockDelete = { source, embeddedId ->
                                    viewModel.process(StudyIntent.DeleteColumnEmbeddedBlock(block.blockId, source, embeddedId))
                                },
                                onSplitText = { currentText, nextText, nextRole ->
                                    val newBlockId = CuidGenerator.create()
                                    pendingFocusBlockId = newBlockId
                                    viewModel.process(StudyIntent.SplitParagraphBlock(block.blockId, newBlockId, currentText, nextText, nextRole))
                                }
                            )
                        }
                        is StudyBlockNode.Note,
                        is StudyBlockNode.Reflection,
                        is StudyBlockNode.QuotedVerse,
                        is StudyBlockNode.Question,
                        is StudyBlockNode.TwoColumn -> {
                            StudyInteractiveBlockCard(
                                block = block,
                                availableVersions = availableBibleVersions,
                                onUpdate = { updated -> viewModel.process(StudyIntent.UpdateBlock(updated)) },
                                onChangeQuotedVerseVersion = { blockId, version ->
                                    viewModel.process(StudyIntent.ChangeQuotedVerseVersion(blockId, version))
                                },
                                onCompareQuotedVerseVersion = { blockId, version ->
                                    viewModel.process(StudyIntent.CompareQuotedVerseVersion(blockId, version))
                                },
                                onToggleCollapsed = { blockId -> viewModel.process(StudyIntent.ToggleBlockCollapsed(blockId)) },
                                onDelete = { blockId -> viewModel.process(StudyIntent.DeleteBlock(blockId)) }
                            )
                        }
                        else -> Unit
                    }
                }
                item(key = "bottom-spacer") {
                    Spacer(modifier = Modifier.height(160.dp))
                }
            }

            LaunchedEffect(pendingFocusBlockId) {
                pendingFocusBlockId?.let { focusId ->
                    val index = editorItems.indexOfFirst { it.block.editorBlockKey() == focusId }
                    if (index >= 0) {
                        listState.animateScrollToItem(index, scrollOffset = -200)
                    }
                }
            }

            LaunchedEffect(selectionState.activeBlockId) {
                selectionState.activeBlockId?.let { activeId ->
                    val index = editorItems.indexOfFirst { it.block.editorBlockKey() == activeId }
                    if (index >= 0 && !listState.layoutInfo.visibleItemsInfo.any { it.key == activeId }) {
                        listState.animateScrollToItem(index, scrollOffset = -200)
                    }
                }
            }

            StudyAssistantOverlay(
                studyTitle = ui.title,
                studyTags = ui.tags,
                selectedText = selectionState.selectedText,
                currentOutline = assistantCurrentOutline(),
                notes = assistantNotes(),
                currentUserName = currentUserName,
                onInsertNote = { text ->
                    viewModel.process(StudyIntent.AddNoteBlock(selectionState.activeBlockId, text))
                    scope.launch { snackbarHostState.showSnackbar("Respuesta insertada como nota.") }
                },
                onInsertReflection = { topic, text ->
                    viewModel.process(StudyIntent.AddReflectionBlock(topic, selectionState.activeBlockId, text))
                    scope.launch { snackbarHostState.showSnackbar("Respuesta insertada como reflexion.") }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }

    if (showSaveDialog) {
        SaveTeachingDialog(
            title = saveTitle,
            onTitleChange = { saveTitle = it; saveError = null },
            tagsInput = saveTagsInput,
            onTagsInputChange = { saveTagsInput = it; saveError = null },
            error = saveError,
            onDismiss = { showSaveDialog = false },
            onSave = {
                val cleanedTitle = saveTitle.trim()
                val parsedTags = withDefaultStateTag(parseStudyTags(saveTagsInput))
                val tagError = validateRequiredStudyTags(parsedTags)
                saveError = when {
                    !validateNonEmptyContent() -> "No puedes guardar una ensenanza vacia."
                    cleanedTitle.isBlank() -> "Debes agregar un titulo para guardar."
                    tagError != null -> tagError
                    else -> null
                }
                if (saveError == null) {
                    viewModel.process(StudyIntent.SaveStudyWithMetadata(cleanedTitle, parsedTags))
                    showSaveDialog = false
                }
            }
        )
    }
}

private data class StudyEditorBlockItem(
    val block: StudyBlockNode,
    val listNumber: Int
)

private fun List<StudyBlockNode>.toEditorItems(): List<StudyEditorBlockItem> {
    var numberedIndex = 0
    return map { block ->
        numberedIndex = if (block is StudyBlockNode.Paragraph && block.role == "numbered") {
            numberedIndex + 1
        } else {
            0
        }
        StudyEditorBlockItem(block = block, listNumber = numberedIndex)
    }
}

private fun StudyBlockNode.editorBlockKey(): String = when (this) {
    is StudyBlockNode.Paragraph -> blockId
    is StudyBlockNode.RichText -> blockId
    is StudyBlockNode.Citation -> citationId
    is StudyBlockNode.Note -> blockId
    is StudyBlockNode.Reflection -> blockId
    is StudyBlockNode.QuotedVerse -> blockId
    is StudyBlockNode.Question -> blockId
    is StudyBlockNode.TwoColumn -> blockId
    is StudyBlockNode.Audio -> blockId
    is StudyBlockNode.Image -> blockId
}

@Composable
private fun StudyParagraphBlockEditor(
    block: StudyBlockNode.Paragraph,
    listNumber: Int,
    focusOnAppear: Boolean,
    onFocusHandled: () -> Unit,
    onActive: (source: String, text: String, selectionStart: Int, selectionEnd: Int) -> Unit,
    onTextChanged: (String) -> Unit,
    onParallelTextChanged: (String) -> Unit,
    onRoleChanged: (String) -> Unit,
    onColumnEmbeddedBlockUpdate: (source: String, block: ColumnEmbeddedBlock) -> Unit,
    onColumnEmbeddedBlockToggle: (source: String, embeddedBlockId: String) -> Unit,
    onColumnEmbeddedBlockDelete: (source: String, embeddedBlockId: String) -> Unit,
    onSplitText: (currentText: String, nextText: String, nextRole: String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var fieldValue by remember(block.blockId) {
        mutableStateOf(TextFieldValue(block.text, selection = TextRange(block.text.length)))
    }
    var parallelFieldValue by remember(block.blockId) {
        mutableStateOf(TextFieldValue(block.parallelText, selection = TextRange(block.parallelText.length)))
    }

    LaunchedEffect(block.text) {
        if (fieldValue.text != block.text) {
            delay(300)
            if (fieldValue.text != block.text) {
                val cursor = fieldValue.selection.end.coerceIn(0, block.text.length)
                fieldValue = fieldValue.copy(text = block.text, selection = TextRange(cursor))
            }
        }
    }

    LaunchedEffect(block.parallelText) {
        if (parallelFieldValue.text != block.parallelText) {
            delay(300)
            if (parallelFieldValue.text != block.parallelText) {
                val cursor = parallelFieldValue.selection.end.coerceIn(0, block.parallelText.length)
                parallelFieldValue = parallelFieldValue.copy(text = block.parallelText, selection = TextRange(cursor))
            }
        }
    }

    LaunchedEffect(focusOnAppear) {
        if (focusOnAppear) {
            fieldValue = fieldValue.copy(selection = TextRange(fieldValue.text.length))
            focusRequester.requestFocus()
            onFocusHandled()
        }
    }

    val textStyle = when (block.role) {
        "heading" -> MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = block.textAlign.toComposeTextAlign()
        )
        else -> MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = block.textAlign.toComposeTextAlign()
        )
    }
    val prefix = when (block.role) {
        "bullet" -> "• "
        "numbered" -> "$listNumber. "
        else -> ""
    }
    val isListItem = block.role == "bullet" || block.role == "numbered"

    if (block.role == "columns") {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 520.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ParallelTextField(
                        value = fieldValue,
                        styles = block.styles,
                        embeddedBlocks = block.embeddedBlocks,
                        onValueChange = {
                            val textChanged = it.text != fieldValue.text
                            fieldValue = it
                            onActive("main", it.text, it.selection.start, it.selection.end)
                            if (textChanged) {
                                onTextChanged(it.text)
                            }
                        },
                        onEmbeddedBlockUpdate = { embedded ->
                            onColumnEmbeddedBlockUpdate("main", embedded)
                        },
                        onEmbeddedBlockToggle = { embeddedId ->
                            onColumnEmbeddedBlockToggle("main", embeddedId)
                        },
                        onEmbeddedBlockDelete = { embeddedId ->
                            onColumnEmbeddedBlockDelete("main", embeddedId)
                        },
                        placeholder = "Columna izquierda...",
                        textStyle = textStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ParallelTextField(
                        value = parallelFieldValue,
                        styles = block.parallelStyles,
                        embeddedBlocks = block.parallelEmbeddedBlocks,
                        onValueChange = {
                            val textChanged = it.text != parallelFieldValue.text
                            parallelFieldValue = it
                            onActive("parallel", it.text, it.selection.start, it.selection.end)
                            if (textChanged) {
                                onParallelTextChanged(it.text)
                            }
                        },
                        onEmbeddedBlockUpdate = { embedded ->
                            onColumnEmbeddedBlockUpdate("parallel", embedded)
                        },
                        onEmbeddedBlockToggle = { embeddedId ->
                            onColumnEmbeddedBlockToggle("parallel", embeddedId)
                        },
                        onEmbeddedBlockDelete = { embeddedId ->
                            onColumnEmbeddedBlockDelete("parallel", embeddedId)
                        },
                        placeholder = "Columna derecha...",
                        textStyle = textStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ParallelTextField(
                        value = fieldValue,
                        styles = block.styles,
                        embeddedBlocks = block.embeddedBlocks,
                        onValueChange = {
                            val textChanged = it.text != fieldValue.text
                            fieldValue = it
                            onActive("main", it.text, it.selection.start, it.selection.end)
                            if (textChanged) {
                                onTextChanged(it.text)
                            }
                        },
                        onEmbeddedBlockUpdate = { embedded ->
                            onColumnEmbeddedBlockUpdate("main", embedded)
                        },
                        onEmbeddedBlockToggle = { embeddedId ->
                            onColumnEmbeddedBlockToggle("main", embeddedId)
                        },
                        onEmbeddedBlockDelete = { embeddedId ->
                            onColumnEmbeddedBlockDelete("main", embeddedId)
                        },
                        placeholder = "Columna izquierda...",
                        textStyle = textStyle,
                        modifier = Modifier.weight(1f)
                    )
                    ParallelTextField(
                        value = parallelFieldValue,
                        styles = block.parallelStyles,
                        embeddedBlocks = block.parallelEmbeddedBlocks,
                        onValueChange = {
                            val textChanged = it.text != parallelFieldValue.text
                            parallelFieldValue = it
                            onActive("parallel", it.text, it.selection.start, it.selection.end)
                            if (textChanged) {
                                onParallelTextChanged(it.text)
                            }
                        },
                        onEmbeddedBlockUpdate = { embedded ->
                            onColumnEmbeddedBlockUpdate("parallel", embedded)
                        },
                        onEmbeddedBlockToggle = { embeddedId ->
                            onColumnEmbeddedBlockToggle("parallel", embeddedId)
                        },
                        onEmbeddedBlockDelete = { embeddedId ->
                            onColumnEmbeddedBlockDelete("parallel", embeddedId)
                        },
                        placeholder = "Columna derecha...",
                        textStyle = textStyle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isListItem) 30.dp else 44.dp)
            .padding(vertical = if (isListItem) 0.dp else 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                modifier = Modifier.padding(top = if (isListItem) 5.dp else 10.dp, end = 4.dp),
                style = textStyle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )
        }
        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                if ((block.role == "bullet" || block.role == "numbered" || block.role == "heading") && newValue.text.contains('\n')) {
                    val newlineIndex = (newValue.selection.start - 1)
                        .takeIf { it >= 0 && it < newValue.text.length && newValue.text[it] == '\n' }
                        ?: newValue.text.indexOf('\n')
                    val before = newValue.text.substring(0, newlineIndex).trimEnd()
                    val after = newValue.text.substring(newlineIndex + 1).trimStart()
                    if (fieldValue.text.isBlank()) {
                        fieldValue = TextFieldValue("", selection = TextRange.Zero)
                        onTextChanged("")
                        onRoleChanged("paragraph")
                    } else {
                        fieldValue = TextFieldValue(before, selection = TextRange(before.length))
                        onActive("main", before, before.length, before.length)
                        val nextRole = if (block.role == "heading") "paragraph" else block.role
                        onSplitText(before, after, nextRole)
                    }
                    return@BasicTextField
                }
                val textChanged = newValue.text != fieldValue.text
                fieldValue = newValue
                onActive("main", newValue.text, newValue.selection.start, newValue.selection.end)
                if (textChanged) {
                    onTextChanged(newValue.text)
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .padding(vertical = if (isListItem) 4.dp else 8.dp),
            textStyle = textStyle,
            visualTransformation = StyleRangeVisualTransformation(block.styles),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (fieldValue.text.isBlank()) {
                        Text(
                            text = "Comienza a escribir tu ensenanza aqui...",
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

private fun String.toComposeTextAlign(): TextAlign {
    return when (this) {
        "center" -> TextAlign.Center
        "end" -> TextAlign.End
        else -> TextAlign.Start
    }
}

private val columnBulletContinuationRegex = Regex("^(\\s*(?:-|\\u2022|\\u2023|\\u25E6)\\s+)")
private val columnNumberContinuationRegex = Regex("^(\\s*)(\\d+)\\.\\s+")
private val orderedListPrefixRegex = Regex("^\\d+\\.\\s*")

@Composable
private fun ParallelTextField(
    value: TextFieldValue,
    styles: List<TextStyleRange>,
    embeddedBlocks: List<ColumnEmbeddedBlock>,
    onValueChange: (TextFieldValue) -> Unit,
    onEmbeddedBlockUpdate: (ColumnEmbeddedBlock) -> Unit,
    onEmbeddedBlockToggle: (String) -> Unit,
    onEmbeddedBlockDelete: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val flowSegments = remember(value.text, embeddedBlocks) {
            StudyDocumentEngine.buildColumnFlow(value.text, embeddedBlocks)
        }
        flowSegments.forEachIndexed { index, segment ->
            ColumnFlowTextField(
                value = value,
                segment = segment,
                styles = styles,
                onValueChange = onValueChange,
                placeholder = placeholder,
                showPlaceholder = value.text.isBlank() && embeddedBlocks.isEmpty() && index == 0,
                textStyle = textStyle,
                minHeight = if (flowSegments.size == 1) 88.dp else 44.dp
            )
            segment.blocksAfter.forEach { block ->
                ColumnEmbeddedBlockCard(
                    block = block,
                    onUpdate = onEmbeddedBlockUpdate,
                    onToggle = { onEmbeddedBlockToggle(block.blockId) },
                    onDelete = { onEmbeddedBlockDelete(block.blockId) }
                )
            }
        }
    }
}

@Composable
private fun ColumnFlowTextField(
    value: TextFieldValue,
    segment: ColumnFlowSegment,
    styles: List<TextStyleRange>,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    showPlaceholder: Boolean,
    textStyle: TextStyle,
    minHeight: Dp
) {
    BasicTextField(
        value = value.toSegmentTextFieldValue(segment),
        onValueChange = { newSegmentValue ->
            val start = segment.start.coerceIn(0, value.text.length)
            val end = segment.end.coerceIn(start, value.text.length)
            val updatedText = value.text.replaceRange(start, end, newSegmentValue.text)
            val nextValue = value.copy(
                text = updatedText,
                selection = TextRange(
                    start + newSegmentValue.selection.start,
                    start + newSegmentValue.selection.end
                )
            )
            onValueChange(applyListContinuation(value, nextValue))
        },
        modifier = Modifier.heightIn(min = minHeight),
        textStyle = textStyle,
        visualTransformation = StyleRangeVisualTransformation(styles.forSegment(segment.start, segment.end)),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.small
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    if (showPlaceholder) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

private fun TextFieldValue.toSegmentTextFieldValue(segment: ColumnFlowSegment): TextFieldValue {
    val start = segment.start.coerceIn(0, text.length)
    val end = segment.end.coerceIn(start, text.length)
    val segmentText = text.substring(start, end)
    fun localOffset(offset: Int): Int {
        return if (offset in start..end) {
            offset - start
        } else {
            segmentText.length
        }
    }
    return TextFieldValue(
        text = segmentText,
        selection = TextRange(
            localOffset(selection.start),
            localOffset(selection.end)
        )
    )
}

private fun List<TextStyleRange>.forSegment(start: Int, end: Int): List<TextStyleRange> {
    if (start >= end) return emptyList()
    return mapNotNull { style ->
        val rangeStart = style.start.coerceAtLeast(start)
        val rangeEnd = style.end.coerceAtMost(end)
        if (rangeStart >= rangeEnd) {
            null
        } else {
            style.copy(start = rangeStart - start, end = rangeEnd - start)
        }
    }
}

private fun applyListContinuation(previous: TextFieldValue, next: TextFieldValue): TextFieldValue {
    if (next.text.length != previous.text.length + 1) return next
    val insertedIndex = (next.selection.start - 1).coerceAtLeast(0)
    if (insertedIndex !in next.text.indices || next.text[insertedIndex] != '\n') return next
    val previousLineStart = next.text.lastIndexOf('\n', (insertedIndex - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    val previousLine = next.text.substring(previousLineStart, insertedIndex)
    val bulletPrefix = columnBulletContinuationRegex.find(previousLine)?.groupValues?.get(1)
    val numberedPrefix = columnNumberContinuationRegex.find(previousLine)
    val continuation = when {
        bulletPrefix != null -> bulletPrefix
        numberedPrefix != null -> {
            val indent = numberedPrefix.groupValues[1]
            val nextNumber = numberedPrefix.groupValues[2].toIntOrNull()?.plus(1) ?: 1
            "$indent$nextNumber. "
        }
        else -> return next
    }
    val updated = next.text.replaceRange(next.selection.start, next.selection.start, continuation)
    val cursor = next.selection.start + continuation.length
    return next.copy(text = updated, selection = TextRange(cursor))
}

@Composable
private fun ColumnEmbeddedBlockCard(
    block: ColumnEmbeddedBlock,
    onUpdate: (ColumnEmbeddedBlock) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = when (block.type) {
        "reflection" -> Color(0xFF7C3AED)
        "quote" -> Color(0xFFB45309)
        else -> Color(0xFF0F766E)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.title.ifBlank {
                        when (block.type) {
                            "reflection" -> "Reflexion"
                            "quote" -> "Cita"
                            else -> "Nota"
                        }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Row {
                    IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (block.collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (block.collapsed) "Mostrar" else "Ocultar",
                            tint = accent
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = accent)
                    }
                }
            }
            if (!block.collapsed) {
                if (block.type == "reflection") {
                    IntegratedBlockTextField(
                        value = block.title,
                        onValueChange = { onUpdate(block.copy(title = it)) },
                        placeholder = "Tema de la reflexion..."
                    )
                }
                IntegratedBlockTextField(
                    value = block.text,
                    onValueChange = { onUpdate(block.copy(text = it)) },
                    placeholder = when (block.type) {
                        "quote" -> "Texto de la cita..."
                        "reflection" -> "Desarrolla la reflexion..."
                        else -> "Escribe una nota..."
                    }
                )
            }
        }
    }
}

private fun TextFieldValue.selectedText(): String {
    val range = selection
    return if (range.start != range.end) {
        text.substring(range.min, range.max.coerceAtMost(text.length))
    } else {
        ""
    }
}

private class StyleRangeVisualTransformation(
    private val styles: List<TextStyleRange>
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        styles.forEach { style ->
            val start = style.start.coerceIn(0, text.text.length)
            val end = style.end.coerceIn(0, text.text.length)
            if (start < end) {
                builder.addStyle(
                    SpanStyle(
                        color = style.color?.toStudyColorOrUnspecified() ?: Color.Unspecified,
                        background = style.background?.toStudyColorOrUnspecified() ?: Color.Unspecified,
                        fontWeight = if (style.bold) FontWeight.Bold else null,
                        fontStyle = if (style.italic) FontStyle.Italic else null,
                        textDecoration = if (style.underline) TextDecoration.Underline else null,
                        fontSize = style.fontSizeSp?.sp ?: TextUnit.Unspecified
                    ),
                    start,
                    end
                )
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private fun String.toPlainEditorText(): String {
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

@Composable
private fun StudyInteractiveBlockCard(
    block: StudyBlockNode,
    availableVersions: List<BibleVersionOption>,
    onUpdate: (StudyBlockNode) -> Unit,
    onChangeQuotedVerseVersion: (String, String) -> Unit,
    onCompareQuotedVerseVersion: (String, String) -> Unit,
    onToggleCollapsed: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    when (block) {
        is StudyBlockNode.Note -> InteractiveBlockShell(
            title = "Nota",
            accent = Color(0xFF0F766E),
            container = Color(0xFFE2F8EF),
            collapsed = block.collapsed,
            onToggle = { onToggleCollapsed(block.blockId) },
            onDelete = { onDelete(block.blockId) }
        ) {
            IntegratedBlockTextField(
                value = block.text,
                onValueChange = { onUpdate(block.copy(text = it)) },
                placeholder = "Escribe una nota..."
            )
        }

        is StudyBlockNode.Reflection -> InteractiveBlockShell(
            title = "Reflexion",
            accent = Color(0xFF7C3AED),
            container = Color(0xFFF0E7FF),
            collapsed = block.collapsed,
            onToggle = { onToggleCollapsed(block.blockId) },
            onDelete = { onDelete(block.blockId) }
        ) {
            IntegratedBlockTextField(
                value = block.topic,
                onValueChange = { onUpdate(block.copy(topic = it)) },
                placeholder = "Texto, palabra o frase vinculada..."
            )
            IntegratedBlockTextField(
                value = block.text,
                onValueChange = { onUpdate(block.copy(text = it)) },
                placeholder = "Desarrolla la reflexion..."
            )
        }

        is StudyBlockNode.QuotedVerse -> InteractiveBlockShell(
            title = "Citar",
            accent = Color(0xFFB45309),
            container = Color(0xFFFFF1D6),
            collapsed = block.collapsed,
            onToggle = { onToggleCollapsed(block.blockId) },
            onDelete = { onDelete(block.blockId) }
        ) {
            var showCompareTools by remember(block.blockId) { mutableStateOf(block.compareText.isNotBlank()) }
            QuotedVerseReferenceHeader(
                block = block,
                availableVersions = availableVersions,
                showCompareTools = showCompareTools,
                onVersionSelected = { version ->
                    onChangeQuotedVerseVersion(block.blockId, version)
                },
                onToggleCompare = { showCompareTools = !showCompareTools },
                onCompareVersionSelected = { version ->
                    onCompareQuotedVerseVersion(block.blockId, version)
                }
            )
            if (showCompareTools) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 520.dp
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            QuotedVerseText(text = block.primaryText)
                            if (block.compareText.isNotBlank()) {
                                QuotedVerseText(text = block.compareText)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            QuotedVerseText(
                                text = block.primaryText,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier.weight(1f),
                            ) {
                                if (block.compareText.isNotBlank()) {
                                    QuotedVerseText(text = block.compareText)
                                }
                            }
                        }
                    }
                }
            } else {
                QuotedVerseText(text = block.primaryText)
            }
        }
        is StudyBlockNode.Question -> InteractiveBlockShell(
            title = "Pregunta",
            accent = Color(0xFF2563EB),
            container = Color(0xFFE2EDFF),
            collapsed = block.collapsed,
            onToggle = { onToggleCollapsed(block.blockId) },
            onDelete = { onDelete(block.blockId) }
        ) {
            IntegratedBlockTextField(
                value = block.question,
                onValueChange = { onUpdate(block.copy(question = it)) },
                placeholder = "Escribe la pregunta..."
            )
            IntegratedBlockTextField(
                value = block.answer,
                onValueChange = { onUpdate(block.copy(answer = it)) },
                placeholder = "Respuesta esperada..."
            )
        }

        is StudyBlockNode.TwoColumn -> InteractiveBlockShell(
            title = "Dos columnas",
            accent = Color(0xFF475569),
            container = Color(0xFFF1F5F9),
            collapsed = block.collapsed,
            onToggle = { onToggleCollapsed(block.blockId) },
            onDelete = { onDelete(block.blockId) }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 520.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TwoColumnEditorSide(
                            title = block.leftTitle,
                            text = block.leftText,
                            titleLabel = "Titulo columna 1",
                            textLabel = "Texto columna 1",
                            onTitleChange = { onUpdate(block.copy(leftTitle = it)) },
                            onTextChange = { onUpdate(block.copy(leftText = it)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TwoColumnEditorSide(
                            title = block.rightTitle,
                            text = block.rightText,
                            titleLabel = "Titulo columna 2",
                            textLabel = "Texto columna 2",
                            onTitleChange = { onUpdate(block.copy(rightTitle = it)) },
                            onTextChange = { onUpdate(block.copy(rightText = it)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TwoColumnEditorSide(
                            title = block.leftTitle,
                            text = block.leftText,
                            titleLabel = "Titulo columna 1",
                            textLabel = "Texto columna 1",
                            onTitleChange = { onUpdate(block.copy(leftTitle = it)) },
                            onTextChange = { onUpdate(block.copy(leftText = it)) },
                            modifier = Modifier.weight(1f)
                        )
                        TwoColumnEditorSide(
                            title = block.rightTitle,
                            text = block.rightText,
                            titleLabel = "Titulo columna 2",
                            textLabel = "Texto columna 2",
                            onTitleChange = { onUpdate(block.copy(rightTitle = it)) },
                            onTextChange = { onUpdate(block.copy(rightText = it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        else -> Unit
    }
}

@Composable
private fun TwoColumnEditorSide(
    title: String,
    text: String,
    titleLabel: String,
    textLabel: String,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IntegratedBlockTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = titleLabel
        )
        IntegratedBlockTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = textLabel
        )
    }
}

@Composable
private fun QuotedVerseReferenceHeader(
    block: StudyBlockNode.QuotedVerse,
    availableVersions: List<BibleVersionOption>,
    showCompareTools: Boolean,
    onVersionSelected: (String) -> Unit,
    onToggleCompare: () -> Unit,
    onCompareVersionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = block.reference.ifBlank { "Referencia citada" },
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFB45309),
            fontWeight = FontWeight.Bold
        )
        QuotedVerseVersionMenuButton(
            label = block.primaryVersion.uppercase(),
            versions = availableVersions,
            selectedVersion = block.primaryVersion,
            onVersionSelected = onVersionSelected
        )
        TextButton(
            onClick = onToggleCompare,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Text(
                text = if (showCompareTools) "ocultar" else "comparar",
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (showCompareTools) {
            QuotedVerseVersionMenuButton(
                label = if (block.compareVersion.isBlank()) "version" else block.compareVersion.uppercase(),
                versions = availableVersions.filter { it.key != block.primaryVersion },
                selectedVersion = block.compareVersion,
                onVersionSelected = onCompareVersionSelected
            )
        }
    }
}

@Composable
private fun QuotedVerseVersionMenuButton(
    label: String,
    versions: List<BibleVersionOption>,
    selectedVersion: String,
    onVersionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            versions.forEach { version ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = version.label,
                            fontWeight = if (version.key == selectedVersion) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onVersionSelected(version.key)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuotedVerseText(
    text: String,
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
    val verseNumberColor = verseNumberAccentColor()
    Text(
        text = text.ifBlank { "Texto de la cita no disponible." }.toVerseNumberStyledText(verseNumberColor),
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            lineHeight = 25.sp
        ),
        color = contentColor
    )
}

@Composable
private fun verseNumberAccentColor(): Color {
    return if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        BiblionBluePrimary
    } else {
        BiblionGoldPrimary
    }
}

private fun String.toVerseNumberStyledText(verseNumberColor: Color): AnnotatedString {
    val builder = AnnotatedString.Builder(this)
    Regex("""(^|\s)(\d{1,3})(?=\s)""").findAll(this).forEach { match ->
        val numberStart = match.range.first + match.groupValues[1].length
        val numberEnd = numberStart + match.groupValues[2].length
        builder.addStyle(
            SpanStyle(
                color = verseNumberColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            numberStart,
            numberEnd
        )
    }
    return builder.toAnnotatedString()
}

@Composable
private fun IntegratedBlockTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
        ),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun InteractiveBlockShell(
    title: String,
    accent: Color,
    container: Color,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(accent, MaterialTheme.shapes.small)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 2.dp, top = 1.dp, bottom = 3.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = accent,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            ),
            modifier = Modifier.weight(1f)
        )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (collapsed) "Expandir bloque" else "Contraer bloque",
                        tint = accent
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar bloque",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!collapsed) {
                content()
            }
            }
    }
}

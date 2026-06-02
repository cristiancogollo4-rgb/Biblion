package com.cristiancogollo.biblion

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyEditorScreen(
    viewModel: StudyViewModel,
    onClose: () -> Unit,
    onFocusModeChanged: (Boolean) -> Unit
) {
    val ui by viewModel.state.collectAsState()
    val context = LocalContext.current
    var availableBibleVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    var activeTextBlockId by remember { mutableStateOf<String?>(null) }
    var activeTextRole by remember { mutableStateOf("paragraph") }
    var activeTextSource by remember { mutableStateOf("main") }
    var activeTextContent by remember { mutableStateOf("") }
    var activeSelectionStart by remember { mutableStateOf(0) }
    var activeSelectionEnd by remember { mutableStateOf(0) }
    var activeSelectedText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOffset by remember { mutableStateOf(IntOffset(0, -120)) }
    var editorContainerWidthPx by remember { mutableStateOf(0) }
    var showFloatingMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }
    var saveTagsInput by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var pendingFocusBlockId by remember { mutableStateOf<String?>(null) }

    fun openMetadataDialog() {
        saveTitle = ui.title
        saveTagsInput = ui.tags.joinToString(", ")
        saveError = null
        showSaveDialog = true
    }

    fun validateNonEmptyContent(): Boolean {
        val plainContent = ui.richHtml
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .trim()
        return plainContent.isNotBlank()
    }

    fun handleSave() {
        if (ui.title.isBlank() && ui.tags.isEmpty()) {
            openMetadataDialog()
            return
        }
        if (!validateNonEmptyContent()) {
            scope.launch { snackbarHostState.showSnackbar("No puedes guardar una enseñanza vacía.") }
            return
        }
        viewModel.process(StudyIntent.SaveStudy)
    }

    fun selectedText(): String = activeSelectedText.trim()

    fun updateActiveParagraphRole(role: String) {
        activeTextBlockId?.let { blockId ->
            activeTextRole = role
            viewModel.process(StudyIntent.UpdateParagraphRole(blockId, role))
        }
    }

    fun insertNoteBlock() = viewModel.process(StudyIntent.AddNoteBlock(activeTextBlockId))

    fun insertReflectionBlock() = viewModel.process(StudyIntent.AddReflectionBlock(selectedText(), activeTextBlockId))

    fun toggleParallelText() {
        updateActiveParagraphRole(if (activeTextRole == "columns") "paragraph" else "columns")
    }

    fun updateActiveSelection(
        source: String,
        text: String,
        selectionStart: Int,
        selectionEnd: Int
    ) {
        activeTextSource = source
        activeTextContent = text
        activeSelectionStart = selectionStart.coerceIn(0, text.length)
        activeSelectionEnd = selectionEnd.coerceIn(0, text.length)
        activeSelectedText = if (activeSelectionStart != activeSelectionEnd) {
            text.substring(activeSelectionStart.coerceAtMost(activeSelectionEnd), activeSelectionStart.coerceAtLeast(activeSelectionEnd))
        } else {
            ""
        }
    }

    fun transformSelectedText(transform: (String) -> String) {
        val blockId = activeTextBlockId ?: return
        val start = activeSelectionStart.coerceAtMost(activeSelectionEnd)
        val end = activeSelectionStart.coerceAtLeast(activeSelectionEnd)
        if (start == end || activeTextContent.isEmpty()) return

        val replacement = transform(activeTextContent.substring(start, end))
        val updated = activeTextContent.replaceRange(start, end, replacement)
        if (activeTextSource == "parallel") {
            viewModel.process(StudyIntent.UpdateParagraphParallelText(blockId, updated))
        } else {
            viewModel.process(StudyIntent.UpdateParagraphBlock(blockId, updated))
        }
        activeTextContent = updated
        activeSelectedText = replacement
        activeSelectionEnd = start + replacement.length
    }

    fun applySelectedStyle(
        color: Color? = null,
        background: Color? = null,
        bold: Boolean = false,
        italic: Boolean = false,
        underline: Boolean = false,
        fontSizeSp: Float? = null
    ) {
        val blockId = activeTextBlockId ?: return
        val start = activeSelectionStart.coerceAtMost(activeSelectionEnd)
        val end = activeSelectionStart.coerceAtLeast(activeSelectionEnd)
        if (start == end) return
        viewModel.process(
            StudyIntent.ApplyParagraphTextStyle(
                blockId = blockId,
                source = activeTextSource,
                start = start,
                end = end,
                color = color?.value?.toLong(),
                background = background?.value?.toLong(),
                bold = bold,
                italic = italic,
                underline = underline,
                fontSizeSp = fontSizeSp
            )
        )
    }

    fun clearSelectedStyle() {
        val blockId = activeTextBlockId ?: return
        val start = activeSelectionStart.coerceAtMost(activeSelectionEnd)
        val end = activeSelectionStart.coerceAtLeast(activeSelectionEnd)
        if (start == end) return
        viewModel.process(StudyIntent.ClearParagraphTextStyle(blockId, activeTextSource, start, end))
    }

    fun clearSelectedTextColor() {
        val blockId = activeTextBlockId ?: return
        val start = activeSelectionStart.coerceAtMost(activeSelectionEnd)
        val end = activeSelectionStart.coerceAtLeast(activeSelectionEnd)
        if (start == end) return
        viewModel.process(
            StudyIntent.ClearParagraphTextStyle(
                blockId = blockId,
                source = activeTextSource,
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
        val blockId = activeTextBlockId ?: return
        val start = activeSelectionStart.coerceAtMost(activeSelectionEnd)
        val end = activeSelectionStart.coerceAtLeast(activeSelectionEnd)
        if (start == end) return
        viewModel.process(
            StudyIntent.ClearParagraphTextStyle(
                blockId = blockId,
                source = activeTextSource,
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

    val hasSelection = activeSelectedText.isNotBlank()
    val hasTextTarget = activeTextBlockId != null
    val showMenu = hasSelection || showFloatingMenu || ui.pendingCitations.isNotEmpty()
    val starterTextBlock = remember { StudyBlockNode.Paragraph(text = "") }
    val documentBlocks = if (ui.blocks.isNotEmpty()) {
        ui.blocks
    } else {
        listOf(starterTextBlock)
    }

    LaunchedEffect(hasSelection) {
        viewModel.process(StudyIntent.SetSelectionActive(hasSelection))
        if (hasSelection) {
            menuOffset = IntOffset(0, -220)
        }
    }

    LaunchedEffect(ui.pendingCitations.size) {
        if (ui.pendingCitations.isNotEmpty()) {
            showFloatingMenu = true
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
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val firstUp = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        if (firstUp != null) {
                            val secondDown = withTimeoutOrNull(300L) {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                            }
                            val secondUp = secondDown?.let {
                                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            }
                            if (secondUp != null) {
                                menuOffset = IntOffset(0, -220)
                                showFloatingMenu = true
                            }
                        }
                    }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                var numberedIndex = 0
                documentBlocks.forEach { block ->
                    numberedIndex = if (block is StudyBlockNode.Paragraph && block.role == "numbered") {
                        numberedIndex + 1
                    } else {
                        0
                    }
                    when (block) {
                        is StudyBlockNode.Paragraph -> {
                            StudyParagraphBlockEditor(
                                block = block,
                                listNumber = numberedIndex.coerceAtLeast(1),
                                focusOnAppear = pendingFocusBlockId == block.blockId,
                                onFocusHandled = { pendingFocusBlockId = null },
                                onActive = { source, text, start, end ->
                                    activeTextBlockId = block.blockId
                                    activeTextRole = block.role
                                    updateActiveSelection(source, text, start, end)
                                },
                                onTextChanged = { text ->
                                    viewModel.process(StudyIntent.UpdateParagraphBlock(block.blockId, text))
                                },
                                onParallelTextChanged = { text ->
                                    viewModel.process(StudyIntent.UpdateParagraphParallelText(block.blockId, text))
                                },
                                onRoleChanged = { role ->
                                    activeTextRole = role
                                    viewModel.process(StudyIntent.UpdateParagraphRole(block.blockId, role))
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
                                    activeTextBlockId = block.blockId
                                    activeTextRole = "paragraph"
                                    updateActiveSelection(source, text, start, end)
                                },
                                onTextChanged = { text ->
                                    viewModel.process(StudyIntent.UpdateParagraphBlock(block.blockId, text))
                                },
                                onParallelTextChanged = { text ->
                                    viewModel.process(StudyIntent.UpdateParagraphParallelText(block.blockId, text))
                                },
                                onRoleChanged = { role ->
                                    activeTextRole = role
                                    viewModel.process(StudyIntent.UpdateParagraphRole(block.blockId, role))
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
            }

            StudyEditorFloatingMenu(
                isVisible = showMenu,
                anchorOffset = menuOffset,
                containerWidthPx = editorContainerWidthPx,
                hasSelection = hasSelection || showFloatingMenu,
                isParallelTextMode = activeTextRole == "columns",
                isBulletMode = activeTextRole == "bullet",
                isNumberedMode = activeTextRole == "numbered",
                pendingCitations = ui.pendingCitations.size,
                onDismiss = {
                    showFloatingMenu = false
                    viewModel.process(StudyIntent.SetContextMenuVisible(false))
                },
                onHeadlineUp = {
                    updateActiveParagraphRole("heading")
                },
                onHeadlineDown = {
                    updateActiveParagraphRole("paragraph")
                },
                onBold = { applySelectedStyle(bold = true) },
                onItalic = { applySelectedStyle(italic = true) },
                onUnderline = { applySelectedStyle(underline = true) },
                onUppercase = { transformSelectedText { it.uppercase() } },
                onLowercase = { transformSelectedText { it.lowercase() } },
                onTextColor = { color -> applySelectedStyle(color = color) },
                onBackgroundColor = { color -> applySelectedStyle(background = color) },
                onClearTextColor = { clearSelectionTextColor() },
                onClearBackground = { clearSelectionBackground() },
                onClearFormatting = { clearSelectionFormatting() },
                onIncreaseSize = {
                    viewModel.process(StudyIntent.IncreaseSelectionFont)
                    applySelectedStyle(fontSizeSp = (ui.selectionFontSizeSp + 2f).coerceAtMost(46f))
                },
                onDecreaseSize = {
                    viewModel.process(StudyIntent.DecreaseSelectionFont)
                    applySelectedStyle(fontSizeSp = (ui.selectionFontSizeSp - 2f).coerceAtLeast(12f))
                },
                onBulletList = {
                    updateActiveParagraphRole(if (activeTextRole == "bullet") "paragraph" else "bullet")
                },
                onOrderedList = {
                    updateActiveParagraphRole(if (activeTextRole == "numbered") "paragraph" else "numbered")
                },
                onInsertPendingCitations = {
                    val pending = viewModel.consumePendingCitations()
                    pending.forEach { request ->
                        viewModel.process(StudyIntent.AddQuotedVerseBlock(activeTextBlockId, request))
                    }
                },
                onInsertNote = { insertNoteBlock() },
                onInsertReflection = { insertReflectionBlock() },
                onInsertTwoColumn = { toggleParallelText() }
            )
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Guardar enseñanza") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = saveTitle,
                        onValueChange = {
                            saveTitle = it
                            saveError = null
                        },
                        singleLine = true,
                        label = { Text("Título") },
                        placeholder = { Text("Ej: La fe en tiempos difíciles") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BiblionNavy,
                            unfocusedBorderColor = BiblionGoldPrimary,
                            focusedLabelColor = BiblionNavy,
                            cursorColor = BiblionNavy
                        )
                    )
                    StudyTagSelector(
                        value = saveTagsInput,
                        onValueChange = {
                            saveTagsInput = it
                            saveError = null
                        }
                    )
                    saveError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleanedTitle = saveTitle.trim()
                    val parsedTags = parseStudyTags(saveTagsInput)
                    val tagError = validateRequiredStudyTags(parsedTags)
                    saveError = when {
                        !validateNonEmptyContent() -> "No puedes guardar una enseñanza vacía."
                        cleanedTitle.isBlank() -> "Debes agregar un titulo para guardar."
                        tagError != null -> tagError
                        else -> null
                    }

                    if (saveError == null) {
                        viewModel.process(StudyIntent.SaveStudyWithMetadata(cleanedTitle, parsedTags))
                        showSaveDialog = false
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = BiblionGoldPrimary)) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = BiblionBluePrimary)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
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
            val cursor = fieldValue.selection.end.coerceIn(0, block.text.length)
            fieldValue = fieldValue.copy(text = block.text, selection = TextRange(cursor))
        }
    }

    LaunchedEffect(block.parallelText) {
        if (parallelFieldValue.text != block.parallelText) {
            val cursor = parallelFieldValue.selection.end.coerceIn(0, block.parallelText.length)
            parallelFieldValue = parallelFieldValue.copy(text = block.parallelText, selection = TextRange(cursor))
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
            color = MaterialTheme.colorScheme.onBackground
        )
        else -> MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground
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
                        onValueChange = {
                            val textChanged = it.text != fieldValue.text
                            fieldValue = it
                            onActive("main", it.text, it.selection.start, it.selection.end)
                            if (textChanged) {
                                onTextChanged(it.text)
                            }
                        },
                        placeholder = "Columna izquierda...",
                        textStyle = textStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ParallelTextField(
                        value = parallelFieldValue,
                        styles = block.parallelStyles,
                        onValueChange = {
                            val textChanged = it.text != parallelFieldValue.text
                            parallelFieldValue = it
                            onActive("parallel", it.text, it.selection.start, it.selection.end)
                            if (textChanged) {
                                onParallelTextChanged(it.text)
                            }
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
                        onValueChange = {
                            val textChanged = it.text != fieldValue.text
                            fieldValue = it
                            onActive("main", it.text, it.selection.start, it.selection.end)
                            if (textChanged) {
                                onTextChanged(it.text)
                            }
                        },
                        placeholder = "Columna izquierda...",
                        textStyle = textStyle,
                        modifier = Modifier.weight(1f)
                    )
                    ParallelTextField(
                        value = parallelFieldValue,
                        styles = block.parallelStyles,
                        onValueChange = {
                            val textChanged = it.text != parallelFieldValue.text
                            parallelFieldValue = it
                            onActive("parallel", it.text, it.selection.start, it.selection.end)
                            if (textChanged) {
                                onParallelTextChanged(it.text)
                            }
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
                if ((block.role == "bullet" || block.role == "numbered") && newValue.text.contains('\n')) {
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
                        onSplitText(before, after, block.role)
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

@Composable
private fun ParallelTextField(
    value: TextFieldValue,
    styles: List<TextStyleRange>,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = 88.dp)
            .padding(vertical = 6.dp),
        textStyle = textStyle,
        visualTransformation = StyleRangeVisualTransformation(styles),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.small
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    if (value.text.isBlank()) {
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
                        color = style.color?.let { Color(it.toULong()) } ?: Color.Unspecified,
                        background = style.background?.let { Color(it.toULong()) } ?: Color.Unspecified,
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
    var labelText by remember(title) { mutableStateOf(title) }

    LaunchedEffect(labelText) {
        if (labelText.isBlank()) {
            onDelete()
        }
    }

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
                BasicTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelLarge.copy(
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
            }

            if (!collapsed) {
                content()
            }
            }
    }
}

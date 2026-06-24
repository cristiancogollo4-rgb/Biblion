package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.domain.InsertBlockCommand
import com.cristiancogollo.biblion.feature.studydocs.domain.MergeBlocksCommand
import com.cristiancogollo.biblion.feature.studydocs.domain.ReplaceBlockCommand
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.engine.CursorNavigator
import com.cristiancogollo.biblion.feature.studydocs.engine.SpanType
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyOp
import com.cristiancogollo.biblion.feature.studydocs.engine.hasSpan
import com.cristiancogollo.biblion.feature.studydocs.engine.toNavigatorKey
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.TextStylePatch
import com.cristiancogollo.biblion.feature.studydocs.model.displayTypeName
import com.cristiancogollo.biblion.feature.studydocs.model.isList
import com.cristiancogollo.biblion.feature.studydocs.model.isTextEditable
import kotlinx.coroutines.launch
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.BulletListBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.CalloutBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.DividerBlockView
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.HeadingBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.NoteBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.NumberedListBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.PageBreakBlockView
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.ParagraphBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.QuoteBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.ReflectionBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.TableBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.VerseBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.outline.OutlinePanel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StudyDocEditorScreen(
    viewModel: StudyDocViewModel,
    onBack: () -> Unit,
    onFocusModeChanged: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showOutline by remember { mutableStateOf(false) }
    var showSlashMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val selectionState = remember { SelectionState() }
    val focusRequesters = remember { androidx.compose.runtime.mutableStateMapOf<BlockId, androidx.compose.ui.focus.FocusRequester>() }
    val zoomState = remember { mutableStateOf(CameraState.Initial) }

    // --- ZOOM + SCROLL: Camera/Viewport/Document architecture ---
    // Camera: zoom + offset. Viewport: pantalla (clipped). Document: PaginatedPaperSheet.
    // screen = world * zoom + offset
    // world  = (screen - offset) / zoom
    val activity = (androidx.compose.ui.platform.LocalContext.current as android.app.Activity)
    val scaleDetector = remember {
        android.view.ScaleGestureDetector(
            activity,
            object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                    zoomState.value = zoomState.value.focalZoom(
                        newZoom = zoomState.value.zoom * detector.scaleFactor,
                        focusX = detector.focusX,
                        focusY = detector.focusY,
                    )
                    return true
                }
                override fun onScaleEnd(detector: android.view.ScaleGestureDetector) {
                    zoomState.value = zoomState.value.snapZoom()
                }
            },
        ).apply { isQuickScaleEnabled = true }
    }
    val windowCallback = remember { activity.window.callback }
    DisposableEffect(Unit) {
        activity.window.callback = object : android.view.Window.Callback by windowCallback {
            override fun dispatchTouchEvent(event: android.view.MotionEvent?): Boolean {
                event?.let { scaleDetector.onTouchEvent(it) }
                return windowCallback.dispatchTouchEvent(event)
            }
        }
        onDispose { activity.window.callback = windowCallback }
    }

    // Tamaño del documento medido para clamp() de la camara
    var docSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    // Auto-foco en el primer bloque cuando se acaba de crear el doc.
    LaunchedEffect(uiState.wasJustCreated) {
        if (!uiState.wasJustCreated) return@LaunchedEffect
        val firstId = uiState.doc.blocks.firstOrNull()?.id ?: return@LaunchedEffect
        kotlinx.coroutines.delay(80)
        focusRequesters[firstId]?.requestFocus()
        viewModel.clearJustCreatedFlag()
    }

    val onBold: () -> Unit = { applyStyleToFocused(viewModel, TextStylePatch(bold = true)) }
    val onItalic: () -> Unit = { applyStyleToFocused(viewModel, TextStylePatch(italic = true)) }
    val onUnderline: () -> Unit = { applyStyleToFocused(viewModel, TextStylePatch(underline = true)) }
    val onStrikethrough: () -> Unit = { applyStyleToFocused(viewModel, TextStylePatch(strikethrough = true)) }
    val onBullet: () -> Unit = { viewModel.toggleListOnTarget(com.cristiancogollo.biblion.feature.studydocs.engine.ListType.Bullet) }
    val onNumbered: () -> Unit = { viewModel.toggleListOnTarget(com.cristiancogollo.biblion.feature.studydocs.engine.ListType.Numbered) }
    val onColorClick: () -> Unit = { showColorPicker = true }
    val onAlignClick: () -> Unit = { viewModel.cycleAlignment() }
    val onTypeSelected: (Int) -> Unit = { index ->
        val sid = uiState.selectedBlockId
        if (sid != null) {
            val targetId = BlockId(sid)
            val target = uiState.doc.blocks.firstOrNull { it.id == targetId }
            if (target != null) {
                val newBlock: StudyBlock? = when (index) {
                    0 -> StudyBlock.Paragraph(id = targetId, text = StyledText.Empty)
                    1 -> StudyBlock.Heading(id = targetId, level = 1, text = StyledText.Empty)
                    2 -> StudyBlock.Heading(id = targetId, level = 2, text = StyledText.Empty)
                    3 -> StudyBlock.Heading(id = targetId, level = 3, text = StyledText.Empty)
                    4 -> StudyBlock.Quote(id = targetId, text = StyledText.Empty)
                    5 -> StudyBlock.BulletList(id = targetId, items = emptyList())
                    else -> null
                }
                if (newBlock != null) {
                    viewModel.executeCommand(ReplaceBlockCommand(targetId, newBlock))
                }
            }
        }
    }

    val currentTfv = viewModel.blockTextStates[viewModel.lastFocusedBlockId.value]
    val isBold = currentTfv?.hasSpan(SpanType.Bold) ?: false
    val isItalic = currentTfv?.hasSpan(SpanType.Italic) ?: false
    val isUnderline = currentTfv?.hasSpan(SpanType.Underline) ?: false
    val isStrikethrough = currentTfv?.hasSpan(SpanType.Strikethrough) ?: false
    val activeBlock = uiState.doc.blocks.firstOrNull { it.id.value == uiState.selectedBlockId }
    val isBulletActive = activeBlock is StudyBlock.BulletList
    val isNumberedActive = activeBlock is StudyBlock.NumberedList

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.doc.title.ifBlank { "Sin titulo" },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (uiState.doc.blocks.isNotEmpty()) {
                            Text(
                                text = "${uiState.doc.blocks.size} bloques",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (onFocusModeChanged != null) {
                        IconButton(onClick = onFocusModeChanged) {
                            Icon(
                                Icons.Filled.AspectRatio,
                                contentDescription = "Focus mode",
                            )
                        }
                    }
                    IconButton(onClick = { showOutline = !showOutline }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.MenuBook, contentDescription = "Outline")
                    }
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Filled.Save, contentDescription = "Guardar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .undoRedoKeyHandler(
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                )
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val selectedId = uiState.selectedBlockId ?: return@onPreviewKeyEvent false
                    val navKey = event.toNavigatorKey() ?: return@onPreviewKeyEvent false
                    val currentValue = viewModel.blockTextStates[BlockId(selectedId)]
                        ?: return@onPreviewKeyEvent false
                    val blocks = uiState.doc.blocks
                    val blockIndex = blocks.indexOfFirst { it.id.value == selectedId }
                    if (blockIndex < 0) return@onPreviewKeyEvent false
                    val block = blocks[blockIndex]
                    val action = CursorNavigator.decide(
                        key = navKey,
                        blockIndex = blockIndex,
                        offset = currentValue.selection.start,
                        textLength = currentValue.text.length,
                        isTextBlock = block.isTextEditable,
                        isListBlock = block.isList,
                        hasNextBlock = blockIndex < blocks.lastIndex,
                        hasPrevBlock = blockIndex > 0,
                        isShiftPressed = event.isShiftPressed,
                    )
                    when (action) {
                        is CursorNavigator.Action.MoveFocusTo -> {
                            val targetId = blocks.getOrNull(action.blockIndex)?.id
                            if (targetId == null) return@onPreviewKeyEvent false
                            focusRequesters[targetId]?.requestFocus()
                            true
                        }
                        is CursorNavigator.Action.InsertNewBlockAfter -> {
                            viewModel.executeCommand(
                                InsertBlockCommand(
                                    atIndex = blockIndex + 1,
                                    block = StudyBlock.Paragraph(),
                                ),
                            )
                            true
                        }
                        is CursorNavigator.Action.AppendListItem -> {
                            viewModel.appendListItem(blockIndex)
                            true
                        }
                        CursorNavigator.Action.Handled -> true
                        CursorNavigator.Action.PassThrough -> false
                    }
                },
        ) {
            if (showOutline) {
                Row(modifier = Modifier.fillMaxSize()) {
                    OutlinePanel(
                        doc = uiState.doc,
                        onHeadingClick = { _, idx ->
                            showOutline = false
                            coroutineScope.launch {
                                listState.animateScrollToItem(idx)
                            }
                        },
                        onClose = { showOutline = false },
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp)) {
                    EditorTopBar(
                        isFormatEnabled = uiState.selectedBlockId != null,
                        onBold = onBold,
                        onItalic = onItalic,
                        onUnderline = onUnderline,
                        onStrikethrough = onStrikethrough,
                        onBullet = onBullet,
                        onNumbered = onNumbered,
                        onAddBlock = { showSlashMenu = true },
                        canUndo = uiState.canUndo,
                        canRedo = uiState.canRedo,
                        undoDescription = uiState.undoDescription,
                        redoDescription = uiState.redoDescription,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        activeBlockTypeName = run {
                            val sid = uiState.selectedBlockId
                            if (sid == null) null
                            else uiState.doc.blocks.firstOrNull { it.id.value == sid }?.displayTypeName?.takeIf { it.isNotEmpty() }
                        },
                        isTypeDropdownEnabled = uiState.selectedBlockId?.let { sid ->
                            uiState.doc.blocks.firstOrNull { it.id.value == sid }?.displayTypeName?.isNotEmpty() == true
                        } ?: false,
                        onTypeSelected = onTypeSelected,
                        isBoldActive = isBold,
                        isItalicActive = isItalic,
                        isUnderlineActive = isUnderline,
                        isStrikethroughActive = isStrikethrough,
                        isBulletActive = isBulletActive,
                        isNumberedActive = isNumberedActive,
                        onColorClick = onColorClick,
                        onAlignClick = onAlignClick,
                        zoomPercent = zoomState.value.displayPercent(),
                        isZoomModified = zoomState.value.zoom != CameraState.Initial.zoom,
                        onResetZoom = { zoomState.value = CameraState.Initial },
                        onZoomIn = { zoomState.value = zoomState.value.stepIn() },
                        onZoomOut = { zoomState.value = zoomState.value.stepOut() },
                    )
                    // Viewport (clipea el contenido que excede la pantalla)
                    // Camera (graphicsLayer con zoom + offset)
                    // Document (PaginatedPaperSheet a tamano nativo)
                    Box(Modifier.fillMaxSize()
                        .clipToBounds()
                        .onGloballyPositioned { coords -> docSize = coords.size }
                    ) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                val cam = zoomState.value
                                scaleX = cam.zoom
                                scaleY = cam.zoom
                                translationX = cam.offsetX
                                translationY = cam.offsetY
                                transformOrigin = TransformOrigin(0f, 0f)
                            },
                        ) {
                            PaginatedPaperSheet(
                                blocks = uiState.doc.blocks,
                            ) { index, block ->
                            BlockWithHandle(
                                        block = block,
                                        blockIndex = index,
                                        isSelected = uiState.selectedBlockId == block.id.value,
                                        blockAlignments = viewModel.blockAlignments,
                                        selectionState = selectionState,
                                        onClick = { viewModel.selectBlock(block.id.value) },
                                        onSelectionChange = { range ->
                                            selectionState.set(block.id.value, range)
                                            viewModel.setActiveRange(block.id, range)
                                            viewModel.selectBlock(block.id.value)
                                        },
                                        onReplace = { newBlock ->
                                            viewModel.applyOp(StudyOp.ReplaceBlock(block.id, newBlock))
                                        },
                                        onInsert = { atIndex, newBlock ->
                                            viewModel.applyOp(StudyOp.InsertBlock(atIndex, newBlock))
                                        },
                                        onDelete = { viewModel.applyOp(StudyOp.DeleteBlock(block.id)) },
                                        onFieldValueChange = { tfv ->
                                            viewModel.blockTextStates[block.id] = tfv
                                        },
                                        onRegisterFocus = { fr -> focusRequesters[block.id] = fr },
                                    )
                            }
                        }
                        }
                }
            }
        }

        if (showSlashMenu) {
            SlashCommandMenu(
                onSelect = { factory ->
                    val newBlock = factory()
                    val atIndex = uiState.doc.blocks.size
                    viewModel.applyOp(StudyOp.InsertBlock(atIndex, newBlock))
                    viewModel.selectBlock(newBlock.id.value)
                    showSlashMenu = false
                },
                onDismiss = { showSlashMenu = false },
            )
        }

        if (showColorPicker) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showColorPicker = false },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showColorPicker = false }) {
                        Text("Cerrar")
                    }
                },
                title = { Text("Color de texto") },
                text = {
                    val palette = listOf(
                        0xFF000000L, 0xFF424242L, 0xFF9E9E9EL, 0xFFE53935L,
                        0xFFFB8C00L, 0xFFFDD835L, 0xFF43A047L, 0xFF1E88E5L,
                        0xFF3949ABL, 0xFF8E24AAL, 0xFFD81B60L, 0xFFFFFFFFL,
                    )
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(palette.size) { idx ->
                            val c = palette[idx].toInt()
                            Surface(
                                onClick = {
                                    applyStyleToFocused(viewModel, TextStylePatch(color = c))
                                    showColorPicker = false
                                },
                                color = Color(c),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).size(40.dp),
                            ) { }
                        }
                    }
                },
            )
        }

        // El boton de alineacion en la toolbar cicla directamente la alineacion
        // del bloque activo. No se necesita dialog.

        if (showSaveDialog) {
            SaveTeachingDialogHost(
                viewModel = viewModel,
                currentTitle = uiState.doc.title,
                currentTags = uiState.doc.metadata.tags,
                onDismiss = { showSaveDialog = false },
            )
        }
    }
}

@Composable
private fun SaveTeachingDialogHost(
    viewModel: StudyDocViewModel,
    currentTitle: String,
    currentTags: List<String>,
    onDismiss: () -> Unit,
) {
    var titleInput by remember { mutableStateOf(currentTitle) }
    var tagsInput by remember { mutableStateOf(currentTags.joinToString(", ")) }
    var error by remember { mutableStateOf<String?>(null) }

    SaveTeachingDialog(
        title = titleInput,
        onTitleChange = {
            titleInput = it
            error = null
        },
        tagsInput = tagsInput,
        onTagsInputChange = {
            tagsInput = it
            error = null
        },
        error = error,
        onDismiss = onDismiss,
        onSave = {
            val cleanTitle = titleInput.trim()
            if (cleanTitle.isBlank()) {
                error = "El titulo es obligatorio."
                return@SaveTeachingDialog
            }
            val cleanTags = parseStudyTags(tagsInput)
            val tagError = validateRequiredStudyTags(cleanTags)
            if (tagError != null) {
                error = tagError
                return@SaveTeachingDialog
            }
            viewModel.saveNow(title = cleanTitle, tags = cleanTags)
            onDismiss()
        },
    )
}

private fun applyStyleToFocused(
    viewModel: StudyDocViewModel,
    patch: TextStylePatch,
) {
    viewModel.applyStyleAtSelection(patch)
}

@Composable
private fun BlockWithHandle(
    block: StudyBlock,
    blockIndex: Int,
    isSelected: Boolean,
    selectionState: SelectionState,
    blockAlignments: androidx.compose.runtime.snapshots.SnapshotStateMap<com.cristiancogollo.biblion.feature.studydocs.model.BlockId, androidx.compose.ui.text.style.TextAlign>,
    onClick: () -> Unit,
    onSelectionChange: (IntRange?) -> Unit,
    onReplace: (StudyBlock) -> Unit,
    onInsert: (Int, StudyBlock) -> Unit,
    onDelete: () -> Unit,
    onFieldValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onRegisterFocus: (androidx.compose.ui.focus.FocusRequester) -> Unit,
) {
    val highlight = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
    val localFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(block.id) { onRegisterFocus(localFocusRequester) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(highlight)
            .pointerInput(block.id) {
                detectTapGestures(onTap = { onClick() })
            },
    ) {
        Box(
            modifier = Modifier.size(28.dp).padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = "Arrastrar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            val blockAlign = blockAlignments[block.id]
            when (block) {
                is StudyBlock.Paragraph -> ParagraphBlockEditor(
                    block = block, isSelected = isSelected, onClick = onClick,
                    onSelectionChange = onSelectionChange, onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                    onShortcutDetected = { newBlock -> onReplace(newBlock) },
                    onFieldValueChange = onFieldValueChange,
                    textAlign = blockAlign,
                )
                is StudyBlock.Heading -> HeadingBlockEditor(
                    block = block, isSelected = isSelected, onClick = onClick,
                    onSelectionChange = onSelectionChange, onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                    textAlign = blockAlign,
                )
                is StudyBlock.Quote -> QuoteBlockEditor(
                    block = block, isSelected = isSelected, onClick = onClick,
                    onSelectionChange = onSelectionChange, onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                    textAlign = blockAlign,
                )
                is StudyBlock.BulletList -> BulletListBlockEditor(
                    block = block, isSelected = isSelected, onClick = onClick,
                    onSelectionChange = onSelectionChange,
                    onItemsChange = { onReplace(block.copy(items = it)) },
                    onAppendItem = { onReplace(block.copy(items = block.items + StyledText.Empty)) },
                    onRemoveItem = { idx ->
                        if (block.items.size > 1) {
                            onReplace(block.copy(items = block.items.toMutableList().apply { removeAt(idx) }))
                        }
                    },
                    onItemChange = { idx, newItem ->
                        onReplace(block.copy(items = block.items.toMutableList().apply { this[idx] = newItem }))
                    },
                )
                is StudyBlock.NumberedList -> NumberedListBlockEditor(
                    block = block, isSelected = isSelected, onClick = onClick,
                    onSelectionChange = onSelectionChange,
                    onAppendItem = { onReplace(block.copy(items = block.items + StyledText.Empty)) },
                    onRemoveItem = { idx ->
                        if (block.items.size > 1) {
                            onReplace(block.copy(items = block.items.toMutableList().apply { removeAt(idx) }))
                        }
                    },
                    onItemChange = { idx, newItem ->
                        onReplace(block.copy(items = block.items.toMutableList().apply { this[idx] = newItem }))
                    },
                )
                is StudyBlock.Note -> NoteBlockEditor(
                    block = block, isSelected = isSelected, onSelectionChange = onSelectionChange,
                    onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                )
                is StudyBlock.Reflection -> ReflectionBlockEditor(
                    block = block, isSelected = isSelected, onSelectionChange = onSelectionChange,
                    onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                )
                is StudyBlock.Callout -> CalloutBlockEditor(
                    block = block, isSelected = isSelected, onSelectionChange = onSelectionChange,
                    onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                )
                is StudyBlock.Verse -> VerseBlockEditor(
                    block = block, isSelected = isSelected, onSelectionChange = onSelectionChange,
                    onTextChange = { newText -> onReplace(block.copy(primaryText = newText)) },
                )
                is StudyBlock.Divider -> DividerBlockView()
                is StudyBlock.PageBreak -> PageBreakBlockView()
                is StudyBlock.Table -> TableBlockEditor(
                    block = block, isSelected = isSelected,
                    onAddRow = { onReplace(block.copy(rows = block.rows + StudyBlock.Table.TableRow(cells = block.rows.firstOrNull()?.cells ?: listOf(StyledText.Empty, StyledText.Empty)))) },
                    onAddCol = { onReplace(block.copy(rows = block.rows.map { it.copy(cells = it.cells + StyledText.Empty) })) },
                    onHeaderToggle = { onReplace(block.copy(hasHeaderRow = !block.hasHeaderRow)) },
                    onCellChange = { row, col, cell ->
                        val newRows = block.rows.toMutableList()
                        val newCells = newRows[row].cells.toMutableList()
                        newCells[col] = cell
                        newRows[row] = block.rows[row].copy(cells = newCells)
                        onReplace(block.copy(rows = newRows))
                    },
                )
            }
        }
    }
}



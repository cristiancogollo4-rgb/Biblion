package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.domain.InsertBlockCommand
import com.cristiancogollo.biblion.feature.studydocs.domain.MergeBlocksCommand
import com.cristiancogollo.biblion.feature.studydocs.domain.ReplaceBlockCommand
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.engine.CursorNavigator
import com.cristiancogollo.biblion.feature.studydocs.engine.SpanType
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyOp
import com.cristiancogollo.biblion.feature.studydocs.engine.hasSpan
import com.cristiancogollo.biblion.feature.studydocs.engine.isDegradableSpecialBlock
import com.cristiancogollo.biblion.feature.studydocs.engine.isEffectivelyEmpty
import com.cristiancogollo.biblion.feature.studydocs.engine.isImmutableBoundaryBlock
import com.cristiancogollo.biblion.feature.studydocs.engine.splitTextAt
import com.cristiancogollo.biblion.feature.studydocs.engine.toNavigatorKey
import com.cristiancogollo.biblion.feature.studydocs.engine.toParagraphBlock
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.TodoListBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.ColumnLayoutBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.TextStylePatch
import com.cristiancogollo.biblion.feature.studydocs.model.displayTypeName
import com.cristiancogollo.biblion.feature.studydocs.model.isList
import com.cristiancogollo.biblion.feature.studydocs.model.isTextEditable
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
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.CommentOverlay
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.TableBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.TodoListBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks.VerseBlockEditor
import com.cristiancogollo.biblion.feature.studydocs.ui.outline.OutlinePanel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun StudyDocEditorScreen(
    viewModel: StudyDocViewModel,
    onBack: () -> Unit,
    onFocusModeChanged: (() -> Unit)? = null,
    isExpandable: Boolean = false,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    isMultiColumnEnabled: Boolean = false,
    onToggleMultiColumn: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showOutline by remember { mutableStateOf(false) }
    var showSlashMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val selectionState = remember { SelectionState() }
    val focusRequesters = remember { androidx.compose.runtime.mutableStateMapOf<BlockId, androidx.compose.ui.focus.FocusRequester>() }
    var selectedBlockIds by remember { mutableStateOf(setOf<BlockId>()) }
    val context = LocalContext.current

    val scrollState = androidx.compose.foundation.rememberScrollState()

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
        val targetIds = if (selectedBlockIds.size > 1) selectedBlockIds
        else setOfNotNull(uiState.selectedBlockId?.let { BlockId(it) })

        targetIds.forEach { targetId ->
            val target = uiState.doc.blocks.firstOrNull { it.id == targetId } ?: return@forEach
            val text = when (target) {
                is StudyBlock.Paragraph -> target.text
                is StudyBlock.Heading -> target.text
                is StudyBlock.Quote -> target.text
                is StudyBlock.Note -> target.text
                is StudyBlock.Reflection -> target.text
                is StudyBlock.Callout -> target.text
                is StudyBlock.Verse -> target.primaryText
                is StudyBlock.BulletList -> target.items.firstOrNull() ?: StyledText.Empty
                is StudyBlock.NumberedList -> target.items.firstOrNull() ?: StyledText.Empty
                is StudyBlock.TodoList -> target.items.firstOrNull()?.text ?: StyledText.Empty
                else -> StyledText.Empty
            }
            val newBlock: StudyBlock? = when (index) {
                0 -> StudyBlock.Paragraph(id = targetId, text = text)
                1 -> StudyBlock.Heading(id = targetId, level = 1, text = text)
                2 -> StudyBlock.Heading(id = targetId, level = 2, text = text)
                3 -> StudyBlock.Heading(id = targetId, level = 3, text = text)
                4 -> StudyBlock.Quote(id = targetId, text = text)
                5 -> StudyBlock.BulletList(id = targetId, items = listOf(text))
                6 -> StudyBlock.NumberedList(id = targetId, items = listOf(text))
                7 -> StudyBlock.Note(id = targetId, text = text)
                else -> null
            }
            if (newBlock != null) {
                viewModel.executeCommand(ReplaceBlockCommand(targetId, newBlock))
            }
        }
        selectedBlockIds = emptySet()
    }

    val lastFocusedBlockId by viewModel.lastFocusedBlockId.collectAsState()
    val currentTfv = lastFocusedBlockId?.let { viewModel.blockTextStates[it] }
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
                    IconButton(onClick = {
                        val pdfFile = PdfExporter.export(context, uiState.doc)
                        ShareHelper.sharePdf(context, pdfFile)
                    }) {
                        Text("PDF", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        ShareHelper.shareBiblion(context, uiState.doc)
                    }) {
                        Text(".bib", style = MaterialTheme.typography.labelSmall)
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
                        isEmpty = block.isEffectivelyEmpty(),
                        isDegradableSpecial = block.isDegradableSpecialBlock(),
                        isPrevImmutable = blockIndex > 0 && blocks.getOrNull(blockIndex - 1)?.isImmutableBoundaryBlock() == true,
                    )
                    when (action) {
                        is CursorNavigator.Action.MoveFocusTo -> {
                            val targetId = blocks.getOrNull(action.blockIndex)?.id
                            if (targetId == null) return@onPreviewKeyEvent false
                            viewModel.selectBlock(targetId.value)
                            true
                        }
                        is CursorNavigator.Action.MergeWithPrevious -> {
                            val currentIndex = action.blockIndex
                            val previousIndex = currentIndex - 1
                            val previousBlock = blocks.getOrNull(previousIndex)
                            val currentBlock = blocks.getOrNull(currentIndex)
                            if (previousBlock !is StudyBlock.Paragraph || currentBlock !is StudyBlock.Paragraph) {
                                return@onPreviewKeyEvent false
                            }
                            val mergedPlain = previousBlock.text.plain() + currentBlock.text.plain()
                            viewModel.executeCommand(
                                MergeBlocksCommand(
                                    targetIndex = previousIndex,
                                    targetOriginalText = previousBlock.text,
                                    sourceIndex = currentIndex,
                                    sourceOriginalText = currentBlock.text,
                                ),
                            )
                            viewModel.selectBlock(previousBlock.id.value)
                            viewModel.blockTextStates[previousBlock.id] = androidx.compose.ui.text.input.TextFieldValue(
                                annotatedString = androidx.compose.ui.text.AnnotatedString(mergedPlain),
                                selection = androidx.compose.ui.text.TextRange(mergedPlain.length),
                            )
                            viewModel.setActiveRange(previousBlock.id, null)
                            true
                        }
                        is CursorNavigator.Action.InsertNewBlockAfter -> {
                            val newBlock = StudyBlock.Paragraph()
                            viewModel.executeCommand(
                                InsertBlockCommand(
                                    atIndex = blockIndex + 1,
                                    block = newBlock,
                                ),
                            )
                            viewModel.selectBlock(newBlock.id.value)
                            true
                        }
                        is CursorNavigator.Action.DegradeToParagraph -> {
                            val curr = blocks.getOrNull(action.blockIndex) ?: return@onPreviewKeyEvent false
                            val para = curr.toParagraphBlock() ?: return@onPreviewKeyEvent false
                            viewModel.executeCommand(ReplaceBlockCommand(curr.id, para))
                            viewModel.blockTextStates[curr.id] = androidx.compose.ui.text.input.TextFieldValue(
                                annotatedString = androidx.compose.ui.text.AnnotatedString(para.text.plain()),
                                selection = androidx.compose.ui.text.TextRange(currentValue.selection.start.coerceAtMost(para.text.length)),
                            )
                            true
                        }
                        is CursorNavigator.Action.SelectPreviousBlock -> {
                            val targetId = blocks.getOrNull(action.blockIndex)?.id ?: return@onPreviewKeyEvent false
                            viewModel.selectBlock(targetId.value)
                            true
                        }
                        is CursorNavigator.Action.SplitBlockAt -> {
                            val curr = blocks.getOrNull(action.blockIndex) ?: return@onPreviewKeyEvent false
                            val (left, right) = curr.splitTextAt(action.cursorOffset) ?: return@onPreviewKeyEvent false
                            viewModel.executeCommand(ReplaceBlockCommand(curr.id, left))
                            viewModel.executeCommand(InsertBlockCommand(action.blockIndex + 1, right))
                            viewModel.selectBlock(right.id.value)
                            viewModel.blockTextStates[right.id] = androidx.compose.ui.text.input.TextFieldValue(
                                annotatedString = androidx.compose.ui.text.AnnotatedString(right.text.plain()),
                                selection = androidx.compose.ui.text.TextRange(0),
                            )
                            true
                        }
                        is CursorNavigator.Action.EscapeToParagraph -> {
                            val curr = blocks.getOrNull(action.blockIndex) ?: return@onPreviewKeyEvent false
                            val para = curr.toParagraphBlock() ?: return@onPreviewKeyEvent false
                            viewModel.executeCommand(ReplaceBlockCommand(curr.id, para))
                            viewModel.blockTextStates[curr.id] = androidx.compose.ui.text.input.TextFieldValue(
                                annotatedString = androidx.compose.ui.text.AnnotatedString(""),
                                selection = androidx.compose.ui.text.TextRange(0),
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
                        onHeadingClick = { _, _ ->
                            showOutline = false
                        },
                        onClose = { showOutline = false },
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    EditorTopBar(
                        isFormatEnabled = uiState.selectedBlockId != null || selectedBlockIds.isNotEmpty(),
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
                            if (selectedBlockIds.size > 1) {
                                "${selectedBlockIds.size} bloques"
                            } else {
                                val sid = uiState.selectedBlockId
                                if (sid == null) null
                                else uiState.doc.blocks.firstOrNull { it.id.value == sid }?.displayTypeName?.takeIf { it.isNotEmpty() }
                            }
                        },
                        isTypeDropdownEnabled = if (selectedBlockIds.size > 1) true
                        else uiState.selectedBlockId?.let { sid ->
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
                        fontSizeLabel = "${(viewModel.currentFontSize / DocConfig.FontSize.value * 100).toInt()}%",
                        isFontSizeAtMax = viewModel.fontSizeIndex.collectAsState().value >= DocConfig.FontSizeLevels.lastIndex,
                        isFontSizeAtMin = viewModel.fontSizeIndex.collectAsState().value <= 0,
                        onFontSizeIncrease = { viewModel.increaseFontSize() },
                        onFontSizeDecrease = { viewModel.decreaseFontSize() },
                        isExpandable = isExpandable,
                        isExpanded = isExpanded,
                        onToggleExpand = onToggleExpand,
                        isMultiColumnEnabled = isMultiColumnEnabled,
                        onToggleMultiColumn = onToggleMultiColumn,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .verticalScroll(scrollState),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DocConfig.EditorHorizontalPadding)
                                .padding(vertical = DocConfig.EditorContentVerticalPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier.widthIn(max = DocConfig.EditorMaxWidth).fillMaxWidth(),
                            ) {
                                if (isMultiColumnEnabled && uiState.doc.blocks.size >= 4) {
                                    val blocks = uiState.doc.blocks
                                    val mid = blocks.size / 2
                                    val leftBlocks = blocks.subList(0, mid)
                                    val rightBlocks = blocks.subList(mid, blocks.size)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            leftBlocks.forEachIndexed { index, block ->
                                                BlockWithHandle(
                                                    block = block,
                                                    blockIndex = index,
                                                    isSelected = uiState.selectedBlockId == block.id.value || block.id in selectedBlockIds,
                                                    blockAlignments = viewModel.blockAlignments,
                                                    selectionState = selectionState,
                                                    onClick = { selectedBlockIds = emptySet(); viewModel.selectBlock(block.id.value) },
                                                    onMultiSelectClick = { isShift, isCtrl ->
                                                        if (isShift && selectedBlockIds.isNotEmpty()) {
                                                            val firstSelectedIndex = uiState.doc.blocks.indexOfFirst { it.id in selectedBlockIds }
                                                            val currentIndex = index
                                                            val start = minOf(firstSelectedIndex, currentIndex)
                                                            val end = maxOf(firstSelectedIndex, currentIndex)
                                                            selectedBlockIds = (start..end).map { uiState.doc.blocks[it].id }.toSet()
                                                            viewModel.selectBlock(block.id.value)
                                                        } else if (isCtrl) {
                                                            selectedBlockIds = if (block.id in selectedBlockIds)
                                                                selectedBlockIds - block.id else selectedBlockIds + block.id
                                                            viewModel.selectBlock(block.id.value)
                                                        }
                                                    },
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
                                                    onBlockFieldFocus = { fieldKey -> viewModel.onFieldFocused(fieldKey) },
                                                    onListItemFieldValueChange = { idx, tfv ->
                                                        viewModel.blockTextStates[BlockId("${block.id.value}:item:$idx")] = tfv
                                                    },
                                                    onListItemFocusChanged = { idx ->
                                                        viewModel.onFieldFocused("${block.id.value}:item:$idx")
                                                    },
                                                    fontSize = viewModel.currentFontSize.sp,
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            rightBlocks.forEachIndexed { index, block ->
                                                BlockWithHandle(
                                                    block = block,
                                                    blockIndex = mid + index,
                                                    isSelected = uiState.selectedBlockId == block.id.value || block.id in selectedBlockIds,
                                                    blockAlignments = viewModel.blockAlignments,
                                                    selectionState = selectionState,
                                                    onClick = { selectedBlockIds = emptySet(); viewModel.selectBlock(block.id.value) },
                                                    onMultiSelectClick = { isShift, isCtrl ->
                                                        if (isShift && selectedBlockIds.isNotEmpty()) {
                                                            val firstSelectedIndex = uiState.doc.blocks.indexOfFirst { it.id in selectedBlockIds }
                                                            val currentIndex = mid + index
                                                            val start = minOf(firstSelectedIndex, currentIndex)
                                                            val end = maxOf(firstSelectedIndex, currentIndex)
                                                            selectedBlockIds = (start..end).map { uiState.doc.blocks[it].id }.toSet()
                                                            viewModel.selectBlock(block.id.value)
                                                        } else if (isCtrl) {
                                                            selectedBlockIds = if (block.id in selectedBlockIds)
                                                                selectedBlockIds - block.id else selectedBlockIds + block.id
                                                            viewModel.selectBlock(block.id.value)
                                                        }
                                                    },
                                                    onSelectionChange = { range ->
                                                        selectionState.set(block.id.value, range)
                                                        viewModel.setActiveRange(block.id, range)
                                                        viewModel.selectBlock(block.id.value)
                                                    },
                                                    onReplace = { newBlock ->
                                                        viewModel.applyOp(StudyOp.ReplaceBlock(block.id, newBlock))
                                                    },
                                                    onInsert = { atIndex, newBlock ->
                                                        viewModel.applyOp(StudyOp.InsertBlock(mid + atIndex, newBlock))
                                                    },
                                                    onDelete = { viewModel.applyOp(StudyOp.DeleteBlock(block.id)) },
                                                    onFieldValueChange = { tfv ->
                                                        viewModel.blockTextStates[block.id] = tfv
                                                    },
                                                    onRegisterFocus = { fr -> focusRequesters[block.id] = fr },
                                                    onBlockFieldFocus = { fieldKey -> viewModel.onFieldFocused(fieldKey) },
                                                    onListItemFieldValueChange = { idx, tfv ->
                                                        viewModel.blockTextStates[BlockId("${block.id.value}:item:$idx")] = tfv
                                                    },
                                                    onListItemFocusChanged = { idx ->
                                                        viewModel.onFieldFocused("${block.id.value}:item:$idx")
                                                    },
                                                    fontSize = viewModel.currentFontSize.sp,
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    uiState.doc.blocks.forEachIndexed { index, block ->
                                        BlockWithHandle(
                                            block = block,
                                            blockIndex = index,
                                            isSelected = uiState.selectedBlockId == block.id.value || block.id in selectedBlockIds,
                                            blockAlignments = viewModel.blockAlignments,
                                            selectionState = selectionState,
                                            onClick = { selectedBlockIds = emptySet(); viewModel.selectBlock(block.id.value) },
                                            onMultiSelectClick = { isShift, isCtrl ->
                                                if (isShift && selectedBlockIds.isNotEmpty()) {
                                                    val firstSelectedIndex = uiState.doc.blocks.indexOfFirst { it.id in selectedBlockIds }
                                                    val currentIndex = index
                                                    val start = minOf(firstSelectedIndex, currentIndex)
                                                    val end = maxOf(firstSelectedIndex, currentIndex)
                                                    selectedBlockIds = (start..end).map { uiState.doc.blocks[it].id }.toSet()
                                                    viewModel.selectBlock(block.id.value)
                                                } else if (isCtrl) {
                                                    selectedBlockIds = if (block.id in selectedBlockIds)
                                                        selectedBlockIds - block.id else selectedBlockIds + block.id
                                                    viewModel.selectBlock(block.id.value)
                                                }
                                            },
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
                                            onBlockFieldFocus = { fieldKey -> viewModel.onFieldFocused(fieldKey) },
                                            onListItemFieldValueChange = { idx, tfv ->
                                                viewModel.blockTextStates[BlockId("${block.id.value}:item:$idx")] = tfv
                                            },
                                            onListItemFocusChanged = { idx ->
                                                viewModel.onFieldFocused("${block.id.value}:item:$idx")
                                            },
                                            fontSize = viewModel.currentFontSize.sp,
                                        )
                                    }
                                }
                                val docComments = uiState.doc.blocks.filterIsInstance<StudyBlock.Comment>()
                                if (docComments.isNotEmpty()) {
                                    CommentOverlay(
                                        comments = docComments,
                                        onCommentClick = { commentId ->
                                            viewModel.selectBlock(commentId.value)
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    )
                                }
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
    onMultiSelectClick: (isShift: Boolean, isCtrl: Boolean) -> Unit = { _, _ -> },
    onSelectionChange: (IntRange?) -> Unit,
    onReplace: (StudyBlock) -> Unit,
    onInsert: (Int, StudyBlock) -> Unit,
    onDelete: () -> Unit,
    onFieldValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onRegisterFocus: (androidx.compose.ui.focus.FocusRequester) -> Unit,
    onBlockFieldFocus: (String) -> Unit,
    onListItemFieldValueChange: (Int, androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onListItemFocusChanged: (Int) -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit = com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig.FontSize,
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
            @Suppress("DEPRECATION")
            val dragIcon = Icons.Filled.MenuBook
            Icon(
                imageVector = dragIcon,
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
                    fontSize = fontSize,
                )
                is StudyBlock.Heading -> HeadingBlockEditor(
                    block = block, isSelected = isSelected, onClick = onClick,
                    onSelectionChange = onSelectionChange, onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                    onFocusChanged = { if (it) onBlockFieldFocus(block.id.value) },
                    textAlign = blockAlign,
                    fontSize = fontSize,
                )
                is StudyBlock.Quote -> QuoteBlockEditor(
                    block = block, isSelected = isSelected, onClick = onClick,
                    onSelectionChange = onSelectionChange, onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                    onFocusChanged = { if (it) onBlockFieldFocus(block.id.value) },
                    textAlign = blockAlign,
                    fontSize = fontSize,
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
                    onItemFieldValueChange = { idx, tfv -> onListItemFieldValueChange(idx, tfv) },
                    onItemFocusChanged = { idx -> onListItemFocusChanged(idx) },
                    fontSize = fontSize,
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
                    onItemFieldValueChange = { idx, tfv -> onListItemFieldValueChange(idx, tfv) },
                    onItemFocusChanged = { idx -> onListItemFocusChanged(idx) },
                    fontSize = fontSize,
                )
                is StudyBlock.Note -> NoteBlockEditor(
                    block = block, isSelected = isSelected, onSelectionChange = onSelectionChange,
                    onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                    fontSize = fontSize,
                )
                is StudyBlock.Reflection -> ReflectionBlockEditor(
                    block = block, isSelected = isSelected, onSelectionChange = onSelectionChange,
                    onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                    fontSize = fontSize,
                )
                is StudyBlock.Callout -> CalloutBlockEditor(
                    block = block, isSelected = isSelected, onSelectionChange = onSelectionChange,
                    onTextChange = { newText -> onReplace(block.copy(text = newText)) },
                    fontSize = fontSize,
                )
                is StudyBlock.Verse -> VerseBlockEditor(
                    block = block,
                    isSelected = isSelected,
                    onReplace = onReplace,
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
                is StudyBlock.TodoList -> TodoListBlockEditor(
                    block = block,
                    isSelected = isSelected,
                    onItemCheck = { idx, checked ->
                        val updated = block.items.toMutableList().apply {
                            this[idx] = this[idx].copy(checked = checked)
                        }
                        onReplace(block.copy(items = updated))
                    },
                    onItemTextChange = { idx, text ->
                        val updated = block.items.toMutableList().apply {
                            this[idx] = this[idx].copy(text = text)
                        }
                        onReplace(block.copy(items = updated))
                    },
                    onAppendItem = {
                        onReplace(block.copy(items = block.items + StudyBlock.TodoList.TodoItem()))
                    },
                    onRemoveItem = { idx ->
                        if (block.items.size > 1) {
                            onReplace(block.copy(items = block.items.toMutableList().apply { removeAt(idx) }))
                        }
                    },
                    onItemFieldValueChange = { idx, tfv ->
                        onListItemFieldValueChange(idx, tfv)
                    },
                    onItemFocusChanged = { idx ->
                        onListItemFocusChanged(idx)
                    },
                    fontSize = fontSize,
                )
                is StudyBlock.ColumnLayout -> ColumnLayoutBlockEditor(
                    block = block,
                    isSelected = isSelected,
                    onReplace = onReplace,
                )
                is StudyBlock.Comment -> {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = block.text.ifBlank { "Comentario" },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}



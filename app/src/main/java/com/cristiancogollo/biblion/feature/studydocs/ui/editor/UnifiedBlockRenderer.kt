package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.domain.EditorFocusRequest
import com.cristiancogollo.biblion.feature.studydocs.domain.EditorTextKey
import com.cristiancogollo.biblion.feature.studydocs.debug.StudyEditorDebugLog
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.toAnnotatedString
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

/**
 * Renderizador unificado de bloques para editor (isEditing=true) y visualizador
 * (isEditing=false).
 *
 * Misma fuente de verdad: [block] y [allBlocks].
 *
 * Sobrecarga por bloque (la usa el modo split actual, sin paginacion visual).
 * Para renderizar un [PageFragment] producido por [com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginationEngine]
 * usa la sobrecarga que acepta un fragmento.
 */
@Composable
fun UnifiedBlockRenderer(
    block: StudyBlock,
    blockIndex: Int,
    richState: RichTextState?,
    isActive: Boolean,
    isEditing: Boolean,
    allBlocks: List<StudyBlock>,
    focusRequest: EditorFocusRequest? = null,
    splitViewModel: StudyDocSplitViewModel? = null,
    viewModel: StudyDocViewModel? = null,
    onVerseClick: ((StudyBlock.Verse) -> Unit)? = null,
    onVerseComparisonSelected: ((StudyBlock.Verse, String?) -> Unit)? = null,
    onVerseDelete: ((StudyBlock.Verse) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val textStyle = remember(block, block.fontSize, isEditing) { buildTextStyle(block, textColor) }

    if (isEditing) {
        if (block is StudyBlock.BulletList || block is StudyBlock.OrderedList) {
            EditableListBlock(
                block = block,
                blockIndex = blockIndex,
                allBlocks = allBlocks,
                textStyle = textStyle,
                isActive = isActive,
                focusRequest = focusRequest,
                splitViewModel = splitViewModel,
                viewModel = viewModel,
                modifier = modifier,
            )
            return
        }
        Log.d("BIBLION_STUDY", "UnifiedBlockRenderer(block) EDITING blockId=${block.id} richState=${richState != null} text=[${richState?.annotatedString?.text?.take(40)}]")
        val focusRequester = remember(block.id) { FocusRequester() }
        var hasPhysicalFocus by remember(block.id) { mutableStateOf(false) }
        val editorKey = EditorTextKey(block.id)
        LaunchedEffect(editorKey) {
            if (isActive && focusRequest == null) {
                StudyEditorDebugLog.log(
                    "FOCUS_MOUNT_RESTORE",
                    "renderer=block block=${block.id.value}",
                )
                requestFocusWithRetry(
                    requester = focusRequester,
                    target = "renderer=block-remount block=${block.id.value}",
                )
            }
        }
        LaunchedEffect(block.id, isActive, focusRequest?.sequence) {
            if (isActive && focusRequest?.target == editorKey) {
                val requested = requestFocusWithRetry(
                    requester = focusRequester,
                    target = "renderer=block block=${block.id.value} active=$isActive",
                )
                StudyEditorDebugLog.log(
                    "FOCUS_REQUEST_RESULT",
                    "target=${editorKey.blockId.value} requested=$requested sequence=${focusRequest.sequence}",
                )
                if (requested && hasPhysicalFocus) {
                    splitViewModel?.onEditorFocused(block.id)
                        ?: viewModel?.onEditorFocused(block.id)
                }
            }
        }

        LaunchedEffect(richState, block.id, splitViewModel, viewModel) {
            if (richState != null) {
                snapshotFlow { richState.selection }
                    .collect {
                        if (isActive) {
                            splitViewModel?.syncActiveFormat(richState)
                                ?: viewModel?.syncActiveFormat(richState)
                        }
                    }
            }
        }

        LaunchedEffect(richState, block.id, splitViewModel, viewModel) {
            if (richState != null) {
                snapshotFlow { richState.annotatedString }
                    .collect {
                        splitViewModel?.onRichTextChanged(block.id, richState)
                            ?: viewModel?.onRichTextChanged(block.id, richState)
                    }
            }
        }

        val keyModifier = if (richState != null) {
            Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val sel = richState.selection
                StudyEditorDebugLog.log(
                    "KEY_EVENT",
                    "renderer=block block=${block.id.value} key=${event.key} shift=${event.isShiftPressed} " +
                        "selection=$sel textLen=${richState.annotatedString.length} blockIndex=$blockIndex " +
                        "active=$isActive",
                )
                when (event.key) {
                    Key.Enter -> {
                        if (event.isShiftPressed) return@onPreviewKeyEvent false
                        StudyEditorDebugLog.log(
                            "KEY_ENTER",
                            "renderer=block block=${block.id.value} cursor=${richState.selection} " +
                                "stateLen=${richState.annotatedString.length}",
                        )
                        val handled = splitViewModel?.handleEnter(block.id)
                            ?: viewModel?.handleEnter(block.id)
                            ?: false
                        StudyEditorDebugLog.log(
                            "KEY_ENTER_RESULT",
                            "renderer=block block=${block.id.value} handled=$handled",
                        )
                        handled
                    }
                    Key.Backspace -> {
                        if (sel.collapsed && sel.start == 0) {
                            splitViewModel?.handleBackspace(block.id)
                                ?: viewModel?.handleBackspace(block.id)
                                ?: false
                        } else false
                    }
                    Key.DirectionUp -> {
                        val allowed = sel.collapsed && sel.start == 0 && blockIndex > 0
                        val handled = if (allowed) {
                            splitViewModel?.moveCursorToPrevBlock(block.id)
                                ?: viewModel?.moveCursorToPrevBlock(block.id)
                                ?: false
                        } else false
                        StudyEditorDebugLog.log(
                            "KEY_ARROW_RESULT",
                            "renderer=block direction=up block=${block.id.value} allowed=$allowed handled=$handled",
                        )
                        handled
                    }
                    Key.DirectionDown -> {
                        val text = richState.annotatedString.text
                        val totalBlocks = splitViewModel?.editorState?.value?.doc?.blocks?.size
                            ?: viewModel?.uiState?.value?.doc?.blocks?.size ?: 0
                        val allowed = sel.collapsed && sel.start >= text.length && blockIndex < totalBlocks - 1
                        val handled = if (allowed) {
                            splitViewModel?.moveCursorToNextBlock(block.id)
                                ?: viewModel?.moveCursorToNextBlock(block.id)
                                ?: false
                        } else false
                        StudyEditorDebugLog.log(
                            "KEY_ARROW_RESULT",
                            "renderer=block direction=down block=${block.id.value} allowed=$allowed handled=$handled " +
                                "blockIndex=$blockIndex total=$totalBlocks",
                        )
                        handled
                    }
                    else -> false
                }
            }
        } else Modifier

        val imeAction = KeyboardActions(
            onDone = { splitViewModel?.handleEnter(block.id) ?: viewModel?.handleEnter(block.id) },
        )
        val imeOptions = KeyboardOptions(imeAction = ImeAction.Done)

        when (block) {
            is StudyBlock.Verse -> BibleVerseBlock(
                block = block,
                mode = VerseBlockMode.Editing,
                isSelected = isActive,
                onClick = onVerseClick?.let { callback -> { callback(block) } },
                onComparisonSelected = onVerseComparisonSelected?.let { callback ->
                    { version -> callback(block, version) }
                },
                onDelete = onVerseDelete?.let { callback -> { callback(block) } },
                modifier = modifier.fillMaxWidth(),
            )
            is StudyBlock.BulletList -> {
                Row(modifier = modifier.fillMaxWidth().then(keyModifier)
                    .onFocusChanged {
                        hasPhysicalFocus = it.hasFocus
                        StudyEditorDebugLog.log(
                            "FOCUS_CHANGED",
                            "renderer=block block=${block.id.value} focused=${it.isFocused} " +
                                "hasFocus=${it.hasFocus} active=$isActive",
                        )
                        if (it.hasFocus) {
                            splitViewModel?.onEditorFocused(block.id) ?: viewModel?.onEditorFocused(block.id)
                        }
                    },
                    verticalAlignment = Alignment.Top) {
                    Text("\u2022", style = textStyle, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
                    BasicRichTextEditor(state = richState!!, modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        textStyle = textStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = false, keyboardOptions = imeOptions, keyboardActions = imeAction)
                }
            }
            is StudyBlock.OrderedList -> {
                val num = calculateOrderedListNumber(allBlocks, blockIndex)
                Row(modifier = modifier.fillMaxWidth().then(keyModifier)
                    .onFocusChanged {
                        hasPhysicalFocus = it.hasFocus
                        StudyEditorDebugLog.log(
                            "FOCUS_CHANGED",
                            "renderer=block block=${block.id.value} focused=${it.isFocused} " +
                                "hasFocus=${it.hasFocus} active=$isActive",
                        )
                        if (it.hasFocus) {
                            splitViewModel?.onEditorFocused(block.id) ?: viewModel?.onEditorFocused(block.id)
                        }
                    },
                    verticalAlignment = Alignment.Top) {
                    Text("$num.", style = textStyle, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
                    BasicRichTextEditor(state = richState!!, modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        textStyle = textStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = false, keyboardOptions = imeOptions, keyboardActions = imeAction)
                }
            }
            else -> {
                BasicRichTextEditor(state = richState!!,
                    modifier = modifier.fillMaxWidth().then(keyModifier).focusRequester(focusRequester)
                        .onFocusChanged {
                            hasPhysicalFocus = it.isFocused
                            StudyEditorDebugLog.log(
                                "FOCUS_CHANGED",
                                "renderer=block block=${block.id.value} focused=${it.isFocused} " +
                                    "hasFocus=${it.hasFocus} active=$isActive selection=${richState.selection}",
                            )
                            if (it.isFocused) {
                                splitViewModel?.onEditorFocused(block.id) ?: viewModel?.onEditorFocused(block.id)
                            }
                        },
                    textStyle = textStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = false, keyboardOptions = imeOptions, keyboardActions = imeAction)
            }
        }
    } else {
        when (block) {
            is StudyBlock.Verse -> BibleVerseBlock(
                block = block,
                mode = VerseBlockMode.Reading,
                onClick = onVerseClick?.let { callback -> { callback(block) } },
                onComparisonSelected = onVerseComparisonSelected?.let { callback ->
                    { version -> callback(block, version) }
                },
                modifier = modifier.fillMaxWidth(),
            )
            is StudyBlock.BulletList -> {
                val itemTexts = block.toStyledTextList()
                Column(modifier = modifier.fillMaxWidth()) {
                    itemTexts.forEach { styled ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("\u2022  ", style = textStyle)
                            Text(text = styled.toAnnotatedString(), style = textStyle)
                        }
                    }
                }
            }
            is StudyBlock.OrderedList -> {
                val itemTexts = block.toStyledTextList()
                Column(modifier = modifier.fillMaxWidth()) {
                    val startNum = calculateOrderedListNumber(allBlocks, blockIndex)
                    itemTexts.forEachIndexed { i, styled ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("${startNum + i}. ", style = textStyle)
                            Text(text = styled.toAnnotatedString(), style = textStyle)
                        }
                    }
                }
            }
            is StudyBlock.Quote -> {
                Column(modifier = modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    val styled = block.toStyledTextList().firstOrNull()
                    if (styled != null) {
                        Text(text = styled.toAnnotatedString(), style = textStyle.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(start = 16.dp))
                    }
                    if (!block.attribution.isNullOrBlank()) {
                        Text("\u2014 ${block.attribution}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            else -> {
                val styled = block.toStyledTextList().firstOrNull()
                if (styled != null) {
                    Text(text = styled.toAnnotatedString(), style = textStyle, modifier = modifier.fillMaxWidth())
                }
            }
        }

        LaunchedEffect(richState, block.id) {
            if (richState != null) {
                snapshotFlow { richState.annotatedString }
                    .collect {
                        splitViewModel?.onRichTextChanged(block.id, richState)
                            ?: viewModel?.onRichTextChanged(block.id, richState)
                    }
            }
        }
    }
}

@Composable
private fun EditableListBlock(
    block: StudyBlock,
    blockIndex: Int,
    allBlocks: List<StudyBlock>,
    textStyle: TextStyle,
    isActive: Boolean,
    focusRequest: EditorFocusRequest?,
    splitViewModel: StudyDocSplitViewModel?,
    viewModel: StudyDocViewModel?,
    modifier: Modifier,
) {
    val items = when (block) {
        is StudyBlock.BulletList -> block.items
        is StudyBlock.OrderedList -> block.items
        else -> return
    }
    val listStart = if (block is StudyBlock.OrderedList) {
        calculateOrderedListNumber(allBlocks, blockIndex)
    } else 0

    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { itemIndex, _ ->
            val state = splitViewModel?.richStateFor(block.id, itemIndex)
                ?: viewModel?.richStateFor(block.id, itemIndex)
                ?: return@forEachIndexed
            val itemFocusRequester = remember(block.id, itemIndex) { FocusRequester() }
            var hasPhysicalFocus by remember(block.id, itemIndex) { mutableStateOf(false) }
            val activeItemIndex = splitViewModel?.editorState?.value?.activeListItemIndex
                ?: viewModel?.uiState?.value?.activeListItemIndex
            val isItemActive = isActive && activeItemIndex == itemIndex

            val editorKey = EditorTextKey(block.id, itemIndex)
            LaunchedEffect(editorKey) {
                if (isItemActive && focusRequest == null) {
                    StudyEditorDebugLog.log(
                        "FOCUS_MOUNT_RESTORE",
                        "renderer=list block=${block.id.value} item=$itemIndex",
                    )
                    requestFocusWithRetry(
                        requester = itemFocusRequester,
                        target = "renderer=list-remount block=${block.id.value} item=$itemIndex",
                    )
                }
            }
            LaunchedEffect(block.id, itemIndex, isItemActive, focusRequest?.sequence) {
                if (isItemActive && focusRequest?.target == editorKey) {
                    val requested = requestFocusWithRetry(
                        requester = itemFocusRequester,
                        target = "renderer=list block=${block.id.value} item=$itemIndex active=$isItemActive",
                    )
                    StudyEditorDebugLog.log(
                        "FOCUS_REQUEST_RESULT",
                        "target=${editorKey.blockId.value}.$itemIndex requested=$requested " +
                            "sequence=${focusRequest.sequence}",
                    )
                    if (requested && hasPhysicalFocus) {
                        splitViewModel?.onEditorFocused(block.id, itemIndex)
                            ?: viewModel?.onEditorFocused(block.id, itemIndex)
                    }
                }
            }
            LaunchedEffect(state, block.id, itemIndex, splitViewModel, viewModel) {
                snapshotFlow { state.selection }
                    .collect {
                        if (isItemActive) {
                            splitViewModel?.syncActiveFormat(state)
                                ?: viewModel?.syncActiveFormat(state)
                        }
                    }
            }
            LaunchedEffect(state, block.id, itemIndex, splitViewModel, viewModel) {
                snapshotFlow { state.annotatedString }
                    .collect {
                        splitViewModel?.onRichTextChanged(block.id, state, itemIndex)
                            ?: viewModel?.onRichTextChanged(block.id, state, itemIndex)
                    }
            }

            val keyModifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                StudyEditorDebugLog.log(
                    "KEY_EVENT",
                    "renderer=list block=${block.id.value} item=$itemIndex key=${event.key} " +
                        "shift=${event.isShiftPressed} selection=${state.selection} " +
                        "textLen=${state.annotatedString.length} active=$isItemActive",
                )
                when (event.key) {
                    Key.Enter -> if (event.isShiftPressed) false else {
                        val handled = splitViewModel?.handleEnter(block.id)
                            ?: viewModel?.handleEnter(block.id)
                            ?: false
                        StudyEditorDebugLog.log(
                            "KEY_LIST_RESULT",
                            "action=enter block=${block.id.value} item=$itemIndex handled=$handled",
                        )
                        handled
                    }
                    Key.Backspace -> {
                        val handled = splitViewModel?.handleBackspace(block.id)
                            ?: viewModel?.handleBackspace(block.id)
                            ?: false
                        StudyEditorDebugLog.log(
                            "KEY_LIST_RESULT",
                            "action=backspace block=${block.id.value} item=$itemIndex handled=$handled",
                        )
                        handled
                    }
                    Key.DirectionUp -> if (state.selection.collapsed && state.selection.start == 0) {
                        val handled = splitViewModel?.moveCursorToPrevListItem(block.id, itemIndex)
                            ?: viewModel?.moveCursorToPrevListItem(block.id, itemIndex)
                            ?: false
                        StudyEditorDebugLog.log(
                            "KEY_ARROW_RESULT",
                            "renderer=list direction=up block=${block.id.value} item=$itemIndex handled=$handled",
                        )
                        handled
                    } else false
                    Key.DirectionDown -> if (state.selection.collapsed && state.selection.start >= state.annotatedString.length) {
                        val handled = splitViewModel?.moveCursorToNextListItem(block.id, itemIndex)
                            ?: viewModel?.moveCursorToNextListItem(block.id, itemIndex)
                            ?: false
                        StudyEditorDebugLog.log(
                            "KEY_ARROW_RESULT",
                            "renderer=list direction=down block=${block.id.value} item=$itemIndex handled=$handled",
                        )
                        handled
                    } else false
                    else -> false
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .then(keyModifier)
                    .onFocusChanged {
                        hasPhysicalFocus = it.hasFocus
                        StudyEditorDebugLog.log(
                            "FOCUS_CHANGED",
                            "renderer=list block=${block.id.value} item=$itemIndex focused=${it.isFocused} " +
                                "hasFocus=${it.hasFocus} active=$isItemActive selection=${state.selection}",
                        )
                        if (it.hasFocus) {
                            splitViewModel?.onEditorFocused(block.id, itemIndex)
                                ?: viewModel?.onEditorFocused(block.id, itemIndex)
                        }
                    },
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = if (block is StudyBlock.BulletList) "\u2022" else "${listStart + itemIndex}.",
                    style = textStyle,
                    modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                )
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(itemFocusRequester),
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            val handled = splitViewModel?.handleEnter(block.id)
                                ?: viewModel?.handleEnter(block.id)
                                ?: false
                            StudyEditorDebugLog.log(
                                "IME_LIST_ENTER",
                                "renderer=list block=${block.id.value} item=$itemIndex handled=$handled",
                            )
                        },
                    ),
                )
            }
        }
    }
}

@Composable
fun UnifiedBlockRenderer(
    fragment: PageFragment,
    allBlocks: List<StudyBlock>,
    isEditing: Boolean,
    isOwnerFragment: Boolean,
    richState: RichTextState?,
    isActive: Boolean,
    focusRequest: EditorFocusRequest? = null,
    splitViewModel: StudyDocSplitViewModel? = null,
    viewModel: StudyDocViewModel? = null,
    onVerseClick: ((StudyBlock.Verse) -> Unit)? = null,
    onVerseComparisonSelected: ((StudyBlock.Verse, String?) -> Unit)? = null,
    onVerseDelete: ((StudyBlock.Verse) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val block = fragment.originBlock(allBlocks)
    if (block == null) {
        StudyEditorDebugLog.log(
            "FRAGMENT_DROP",
            "origin=${fragment.originBlockId.value} fragment=$fragment " +
                "blocks=${StudyEditorDebugLog.blocksSummary(allBlocks)}",
        )
        return
    }
    Log.d("BIBLION_STUDY", "UnifiedBlockRenderer fragment=$fragment isEditing=$isEditing isOwnerFragment=$isOwnerFragment richState=${richState != null} blockId=${fragment.originBlockId} blockType=${block::class.simpleName}")
    val blockIndex = allBlocks.indexOfFirst { it.id == fragment.originBlockId }
    val textStyle = remember(block, block.fontSize, isEditing) { buildTextStyle(block, textColor) }
    fragment.charRangeOrNull()?.let { (start, endExclusive) ->
        val modelLength = block.plainText().length
        if (start > modelLength || endExclusive > modelLength) {
            StudyEditorDebugLog.log(
                "FRAGMENT_RANGE_MISMATCH",
                "origin=${fragment.originBlockId.value} range=$start..$endExclusive " +
                    "modelLen=$modelLength owner=$isOwnerFragment fragment=$fragment",
            )
        }
    }
    LaunchedEffect(fragment, isOwnerFragment, richState?.annotatedString?.length) {
        StudyEditorDebugLog.log(
            "FRAGMENT_RENDER",
            "origin=${fragment.originBlockId.value} fragment=$fragment owner=$isOwnerFragment " +
                "richLen=${richState?.annotatedString?.length} modelLen=${block.plainText().length} " +
                "allBlocks=${StudyEditorDebugLog.blocksSummary(allBlocks)}",
        )
    }

    if (isEditing && isOwnerFragment && richState != null && block !is StudyBlock.Verse) {
        when (fragment) {
            is PageFragment.ListItemSlice -> {
                EditableListItemFragment(
                    block = fragment.block,
                    itemIndex = fragment.itemIndex,
                    blockIndex = blockIndex,
                    allBlocks = allBlocks,
                    state = richState,
                    textStyle = textStyle,
                    isActive = isActive,
                    focusRequest = focusRequest,
                    splitViewModel = splitViewModel,
                    viewModel = viewModel,
                    modifier = modifier,
                )
                return
            }
            is PageFragment.OrderedListItemSlice -> {
                EditableListItemFragment(
                    block = fragment.block,
                    itemIndex = fragment.itemIndex,
                    blockIndex = blockIndex,
                    allBlocks = allBlocks,
                    state = richState,
                    textStyle = textStyle,
                    isActive = isActive,
                    focusRequest = focusRequest,
                    splitViewModel = splitViewModel,
                    viewModel = viewModel,
                    modifier = modifier,
                )
                return
            }
            else -> Unit
        }
    }

    if (isEditing && isOwnerFragment && richState != null && block !is StudyBlock.Verse) {
        UnifiedBlockRenderer(
            block = block,
            blockIndex = blockIndex,
            richState = richState,
            isActive = isActive,
            isEditing = true,
            allBlocks = allBlocks,
            focusRequest = focusRequest,
            splitViewModel = splitViewModel,
            viewModel = viewModel,
            onVerseClick = onVerseClick,
            onVerseComparisonSelected = onVerseComparisonSelected,
            onVerseDelete = onVerseDelete,
            modifier = modifier,
        )
        return
    }
    val canActivateText = !isOwnerFragment && isEditing && richState != null &&
        (block is StudyBlock.Paragraph ||
            block is StudyBlock.Heading ||
            block is StudyBlock.BulletList ||
            block is StudyBlock.OrderedList ||
            block is StudyBlock.Quote)
    val activateText: (() -> Unit)? = if (canActivateText) {
        {
            val targetOffset = fragment.charRangeOrNull()?.first ?: 0
            richState?.let { state ->
                state.selection = androidx.compose.ui.text.TextRange(
                    targetOffset.coerceIn(0, state.annotatedString.length),
                )
            }
            StudyEditorDebugLog.log(
                "FOCUS_TAP",
                "origin=${block.id.value} offset=$targetOffset fragment=$fragment",
            )
            val itemIndex = when (fragment) {
                is PageFragment.ListItemSlice -> fragment.itemIndex
                is PageFragment.OrderedListItemSlice -> fragment.itemIndex
                else -> null
            }
            splitViewModel?.setActiveBlock(block.id, itemIndex)
                ?: viewModel?.setActiveBlock(block.id, itemIndex)
        }
    } else null
    renderFragmentReadOnly(
        fragment = fragment,
        block = block,
        blockIndex = blockIndex,
        textStyle = textStyle,
        allBlocks = allBlocks,
        modifier = modifier,
        onActivate = activateText,
        onVerseClick = onVerseClick,
        onVerseComparisonSelected = onVerseComparisonSelected,
        onVerseDelete = onVerseDelete,
        isVerseSelected = isActive && block is StudyBlock.Verse,
    )
}

@Composable
private fun EditableListItemFragment(
    block: StudyBlock,
    itemIndex: Int,
    blockIndex: Int,
    allBlocks: List<StudyBlock>,
    state: RichTextState,
    textStyle: TextStyle,
    isActive: Boolean,
    focusRequest: EditorFocusRequest?,
    splitViewModel: StudyDocSplitViewModel?,
    viewModel: StudyDocViewModel?,
    modifier: Modifier,
) {
    val itemFocusRequester = remember(block.id, itemIndex) { FocusRequester() }
    var hasPhysicalFocus by remember(block.id, itemIndex) { mutableStateOf(false) }
    val listStart = if (block is StudyBlock.OrderedList) calculateOrderedListNumber(allBlocks, blockIndex) else 0
    val editorKey = EditorTextKey(block.id, itemIndex)
    LaunchedEffect(editorKey) {
        if (isActive && focusRequest == null) {
            StudyEditorDebugLog.log(
                "FOCUS_MOUNT_RESTORE",
                "renderer=list-fragment block=${block.id.value} item=$itemIndex",
            )
            requestFocusWithRetry(
                requester = itemFocusRequester,
                target = "renderer=list-fragment-remount block=${block.id.value} item=$itemIndex",
            )
        }
    }
    LaunchedEffect(block.id, itemIndex, isActive, focusRequest?.sequence) {
        if (isActive && focusRequest?.target == editorKey) {
            val requested = requestFocusWithRetry(
                requester = itemFocusRequester,
                target = "renderer=list-fragment block=${block.id.value} item=$itemIndex active=$isActive",
            )
            StudyEditorDebugLog.log(
                "FOCUS_REQUEST_RESULT",
                "target=${editorKey.blockId.value}.$itemIndex requested=$requested " +
                    "sequence=${focusRequest.sequence}",
            )
            if (requested && hasPhysicalFocus) {
                splitViewModel?.onEditorFocused(block.id, itemIndex)
                    ?: viewModel?.onEditorFocused(block.id, itemIndex)
            }
        }
    }
    LaunchedEffect(state, block.id, itemIndex) {
        snapshotFlow { state.annotatedString }
            .collect {
                splitViewModel?.onRichTextChanged(block.id, state, itemIndex)
                    ?: viewModel?.onRichTextChanged(block.id, state, itemIndex)
            }
    }
    val keyModifier = Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        StudyEditorDebugLog.log(
            "KEY_EVENT",
            "renderer=list-fragment block=${block.id.value} item=$itemIndex key=${event.key} " +
                "shift=${event.isShiftPressed} selection=${state.selection} " +
                "textLen=${state.annotatedString.length}",
        )
        when (event.key) {
            Key.Enter -> if (event.isShiftPressed) false else {
                StudyEditorDebugLog.log(
                    "KEY_ENTER",
                    "renderer=fragment block=${block.id.value} cursor=${state.selection} " +
                        "stateLen=${state.annotatedString.length}",
                )
                val handled = splitViewModel?.handleEnter(block.id)
                    ?: viewModel?.handleEnter(block.id)
                    ?: false
                StudyEditorDebugLog.log(
                    "KEY_ENTER_RESULT",
                    "renderer=fragment block=${block.id.value} handled=$handled",
                )
                handled
            }
            Key.Backspace -> {
                val handled = splitViewModel?.handleBackspace(block.id)
                    ?: viewModel?.handleBackspace(block.id)
                    ?: false
                StudyEditorDebugLog.log(
                    "KEY_LIST_RESULT",
                    "action=backspace renderer=list-fragment block=${block.id.value} item=$itemIndex handled=$handled",
                )
                handled
            }
            Key.DirectionUp -> if (state.selection.collapsed && state.selection.start == 0) {
                val handled = splitViewModel?.moveCursorToPrevListItem(block.id, itemIndex)
                    ?: viewModel?.moveCursorToPrevListItem(block.id, itemIndex)
                    ?: false
                StudyEditorDebugLog.log(
                    "KEY_ARROW_RESULT",
                    "renderer=list-fragment direction=up block=${block.id.value} item=$itemIndex handled=$handled",
                )
                handled
            } else false
            Key.DirectionDown -> if (state.selection.collapsed && state.selection.start >= state.annotatedString.length) {
                val handled = splitViewModel?.moveCursorToNextListItem(block.id, itemIndex)
                    ?: viewModel?.moveCursorToNextListItem(block.id, itemIndex)
                    ?: false
                StudyEditorDebugLog.log(
                    "KEY_ARROW_RESULT",
                    "renderer=list-fragment direction=down block=${block.id.value} item=$itemIndex handled=$handled",
                )
                handled
            } else false
            else -> false
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(keyModifier)
            .onFocusChanged {
                hasPhysicalFocus = it.hasFocus
                StudyEditorDebugLog.log(
                    "FOCUS_CHANGED",
                    "renderer=list-fragment block=${block.id.value} item=$itemIndex focused=${it.isFocused} " +
                        "hasFocus=${it.hasFocus} selection=${state.selection}",
                )
                if (it.hasFocus) {
                    splitViewModel?.onEditorFocused(block.id, itemIndex)
                        ?: viewModel?.onEditorFocused(block.id, itemIndex)
                }
            },
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = if (block is StudyBlock.BulletList) "\u2022" else "${listStart + itemIndex}.",
            style = textStyle,
            modifier = Modifier.padding(top = 2.dp, end = 8.dp),
        )
        BasicRichTextEditor(
            state = state,
            modifier = Modifier
                .weight(1f)
                .focusRequester(itemFocusRequester),
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    val handled = splitViewModel?.handleEnter(block.id)
                        ?: viewModel?.handleEnter(block.id)
                        ?: false
                    StudyEditorDebugLog.log(
                        "IME_LIST_ENTER",
                        "renderer=list-fragment block=${block.id.value} item=$itemIndex handled=$handled",
                    )
                },
            ),
        )
    }
}

internal suspend fun requestFocusWithRetry(
    requester: FocusRequester,
    target: String,
) : Boolean {
    repeat(12) { attempt ->
        if (attempt > 0) kotlinx.coroutines.delay(16L)
        val requested = runCatching { requester.requestFocus() }.getOrDefault(false)
        StudyEditorDebugLog.log(
            "FOCUS_REQUEST",
            "$target attempt=${attempt + 1} requested=$requested",
        )
        if (requested) return true
    }
    return false
}

@Composable
private fun renderFragmentReadOnly(
    fragment: PageFragment,
    block: StudyBlock,
    blockIndex: Int,
    textStyle: TextStyle,
    allBlocks: List<StudyBlock>,
    modifier: Modifier,
    onActivate: (() -> Unit)? = null,
    onVerseClick: ((StudyBlock.Verse) -> Unit)? = null,
    onVerseComparisonSelected: ((StudyBlock.Verse, String?) -> Unit)? = null,
    onVerseDelete: ((StudyBlock.Verse) -> Unit)? = null,
    isVerseSelected: Boolean = false,
) {
    val activationModifier = modifier.clickable(
        enabled = onActivate != null,
        onClick = { onActivate?.invoke() },
    )
    when (fragment) {
        is PageFragment.Whole -> {
            UnifiedBlockRenderer(
                block = block,
                blockIndex = blockIndex,
                richState = null,
                isActive = false,
                isEditing = false,
                allBlocks = allBlocks,
                focusRequest = null,
                splitViewModel = null,
                viewModel = null,
                onVerseClick = onVerseClick,
                onVerseComparisonSelected = onVerseComparisonSelected,
                onVerseDelete = onVerseDelete,
                modifier = activationModifier,
            )
        }
        is PageFragment.ParagraphSlice -> {
            val sliced = fragment.block.text.slice(fragment.charStart until fragment.charEndExclusive)
            Text(
                text = sliced.toAnnotatedString(),
                style = textStyle,
                modifier = activationModifier.fillMaxWidth(),
            )
        }
        is PageFragment.HeadingSlice -> {
            val sliced = fragment.block.text.slice(fragment.charStart until fragment.charEndExclusive)
            Text(
                text = sliced.toAnnotatedString(),
                style = textStyle,
                modifier = activationModifier.fillMaxWidth(),
            )
        }
        is PageFragment.ListItemSlice -> {
            val item = fragment.block.items.getOrNull(fragment.itemIndex) ?: return
            val sliced = item.slice(fragment.charStart until fragment.charEndExclusive)
            Row(modifier = activationModifier.fillMaxWidth()) {
                Text("\u2022  ", style = textStyle)
                Text(text = sliced.toAnnotatedString(), style = textStyle)
            }
        }
        is PageFragment.OrderedListItemSlice -> {
            val item = fragment.block.items.getOrNull(fragment.itemIndex) ?: return
            val sliced = item.slice(fragment.charStart until fragment.charEndExclusive)
            val startNum = calculateOrderedListNumber(allBlocks, blockIndex)
            Row(modifier = activationModifier.fillMaxWidth()) {
                Text("${startNum + fragment.itemIndex}. ", style = textStyle)
                Text(text = sliced.toAnnotatedString(), style = textStyle)
            }
        }
        is PageFragment.VerseSlice -> {
            val accent = verseAccent()
            val verseFocusRequester = remember(
                fragment.originBlockId,
                fragment.sliceIndex,
            ) { FocusRequester() }
            val selectVerse = onVerseClick?.let { callback ->
                {
                    if (onVerseDelete != null) {
                        runCatching { verseFocusRequester.requestFocus() }
                    }
                    callback(fragment.block)
                }
            }
            val primaryText = fragment.block.contents[fragment.block.sourceVersion].orEmpty()
            val primaryStart = fragment.charStart.coerceIn(0, primaryText.length)
            val primaryEnd = fragment.charEndExclusive.coerceIn(primaryStart, primaryText.length)
            val comparisonText = fragment.comparisonVersion
                ?.let { fragment.block.contents[it] }
                .orEmpty()
            val comparisonStart =
                fragment.comparisonCharStart.coerceIn(0, comparisonText.length)
            val comparisonEnd =
                fragment.comparisonCharEndExclusive.coerceIn(comparisonStart, comparisonText.length)
            val bodyStyle = verseBodyTextStyle(
                block = fragment.block,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .then(
                        if (onVerseDelete != null) {
                            Modifier
                                .focusRequester(verseFocusRequester)
                                .focusable()
                                .onPreviewKeyEvent { event ->
                                    val deletesBlock =
                                        event.type == KeyEventType.KeyDown &&
                                            (
                                                event.key == Key.Backspace ||
                                                    event.key == Key.Delete
                                                )
                                    if (deletesBlock) {
                                        onVerseDelete(fragment.block)
                                        true
                                    } else {
                                        false
                                    }
                                }
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (isVerseSelected) {
                            Modifier.background(accent.copy(alpha = 0.08f))
                        } else {
                            Modifier
                        }
                    )
                    .drawBehind {
                        drawRect(
                            color = accent,
                            size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height),
                        )
                    }
                    .padding(start = 16.dp),
            ) {
                if (fragment.showHeader) {
                    VerseReferenceHeader(
                        block = fragment.block,
                        onReferenceClick = selectVerse,
                        onComparisonSelected = onVerseComparisonSelected?.let {
                            { version -> it(fragment.block, version) }
                        },
                        onDelete = onVerseDelete?.let {
                            { it(fragment.block) }
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (fragment.comparisonVersion != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .clickable(enabled = selectVerse != null) {
                                selectVerse?.invoke()
                            },
                    ) {
                        VerseColumn(
                            block = fragment.block,
                            version = fragment.block.sourceVersion,
                            text = primaryText.substring(primaryStart, primaryEnd),
                            showVersion = fragment.showHeader,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                            )
                        }
                        VerseColumn(
                            block = fragment.block,
                            version = fragment.comparisonVersion,
                            text = comparisonText.substring(comparisonStart, comparisonEnd),
                            showVersion = fragment.showHeader,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Text(
                        text = primaryText.substring(primaryStart, primaryEnd),
                        style = bodyStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = selectVerse != null) {
                                selectVerse?.invoke()
                            },
                    )
                }
            }
        }
        is PageFragment.QuoteSlice -> {
            val sliced = fragment.block.text.slice(
                fragment.charStart until fragment.charEndExclusive
            )
            val accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            Text(
                text = sliced.toAnnotatedString(),
                style = textStyle.copy(fontStyle = FontStyle.Italic),
                modifier = activationModifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRect(
                            color = accentColor,
                            size = androidx.compose.ui.geometry.Size(
                                width = 2.dp.toPx(),
                                height = size.height,
                            ),
                        )
                    }
                    .padding(start = 16.dp),
            )
        }
    }
}

private fun PageFragment.originBlock(allBlocks: List<StudyBlock>): StudyBlock? =
    allBlocks.firstOrNull { it.id == originBlockId } ?: when (this) {
        is PageFragment.ParagraphSlice -> block
        is PageFragment.HeadingSlice -> block
        is PageFragment.ListItemSlice -> block
        is PageFragment.OrderedListItemSlice -> block
        is PageFragment.VerseSlice -> block
        is PageFragment.QuoteSlice -> block
        is PageFragment.Whole -> block
    }

private fun PageFragment.charRangeOrNull(): Pair<Int, Int>? = when (this) {
    is PageFragment.ParagraphSlice -> charStart to charEndExclusive
    is PageFragment.HeadingSlice -> charStart to charEndExclusive
    is PageFragment.ListItemSlice -> charStart to charEndExclusive
    is PageFragment.OrderedListItemSlice -> charStart to charEndExclusive
    is PageFragment.VerseSlice -> charStart to charEndExclusive
    is PageFragment.QuoteSlice -> charStart to charEndExclusive
    is PageFragment.Whole -> null
}

private fun buildTextStyle(block: StudyBlock, textColor: Color): TextStyle {
    val fontSizeSp = when (block) {
        is StudyBlock.Heading -> when (block.level) {
            1 -> 20.sp; 2 -> 18.sp; 3 -> 16.sp; else -> block.fontSize.sp
        }
        else -> block.fontSize.sp
    }
    return TextStyle(
        fontSize = fontSizeSp,
        fontWeight = if (block is StudyBlock.Heading) FontWeight.Bold else FontWeight.Normal,
        fontFamily = if (block is StudyBlock.Verse) FontFamily.Serif else FontFamily.Default,
        fontStyle = if (block is StudyBlock.Quote) FontStyle.Italic else FontStyle.Normal,
        textAlign = block.alignment.toTextAlign(),
        color = textColor,
        lineHeight = (block.fontSize * 1.5f).sp,
    )
}

internal fun calculateOrderedListNumber(blocks: List<StudyBlock>, index: Int): Int {
    var n = 1
    for (i in (index - 1) downTo 0) {
        if (blocks[i] is StudyBlock.OrderedList) n++ else return n
    }
    return n
}

internal fun BlockAlignment.toTextAlign(): TextAlign = when (this) {
    BlockAlignment.Start -> TextAlign.Start
    BlockAlignment.Center -> TextAlign.Center
    BlockAlignment.End -> TextAlign.End
    BlockAlignment.Justify -> TextAlign.Justify
}

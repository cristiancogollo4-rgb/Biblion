package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.debug.StudyEditorDebugLog
import com.cristiancogollo.biblion.feature.studydocs.domain.EditorFocusRequest
import com.cristiancogollo.biblion.feature.studydocs.domain.EditorTextKey
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PagedEditorUnit
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginationEngine
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
fun PagedEditorUnitRenderer(
    unit: PagedEditorUnit,
    allBlocks: List<StudyBlock>,
    richState: RichTextState,
    isActive: Boolean,
    focusRequest: EditorFocusRequest?,
    baseDensity: Density,
    splitViewModel: StudyDocSplitViewModel?,
    viewModel: StudyDocViewModel?,
    modifier: Modifier = Modifier,
) {
    val block = unit.block
    val itemIndex = unit.key.itemIndex
    val blockIndex = allBlocks.indexOfFirst { it.id == block.id }
    val editorKey = EditorTextKey(block.id, itemIndex)
    val focusRequester = remember(editorKey) { FocusRequester() }
    var hasPhysicalFocus by remember(editorKey) { mutableStateOf(false) }
    val currentText = StyledText.fromAnnotatedString(richState.annotatedString)
    var fieldValue by remember(editorKey) {
        mutableStateOf(
            TextFieldValue(
                annotatedString = AnnotatedString(currentText.raw),
                selection = richState.selection,
            ),
        )
    }

    LaunchedEffect(richState.annotatedString.text, richState.selection, hasPhysicalFocus) {
        val sourceText = richState.annotatedString.text
        when {
            fieldValue.text != sourceText -> {
                fieldValue = TextFieldValue(
                    annotatedString = AnnotatedString(sourceText),
                    selection = richState.selection.clamp(sourceText.length),
                )
            }
            !hasPhysicalFocus && fieldValue.selection != richState.selection -> {
                fieldValue = fieldValue.copy(
                    selection = richState.selection.clamp(sourceText.length),
                )
            }
        }
    }

    LaunchedEffect(richState, editorKey) {
        snapshotFlow { richState.annotatedString }
            .collect {
                splitViewModel?.onRichTextChanged(block.id, richState, itemIndex)
                    ?: viewModel?.onRichTextChanged(block.id, richState, itemIndex)
            }
    }

    LaunchedEffect(editorKey, isActive, focusRequest?.sequence) {
        if (isActive && (focusRequest == null || focusRequest.target == editorKey)) {
            requestFocusWithRetry(
                requester = focusRequester,
                target = "renderer=paged block=${block.id.value} item=$itemIndex",
            )
        }
    }

    val updateSelection: (TextRange) -> Unit = { selection ->
        splitViewModel?.updatePagedSelection(block.id, itemIndex, selection)
            ?: viewModel?.updatePagedSelection(block.id, itemIndex, selection)
    }
    val handleEnter: () -> Boolean = {
        updateSelection(fieldValue.selection)
        splitViewModel?.handleEnter(block.id)
            ?: viewModel?.handleEnter(block.id)
            ?: false
    }
    val keyModifier = Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        updateSelection(fieldValue.selection)
        when (event.key) {
            Key.Enter -> if (event.isShiftPressed) false else handleEnter()
            Key.Backspace -> {
                val isList = block is StudyBlock.BulletList || block is StudyBlock.OrderedList
                if (isList || (fieldValue.selection.collapsed && fieldValue.selection.start == 0)) {
                    splitViewModel?.handleBackspace(block.id)
                        ?: viewModel?.handleBackspace(block.id)
                        ?: false
                } else {
                    false
                }
            }
            Key.DirectionUp -> {
                if (!fieldValue.selection.collapsed || fieldValue.selection.start > 0) {
                    false
                } else if (itemIndex != null) {
                    splitViewModel?.moveCursorToPrevListItem(block.id, itemIndex)
                        ?: viewModel?.moveCursorToPrevListItem(block.id, itemIndex)
                        ?: false
                } else {
                    splitViewModel?.moveCursorToPrevBlock(block.id)
                        ?: viewModel?.moveCursorToPrevBlock(block.id)
                        ?: false
                }
            }
            Key.DirectionDown -> {
                if (!fieldValue.selection.collapsed ||
                    fieldValue.selection.start < fieldValue.text.length
                ) {
                    false
                } else if (itemIndex != null) {
                    splitViewModel?.moveCursorToNextListItem(block.id, itemIndex)
                        ?: viewModel?.moveCursorToNextListItem(block.id, itemIndex)
                        ?: false
                } else {
                    splitViewModel?.moveCursorToNextBlock(block.id)
                        ?: viewModel?.moveCursorToNextBlock(block.id)
                        ?: false
                }
            }
            else -> false
        }
    }

    val textStyle = PaginationEngine.textStyleFor(block, baseDensity).copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    val visualTransformation = remember(currentText, unit.gaps, baseDensity) {
        PageGapVisualTransformation(
            styledText = currentText,
            gaps = unit.gaps,
            pxToSp = { px -> with(baseDensity) { px.toSp() } },
        )
    }
    val imeAction = if (itemIndex != null) ImeAction.Next else ImeAction.Done
    val editor: @Composable (Modifier) -> Unit = { editorModifier ->
        BasicTextField(
            value = fieldValue,
            onValueChange = { nextValue ->
                val previousValue = fieldValue
                fieldValue = nextValue
                if (nextValue.text != previousValue.text) {
                    splitViewModel?.replacePagedText(
                        block.id,
                        itemIndex,
                        nextValue.text,
                        nextValue.selection,
                    ) ?: viewModel?.replacePagedText(
                        block.id,
                        itemIndex,
                        nextValue.text,
                        nextValue.selection,
                    )
                } else if (nextValue.selection != previousValue.selection) {
                    updateSelection(nextValue.selection)
                }
            },
            modifier = editorModifier
                .then(keyModifier)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    hasPhysicalFocus = focusState.hasFocus
                    if (focusState.hasFocus) {
                        splitViewModel?.onEditorFocused(block.id, itemIndex)
                            ?: viewModel?.onEditorFocused(block.id, itemIndex)
                    }
                    StudyEditorDebugLog.log(
                        "FOCUS_CHANGED",
                        "renderer=paged block=${block.id.value} item=$itemIndex " +
                            "focused=${focusState.isFocused} hasFocus=${focusState.hasFocus}",
                    )
                },
            textStyle = textStyle,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = false,
            onTextLayout = { result ->
                val gapLayout = unit.gaps.joinToString(";") { gap ->
                    val markerOffset = gap.offset.coerceIn(0, result.layoutInput.text.length)
                    val spacerOffset = (markerOffset + 1)
                        .coerceIn(0, result.layoutInput.text.length)
                    val continuationOffset = (markerOffset + 3)
                        .coerceIn(0, result.layoutInput.text.length)
                    val markerLine = result.getLineForOffset(markerOffset)
                    val spacerLine = result.getLineForOffset(spacerOffset)
                    val continuationLine = result.getLineForOffset(continuationOffset)
                    "offset=${gap.offset} expected=${gap.startPx}..${gap.startPx + gap.heightPx} " +
                        "actual=${result.getLineTop(markerLine)}:" +
                        "${result.getLineTop(spacerLine)}.." +
                        "${result.getLineTop(continuationLine)}"
                }
                StudyEditorDebugLog.log(
                    "PAGED_FIELD_LAYOUT",
                    "block=${block.id.value} item=$itemIndex length=${fieldValue.text.length} " +
                        "layout=${result.size.width}x${result.size.height} lines=${result.lineCount} " +
                        "unitHeightPx=${unit.heightPx} gaps=[$gapLayout]",
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onDone = { handleEnter() },
                onNext = { handleEnter() },
            ),
        )
    }

    when (block) {
        is StudyBlock.BulletList,
        is StudyBlock.OrderedList,
        -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            val marker = if (block is StudyBlock.BulletList) {
                "\u2022"
            } else {
                "${calculateOrderedListNumber(allBlocks, blockIndex) + (itemIndex ?: 0)}."
            }
            Text(
                text = marker,
                style = textStyle,
                modifier = Modifier.padding(top = 2.dp, end = 8.dp),
            )
            editor(Modifier.weight(1f))
        }
        is StudyBlock.Quote -> {
            val accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            editor(
                modifier
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
        else -> editor(modifier.fillMaxWidth())
    }
}

private fun TextRange.clamp(length: Int): TextRange = TextRange(
    start = start.coerceIn(0, length),
    end = end.coerceIn(0, length),
)

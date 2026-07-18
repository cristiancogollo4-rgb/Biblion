package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

@Composable
fun StudyModeBlockRenderer(
    block: StudyBlock,
    blockIndex: Int,
    richState: RichTextState,
    isActive: Boolean,
    focusRequesters: SnapshotStateMap<BlockId, FocusRequester>,
    splitViewModel: StudyDocSplitViewModel?,
    viewModel: StudyDocViewModel?,
    allBlocks: List<StudyBlock>,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(block.id) { focusRequesters[block.id] = focusRequester }

    LaunchedEffect(isActive) {
        if (isActive) focusRequester.requestFocus()
    }

    val textStyle = remember(block, block.fontSize) {
        TextStyle(
            fontSize = when (block) {
                is StudyBlock.Heading -> when (block.level) {
                    1 -> 20.sp; 2 -> 18.sp; 3 -> 16.sp; else -> block.fontSize.sp
                }
                else -> block.fontSize.sp
            },
            fontWeight = if (block is StudyBlock.Heading) FontWeight.Bold else FontWeight.Normal,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            textAlign = block.alignment.toTextAlign(),
            color = Color(0xFF0F172A),
            lineHeight = (block.fontSize * 1.5).sp,
        )
    }

    LaunchedEffect(richState) {
        snapshotFlow { richState.selection }
            .collect {
                if (isActive) {
                    splitViewModel?.syncActiveFormat(richState)
                        ?: viewModel?.syncActiveFormat(richState)
                }
            }
    }

    val keyModifier = Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        if (event.isShiftPressed) return@onPreviewKeyEvent false
        val sel = richState.selection
        when (event.key) {
            Key.Enter -> {
                splitViewModel?.handleEnter(block.id)
                    ?: viewModel?.handleEnter(block.id)
                true
            }
            Key.Backspace -> {
                if (sel.collapsed && sel.start == 0) {
                    splitViewModel?.handleBackspace(block.id)
                        ?: viewModel?.handleBackspace(block.id)
                    true
                } else false
            }
            else -> false
        }
    }

    when (block) {
        is StudyBlock.BulletList -> {
            Log.d("LIST_DEBUG", "Renderer: BulletList id=${block.id.value} fontSize=${block.fontSize}")
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .then(keyModifier)
                    .focusRequester(focusRequester)
                    .onFocusChanged { fs ->
                        if (fs.isFocused) {
                            splitViewModel?.setActiveBlock(block.id)
                                ?: viewModel?.setActiveBlock(block.id)
                        }
                    },
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "\u2022",
                    style = textStyle,
                    modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                )
                BasicRichTextEditor(
                    state = richState,
                    modifier = Modifier.weight(1f),
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
        }
        is StudyBlock.OrderedList -> {
            val number = calculateOrderedListNumber(allBlocks, blockIndex)
            Log.d("LIST_DEBUG", "Renderer: OrderedList id=${block.id.value} number=$number blockIndex=$blockIndex")
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .then(keyModifier)
                    .focusRequester(focusRequester)
                    .onFocusChanged { fs ->
                        if (fs.isFocused) {
                            splitViewModel?.setActiveBlock(block.id)
                                ?: viewModel?.setActiveBlock(block.id)
                        }
                    },
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "$number.",
                    style = textStyle,
                    modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                )
                BasicRichTextEditor(
                    state = richState,
                    modifier = Modifier.weight(1f),
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
        }
        else -> {
            BasicRichTextEditor(
                state = richState,
                modifier = modifier
                    .fillMaxWidth()
                    .then(keyModifier)
                    .focusRequester(focusRequester)
                    .onFocusChanged { fs ->
                        if (fs.isFocused) {
                            splitViewModel?.setActiveBlock(block.id)
                                ?: viewModel?.setActiveBlock(block.id)
                        }
                    },
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private fun calculateOrderedListNumber(blocks: List<StudyBlock>, index: Int): Int {
    var number = 1
    for (i in (index - 1) downTo 0) {
        when (blocks[i]) {
            is StudyBlock.OrderedList -> number++
            else -> return number
        }
    }
    return number
}

private fun com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.toTextAlign(): TextAlign = when (this) {
    com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Start -> TextAlign.Left
    com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Center -> TextAlign.Center
    com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.End -> TextAlign.Right
    com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Justify -> TextAlign.Justify
}

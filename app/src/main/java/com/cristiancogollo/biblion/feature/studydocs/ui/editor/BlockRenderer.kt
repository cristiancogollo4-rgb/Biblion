package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

@Composable
fun BlockRenderer(
    block: StudyBlock,
    blockIndex: Int,
    richState: RichTextState?,
    isActive: Boolean,
    focusRequesters: SnapshotStateMap<BlockId, FocusRequester>,
    splitViewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel? = null,
    viewModel: StudyDocViewModel? = null,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(block.id) { focusRequesters[block.id] = focusRequester }

    LaunchedEffect(isActive) {
        if (isActive) focusRequester.requestFocus()
    }

    val textStyle = resolveBlockStyle(block, block.fontSize)

    LaunchedEffect(richState != null) {
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

    val keyModifier = if (richState != null) {
        Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            if (event.isShiftPressed) return@onPreviewKeyEvent false
            val sel = richState.selection
            when (event.key) {
                Key.Enter -> {
                    splitViewModel?.handleEnter(block.id) ?: viewModel?.handleEnter(block.id)
                    true
                }
                Key.Backspace -> {
                    if (sel.collapsed && sel.start == 0) {
                        splitViewModel?.handleBackspace(block.id) ?: viewModel?.handleBackspace(block.id)
                        true
                    } else false
                }
                Key.DirectionUp -> {
                    if (sel.collapsed && sel.start == 0 && blockIndex > 0) {
                        splitViewModel?.moveCursorToPrevBlock(block.id)
                            ?: viewModel?.moveCursorToPrevBlock(block.id)
                        true
                    } else false
                }
                Key.DirectionDown -> {
                    val text = richState.annotatedString.text
                    val totalBlocks = (splitViewModel?.editorState?.value?.doc?.blocks?.size
                        ?: viewModel?.uiState?.value?.doc?.blocks?.size) ?: 0
                    if (sel.collapsed && sel.start >= text.length && blockIndex < totalBlocks - 1) {
                        splitViewModel?.moveCursorToNextBlock(block.id)
                            ?: viewModel?.moveCursorToNextBlock(block.id)
                        true
                    } else false
                }
                else -> false
            }
        }
    } else Modifier

    when (block) {
        is StudyBlock.Verse -> {
            BibleVerseBlock(
                block = block,
                modifier = modifier.fillMaxWidth(),
            )
        }
        is StudyBlock.Paragraph,
        is StudyBlock.Heading,
        is StudyBlock.Quote -> {
            Box(modifier = modifier.fillMaxWidth().then(keyModifier)) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp)
                            .width(3.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                BasicRichTextEditor(
                    state = richState!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { fs ->
                            if (fs.isFocused) {
                                splitViewModel?.setActiveBlock(block.id)
                                    ?: viewModel?.setActiveBlock(block.id)
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 16.dp),
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
        }
        is StudyBlock.BulletList -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { fs ->
                        if (fs.isFocused) {
                            splitViewModel?.setActiveBlock(block.id)
                                ?: viewModel?.setActiveBlock(block.id)
                        }
                    }
                    .padding(vertical = 4.dp, horizontal = 16.dp),
            ) {
                block.items.forEachIndexed { _, _ ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("\u2022", style = textStyle)
                        BasicRichTextEditor(
                            state = richState!!,
                            modifier = Modifier.weight(1f),
                            textStyle = textStyle,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
        is StudyBlock.OrderedList -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { fs ->
                        if (fs.isFocused) {
                            splitViewModel?.setActiveBlock(block.id)
                                ?: viewModel?.setActiveBlock(block.id)
                        }
                    }
                    .padding(vertical = 4.dp, horizontal = 16.dp),
            ) {
                block.items.forEachIndexed { index, _ ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("${index + 1}.", style = textStyle)
                        BasicRichTextEditor(
                            state = richState!!,
                            modifier = Modifier.weight(1f),
                            textStyle = textStyle,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun resolveBlockStyle(block: StudyBlock, fontSize: Int): TextStyle {
    val baseStyle = MaterialTheme.typography.bodyLarge
    val size = when (block) {
        is StudyBlock.Heading -> when (block.level) {
            1 -> 32; 2 -> 24; 3 -> 20; else -> fontSize
        }
        else -> fontSize
    }
    val family = when (block.fontFamily) {
        "serif" -> FontFamily.Serif
        "sans" -> FontFamily.SansSerif
        "monospace" -> FontFamily.Monospace
        "default" -> FontFamily.Default
        null -> null
        else -> null
    }
    return baseStyle.copy(
        fontSize = size.sp,
        fontWeight = if (block is StudyBlock.Heading)
            androidx.compose.ui.text.font.FontWeight.Bold
        else baseStyle.fontWeight,
        fontFamily = family ?: baseStyle.fontFamily,
    )
}

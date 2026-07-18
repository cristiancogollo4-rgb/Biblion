package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
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

/**
 * Renderizador de bloques para el modo estudio (lienzo de papel).
 * Diseño limpio y enfocado.
 */
@Composable
fun StudyModeBlockRenderer(
    block: StudyBlock,
    blockIndex: Int,
    richState: RichTextState,
    isActive: Boolean,
    focusRequesters: SnapshotStateMap<BlockId, FocusRequester>,
    splitViewModel: StudyDocSplitViewModel?,
    viewModel: StudyDocViewModel?,
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

    BasicRichTextEditor(
        state = richState,
        modifier = modifier
            .fillMaxWidth()
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

private fun com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.toTextAlign(): TextAlign = when (this) {
    com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Start -> TextAlign.Left
    com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Center -> TextAlign.Center
    com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.End -> TextAlign.Right
    com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Justify -> TextAlign.Justify
}

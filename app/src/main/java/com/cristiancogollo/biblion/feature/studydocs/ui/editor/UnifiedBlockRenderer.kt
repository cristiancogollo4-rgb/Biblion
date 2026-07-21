package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
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
    focusRequesters: SnapshotStateMap<BlockId, FocusRequester>? = null,
    splitViewModel: StudyDocSplitViewModel? = null,
    viewModel: StudyDocViewModel? = null,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val textStyle = remember(block, block.fontSize, isEditing) { buildTextStyle(block, textColor) }

    if (isEditing) {
        Log.d("BIBLION_STUDY", "UnifiedBlockRenderer(block) EDITING blockId=${block.id} richState=${richState != null} text=[${richState?.annotatedString?.text?.take(40)}]")
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(block.id) { focusRequesters?.set(block.id, focusRequester) }
        LaunchedEffect(isActive) { if (isActive) focusRequester.requestFocus() }

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
                    else -> false
                }
            }
        } else Modifier

        when (block) {
            is StudyBlock.Verse -> BibleVerseBlock(block = block, modifier = modifier.fillMaxWidth())
            is StudyBlock.BulletList -> {
                Row(modifier = modifier.fillMaxWidth().then(keyModifier).focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) splitViewModel?.setActiveBlock(block.id) ?: viewModel?.setActiveBlock(block.id) },
                    verticalAlignment = Alignment.Top) {
                    Text("\u2022", style = textStyle, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
                    BasicRichTextEditor(state = richState!!, modifier = Modifier.weight(1f), textStyle = textStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary))
                }
            }
            is StudyBlock.OrderedList -> {
                val num = calculateOrderedListNumber(allBlocks, blockIndex)
                Row(modifier = modifier.fillMaxWidth().then(keyModifier).focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) splitViewModel?.setActiveBlock(block.id) ?: viewModel?.setActiveBlock(block.id) },
                    verticalAlignment = Alignment.Top) {
                    Text("$num.", style = textStyle, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
                    BasicRichTextEditor(state = richState!!, modifier = Modifier.weight(1f), textStyle = textStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary))
                }
            }
            else -> {
                BasicRichTextEditor(state = richState!!, modifier = modifier.fillMaxWidth().then(keyModifier).focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) splitViewModel?.setActiveBlock(block.id) ?: viewModel?.setActiveBlock(block.id) },
                    textStyle = textStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary))
            }
        }
    } else {
        when (block) {
            is StudyBlock.Verse -> BibleVerseBlock(block = block, modifier = modifier.fillMaxWidth())
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
    focusRequesters: SnapshotStateMap<BlockId, FocusRequester>? = null,
    splitViewModel: StudyDocSplitViewModel? = null,
    viewModel: StudyDocViewModel? = null,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val block = fragment.originBlock(allBlocks)
    if (block == null) return
    Log.d("BIBLION_STUDY", "UnifiedBlockRenderer fragment=$fragment isEditing=$isEditing isOwnerFragment=$isOwnerFragment richState=${richState != null} blockId=${fragment.originBlockId} blockType=${block::class.simpleName}")
    val blockIndex = allBlocks.indexOfFirst { it.id == fragment.originBlockId }
    val textStyle = remember(block, block.fontSize, isEditing) { buildTextStyle(block, textColor) }

    if (isEditing && isOwnerFragment && richState != null) {
        UnifiedBlockRenderer(
            block = block,
            blockIndex = blockIndex,
            richState = richState,
            isActive = isActive,
            isEditing = true,
            allBlocks = allBlocks,
            focusRequesters = focusRequesters,
            splitViewModel = splitViewModel,
            viewModel = viewModel,
            modifier = modifier,
        )
        return
    }
    renderFragmentReadOnly(fragment, block, blockIndex, textStyle, allBlocks, modifier)
}

@Composable
private fun renderFragmentReadOnly(
    fragment: PageFragment,
    block: StudyBlock,
    blockIndex: Int,
    textStyle: TextStyle,
    allBlocks: List<StudyBlock>,
    modifier: Modifier,
) {
    when (fragment) {
        is PageFragment.Whole -> {
            UnifiedBlockRenderer(
                block = block,
                blockIndex = blockIndex,
                richState = null,
                isActive = false,
                isEditing = false,
                allBlocks = allBlocks,
                focusRequesters = null,
                splitViewModel = null,
                viewModel = null,
                modifier = modifier,
            )
        }
        is PageFragment.ParagraphSlice -> {
            val sliced = fragment.block.text.slice(fragment.charStart until fragment.charEndExclusive)
            Text(
                text = sliced.toAnnotatedString(),
                style = textStyle,
                modifier = modifier.fillMaxWidth(),
            )
        }
        is PageFragment.HeadingSlice -> {
            val sliced = fragment.block.text.slice(fragment.charStart until fragment.charEndExclusive)
            Text(
                text = sliced.toAnnotatedString(),
                style = textStyle,
                modifier = modifier.fillMaxWidth(),
            )
        }
        is PageFragment.ListItemSlice -> {
            val item = fragment.block.items.getOrNull(fragment.itemIndex) ?: return
            val sliced = item.slice(fragment.charStart until fragment.charEndExclusive)
            Row(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text("\u2022  ", style = textStyle)
                Text(text = sliced.toAnnotatedString(), style = textStyle)
            }
        }
        is PageFragment.OrderedListItemSlice -> {
            val item = fragment.block.items.getOrNull(fragment.itemIndex) ?: return
            val sliced = item.slice(fragment.charStart until fragment.charEndExclusive)
            val startNum = calculateOrderedListNumber(allBlocks, blockIndex)
            Row(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text("${startNum + fragment.itemIndex}. ", style = textStyle)
                Text(text = sliced.toAnnotatedString(), style = textStyle)
            }
        }
        is PageFragment.VerseSlice -> {
            val original = fragment.block.contents[fragment.block.sourceVersion] ?: ""
            val recorte = original.substring(
                fragment.charStart.coerceAtLeast(0),
                fragment.charEndExclusive.coerceAtMost(original.length),
            )
            Text(
                text = recorte,
                style = textStyle.copy(fontFamily = FontFamily.Serif),
                modifier = modifier.fillMaxWidth(),
            )
        }
        is PageFragment.QuoteSlice -> {
            val sliced = fragment.block.text.slice(
                fragment.charStart until fragment.charEndExclusive
            )
            Column(modifier = modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = sliced.toAnnotatedString(),
                    style = textStyle.copy(fontStyle = FontStyle.Italic),
                    modifier = Modifier.padding(start = 16.dp),
                )
                if (fragment.charEndExclusive >= fragment.block.text.length &&
                    !fragment.block.attribution.isNullOrBlank()
                ) {
                    Text(
                        text = "\u2014 ${fragment.block.attribution}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
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
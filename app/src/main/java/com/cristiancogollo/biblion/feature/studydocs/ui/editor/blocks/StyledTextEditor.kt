package com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.engine.BlockMarkdownShortcuts
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.TextStylePatch
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.fromAnnotatedString
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.richTextSelectionRangeOrNull
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.syncFromStyledText
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

@Composable
fun StyledTextEditor(
    text: StyledText,
    onTextChange: (StyledText) -> Unit,
    onSelectionChange: (IntRange?) -> Unit = {},
    onShortcutDetected: ((StudyBlock) -> Unit)? = null,
    onFieldValueChange: (TextFieldValue) -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    baseStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    requestFocus: Boolean = false,
    blockId: String? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = DocConfig.FontSize,
) {
    val richState = remember(blockId) { RichTextState() }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val resolvedStyle = baseStyle.copy(
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = DocConfig.LineHeight,
        fontSize = baseStyle.fontSize.takeIf { it != androidx.compose.ui.unit.TextUnit.Unspecified } ?: fontSize,
    )

    LaunchedEffect(text, blockId) {
        richState.syncFromStyledText(text)
    }

    LaunchedEffect(richState.annotatedString, blockId) {
        val currentStyled = StyledText.fromAnnotatedString(richState.annotatedString)
        if (currentStyled != text) {
            val shortcutId = blockId
            if (shortcutId != null && onShortcutDetected != null) {
                val match = BlockMarkdownShortcuts.detect(
                    originalId = shortcutId,
                    text = StyledText.plain(currentStyled.plain()),
                )
                if (match != null) {
                    onShortcutDetected(match.block)
                    return@LaunchedEffect
                }
            }
            if (currentStyled.plain().length > text.plain().length) {
                val cursorPos = richState.selection.min
                if (cursorPos in 1..currentStyled.plain().length) {
                    val prevStyle = text.styleAt(cursorPos - 1)
                    if (prevStyle != null && !prevStyle.isEmpty) {
                        val patch = TextStylePatch(
                            bold = prevStyle.bold.takeIf { it },
                            italic = prevStyle.italic.takeIf { it },
                            underline = prevStyle.underline.takeIf { it },
                            strikethrough = prevStyle.strikethrough.takeIf { it },
                            color = prevStyle.color,
                            background = prevStyle.background,
                        )
                        val inherited = currentStyled.withStyle(
                            (cursorPos - 1) until cursorPos,
                            patch,
                        )
                        onTextChange(inherited)
                        return@LaunchedEffect
                    }
                }
            }
            onTextChange(currentStyled)
        }
    }

    LaunchedEffect(richState.selection, blockId) {
        val selectionRange = richTextSelectionRangeOrNull(
            richState.selection.min,
            richState.selection.max,
        )
        onSelectionChange(selectionRange)
        onFieldValueChange(
            TextFieldValue(
                annotatedString = richState.annotatedString,
                selection = TextRange(richState.selection.min, richState.selection.max),
            ),
        )
    }

    LaunchedEffect(isSelected, requestFocus) {
        if (isSelected && requestFocus) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(0.dp),
    ) {
        if (isFocused && richState.annotatedString.text.isBlank() && placeholder.isNotBlank()) {
            Text(
                text = placeholder,
                style = resolvedStyle.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                ),
            )
        }

        BasicRichTextEditor(
            state = richState,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    isFocused = it.isFocused
                    onFocusChanged(it.isFocused)
                },
            textStyle = resolvedStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        )
    }
}

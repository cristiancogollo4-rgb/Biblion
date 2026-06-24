package com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.engine.BlockMarkdownShortcuts
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.StyledTextRenderer

@Composable
fun StyledTextEditor(
    text: StyledText,
    onTextChange: (StyledText) -> Unit,
    onSelectionChange: (IntRange?) -> Unit = {},
    onShortcutDetected: ((StudyBlock) -> Unit)? = null,
    onFieldValueChange: (TextFieldValue) -> Unit = {},
    baseStyle: TextStyle = LocalTextStyle.current,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    requestFocus: Boolean = false,
    blockId: String? = null,
) {
    val annotated = remember(text) { StyledTextRenderer.toAnnotatedString(text, Color.Unspecified) }
    val focusRequester = remember { FocusRequester() }

    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                annotatedString = annotated,
                selection = TextRange(annotated.length),
            )
        )
    }

    LaunchedEffect(text) {
        if (fieldValue.text != text.plain()) {
            fieldValue = TextFieldValue(
                annotatedString = annotated,
                selection = TextRange(annotated.length),
            )
        }
        onFieldValueChange(fieldValue)
    }

    LaunchedEffect(isSelected, requestFocus) {
        if (isSelected && requestFocus) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .focusRequester(focusRequester),
    ) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val newRaw = newValue.text
                if (newRaw != text.plain()) {
                    val shortcutId = blockId
                    if (shortcutId != null && onShortcutDetected != null) {
                        val match = BlockMarkdownShortcuts.detect(
                            originalId = shortcutId,
                            text = StyledText.plain(newRaw),
                        )
                        if (match != null) {
                            onShortcutDetected(match.block)
                            fieldValue = TextFieldValue(
                                annotatedString = androidx.compose.ui.text.AnnotatedString(
                                    when (match.block) {
                                        is StudyBlock.Heading -> match.block.text.plain()
                                        is StudyBlock.BulletList -> match.block.items.firstOrNull()?.plain().orEmpty()
                                        is StudyBlock.NumberedList -> match.block.items.firstOrNull()?.plain().orEmpty()
                                        is StudyBlock.Quote -> match.block.text.plain()
                                        is StudyBlock.Reflection -> match.block.text.plain()
                                        is StudyBlock.Note -> match.block.text.plain()
                                        else -> ""
                                    }
                                ),
                                selection = TextRange(0),
                            )
                            onFieldValueChange(fieldValue)
                            return@BasicTextField
                        }
                    }
                    onTextChange(StyledText.plain(newRaw))
                }
                fieldValue = newValue
                onFieldValueChange(newValue)
            },
            textStyle = baseStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = @Composable { inner ->
                if (text.isBlank && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = baseStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        ),
                    )
                }
                inner()
            },
        )
    }
}

package com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig

@Composable
fun ParagraphBlockEditor(
    block: StudyBlock.Paragraph,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectionChange: (IntRange?) -> Unit = {},
    onTextChange: (StyledText) -> Unit,
    onShortcutDetected: ((StudyBlock) -> Unit)? = null,
    onFieldValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = DocConfig.FontSize,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StyledTextEditor(
            text = block.text,
            onTextChange = onTextChange,
            onSelectionChange = onSelectionChange,
            onShortcutDetected = onShortcutDetected,
            onFieldValueChange = onFieldValueChange,
            onFocusChanged = onFocusChanged,
            baseStyle = if (textAlign != null) MaterialTheme.typography.bodyLarge.copy(textAlign = textAlign) else MaterialTheme.typography.bodyLarge,
            placeholder = "Escribe algo...",
            isSelected = isSelected,
            requestFocus = isSelected,
            blockId = block.id.value,
            fontSize = fontSize,
        )
    }
}

@Composable
fun HeadingBlockEditor(
    block: StudyBlock.Heading,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectionChange: (IntRange?) -> Unit = {},
    onTextChange: (StyledText) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = DocConfig.FontSize,
) {
    val size = when (block.level) {
        1 -> DocConfig.Heading1Size
        2 -> DocConfig.Heading2Size
        3 -> DocConfig.Heading3Size
        else -> fontSize
    }
    Column(modifier = modifier.fillMaxWidth()) {
        StyledTextEditor(
            text = block.text,
            onTextChange = onTextChange,
            onSelectionChange = onSelectionChange,
            onFocusChanged = onFocusChanged,
            baseStyle = if (textAlign != null) MaterialTheme.typography.headlineSmall.copy(
                fontSize = size,
                fontWeight = FontWeight.Bold,
                textAlign = textAlign,
            ) else MaterialTheme.typography.headlineSmall.copy(
                fontSize = size,
                fontWeight = FontWeight.Bold,
            ),
            placeholder = "Encabezado",
            isSelected = isSelected,
            requestFocus = isSelected,
            blockId = block.id.value,
            fontSize = fontSize,
        )
    }
}

@Composable
fun QuoteBlockEditor(
    block: StudyBlock.Quote,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectionChange: (IntRange?) -> Unit = {},
    onTextChange: (StyledText) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = DocConfig.FontSize,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "\u201C", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        StyledTextEditor(
            text = block.text,
            onTextChange = onTextChange,
            onSelectionChange = onSelectionChange,
            onFocusChanged = onFocusChanged,
            baseStyle = if (textAlign != null) MaterialTheme.typography.bodyLarge.copy(textAlign = textAlign) else MaterialTheme.typography.bodyLarge,
            placeholder = "Cita...",
            isSelected = isSelected,
            requestFocus = isSelected,
            blockId = block.id.value,
            fontSize = fontSize,
        )
        if (!block.attribution.isNullOrBlank()) {
            Text(
                text = "\u2014 ${block.attribution}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun BulletListBlockEditor(
    block: StudyBlock.BulletList,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectionChange: (IntRange?) -> Unit = {},
    onItemsChange: (List<StyledText>) -> Unit,
    onAppendItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onItemChange: (Int, StyledText) -> Unit,
    onItemFieldValueChange: (Int, TextFieldValue) -> Unit = { _, _ -> },
    onItemFocusChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = DocConfig.FontSize,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("\u2022  ", style = MaterialTheme.typography.bodyLarge)
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                    StyledTextEditor(
                        text = item,
                        onTextChange = { onItemChange(index, it) },
                        onSelectionChange = onSelectionChange,
                        onFieldValueChange = { onItemFieldValueChange(index, it) },
                        onFocusChanged = { if (it) onItemFocusChanged(index) },
                        baseStyle = MaterialTheme.typography.bodyLarge,
                        placeholder = "Item...",
                        isSelected = isSelected,
                        blockId = "${block.id.value}:item:$index",
                        fontSize = fontSize,
                    )
                }
                androidx.compose.material3.IconButton(onClick = { onRemoveItem(index) }) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Filled.Clear,
                        contentDescription = "Quitar",
                    )
                }
            }
        }
        androidx.compose.material3.TextButton(onClick = onAppendItem) {
            Text("+ Item")
        }
    }
}

@Composable
fun NumberedListBlockEditor(
    block: StudyBlock.NumberedList,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectionChange: (IntRange?) -> Unit = {},
    onAppendItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onItemChange: (Int, StyledText) -> Unit,
    onItemFieldValueChange: (Int, TextFieldValue) -> Unit = { _, _ -> },
    onItemFocusChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = DocConfig.FontSize,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("${index + 1}. ", style = MaterialTheme.typography.bodyLarge)
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                    StyledTextEditor(
                        text = item,
                        onTextChange = { onItemChange(index, it) },
                        onSelectionChange = onSelectionChange,
                        onFieldValueChange = { onItemFieldValueChange(index, it) },
                        onFocusChanged = { if (it) onItemFocusChanged(index) },
                        baseStyle = MaterialTheme.typography.bodyLarge,
                        placeholder = "Item...",
                        isSelected = isSelected,
                        blockId = "${block.id.value}:item:$index",
                        fontSize = fontSize,
                    )
                }
                androidx.compose.material3.IconButton(onClick = { onRemoveItem(index) }) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Filled.Clear,
                        contentDescription = "Quitar",
                    )
                }
            }
        }
        androidx.compose.material3.TextButton(onClick = onAppendItem) {
            Text("+ Item")
        }
    }
}

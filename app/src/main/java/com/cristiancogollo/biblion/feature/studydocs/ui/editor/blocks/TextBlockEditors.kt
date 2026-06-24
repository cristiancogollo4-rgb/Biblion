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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

@Composable
fun ParagraphBlockEditor(
    block: StudyBlock.Paragraph,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectionChange: (IntRange?) -> Unit = {},
    onTextChange: (StyledText) -> Unit,
    onShortcutDetected: ((StudyBlock) -> Unit)? = null,
    onFieldValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit = {},
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        StyledTextEditor(
            text = block.text,
            onTextChange = onTextChange,
            onSelectionChange = onSelectionChange,
            onShortcutDetected = onShortcutDetected,
            onFieldValueChange = onFieldValueChange,
            baseStyle = if (textAlign != null) MaterialTheme.typography.bodyLarge.copy(textAlign = textAlign) else MaterialTheme.typography.bodyLarge,
            placeholder = "Escribe algo...",
            isSelected = isSelected,
            blockId = block.id.value,
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
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    val size = when (block.level) {
        1 -> 28.sp
        2 -> 24.sp
        3 -> 20.sp
        else -> 18.sp
    }
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        StyledTextEditor(
            text = block.text,
            onTextChange = onTextChange,
            onSelectionChange = onSelectionChange,
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
            blockId = block.id.value,
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
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 8.dp)) {
        Text(text = "\u201C", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        StyledTextEditor(
            text = block.text,
            onTextChange = onTextChange,
            onSelectionChange = onSelectionChange,
            baseStyle = if (textAlign != null) MaterialTheme.typography.bodyLarge.copy(textAlign = textAlign) else MaterialTheme.typography.bodyLarge,
            placeholder = "Cita...",
            isSelected = isSelected,
            blockId = block.id.value,
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("\u2022  ", style = MaterialTheme.typography.bodyLarge)
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                    StyledTextEditor(
                        text = item,
                        onTextChange = { onItemChange(index, it) },
                        onSelectionChange = onSelectionChange,
                        baseStyle = MaterialTheme.typography.bodyLarge,
                        placeholder = "Item...",
                        isSelected = isSelected,
                        blockId = block.id.value,
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("${index + 1}. ", style = MaterialTheme.typography.bodyLarge)
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                    StyledTextEditor(
                        text = item,
                        onTextChange = { onItemChange(index, it) },
                        onSelectionChange = onSelectionChange,
                        baseStyle = MaterialTheme.typography.bodyLarge,
                        placeholder = "Item...",
                        isSelected = isSelected,
                        blockId = block.id.value,
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

package com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

@Composable
fun NoteBlockEditor(
    block: StudyBlock.Note,
    isSelected: Boolean,
    onSelectionChange: (IntRange?) -> Unit = {},
    onTextChange: (StyledText) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        Column {
            Text("Nota", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StyledTextEditor(
                text = block.text,
                onTextChange = onTextChange,
                onSelectionChange = onSelectionChange,
                baseStyle = MaterialTheme.typography.bodyMedium,
                placeholder = "Nota...",
                isSelected = isSelected,
                blockId = block.id.value,
            )
        }
    }
}

@Composable
fun ReflectionBlockEditor(
    block: StudyBlock.Reflection,
    isSelected: Boolean,
    onSelectionChange: (IntRange?) -> Unit = {},
    onTextChange: (StyledText) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        Column {
            if (!block.prompt.isNullOrBlank()) {
                Text(block.prompt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            StyledTextEditor(
                text = block.text,
                onTextChange = onTextChange,
                onSelectionChange = onSelectionChange,
                baseStyle = MaterialTheme.typography.bodyMedium,
                placeholder = "Reflexion...",
                isSelected = isSelected,
                blockId = block.id.value,
            )
        }
    }
}

@Composable
fun CalloutBlockEditor(
    block: StudyBlock.Callout,
    isSelected: Boolean,
    onSelectionChange: (IntRange?) -> Unit = {},
    onTextChange: (StyledText) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        Column {
            Text("! Destacado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StyledTextEditor(
                text = block.text,
                onTextChange = onTextChange,
                onSelectionChange = onSelectionChange,
                baseStyle = MaterialTheme.typography.bodyMedium,
                placeholder = "Destacado...",
                isSelected = isSelected,
                blockId = block.id.value,
            )
        }
    }
}

@Composable
fun VerseBlockEditor(
    block: StudyBlock.Verse,
    isSelected: Boolean,
    onSelectionChange: (IntRange?) -> Unit = {},
    onTextChange: (StyledText) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            block.reference.displayShort(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        StyledTextEditor(
            text = block.primaryText,
            onTextChange = onTextChange,
            onSelectionChange = onSelectionChange,
            baseStyle = MaterialTheme.typography.bodyMedium,
            placeholder = "Texto del versiculo...",
            isSelected = isSelected,
            blockId = block.id.value,
        )
        if (block.compareText != null && block.compareVersion != null) {
            Text(
                "${block.compareVersion}:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                block.compareText.plain(),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
fun DividerBlockView(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        thickness = 2.dp,
    )
}

@Composable
fun PageBreakBlockView(modifier: Modifier = Modifier) {
    Text(
        "--- Salto de pagina ---",
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun TableBlockEditor(
    block: StudyBlock.Table,
    isSelected: Boolean,
    onAddRow: () -> Unit,
    onAddCol: () -> Unit,
    onHeaderToggle: () -> Unit,
    onCellChange: (Int, Int, StyledText) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.TextButton(onClick = onHeaderToggle) {
                Text(if (block.hasHeaderRow) "Header ON" else "Header OFF")
            }
            androidx.compose.material3.TextButton(onClick = onAddRow) { Text("+ fila") }
            androidx.compose.material3.TextButton(onClick = onAddCol) { Text("+ col") }
        }
        block.rows.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.cells.forEachIndexed { colIndex, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .padding(2.dp),
                    ) {
                        StyledTextEditor(
                            text = cell,
                            onTextChange = { onCellChange(rowIndex, colIndex, it) },
                            baseStyle = MaterialTheme.typography.bodySmall,
                            isSelected = isSelected,
                            blockId = block.id.value,
                        )
                    }
                }
            }
        }
    }
}

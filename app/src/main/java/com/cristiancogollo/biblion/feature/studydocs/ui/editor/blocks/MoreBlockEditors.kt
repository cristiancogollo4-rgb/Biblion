package com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

@Composable
fun NoteBlockEditor(
    block: StudyBlock.Note,
    isSelected: Boolean,
    onSelectionChange: (IntRange?) -> Unit = {},
    onTextChange: (StyledText) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig.FontSize,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp)),
    ) {
        Column {
            Text("Nota", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StyledTextEditor(
                text = block.text,
                onTextChange = onTextChange,
                onSelectionChange = onSelectionChange,
                onFocusChanged = onFocusChanged,
                baseStyle = MaterialTheme.typography.bodyMedium,
                placeholder = "Nota...",
                isSelected = isSelected,
                requestFocus = isSelected,
                blockId = block.id.value,
                fontSize = fontSize,
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
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig.FontSize,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp)),
    ) {
        Column {
            if (!block.prompt.isNullOrBlank()) {
                Text(block.prompt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            StyledTextEditor(
                text = block.text,
                onTextChange = onTextChange,
                onSelectionChange = onSelectionChange,
                onFocusChanged = onFocusChanged,
                baseStyle = MaterialTheme.typography.bodyMedium,
                placeholder = "Reflexion...",
                isSelected = isSelected,
                requestFocus = isSelected,
                blockId = block.id.value,
                fontSize = fontSize,
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
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig.FontSize,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp)),
    ) {
        Column {
            Text("! Destacado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StyledTextEditor(
                text = block.text,
                onTextChange = onTextChange,
                onSelectionChange = onSelectionChange,
                onFocusChanged = onFocusChanged,
                baseStyle = MaterialTheme.typography.bodyMedium,
                placeholder = "Destacado...",
                isSelected = isSelected,
                requestFocus = isSelected,
                blockId = block.id.value,
                fontSize = fontSize,
            )
        }
    }
}

@Composable
fun VerseBlockEditor(
    block: StudyBlock.Verse,
    isSelected: Boolean,
    onReplace: (StudyBlock) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            block.reference.displayShort(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = block.primaryText.plain(),
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
        )
        if (block.compareVersion != null && block.compareText != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        block.primaryVersion,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                    Text(
                        block.primaryText.plain(),
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        block.compareVersion,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        block.compareText.plain(),
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        if (isSelected) {
            VerseContextualToolbar(block, onReplace)
        }
    }
}

@Composable
private fun VerseContextualToolbar(
    block: StudyBlock.Verse,
    onReplace: (StudyBlock) -> Unit,
) {
    var showVersionMenu by remember { mutableStateOf(false) }
    val versions = listOf("RVR1960", "NVI", "PDT", "TLA", "RVC")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            AssistChip(
                onClick = { showVersionMenu = true },
                label = { Text(block.primaryVersion, fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
            DropdownMenu(
                expanded = showVersionMenu,
                onDismissRequest = { showVersionMenu = false },
            ) {
                versions.forEach { v ->
                    DropdownMenuItem(
                        text = { Text(v) },
                        onClick = {
                            onReplace(block.copy(primaryVersion = v))
                            showVersionMenu = false
                        },
                    )
                }
            }
        }
        AssistChip(
            onClick = {
                if (block.compareVersion != null) {
                    onReplace(block.copy(compareVersion = null, compareText = null))
                } else {
                    onReplace(block.copy(compareVersion = "NVI"))
                }
            },
            label = {
                Text(
                    if (block.compareVersion != null) "Quitar comparacion" else "Comparar",
                    fontSize = 11.sp,
                )
            },
        )
    }
}

@Composable
fun DividerBlockView(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        thickness = 2.dp,
    )
}

@Composable
fun PageBreakBlockView(modifier: Modifier = Modifier) {
    Text(
        "--- Salto de pagina ---",
        modifier = modifier.fillMaxWidth(),
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
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
                            .clip(RoundedCornerShape(2.dp)),
                    ) {
                        StyledTextEditor(
                            text = cell,
                            onTextChange = { onCellChange(rowIndex, colIndex, it) },
                            baseStyle = MaterialTheme.typography.bodySmall,
                            onFocusChanged = { },
                            isSelected = isSelected,
                            blockId = block.id.value,
                        )
                    }
                }
            }
        }
    }
}

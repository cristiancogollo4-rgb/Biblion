package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cristiancogollo.biblion.feature.studydocs.domain.ActiveFormatSnapshot
import com.cristiancogollo.biblion.feature.studydocs.domain.TextStyleKind

@Composable
fun EditorToolbar(
    activeFormat: ActiveFormatSnapshot,
    currentFontSize: Int,
    currentFontFamily: String?,
    onToggleStyle: (TextStyleKind) -> Unit,
    onTextColor: (Int) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onClearColor: () -> Unit,
    onStepFontSize: (Int) -> Unit,
    onSetFontSize: (Int) -> Unit,
    onFontFamily: (String) -> Unit,
    onCycleAlignment: () -> Unit,
    onInsertBlock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().zIndex(10f),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FormatToggleButton(
                icon = Icons.Filled.FormatBold,
                label = "Negrita",
                isActive = activeFormat.bold,
                onClick = { onToggleStyle(TextStyleKind.Bold) },
            )
            FormatToggleButton(
                icon = Icons.Filled.FormatItalic,
                label = "Cursiva",
                isActive = activeFormat.italic,
                onClick = { onToggleStyle(TextStyleKind.Italic) },
            )
            FormatToggleButton(
                icon = Icons.Filled.FormatUnderlined,
                label = "Subrayado",
                isActive = activeFormat.underline,
                onClick = { onToggleStyle(TextStyleKind.Underline) },
            )
            FormatToggleButton(
                icon = Icons.Filled.StrikethroughS,
                label = "Tachado",
                isActive = activeFormat.strikethrough,
                onClick = { onToggleStyle(TextStyleKind.Strikethrough) },
            )

            VerticalDivider(dividerModifier)

            var showTextColor by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showTextColor = true }) {
                    Icon(
                        Icons.Filled.FormatColorText,
                        contentDescription = "Color de texto",
                        modifier = Modifier.size(20.dp),
                        tint = activeFormat.color?.let { Color(it) }
                            ?: MaterialTheme.colorScheme.onSurface,
                    )
                }
                ColorDropdown(
                    expanded = showTextColor,
                    onDismiss = { showTextColor = false },
                    onColorPicked = { onTextColor(it); showTextColor = false },
                    onClear = { onClearColor(); showTextColor = false },
                )
            }

            val hasFormat = activeFormat.bold || activeFormat.italic ||
                    activeFormat.underline || activeFormat.strikethrough ||
                    activeFormat.color != null || activeFormat.background != null
            FormatToggleButton(
                icon = Icons.Filled.FormatClear,
                label = "Limpiar formato",
                isActive = false,
                enabled = hasFormat,
                onClick = { onClearColor() },
            )

            VerticalDivider(dividerModifier)

            FontSizeControl(
                currentFontSize = currentFontSize,
                onStepFontSize = onStepFontSize,
                onSetFontSize = onSetFontSize,
            )

            VerticalDivider(dividerModifier)

            var showFontMenu by remember { mutableStateOf(false) }
            val fontLabel = when (currentFontFamily) {
                "serif" -> "Serif"
                "monospace" -> "Mono"
                "default" -> "Defecto"
                else -> "Sans"
            }
            Box {
                TextButton(onClick = { showFontMenu = true }) {
                    Text(
                        text = fontLabel,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                DropdownMenu(
                    expanded = showFontMenu,
                    onDismissRequest = { showFontMenu = false },
                ) {
                    listOf(
                        "sans" to "Sans Serif",
                        "serif" to "Serif",
                        "monospace" to "Monospace",
                        "default" to "Por defecto",
                    ).forEach { (key, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = { onFontFamily(key); showFontMenu = false },
                        )
                    }
                }
            }

            FormatToggleButton(
                icon = Icons.Filled.FormatListBulleted,
                label = "Alineacion",
                isActive = false,
                onClick = { onCycleAlignment() },
            )

            VerticalDivider(dividerModifier)

            var showInsertMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showInsertMenu = true }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Insertar bloque",
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = showInsertMenu,
                    onDismissRequest = { showInsertMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Parrafo") },
                        onClick = { onInsertBlock("paragraph"); showInsertMenu = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Titulo 1") },
                        onClick = { onInsertBlock("heading1"); showInsertMenu = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Titulo 2") },
                        onClick = { onInsertBlock("heading2"); showInsertMenu = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Titulo 3") },
                        onClick = { onInsertBlock("heading3"); showInsertMenu = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Lista con vinetas") },
                        onClick = { onInsertBlock("bullet"); showInsertMenu = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Lista numerada") },
                        onClick = { onInsertBlock("ordered"); showInsertMenu = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Cita") },
                        onClick = { onInsertBlock("quote"); showInsertMenu = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onColorPicked: (Int) -> Unit,
    onClear: () -> Unit,
) {
    val palette = listOf(
        0xFF000000.toInt(), 0xFF424242.toInt(), 0xFF9E9E9E.toInt(), 0xFFE53935.toInt(),
        0xFFFB8C00.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(),
        0xFF3949AB.toInt(), 0xFF8E24AA.toInt(), 0xFFD81B60.toInt(), 0xFFFFFFFF.toInt(),
    )
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        palette.forEach { c ->
            DropdownMenuItem(
                text = { Box(Modifier.size(20.dp).background(Color(c), RoundedCornerShape(4.dp))) },
                onClick = { onColorPicked(c) },
            )
        }
        DropdownMenuItem(
            text = { Text("Quitar color") },
            onClick = onClear,
        )
    }
}

@Composable
private fun FormatToggleButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (isActive && enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val dividerModifier = Modifier.padding(horizontal = 6.dp).height(24.dp)

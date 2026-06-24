package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun EditorTopBar(
    isFormatEnabled: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrikethrough: () -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onAddBlock: () -> Unit,
    modifier: Modifier = Modifier,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    undoDescription: String? = null,
    redoDescription: String? = null,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    activeBlockTypeName: String? = null,
    isTypeDropdownEnabled: Boolean = false,
    onTypeSelected: (Int) -> Unit = {},
    isBoldActive: Boolean = false,
    isItalicActive: Boolean = false,
    isUnderlineActive: Boolean = false,
    isStrikethroughActive: Boolean = false,
    isBulletActive: Boolean = false,
    isNumberedActive: Boolean = false,
    onColorClick: () -> Unit = {},
    onAlignClick: () -> Unit = {},
    alignIcon: ImageVector = Icons.Filled.FormatAlignLeft,
    zoomPercent: String = "100%",
    isZoomModified: Boolean = false,
    onResetZoom: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
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
        ) {
            ToolbarIconButton(Icons.Filled.Undo, undoDescription ?: "Deshacer", canUndo, onUndo)
            ToolbarIconButton(Icons.Filled.Redo, redoDescription ?: "Rehacer", canRedo, onRedo)

            VerticalDivider(Modifier.padding(horizontal = 6.dp).height(24.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            ToolbarIconButton(Icons.Filled.FormatBold, "Negrita", isFormatEnabled && !isBoldActive, onBold,
                isActive = isBoldActive && isFormatEnabled)
            ToolbarIconButton(Icons.Filled.FormatItalic, "Cursiva", isFormatEnabled && !isItalicActive, onItalic,
                isActive = isItalicActive && isFormatEnabled)
            ToolbarIconButton(Icons.Filled.FormatUnderlined, "Subrayado", isFormatEnabled && !isUnderlineActive, onUnderline,
                isActive = isUnderlineActive && isFormatEnabled)
            ToolbarIconButton(Icons.Filled.FormatStrikethrough, "Tachado", isFormatEnabled && !isStrikethroughActive, onStrikethrough,
                isActive = isStrikethroughActive && isFormatEnabled)

            VerticalDivider(Modifier.padding(horizontal = 6.dp).height(24.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            ToolbarIconButton(Icons.Filled.FormatColorText, "Color de texto", isFormatEnabled, onColorClick)
            ToolbarIconButton(alignIcon, "Alineacion", isFormatEnabled, onAlignClick)

            VerticalDivider(Modifier.padding(horizontal = 6.dp).height(24.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Indicador de zoom con botones +/- y reset
            ToolbarTextButton(
                text = "\u2212",
                contentDescription = "Reducir zoom",
                enabled = true,
                onClick = onZoomOut,
            )
            Text(
                text = zoomPercent,
                style = MaterialTheme.typography.labelSmall,
                color = if (isZoomModified) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            ToolbarTextButton(
                text = "+",
                contentDescription = "Aumentar zoom",
                enabled = true,
                onClick = onZoomIn,
            )
            ToolbarIconButton(
                Icons.Filled.Refresh,
                "Restablecer zoom",
                enabled = isZoomModified,
                onClick = onResetZoom,
            )

            ToolbarIconButton(Icons.Filled.FormatListBulleted, "Lista con vi\u00f1etas", isFormatEnabled && !isBulletActive, onBullet,
                isActive = isBulletActive && isFormatEnabled)
            ToolbarIconButton(Icons.Filled.FormatListNumbered, "Lista numerada", isFormatEnabled && !isNumberedActive, onNumbered,
                isActive = isNumberedActive && isFormatEnabled)

            VerticalDivider(Modifier.padding(horizontal = 6.dp).height(24.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            TypeDropdown(activeBlockTypeName, isTypeDropdownEnabled, onTypeSelected)

            Box(modifier = Modifier.weight(1f))
            ToolbarIconButton(Icons.Filled.Add, "Anadir bloque estructural", true, onAddBlock)
        }
    }
}

@Composable
private fun TypeDropdown(
    activeBlockTypeName: String?,
    isEnabled: Boolean,
    onTypeSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = activeBlockTypeName ?: "Tipo",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isEnabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = { if (isEnabled) expanded = true }, enabled = isEnabled, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Tipo de bloque",
                tint = if (isEnabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        listOf("Parrafo", "Titulo 1", "Titulo 2", "Titulo 3", "Cita", "Lista")
            .forEachIndexed { index, label ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    expanded = false
                    onTypeSelected(index)
                })
        }
    }
}

@Composable
private fun ToolbarTextButton(
    text: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(32.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isActive: Boolean = false,
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .let { if (isActive) it.then(Modifier.background(bg, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))) else it }
        ) {
            Icon(
                icon, contentDescription,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp).align(Alignment.Center),
            )
        }
    }
}

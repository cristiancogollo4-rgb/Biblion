package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.domain.ActiveFormatSnapshot
import com.cristiancogollo.biblion.feature.studydocs.domain.TextStyleKind
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

/**
 * Toolbar deslizable horizontal con gradientes de atenuación en los bordes.
 *
 * Características:
 * - Targets de toque de 44dp mínimo
 * - Gradientes laterales cuando hay contenido oculto
 * - Haptic feedback al hacer tap
 * - Estado activo reflejado visualmente
 */
@Composable
fun SwipeableEditorToolbar(
    activeFormat: ActiveFormatSnapshot,
    currentFontSize: Int,
    onToggleStyle: (TextStyleKind) -> Unit,
    onTextColor: (Int) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onClearColor: () -> Unit,
    onStepFontSize: (Int) -> Unit,
    onCycleAlignment: () -> Unit,
    onInsertBlock: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    // Detectar si hay contenido oculto
    val canScrollLeft by remember { derivedStateOf { scrollState.canScrollBackward } }
    val canScrollRight by remember { derivedStateOf { scrollState.canScrollForward } }

    Box(modifier = modifier.fillMaxWidth()) {
        // Toolbar deslizable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Estilos de texto
            ToolbarButton(
                icon = Icons.Filled.FormatBold,
                isActive = activeFormat.bold,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleStyle(TextStyleKind.Bold)
                }
            )
            ToolbarButton(
                icon = Icons.Filled.FormatItalic,
                isActive = activeFormat.italic,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleStyle(TextStyleKind.Italic)
                }
            )
            ToolbarButton(
                icon = Icons.Filled.FormatUnderlined,
                isActive = activeFormat.underline,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleStyle(TextStyleKind.Underline)
                }
            )
            ToolbarButton(
                icon = Icons.Filled.FormatStrikethrough,
                isActive = activeFormat.strikethrough,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleStyle(TextStyleKind.Strikethrough)
                }
            )

            ToolbarDivider()

            // Tamaño de fuente
            ToolbarButton(
                icon = Icons.Filled.TextDecrease,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStepFontSize(-1)
                }
            )
            Text(
                text = "${currentFontSize}px",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            ToolbarButton(
                icon = Icons.Filled.TextIncrease,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStepFontSize(1)
                }
            )

            ToolbarDivider()

            // Alineación
            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCycleAlignment()
                }
            )

            ToolbarDivider()

            // Color de texto
            ToolbarButton(
                icon = Icons.Filled.FormatColorText,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTextColor(BiblionBluePrimary.toArgb())
                }
            )

            // Color de fondo
            ToolbarButton(
                icon = Icons.Filled.FormatColorFill,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackgroundColor(BiblionGoldPrimary.toArgb())
                }
            )

            // Limpiar color
            ToolbarButton(
                icon = Icons.Filled.FormatColorReset,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClearColor()
                }
            )

            ToolbarDivider()

            // Insertar bloque
            ToolbarButton(
                icon = Icons.Filled.Add,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onInsertBlock("paragraph")
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Guardar
            ToolbarButton(
                icon = Icons.Filled.Save,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave()
                }
            )
        }

        // Gradiente izquierdo
        if (canScrollLeft) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Gradiente derecho
        if (canScrollRight) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) BiblionGoldPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            .padding(horizontal = 4.dp)
    )
}

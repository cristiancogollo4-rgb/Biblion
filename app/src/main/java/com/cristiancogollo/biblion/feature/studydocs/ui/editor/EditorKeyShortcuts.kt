package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

fun Modifier.undoRedoKeyHandler(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
): Modifier = this.onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val ctrl = event.isCtrlPressed || event.isMetaPressed
    if (!ctrl) return@onPreviewKeyEvent false
    when (event.key) {
        Key.Z -> {
            if (event.isShiftPressed) {
                if (canRedo) onRedo()
            } else {
                if (canUndo) onUndo()
            }
            true
        }
        Key.Y -> {
            if (canRedo) onRedo()
            true
        }
        else -> false
    }
}

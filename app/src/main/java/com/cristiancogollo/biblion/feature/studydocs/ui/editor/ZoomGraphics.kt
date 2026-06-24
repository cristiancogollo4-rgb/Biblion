package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Aplica el [EditorZoomState] como una transformacion graphics-layer
 * uniforme sobre el editor: scaleX = scaleY = state.scale.
 *
 * El origen de la transformacion es el centro de la pantalla (0.5, 0.5).
 */
fun Modifier.zoomGraphics(state: EditorZoomState): Modifier = this.graphicsLayer {
    scaleX = state.scale
    scaleY = state.scale
    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
}

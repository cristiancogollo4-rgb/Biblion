package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Aplica el [EditorZoomState] como una transformacion graphics-layer
 * sobre el editor: scaleY y translationY desde el centro vertical.
 *
 * Mantiene scaleX=1f para no deformar el texto horizontalmente.
 */
fun Modifier.zoomGraphics(state: EditorZoomState): Modifier = this.graphicsLayer {
    scaleX = 1f
    scaleY = state.scale
    translationY = state.offsetY
    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
}

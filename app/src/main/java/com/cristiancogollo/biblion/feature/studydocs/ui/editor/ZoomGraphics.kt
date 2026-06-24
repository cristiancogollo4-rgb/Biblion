package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Aplica el [EditorZoomState] como una transformacion graphics-layer
 * uniforme. Usa top-center como origen para que el zoom expanda el
 * contenido hacia abajo y a los lados desde el centro superior.
 */
fun Modifier.zoomGraphics(state: EditorZoomState): Modifier = this.graphicsLayer {
    scaleX = state.scale
    scaleY = state.scale
    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
}

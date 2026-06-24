package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAll

/**
 * Modifier que aplica gestos de zoom y pan vertical al editor.
 *
 * - **Pinch vertical** (dos dedos) -> escala el eje Y entre 0.5x y 3.0x.
 * - **Pan vertical** (un dedo, o un dedo secundario durante pinch) -> desplaza
 *   el lienzo verticalmente.
 *
 * El estado se mantiene en un [MutableState] externo (no se observa aqui) para
 * que el padre pueda mostrar indicadores, resetear, o persistir.
 *
 * El zoom es **continuo, sin snap** (ver [EditorZoomState]).
 *
 * @param state estado de zoom externo. Solo se muta, no se observa dentro del modifier.
 * @param onInteractionEnd callback al terminar un gesto (dragend/pinchend).
 */
fun Modifier.verticalPinchZoom(
    state: MutableState<EditorZoomState>,
    onInteractionEnd: () -> Unit = {},
): Modifier = this.pointerInput(Unit) {
    detectTransformGestures { _, pan, zoom, _ ->
        val current = state.value
        val newPanY = pan.y
        val scaleFactor = if (zoom != 1f) zoom else 1f
        state.value = current.applyPinchAndPan(
            scaleFactor = scaleFactor,
            panDeltaY = newPanY,
        )
    }
}.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (!event.changes.fastAll { !it.pressed }) {
                // Al menos un dedo sigue presionado
            } else {
                if (state.value.isZooming) {
                    state.value = state.value.endInteraction()
                    onInteractionEnd()
                }
            }
        }
    }
}

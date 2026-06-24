package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modifier que aplica gestos de pinch-to-zoom al editor.
 *
 * - **Pinch con 2+ dedos**: escala uniforme entre 0.5x y 3.0x.
 * - **Scroll con 1 dedo**: pasa sin interferencia al LazyColumn.
 *
 * Usa [PointerEventPass.Initial] para observar eventos sin consumirlos,
 * permitiendo que el LazyColumn y el zoom coexistan sin bloquearse.
 *
 * @param state estado de zoom externo. Solo se muta, no se observa aqui.
 */
fun Modifier.verticalPinchZoom(
    state: MutableState<EditorZoomState>,
): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        var prevDist = 0f
        while (true) {
            // Observa los eventos en la fase Initial sin consumirlos.
            // Asi el LazyColumn recibe scroll y el pinch puede actuar simultaneamente.
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val active = event.changes.filter { it.pressed }

            if (active.size >= 2) {
                val positions = active.map { it.position }
                val cx = positions.sumOf { it.x.toDouble() }.toFloat() / positions.size
                val cy = positions.sumOf { it.y.toDouble() }.toFloat() / positions.size
                val centroid = Offset(cx, cy)
                val avgDist = positions.map { (it - centroid).getDistance() }.average().toFloat()

                if (prevDist > 0f) {
                    val factor = avgDist / prevDist
                    state.value = state.value.applyPinch(factor)
                }
                prevDist = avgDist
            } else {
                prevDist = 0f
            }
            // NUNCA consumir eventos. LazyColumn recibe los mismos.
        }
    }
}

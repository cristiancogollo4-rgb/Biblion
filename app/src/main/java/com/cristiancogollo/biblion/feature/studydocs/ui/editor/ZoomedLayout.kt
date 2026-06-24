package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import kotlin.math.roundToInt

/**
 * Envuelve [content] en un [SubcomposeLayout] que:
 *
 * 1. Mide el contenido a tamano 1x.
 * 2. Reporta al padre un tamano de layout = contenido × [zoom].
 * 3. Aplica [graphicsLayer] con scale=[zoom] al contenido para que
 *    el visual llene exactamente el espacio de layout.
 *
 * Esto elimina el espacio en blanco al hacer zoom out (layout se
 * reduce proporcionalmente) y evita el clipping al hacer zoom in
 * (layout crece proporcionalmente, activando scroll).
 *
 * @param zoom factor de escala (0.5x-3.0x).
 * @param content el documento a renderizar (PaginatedPaperSheet).
 */
@Composable
fun ZoomedLayout(
    zoom: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val measurable = subcompose("zoomable") {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            ) {
                content()
            }
        }.first()

        val placeable = measurable.measure(
            constraints.copy(
                maxWidth = if (zoom >= 1f) constraints.maxWidth
                else (constraints.maxWidth / zoom).roundToInt().coerceAtLeast(1),
            ),
        )

        val w = (placeable.width * zoom).roundToInt().coerceAtLeast(1)
        val h = (placeable.height * zoom).roundToInt().coerceAtLeast(1)
        layout(w, h) {
            placeable.place(0, 0)
        }
    }
}

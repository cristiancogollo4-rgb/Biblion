package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * Estado del factor de zoom del documento.
 *
 * El desplazamiento pertenece a los estados de scroll del lienzo. Mantener una sola
 * autoridad para el paneo evita que el zoom y el scroll compitan por la misma
 * posicion durante un pinch.
 */
@Stable
class DocumentZoomState(
    initial: Float = 1.0f,
    private val min: Float = MIN_ZOOM,
    private val max: Float = MAX_ZOOM,
) {
    var zoom by mutableFloatStateOf(initial.coerceIn(min, max))
        private set
    var isGesturing by mutableStateOf(false)
        private set
    var gestureZoom by mutableFloatStateOf(zoom)
        private set
    var gestureAnchorFraction by mutableStateOf(Offset(0.5f, 0.5f))
        private set

    val visualScale: Float
        get() = if (isGesturing) gestureZoom / zoom else 1f

    private var viewportSize by mutableStateOf(Size.Zero)
    private var contentSize by mutableStateOf(Size.Zero)

    /** Multiplica el zoom actual por (1 + delta). */
    fun step(delta: Float) {
        set(zoom * (1f + delta))
    }

    /** Fuerza un valor absoluto respetando el rango configurado. */
    fun set(value: Float) {
        if (!value.isFinite()) return
        zoom = value.coerceIn(min, max)
        gestureZoom = zoom
        isGesturing = false
    }

    fun beginGesture(anchorFraction: Offset = Offset(0.5f, 0.5f)) {
        if (isGesturing) return
        gestureZoom = zoom
        gestureAnchorFraction = Offset(
            if (anchorFraction.x.isFinite()) anchorFraction.x.coerceIn(0f, 1f) else 0.5f,
            if (anchorFraction.y.isFinite()) anchorFraction.y.coerceIn(0f, 1f) else 0.5f,
        )
        isGesturing = true
    }

    fun updateGesture(zoomChange: Float) {
        if (!isGesturing || !zoomChange.isFinite() || zoomChange <= 0f) return
        gestureZoom = (gestureZoom * zoomChange).coerceIn(min, max)
    }

    fun endGesture() {
        if (!isGesturing) return
        zoom = gestureZoom
        isGesturing = false
    }

    /** Conserva las medidas base necesarias para el comando "ajustar al ancho". */
    fun updateViewport(size: Size) {
        if (viewportSize != size) viewportSize = size
    }

    fun updateContent(size: Size) {
        if (contentSize != size) contentSize = size
    }

    /** Ajusta el lienzo al ancho disponible sin introducir una traslacion paralela. */
    fun fitWidth(horizontalPaddingPx: Float = 32f) {
        if (viewportSize.width <= horizontalPaddingPx * 2f || contentSize.width <= 0f) return
        val availableWidth = (viewportSize.width - horizontalPaddingPx * 2f).coerceAtLeast(1f)
        set(availableWidth / contentSize.width)
    }

    /** Vuelve al zoom neutro. */
    fun reset() {
        set(1.0f)
    }

    companion object {
        const val MIN_ZOOM = 0.75f
        const val MAX_ZOOM = 2.0f

        val PRESETS: List<Float> = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    }
}

@Composable
fun rememberDocumentZoomState(
    initial: Float = 1.0f,
    min: Float = DocumentZoomState.MIN_ZOOM,
    max: Float = DocumentZoomState.MAX_ZOOM,
): DocumentZoomState {
    return remember { DocumentZoomState(initial, min, max) }
}

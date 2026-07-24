package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size

/**
 * Estado del factor de zoom del documento.
 *
 * El desplazamiento pertenece a los ScrollState del lienzo. Mantener una sola
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

    private var viewportSize by mutableStateOf(Size.Zero)
    private var contentSize by mutableStateOf(Size.Zero)

    /** Multiplica el zoom actual por (1 + delta). */
    fun step(delta: Float) {
        set(zoom * (1f + delta))
    }

    /** Fuerza un valor absoluto respetando el rango configurado. */
    fun set(value: Float) {
        zoom = value.coerceIn(min, max)
    }

    /** Conserva las medidas base necesarias para el comando "ajustar al ancho". */
    fun updateViewport(size: Size) {
        viewportSize = size
    }

    fun updateContent(size: Size) {
        contentSize = size
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

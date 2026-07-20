package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Estado de zoom del lienzo de hojas paginadas.
 *
 * Modelo simplificado post-paginacion: el scroll vertical nativo del [PaginatedSheet]
 * reemplaza el paneo manual. Aqui queda solo el factor de escala visual, aplicado
 * mediante un `LocalDensity` virtual (densidad fisica * zoom) que conserva la
 * composicion visual sin reflow (igual que Google Docs al hacer zoom).
 *
 * Rango por defecto 0.75x-2.0x alineado con los presets de la toolbar
 * (75/100/125/150/200%).
 */
@Stable
class DocumentZoomState(
    initial: Float = 1.0f,
    private val min: Float = MIN_ZOOM,
    private val max: Float = MAX_ZOOM,
) {
    var zoom by mutableFloatStateOf(initial.coerceIn(min, max))
        private set

    /** Multiplica el zoom actual por (1 + delta), p.ej. `step(0.25f)` sube un escalon. */
    fun step(delta: Float) {
        set(zoom * (1f + delta))
    }

    /** Forza un valor absoluto, respetando el rango [min, max]. */
    fun set(value: Float) {
        zoom = value.coerceIn(min, max)
    }

    /** Vuelve al zoom neutro (1.0f). */
    fun reset() {
        zoom = 1.0f.coerceIn(min, max)
    }

    companion object {
        const val MIN_ZOOM = 0.75f
        const val MAX_ZOOM = 2.0f

        /** Presets usados por la toolbar (proporcion sobre 1.0). */
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

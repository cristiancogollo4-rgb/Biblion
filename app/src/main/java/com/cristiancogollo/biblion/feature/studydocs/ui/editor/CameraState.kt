package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Estado de zoom del editor. Solo maneja escala visual; el scroll
 * es nativo del Viewport exterior (Box + verticalScroll/horizontalScroll).
 *
 * Arquitectura CSS (Google Docs):
 *   Viewport     → Box(fillMaxSize).scroll()
 *   DocumentCanvas → Box(size(native*z)).graphicsLayer { scale=z }
 *   Pages        → PaperSheet(width=DocConfig.PageWidth, height=DocConfig.PageHeight)
 *
 * - [zoom]: escala visual (0.5x a 2.0x, 7 niveles discretos).
 */
data class CameraState(
    val zoom: Float = 1f,
) {
    companion object {
        const val MIN_ZOOM: Float = DocConfig.MinZoom
        const val MAX_ZOOM: Float = DocConfig.MaxZoom
        val ZOOM_LEVELS: List<Float> = DocConfig.ZoomLevels
        val Initial: CameraState = CameraState()
    }

    /**
     * Ajusta el zoom al nivel discreto mas cercano.
     */
    fun snapZoom(): CameraState = copy(
        zoom = ZOOM_LEVELS.minByOrNull { abs(it - zoom) } ?: zoom,
    )

    /**
     * Sube un nivel discreto de zoom (usado por boton +).
     */
    fun stepIn(): CameraState {
        val next = ZOOM_LEVELS.firstOrNull { it > zoom + 0.01f } ?: MAX_ZOOM
        return copy(zoom = next)
    }

    /**
     * Baja un nivel discreto de zoom (usado por boton -).
     */
    fun stepOut(): CameraState {
        val prev = ZOOM_LEVELS.lastOrNull { it < zoom - 0.01f } ?: MIN_ZOOM
        return copy(zoom = prev)
    }

    /**
     * Restablece a los valores iniciales.
     */
    fun reset(): CameraState = Initial

    /**
     * Porcentaje legible para la UI (p.ej. "150%").
     */
    fun displayPercent(): String = "${(zoom * 100).roundToInt()}%"
}

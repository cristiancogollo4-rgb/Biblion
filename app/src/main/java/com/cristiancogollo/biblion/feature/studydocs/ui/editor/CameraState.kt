package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Estado de la camara virtual que define la relacion entre
 * el documento (coordenadas de mundo) y la pantalla (viewport).
 *
 * Arquitectura Google Docs:
 *   screen = world * zoom + offset
 *   world  = (screen - offset) / zoom
 *
 * - [zoom]: escala visual (0.5x a 2.5x, discreta en 7 niveles)
 * - [offsetX]/[offsetY]: desplazamiento de la camara en pixeles
 */
data class CameraState(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    companion object {
        const val MIN_ZOOM: Float = 0.5f
        const val MAX_ZOOM: Float = 2.5f
        val ZOOM_LEVELS: List<Float> = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f)
        val Initial: CameraState = CameraState()
    }

    /**
     * Convierte coordenadas de pantalla a coordenadas de documento.
     *   world = (screen - offset) / zoom
     */
    fun screenToWorld(screenX: Float, screenY: Float): Offset =
        Offset((screenX - offsetX) / zoom, (screenY - offsetY) / zoom)

    /**
     * Convierte coordenadas de documento a coordenadas de pantalla.
     *   screen = world * zoom + offset
     */
    fun worldToScreen(worldX: Float, worldY: Float): Offset =
        Offset(worldX * zoom + offsetX, worldY * zoom + offsetY)

    /**
     * Zoom focalizado desde un punto de la pantalla.
     * El punto bajo los dedos NO se mueve al hacer zoom.
     *
     * @param newZoom nueva escala deseada (se limita a [MIN_ZOOM, MAX_ZOOM]).
     * @param focusX coordenada X del foco en pixeles de pantalla.
     * @param focusY coordenada Y del foco en pixeles de pantalla.
     */
    fun focalZoom(newZoom: Float, focusX: Float, focusY: Float): CameraState {
        val z = newZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val world = screenToWorld(focusX, focusY)
        return copy(
            zoom = z,
            offsetX = focusX - world.x * z,
            offsetY = focusY - world.y * z,
        )
    }

    /**
     * Limita los offsets para que el documento nunca se escape
     * completamente del viewport (no hay "vacio infinito").
     *
     * @param vpW ancho del viewport en pixeles.
     * @param vpH alto del viewport en pixeles.
     * @param docW ancho del documento a 1x en pixeles.
     * @param docH alto del documento a 1x en pixeles.
     */
    fun clamp(vpW: Float, vpH: Float, docW: Float, docH: Float): CameraState {
        val scaledW = docW * zoom
        val scaledH = docH * zoom
        return copy(
            offsetX = offsetX.coerceIn((-scaledW + vpW).coerceAtMost(0f), 0f),
            offsetY = offsetY.coerceIn((-scaledH + vpH).coerceAtMost(0f), 0f),
        )
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

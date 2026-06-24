package com.cristiancogollo.biblion.feature.studydocs.ui.editor

/**
 * State holder inmutable para el zoom vertical del editor.
 *
 * El zoom afecta **solo al eje Y** (escala vertical de la linea de
 * tiempo de bloques). El eje X mantiene el tamano de fuente original
 * para que el texto siga siendo legible mientras el usuario "abre" o
 * "compacta" la altura del lienzo.
 *
 * El zoom es **continuo, sin snap**: el factor puede ser cualquier
 * float en [MIN_SCALE, MAX_SCALE].
 */
data class EditorZoomState(
    val scale: Float = 1f,
    val offsetY: Float = 0f,
    val isZooming: Boolean = false,
) {
    companion object {
        const val MIN_SCALE: Float = 0.5f
        const val MAX_SCALE: Float = 3.0f
        val Initial: EditorZoomState = EditorZoomState()
    }

    /**
     * Limita [scale] al rango permitido. No modifica el offset.
     */
    fun clamped(): EditorZoomState = copy(scale = scale.coerceIn(MIN_SCALE, MAX_SCALE))

    /**
     * Aplica un pinch (multiplicador de escala, p.ej. 1.05f) y un
     * delta de pan en pixeles del eje Y (focal point).
     *
     * @param scaleFactor multiplicador que viene de detectTransformGestures.
     * @param panDeltaY desplazamiento vertical en pixeles.
     */
    fun applyPinchAndPan(
        scaleFactor: Float,
        panDeltaY: Float,
    ): EditorZoomState {
        val nextScale = (scale * scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
        return copy(
            scale = nextScale,
            offsetY = offsetY + panDeltaY,
            isZooming = true,
        )
    }

    /**
     * Aplica solo pan (un dedo) sin cambiar el zoom. Util cuando el
     * usuario arrastra con un solo dedo estando en modo zoom.
     */
    fun applyPan(panDeltaY: Float): EditorZoomState =
        copy(offsetY = offsetY + panDeltaY, isZooming = true)

    /**
     * Marca fin del gesto de zoom.
     */
    fun endInteraction(): EditorZoomState = copy(isZooming = false)

    /**
     * Restablece a los valores iniciales.
     */
    fun reset(): EditorZoomState = Initial

    /**
     * Porcentaje legible para la UI (p.ej. "150%").
     */
    fun displayPercent(): String = "${(scale * 100).toInt()}%"
}

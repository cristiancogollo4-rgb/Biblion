package com.cristiancogollo.biblion.feature.studydocs.ui.editor

/**
 * State holder inmutable para el zoom del editor.
 *
 * El zoom es **uniforme** (scaleX = scaleY = scale), continuo,
 * sin snap: el factor puede ser cualquier float en [MIN_SCALE, MAX_SCALE].
 *
 * El scroll del LazyColumn se maneja independientemente; este state
 * solo controla la escala visual aplicada via graphicsLayer.
 */
data class EditorZoomState(
    val scale: Float = 1f,
    val isZooming: Boolean = false,
) {
    companion object {
        const val MIN_SCALE: Float = 0.5f
        const val MAX_SCALE: Float = 3.0f
        val Initial: EditorZoomState = EditorZoomState()
    }

    /**
     * Aplica un factor de escala (multiplicador) y lo limita al rango permitido.
     *
     * @param scaleFactor multiplicador del pinch (p.ej. 1.05f o 0.95f).
     */
    fun applyPinch(scaleFactor: Float): EditorZoomState {
        val nextScale = (scale * scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
        return copy(scale = nextScale, isZooming = true)
    }

    /**
     * Marca fin del gesto de zoom.
     */
    fun endInteraction(): EditorZoomState = copy(isZooming = false)

    /**
     * Restablece a los valores iniciales.
     */
    fun reset(): EditorZoomState = Initial

    /**
     * Sube un nivel de zoom (0.25x). Limita a [MAX_SCALE].
     */
    fun stepIn(): EditorZoomState = copy(scale = (scale + 0.25f).coerceAtMost(MAX_SCALE))

    /**
     * Baja un nivel de zoom (0.25x). Limita a [MIN_SCALE].
     */
    fun stepOut(): EditorZoomState = copy(scale = (scale - 0.25f).coerceAtLeast(MIN_SCALE))

    /**
     * Porcentaje legible para la UI (p.ej. "150%").
     */
    fun displayPercent(): String = "${(scale * 100).toInt()}%"
}

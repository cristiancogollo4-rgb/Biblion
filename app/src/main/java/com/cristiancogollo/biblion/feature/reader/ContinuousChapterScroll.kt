package com.cristiancogollo.biblion.feature.reader

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * Lectura continua al estilo de YouVersion (medido en tablet Android):
 * - Dentro del capitulo activo el scroll es libre.
 * - El capitulo vecino asoma atenuado y una inercia suave se detiene en el limite: el inicio
 *   del siguiente no sube de NEXT_CHAPTER_PEEK_LINE y el del actual no baja de
 *   PREVIOUS_CHAPTER_PEEK_LINE.
 * - El texto sigue al dedo mas alla del limite (borde elastico). Soltar con recorrido o
 *   velocidad suficiente confirma el paso; un lanzamiento fuerte desde dentro del capitulo
 *   continua hasta el siguiente. La animacion conserva la velocidad del gesto.
 */

/** Linea (fraccion del alto visible) que el inicio del capitulo siguiente no cruza sin confirmar. */
internal const val NEXT_CHAPTER_PEEK_LINE = 0.62f

/** Linea que el inicio del capitulo actual no baja al asomar el anterior. */
internal const val PREVIOUS_CHAPTER_PEEK_LINE = 0.38f

/** Al avanzar, el capitulo nuevo queda aqui, con el final del anterior asomando arriba. */
internal const val COMMITTED_CHAPTER_TOP_LINE = 0.12f

/** Opacidad del capitulo vecino. */
internal const val NEIGHBOR_CHAPTER_ALPHA = 0.38f

/** Fraccion del arrastre que el texto sigue mas alla del limite (borde elastico). */
internal const val CHAPTER_BOUNDARY_RESISTANCE = 0.55f

/** Una inercia que llega al limite con esta proporcion de la velocidad de paso, continua. */
private const val FLING_CARRY_FACTOR = 1.6f

/** Cuadros mas separados que esto no sirven para estimar la velocidad de la inercia. */
private const val MAX_FLING_FRAME_NANOS = 50_000_000L

/** Pixeles que la lista puede avanzar antes de que el siguiente capitulo llegue a su linea. */
internal fun forwardRoomBeforeBoundary(nextStartOffset: Int?, boundaryLine: Float): Float =
    nextStartOffset?.let { (it - boundaryLine).coerceAtLeast(0f) } ?: Float.POSITIVE_INFINITY

/** Pixeles que la lista puede retroceder antes de que el capitulo actual baje de su linea. */
internal fun backwardRoomBeforeBoundary(currentStartOffset: Int?, boundaryLine: Float): Float =
    currentStartOffset?.let { (boundaryLine - it).coerceAtLeast(0f) } ?: Float.POSITIVE_INFINITY

/**
 * Divide un delta de scroll (convencion de Compose: negativo = avanzar) en lo que la lista
 * puede recorrer y el exceso que choca contra el limite del capitulo.
 */
internal fun splitScrollAtBoundary(
    deltaY: Float,
    forwardRoom: Float,
    backwardRoom: Float,
): Pair<Float, Float> = when {
    deltaY < 0f -> {
        val allowed = max(deltaY, -forwardRoom)
        allowed to (deltaY - allowed)
    }
    deltaY > 0f -> {
        val allowed = min(deltaY, backwardRoom)
        allowed to (deltaY - allowed)
    }
    else -> 0f to 0f
}

/**
 * Decide si soltar el gesto confirma el paso de capitulo. [pull] positivo = hacia el siguiente.
 * [velocityY] usa la convencion de Compose (negativa al lanzar hacia arriba = avanzar).
 */
internal fun chapterCommitDirection(
    pull: Float,
    velocityY: Float,
    distanceThresholdPx: Float,
    velocityThresholdPx: Float,
    minPullPx: Float,
): Int? {
    if (pull == 0f) return null
    val direction = if (pull > 0f) 1 else -1
    val pushedFarEnough = abs(pull) >= distanceThresholdPx
    val flungThrough = abs(pull) >= minPullPx &&
        -velocityY * direction >= velocityThresholdPx
    return if (pushedFarEnough || flungThrough) direction else null
}

@Stable
internal class ContinuousChapterScroll(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    /** Clave del primer item del capitulo siguiente (encabezado o cargador); null si no hay. */
    private val nextStartKey: () -> Any?,
    /** Clave del encabezado del capitulo actual cuando hay un capitulo anterior visible. */
    private val currentStartKey: () -> Any?,
    private val canCommit: (direction: Int) -> Boolean,
    private val onCommitted: (direction: Int) -> Unit,
    private val commitDistancePx: Float,
    private val commitVelocityPx: Float,
    private val minPullPx: Float,
    /** Inercia del sistema: predice si un lanzamiento llegaria al capitulo siguiente. */
    private val flingDecay: DecayAnimationSpec<Float>,
    private val reduceMotion: () -> Boolean,
) {
    /**
     * Desplazamiento visual del texto mas alla del limite (positivo = hacia el siguiente).
     * El texto sigue al dedo con algo de resistencia, como un borde elastico.
     */
    var pull by mutableFloatStateOf(0f)
        private set

    var isCommitting by mutableStateOf(false)
        private set

    /** Direccion del paso en curso; permite atenuar/aclarar los capitulos durante la animacion. */
    var committingDirection by mutableIntStateOf(0)
        private set

    private var settleJob: Job? = null
    private var lastFlingFrameNanos = 0L

    private fun offsetOf(key: Any?): Int? =
        key?.let { k -> listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == k }?.offset }

    private fun viewportLine(fraction: Float): Float? {
        val info = listState.layoutInfo
        val height = info.viewportEndOffset - info.viewportStartOffset
        if (height <= 0) return null
        return info.viewportStartOffset + height * fraction
    }

    private fun forwardRoom(): Float = viewportLine(NEXT_CHAPTER_PEEK_LINE)
        ?.let { forwardRoomBeforeBoundary(offsetOf(nextStartKey()), it) }
        ?: Float.POSITIVE_INFINITY

    private fun backwardRoom(): Float = viewportLine(PREVIOUS_CHAPTER_PEEK_LINE)
        ?.let { backwardRoomBeforeBoundary(offsetOf(currentStartKey()), it) }
        ?: Float.POSITIVE_INFINITY

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (isCommitting) return available
            if (available.y == 0f) return Offset.Zero
            if (source == NestedScrollSource.UserInput) settleJob?.cancel()
            var remaining = available.y
            var consumed = 0f

            // Si el usuario se devuelve, primero se recoge el borde elastico.
            if (source == NestedScrollSource.UserInput && pull != 0f) {
                if (pull > 0f && remaining > 0f) {
                    val used = min(remaining, pull / CHAPTER_BOUNDARY_RESISTANCE)
                    pull = max(0f, pull - used * CHAPTER_BOUNDARY_RESISTANCE)
                    remaining -= used
                    consumed += used
                } else if (pull < 0f && remaining < 0f) {
                    val used = max(remaining, pull / CHAPTER_BOUNDARY_RESISTANCE)
                    pull = min(0f, pull - used * CHAPTER_BOUNDARY_RESISTANCE)
                    remaining -= used
                    consumed += used
                }
                if (remaining == 0f) return Offset(0f, consumed)
            }

            val (_, excess) = splitScrollAtBoundary(
                deltaY = remaining,
                forwardRoom = forwardRoom(),
                backwardRoom = backwardRoom(),
            )
            if (source != NestedScrollSource.UserInput) {
                // Velocidad de la inercia estimada por cuadro: si llega fuerte al limite,
                // continua hasta el capitulo vecino en vez de frenar en seco.
                val now = System.nanoTime()
                val elapsed = (now - lastFlingFrameNanos).coerceAtLeast(1L)
                lastFlingFrameNanos = now
                if (excess != 0f && elapsed < MAX_FLING_FRAME_NANOS) {
                    val scrollVelocity = -remaining / (elapsed / 1_000_000_000f)
                    val direction = if (scrollVelocity > 0f) 1 else -1
                    if (abs(scrollVelocity) >= commitVelocityPx * FLING_CARRY_FACTOR && canCommit(direction)) {
                        commit(direction, scrollVelocity)
                        return available
                    }
                }
            }
            if (excess == 0f) return Offset(0f, consumed)
            // El arrastre mueve el texto mas alla del limite; una inercia suave se detiene en el.
            if (source == NestedScrollSource.UserInput) {
                pull += -excess * CHAPTER_BOUNDARY_RESISTANCE
            }
            return Offset(0f, consumed + excess)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (isCommitting) return available
            val released = pull
            if (released != 0f) {
                val direction = chapterCommitDirection(
                    pull = released,
                    velocityY = available.y,
                    distanceThresholdPx = commitDistancePx,
                    velocityThresholdPx = commitVelocityPx,
                    minPullPx = minPullPx,
                )
                if (direction != null && canCommit(direction)) {
                    commit(direction, scrollVelocity = -available.y)
                } else {
                    settleBack()
                }
                // El gesto termina en el limite o en la transicion, nunca con inercia sobrante.
                return available
            }

            // Lanzamiento desde dentro del capitulo: si la inercia llegaria con fuerza al
            // capitulo vecino, se continua hasta el sin detenerse (como en YouVersion).
            val scrollVelocity = -available.y
            if (abs(scrollVelocity) < commitVelocityPx) return Velocity.Zero
            val direction = if (scrollVelocity > 0f) 1 else -1
            val room = if (direction > 0) forwardRoom() else backwardRoom()
            if (room.isInfinite()) return Velocity.Zero
            val travel = abs(flingDecay.calculateTargetValue(0f, scrollVelocity))
            val overshoot = travel - room
            if (overshoot >= commitDistancePx / CHAPTER_BOUNDARY_RESISTANCE && canCommit(direction)) {
                commit(direction, scrollVelocity)
                return available
            }
            return Velocity.Zero
        }
    }

    private fun settleBack() {
        val from = pull
        settleJob?.cancel()
        settleJob = scope.launch {
            if (reduceMotion()) {
                pull = 0f
                return@launch
            }
            animate(
                initialValue = from,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            ) { value, _ -> pull = value }
        }
    }

    private fun commit(direction: Int, scrollVelocity: Float) {
        val distance = if (direction > 0) {
            val start = offsetOf(nextStartKey()) ?: return settleBack()
            val line = viewportLine(COMMITTED_CHAPTER_TOP_LINE) ?: return settleBack()
            start - line
        } else {
            val start = offsetOf(currentStartKey()) ?: return settleBack()
            val line = viewportLine(NEXT_CHAPTER_PEEK_LINE) ?: return settleBack()
            start - line
        }
        settleJob?.cancel()
        isCommitting = true
        committingDirection = direction
        scope.launch {
            try {
                listState.scroll {
                    // El texto ya esta desplazado por el borde elastico: se convierte ese
                    // desplazamiento en scroll real para que no haya salto visual.
                    val alreadyMoved = pull
                    scrollBy(alreadyMoved)
                    pull = 0f
                    val rest = distance - alreadyMoved
                    if (reduceMotion()) {
                        scrollBy(rest)
                    } else {
                        var previous = 0f
                        // Conserva la velocidad del gesto: el paso continua el movimiento del dedo.
                        animate(
                            initialValue = 0f,
                            targetValue = rest,
                            initialVelocity = scrollVelocity,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        ) { value, _ ->
                            scrollBy(value - previous)
                            previous = value
                        }
                    }
                }
                onCommitted(direction)
            } finally {
                pull = 0f
                isCommitting = false
                committingDirection = 0
            }
        }
    }
}

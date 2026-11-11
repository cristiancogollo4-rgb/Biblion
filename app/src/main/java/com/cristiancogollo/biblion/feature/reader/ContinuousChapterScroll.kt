package com.cristiancogollo.biblion.feature.reader

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * Lectura continua al estilo de YouVersion (medido en tablet Android):
 * - Dentro del capitulo activo el scroll es libre.
 * - El capitulo vecino asoma atenuado, pero el scroll se detiene en el limite: el inicio del
 *   siguiente no sube de NEXT_CHAPTER_PEEK_LINE y el del actual no baja de
 *   PREVIOUS_CHAPTER_PEEK_LINE. Una inercia nunca cruza de capitulo.
 * - Empujar mas alla del limite (distancia o velocidad suficiente) confirma el paso: la lista
 *   se desliza hasta el nuevo capitulo y este pasa a ser el activo.
 */

/** Linea (fraccion del alto visible) que el inicio del capitulo siguiente no cruza sin confirmar. */
internal const val NEXT_CHAPTER_PEEK_LINE = 0.62f

/** Linea que el inicio del capitulo actual no baja al asomar el anterior. */
internal const val PREVIOUS_CHAPTER_PEEK_LINE = 0.38f

/** Al avanzar, el capitulo nuevo queda aqui, con el final del anterior asomando arriba. */
internal const val COMMITTED_CHAPTER_TOP_LINE = 0.12f

/** Opacidad del capitulo vecino. */
internal const val NEIGHBOR_CHAPTER_ALPHA = 0.38f

/** El empuje en el limite avanza menos que el dedo, para que se sienta como un tope. */
internal const val CHAPTER_BOUNDARY_RESISTANCE = 0.4f

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
    private val reduceMotion: () -> Boolean,
) {
    /** Empuje acumulado contra el limite (positivo = hacia el siguiente). */
    var pull by mutableFloatStateOf(0f)
        private set

    var isCommitting by mutableStateOf(false)
        private set

    /** Direccion del paso en curso; permite atenuar/aclarar los capitulos durante la animacion. */
    var committingDirection by mutableIntStateOf(0)
        private set

    private fun offsetOf(key: Any?): Int? =
        key?.let { k -> listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == k }?.offset }

    private fun viewportLine(fraction: Float): Float? {
        val info = listState.layoutInfo
        val height = info.viewportEndOffset - info.viewportStartOffset
        if (height <= 0) return null
        return info.viewportStartOffset + height * fraction
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (isCommitting) return available
            if (available.y == 0f) return Offset.Zero
            var remaining = available.y
            var consumed = 0f

            // Si el usuario se devuelve, primero se deshace el empuje acumulado.
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

            val nextLine = viewportLine(NEXT_CHAPTER_PEEK_LINE) ?: return Offset(0f, consumed)
            val previousLine = viewportLine(PREVIOUS_CHAPTER_PEEK_LINE) ?: return Offset(0f, consumed)
            val (_, excess) = splitScrollAtBoundary(
                deltaY = remaining,
                forwardRoom = forwardRoomBeforeBoundary(offsetOf(nextStartKey()), nextLine),
                backwardRoom = backwardRoomBeforeBoundary(offsetOf(currentStartKey()), previousLine),
            )
            if (excess == 0f) return Offset(0f, consumed)
            // Solo el arrastre del usuario empuja; la inercia simplemente se detiene en el limite.
            if (source == NestedScrollSource.UserInput) {
                pull += -excess * CHAPTER_BOUNDARY_RESISTANCE
            }
            return Offset(0f, consumed + excess)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (isCommitting) return available
            val released = pull
            if (released == 0f) return Velocity.Zero
            pull = 0f
            val direction = chapterCommitDirection(
                pull = released,
                velocityY = available.y,
                distanceThresholdPx = commitDistancePx,
                velocityThresholdPx = commitVelocityPx,
                minPullPx = minPullPx,
            )
            if (direction != null && canCommit(direction)) commit(direction)
            // El gesto termina en el limite o en la transicion, nunca con inercia sobrante.
            return available
        }
    }

    private fun commit(direction: Int) {
        val distance = if (direction > 0) {
            val start = offsetOf(nextStartKey()) ?: return
            val line = viewportLine(COMMITTED_CHAPTER_TOP_LINE) ?: return
            start - line
        } else {
            val start = offsetOf(currentStartKey()) ?: return
            val line = viewportLine(NEXT_CHAPTER_PEEK_LINE) ?: return
            start - line
        }
        isCommitting = true
        committingDirection = direction
        scope.launch {
            try {
                if (reduceMotion()) {
                    listState.scrollBy(distance)
                } else {
                    listState.animateScrollBy(
                        value = distance,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                    )
                }
                onCommitted(direction)
            } finally {
                isCommitting = false
                committingDirection = 0
            }
        }
    }
}

private suspend fun LazyListState.scrollBy(distance: Float) {
    scroll { scrollBy(distance) }
}

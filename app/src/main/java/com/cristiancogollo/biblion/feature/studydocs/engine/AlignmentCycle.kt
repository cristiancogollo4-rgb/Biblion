package com.cristiancogollo.biblion.feature.studydocs.engine

import androidx.compose.ui.text.style.TextAlign

/**
 * Helper puro para ciclar la alineacion de un bloque.
 *
 * Secuencia: Start/Left -> Center -> End/Right -> Justify -> Start.
 *
 * Si [current] es null (primera vez), empieza con [TextAlign.Start].
 */
object AlignmentCycle {

    fun next(current: TextAlign?): TextAlign = when (current) {
        null -> TextAlign.Start
        TextAlign.Start, TextAlign.Left -> TextAlign.Center
        TextAlign.Center -> TextAlign.End
        TextAlign.End, TextAlign.Right -> TextAlign.Justify
        TextAlign.Justify -> TextAlign.Start
        else -> TextAlign.Start
    }

    fun displayName(align: TextAlign?): String = when (align) {
        TextAlign.Start, TextAlign.Left -> "Izquierda"
        TextAlign.Center -> "Centro"
        TextAlign.End, TextAlign.Right -> "Derecha"
        TextAlign.Justify -> "Justificado"
        null -> "Sin alineacion"
        else -> "?"
    }
}

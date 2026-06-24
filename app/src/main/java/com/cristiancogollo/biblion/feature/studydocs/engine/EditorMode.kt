package com.cristiancogollo.biblion.feature.studydocs.engine

/**
 * Modo del editor de versiculos.
 *
 * - [Single] es el modo normal: el versiculo se muestra con su
 *   version principal.
 * - [Compare] activa la comparacion lado a lado de dos versiones
 *   biblicas. El render del modo Compare se implementa en una
 *   sesion posterior; esta sesion solo guarda el estado y
 *   serializa la transicion.
 */
sealed interface EditorMode {
    data object Single : EditorMode
    data class Compare(val versionA: String, val versionB: String) : EditorMode
}

/**
 * Tipos de lista soportados por el toggle de toolbar.
 */
enum class ListType { Bullet, Numbered }

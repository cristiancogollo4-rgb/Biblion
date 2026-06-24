package com.cristiancogollo.biblion.feature.studydocs.engine

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key

fun KeyEvent.toNavigatorKey(): CursorNavigator.Key? = when (this.key) {
    Key.DirectionUp -> CursorNavigator.Key.ArrowUp
    Key.DirectionDown -> CursorNavigator.Key.ArrowDown
    Key.Enter -> CursorNavigator.Key.Enter
    else -> null
}

package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cristiancogollo.biblion.feature.studydocs.domain.SplitUiState

/**
 * Controlador de layout para modo estudio split-screen.
 *
 * Distribución fija 50/50.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitLayoutController(
    leftPane: @Composable BoxScope.() -> Unit,
    rightPane: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // Panel izquierdo 50%
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
        ) {
            leftPane()
        }

        // Panel derecho 50%
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .imePadding()
        ) {
            rightPane()
        }
    }
}

package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Hoja carta que envuelve el contenido del editor.
 *
 * Centrada horizontalmente, ancho maximo 600dp (tamano carta),
 * altura segun el espacio disponible (`fillMaxSize`), fondo blanco
 * con sombra de 4dp para simular una hoja de papel real.
 *
 * El zoom se aplica via [zoomGraphics] a toda la hoja, escalando
 * texto, padding, bordes y bloques juntos, como Google Docs.
 *
 * @param zoomState estado de zoom externo. Solo se lee `scale`; el
 *   pinch gesture se maneja en un overlay separado.
 * @param content bloques del documento (LazyColumn con BlockWithHandle).
 */
@Composable
fun PaperSheet(
    zoomState: MutableState<EditorZoomState>,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .zoomGraphics(zoomState.value),
            color = Color.White,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(2.dp),
        ) {
            content(Modifier)
        }
    }
}

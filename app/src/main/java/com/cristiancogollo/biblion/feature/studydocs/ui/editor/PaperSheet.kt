package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PaperBorderGray = Color(0xFFDADCE0)
val PaperWidthDp = 560.dp
val PaperHeightDp = 900.dp

/**
 * Hoja carta individual sin zoom.
 *
 * Ancho fijo 560dp, altura fija 900dp (proporcion carta), fondo blanco
 * con sombra 4dp y borde gris de 0.5dp. El zoom se aplica externamente
 * via un wrapper Box con graphicsLayer.
 */
@Composable
fun PaperSheet(
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .width(PaperWidthDp)
                .height(PaperHeightDp),
            color = Color.White,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(0.5.dp, PaperBorderGray),
        ) {
            content(Modifier)
        }
    }
}

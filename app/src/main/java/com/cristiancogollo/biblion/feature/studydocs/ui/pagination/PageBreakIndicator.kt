package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Indicador visual discreto que aparece entre el final de una pagina y el
 * primer bloque empujado a la siguiente.
 *
 * Diseno no invasivo: una linea suave de corte con un texto opcional pequeño
 * (ej. "Continuacion en la siguiente pagina") para evitar vacios blancos
 * confusos sin saturar el lienzo.
 */
@Composable
fun PageBreakIndicator(
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    )
    if (text != null) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        )
    }
}

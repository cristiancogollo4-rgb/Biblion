package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.R

/**
 * Control de zoom compacto para la barra de herramientas del modo estudio.
 *
 * - Muestra el porcentaje actual como boton tappable que abre un menu con los
 *   presets de [DocumentZoomState.PRESETS] (75/100/125/150/200%).
 * - Si [showStepButtons] es true, añade botones A-/A+ para zoom in/out en pasos
 *   de +/-20%/+25%. Se usa en standalone/lectura donde la toolbar admite mas
 *   controles; en modo split se omite para no saturar la barra.
 */
@Composable
fun ZoomMenu(
    zoomState: DocumentZoomState,
    showStepButtons: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentPercent = (zoomState.zoom * 100).toInt().coerceIn(0, 999)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (showStepButtons) {
            IconButton(onClick = { zoomState.step(-0.2f) }) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.zoom_out),
                )
            }
        }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(
                    text = "$currentPercent%",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DocumentZoomState.PRESETS.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text("${(preset * 100).toInt()}%") },
                        onClick = {
                            zoomState.set(preset)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (showStepButtons) {
            IconButton(onClick = { zoomState.step(0.25f) }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.zoom_in),
                )
            }
        }
    }
}

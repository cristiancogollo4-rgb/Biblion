package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

private data class SlashCommandItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val factory: () -> StudyBlock,
)

@Composable
fun SlashCommandMenu(
    onSelect: (() -> StudyBlock) -> Unit,
    onDismiss: () -> Unit,
) {
    val items = remember {
        listOf(
            SlashCommandItem(
                "Versiculo", "Versiculo biblico", Icons.Filled.MenuBook,
            ) { StudyBlock.Verse(reference = com.cristiancogollo.biblion.feature.studydocs.model.VerseRef(book = "Juan", chapter = 3, verseStart = 16, verseEnd = 16, version = "RVR1960")) },
            SlashCommandItem(
                "Tabla", "Tabla de datos", Icons.Filled.TableChart,
            ) { StudyBlock.Table(rows = listOf(StudyBlock.Table.TableRow(cells = listOf(StyledText.Empty, StyledText.Empty)))) },
            SlashCommandItem(
                "Separador", "Linea horizontal", Icons.Filled.MenuBook,
            ) { StudyBlock.Divider() },
            SlashCommandItem(
                "Nota destacada", "Callout con color", Icons.Filled.CallSplit,
            ) { StudyBlock.Callout() },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text("Insertar bloque estructural") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items.size) { index ->
                    val item = items[index]
                    Surface(
                        onClick = { onSelect(item.factory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Icon(item.icon, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                            androidx.compose.foundation.layout.Column {
                                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        properties = DialogProperties(),
    )
}

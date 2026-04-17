package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import androidx.compose.material3.RadioButtonDefaults

@Composable
fun BibleVersionDialog(
    versions: List<BibleVersionOption>,
    selectedVersionKey: String,
    onVersionSelected: (BibleVersionOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(
                        text = "Elegir versión",
                        style = MaterialTheme.typography.headlineSmall,
                        color = BiblionNavy
                    )
                    Text(
                        text = "Selecciona la traducción para continuar leyendo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(versions, key = { it.key }) { version ->
                        val isSelected = version.key == selectedVersionKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVersionSelected(version) }
                                .background(
                                    //color = if (isSelected) BiblionNavy.copy(alpha = 0.08f) else Color.Transparent,
                                    color = if (isSelected) BiblionGoldSoft.copy(alpha = 0.22f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onVersionSelected(version) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BiblionGoldPrimary,
                                    unselectedColor = BiblionGoldSoft.copy(alpha = 0.7f)
                                )
                            )
                            Text(
                                text = version.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        //Text("CERRAR", color = BiblionNavy)
                        Text("CERRAR", color = BiblionGoldPrimary)
                    }
                }
            }
        }
    }
}

package com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.engine.plainText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

@Composable
fun ColumnLayoutBlockEditor(
    block: StudyBlock.ColumnLayout,
    isSelected: Boolean,
    onReplace: (StudyBlock) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            block.columnBlocks.forEachIndexed { colIndex, columnBlocks ->
                Column(modifier = Modifier.weight(1f)) {
                    columnBlocks.forEach { innerBlock ->
                        when (innerBlock) {
                            is StudyBlock.Paragraph -> Text(
                                innerBlock.text.plain(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            is StudyBlock.Heading -> Text(
                                innerBlock.text.plain(),
                                style = when (innerBlock.level) {
                                    1 -> MaterialTheme.typography.headlineMedium
                                    2 -> MaterialTheme.typography.headlineSmall
                                    else -> MaterialTheme.typography.titleMedium
                                },
                            )
                            is StudyBlock.Quote -> Text(
                                innerBlock.text.plain(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            else -> Text(
                                innerBlock.plainText().ifBlank { "Bloque vacio" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (columnBlocks.isEmpty()) {
                        Text(
                            "Col ${colIndex + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

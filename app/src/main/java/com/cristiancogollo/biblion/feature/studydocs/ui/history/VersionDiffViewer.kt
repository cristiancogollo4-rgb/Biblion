package com.cristiancogollo.biblion.feature.studydocs.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.data.DocVersionRepository.DocVersion
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.displayTypeName
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionDiffViewer(
    currentDoc: StudyDoc,
    oldVersion: DocVersion,
    oldDoc: StudyDoc,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Comparar versiones", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "v${oldVersion.versionNumber} vs actual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DiffSection(
                title = "Titulo",
                oldValue = oldDoc.title.ifBlank { "(sin titulo)" },
                newValue = currentDoc.title.ifBlank { "(sin titulo)" },
                changed = oldDoc.title != currentDoc.title,
            )

            DiffSection(
                title = "Bloques",
                oldValue = "${oldDoc.blocks.size} bloques",
                newValue = "${currentDoc.blocks.size} bloques",
                changed = oldDoc.blocks.size != currentDoc.blocks.size,
            )

            DiffSection(
                title = "Palabras",
                oldValue = "${oldDoc.wordCount()} palabras",
                newValue = "${currentDoc.wordCount()} palabras",
                changed = oldDoc.wordCount() != currentDoc.wordCount(),
            )

            Text(
                text = "Contenido de la version v${oldVersion.versionNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            oldDoc.blocks.forEachIndexed { index, block ->
                BlockDiffCard(
                    block = block,
                    blockIndex = index,
                    currentDocBlocks = currentDoc.blocks,
                )
            }
        }
    }
}

@Composable
private fun DiffSection(
    title: String,
    oldValue: String,
    newValue: String,
    changed: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (changed) {
                BiblionGoldPrimary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                Text(
                    text = oldValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (changed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "→",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = newValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (changed) BiblionBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (changed) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun BlockDiffCard(
    block: StudyBlock,
    blockIndex: Int,
    currentDocBlocks: List<StudyBlock>,
) {
    val existsInCurrent = currentDocBlocks.any { it.id == block.id }
    val bgColor = if (existsInCurrent) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "#${blockIndex + 1} · ${block.displayTypeName}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!existsInCurrent) {
                    Text(
                        text = "ELIMINADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getBlockPreview(block),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun getBlockPreview(block: StudyBlock): String = when (block) {
    is StudyBlock.Paragraph -> block.text.raw.take(200)
    is StudyBlock.Heading -> "[H${block.level}] ${block.text.raw.take(100)}"
    is StudyBlock.Quote -> "\"${block.text.raw.take(150)}\""
    is StudyBlock.BulletList -> block.items.joinToString("\n") { "• ${it.raw.take(80)}" }
    is StudyBlock.NumberedList -> block.items.mapIndexed { i, item -> "${i + 1}. ${item.raw.take(80)}" }.joinToString("\n")
    is StudyBlock.Verse -> "[${block.reference.displayShort()}] ${block.primaryText.raw.take(100)}"
    is StudyBlock.Note -> "[Nota] ${block.text.raw.take(150)}"
    is StudyBlock.Reflection -> "[Reflexion] ${block.text.raw.take(150)}"
    is StudyBlock.Callout -> "[!] ${block.text.raw.take(150)}"
    is StudyBlock.Divider -> "---"
    is StudyBlock.PageBreak -> "[Salto de pagina]"
    is StudyBlock.TodoList -> block.items.joinToString("\n") { "${if (it.checked) "[x]" else "[ ]"} ${it.text.raw.take(80)}" }
    is StudyBlock.Table -> "[Tabla ${block.rows.size}x${block.rows.firstOrNull()?.cells?.size ?: 0}]"
    is StudyBlock.ColumnLayout -> "[Columnas: ${block.columnBlocks.size} columnas]"
    is StudyBlock.Comment -> "[Comentario] ${block.text.take(100)}"
}

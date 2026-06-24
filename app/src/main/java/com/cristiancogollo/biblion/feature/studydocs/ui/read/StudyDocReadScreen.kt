package com.cristiancogollo.biblion.feature.studydocs.ui.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.engine.Paginator
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.StyledTextRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDocReadScreen(
    viewModel: StudyDocViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val doc = uiState.doc
    val pages = remember(doc.blocks) { Paginator.paginate(doc.blocks) }
    var isPagedMode by remember { mutableStateOf(pages.size > 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(doc.title.ifBlank { "Sin titulo" }, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (pages.size > 1) {
                        IconButton(onClick = { isPagedMode = !isPagedMode }) {
                            Icon(
                                imageVector = if (isPagedMode) Icons.Filled.MenuBook else Icons.Filled.MenuBook,
                                contentDescription = if (isPagedMode) "Ver continuo" else "Ver paginado",
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                },
            )
        },
    ) { padding ->
        if (doc.blocks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Documento vacio", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (isPagedMode && pages.size > 1) {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pages.size })
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) { pageIndex ->
                PageContent(blocks = pages[pageIndex])
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(doc.blocks, key = { it.id.value }) { block ->
                    ReadBlockRender(block)
                }
            }
        }
    }
}

@Composable
private fun PageContent(blocks: List<StudyBlock>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(blocks, key = { it.id.value }) { block -> ReadBlockRender(block) }
    }
}

@Composable
private fun ReadBlockRender(block: StudyBlock) {
    when (block) {
        is StudyBlock.Paragraph -> Text(
            text = StyledTextRenderer.toAnnotatedString(block.text, androidx.compose.ui.graphics.Color.Unspecified),
            style = MaterialTheme.typography.bodyLarge,
        )
        is StudyBlock.Heading -> {
            val size = when (block.level) {
                1 -> 28.sp; 2 -> 24.sp; 3 -> 20.sp; else -> 18.sp
            }
            Text(
                text = StyledTextRenderer.toAnnotatedString(block.text, Color.Unspecified),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = size, fontWeight = FontWeight.Bold),
            )
        }
        is StudyBlock.Quote -> Column {
            Text("\u201C", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                text = StyledTextRenderer.toAnnotatedString(block.text, Color.Unspecified),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!block.attribution.isNullOrBlank()) {
                Text("\u2014 ${block.attribution}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        is StudyBlock.BulletList -> Column {
            block.items.forEach { item ->
                androidx.compose.foundation.layout.Row {
                    Text("\u2022  ", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = StyledTextRenderer.toAnnotatedString(item, Color.Unspecified),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        is StudyBlock.NumberedList -> Column {
            block.items.forEachIndexed { i, item ->
                androidx.compose.foundation.layout.Row {
                    Text("${i + 1}. ", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = StyledTextRenderer.toAnnotatedString(item, Color.Unspecified),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        is StudyBlock.Divider -> HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        is StudyBlock.Note -> Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).padding(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Nota", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = StyledTextRenderer.toAnnotatedString(block.text, Color.Unspecified),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        is StudyBlock.Reflection -> Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).padding(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (!block.prompt.isNullOrBlank()) {
                    Text(block.prompt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                Text(
                    text = StyledTextRenderer.toAnnotatedString(block.text, Color.Unspecified),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
        is StudyBlock.Callout -> Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).padding(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("! Destacado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = StyledTextRenderer.toAnnotatedString(block.text, Color.Unspecified),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        is StudyBlock.Verse -> Column {
            Text(block.reference.displayShort(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                text = StyledTextRenderer.toAnnotatedString(block.primaryText, Color.Unspecified),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (block.compareText != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = StyledTextRenderer.toAnnotatedString(block.compareText, Color.Unspecified),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
        is StudyBlock.Table -> Column {
            block.rows.forEach { row ->
                androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                    row.cells.forEach { cell ->
                        Text(
                            text = StyledTextRenderer.toAnnotatedString(cell, Color.Unspecified),
                            modifier = Modifier.weight(1f).padding(4.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        is StudyBlock.PageBreak -> HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
    }
}

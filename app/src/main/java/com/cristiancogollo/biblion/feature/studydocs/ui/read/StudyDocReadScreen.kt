package com.cristiancogollo.biblion.feature.studydocs.ui.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.toTextAlign
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.toAnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDocReadScreen(
    viewModel: StudyDocViewModel,
    remoteId: String? = null,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(remoteId) {
        if (remoteId != null) viewModel.loadByRemoteId(remoteId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.doc.title.ifBlank { "Sin titulo" }, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                },
            )
        },
    ) { padding ->
        val blocks = uiState.doc.blocks
        if (blocks.all { it.plainText().isBlank() }) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Documento vacio",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(blocks, key = { it.id.value }) { block ->
                    ReadBlockRender(block)
                }
            }
        }
    }
}

@Composable
private fun ReadBlockRender(block: StudyBlock) {
    when (block) {
        is StudyBlock.Paragraph -> Text(
            text = block.text.toAnnotatedString(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = block.fontSize.sp,
                textAlign = block.alignment.toTextAlign(),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        is StudyBlock.Heading -> Text(
            text = block.text.toAnnotatedString(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = block.fontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = block.alignment.toTextAlign(),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        is StudyBlock.BulletList -> Column(modifier = Modifier.fillMaxWidth()) {
            block.items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Text("\u2022 ")
                    Text(
                        text = item.toAnnotatedString(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = block.fontSize.sp,
                            textAlign = block.alignment.toTextAlign(),
                        ),
                    )
                }
            }
        }
        is StudyBlock.OrderedList -> Column(modifier = Modifier.fillMaxWidth()) {
            block.items.forEachIndexed { i, item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Text("${i + 1}. ")
                    Text(
                        text = item.toAnnotatedString(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = block.fontSize.sp,
                            textAlign = block.alignment.toTextAlign(),
                        ),
                    )
                }
            }
        }
        is StudyBlock.Verse -> Column {}
is StudyBlock.Quote -> Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = block.text.toAnnotatedString(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = block.fontSize.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = block.alignment.toTextAlign(),
                ),
                modifier = Modifier.padding(start = 16.dp),
            )
            if (!block.attribution.isNullOrBlank()) {
                Text(
                    text = "\u2014 ${block.attribution}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

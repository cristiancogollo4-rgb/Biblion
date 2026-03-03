package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.RichTextEditor

@Composable
fun StudyEditorScreen(
    viewModel: StudyViewModel,
    onClose: () -> Unit,
    onFocusModeChanged: (Boolean) -> Unit
) {
    val ui by viewModel.state.collectAsState()
    val richState = rememberRichTextState()
    var title by remember(ui.selectedStudyId) { mutableStateOf(ui.title) }

    LaunchedEffect(ui.richHtml) {
        if (richState.toHtml() != ui.richHtml) {
            richState.setHtml(ui.richHtml)
        }
    }

    LaunchedEffect(richState.toHtml()) {
        viewModel.process(StudyIntent.UpdateRichHtml(richState.toHtml()))
    }

    LaunchedEffect(ui.pendingCitations.size) {
        viewModel.consumePendingCitations().forEach { request ->
            if (request.includeFullText) {
                richState.addText("\n🔖 ${request.reference}\n${request.text}\n")
            } else {
                richState.addText(" ${request.reference} ")
            }
        }
    }

    LaunchedEffect(ui.focusMode) {
        onFocusModeChanged(ui.focusMode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ESCRITORIO DEL TEÓLOGO", style = MaterialTheme.typography.titleMedium, color = BiblionNavy)
            Row {
                IconButton(onClick = {
                    viewModel.process(StudyIntent.ToggleFocusMode(!ui.focusMode))
                }) {
                    Icon(if (ui.focusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = "Focus")
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        }

        NotebookStrip(ui = ui, onIntent = viewModel::process)

        androidx.compose.foundation.text.BasicTextField(
            value = title,
            onValueChange = {
                title = it
                viewModel.process(StudyIntent.UpdateTitle(it))
            },
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Toolbar(
            onBold = { richState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
            onItalic = { richState.toggleSpanStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) },
            onOrdered = { richState.toggleOrderedList() },
            onUnordered = { richState.toggleUnorderedList() },
            onUndo = { viewModel.process(StudyIntent.Undo) },
            onRedo = { viewModel.process(StudyIntent.Redo) },
            onAudio = { viewModel.process(StudyIntent.AddAudioBlock("audio://nuevo", "Nota de voz")) },
            onImage = { viewModel.process(StudyIntent.AddImageBlock("image://nueva", "Imagen de apoyo")) }
        )

        RichTextEditor(
            state = richState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp)
        )

        BlockPreview(ui.blocks)
    }
}

@Composable
private fun NotebookStrip(ui: StudyUiState, onIntent: (StudyIntent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ui.notebooks.forEach { notebook ->
            AssistChip(
                onClick = { onIntent(StudyIntent.SelectNotebook(notebook.id)) },
                label = { Text(notebook.title) }
            )
        }
    }
}

@Composable
private fun Toolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onOrdered: () -> Unit,
    onUnordered: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAudio: () -> Unit,
    onImage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(onClick = onBold) { Icon(Icons.Default.FormatBold, contentDescription = "Bold") }
        IconButton(onClick = onItalic) { Icon(Icons.Default.FormatItalic, contentDescription = "Italic") }
        IconButton(onClick = onOrdered) { Icon(Icons.Default.FormatListNumbered, contentDescription = "OL") }
        IconButton(onClick = onUnordered) { Icon(Icons.Default.FormatListBulleted, contentDescription = "UL") }
        IconButton(onClick = onUndo) { Icon(Icons.Default.Undo, contentDescription = "Undo") }
        IconButton(onClick = onRedo) { Icon(Icons.Default.Redo, contentDescription = "Redo") }
        IconButton(onClick = onAudio) { Icon(Icons.Default.Mic, contentDescription = "Audio") }
        IconButton(onClick = onImage) { Icon(Icons.Default.Image, contentDescription = "Imagen") }
    }
    HorizontalDivider()
}

@Composable
private fun BlockPreview(blocks: List<StudyBlockNode>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        blocks.takeLast(3).forEach { block ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                when (block) {
                    is StudyBlockNode.Audio -> Text("🎙️ ${block.title}", modifier = Modifier.padding(8.dp))
                    is StudyBlockNode.Image -> Text("🖼️ ${block.caption}", modifier = Modifier.padding(8.dp))
                    is StudyBlockNode.Citation -> Text("📖 ${block.reference.display}", modifier = Modifier.padding(8.dp))
                    is StudyBlockNode.RichText -> {
                        if (block.references.isNotEmpty()) {
                            Text(
                                text = "Referencias detectadas: " + block.references.joinToString { it.display },
                                modifier = Modifier.padding(8.dp),
                                color = BiblionNavy
                            )
                        } else {
                            Spacer(Modifier.height(1.dp))
                        }
                    }
                }
            }
        }
    }
}

package com.cristiancogollo.biblion.feature.bibi.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyEditorUiState
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc

@Composable
fun BibiStudyPanel(
    editorState: StudyEditorUiState,
    currentUserName: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BibiChatPanel(
        viewModelKey = "bibi-study",
        mode = "study",
        greeting = "Soy Bibi. Puedo ayudarte a comprender, conectar y desarrollar ideas mientras trabajas en tu enseñanza.",
        initialSuggestions = listOf(
            BibiSuggestion("Crear un bosquejo", "Crea un bosquejo para esta enseñanza", isAi = true),
            BibiSuggestion("Sugerir aplicaciones", "Sugiere aplicaciones prácticas", isAi = true),
            BibiSuggestion("Buscar conexiones", "Busca referencias bíblicas relacionadas", isAi = true),
        ),
        bibiContext = buildStudyBibiContext(editorState.doc, editorState.activeBlockId),
        currentUserName = currentUserName,
        subtitle = "Asistente bíblico · Solo consulta",
        placeholder = "Pregunta a Bibi...",
        onClose = onClose,
        modifier = modifier,
    )
}

internal fun buildStudyBibiContext(
    doc: StudyDoc,
    activeBlockId: BlockId?,
): BibiContext.Study {
    val verseBlocks = doc.blocks.filterIsInstance<StudyBlock.Verse>()
    val passages = verseBlocks.map { block ->
        BibiPassage(
            book = block.bookId,
            chapter = block.chapter,
            verse = block.selectedVerseNumbers().firstOrNull() ?: block.verseStart,
            text = block.contents[block.sourceVersion].orEmpty(),
        )
    }
    return BibiContext.Study(
        documentId = doc.remoteId ?: doc.id.value,
        title = doc.title,
        tags = doc.metadata.tags,
        selectedText = doc.blocks
            .firstOrNull { it.id == activeBlockId }
            ?.plainText()
            .orEmpty(),
        outline = doc.blocks
            .map { it.plainText().trim() }
            .filter { it.isNotEmpty() }
            .take(20),
        notes = emptyList(),
        passages = passages,
        bibleVersion = verseBlocks.firstOrNull()?.sourceVersion?.ifBlank { "rv1960" } ?: "rv1960",
    )
}

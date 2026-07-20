package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.PageDimensions
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocumentZoomState
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.rememberDocumentZoomState

/**
 * Vista paginada estilo Word/Google Docs para el modo estudio.
 *
 * 1. Mide el contenido con [PaginationEngine.paginate] usando [rememberTextMeasurer].
 * 2. Cada pagina se renderiza como una [Card] fija de [LETTER_WIDTH] x [LETTER_HEIGHT].
 * 3. El zoom visual se aplica via [LocalDensity]; la paginacion NO se recalcula en zoom
 *    (mide en density = 1.0 del papel, la virtual density del render escala al usuario).
 * 4. Scroll vertical continuo (Column + verticalScroll) sobre paginas apiladas con gap
 *    entre hojas. Se usa Column y NO LazyColumn para que el RichTextState del bloque
 *    activo no se libere al reciclar items fuera de pantalla.
 *
 * El modelo StudyDoc/StudyBlock/StudyOp/StudyDocEngine NO se modifica: la paginacion
 * es estrictamente visual.
 */
@Composable
fun PaginatedSheet(
    blocks: List<StudyBlock>,
    isEditing: Boolean,
    modifier: Modifier = Modifier,
    zoomState: DocumentZoomState = rememberDocumentZoomState(),
    contentFragmentRenderer: @Composable (PageFragment, Int) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val pageWidthPx = with(density) { PageDimensions.LETTER_WIDTH.toPx() }
    val pageHeightPx = with(density) { PageDimensions.LETTER_HEIGHT.toPx() }

    val pages = remember(blocks, pageWidthPx, pageHeightPx) {
        val result = PaginationEngine.paginate(
            blocks = blocks,
            pageWidthPx = pageWidthPx,
            pageHeightPx = pageHeightPx,
            textMeasurer = textMeasurer,
            density = Density(1f),
        )
        Log.d("BIBLION_STUDY", "PaginatedSheet paginate blocks=${blocks.size} pages=${result.size} fragments=${result.sumOf { it.fragments.size }}")
        result
    }

    val virtualDensity = Density(density.density * zoomState.zoom, density.fontScale)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (pages.isEmpty()) {
            Text(
                text = "Documento sin contenido paginable",
                modifier = Modifier.padding(16.dp),
            )
            return@Box
        }
        CompositionLocalProvider(LocalDensity provides virtualDensity) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                pages.forEach { page ->
                    PageCard(page, contentFragmentRenderer)
                }
            }
        }
    }
}

@Composable
private fun PageCard(
    page: Page,
    contentFragmentRenderer: @Composable (PageFragment, Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(PageDimensions.LETTER_WIDTH)
            .height(PageDimensions.LETTER_HEIGHT),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        page.fragments.forEachIndexed { idx, fragment ->
            contentFragmentRenderer(fragment, idx)
        }
    }
}

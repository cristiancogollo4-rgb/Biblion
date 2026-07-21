package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.PageDimensions
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocumentZoomState
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.rememberDocumentZoomState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Vista paginada estilo Word/Google Docs para el modo estudio.
 *
 * 1. Mide el contenido con [PaginationEngine.paginate] usando [rememberTextMeasurer]
 *    en la MISMA densidad en la que se renderiza la hoja (sin zoom), para que
 *    `pageWidthPx`/`pageHeightPx` (en px fisicos de la hoja) coincidan con
 *    `LETTER_WIDTH`/`LETTER_HEIGHT` (en dp).
 * 2. Cada pagina se renderiza como una [Card] fija de [LETTER_WIDTH] x [LETTER_HEIGHT]
 *    con [PAGE_MARGIN] interno.
 * 3. El zoom visual se aplica via [LocalDensity] virtual (con [animateFloatAsState]
 *    para suavizar la transicion); la paginacion NO se recalcula en zoom.
 * 4. Scroll vertical continuo (Column + verticalScroll) sobre paginas apiladas.
 * 5. Pinch-to-zoom con 2 dedos: detector custom con [awaitEachGesture] en
 *    [PointerEventPass.Initial] sobre el Box (padre). Solo consume eventos cuando
 *    hay 2+ punteros, de modo que con 1 dedo el evento pasa al scroll del Column.
 * 6. Doble-tap reset a 100% (solo en modo lectura).
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

    val pages by produceState(
        initialValue = emptyList(),
        key1 = blocks,
        key2 = pageWidthPx,
        key3 = pageHeightPx,
    ) {
        value = withContext(Dispatchers.Default) {
            PaginationEngine.paginate(
                blocks = blocks,
                pageWidthPx = pageWidthPx,
                pageHeightPx = pageHeightPx,
                textMeasurer = textMeasurer,
                density = density,
            )
        }
    }

    // Zoom suavizado para evitar saltos bruscos y acumular drift.
    val animatedZoom by animateFloatAsState(
        targetValue = zoomState.zoom,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "zoom",
    )

    val virtualDensity = androidx.compose.ui.unit.Density(
        density.density * animatedZoom,
        density.fontScale,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            // Pinch: procesa en Initial pass (antes que el scroll del hijo).
            // Solo consume cuando hay 2+ punteros. 1 dedo pasa al scroll.
            // Si se levanta 1 dedo durante el pinch, desactiva zoom y el scroll
            // se reanuda automaticamente.
            .pointerInput(zoomState) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var zoomActive = false
                    var cumulativeZoom = zoomState.zoom
                    var event = awaitPointerEvent(PointerEventPass.Initial)
                    while (event.changes.any { it.pressed }) {
                        if (event.changes.size >= 2) {
                            if (!zoomActive) {
                                zoomActive = true
                                cumulativeZoom = zoomState.zoom
                            }
                            val zoomDelta = event.calculateZoom()
                            if (zoomDelta != 1f) {
                                cumulativeZoom = (cumulativeZoom * zoomDelta).coerceIn(
                                    DocumentZoomState.MIN_ZOOM,
                                    DocumentZoomState.MAX_ZOOM,
                                )
                                zoomState.set(cumulativeZoom)
                            }
                            event.changes.forEach { it.consume() }
                        } else {
                            zoomActive = false
                        }
                        event = awaitPointerEvent(PointerEventPass.Initial)
                    }
                }
            }
            // Doble-tap reset: solo en modo lectura (Main pass, despues del scroll)
            .pointerInput(isEditing) {
                if (!isEditing) {
                    detectTapGestures(onDoubleTap = { zoomState.reset() })
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        if (pages.isEmpty()) {
            Text(
                text = "Documento sin contenido paginable",
                modifier = Modifier.padding(16.dp),
            )
        } else {
            CompositionLocalProvider(LocalDensity provides virtualDensity) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PageDimensions.PAGE_MARGIN),
        ) {
            page.fragments.forEachIndexed { idx, fragment ->
                contentFragmentRenderer(fragment, idx)
            }
        }
    }
}

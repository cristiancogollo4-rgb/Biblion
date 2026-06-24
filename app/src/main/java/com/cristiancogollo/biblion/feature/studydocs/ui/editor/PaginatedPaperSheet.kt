package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

private val DeskGapColor = Color(0xFFE8EAED)
private const val BlocksPerPage = 30

/**
 * Renderiza el documento como paginas carta apiladas verticalmente.
 *
 * Cada pagina contiene hasta [BlocksPerPage] bloques. Entre paginas
 * hay un gap gris de 16dp. El zoom se aplica externamente via un
 * wrapper Box con graphicsLayer.
 */
@Composable
fun PaginatedPaperSheet(
    blocks: List<StudyBlock>,
    modifier: Modifier = Modifier,
    renderBlock: @Composable (Int, StudyBlock) -> Unit,
) {
    val pages = blocks.chunked(BlocksPerPage)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 32.dp),
    ) {
        itemsIndexed(pages) { pageIndex, pageBlocks ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                PaperSheet { innerModifier ->
                    Column(
                        modifier = innerModifier
                            .fillMaxSize()
                            .padding(vertical = 80.dp, horizontal = 64.dp),
                    ) {
                        pageBlocks.forEachIndexed { blockIndex, block ->
                            val globalIndex = pageIndex * BlocksPerPage + blockIndex
                            renderBlock(globalIndex, block)
                        }
                    }
                }
            }

            if (pageIndex < pages.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(DeskGapColor),
                )
            }
        }
    }
}

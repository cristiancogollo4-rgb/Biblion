package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

private val DeskGapColor = Color(0xFFE8EAED)

/**
 * Renderiza el documento como paginas carta apiladas verticalmente.
 *
 * La paginacion se calcula por altura estimada real del contenido,
 * no por conteo bruto de bloques.
 */
@Composable
fun PaginatedPaperSheet(
    blocks: List<StudyBlock>,
    modifier: Modifier = Modifier,
    renderBlock: @Composable (Int, StudyBlock) -> Unit,
) {
    val pagination = rememberDocumentPagination(blocks)

    Column(modifier = modifier) {
        var runningIndex = 0
        pagination.pages.forEachIndexed { pageIndex, pageBlocks ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                PaperSheet { innerModifier ->
                    Column(
                        modifier = innerModifier
                            .fillMaxSize()
                            .padding(
                                vertical = DocConfig.PageContentVerticalPadding,
                                horizontal = DocConfig.PagePadding,
                            ),
                    ) {
                        pageBlocks.forEachIndexed { blockIndex, block ->
                            val globalIndex = runningIndex + blockIndex
                            renderBlock(globalIndex, block)
                        }
                    }
                }
            }

            runningIndex += pageBlocks.size

            if (pageIndex < pagination.pages.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DocConfig.PageGap)
                        .background(DeskGapColor),
                )
            }
        }
    }
}

package com.cristiancogollo.biblion.feature.studydocs.ui.list

import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.PageDimensions
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocSyncStatus
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.studyTagDisplayName
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginationEngine
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class TeachingCardPageCountKey(
    val documentId: String,
    val version: Int,
    val updatedAt: Long,
    val pageWidthPx: Int,
    val pageHeightPx: Int,
    val fontScaleBits: Int,
)

private val teachingCardPageCountCache =
    LruCache<TeachingCardPageCountKey, Int>(64)

private val teachingCardPaginationDispatcher =
    Dispatchers.Default.limitedParallelism(1)

private fun cachedPageCount(key: TeachingCardPageCountKey): Int? =
    synchronized(teachingCardPageCountCache) {
        teachingCardPageCountCache.get(key)
    }

private fun cachePageCount(key: TeachingCardPageCountKey, pageCount: Int) {
    synchronized(teachingCardPageCountCache) {
        teachingCardPageCountCache.put(key, pageCount)
    }
}

/**
 * Tarjeta de ensenanza con el visual de v1 (EnsenanzaCard):
 * - Indicator stripe vertical a la izquierda segun el primer tag de proposito
 * - Avatar circular con icono segun la categoria
 * - Badge del proposito real, estado de la ensenanza y estado de sincronizacion
 * - Titulo en bold
 * - Fecha con icono
 * - Preview del contenido (3 lineas)
 * - Tag chips (hasta 6 + indicador "+N")
 * - Menu de mas opciones (3 puntos)
 *
 * @param doc documento de estudio v2 a renderizar
 * @param onOpen abre el lector del documento
 * @param onEdit abre el editor v2 en modo edicion
 * @param onEditMetadata abre el dialogo para editar titulo y tags
 * @param onShare abre las opciones de exportacion
 * @param onDelete elimina el documento
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeachingCard(
    doc: StudyDoc,
    syncStatus: StudyDocSyncStatus? = null,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onEditMetadata: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val purposeTag = doc.metadata.tags.firstOrNull { it in DocTagGroups.PURPOSE_TAGS }
    val stateTag = doc.metadata.tags.firstOrNull { it in DocTagGroups.STATE_TAGS }
    val secondaryTags = doc.metadata.tags.filterNot { it == purposeTag || it == stateTag }
    val isPredicacion = purposeTag == "predicacion"
    val categoryColor = if (isPredicacion) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.tertiary
    val badgeBg = if (isPredicacion) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.tertiaryContainer
    val badgeText = purposeTag?.let { studyTagDisplayName(it) }
        ?: stringResource(R.string.study_library_uncategorized)

    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val dateText = dateFormat.format(Date(doc.updatedAt))
    val preview = remember(doc.blocks) {
        doc.plainText()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .take(220)
    }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val pageWidthPx = with(density) {
        (PageDimensions.LETTER_WIDTH - PageDimensions.PAGE_MARGIN * 2).toPx()
    }
    val pageHeightPx = with(density) {
        (
            PageDimensions.LETTER_HEIGHT -
                PageDimensions.PAGE_MARGIN * 2 -
                PageDimensions.FOOTER_HEIGHT
            ).toPx()
    }
    val pageCountKey = remember(
        doc.id.value,
        doc.version,
        doc.updatedAt,
        pageWidthPx,
        pageHeightPx,
        density.fontScale,
    ) {
        TeachingCardPageCountKey(
            documentId = doc.id.value,
            version = doc.version,
            updatedAt = doc.updatedAt,
            pageWidthPx = pageWidthPx.toInt(),
            pageHeightPx = pageHeightPx.toInt(),
            fontScaleBits = density.fontScale.toBits(),
        )
    }
    val pageCount by produceState(
        initialValue = cachedPageCount(pageCountKey) ?: 1,
        key1 = pageCountKey,
    ) {
        cachedPageCount(pageCountKey)?.let { cached ->
            value = cached
            return@produceState
        }

        value = withContext(teachingCardPaginationDispatcher) {
            PaginationEngine.paginate(
                blocks = doc.blocks,
                pageWidthPx = pageWidthPx,
                pageHeightPx = pageHeightPx,
                textMeasurer = textMeasurer,
                density = density,
            ).size.also { count ->
                cachePageCount(pageCountKey, count)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(BiblionGoldPrimary)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier
                        .width(56.dp)
                        .height(72.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, BiblionGoldSoft.copy(alpha = 0.55f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 7.dp)
                                .width(2.dp)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                                .background(BiblionGoldSoft.copy(alpha = 0.55f))
                        )
                        Icon(
                            imageVector = if (isPredicacion) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.AutoMirrored.Filled.MenuBook
                            },
                            contentDescription = null,
                            tint = BiblionGoldPrimary,
                            modifier = Modifier.size(25.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f, fill = false),
                            color = badgeBg,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = badgeText.uppercase(),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                color = categoryColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.study_library_more_options),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.study_library_edit)) },
                                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.study_library_tags)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEditMetadata()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.study_library_share)) },
                                    leadingIcon = { Icon(Icons.Filled.Share, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onShare()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.study_library_delete)) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = doc.title.ifBlank { "Sin título" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = stringResource(R.string.study_library_updated, dateText),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (stateTag != null || syncStatus != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (stateTag != null) {
                                Text(
                                    text = studyTagDisplayName(stateTag),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (syncStatus != null) {
                                Text(
                                    text = stringResource(
                                        when (syncStatus) {
                                            StudyDocSyncStatus.LOCAL_ONLY -> R.string.study_library_sync_local
                                            StudyDocSyncStatus.PENDING -> R.string.study_library_sync_pending
                                            StudyDocSyncStatus.SYNCED -> R.string.study_library_sync_done
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (preview.isNotBlank()) {
                        Text(
                            text = preview,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                        )
                    }

                    if (secondaryTags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            secondaryTags.take(3).forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(7.dp),
                                ) {
                                    Text(
                                        text = "#${studyTagDisplayName(tag)}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (secondaryTags.size > 3) {
                                Surface(
                                    color = BiblionGoldSoft.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(7.dp),
                                ) {
                                    Text(
                                        text = "+${secondaryTags.size - 3}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        color = BiblionGoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (pageCount == 1) stringResource(R.string.study_library_pages_one)
                                else stringResource(R.string.study_library_pages_many, pageCount),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.study_library_read),
                            style = MaterialTheme.typography.labelMedium,
                            color = BiblionGoldPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = BiblionGoldPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

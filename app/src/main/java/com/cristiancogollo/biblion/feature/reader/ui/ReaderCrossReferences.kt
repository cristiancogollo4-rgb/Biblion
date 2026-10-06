package com.cristiancogollo.biblion.feature.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.bibi.engine.CrossReferenceTarget
import com.cristiancogollo.biblion.feature.bibi.engine.CrossReferenceVoteEngine
import com.cristiancogollo.biblion.feature.bibi.model.RelatedVerse
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import kotlinx.coroutines.CancellationException

@Immutable
data class ReaderVerseReference(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val versionKey: String,
    val pane: Int? = null,
) {
    val label: String get() = "$book $chapter:$verse"
}

@Composable
fun ReaderCrossReferenceIcon(
    reference: ReaderVerseReference,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = stringResource(R.string.reader_cross_reference_icon, reference.label),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderCrossReferencesSheet(
    source: ReaderVerseReference,
    onDismiss: () -> Unit,
    onOpenPassage: (RelatedVerse) -> Unit,
    onAskBibi: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var targets by remember(source) { mutableStateOf<List<CrossReferenceTarget>?>(null) }
    var loadFailed by remember(source) { mutableStateOf(false) }
    var reloadRequest by remember(source) { mutableIntStateOf(0) }

    LaunchedEffect(source, reloadRequest) {
        targets = null
        loadFailed = false
        try {
            targets = CrossReferenceVoteEngine.getReferenceTargets(
                context = context,
                book = source.book,
                chapter = source.chapter,
                verse = source.verse,
                versionKey = source.versionKey,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_cross_references_title, source.label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 48.dp)
                        .background(BiblionGoldPrimary, RoundedCornerShape(2.dp)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = source.versionKey.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = BiblionGoldPrimary,
                    )
                    Text(
                        text = source.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onAskBibi != null) {
                TextButton(onClick = onAskBibi) {
                    Text(stringResource(R.string.reader_cross_references_ask_bibi))
                }
            }
            when {
                loadFailed -> {
                    Text(stringResource(R.string.reader_cross_references_error))
                    TextButton(onClick = { reloadRequest++ }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                targets == null -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.reader_cross_references_loading))
                }
                targets.isNullOrEmpty() -> {
                    Text(stringResource(R.string.reader_cross_references_empty))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            items = targets.orEmpty(),
                            key = { index, target -> "$index-${target.reference}" },
                        ) { _, target ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = target.passage != null) {
                                        target.passage?.let(onOpenPassage)
                                    },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = target.passage?.reference ?: target.reference,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = target.passage?.text
                                            ?: stringResource(R.string.reader_cross_references_text_unavailable),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (target.passage != null) {
                                        Text(
                                            text = stringResource(R.string.reader_cross_references_open),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

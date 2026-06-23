package com.cristiancogollo.biblion.feature.search

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.bibi.RelatedVerse
import com.cristiancogollo.biblion.feature.bibi.TopicEngine
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tarjeta expandible de un tema canonico. Muestra nombre, categoria y
 * descripcion. Al expandirse, carga TODOS los versiculos relacionados al
 * tema desde la DB; si superan [INITIAL_VERSE_COUNT], muestra los primeros
 * y un boton "Ver mas" para expandir la lista.
 *
 * Optimizaciones anti-bloqueo:
 * - Solo se cargan versiculos cuando `expanded=true`.
 * - Se usa `produceState` para deduplicar la carga automaticamente.
 * - Si el tema cambia o se colapsa, no se vuelve a cargar.
 * - Try/catch para evitar crashes si la DB no esta lista.
 *
 * @param topic tema canonico a mostrar
 * @param expanded si la tarjeta esta expandida (controlado por el padre)
 * @param onToggleExpansion callback al tocar la cabecera para expandir/colapsar
 * @param onVerseClick callback al tocar un versiculo (navega al Reader)
 * @param topicColor color identitario del tema
 */
@Composable
fun ExpandableTopicCard(
    topic: TopicHit,
    expanded: Boolean,
    onToggleExpansion: () -> Unit,
    onVerseClick: (RelatedVerse) -> Unit,
    topicColor: Color
) {
    val context = LocalContext.current
    val currentTopic by rememberUpdatedState(topic)
    var showAllVerses by remember(currentTopic.slug) { mutableStateOf(false) }

    val versesState by produceState<VersesState>(
        initialValue = VersesState.Idle,
        key1 = currentTopic.slug,
        key2 = expanded
    ) {
        if (expanded) {
            value = VersesState.Loading
            value = try {
                val result = withContext(Dispatchers.IO) {
                    TopicEngine.getVersesForTopic(
                        context = context,
                        query = currentTopic.slug,
                        minScore = 0,
                        maxTotal = 100
                    )
                }
                if (result.isEmpty()) VersesState.Empty else VersesState.Loaded(result)
            } catch (e: Exception) {
                VersesState.Error(e.message ?: "Error")
            }
        } else {
            showAllVerses = false
            value = VersesState.Idle
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpansion),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = topicColor.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera siempre visible
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.search_topics_collapse else R.string.search_topics_expand
                    ),
                    tint = topicColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(32.dp),
                    color = topicColor.copy(alpha = 0.18f),
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = topic.nameEs.take(1).uppercase(),
                            color = topicColor,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.nameEs,
                        color = topicColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = CategoryLabels.label(topic.category),
                        color = topicColor.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (topic.verseCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.search_topics_verse_count,
                            topic.verseCount,
                            topic.verseCount
                        ),
                        color = topicColor.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Descripcion (siempre visible, completa sin ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = topic.description,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall
            )

            // Versiculos (solo si esta expandido)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = topicColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    when (val state = versesState) {
                        VersesState.Idle -> Unit
                        VersesState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = topicColor
                                )
                            }
                        }
                        VersesState.Empty -> {
                            Text(
                                text = stringResource(R.string.search_topics_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        is VersesState.Error -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        is VersesState.Loaded -> {
                            val total = state.verses.size
                            val visibleCount = if (showAllVerses || total <= INITIAL_VERSE_COUNT) {
                                total
                            } else {
                                INITIAL_VERSE_COUNT
                            }
                            val remaining = total - visibleCount
                            state.verses.take(visibleCount).forEach { verse ->
                                TopicVerseItem(
                                    verse = verse,
                                    topicColor = topicColor,
                                    onClick = { onVerseClick(verse) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (remaining > 0 && !showAllVerses) {
                                Text(
                                    text = stringResource(
                                        R.string.search_topics_see_more,
                                        remaining
                                    ),
                                    color = topicColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAllVerses = true }
                                        .padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            } else if (total > INITIAL_VERSE_COUNT && showAllVerses) {
                                Text(
                                    text = stringResource(R.string.search_topics_see_less),
                                    color = topicColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAllVerses = false }
                                        .padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val INITIAL_VERSE_COUNT = 10

/**
 * Estado sellado de la carga de versiculos para un tema.
 * Produce un unico estado final por invocacion, evitando multiples
 * emisiones y re-composiciones innecesarias.
 */
private sealed interface VersesState {
    data object Idle : VersesState
    data object Loading : VersesState
    data object Empty : VersesState
    data class Loaded(val verses: List<RelatedVerse>) : VersesState
    data class Error(val message: String) : VersesState
}

@Composable
private fun TopicVerseItem(
    verse: RelatedVerse,
    topicColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = verse.reference,
            color = topicColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = verse.text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

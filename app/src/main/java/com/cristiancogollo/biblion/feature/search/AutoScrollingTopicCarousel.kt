package com.cristiancogollo.biblion.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import kotlinx.coroutines.delay

/**
 * Carrusel horizontal de temas populares con rotacion aleatoria y
 * regla de unicidad estricta entre cards.
 *
 * Cada card usa el diseno de [PopularTopicCard] (160dp de ancho) y
 * muestra un tema individual con su nombre como titulo y la
 * descripcion debajo. Las [slotCount] cards visibles mantienen un
 * conjunto de slugs actualmente mostrados; cuando una card salta a
 * un nuevo tema, elige aleatoriamente del [pool] excluyendo los
 * slugs de las demas cards (y su propio slug actual).
 *
 * Comportamiento:
 * - [slotCount] cards visibles (default 4) con scroll horizontal.
 * - Cada card salta a un tema aleatorio cada [rotationIntervalMs].
 * - Saltos escalonados (1.1s entre cards) para que no cambien al unisono.
 * - Si todos los temas del pool estan ocupados, la card mantiene su tema.
 *
 * @param pool lista completa de temas disponibles para rotacion
 * @param slotCount cantidad de cards a mostrar (default 4)
 * @param rotationIntervalMs tiempo entre saltos aleatorios (default 5000 ms)
 * @param onTopicClick callback al tocar una card
 */
@Composable
fun AutoScrollingTopicCarousel(
    pool: List<PopularTopic>,
    slotCount: Int = 4,
    rotationIntervalMs: Long = 5000L,
    onTopicClick: (PopularTopic) -> Unit
) {
    if (pool.isEmpty()) return

    val effectiveSlots = minOf(slotCount, pool.size)
    val listState = rememberLazyListState()

    val slots: SnapshotStateList<String> = remember {
        mutableStateListOf<String>().apply {
            pool.take(effectiveSlots).forEach { add(it.slug) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = BiblionGoldPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = stringResource(R.string.search_popular_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.size(10.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = true
        ) {
            items(
                count = effectiveSlots,
                key = { it }
            ) { slotIndex ->
                val currentSlug = slots.getOrNull(slotIndex) ?: pool[slotIndex % pool.size].slug
                val currentTopic = pool.firstOrNull { it.slug == currentSlug } ?: pool[0]
                RotatingCard(
                    pool = pool,
                    selfIndex = slotIndex,
                    currentTopic = currentTopic,
                    occupied = { idx -> slots.filterIndexed { i, _ -> i != idx }.toSet() },
                    onPick = { newTopic ->
                        if (slotIndex < slots.size) {
                            slots[slotIndex] = newTopic.slug
                        }
                    },
                    rotationIntervalMs = rotationIntervalMs,
                    onTopicClick = onTopicClick
                )
            }
        }
    }
}

@Composable
private fun RotatingCard(
    pool: List<PopularTopic>,
    selfIndex: Int,
    currentTopic: PopularTopic,
    occupied: (Int) -> Set<String>,
    onPick: (PopularTopic) -> Unit,
    rotationIntervalMs: Long,
    onTopicClick: (PopularTopic) -> Unit
) {
    LaunchedEffect(selfIndex, currentTopic.slug) {
        val staggerMs = (selfIndex * 1100L) % rotationIntervalMs
        if (staggerMs > 0) delay(staggerMs)
        while (true) {
            delay(rotationIntervalMs)
            val occupiedSlugs = occupied(selfIndex) + currentTopic.slug
            val available = pool.filter { it.slug !in occupiedSlugs }
            if (available.isNotEmpty()) {
                onPick(available.random())
            }
        }
    }

    PopularTopicCard(
        topic = currentTopic,
        onClick = { onTopicClick(currentTopic) }
    )
}

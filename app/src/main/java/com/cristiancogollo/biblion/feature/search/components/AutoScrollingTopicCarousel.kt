package com.cristiancogollo.biblion.feature.search.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

/** Temas estables que el usuario puede explorar a su ritmo con scroll horizontal. */
@Composable
fun AutoScrollingTopicCarousel(
    pool: List<PopularTopic>,
    onTopicClick: (PopularTopic) -> Unit
) {
    if (pool.isEmpty()) return

    val listState = rememberLazyListState()
    val visibleTopics = pool.take(12)

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
                count = visibleTopics.size,
                key = { visibleTopics[it].slug }
            ) { index ->
                val topic = visibleTopics[index]
                PopularTopicCard(
                    topic = topic,
                    onClick = { onTopicClick(topic) },
                )
            }
        }
    }
}

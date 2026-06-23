package com.cristiancogollo.biblion.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.bibi.RelatedVerse
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

/**
 * Seccion "Temas relacionados" mostrada arriba de los resultados biblicos.
 * Contiene una lista de tarjetas expandibles (ExpandableTopicCard).
 *
 * @param topics lista de temas canonicos encontrados para la query actual
 * @param expandedSlug slug del tema actualmente expandido (o null si ninguno)
 * @param onToggleExpansion callback al tocar la cabecera de una tarjeta
 * @param onVerseClick callback al tocar un versiculo dentro de una tarjeta expandida
 */
@Composable
fun TopicsSection(
    topics: List<TopicHit>,
    expandedSlug: String?,
    onToggleExpansion: (TopicHit) -> Unit,
    onVerseClick: (RelatedVerse) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = BiblionGoldPrimary,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = stringResource(R.string.search_topics_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.padding(start = 6.dp))
            Text(
                text = "(${topics.size})",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.forEach { topic ->
                ExpandableTopicCard(
                    topic = topic,
                    expanded = topic.slug == expandedSlug,
                    onToggleExpansion = { onToggleExpansion(topic) },
                    onVerseClick = onVerseClick,
                    topicColor = topicColorFor(topic.category)
                )
            }
        }
    }
}

/**
 * Asigna un color identitario por categoria. Si la categoria no tiene
 * color definido, usa el dorado Biblion como fallback.
 */
private fun topicColorFor(category: String): Color = when (category) {
    "ATTRIBUTE_OF_GOD" -> Color(0xFF1976D2)
    "DOCTRINE" -> Color(0xFF7B1FA2)
    "CHRISTIAN_LIFE" -> Color(0xFFE57373)
    "PROPHECY" -> Color(0xFFFFB74D)
    "SIN" -> Color(0xFF8D6E63)
    "CHURCH" -> Color(0xFF26A69A)
    "PERSON" -> Color(0xFF66BB6A)
    "PLACE" -> Color(0xFF42A5F5)
    "EVENT" -> Color(0xFFAB47BC)
    "BOOK" -> Color(0xFF5C6BC0)
    "COMPARATIVE_RELIGION" -> Color(0xFFEC407A)
    else -> BiblionGoldPrimary
}

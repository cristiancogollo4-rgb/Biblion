package com.cristiancogollo.biblion.feature.studydocs.ui.outline

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.outlineTitle

@Composable
fun OutlinePanel(
    doc: StudyDoc,
    onHeadingClick: (StudyBlock.Heading, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(240.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            item {
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar outline")
                }
                HorizontalDivider()
            }
            val headings = doc.headings()
            items(headings.size) { index ->
                val h = headings[index]
                val level = h.level
                val indent = ((level - 1) * 12).dp
                TextButton(
                    onClick = { onHeadingClick(h, doc.blocks.indexOf(h)) },
                    modifier = Modifier.fillMaxWidth().padding(start = indent),
                ) {
                    Text(
                        text = h.outlineTitle.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (level == 1) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

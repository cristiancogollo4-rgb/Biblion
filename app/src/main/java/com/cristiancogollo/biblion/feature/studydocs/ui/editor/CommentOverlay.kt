package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

@Composable
fun CommentOverlay(
    comments: List<StudyBlock.Comment>,
    onCommentClick: (BlockId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(start = 8.dp)) {
        comments.forEach { comment ->
            Card(
                modifier = Modifier
                    .width(140.dp)
                    .padding(vertical = 2.dp)
                    .clickable { onCommentClick(comment.id) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = comment.text.ifBlank { "Comentario vacio" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                    )
                    Text(
                        text = "Bloque: ${comment.anchorBlockId.value.take(8)}...",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

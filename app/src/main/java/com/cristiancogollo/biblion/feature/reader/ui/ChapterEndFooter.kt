package com.cristiancogollo.biblion.feature.reader.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.R

/**
 * Cierre de capitulo cuando no hay un capitulo siguiente en la misma lista: en modo por
 * capitulos ofrece continuar; al final del libro lo anuncia.
 */
@Composable
internal fun ChapterEndFooter(
    bookName: String,
    chapter: Int,
    chapterCount: Int,
    onNextChapter: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val hasNext = chapter < chapterCount
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = accent.copy(alpha = 0.25f),
            )
            Text(
                text = if (hasNext) {
                    stringResource(R.string.reader_chapter_end, bookName, chapter)
                } else {
                    stringResource(R.string.reader_book_end, bookName)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .semantics { heading() },
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = accent.copy(alpha = 0.25f),
            )
        }

        if (hasNext) {
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(
                onClick = onNextChapter,
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(stringResource(R.string.reader_continue_with_chapter, bookName, chapter + 1))
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Inicio de capitulo en lectura continua, como en YouVersion: un numero grande y discreto,
 * con un espacio amplio que separa del capitulo anterior.
 */
@Composable
internal fun ContinuousChapterStart(
    bookName: String,
    chapter: Int,
    separateFromPrevious: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (separateFromPrevious) 72.dp else 4.dp, bottom = 8.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = chapter.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = bookName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
}

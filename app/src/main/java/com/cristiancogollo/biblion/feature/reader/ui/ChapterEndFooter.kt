package com.cristiancogollo.biblion.feature.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

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
 * Inicio de capitulo en lectura continua: el libro en versalitas discretas, el numero con la
 * tipografia del lector y una linea dorada, centrados y separados del capitulo anterior.
 */
@Composable
internal fun ContinuousChapterStart(
    bookName: String,
    chapter: Int,
    separateFromPrevious: Boolean,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
) {
    val description = "$bookName $chapter"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (separateFromPrevious) 64.dp else 8.dp, bottom = 20.dp)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = description
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = bookName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = chapter.toString(),
            fontFamily = fontFamily,
            fontSize = 40.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(BiblionGoldPrimary, RoundedCornerShape(1.dp)),
        )
    }
}

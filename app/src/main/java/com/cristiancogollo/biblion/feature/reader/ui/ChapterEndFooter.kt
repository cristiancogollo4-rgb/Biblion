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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.TextStyle
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.ui.theme.BiblionDisplay
import com.cristiancogollo.biblion.ui.theme.BiblionEyebrowStyle
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionSerif
import com.cristiancogollo.biblion.ui.theme.LiningNumerals
import com.cristiancogollo.biblion.ui.theme.biblionLabelColor

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
            style = BiblionEyebrowStyle,
            letterSpacing = 4.sp,
            color = biblionLabelColor(),
        )
        val isSerif = fontFamily == BiblionSerif
        Text(
            text = chapter.toString(),
            style = TextStyle(
                // Con la serif del lector, el numero va en la serif de exhibicion de la marca.
                fontFamily = if (isSerif) BiblionDisplay else fontFamily,
                fontSize = if (isSerif) 64.sp else 44.sp,
                lineHeight = if (isSerif) 68.sp else 52.sp,
                fontWeight = if (isSerif) FontWeight.SemiBold else FontWeight.Normal,
                fontFeatureSettings = LiningNumerals,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChapterOrnamentRule()
            Text("✦", color = BiblionGoldPrimary, fontSize = 11.sp)
            ChapterOrnamentRule()
        }
    }
}

@Composable
private fun ChapterOrnamentRule() {
    Box(
        modifier = Modifier
            .width(34.dp)
            .height(1.dp)
            .background(BiblionGoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(1.dp)),
    )
}

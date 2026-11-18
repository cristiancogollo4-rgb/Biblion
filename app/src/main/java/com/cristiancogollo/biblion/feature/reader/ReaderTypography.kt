package com.cristiancogollo.biblion.feature.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/** Titulo de seccion como en una Biblia impresa: cursiva centrada en la tipografia del lector. */
@Composable
internal fun readerSectionHeadingStyle(fontFamily: FontFamily): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        fontFamily = fontFamily,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center,
    )

/**
 * Los numeros de versiculo van en la sans del sistema: en voladita, las cifras antiguas de
 * Garamond se ven desiguales y se confunden con el texto.
 */
internal val ReaderVerseNumberFont: FontFamily = FontFamily.Default

package com.cristiancogollo.biblion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.R

/**
 * Serif de lectura y titulos (EB Garamond, licencia OFL en `assets/licenses`).
 *
 * Va incluida en la app porque algunos fabricantes reemplazan incluso `FontFamily.Serif` con la
 * fuente que el usuario elige en el sistema, y la identidad de Biblion desaparecia.
 * Los pesos 500 caen en Regular y la cursiva solo existe en peso normal.
 */
val BiblionSerif = FontFamily(
    Font(R.font.eb_garamond_regular, FontWeight.Normal),
    Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.eb_garamond_semibold, FontWeight.SemiBold),
    Font(R.font.eb_garamond_bold, FontWeight.Bold),
)

/** Serif de exhibicion (Cormorant Garamond, OFL) para la marca y los numeros de capitulo. */
val BiblionDisplay = FontFamily(
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_bold, FontWeight.Bold),
)

/** Cormorant usa cifras antiguas por defecto: un "1" parece una "I" sin esta caracteristica. */
const val LiningNumerals = "lnum"

/**
 * Etiqueta editorial en versalitas espaciadas ("VERSICULO DEL DIA", "CONTINUAR LECTURA").
 * El texto se pasa en mayusculas y el color sale de [biblionLabelColor].
 */
val BiblionEyebrowStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.5.sp,
    lineHeight = 16.sp,
    letterSpacing = 2.2.sp,
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

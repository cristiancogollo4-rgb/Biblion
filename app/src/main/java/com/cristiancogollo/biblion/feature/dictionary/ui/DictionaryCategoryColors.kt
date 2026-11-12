package com.cristiancogollo.biblion.feature.dictionary.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryCategory
import com.cristiancogollo.biblion.ui.theme.BiblionHue
import com.cristiancogollo.biblion.ui.theme.BiblionPalette
import com.cristiancogollo.biblion.ui.theme.LocalBiblionThemeMode

/** Matiz de cada categoria del diccionario, coherente con Temas y Libros. */
internal fun DictionaryCategory.hue(): BiblionHue = when (this) {
    DictionaryCategory.PERSON -> BiblionHue.BROWN
    DictionaryCategory.PLACE -> BiblionHue.SAGE
    DictionaryCategory.CONCEPT -> BiblionHue.SLATE
    DictionaryCategory.OBJECT -> BiblionHue.SKY
    DictionaryCategory.PRACTICE -> BiblionHue.LAVENDER
    DictionaryCategory.EVENT -> BiblionHue.TERRACOTTA
    DictionaryCategory.BOOK -> BiblionHue.SAND
    DictionaryCategory.OTHER -> BiblionHue.STEEL
}

/** Color de acento de la categoria para el tema actual. */
internal val DictionaryCategory.accentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = BiblionPalette.accent(hue(), LocalBiblionThemeMode.current)

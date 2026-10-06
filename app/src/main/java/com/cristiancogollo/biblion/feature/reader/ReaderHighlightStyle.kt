package com.cristiancogollo.biblion.feature.reader

import androidx.compose.ui.graphics.Color
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary

internal fun readerVerseForeground(
    highlightColor: Color,
    normalColor: Color,
    isRangeSelected: Boolean = false,
): Color = if (highlightColor.alpha > 0f && !isRangeSelected) {
    BiblionBluePrimary
} else {
    normalColor
}

package com.cristiancogollo.biblion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun Color.toStudyColorLong(): Long {
    return toArgb().toLong() and 0xFFFFFFFFL
}

fun Long.toStudyColorOrUnspecified(): Color {
    return toStudyColorOrNull() ?: Color.Unspecified
}

private fun Long.toStudyColorOrNull(): Color? {
    if (this in 0L..0xFFFFFFFFL) {
        return Color(this.toInt())
    }

    return runCatching {
        Color(this.toULong()).also { it.toArgb() }
    }.getOrNull()
}

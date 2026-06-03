package com.cristiancogollo.biblion

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

fun biblionLogoRes(isDarkTheme: Boolean): Int {
    return if (isDarkTheme) R.drawable.logo_biblion_dark else R.drawable.logo_biblion_light
}

@Composable
fun biblionLogoResForCurrentTheme(): Int {
    return biblionLogoRes(MaterialTheme.colorScheme.background.luminance() < 0.5f)
}

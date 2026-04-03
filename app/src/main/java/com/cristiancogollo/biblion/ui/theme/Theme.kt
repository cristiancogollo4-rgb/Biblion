package com.cristiancogollo.biblion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BiblionBluePrimary,
    secondary = BiblionBlueSecondary,
    tertiary = BiblionGoldPrimary,
    background = BiblionGoldSoft.copy(alpha = 0.18f),
    surface = Color.White,
    surfaceVariant = BiblionGoldSoft.copy(alpha = 0.22f),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = BiblionBluePrimary,
    onSurface = BiblionBluePrimary,
)

@Composable
fun BiblionTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

package com.cristiancogollo.biblion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BiblionBluePrimary,
    secondary = BiblionBlueSecondary,
    tertiary = BiblionGoldPrimary,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
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

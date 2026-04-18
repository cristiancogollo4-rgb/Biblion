package com.cristiancogollo.biblion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val DarkColorScheme = darkColorScheme(
    primary = BiblionGoldPrimary,
    secondary = BiblionGoldSoft,
    tertiary = Color(0xFF1A1A1A),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF151515),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onTertiary = Color.White,
    onBackground = Color(0xFFF5E8C7),
    onSurface = Color(0xFFF5E8C7),
)

@Composable
fun BiblionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

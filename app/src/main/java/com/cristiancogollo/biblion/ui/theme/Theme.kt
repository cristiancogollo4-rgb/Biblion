package com.cristiancogollo.biblion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class BiblionThemeMode {
    LIGHT,
    BLUE,
    DARK,
}

val LocalBiblionThemeMode = staticCompositionLocalOf { BiblionThemeMode.LIGHT }

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

private val BlueColorScheme = darkColorScheme(
    primary = BiblionGoldSoft,
    secondary = BiblionGoldSoft,
    tertiary = BiblionBlueSecondary,
    background = Color(0xFF0B2A4A),
    surface = Color(0xFF123F6B),
    surfaceVariant = Color(0xFF1E5B91),
    onPrimary = BiblionBluePrimary,
    onSecondary = BiblionBluePrimary,
    onTertiary = Color.White,
    onBackground = Color(0xFFF7F1E3),
    onSurface = Color(0xFFF7F1E3),
)

@Composable
fun BiblionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    mode: BiblionThemeMode? = null,
    content: @Composable () -> Unit
) {
    val resolvedMode = mode ?: if (darkTheme) BiblionThemeMode.DARK else BiblionThemeMode.LIGHT
    CompositionLocalProvider(LocalBiblionThemeMode provides resolvedMode) {
        MaterialTheme(
            colorScheme = when (resolvedMode) {
                BiblionThemeMode.LIGHT -> LightColorScheme
                BiblionThemeMode.BLUE -> BlueColorScheme
                BiblionThemeMode.DARK -> DarkColorScheme
            },
            typography = Typography,
            content = content
        )
    }
}

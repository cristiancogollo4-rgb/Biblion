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

// Todos los roles se declaran para que Material 3 no rellene con sus tonos lavanda por defecto.
internal val LightColorScheme = lightColorScheme(
    primary = BiblionBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F0),
    onPrimaryContainer = Color(0xFF0B2540),
    inversePrimary = BiblionGoldSoft,
    secondary = BiblionBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6ECF2),
    onSecondaryContainer = BiblionBluePrimary,
    tertiary = BiblionGoldPrimary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3EAD8),
    onTertiaryContainer = Color(0xFF5A4520),
    background = Color.White,
    onBackground = BiblionBluePrimary,
    surface = Color.White,
    onSurface = BiblionBluePrimary,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF46576A),
    surfaceTint = BiblionBluePrimary,
    inverseSurface = BiblionBluePrimary,
    inverseOnSurface = BiblionBlueCream,
    outline = Color(0xFF74818F),
    outlineVariant = Color(0xFFD5DCE3),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE3E7EC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainer = Color(0xFFF2F4F7),
    surfaceContainerHigh = Color(0xFFECEFF3),
    surfaceContainerHighest = Color(0xFFE6EAEF),
)

internal val DarkColorScheme = darkColorScheme(
    primary = BiblionGoldPrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF3D3320),
    onPrimaryContainer = BiblionDarkCream,
    inversePrimary = BiblionBluePrimary,
    secondary = BiblionGoldSoft,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF2A261E),
    onSecondaryContainer = BiblionDarkCream,
    tertiary = Color(0xFF1A1A1A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF262626),
    onTertiaryContainer = BiblionDarkCream,
    background = Color(0xFF000000),
    onBackground = BiblionDarkCream,
    surface = Color(0xFF0A0A0A),
    onSurface = BiblionDarkCream,
    surfaceVariant = Color(0xFF151515),
    onSurfaceVariant = Color(0xFFC2B8A3),
    surfaceTint = BiblionGoldPrimary,
    inverseSurface = BiblionDarkCream,
    inverseOnSurface = Color(0xFF1A1A1A),
    outline = Color(0xFF857D6E),
    outlineVariant = Color(0xFF36322B),
    surfaceBright = Color(0xFF2A2A2A),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0F0F0F),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF242424),
)

internal val BlueColorScheme = darkColorScheme(
    primary = BiblionGoldSoft,
    onPrimary = BiblionBluePrimary,
    primaryContainer = Color(0xFF3D4F5A),
    onPrimaryContainer = BiblionBlueCream,
    inversePrimary = BiblionBluePrimary,
    secondary = BiblionGoldSoft,
    onSecondary = BiblionBluePrimary,
    secondaryContainer = Color(0xFF1C4A75),
    onSecondaryContainer = BiblionBlueCream,
    tertiary = BiblionBlueSecondary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1C4A75),
    onTertiaryContainer = BiblionBlueCream,
    background = BiblionBlueBackground,
    onBackground = BiblionBlueCream,
    surface = BiblionBlueSurface,
    onSurface = BiblionBlueCream,
    surfaceVariant = Color(0xFF17497A),
    onSurfaceVariant = Color(0xFFC8D4E0),
    surfaceTint = BiblionGoldSoft,
    inverseSurface = BiblionBlueCream,
    inverseOnSurface = BiblionBluePrimary,
    outline = Color(0xFF8CA2B8),
    outlineVariant = Color(0xFF2C5683),
    surfaceBright = Color(0xFF1E5287),
    surfaceDim = BiblionBlueBackground,
    surfaceContainerLowest = BiblionBlueBackground,
    surfaceContainerLow = Color(0xFF0F3459),
    surfaceContainer = BiblionBlueSurface,
    surfaceContainerHigh = Color(0xFF164672),
    surfaceContainerHighest = Color(0xFF1A4D7C),
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

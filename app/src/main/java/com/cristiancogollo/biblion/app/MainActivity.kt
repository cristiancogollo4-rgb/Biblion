package com.cristiancogollo.biblion

import android.os.Bundle
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.ui.theme.BiblionTheme
import kotlinx.coroutines.delay

/**
 * Punto de entrada Android de la aplicación.
 *
 * Responsabilidad:
 * - Inicializar Compose.
 * - Aplicar el tema global de Biblion.
 * - Montar el árbol raíz de navegación (`AppNavigation`).
 *
 * Esta clase no contiene lógica de negocio; solo configuración de arranque.
 */
class MainActivity : ComponentActivity() {
    private var pendingLauncherIconDarkTheme: Boolean? = null

    /**
     * Ciclo de vida inicial del Activity.
     *
     * @param savedInstanceState estado previo de Android para restauración de proceso/actividad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirestoreSyncManager.initialize(this)
        setContent {
            BiblionApp()
        }
    }

    override fun onStop() {
        super.onStop()
        pendingLauncherIconDarkTheme?.let { isDarkTheme ->
            BiblionLauncherIconManager.applyThemeIcon(this, isDarkTheme)
            pendingLauncherIconDarkTheme = null
        }
    }

    fun scheduleLauncherIconUpdate(isDarkTheme: Boolean) {
        pendingLauncherIconDarkTheme = isDarkTheme
    }
}

@Composable
private fun MainActivity.BiblionApp() {
    val activity = this
    val systemDarkTheme = isSystemInDarkTheme()
    var darkThemeEnabled by remember {
        mutableStateOf(
            ThemePreferences.isDarkModeEnabled(
                context = activity,
                defaultValue = systemDarkTheme
            )
        )
    }
    var showLaunchScreen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(350)
        showLaunchScreen = false
    }

    DisposableEffect(systemDarkTheme) {
        val prefs = activity.getSharedPreferences(
            AppPreferencesSyncStore.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPreferencesSyncStore.KEY_DARK_MODE_ENABLED) {
                val enabled = ThemePreferences.isDarkModeEnabled(
                    context = activity,
                    defaultValue = systemDarkTheme
                )
                darkThemeEnabled = enabled
                activity.scheduleLauncherIconUpdate(enabled)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    BiblionTheme(darkTheme = darkThemeEnabled) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showLaunchScreen) {
                BiblionLaunchScreen(isDarkTheme = darkThemeEnabled)
            } else {
                AppNavigation(
                    isDarkTheme = darkThemeEnabled,
                    onToggleDarkTheme = { enabled ->
                        darkThemeEnabled = enabled
                        ThemePreferences.setDarkModeEnabled(activity, enabled)
                        activity.scheduleLauncherIconUpdate(enabled)
                    }
                )
            }
        }
    }
}

@Composable
private fun BiblionLaunchScreen(isDarkTheme: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = biblionLogoRes(isDarkTheme)),
            contentDescription = null,
            modifier = Modifier.size(144.dp),
            contentScale = ContentScale.Fit
        )
    }
}

package com.cristiancogollo.biblion

import android.os.Bundle
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.cristiancogollo.biblion.ui.theme.BiblionTheme

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
    DisposableEffect(systemDarkTheme) {
        val prefs = activity.getSharedPreferences(
            AppPreferencesSyncStore.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPreferencesSyncStore.KEY_DARK_MODE_ENABLED) {
                darkThemeEnabled = ThemePreferences.isDarkModeEnabled(
                    context = activity,
                    defaultValue = systemDarkTheme
                )
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
            AppNavigation(
                isDarkTheme = darkThemeEnabled,
                onToggleDarkTheme = { enabled ->
                    darkThemeEnabled = enabled
                    ThemePreferences.setDarkModeEnabled(activity, enabled)
                }
            )
        }
    }
}

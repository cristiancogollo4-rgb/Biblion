package com.cristiancogollo.biblion

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.ui.theme.BiblionTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

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
    internal val incomingImportUri = MutableStateFlow<Uri?>(null)

    /**
     * Ciclo de vida inicial del Activity.
     *
     * @param savedInstanceState estado previo de Android para restauración de proceso/actividad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingImportUri.value = intent.studyImportUri()
        enableEdgeToEdge()
        FirestoreSyncManager.initialize(this)
        if (savedInstanceState == null) purgeEmptyStudyDrafts()
        setContent {
            BiblionApp()
        }
    }

    /** Cold start only: a restored editor may still hold an untouched new draft. */
    private fun purgeEmptyStudyDrafts() {
        val dao = StudyDocDatabase.getInstance(applicationContext).studyDocDao()
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { StudyDocRepository(dao).purgeEmptyDrafts() }
                .onSuccess { removed ->
                    if (removed > 0) android.util.Log.i("BIBLION_STUDY", "Borradores vacios eliminados: $removed")
                }
                .onFailure { android.util.Log.w("BIBLION_STUDY", "No se pudieron limpiar borradores", it) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingImportUri.value = intent.studyImportUri()
    }

}

@Composable
private fun MainActivity.BiblionApp() {
    val activity = this
    val importUri by incomingImportUri.collectAsStateWithLifecycle()
    var themeMode by remember {
        mutableStateOf(
            ThemePreferences.getThemeMode(activity)
        )
    }

    DisposableEffect(Unit) {
        val prefs = activity.getSharedPreferences(
            AppPreferencesSyncStore.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPreferencesSyncStore.KEY_DARK_MODE_ENABLED || key == "biblion_theme_mode") {
                themeMode = ThemePreferences.getThemeMode(activity)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    BiblionTheme(darkTheme = themeMode == BiblionThemeMode.DARK, mode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            AppNavigation(
                isDarkTheme = themeMode == BiblionThemeMode.DARK,
                onToggleDarkTheme = { enabled ->
                    themeMode = if (enabled) BiblionThemeMode.DARK else BiblionThemeMode.LIGHT
                    ThemePreferences.setThemeMode(activity, themeMode)
                },
                themeMode = themeMode,
                onThemeModeChange = { selected ->
                    themeMode = selected
                    ThemePreferences.setThemeMode(activity, selected)
                },
                incomingImportUri = importUri,
                onImportUriConsumed = { incomingImportUri.value = null },
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun Intent.studyImportUri(): Uri? =
    data ?: getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

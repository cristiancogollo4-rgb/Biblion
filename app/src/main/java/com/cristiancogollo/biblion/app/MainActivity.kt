package com.cristiancogollo.biblion

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.cristiancogollo.biblion.ui.brand.BiblionIntro
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
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
    internal val showIntro = mutableStateOf(false)

    /**
     * Ciclo de vida inicial del Activity.
     *
     * @param savedInstanceState estado previo de Android para restauración de proceso/actividad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) fadeOutSystemSplash()
        showIntro.value = savedInstanceState == null
        incomingImportUri.value = intent.studyImportUri()
        enableEdgeToEdge()
        FirestoreSyncManager.initialize(this)
        if (savedInstanceState == null) purgeEmptyStudyDrafts()
        setContent {
            BiblionApp()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            splashScreen.setSplashScreenTheme(splashThemeFor(ThemePreferences.getThemeMode(this)))
        }
    }

    /** The system splash only shows the background; it dissolves into [BiblionIntro]. */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun fadeOutSystemSplash() {
        splashScreen.setOnExitAnimationListener { splashView ->
            splashView.animate()
                .alpha(0f)
                .setDuration(SPLASH_FADE_MS)
                .withEndAction { splashView.remove() }
                .start()
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

    val lightSystemBars = themeMode == BiblionThemeMode.LIGHT
    SideEffect {
        // enableEdgeToEdge() elige los iconos segun el modo del sistema: con el sistema oscuro y
        // la app en Claro quedaban blancos sobre blanco. Deben seguir el tema de la app.
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).run {
            isAppearanceLightStatusBars = lightSystemBars
            isAppearanceLightNavigationBars = lightSystemBars
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
        if (showIntro.value) {
            BiblionIntro(themeMode = themeMode, onFinished = { showIntro.value = false })
        }
    }
}

private const val SPLASH_FADE_MS = 220L

/** The next launch's system splash takes the background of the theme chosen in the app. */
private fun splashThemeFor(mode: BiblionThemeMode): Int = when (mode) {
    BiblionThemeMode.LIGHT -> R.style.Theme_Biblion_Splash_Light
    BiblionThemeMode.DARK -> R.style.Theme_Biblion_Splash_Dark
    BiblionThemeMode.BLUE -> R.style.Theme_Biblion_Splash_Blue
}

@Suppress("DEPRECATION")
private fun Intent.studyImportUri(): Uri? =
    data ?: getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

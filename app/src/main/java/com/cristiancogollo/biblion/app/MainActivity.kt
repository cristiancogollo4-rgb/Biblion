package com.cristiancogollo.biblion

import android.os.Bundle
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.ui.theme.BiblionTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    private val sharedStudyJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "nodeType"
    }

    /**
     * Ciclo de vida inicial del Activity.
     *
     * @param savedInstanceState estado previo de Android para restauración de proceso/actividad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirestoreSyncManager.initialize(this)
        handleIncomingBiblionStudy(intent)
        setContent {
            BiblionApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingBiblionStudy(intent)
    }

    private fun handleIncomingBiblionStudy(intent: Intent?) {
        @Suppress("DEPRECATION")
        val sharedStreamUri = intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        val uri = intent?.data
            ?: sharedStreamUri
            ?: return
        val action = intent?.action
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return
        lifecycleScope.launch {
            val result = runCatching {
                importBiblionStudy(uri)
            }
            result.onSuccess { title ->
                Toast.makeText(
                    this@MainActivity,
                    "Ensenanza importada: $title",
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure { error ->
                Toast.makeText(
                    this@MainActivity,
                    error.message ?: "No se pudo importar la ensenanza.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun importBiblionStudy(uri: Uri): String = withContext(Dispatchers.IO) {
        val raw = contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("No se pudo abrir el archivo compartido.")

        val shared = sharedStudyJson.decodeFromString<BiblionSharedStudyFile>(raw)
        require(shared.format == BIBLION_STUDY_SHARE_FORMAT) {
            "El archivo no es una ensenanza compatible con Biblion."
        }
        val document = sharedStudyJson.decodeFromString<SerializedStudyDocument>(shared.contentSerialized)
        val title = shared.title.trim().ifBlank { "Ensenanza importada" }
        val dao = StudyDatabase.getInstance(applicationContext).studyDao()
        val now = System.currentTimeMillis()
        val notebook = dao.getAllNotebooksForSync()
            .firstOrNull { it.deletedAt == null && it.title == "Ensenanzas importadas" }
            ?: StudyNotebookEntity(
                title = "Ensenanzas importadas",
                createdAt = now,
                updatedAt = now
            ).let { created ->
                val id = dao.insertNotebook(created)
                created.copy(id = id)
            }

        dao.insertStudy(
            StudyEntity(
                title = title,
                notebookId = notebook.id,
                notebookRemoteId = notebook.remoteId,
                contentSerialized = sharedStudyJson.encodeToString(
                    SerializedStudyDocument(
                        blocks = document.blocks,
                        globalVersion = document.globalVersion,
                        tags = document.tags
                    )
                ),
                createdAt = now,
                updatedAt = now
            )
        )
        FirestoreSyncManager.requestStudiesSync()
        title
    }

}

@Composable
private fun MainActivity.BiblionApp() {
    val activity = this
    var darkThemeEnabled by remember {
        mutableStateOf(
            ThemePreferences.isDarkModeEnabled(
                context = activity,
                defaultValue = false
            )
        )
    }
    var showLaunchScreen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(350)
        showLaunchScreen = false
    }

    DisposableEffect(Unit) {
        val prefs = activity.getSharedPreferences(
            AppPreferencesSyncStore.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPreferencesSyncStore.KEY_DARK_MODE_ENABLED) {
                val enabled = ThemePreferences.isDarkModeEnabled(
                    context = activity,
                    defaultValue = false
                )
                darkThemeEnabled = enabled
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

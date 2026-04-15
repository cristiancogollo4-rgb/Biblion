package com.cristiancogollo.biblion

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import java.util.Calendar

// Estructura para almacenar el versículo diario
/**
 * Modelo de UI para representar un versículo mostrado en pantalla principal.
 *
 * @property text contenido del versículo.
 * @property reference referencia textual (ej. "Juan 3:16").
 */
data class DailyVerse(val text: String, val reference: String)

/**
 * Pantalla principal (Home).
 *
 * Funciones clave:
 * - Renderiza AppBar y menú lateral.
 * - Muestra selector de testamento.
 * - Carga y muestra el versículo del día.
 *
 * @param navController controlador de navegación para cambiar de pantalla.
 * @param modifier modificador externo opcional para composición (por defecto sin cambios).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    loadAvailableVersions: suspend (Context) -> List<BibleVersionOption> = { ctx ->
        BibleRepository.getAvailableVersions(ctx)
    },
    loadDailyVerse: suspend (Context, String) -> DailyVerse = { ctx, versionKey ->
        getDailyVerse(ctx, versionKey)
    },
    onDailyVerseLoaded: (DailyVerse, String) -> Unit = { _, _ -> }
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Rastreo de la ruta actual para evitar re-navegación innecesaria
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var dailyVerse by remember { mutableStateOf<DailyVerse?>(null) }
    var selectedVersionKey by remember { mutableStateOf(BibleRepository.getSelectedVersionKey(context)) }
    var availableVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }

    DailyVerseLoaderEffect(
        context = context,
        selectedVersionKey = selectedVersionKey,
        availableVersions = availableVersions,
        onAvailableVersionsLoaded = { availableVersions = it },
        onDailyVerseLoaded = { verse, versionKey ->
            dailyVerse = verse
            onDailyVerseLoaded(verse, versionKey)
        },
        loadAvailableVersions = loadAvailableVersions,
        loadDailyVerse = loadDailyVerse
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RectangleShape
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                val menuOptions = listOf(
                    HomeDrawerOption.HOME,
                    HomeDrawerOption.PICK_VERSION,
                    HomeDrawerOption.MY_TEACHINGS,
                    HomeDrawerOption.DOCTRINES,
                    HomeDrawerOption.BIBLION,
                    HomeDrawerOption.STUDY_MODE,
                    HomeDrawerOption.ABOUT_US
                )

                menuOptions.forEach { option ->
                    BiblionMenuItem(
                        text = stringResource(option.labelRes),
                        onClick = {
                            scope.launch { drawerState.close() }
                            when (option) {
                                HomeDrawerOption.HOME -> {
                                    if (currentRoute != Screen.Home.route) {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                                HomeDrawerOption.MY_TEACHINGS -> {
                                    navController.navigateSingleTop(Screen.Ensenanzas.route)
                                }
                                HomeDrawerOption.STUDY_MODE -> {
                                    navController.navigateSingleTop(Screen.Reader.createRoute(studyMode = true))
                                }
                                HomeDrawerOption.PICK_VERSION -> {
                                    showVersionDialog = true
                                }
                                HomeDrawerOption.DOCTRINES,
                                HomeDrawerOption.BIBLION -> {
                                    showComingSoonDialog = true
                                }
                                HomeDrawerOption.ABOUT_US -> {
                                    showAboutDialog = true
                                }
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(80.dp))
                    Text(stringResource(R.string.reader_label))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {},
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(stringResource(R.string.sign_in), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                BiblionTopAppBar(
                    onNavigationIconClick = {
                        scope.launch { drawerState.open() }
                    },
                    onSearchIconClick = {
                        navController.navigateSingleTop(Screen.Search.route)
                    },
                    logoResId = R.drawable.logobiblion
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                TestamentSelector(selectedTab = null) { testament ->
                    navController.navigateSingleTop(Screen.Books.createRoute(testament))
                }

                HorizontalDivider(thickness = 0.5.dp)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.daily_verse_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Start
                )

                if (dailyVerse != null) {
                    DailyVerseCard(
                        verse = dailyVerse!!.text,
                        reference = dailyVerse!!.reference
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showVersionDialog) {
        BibleVersionDialog(
            versions = availableVersions,
            selectedVersionKey = selectedVersionKey,
            onVersionSelected = { selected ->
                BibleRepository.setSelectedVersionKey(context, selected.key)
                selectedVersionKey = selected.key
                showVersionDialog = false
                scope.launch { drawerState.close() }
            },
            onDismiss = { showVersionDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutBiblionDialog(onDismiss = { showAboutDialog = false })
    }

    if (showComingSoonDialog) {
        BiblionComingSoonDialog(onDismiss = { showComingSoonDialog = false })
    }
}


private enum class HomeDrawerOption(val labelRes: Int) {
    HOME(R.string.drawer_home),
    PICK_VERSION(R.string.drawer_pick_version),
    MY_TEACHINGS(R.string.drawer_my_teachings),
    DOCTRINES(R.string.drawer_doctrines),
    BIBLION(R.string.drawer_biblion),
    STUDY_MODE(R.string.drawer_study_mode),
    ABOUT_US(R.string.drawer_about_us)
}

@Composable
internal fun DailyVerseLoaderEffect(
    context: Context,
    selectedVersionKey: String,
    availableVersions: List<BibleVersionOption>,
    onAvailableVersionsLoaded: (List<BibleVersionOption>) -> Unit,
    onDailyVerseLoaded: (DailyVerse, String) -> Unit,
    loadAvailableVersions: suspend (Context) -> List<BibleVersionOption>,
    loadDailyVerse: suspend (Context, String) -> DailyVerse
) {
    val latestLoadAvailableVersions by rememberUpdatedState(loadAvailableVersions)
    val latestLoadDailyVerse by rememberUpdatedState(loadDailyVerse)
    val latestOnAvailableVersionsLoaded by rememberUpdatedState(onAvailableVersionsLoaded)
    val latestOnDailyVerseLoaded by rememberUpdatedState(onDailyVerseLoaded)

    LaunchedEffect(selectedVersionKey) {
        if (availableVersions.isEmpty()) {
            val versions = latestLoadAvailableVersions(context)
            latestOnAvailableVersionsLoaded(versions)
        }

        val currentVersion = selectedVersionKey
        val verse = latestLoadDailyVerse(context, currentVersion)
        latestOnDailyVerseLoaded(verse, currentVersion)
    }
}

/**
 * Obtiene el versículo del día de forma sincronizada para todas las versiones.
 * Guarda la referencia (libro, capítulo, versículo) y la marca de tiempo globalmente.
 * El texto se obtiene dinámicamente según la versión seleccionada.
 */
private suspend fun getDailyVerse(context: Context, versionKey: String): DailyVerse {
    val prefs = context.getSharedPreferences("BiblionAppPrefs", Context.MODE_PRIVATE)
    
    // Claves globales para la referencia del día (independiente de la versión)
    val dailyBookKey = "dailyVerseBook"
    val dailyChapterKey = "dailyVerseChapter"
    val dailyVerseNumKey = "dailyVerseNum"
    val dailyTimestampKey = "dailyVerseTimestamp_Global"
    
    val today = Calendar.getInstance()
    val lastUpdateMillis = prefs.getLong(dailyTimestampKey, 0)
    val lastUpdateCalendar = Calendar.getInstance().apply { timeInMillis = lastUpdateMillis }

    val isNewDay = today.get(Calendar.DAY_OF_YEAR) != lastUpdateCalendar.get(Calendar.DAY_OF_YEAR) ||
            today.get(Calendar.YEAR) != lastUpdateCalendar.get(Calendar.YEAR)

    val book: String
    val chapter: String
    val verse: String

    if (isNewDay) {
        try {
            val (newBook, newChapter, newVerse) = BibleRepository.getRandomVerseReference(context)
            book = newBook
            chapter = newChapter
            verse = newVerse
            
            with(prefs.edit()) {
                putString(dailyBookKey, book)
                putString(dailyChapterKey, chapter)
                putString(dailyVerseNumKey, verse)
                putLong(dailyTimestampKey, today.timeInMillis)
                apply()
            }
        } catch (e: Exception) {
            // Fallback en caso de error
            return DailyVerse(
                context.getString(R.string.daily_verse_fallback_text),
                context.getString(R.string.daily_verse_fallback_reference)
            )
        }
    } else {
        book = prefs.getString(dailyBookKey, context.getString(R.string.daily_verse_default_book)) ?: context.getString(R.string.daily_verse_default_book)
        chapter = prefs.getString(dailyChapterKey, context.getString(R.string.daily_verse_default_chapter)) ?: context.getString(R.string.daily_verse_default_chapter)
        verse = prefs.getString(dailyVerseNumKey, context.getString(R.string.daily_verse_default_verse)) ?: context.getString(R.string.daily_verse_default_verse)
    }

    // Obtener el texto del versículo para la versión actual
    return try {
        BibleRepository.getVerseText(context, versionKey, book, chapter, verse)
    } catch (e: Exception) {
        DailyVerse(context.getString(R.string.daily_verse_loading), "$book $chapter:$verse")
    }
}


@Composable
/**
 * Item reutilizable de menú lateral.
 *
 * @param text texto visible de la opción.
 * @param onClick callback invocado cuando el usuario toca la opción.
 */
fun BiblionMenuItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}

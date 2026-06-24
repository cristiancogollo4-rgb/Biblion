package com.cristiancogollo.biblion

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cristiancogollo.biblion.feature.search.SearchScope
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
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
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    currentUserName: String? = null,
    currentUserEmail: String? = null,
    isAuthenticated: Boolean = false,
    showSignedOutDialog: Boolean = false,
    onDismissSignedOutDialog: () -> Unit = {},
    onAuthActionClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
    loadAvailableVersions: suspend (Context) -> List<BibleVersionOption> = { ctx ->
        BibleRepository.getAvailableVersions(ctx)
    },
    loadDailyVerse: suspend (Context, String) -> DailyVerse = { ctx, versionKey ->
        getDailyVerse(ctx, versionKey)
    },
    onDailyVerseLoaded: (DailyVerse, String) -> Unit = { _, _ -> },
    guidedTutorial: GuidedTutorialProgress? = null,
    onGuidedTutorialNext: () -> Unit = {},
    onGuidedTutorialSkip: () -> Unit = {},
    onGuidedTutorialRestart: () -> Unit = {},
    onGuidedTutorialTargetAction: (String) -> Unit = {},
    onStartGuidedTutorial: (GuidedTutorialId) -> Unit = {}
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
    val guidedStep = guidedTutorial
        ?.currentStep()
        ?.takeIf { it.screenTarget == GuidedTutorialScreenTarget.HOME }
    val tutorialTargetBounds = remember { mutableStateMapOf<String, Rect>() }

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

    Box(modifier = modifier.fillMaxSize()) {
        ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            BiblionAppDrawer(
                drawerState = drawerState,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                currentUserName = currentUserName,
                currentUserEmail = currentUserEmail,
                isAuthenticated = isAuthenticated,
                onClose = { scope.launch { drawerState.close() } },
                onNavigateHome = {
                    if (currentRoute != Screen.Home.route) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToTeachings = {
                    navController.navigateSingleTop(StudyDocScreen.StudyDocsList.route)
                },
                onNavigateToDictionary = {
                    navController.navigateSingleTop(Screen.Search.createRoute(SearchScope.DICTIONARY))
                },
                onNavigateToStudyMode = {
                    navController.navigateSingleTop(StudyDocScreen.StudyDocsList.route)
                },
                onPickVersion = { showVersionDialog = true },
                onShowComingSoon = { showComingSoonDialog = true },
                onShowAbout = { showAboutDialog = true },
                onAuthActionClick = onAuthActionClick
            )
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
                    logoResId = biblionLogoRes(isDarkTheme)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                TestamentSelector(
                    selectedTab = null,
                    onTabSelected = { testament ->
                        onGuidedTutorialTargetAction(GuidedTutorialTargets.HOME_TESTAMENT_SELECTOR)
                        navController.navigateSingleTop(Screen.Books.createRoute(testament))
                    },
                    modifier = Modifier.guidedTutorialTarget(
                        GuidedTutorialTargets.HOME_TESTAMENT_SELECTOR,
                        tutorialTargetBounds
                    )
                )

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
                        reference = dailyVerse!!.reference,
                        modifier = Modifier.guidedTutorialTarget(
                            GuidedTutorialTargets.HOME_DAILY_VERSE,
                            tutorialTargetBounds
                        ),
                        onClick = {
                            dailyVerse!!.reference.toBibleNavigationTarget()?.let { target ->
                                navController.navigateSingleTop(
                                    Screen.Reader.createRoute(
                                        bookName = target.bookName,
                                        chapter = target.chapter,
                                        verse = target.verse
                                    )
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    GuidedTutorialOverlay(
        step = guidedStep,
        targetBounds = tutorialTargetBounds,
        onNext = onGuidedTutorialNext,
        onSkip = onGuidedTutorialSkip,
        onRestart = onGuidedTutorialRestart
    )

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
}

private data class BibleNavigationTarget(
    val bookName: String,
    val chapter: Int,
    val verse: String
)

private fun String.toBibleNavigationTarget(): BibleNavigationTarget? {
    val match = Regex("""^(.+)\s+(\d+):(\d+)$""").matchEntire(trim()) ?: return null
    val bookName = match.groupValues[1].ifBlank { return null }
    val chapter = match.groupValues[2].toIntOrNull() ?: return null
    val verse = match.groupValues[3]
    return BibleNavigationTarget(
        bookName = bookName,
        chapter = chapter,
        verse = verse
    )
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
            val (newBook, newChapter, newVerse) = BibleRepository.getRandomVerseReference(context, versionKey)
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

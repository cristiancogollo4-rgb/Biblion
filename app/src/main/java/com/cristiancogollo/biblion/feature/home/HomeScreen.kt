package com.cristiancogollo.biblion

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen
import com.cristiancogollo.biblion.feature.bibi.engine.TopicEngine
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementEvent
import com.cristiancogollo.biblion.feature.achievements.tracking.AchievementTracker
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import com.cristiancogollo.biblion.ui.theme.BiblionBlueNavigation
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionDarkNavigation
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import com.cristiancogollo.biblion.ui.theme.LocalBiblionThemeMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import java.util.Calendar
import com.cristiancogollo.biblion.ui.theme.BiblionSerif
import com.cristiancogollo.biblion.ui.theme.BiblionBlueCream
import com.cristiancogollo.biblion.ui.theme.BiblionEyebrowStyle
import com.cristiancogollo.biblion.ui.theme.biblionLabelColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp

// Estructura para almacenar el versículo diario
/**
 * Modelo de UI para representar un versículo mostrado en pantalla principal.
 *
 * @property text contenido del versículo.
 * @property reference referencia textual (ej. "Juan 3:16").
 */
data class DailyVerse(val text: String, val reference: String)

private data class HomeAnnouncement(
    val title: String,
    val description: String,
    val actionLabel: String,
    val action: HomeAnnouncementAction,
    val icon: ImageVector? = null,
    val logoResId: Int? = null,
)

private enum class HomeAnnouncementAction {
    BIBLE,
    SEARCH,
    BIBI,
    DAILY_VERSE,
    DICTIONARY,
    STUDY,
    PERSONALIZE,
}

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
    themeMode: BiblionThemeMode = if (isDarkTheme) BiblionThemeMode.DARK else BiblionThemeMode.LIGHT,
    onThemeModeChange: (BiblionThemeMode) -> Unit = {},
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
    val context = LocalContext.current
    val achievementScope = rememberCoroutineScope()

    // Rastreo de la ruta actual para evitar re-navegación innecesaria
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var dailyVerse by remember { mutableStateOf<DailyVerse?>(null) }
    var selectedVersionKey by remember { mutableStateOf(BibleRepository.getSelectedVersionKey(context)) }
    var availableVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    var lastReading by remember { mutableStateOf(AppPreferencesSyncStore.getLastReading(context)) }
    var lastReadingDescription by remember { mutableStateOf<String?>(null) }
    var selectedLandscape by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % 4) }
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

    LaunchedEffect(currentRoute) {
        lastReading = AppPreferencesSyncStore.getLastReading(context)
    }

    LaunchedEffect(lastReading) {
        lastReadingDescription = lastReading?.let { reading ->
            TopicEngine.getBySlug(context, reading.book.toHomeTopicSlug())?.description
        }
    }

    val landscapeResources = listOf(
        R.drawable.home_landscape_sunrise,
        R.drawable.home_landscape_moon,
        R.drawable.home_landscape_desert,
        R.drawable.home_landscape_olive,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BiblionTopAppBar(
                logoResId = biblionLogoRes(themeMode != BiblionThemeMode.LIGHT),
                showSearchIcon = false,
                showNavigationIcon = false,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (dailyVerse != null) {
                HomeDailyVerseCard(
                    verse = dailyVerse!!,
                    landscapeRes = landscapeResources[selectedLandscape],
                    modifier = Modifier.guidedTutorialTarget(
                        GuidedTutorialTargets.HOME_DAILY_VERSE,
                        tutorialTargetBounds
                    ),
                    onClick = {
                        dailyVerse!!.reference.toBibleNavigationTarget()?.let { target ->
                            achievementScope.launch {
                                AchievementTracker.track(
                                    context,
                                    AchievementEvent.DailyVerseOpened(
                                        book = target.bookName,
                                        chapter = target.chapter,
                                    ),
                                )
                            }
                            navController.navigateSingleTop(
                                Screen.Reader.createRoute(
                                    bookName = target.bookName,
                                    chapter = target.chapter,
                                    verse = target.verse
                                )
                            )
                        }
                    },
                )
            }

            if (lastReading != null) {
                HomeContinueReadingCard(
                    reading = lastReading!!,
                    description = lastReadingDescription,
                    onOpen = {
                        val reading = lastReading!!
                        navController.navigateSingleTop(
                            Screen.Reader.createRoute(
                                bookName = reading.book,
                                chapter = reading.chapter,
                                verse = reading.verse,
                            )
                        )
                    },
                )
            } else {
                HomeEmptyReadingCard(onOpenBible = { navController.navigateSingleTop(Screen.Books.createRoute(Testament.OLD)) })
            }

            HomeAnnouncementsCarousel(
                onAction = { action ->
                    when (action) {
                        HomeAnnouncementAction.BIBLE ->
                            navController.navigateSingleTop(Screen.Books.createRoute(Testament.OLD))
                        HomeAnnouncementAction.SEARCH ->
                            navController.navigateSingleTop(Screen.Search.route)
                        HomeAnnouncementAction.BIBI,
                        HomeAnnouncementAction.PERSONALIZE -> {
                            val reading = lastReading
                            if (reading != null) {
                                navController.navigateSingleTop(
                                    Screen.Reader.createRoute(
                                        bookName = reading.book,
                                        chapter = reading.chapter,
                                        verse = reading.verse,
                                    )
                                )
                            } else {
                                navController.navigateSingleTop(Screen.Books.createRoute(Testament.OLD))
                            }
                        }
                        HomeAnnouncementAction.DAILY_VERSE -> {
                            dailyVerse
                                ?.reference
                                ?.toBibleNavigationTarget()
                                ?.let { target ->
                                    navController.navigateSingleTop(
                                        Screen.Reader.createRoute(
                                            bookName = target.bookName,
                                            chapter = target.chapter,
                                            verse = target.verse,
                                        )
                                    )
                                }
                        }
                        HomeAnnouncementAction.DICTIONARY ->
                            navController.navigateSingleTop(Screen.Dictionary.route)
                        HomeAnnouncementAction.STUDY ->
                            navController.navigateSingleTop(StudyDocScreen.StudyDocsList.route)
                    }
                },
            )

            Spacer(Modifier.height(112.dp))
        }
    }

    GuidedTutorialOverlay(
        step = guidedStep,
        targetBounds = tutorialTargetBounds,
        onNext = onGuidedTutorialNext,
        onSkip = onGuidedTutorialSkip,
        onRestart = onGuidedTutorialRestart,
        isRestart = guidedTutorial?.isRestart == true
    )

}

private fun String.toHomeTopicSlug(): String {
    return lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace("ñ", "n")
        .replace(" ", "-")
}

@Composable
private fun HomeDailyVerseCard(
    verse: DailyVerse,
    landscapeRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = homeCardElevation(3.dp)),
        border = homeCardBorder(),
    ) {
        Box(modifier = Modifier.heightIn(min = 250.dp).fillMaxWidth()) {
            Image(
                painter = painterResource(landscapeRes),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.83f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.20f),
                        )
                    )
                )
            )
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", color = BiblionGoldPrimary, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    HomeEyebrow(stringResource(R.string.daily_verse_title))
                }
                Text(
                    text = "“${verse.text}”",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = BiblionSerif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        letterSpacing = 0.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(0.80f),
                )
                HomeEyebrow(verse.reference)
            }
        }
    }
}

@Composable
private fun HomeEmptyReadingCard(onOpenBible: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = homeCardElevation(2.dp)),
        border = homeCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeEyebrow(stringResource(R.string.home_continue_reading))
            Text(
                stringResource(R.string.home_no_recent_reading),
                fontFamily = BiblionSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
            )
            Text(
                stringResource(R.string.home_open_book_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenBible) {
                Text(stringResource(R.string.home_open_bible), color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun HomeContinueReadingCard(
    reading: LastReading,
    description: String?,
    onOpen: () -> Unit,
) {
    val chapterLabel = "${reading.book} ${reading.chapter}"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = homeCardElevation(2.dp)),
        border = homeCardBorder(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HomeEyebrow(
                    text = stringResource(R.string.home_continue_reading),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpen) {
                    Text("Ir", color = biblionLabelColor(), fontWeight = FontWeight.SemiBold)
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = biblionLabelColor(),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                HomeBookCover(bookName = reading.book)
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = chapterLabel,
                        fontFamily = BiblionSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 25.sp,
                        lineHeight = 28.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Última lectura · ${reading.book} ${reading.chapter}:${reading.verse ?: "1"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        description ?: "Retoma tu lectura y continúa explorando este libro.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = BiblionSerif,
                        fontSize = 15.5.sp,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Etiqueta editorial de Inicio: versalitas espaciadas en el dorado legible de cada tema. */
@Composable
private fun HomeEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = BiblionEyebrowStyle,
        color = biblionLabelColor(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** En Claro las tarjetas se separan del papel con un filete dorado; Azul y Oscuro conservan su relieve. */
@Composable
private fun homeCardBorder(): BorderStroke? =
    if (LocalBiblionThemeMode.current == BiblionThemeMode.LIGHT) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

@Composable
private fun homeCardElevation(default: Dp): Dp =
    if (LocalBiblionThemeMode.current == BiblionThemeMode.LIGHT) 0.dp else default

@Composable
private fun HomeBookCover(
    bookName: String,
    modifier: Modifier = Modifier,
) {
    val coverColor = when (LocalBiblionThemeMode.current) {
        BiblionThemeMode.LIGHT -> BiblionNavy
        BiblionThemeMode.BLUE -> BiblionBlueNavigation
        BiblionThemeMode.DARK -> BiblionDarkNavigation
    }
    val coverShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 11.dp,
        bottomStart = 4.dp,
        bottomEnd = 11.dp,
    )
    Box(
        modifier = modifier.size(width = 84.dp, height = 116.dp),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 78.dp, height = 108.dp),
            shape = coverShape,
            color = BiblionGoldSoft.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, BiblionGoldPrimary.copy(alpha = 0.52f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 3.dp, bottom = 5.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom,
            ) {
                repeat(3) {
                    HorizontalDivider(
                        modifier = Modifier.width(68.dp),
                        thickness = 1.dp,
                        color = BiblionNavy.copy(alpha = 0.16f),
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(width = 78.dp, height = 110.dp),
            shape = coverShape,
            color = coverColor,
            shadowElevation = 5.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(9.dp)
                        .background(Color.Black.copy(alpha = 0.16f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .offset(x = 10.dp)
                        .background(BiblionGoldSoft.copy(alpha = 0.65f)),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 7.dp, top = 12.dp, bottom = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = BiblionGoldSoft,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = bookName.uppercase(),
                        color = BiblionGoldSoft,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "BIBLION",
                        color = BiblionGoldSoft,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.7.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeAnnouncementsCarousel(
    onAction: (HomeAnnouncementAction) -> Unit,
) {
    val announcements = listOf(
        HomeAnnouncement(
            title = stringResource(R.string.home_announcement_offline_title),
            description = stringResource(R.string.home_announcement_offline_description),
            actionLabel = stringResource(R.string.home_announcement_offline_action),
            icon = Icons.Default.CloudOff,
            action = HomeAnnouncementAction.BIBLE,
        ),
        HomeAnnouncement(
            title = stringResource(R.string.home_announcement_connections_title),
            description = stringResource(R.string.home_announcement_connections_description),
            actionLabel = stringResource(R.string.home_announcement_connections_action),
            icon = Icons.Default.Hub,
            action = HomeAnnouncementAction.SEARCH,
        ),
        HomeAnnouncement(
            title = stringResource(R.string.home_announcement_bibi_title),
            description = stringResource(R.string.home_announcement_bibi_description),
            actionLabel = stringResource(R.string.home_announcement_bibi_action),
            logoResId = R.drawable.bibi_logo,
            action = HomeAnnouncementAction.BIBI,
        ),
        HomeAnnouncement(
            title = stringResource(R.string.home_announcement_daily_title),
            description = stringResource(R.string.home_announcement_daily_description),
            actionLabel = stringResource(R.string.home_announcement_daily_action),
            icon = Icons.Default.WbSunny,
            action = HomeAnnouncementAction.DAILY_VERSE,
        ),
        HomeAnnouncement(
            title = stringResource(R.string.home_announcement_dictionary_title),
            description = stringResource(R.string.home_announcement_dictionary_description),
            actionLabel = stringResource(R.string.home_announcement_dictionary_action),
            icon = Icons.Default.LocalLibrary,
            action = HomeAnnouncementAction.DICTIONARY,
        ),
        HomeAnnouncement(
            title = stringResource(R.string.home_announcement_study_title),
            description = stringResource(R.string.home_announcement_study_description),
            actionLabel = stringResource(R.string.home_announcement_study_action),
            icon = Icons.Default.EditNote,
            action = HomeAnnouncementAction.STUDY,
        ),
        HomeAnnouncement(
            title = stringResource(R.string.home_announcement_personalize_title),
            description = stringResource(R.string.home_announcement_personalize_description),
            actionLabel = stringResource(R.string.home_announcement_personalize_action),
            icon = Icons.Default.Bookmark,
            action = HomeAnnouncementAction.PERSONALIZE,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { announcements.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp,
        ) { page ->
            val announcement = announcements[page]
            val isLight = LocalBiblionThemeMode.current == BiblionThemeMode.LIGHT
            val panelColor = when (LocalBiblionThemeMode.current) {
                BiblionThemeMode.LIGHT -> BiblionNavy
                BiblionThemeMode.BLUE -> BiblionBlueNavigation
                BiblionThemeMode.DARK -> BiblionDarkNavigation
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    // En Claro el velo azul agrisaba el papel: la tarjeta queda blanca.
                                    panelColor.copy(alpha = if (isLight) 0f else 0.13f),
                                    Color.Transparent,
                                )
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 54.dp, y = (-58).dp)
                            .size(180.dp)
                            .background(
                                BiblionGoldSoft.copy(alpha = 0.08f),
                                CircleShape,
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(54.dp),
                            shape = RoundedCornerShape(17.dp),
                            color = panelColor,
                            shadowElevation = 3.dp,
                        ) {
                            if (announcement.logoResId != null) {
                                Image(
                                    painter = painterResource(announcement.logoResId),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(7.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = requireNotNull(announcement.icon),
                                    contentDescription = null,
                                    tint = BiblionGoldSoft,
                                    modifier = Modifier.padding(14.dp),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                text = announcement.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = BiblionSerif,
                                    fontSize = 24.sp,
                                    lineHeight = 27.sp,
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = announcement.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(5.dp))
                            Button(
                                onClick = { onAction(announcement.action) },
                                shape = RoundedCornerShape(50),
                                // Claro: tinta azul sobre papel, como un sello; Azul y Oscuro: boton dorado.
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLight) BiblionNavy else BiblionGoldSoft,
                                    contentColor = if (isLight) BiblionBlueCream else BiblionNavy,
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = announcement.actionLabel,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = if (isLight) BiblionGoldSoft else BiblionNavy,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            announcements.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                val indicatorDescription = stringResource(
                    R.string.home_announcement_indicator,
                    index + 1,
                    announcements.size,
                )
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 48.dp)
                        .semantics {
                            contentDescription = indicatorDescription
                            this.selected = selected
                        }
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (selected) 20.dp else 6.dp)
                            .background(
                                if (selected) BiblionGoldPrimary
                                else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            ),
                    )
                }
            }
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

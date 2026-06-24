package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cristiancogollo.biblion.feature.search.SearchScope
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import kotlinx.coroutines.launch
import com.cristiancogollo.biblion.feature.books.BookCategory
import com.cristiancogollo.biblion.feature.books.BookCategoryColors
import com.cristiancogollo.biblion.feature.books.toBookCategory
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment

// Optimizacion: Listas fuera del composable para evitar re-asignacion constante
private val oldTestamentBooks = listOf(
    "Genesis", "Exodo", "Levitico", "Numeros", "Deuteronomio", "Josue", "Jueces", "Rut",
    "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", "1 Cronicas", "2 Cronicas", "Esdras",
    "Nehemias", "Ester", "Job", "Salmos", "Proverbios", "Eclesiastes", "Cantares",
    "Isaias", "Jeremias", "Lamentaciones", "Ezequiel", "Daniel", "Oseas", "Joel", "Amos",
    "Abdias", "Jonas", "Miqueas", "Nahum", "Habacuc", "Sofonias", "Hageo", "Zacarias", "Malaquias"
)

private val newTestamentBooks = listOf(
    "Mateo", "Marcos", "Lucas", "Juan", "Hechos", "Romanos", "1 Corintios", "2 Corintios",
    "Galatas", "Efesios", "Filipenses", "Colosenses", "1 Tesalonicenses", "2 Tesalonicenses",
    "1 Timoteo", "2 Timoteo", "Tito", "Filemon", "Hebreos", "Santiago", "1 Pedro", "2 Pedro",
    "1 Juan", "2 Juan", "3 Juan", "Judas", "Apocalipsis"
)

/**
 * Pantalla de listado de libros por testamento.
 *
 * @param navController navegación para abrir lector y otras secciones.
 * @param selectedTestament testamento inicial recibido desde la pantalla anterior.
 */
@Composable
fun BooksScreen(
    navController: NavController,
    selectedTestament: Testament,
    openInStudyMode: Boolean = false,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    currentUserName: String? = null,
    currentUserEmail: String? = null,
    isAuthenticated: Boolean = false,
    showSignedOutDialog: Boolean = false,
    onDismissSignedOutDialog: () -> Unit = {},
    onAuthActionClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    guidedTutorial: GuidedTutorialProgress? = null,
    onGuidedTutorialNext: () -> Unit = {},
    onGuidedTutorialSkip: () -> Unit = {},
    onGuidedTutorialRestart: () -> Unit = {},
    onGuidedTutorialTargetAction: (String) -> Unit = {}
) {
    var showCategoryLegendSheet by remember { mutableStateOf(false) }
    var selectedCategoryForLegend by remember { mutableStateOf<BookCategory?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var selectedVersionKey by remember { mutableStateOf(BibleRepository.getSelectedVersionKey(context)) }
    var availableVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }
    var testamentDrag by remember { mutableFloatStateOf(0f) }
    val guidedStep = guidedTutorial
        ?.currentStep()
        ?.takeIf { it.screenTarget == GuidedTutorialScreenTarget.BOOKS }
    val tutorialTargetBounds = remember { mutableStateMapOf<String, Rect>() }

    var currentSelectedTestamentArg by rememberSaveable { mutableStateOf(selectedTestament.toRouteArg()) }
    val currentSelectedTestament = Testament.fromRouteArg(currentSelectedTestamentArg)

    LaunchedEffect(Unit) {
        availableVersions = BibleRepository.getAvailableVersions(context)
        selectedVersionKey = BibleRepository.getSelectedVersionKey(context)
    }

    // Optimizacion: Derivamos los datos solo cuando cambia el testamento seleccionado
    val booksToShow = remember(currentSelectedTestament) {
        if (currentSelectedTestament == Testament.OLD) oldTestamentBooks else newTestamentBooks
    }
    val bookCardMinSize = if (configuration.screenWidthDp >= 600) 140.dp else 92.dp
    
    val title = stringResource(
        R.string.books_screen_title,
        stringResource(currentSelectedTestament.labelRes)
    )

    Box(modifier = Modifier.fillMaxSize()) {
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
                            navController.navigateSingleTop(Screen.Home.route)
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
                    onShowAbout = { showAboutDialog = true },
                    onShowComingSoon = { showComingSoonDialog = true },
                    onAuthActionClick = onAuthActionClick
                )
            }
        ) {
            Scaffold(
                topBar = {
                    BiblionTopAppBar(
                        onNavigationIconClick = { scope.launch { drawerState.open() } },
                        onSearchIconClick = { navController.navigateSingleTop(Screen.Search.route) },
                        logoResId = biblionLogoRes(isDarkTheme),
                    )
                }
            ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(currentSelectedTestament) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                testamentDrag += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    testamentDrag <= -50f && currentSelectedTestament == Testament.OLD -> {
                                        currentSelectedTestamentArg = Testament.NEW.toRouteArg()
                                    }
                                    testamentDrag >= 50f && currentSelectedTestament == Testament.NEW -> {
                                        currentSelectedTestamentArg = Testament.OLD.toRouteArg()
                                    }
                                }
                                testamentDrag = 0f
                            }
                        )
                    }
            ) {
                TestamentSelector(
                    selectedTab = currentSelectedTestament,
                    onTabSelected = { currentSelectedTestamentArg = it.toRouteArg() }
                )

                HorizontalDivider(thickness = 0.5.dp, color = BiblionGoldSoft.copy(alpha = 0.35f))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val activeCategories = remember(currentSelectedTestament) {
                    if (currentSelectedTestament == Testament.OLD) {
                        listOf(
                            BookCategory.PENTATEUCO,
                            BookCategory.HISTORICO,
                            BookCategory.SAPIENCIAL,
                            BookCategory.PROFETICO
                        )
                    } else {
                        listOf(
                            BookCategory.EVANGELIO,
                            BookCategory.HISTORICO_NT,
                            BookCategory.EPISTOLA_PAULINA,
                            BookCategory.EPISTOLA_CATOLICA,
                            BookCategory.APOCALIPSIS
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeCategories) { category ->
                        val colors = BookCategoryColors.getColors(category, isDarkTheme)
                        SuggestionChip(
                            onClick = {
                                selectedCategoryForLegend = category
                                showCategoryLegendSheet = true
                            },
                            label = {
                                Text(
                                    text = category.labelEs,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = colors.bg,
                                labelColor = colors.text
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = colors.border,
                                borderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = bookCardMinSize),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Optimización: usamos key para que Compose identifique cada item y sea fluido.
                    itemsIndexed(booksToShow, key = { _, bookName -> bookName }) { index, bookName ->
                        val category = bookName.toBookCategory()
                        val colorSchema = BookCategoryColors.getColors(category, isDarkTheme)
                        BookCard(
                            bookName = bookName,
                            colorSchema = colorSchema,
                            modifier = if (index == 0) {
                                Modifier.guidedTutorialTarget(
                                    GuidedTutorialTargets.BOOKS_FIRST_BOOK,
                                    tutorialTargetBounds
                                )
                            } else {
                                Modifier
                            },
                            onClick = {
                                onGuidedTutorialTargetAction(GuidedTutorialTargets.BOOKS_FIRST_BOOK)
                                navController.navigateSingleTop(
                                    Screen.Reader.createRoute(bookName = bookName, studyMode = openInStudyMode)
                                )
                            }
                        )
                    }
                }
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

        if (showCategoryLegendSheet && selectedCategoryForLegend != null) {
            val category = selectedCategoryForLegend!!
            val colors = BookCategoryColors.getColors(category, isDarkTheme)
            val booksInCategory = remember(category, currentSelectedTestament) {
                val fullList = if (currentSelectedTestament == Testament.OLD) oldTestamentBooks else newTestamentBooks
                fullList.filter { it.toBookCategory() == category }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { showCategoryLegendSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.bg)
                                .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                        )
                        Text(
                            text = category.labelEs,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.bg,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = "${booksInCategory.size} libros",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                        }
                    }

                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Libros en esta categoría:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        booksInCategory.forEach { bookName ->
                            Surface(
                                onClick = {
                                    showCategoryLegendSheet = false
                                    navController.navigateSingleTop(
                                        Screen.Reader.createRoute(bookName = bookName, studyMode = openInStudyMode)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = colors.bg,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    text = bookName,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                            }
                        }
                    }
                }
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

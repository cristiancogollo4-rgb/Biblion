package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import kotlinx.coroutines.launch

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
fun BooksScreen(navController: NavController, selectedTestament: Testament, openInStudyMode: Boolean = false) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedVersionKey by remember { mutableStateOf(BibleRepository.getSelectedVersionKey(context)) }
    var availableVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }

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
    
    val title = stringResource(
        R.string.books_screen_title,
        stringResource(currentSelectedTestament.labelRes)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            BiblionDrawerContent(
                navController = navController,
                drawerState = drawerState,
                onClose = { scope.launch { drawerState.close() } },
                onPickVersion = { showVersionDialog = true },
                onShowAbout = { showAboutDialog = true },
                onShowComingSoon = { showComingSoonDialog = true }
            )
        }
    ) {
        Scaffold(
            topBar = {
                BiblionTopAppBar(
                    onNavigationIconClick = { scope.launch { drawerState.open() } },
                    onSearchIconClick = { navController.navigateSingleTop(Screen.Search.route) },
                    logoResId = R.drawable.logobiblion,
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surface)
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
                        color = BiblionNavy
                    )
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Optimizacion: Añadimos 'key' para que Compose identifique cada item y sea mas fluido
                    items(booksToShow, key = { it }) { bookName ->
                        BookCard(
                            bookName = bookName,
                            onClick = {
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


private enum class DrawerOption(val labelRes: Int) {
    HOME(R.string.drawer_home),
    PICK_VERSION(R.string.drawer_pick_version),
    MY_TEACHINGS(R.string.drawer_my_teachings),
    DOCTRINES(R.string.drawer_doctrines),
    BIBLION(R.string.drawer_biblion),
    STUDY_MODE(R.string.drawer_study_mode),
    ABOUT_US(R.string.drawer_about_us)
}

@Composable
/**
 * Contenido del drawer lateral de la app.
 *
 * @param navController permite abrir rutas globales (inicio, modo estudio, etc.).
 * @param onClose callback para cerrar el drawer al seleccionar opción.
 */
fun BiblionDrawerContent(
    navController: NavController,
    drawerState: DrawerState,
    onClose: () -> Unit,
    onPickVersion: () -> Unit,
    onShowAbout: () -> Unit,
    onShowComingSoon: () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight().width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RectangleShape
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        val menuOptions = listOf(
            DrawerOption.HOME,
            DrawerOption.PICK_VERSION,
            DrawerOption.MY_TEACHINGS,
            DrawerOption.DOCTRINES,
            DrawerOption.BIBLION,
            DrawerOption.STUDY_MODE,
            DrawerOption.ABOUT_US
        )

        menuOptions.forEach { option ->
            BiblionMenuItem(
                text = stringResource(option.labelRes),
                onClick = {
                    if (!drawerState.isOpen) return@BiblionMenuItem
                    onClose()
                    when (option) {
                        DrawerOption.HOME -> {
                            if (currentRoute != Screen.Home.route) {
                                navController.navigateSingleTop(Screen.Home.route)
                            }
                        }
                        DrawerOption.PICK_VERSION -> onPickVersion()
                        DrawerOption.MY_TEACHINGS -> navController.navigateSingleTop(Screen.Ensenanzas.route)
                        DrawerOption.DOCTRINES,
                        DrawerOption.BIBLION -> onShowComingSoon()
                        DrawerOption.ABOUT_US -> onShowAbout()
                        DrawerOption.STUDY_MODE -> {
                            navController.navigateSingleTop(Screen.Reader.createRoute(studyMode = true))
                        }
                    }
                }
            )
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(80.dp))
                Text(stringResource(R.string.reader_label))
            }
        }
    }
}

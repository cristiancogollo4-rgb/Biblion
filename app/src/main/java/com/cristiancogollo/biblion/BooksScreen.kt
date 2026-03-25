package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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

    var currentSelectedTestament by remember { mutableStateOf(selectedTestament) }

    LaunchedEffect(Unit) {
        availableVersions = BibleRepository.getAvailableVersions(context)
        selectedVersionKey = BibleRepository.getSelectedVersionKey(context)
    }

    // Optimizacion: Derivamos los datos solo cuando cambia el testamento seleccionado
    val booksToShow = remember(currentSelectedTestament) {
        if (currentSelectedTestament == Testament.OLD) oldTestamentBooks else newTestamentBooks
    }
    
    val title = remember(currentSelectedTestament) {
        "Libros del ${currentSelectedTestament.label}"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            BiblionDrawerContent(
                navController = navController,
                onClose = { scope.launch { drawerState.close() } },
                onPickVersion = { showVersionDialog = true }
            )
        }
    ) {
        Scaffold(
            topBar = {
                BiblionTopAppBar(
                    onNavigationIconClick = { scope.launch { drawerState.open() } },
                    onSearchIconClick = { navController.navigate(Screen.Search.route) },
                    logoResId = R.drawable.logobiblionsinletras
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.White)
            ) {
                TestamentSelector(
                    selectedTab = currentSelectedTestament,
                    onTabSelected = { currentSelectedTestament = it }
                )

                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

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
                                navController.navigate(Screen.Reader.createRoute(bookName = bookName, studyMode = openInStudyMode))
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
    onClose: () -> Unit,
    onPickVersion: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight().width(300.dp),
        drawerContainerColor = Color.White,
        drawerShape = RectangleShape
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        val opciones = listOf("Inicio", "Elegir Versión", "Mis Enseñanzas", "Doctrinas", "Biblion", "Modo Estudio", "Sobre Nosotros")

        opciones.forEach { titulo ->
            BiblionMenuItem(
                text = titulo,
                onClick = {
                    onClose()
                    when (titulo) {
                        "Inicio" -> navController.navigate(Screen.Home.route)
                        "Elegir Versión" -> onPickVersion()
                        "Modo Estudio" -> {
                            navController.navigate(Screen.Reader.createRoute(studyMode = true))
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
                Text("Lector")
            }
        }
    }
}

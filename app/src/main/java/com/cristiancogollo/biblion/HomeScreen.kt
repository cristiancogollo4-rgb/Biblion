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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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
fun HomeScreen(navController: NavController, modifier: Modifier=Modifier) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    var dailyVerse by remember { mutableStateOf<DailyVerse?>(null) }

    // Cargar el versículo del día al entrar en la pantalla
    LaunchedEffect(Unit) {
        dailyVerse = getDailyVerse(context)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp),
                drawerContainerColor = Color.White,
                drawerShape = RectangleShape
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                val opciones = listOf("Inicio", "Elegir Versión", "Mis Enseñanzas", "Doctrinas", "Biblion", "Modo Estudio", "Sobre Nosotros")

                opciones.forEach { titulo ->
                    BiblionMenuItem(
                        text = titulo,
                        onClick = {
                            scope.launch { drawerState.close() }
                            when (titulo) {
                                "Mis Enseñanzas" -> {
                                    navController.navigate("ensenanzas")
                                }
                                "Modo Estudio" -> {
                                    // Navegamos a un libro por defecto (ej: Genesis) con modo estudio activado
                                    val encodedBook = URLEncoder.encode("Genesis", StandardCharsets.UTF_8.toString())
                                    navController.navigate("reader/$encodedBook?studyMode=true")
                                }
                                //navegación para otras opciones si es necesario
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
                    Text("Lector")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {},
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.Black)
                    ) {
                        Text("Iniciar Sesión", color = Color.Black)
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
                        navController.navigate("search")
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Para que la pantalla pueda hacer scroll si el contenido crece
            ) {
                // *** CORRECCIÓN CLAVE: Pasar "ANTIGUO_TESTAMENTO" como estado inicial ***
                var selectedTestament by remember { mutableStateOf("ANTIGUO TESTAMENTO") }

                TestamentSelector(selectedTab = selectedTestament) { testament ->
                    selectedTestament = testament // Actualiza el color del botón seleccionado

                    val encodedTestament = URLEncoder.encode(testament, StandardCharsets.UTF_8.toString())

                    navController.navigate("books/$encodedTestament")
                }

                HorizontalDivider(thickness = 0.5.dp)

                Spacer(modifier = Modifier.height(24.dp))

                // Título fijo para el versículo del día
                Text(
                    text = "VERSÍCULO DEL DÍA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Start
                )

                // Mostrar la tarjeta del versículo diario
                if (dailyVerse != null) {
                    DailyVerseCard(
                        verse = dailyVerse!!.text,
                        reference = dailyVerse!!.reference
                    )
                } else {
                    // Podrías mostrar un CircularProgressIndicator aquí mientras carga
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Aquí puedes añadir más contenido futuro si lo deseas...
            }
        }
    }
}

/**
 * Obtiene el versículo del día. Si ya se generó uno hoy, lo recupera.
 * Si no, genera uno nuevo aleatoriamente y lo guarda.
 */
private suspend fun getDailyVerse(context: Context): DailyVerse {
    val prefs = context.getSharedPreferences("BiblionAppPrefs", Context.MODE_PRIVATE)
    val today = Calendar.getInstance()

    val lastUpdateMillis = prefs.getLong("dailyVerseTimestamp", 0)
    val lastUpdateCalendar = Calendar.getInstance().apply { timeInMillis = lastUpdateMillis }

    val isNewDay = today.get(Calendar.DAY_OF_YEAR) != lastUpdateCalendar.get(Calendar.DAY_OF_YEAR) ||
            today.get(Calendar.YEAR) != lastUpdateCalendar.get(Calendar.YEAR)

    if (isNewDay) {
        return try {
            val newVerse = BibleRepository.getRandomVerse(context)
            with(prefs.edit()) {
                putString("dailyVerseText", newVerse.text)
                putString("dailyVerseRef", newVerse.reference)
                putLong("dailyVerseTimestamp", today.timeInMillis)
                apply()
            }
            newVerse
        } catch (_: Exception) {
            DailyVerse(
                "Porque de tal manera amó Dios al mundo, que ha dado a su Hijo unigénito, para que todo aquel que en él cree, no se pierda, mas tenga vida eterna.",
                "Juan 3:16"
            )
        }
    }

    val text = prefs.getString("dailyVerseText", "")!!
    val ref = prefs.getString("dailyVerseRef", "")!!
    return DailyVerse(text, ref)
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

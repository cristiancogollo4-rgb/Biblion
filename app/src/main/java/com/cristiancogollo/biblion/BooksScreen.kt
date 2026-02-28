package com.cristiancogollo.biblion

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@Composable
fun BooksScreen(navController: NavController, selectedTestament: String) {
    // Log para verificar qué llega exactamente
    Log.d("BooksScreen", "Received testament: '$selectedTestament'")

    // Quitamos guiones bajos por si acaso llegaran, para normalizar
    val normalizedTestament = selectedTestament.replace("_", " ")
    var currentSelectedTestament by remember { mutableStateOf(normalizedTestament) }

    // NOTA: Asegúrate de que estos nombres coincidan EXACTAMENTE con las llaves de tu JSON
    // Si en el JSON dice "Génesis", aquí debe decir "Génesis"
    val oldTestamentBooks = listOf(
        "Genesis", "Exodo", "Levitico", "Numeros", "Deuteronomio", "Josue", "Jueces", "Rut",
        "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", "1 Cronicas", "2 Cronicas", "Esdras",
        "Nehemias", "Ester", "Job", "Salmos", "Proverbios", "Eclesiastes", "Cantares",
        "Isaias", "Jeremias", "Lamentaciones", "Ezequiel", "Daniel", "Oseas", "Joel", "Amos",
        "Abdias", "Jonas", "Miqueas", "Nahum", "Habacuc", "Sofonias", "Hageo", "Zacarias", "Malaquias"
    )

    val newTestamentBooks = listOf(
        "Mateo", "Marcos", "Lucas", "Juan", "Hechos", "Romanos", "1 Corintios", "2 Corintios",
        "Galatas", "Efesios", "Filipenses", "Colosenses", "1 Tesalonicenses", "2 Tesalonicenses",
        "1 Timoteo", "2 Timoteo", "Tito", "Filemon", "Hebreos", "Santiago", "1 Pedro", "2 Pedro",
        "1 Juan", "2 Juan", "3 Juan", "Judas", "Apocalipsis"
    )

    // CORRECCIÓN 1: Comparación flexible (ignorando guiones o espacios)
    val booksToShow = if (currentSelectedTestament.contains("ANTIGUO")) oldTestamentBooks else newTestamentBooks

    val title = "Libros Del ${currentSelectedTestament.lowercase().replaceFirstChar { it.uppercase() }}"

    Scaffold(
        topBar = {
            BiblionTopAppBar(
                onNavigationIconClick = { navController.navigateUp() },
                onSearchIconClick = { navController.navigate("search") }
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
                // Enviamos el valor con guion bajo si tu selector lo espera así
                selectedTab = currentSelectedTestament.replace(" ", "_"),
                onTabSelected = { newValue ->
                    currentSelectedTestament = newValue.replace("_", " ")
                }
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

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(booksToShow) { bookName ->
                    BookCard(
                        bookName = bookName,
                        onClick = {
                            // CORRECCIÓN 2: Codificar el nombre del libro para la navegación
                            val encodedBook = URLEncoder.encode(bookName, StandardCharsets.UTF_8.toString())
                            navController.navigate("reader/$encodedBook")
                        }
                    )
                }
            }
        }
    }
}
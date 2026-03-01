package com.cristiancogollo.biblion

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Grafo principal de navegación de la app.
 *
 * Define rutas Compose y sus parámetros codificados en URL.
 * Se centraliza aquí para mantener consistente el flujo entre Home, Books, Reader y Search.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    /**
     * startDestination = "home": pantalla inicial al abrir la aplicación.
     */
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { 
            HomeScreen(navController) 
        }

        composable(
            route = "books/{testament}",
            arguments = listOf(navArgument("testament") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedTestament = backStackEntry.arguments?.getString("testament") ?: "ANTIGUO TESTAMENTO"
            val decodedTestament = java.net.URLDecoder.decode(encodedTestament, java.nio.charset.StandardCharsets.UTF_8.toString())

            BooksScreen(
                navController = navController,
                selectedTestament = decodedTestament
            )
        }

        composable(
            route = "reader/{bookName}?studyMode={studyMode}",
            arguments = listOf(
                navArgument("bookName") { type = NavType.StringType },
                navArgument("studyMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val encodedBookName = backStackEntry.arguments?.getString("bookName") ?: ""
            val studyMode = backStackEntry.arguments?.getBoolean("studyMode") ?: false
            val decodedBookName = java.net.URLDecoder.decode(encodedBookName, java.nio.charset.StandardCharsets.UTF_8.toString())

            ReaderScreen(
                navController = navController,
                bookName = decodedBookName,
                initialStudyMode = studyMode // Pasamos el valor que viene del menú
            )
        }

        // Ruta específica para Modo Estudio (opcional, pero recomendada por el usuario)
        composable(
            route = "study/{bookName}",
            arguments = listOf(navArgument("bookName") { type = NavType.StringType })
        ) { entry ->
            val encodedName = entry.arguments?.getString("bookName") ?: ""
            val bookName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
            ReaderScreen(navController, bookName, initialStudyMode = true)
        }
        
        composable("search") { 
            SearchScreen(navController) 
        }
    }
}

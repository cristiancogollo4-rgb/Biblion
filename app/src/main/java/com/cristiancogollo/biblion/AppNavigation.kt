package com.cristiancogollo.biblion

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { 
            HomeScreen(navController) 
        }

        composable(
            route = "books/{testament}",
            arguments = listOf(navArgument("testament") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedTestament = backStackEntry.arguments?.getString("testament") ?: "ANTIGUO TESTAMENTO"

            // Esta línea convierte "NUEVO+TESTAMENTO" de vuelta a "NUEVO TESTAMENTO"
            val decodedTestament = URLDecoder.decode(encodedTestament, StandardCharsets.UTF_8.toString())

            BooksScreen(
                navController = navController,
                selectedTestament = decodedTestament // Pasamos el nombre limpio
            )
        }
        
        composable(
            route = "reader/{bookName}",
            arguments = listOf(navArgument("bookName") { type = NavType.StringType })
        ) { entry ->
            val encodedName = entry.arguments?.getString("bookName") ?: ""
            val bookName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
            ReaderScreen(navController, bookName)
        }
        
        composable("search") { 
            SearchScreen(navController) 
        }
    }
}

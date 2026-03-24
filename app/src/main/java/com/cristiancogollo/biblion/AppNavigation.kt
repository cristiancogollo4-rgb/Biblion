package com.cristiancogollo.biblion

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        composable(Screen.Ensenanzas.route) {
            EnsenanzaScreen(navController)
        }

        composable(
            route = Screen.Books.route,
            arguments = listOf(navArgument("testament") { type = NavType.StringType })
        ) { backStackEntry ->
            val testament = Testament.fromRouteArg(backStackEntry.arguments?.getString("testament"))
            BooksScreen(
                navController = navController,
                selectedTestament = testament
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("bookName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("studyMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val encodedBookName = backStackEntry.arguments?.getString("bookName") ?: ""
            val studyMode = backStackEntry.arguments?.getBoolean("studyMode") ?: false
            val decodedBookName = decodeArg(encodedBookName).ifBlank { null }

            ReaderScreen(
                navController = navController,
                bookName = decodedBookName,
                initialStudyMode = studyMode
            )
        }

        composable(
            route = Screen.Study.route,
            arguments = listOf(
                navArgument("bookName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            val encodedName = entry.arguments?.getString("bookName") ?: ""
            val bookName = decodeArg(encodedName).ifBlank { null }
            ReaderScreen(navController, bookName, initialStudyMode = true)
        }

        composable(Screen.Search.route) {
            SearchScreen(navController)
        }
    }
}

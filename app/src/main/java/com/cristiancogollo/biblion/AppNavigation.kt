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
            route = Screen.ReaderWithBook.route,
            arguments = listOf(
                navArgument("bookName") { type = NavType.StringType },
                navArgument("studyMode") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("chapter") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("verse") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val encodedBookName = backStackEntry.arguments?.getString("bookName") ?: ""
            val studyMode = backStackEntry.arguments?.getBoolean("studyMode") ?: false
            val initialChapter = backStackEntry.arguments?.getInt("chapter") ?: 1
            val initialVerse = decodeArg(backStackEntry.arguments?.getString("verse") ?: "").ifBlank { null }
            val decodedBookName = decodeArg(encodedBookName).ifBlank { null }

            ReaderScreen(
                navController = navController,
                bookName = decodedBookName,
                initialStudyMode = studyMode,
                initialChapter = initialChapter,
                targetVerse = initialVerse
            )
        }

        composable(
            route = Screen.ReaderWithoutBook.route,
            arguments = listOf(
                navArgument("studyMode") {
                    type = NavType.BoolType
                    defaultValue = true
                },
                navArgument("chapter") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("verse") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val studyMode = backStackEntry.arguments?.getBoolean("studyMode") ?: true
            val initialChapter = backStackEntry.arguments?.getInt("chapter") ?: 1
            val initialVerse = decodeArg(backStackEntry.arguments?.getString("verse") ?: "").ifBlank { null }
            ReaderScreen(
                navController = navController,
                bookName = null,
                initialStudyMode = studyMode,
                initialChapter = initialChapter,
                targetVerse = initialVerse
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

        composable(Screen.BiblionComingSoon.route) {
            BiblionComingSoonScreen(navController)
        }

        composable(Screen.About.route) {
            AboutBiblionScreen(navController)
        }
    }
}

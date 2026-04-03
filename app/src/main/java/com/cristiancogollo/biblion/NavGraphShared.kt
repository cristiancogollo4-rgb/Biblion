package com.cristiancogollo.biblion

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Destinos compartidos entre navegación principal y navegación interna de modo estudio.
 */
fun NavGraphBuilder.addSharedPrimaryDestinations(
    navController: NavController,
    openBooksInStudyMode: Boolean = false
) {
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
            selectedTestament = testament,
            openInStudyMode = openBooksInStudyMode
        )
    }

    composable(Screen.Search.route) {
        SearchScreen(navController)
    }

    composable(
        route = Screen.StudyRead.route,
        arguments = listOf(navArgument("studyId") { type = NavType.LongType })
    ) { backStackEntry ->
        val studyId = backStackEntry.arguments?.getLong("studyId") ?: return@composable
        StudyReadScreen(navController = navController, studyId = studyId)
    }
}

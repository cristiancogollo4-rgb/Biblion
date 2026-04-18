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
    openBooksInStudyMode: Boolean = false,
    includeHome: Boolean = true,
    includeBooks: Boolean = true,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    currentUserEmail: String? = null,
    isAuthenticated: Boolean = false,
    showSignedOutDialog: Boolean = false,
    onDismissSignedOutDialog: () -> Unit = {},
    onAuthActionClick: () -> Unit = {}
) {
    if (includeHome) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                currentUserEmail = currentUserEmail,
                isAuthenticated = isAuthenticated,
                showSignedOutDialog = showSignedOutDialog,
                onDismissSignedOutDialog = onDismissSignedOutDialog,
                onAuthActionClick = onAuthActionClick
            )
        }
    }

    composable(Screen.Ensenanzas.route) {
        EnsenanzaScreen(navController)
    }

    if (includeBooks) {
        composable(
            route = Screen.Books.route,
            arguments = listOf(navArgument("testament") { type = NavType.StringType })
        ) { backStackEntry ->
            val testament = Testament.fromRouteArg(backStackEntry.arguments?.getString("testament"))
            BooksScreen(
                navController = navController,
                selectedTestament = testament,
                openInStudyMode = openBooksInStudyMode,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                currentUserEmail = currentUserEmail,
                isAuthenticated = isAuthenticated,
                showSignedOutDialog = showSignedOutDialog,
                onDismissSignedOutDialog = onDismissSignedOutDialog,
                onAuthActionClick = onAuthActionClick
            )
        }
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

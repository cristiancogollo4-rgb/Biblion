package com.cristiancogollo.biblion

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cristiancogollo.biblion.feature.search.ExploreTopicsScreen
import com.cristiancogollo.biblion.feature.search.ExploreCategoryScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.StudyDocEditorRoute
import com.cristiancogollo.biblion.feature.studydocs.ui.StudyDocReadRoute
import com.cristiancogollo.biblion.feature.studydocs.ui.StudyDocsListRoute
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen

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
    currentUserName: String? = null,
    currentUserEmail: String? = null,
    isAuthenticated: Boolean = false,
    showSignedOutDialog: Boolean = false,
    onDismissSignedOutDialog: () -> Unit = {},
    onAuthActionClick: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    if (includeHome) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                currentUserName = currentUserName,
                currentUserEmail = currentUserEmail,
                isAuthenticated = isAuthenticated,
                showSignedOutDialog = showSignedOutDialog,
                onDismissSignedOutDialog = onDismissSignedOutDialog,
                onAuthActionClick = onAuthActionClick,
                onNavigateToProfile = onNavigateToProfile
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
                currentUserName = currentUserName,
                currentUserEmail = currentUserEmail,
                isAuthenticated = isAuthenticated,
                showSignedOutDialog = showSignedOutDialog,
                onDismissSignedOutDialog = onDismissSignedOutDialog,
                onAuthActionClick = onAuthActionClick,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    }

    composable(Screen.Search.route) {
        SearchScreen(navController)
    }

    composable(Screen.ExploreTopics.route) {
        ExploreTopicsScreen(navController)
    }

    composable(
        route = Screen.ExploreCategory.route,
        arguments = listOf(navArgument("category") { type = NavType.StringType })
    ) { backStackEntry ->
        val category = backStackEntry.arguments?.getString("category").orEmpty()
        ExploreCategoryScreen(
            navController = navController,
            category = category
        )
    }

    composable(
        route = Screen.StudyRead.route,
        arguments = listOf(navArgument("studyId") { type = NavType.LongType })
    ) { backStackEntry ->
        val studyId = backStackEntry.arguments?.getLong("studyId") ?: return@composable
        StudyReadScreen(
            navController = navController,
            studyId = studyId,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme
        )
    }

    // Rutas del modo estudio v2 (estudydocs)
    composable(StudyDocScreen.StudyDocsList.route) {
        StudyDocsListRoute(navController = navController)
    }

    composable(
        route = StudyDocScreen.StudyDocEditor.route,
        arguments = listOf(navArgument(StudyDocScreen.StudyDocEditor.ARG_REMOTE_ID) { type = NavType.StringType })
    ) { backStackEntry ->
        val remoteId = backStackEntry.arguments?.getString(StudyDocScreen.StudyDocEditor.ARG_REMOTE_ID)
        StudyDocEditorRoute(
            navController = navController,
            remoteId = if (remoteId == "new") null else remoteId,
        )
    }

    composable(
        route = StudyDocScreen.StudyDocRead.route,
        arguments = listOf(navArgument(StudyDocScreen.StudyDocRead.ARG_REMOTE_ID) { type = NavType.StringType })
    ) { backStackEntry ->
        val remoteId = backStackEntry.arguments?.getString(StudyDocScreen.StudyDocRead.ARG_REMOTE_ID)
            ?: return@composable
        StudyDocReadRoute(
            navController = navController,
            remoteId = remoteId,
        )
    }
}

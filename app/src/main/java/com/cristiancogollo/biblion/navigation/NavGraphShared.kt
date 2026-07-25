package com.cristiancogollo.biblion

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cristiancogollo.biblion.feature.search.ui.ExploreTopicsScreen
import com.cristiancogollo.biblion.feature.search.ui.ExploreCategoryScreen
import com.cristiancogollo.biblion.feature.search.model.SearchScope
import com.cristiancogollo.biblion.feature.search.ui.SearchScreen
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
    includeSearch: Boolean = true,
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

    if (includeSearch) {
        composable(
            route = Screen.Search.route,
            arguments = listOf(navArgument("scope") { type = NavType.StringType; defaultValue = "bible" })
        ) { backStackEntry ->
            val scopeArg = backStackEntry.arguments?.getString("scope")
            val scope = SearchScope.fromRouteArg(scopeArg)
            SearchScreen(navController = navController, scope = scope)
        }
    }

    composable(Screen.ExploreTopics.route) {
        ExploreTopicsScreen(navController)
    }

    composable(Screen.Dictionary.route) {
        com.cristiancogollo.biblion.feature.dictionary.ui.DictionaryScreen(navController)
    }

    composable(
        route = Screen.ExploreDictionaryCategory.route,
        arguments = listOf(navArgument("category") { type = NavType.StringType })
    ) { backStackEntry ->
        val category = backStackEntry.arguments?.getString("category").orEmpty()
        com.cristiancogollo.biblion.feature.dictionary.ui.DictionaryCategoryScreen(
            navController = navController,
            category = category
        )
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
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
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

    composable(
        route = Screen.DictionaryEntry.route,
        arguments = listOf(navArgument(Screen.DictionaryEntry.ARG_ENTRY_ID) { type = NavType.LongType })
    ) { backStackEntry ->
        val entryId = backStackEntry.arguments?.getLong(Screen.DictionaryEntry.ARG_ENTRY_ID)
            ?: return@composable
        com.cristiancogollo.biblion.feature.dictionary.ui.DictionaryEntryDetailScreen(
            navController = navController,
            entryId = entryId,
        )
    }
}

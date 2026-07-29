package com.cristiancogollo.biblion

import androidx.navigation.NavController
import com.cristiancogollo.biblion.feature.search.model.SearchScope
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val utf8: String = StandardCharsets.UTF_8.toString()

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search?scope={scope}") {
        fun createRoute(scope: SearchScope = SearchScope.BIBLE): String = "search?scope=${scope.name.lowercase()}"
    }
    data object Profile : Screen("profile")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object BiblionComingSoon : Screen("biblion-coming-soon")
    data object BiblionRepository : Screen("biblion_repository")
    data object About : Screen("about")

    data object Books : Screen("books/{testament}") {
        fun createRoute(testament: Testament): String = "books/${testament.toRouteArg()}"
    }

    data object ReaderWithBook : Screen("reader/{bookName}?studyMode={studyMode}&chapter={chapter}&verse={verse}&studyId={studyId}")
    data object ReaderWithoutBook : Screen("reader?studyMode={studyMode}&chapter={chapter}&verse={verse}&studyId={studyId}")

    data object Reader {
        fun createRoute(
            bookName: String? = null,
            studyMode: Boolean = false,
            chapter: Int? = null,
            verse: String? = null,
            studyId: Long? = null
        ): String {
            val chapterArg = chapter ?: 1
            val verseArg = encodeArg(verse.orEmpty())
            val studyIdArg = studyId ?: -1L
            return if (bookName.isNullOrBlank()) {
                "reader?studyMode=$studyMode&chapter=$chapterArg&verse=$verseArg&studyId=$studyIdArg"
            } else {
                "reader/${encodeArg(bookName)}?studyMode=$studyMode&chapter=$chapterArg&verse=$verseArg&studyId=$studyIdArg"
            }
        }
    }

    data object ExploreTopics : Screen("explore_topics")

    data object ExploreCategory : Screen("explore_category/{category}") {
        fun createRoute(category: String): String = "explore_category/${encodeArg(category)}"
    }

    data object Dictionary : Screen("dictionary")

    data object ExploreDictionaryCategory : Screen("dictionary_category/{category}") {
        fun createRoute(category: String): String = "dictionary_category/${encodeArg(category)}"
    }

    data object DictionaryEntry : Screen("dictionary/entry/{entryId}") {
        fun createRoute(entryId: Long): String = "dictionary/entry/$entryId"
        const val ARG_ENTRY_ID: String = "entryId"
    }
}

fun encodeArg(value: String): String = URLEncoder.encode(value, utf8)

fun decodeArg(value: String): String = URLDecoder.decode(value, utf8)

fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.popBackStackOrNavigateHome(): Boolean {
    val popped = popBackStack()
    if (!popped) {
        navigateSingleTop(Screen.Home.route)
    }
    return popped
}

fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

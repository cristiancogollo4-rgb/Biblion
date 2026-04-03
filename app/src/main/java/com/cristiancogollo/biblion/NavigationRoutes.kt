package com.cristiancogollo.biblion

import androidx.navigation.NavController
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val utf8: String = StandardCharsets.UTF_8.toString()

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Ensenanzas : Screen("ensenanzas")
    data object Search : Screen("search")
    data object BiblionComingSoon : Screen("biblion-coming-soon")
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

    data object Study : Screen("study?bookName={bookName}") {
        fun createRoute(bookName: String? = null): String {
            val encodedBook = encodeArg(bookName.orEmpty())
            return "study?bookName=$encodedBook"
        }
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

fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

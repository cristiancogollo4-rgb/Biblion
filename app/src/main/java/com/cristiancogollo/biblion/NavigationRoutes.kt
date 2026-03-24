package com.cristiancogollo.biblion

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val utf8: String = StandardCharsets.UTF_8.toString()

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Ensenanzas : Screen("ensenanzas")
    data object Search : Screen("search")

    data object Books : Screen("books/{testament}") {
        fun createRoute(testament: Testament): String = "books/${testament.toRouteArg()}"
    }

    data object Reader : Screen("reader?bookName={bookName}&studyMode={studyMode}") {
        fun createRoute(bookName: String? = null, studyMode: Boolean = false): String {
            val encodedBook = encodeArg(bookName.orEmpty())
            return "reader?bookName=$encodedBook&studyMode=$studyMode"
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

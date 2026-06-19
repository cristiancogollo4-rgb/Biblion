package com.cristiancogollo.biblion.feature.bibi

data class BibiUserContext(
    val userName: String? = null,
    val currentBook: String? = null,
    val currentChapter: Int? = null,
    val currentVerse: Int? = null,
    val bibleVersion: String = "rv1960",
    val lastQueries: List<String> = emptyList(),
    val chatHistory: List<ChatExchange> = emptyList()
)

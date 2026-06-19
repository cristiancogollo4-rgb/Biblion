package com.cristiancogollo.biblion.feature.bibi

data class ChatExchange(
    val question: String,
    val response: String,
    val resolvedTerm: String? = null,
    val intent: String = "FALLBACK"
)

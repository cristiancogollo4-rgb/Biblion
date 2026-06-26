package com.cristiancogollo.biblion.feature.bibi.ui

import android.util.Log
import com.cristiancogollo.biblion.BuildConfig
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente HTTP simple para llamar al Cloudflare Worker de Bibi.
 * Solo se usa cuando KnowledgeEngine retorna null (DIVE_DEEPER / FALLBACK).
 */
object WorkerClient {

    private const val TAG = "WorkerClient"
    private const val CONNECT_TIMEOUT = 15_000
    private const val READ_TIMEOUT = 45_000

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class WorkerRequest(
        val question: String,
        val mode: String = "reader",
        val intent: String = "question",
        val study: StudyPayload? = null,
        val bible: BiblePayload? = null,
        val chatHistory: List<ChatHistoryEntry> = emptyList(),
        val lastQueries: List<String> = emptyList()
    )

    @Serializable
    data class StudyPayload(
        val title: String = "",
        val tags: List<String> = emptyList(),
        val selectedText: String = "",
        val currentOutline: List<String> = emptyList(),
        val notes: List<String> = emptyList()
    )

    @Serializable
    data class BiblePayload(
        val version: String = "rv1960",
        val availableVersions: List<String> = emptyList(),
        val passages: List<String> = emptyList()
    )

    @Serializable
    data class ChatHistoryEntry(
        val question: String,
        val response: String,
        val resolvedTerm: String? = null,
        val intent: String = ""
    )

    @Serializable
    data class WorkerResponse(
        val answer: String = "",
        val references: List<String> = emptyList(),
        val confidence: String = "high"
    )

    /**
     * Envía una pregunta al Worker y retorna la respuesta como texto.
     * Retorna null si hay error o la respuesta esta vacia.
     */
    suspend fun ask(
        question: String,
        bookName: String? = null,
        chapter: Int = 0,
        selectedText: String = "",
        chatHistory: List<ChatExchange> = emptyList(),
        lastQueries: List<String> = emptyList(),
        bibleVersion: String = "rv1960"
    ): String? = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.BIBI_ENDPOINT_URL
        if (endpoint.isBlank()) {
            Log.w(TAG, "BIBI_ENDPOINT_URL is empty, skipping Worker call")
            return@withContext null
        }

        var connection: HttpURLConnection? = null
        try {
            val studyTitle = listOfNotNull(
                bookName,
                chapter.takeIf { it > 0 }?.let { "capítulo $it" }
            ).joinToString(" ")

            val request = WorkerRequest(
                question = question,
                mode = "reader",
                intent = "question",
                study = StudyPayload(title = studyTitle, selectedText = selectedText),
                bible = BiblePayload(version = bibleVersion),
                chatHistory = chatHistory.takeLast(5).map {
                    ChatHistoryEntry(
                        question = it.question,
                        response = it.response,
                        resolvedTerm = it.resolvedTerm,
                        intent = it.intent
                    )
                },
                lastQueries = lastQueries.takeLast(5)
            )

            val body = json.encodeToString(WorkerRequest.serializer(), request)
            Log.d(TAG, "Sending to Worker: $question")

            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                doOutput = true
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                Log.w(TAG, "Worker returned $responseCode")
                connection.errorStream ?: return@withContext null
            }
            val responseBody = stream.bufferedReader().use { it.readText() }

            if (responseCode !in 200..299) {
                return@withContext null
            }

            if (responseBody.isBlank()) {
                Log.w(TAG, "Worker returned empty body")
                return@withContext null
            }

            val parsed = try {
                json.decodeFromString(WorkerResponse.serializer(), responseBody)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse Worker response, trying raw text")
                WorkerResponse(answer = responseBody)
            }

            val answer = parsed.answer.trim()
            if (answer.isBlank()) {
                Log.w(TAG, "Worker returned blank answer")
                return@withContext null
            }

            Log.d(TAG, "Worker response: ${answer.take(100)}...")
            answer
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Worker", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}

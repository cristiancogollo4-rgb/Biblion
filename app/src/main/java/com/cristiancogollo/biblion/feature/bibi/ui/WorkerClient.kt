package com.cristiancogollo.biblion.feature.bibi.ui

import android.util.Log
import com.cristiancogollo.biblion.BuildConfig
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiMessageText
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import com.google.firebase.auth.FirebaseAuth
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume

object WorkerClient {
    private const val TAG = "BibiWorker"
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT = 30_000

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Serializable
    data class WorkerRequest(
        val question: String,
        val mode: String = "reader",
        val intent: String = "question",
        val study: StudyPayload? = null,
        val bible: BiblePayload? = null,
        val chatHistory: List<ChatHistoryEntry> = emptyList(),
        val lastQueries: List<String> = emptyList(),
    )

    @Serializable
    data class StudyPayload(
        val title: String = "",
        val tags: List<String> = emptyList(),
        val selectedText: String = "",
        val currentOutline: List<String> = emptyList(),
        val notes: List<String> = emptyList(),
    )

    @Serializable
    data class BiblePayload(
        val version: String = "rv1960",
        val availableVersions: List<String> = emptyList(),
        val passages: List<String> = emptyList(),
    )

    @Serializable
    data class ChatHistoryEntry(
        val question: String,
        val response: String,
        val resolvedTerm: String? = null,
        val intent: String = "",
    )

    @Serializable(with = WorkerReferenceSerializer::class)
    data class WorkerReference(
        val reference: String,
        val reason: String = "",
    )

    @Serializable
    data class WorkerResponse(
        val answer: String = "",
        val references: List<WorkerReference> = emptyList(),
        val suggestedBlocks: List<String> = emptyList(),
        val confidence: String = "high",
        val disclaimer: String? = null,
        val intentDetected: String? = null,
    )

    data class RichWorkerResponse(
        val answer: String,
        val references: List<WorkerReference>,
        val suggestedBlocks: List<String>,
        val confidence: String,
        val disclaimer: String?,
        val intentDetected: String?,
    )

    fun buildRequest(
        question: String,
        context: BibiContext,
        intent: String,
        chatHistory: List<ChatExchange>,
        lastQueries: List<String>,
        localContext: String = "",
    ): WorkerRequest {
        val commonHistory = chatHistory.takeLast(5).map {
            ChatHistoryEntry(
                question = it.question,
                response = it.response,
                resolvedTerm = it.resolvedTerm,
                intent = it.intent,
            )
        }
        return when (context) {
            is BibiContext.Reader -> WorkerRequest(
                question = question,
                mode = "reader",
                intent = intent,
                study = StudyPayload(
                    title = "${context.book} ${context.chapter}".trim(),
                    selectedText = context.passages.joinToString("\n") { it.asPromptText() },
                    notes = listOfNotNull(localContext.takeIf { it.isNotBlank() }),
                ),
                bible = BiblePayload(
                    version = context.bibleVersion,
                    passages = context.passages.map { it.asPromptText() },
                ),
                chatHistory = commonHistory,
                lastQueries = lastQueries.takeLast(5),
            )
            is BibiContext.Study -> WorkerRequest(
                question = question,
                mode = "study",
                intent = intent,
                study = StudyPayload(
                    title = context.title,
                    tags = context.tags,
                    selectedText = context.selectedText,
                    currentOutline = context.outline,
                    notes = context.notes + listOfNotNull(localContext.takeIf { it.isNotBlank() }),
                ),
                bible = BiblePayload(
                    version = context.bibleVersion,
                    passages = context.passages.map { it.asPromptText() },
                ),
                chatHistory = commonHistory,
                lastQueries = lastQueries.takeLast(5),
            )
        }
    }

    suspend fun askRich(request: WorkerRequest): RichWorkerResponse? = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.BIBI_ENDPOINT_URL
        if (endpoint.isBlank()) return@withContext null

        var connection: HttpURLConnection? = null
        try {
            val token = getFirebaseIdToken()
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                doOutput = true
            }
            val body = json.encodeToString(WorkerRequest.serializer(), request)
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                it.write(body)
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            } ?: return@withContext null
            val responseBody = stream.bufferedReader().use { it.readText() }
            if (responseCode !in 200..299 || responseBody.isBlank()) {
                Log.w(TAG, "Worker request failed with status $responseCode")
                return@withContext null
            }
            val parsed = json.decodeFromString(WorkerResponse.serializer(), responseBody)
            BibiMessageText.normalize(parsed.answer).takeIf { it.isNotEmpty() }?.let { answer ->
                RichWorkerResponse(
                    answer = answer,
                    references = parsed.references,
                    suggestedBlocks = parsed.suggestedBlocks
                        .map(BibiMessageText::normalize)
                        .filter(String::isNotBlank),
                    confidence = parsed.confidence,
                    disclaimer = parsed.disclaimer
                        ?.let(BibiMessageText::normalize)
                        ?.takeIf(String::isNotBlank),
                    intentDetected = parsed.intentDetected,
                )
            }
        } catch (error: Exception) {
            Log.w(TAG, "Worker request could not be completed: ${error.javaClass.simpleName}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun getFirebaseIdToken(): String? = suspendCancellableCoroutine { continuation ->
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        user.getIdToken(false)
            .addOnSuccessListener { continuation.resume(it.token) }
            .addOnFailureListener { continuation.resume(null) }
    }
}

object WorkerReferenceSerializer : KSerializer<WorkerClient.WorkerReference> {
    override val descriptor: SerialDescriptor =
        JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): WorkerClient.WorkerReference {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> WorkerClient.WorkerReference(
                reference = element.contentOrNull.orEmpty(),
            )
            is JsonObject -> WorkerClient.WorkerReference(
                reference = element["reference"]?.jsonPrimitive?.contentOrNull
                    ?: element["ref"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                reason = element["reason"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
            else -> WorkerClient.WorkerReference("")
        }
    }

    override fun serialize(encoder: Encoder, value: WorkerClient.WorkerReference) {
        (encoder as JsonEncoder).encodeJsonElement(
            JsonObject(
                mapOf(
                    "reference" to JsonPrimitive(value.reference),
                    "reason" to JsonPrimitive(value.reason),
                )
            )
        )
    }
}

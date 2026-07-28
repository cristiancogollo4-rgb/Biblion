package com.cristiancogollo.biblion.feature.bibi.domain

import android.content.Context
import com.cristiancogollo.biblion.feature.bibi.engine.KnowledgeEngine
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiQueryResult
import com.cristiancogollo.biblion.feature.bibi.model.BibiReference
import com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import com.cristiancogollo.biblion.feature.bibi.model.Confidence
import com.cristiancogollo.biblion.feature.bibi.model.Source
import com.cristiancogollo.biblion.feature.bibi.ui.WorkerClient

class BibiOrchestrator(
    private val remoteGateway: RemoteBibiGateway = WorkerRemoteBibiGateway,
) {
    suspend fun answer(
        context: Context,
        question: String,
        bibiContext: BibiContext,
        userName: String?,
        chatHistory: List<ChatExchange>,
        lastQueries: List<String>,
        forceRemote: Boolean = false,
    ): BibiQueryResult {
        val intent = KnowledgeEngine.detectIntent(question, lastQueries)
        val localResponse = if (forceRemote) {
            null
        } else {
            KnowledgeEngine.answer(
                context = context,
                question = question,
                userContext = bibiContext.toKnowledgeContext(userName, chatHistory, lastQueries),
            )
        }

        if (localResponse != null && localResponse.confidence != Confidence.LOW) {
            return BibiQueryResult(
                answer = localResponse.buildChatText(),
                suggestions = localResponse.suggestions,
                confidence = localResponse.confidence,
                source = Source.LOCAL,
                resolvedTerm = localResponse.title,
                intent = intent.name,
            )
        }

        val remoteResponse = remoteGateway.ask(
            request = WorkerClient.buildRequest(
                question = question,
                context = bibiContext,
                intent = intent.toWorkerIntent(),
                chatHistory = chatHistory,
                lastQueries = lastQueries,
                localContext = localResponse?.let {
                    "${it.title}: ${it.definition.take(300)}"
                }.orEmpty(),
            ),
        )

        if (remoteResponse != null) {
            val suggestions = remoteResponse.references.take(3).map {
                BibiSuggestion(
                    label = "Explorar ${it.reference}",
                    query = "explicame ${it.reference}",
                )
            }
            return BibiQueryResult(
                answer = remoteResponse.answer,
                suggestions = suggestions,
                references = remoteResponse.references.map {
                    BibiReference(it.reference, it.reason)
                },
                suggestedBlocks = remoteResponse.suggestedBlocks,
                disclaimer = remoteResponse.disclaimer,
                confidence = remoteResponse.confidence.toConfidence(),
                source = Source.AI,
                intent = remoteResponse.intentDetected ?: intent.name,
            )
        }

        val fallback = localResponse?.buildChatText()
            ?: "No pude procesar tu pregunta. Intenta mencionar un pasaje, personaje o tema biblico concreto."
        return BibiQueryResult(
            answer = fallback,
            confidence = localResponse?.confidence ?: Confidence.LOW,
            source = localResponse?.source ?: Source.LOCAL,
            resolvedTerm = localResponse?.title,
            intent = intent.name,
        )
    }
}

fun interface RemoteBibiGateway {
    suspend fun ask(request: WorkerClient.WorkerRequest): WorkerClient.RichWorkerResponse?
}

private object WorkerRemoteBibiGateway : RemoteBibiGateway {
    override suspend fun ask(
        request: WorkerClient.WorkerRequest,
    ): WorkerClient.RichWorkerResponse? = WorkerClient.askRich(request)
}

private fun BibiContext.toKnowledgeContext(
    userName: String?,
    chatHistory: List<ChatExchange>,
    lastQueries: List<String>,
): KnowledgeEngine.UserContext {
    val reader = when (this) {
        is BibiContext.Reader -> this
        is BibiContext.Study -> BibiContext.Reader(
            book = passages.firstOrNull()?.book.orEmpty(),
            chapter = passages.firstOrNull()?.chapter ?: 0,
            passages = passages,
            bibleVersion = bibleVersion,
        )
    }
    val primary = reader.passages.firstOrNull()
    return KnowledgeEngine.UserContext(
        book = reader.book,
        chapter = reader.chapter,
        verse = primary?.verse ?: 0,
        selectedVerses = reader.passages.map { it.verse }.toSet(),
        userName = userName,
        verseText = reader.passages.joinToString("\n") { it.asPromptText() },
        verseRef = primary?.reference.orEmpty(),
        bibleVersion = reader.bibleVersion,
        lastQueries = lastQueries,
        chatHistory = chatHistory,
    )
}

private fun KnowledgeEngine.BibiIntent.toWorkerIntent(): String = when (this) {
    KnowledgeEngine.BibiIntent.EXPLAIN_VERSE -> "explain"
    KnowledgeEngine.BibiIntent.DEFINE,
    KnowledgeEngine.BibiIntent.WHO,
    KnowledgeEngine.BibiIntent.WHERE,
    KnowledgeEngine.BibiIntent.ORIGINAL_LANG -> "define"
    KnowledgeEngine.BibiIntent.RELATED -> "cross_reference"
    KnowledgeEngine.BibiIntent.TOPICS -> "outline"
    else -> "question"
}

private fun String.toConfidence(): Confidence = when (lowercase()) {
    "high" -> Confidence.HIGH
    "low" -> Confidence.LOW
    else -> Confidence.MEDIUM
}

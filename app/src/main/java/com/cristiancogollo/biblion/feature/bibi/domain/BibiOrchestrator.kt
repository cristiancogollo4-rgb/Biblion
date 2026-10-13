package com.cristiancogollo.biblion.feature.bibi.domain

import android.content.Context
import com.cristiancogollo.biblion.BibleRepository
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
        conversationPerspective: DoctrinalPerspectivePolicy.Perspective? = null,
    ): BibiQueryResult {
        val intent = KnowledgeEngine.detectIntent(question, lastQueries)
        fun attributionFallback(): BibiQueryResult {
            val generalAnswer = InstitutionalDoctrinePolicy.generalQuestionFallback(question)
            return BibiQueryResult(
                answer = generalAnswer ?: InstitutionalDoctrinePolicy.UNVERIFIED_ANSWER,
                confidence = if (generalAnswer == null) Confidence.LOW else Confidence.MEDIUM,
                source = Source.LOCAL,
                intent = intent.name,
            )
        }
        InstitutionalDoctrinePolicy.verifiedAnswer(question)?.let { verifiedAnswer ->
            return BibiQueryResult(
                answer = verifiedAnswer,
                confidence = Confidence.HIGH,
                source = Source.LOCAL,
                intent = intent.name,
            )
        }
        if (InstitutionalDoctrinePolicy.needsOfficialSource(question)) {
            return attributionFallback()
        }
        val doctrinalContext = DoctrinalPerspectivePolicy.analyze(
            question, chatHistory, conversationPerspective,
        )
        if (intent == KnowledgeEngine.BibiIntent.COMPARE_VERSIONS &&
            bibiContext is BibiContext.Reader
        ) {
            return compareReaderVersions(context, question, bibiContext)
        }
        if (doctrinalContext.isDebatedQuestion && doctrinalContext.perspective == null) {
            return BibiQueryResult(
                answer = doctrinalContext.fallbackAnswer(),
                confidence = Confidence.MEDIUM,
                source = Source.LOCAL,
                intent = intent.name,
            )
        }
        val localResponse = if (forceRemote || doctrinalContext.isDebatedQuestion) {
            null
        } else {
            KnowledgeEngine.answer(
                context = context,
                question = question,
                userContext = bibiContext.toKnowledgeContext(userName, chatHistory, lastQueries),
            )
        }

        if (localResponse != null && localResponse.confidence != Confidence.LOW) {
            val localAnswer = localResponse.buildChatText()
            if (InstitutionalDoctrinePolicy.containsUnsupportedAttribution(localAnswer)) {
                return attributionFallback()
            }
            return BibiQueryResult(
                answer = localAnswer,
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
                doctrinalPerspective = doctrinalContext.perspective?.name?.lowercase(),
            ),
        )

        if (remoteResponse != null) {
            if (remoteResponse.intentDetected == "unverified_institutional_doctrine" ||
                remoteResponse.answer.trimStart().startsWith(
                    "No tengo una declaración oficial verificada",
                )
            ) {
                return attributionFallback()
            }
            if (listOf(remoteResponse.answer, remoteResponse.disclaimer.orEmpty())
                    .plus(remoteResponse.references.map { it.reason })
                    .plus(remoteResponse.suggestedBlocks)
                    .any(InstitutionalDoctrinePolicy::containsUnsupportedAttribution)
            ) {
                return attributionFallback()
            }
            if (doctrinalContext.isDebatedQuestion &&
                listOf(remoteResponse.answer, remoteResponse.disclaimer.orEmpty())
                    .plus(remoteResponse.references.map { it.reason })
                    .plus(remoteResponse.suggestedBlocks)
                    .any(DoctrinalPerspectivePolicy::containsUnsafeClaim)
            ) {
                return BibiQueryResult(
                    answer = doctrinalContext.fallbackAnswer(),
                    confidence = Confidence.MEDIUM,
                    source = Source.LOCAL,
                    intent = intent.name,
                )
            }
            val validatedReferences = BibiReferenceValidator.validate(
                references = remoteResponse.references,
                versionKey = bibiContext.bibleVersion,
            ) { book, chapter, version ->
                BibleRepository.getChapter(context, book, chapter, version)
                    .verses.mapNotNull { it.first.toIntOrNull() }.toSet()
            }
            val suggestions = validatedReferences.take(3).map {
                BibiSuggestion(
                    label = "Explorar ${it.reference}",
                    query = "explicame ${it.reference}",
                )
            }
            return BibiQueryResult(
                answer = if (doctrinalContext.isDebatedQuestion &&
                    doctrinalContext.perspective != null
                ) {
                    DoctrinalPerspectivePolicy.attributeAnswer(
                        remoteResponse.answer, doctrinalContext.perspective,
                    )
                } else remoteResponse.answer,
                suggestions = suggestions,
                references = validatedReferences.map {
                    BibiReference(it.reference, it.reason)
                },
                suggestedBlocks = remoteResponse.suggestedBlocks,
                disclaimer = if (doctrinalContext.isDebatedQuestion) {
                    val existing = remoteResponse.disclaimer?.takeIf { it.isNotBlank() }
                    if (existing?.contains("no juzga la fe", ignoreCase = true) == true) {
                        existing
                    } else {
                        listOfNotNull(
                            existing,
                            "Explicación desde la perspectiva que expresaste; no juzga la fe de otras personas.",
                        ).joinToString(" ")
                    }
                } else {
                    remoteResponse.disclaimer
                },
                confidence = remoteResponse.confidence.toConfidence(),
                source = Source.AI,
                intent = remoteResponse.intentDetected ?: intent.name,
            )
        }

        val fallback = (if (doctrinalContext.isDebatedQuestion) {
            doctrinalContext.fallbackAnswer()
        } else localResponse?.buildChatText())
            ?: "No pude procesar tu pregunta. Intenta mencionar un pasaje, personaje o tema biblico concreto."
        return BibiQueryResult(
            answer = fallback,
            confidence = if (doctrinalContext.isDebatedQuestion) {
                Confidence.MEDIUM
            } else localResponse?.confidence ?: Confidence.LOW,
            source = localResponse?.source ?: Source.LOCAL,
            resolvedTerm = localResponse?.title,
            intent = intent.name,
        )
    }

    private suspend fun compareReaderVersions(
        context: Context,
        question: String,
        reader: BibiContext.Reader,
    ): BibiQueryResult {
        fun localAnswer(text: String) = BibiQueryResult(
            answer = text,
            confidence = Confidence.HIGH,
            source = Source.LOCAL,
            intent = KnowledgeEngine.BibiIntent.COMPARE_VERSIONS.name,
        )
        val passages = reader.passages
            .distinctBy { Triple(it.book, it.chapter, it.verse) }
            .sortedBy { it.verse }
        if (passages.isEmpty()) {
            return localAnswer("Selecciona un versículo o indica una referencia para comparar versiones.")
        }
        if (passages.size > 5 || passages.any {
                it.book != passages.first().book || it.chapter != passages.first().chapter
            }) {
            return localAnswer("Puedo comparar hasta cinco versículos de un mismo capítulo a la vez.")
        }

        val available = BibleRepository.getAvailableVersions(context)
        val normalizedQuestion = question.lowercase().replace("rvr1960", "rv1960")
        val requested = available.filter { option ->
            Regex("(?<![a-z0-9])${Regex.escape(option.key)}(?![a-z0-9])")
                .containsMatchIn(normalizedQuestion)
        }
        if (requested.size > 2) {
            return localAnswer("Elige solo dos versiones para compararlas con claridad.")
        }
        val current = available.firstOrNull {
            it.key.equals(reader.bibleVersion, ignoreCase = true)
        }
        val versions = when {
            requested.size == 2 -> requested
            requested.size == 1 && current != null && requested.first().key != current.key ->
                listOf(current, requested.first())
            else -> emptyList()
        }
        if (versions.size < 2) {
            return localAnswer(
                "Indícame dos versiones distintas para comparar, por ejemplo RVR1960 y NVI. " +
                    "Solo usaré las versiones disponibles en Biblion."
            )
        }

        val book = passages.first().book
        val chapterNumber = passages.first().chapter
        val comparison = versions.map { version ->
            val verseText = BibleRepository.getChapter(
                context, book, chapterNumber, version.key,
            ).verses.toMap()
            version to passages.map { passage -> verseText[passage.verse.toString()] }
        }
        if (comparison.any { (_, texts) -> texts.any { it.isNullOrBlank() } }) {
            return localAnswer("No tengo todos los textos de ese pasaje en ambas versiones. No voy a inventar la traducción que falta.")
        }
        val reference = "${book} ${chapterNumber}:${passages.map { it.verse }.sorted().joinToString(", ")}"
        val answer = buildString {
            appendLine("Comparación textual de $reference:")
            comparison.forEach { (version, texts) ->
                appendLine()
                appendLine("${version.label}:")
                passages.forEachIndexed { index, passage ->
                    appendLine("${passage.verse}. ${texts[index]}")
                }
            }
            append("Estas son las redacciones disponibles en Biblion. Si quieres, pregúntame por una diferencia concreta entre ellas.")
        }
        return localAnswer(answer)
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
        selectedVerses = if (reader.hasExplicitSelection) {
            reader.passages.map { it.verse }.toSet()
        } else {
            emptySet()
        },
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
    KnowledgeEngine.BibiIntent.APPLICATION -> "application"
    KnowledgeEngine.BibiIntent.COMPARE_CONCEPTS -> "compare_concepts"
    KnowledgeEngine.BibiIntent.COMPARE_VERSIONS -> "compare_versions"
    KnowledgeEngine.BibiIntent.FALLBACK -> "unknown"
    else -> "question"
}

private fun String.toConfidence(): Confidence = when (lowercase()) {
    "high" -> Confidence.HIGH
    "low" -> Confidence.LOW
    else -> Confidence.MEDIUM
}

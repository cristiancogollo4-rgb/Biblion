package com.cristiancogollo.biblion.feature.bibi

import android.content.Context
import android.util.Log

private const val TAG = "KnowledgeEngine"

object KnowledgeEngine {

    enum class BibiIntent {
        DEFINE,
        WHO,
        WHERE,
        RELATED,
        ORIGINAL_LANG,
        EXPLAIN_VERSE,
        GREETING,
        DIVE_DEEPER,
        FALLBACK
    }

    data class UserContext(
        val book: String = "",
        val chapter: Int = 0,
        val verse: Int = 0,
        val selectedVerses: Set<Int> = emptySet(),
        val verseText: String = "",
        val verseRef: String = "",
        val userName: String? = null,
        val bibleVersion: String = "rv1960",
        val lastQueries: List<String> = emptyList(),
        val chatHistory: List<ChatExchange> = emptyList()
    )

    suspend fun answer(
        context: Context,
        question: String,
        userContext: UserContext = UserContext()
    ): BibiResponse? {
        Log.d(TAG, "Pregunta: $question")

        val intent = detectIntent(question, userContext.lastQueries)
        Log.d(TAG, "Intencion: $intent")

        val bibiCtx = BibiUserContext(
            userName = userContext.userName,
            currentBook = userContext.book.takeIf { it.isNotBlank() },
            currentChapter = userContext.chapter.takeIf { it > 0 },
            currentVerse = userContext.verse.takeIf { it > 0 },
            bibleVersion = userContext.bibleVersion,
            lastQueries = userContext.lastQueries,
            chatHistory = userContext.chatHistory
        )

        return when (intent) {
            BibiIntent.DEFINE -> handleDefine(context, question, bibiCtx)
            BibiIntent.WHO -> handleWho(context, question, bibiCtx)
            BibiIntent.WHERE -> handleWhere(context, question, bibiCtx)
            BibiIntent.RELATED -> handleRelated(context, question, userContext)
            BibiIntent.ORIGINAL_LANG -> handleOriginalLang(context, question)
            BibiIntent.EXPLAIN_VERSE -> handleExplainVerse(context, question, userContext, bibiCtx)
            BibiIntent.GREETING -> handleGreeting(userContext.userName)
            BibiIntent.DIVE_DEEPER -> null
            BibiIntent.FALLBACK -> null
        }
    }

    fun detectIntent(question: String, lastQueries: List<String> = emptyList()): BibiIntent {
        val q = removeAccents(question.lowercase().trim())

        return when {
            diveDeeperDetect(q, lastQueries) -> BibiIntent.DIVE_DEEPER

            q.startsWith("hola") || q.startsWith("buenas") ||
            q.startsWith("hey") || q.startsWith("saludos") ||
            q.startsWith("buen") -> BibiIntent.GREETING

            q.contains("hebreo") || q.contains("griego") ||
            q.contains("arameo") || q.contains("original") ||
            q.contains("strong") || q.matches(Regex(".*[hHgG]\\d+.*")) -> BibiIntent.ORIGINAL_LANG

            q.contains("quien era") || q.contains("quien es") ||
            q.contains("quien fue") || q.contains("quienes eran") ||
            q.contains("quien soy") ||
            q.contains("cuentame de") || q.contains("cuéntame de") ||
            q.contains("hablame de") || q.contains("háblame de") ||
            q.startsWith("dime sobre") ||
            q.startsWith("informacion de") || q.startsWith("información de") -> BibiIntent.WHO

            q.contains("donde queda") || q.contains("donde esta") ||
            q.contains("donde queda") || q.contains("donde se encuentra") ||
            q.contains("ubicacion de") || q.contains("ubicación de") ||
            q.contains("donde esta") || q.contains("donde está") ||
            q.contains("donde nacio") || q.contains("donde nació") ||
            q.contains("donde vivio") || q.contains("donde vivió") -> BibiIntent.WHERE

            q.contains("relacionad") || q.contains("similar") ||
            q.contains("otro pasaje") || q.contains("otros pasajes") ||
            q.contains("donde mas") || q.contains("donde más") ||
            q.contains("que mas dice") || q.contains("qué más dice") ||
            q.contains("pasajes similares") || q.contains("otros versiculos") ||
            q.contains("cross reference") || q.contains("versiculos relacionados") ||
            q.contains("pasajes que hablan") -> BibiIntent.RELATED

            q.contains("que significa") || q.contains("qué significa") ||
            q.contains("define") || q.contains("definir") ||
            q.contains("que es") || q.contains("qué es") ||
            q.contains("que fue") || q.contains("qué fue") ||
            q.contains("que es un") || q.contains("que es una") ||
            q.contains("definicion de") || q.contains("definición de") ||
            q.contains("significado de") ||
            q.contains("explica") || q.contains("explicame") ||
            q.contains("explícame") || q.startsWith("dime que") ||
            q.startsWith("dime qué") -> {
                if (q.contains("versiculo") || q.contains("pasaje") || q.contains("este texto")) {
                    BibiIntent.EXPLAIN_VERSE
                } else {
                    BibiIntent.DEFINE
                }
            }

            else -> BibiIntent.FALLBACK
        }
    }

    private fun diveDeeperDetect(query: String, lastQueries: List<String>): Boolean {
        val deepen = listOf(
            "profundiza", "profundiza", "amplia", "amplía",
            "mas detalle", "más detalle", "explica mas", "explica más",
            "cuentame mas", "cuéntame más", "dime mas", "dime más",
            "sigue", "continua", "continúa", "adelante",
            "y que mas", "y qué más", "y entonces", "despues", "después",
            "explicame eso", "explícame eso", "cuentame sobre eso", "cuéntame sobre eso"
        )
        if (deepen.any { it in query }) return true

        if (lastQueries.isNotEmpty()) {
            val isFollowUp = query.length < 15 &&
                listOf("y", "pero", "por que", "por qué", "como", "cómo", "a que", "a qué")
                    .any { it == query || query.startsWith(it) }
            if (isFollowUp) return true
        }

        return false
    }

    // region Handlers

    private suspend fun handleDefine(
        context: Context, question: String, bibiCtx: BibiUserContext
    ): BibiResponse? {
        val term = extractTermFromQuestion(question, bibiCtx.chatHistory) ?: return noTermFound()
        val result = DictionaryEngine.defineTerm(context, term, bibiCtx, bibiCtx.chatHistory)
        if (result != null) return result

        val alternatives = AmbiguousTermResolver.findClosest(context, term)
        return if (alternatives.isNotEmpty()) {
            BibiResponse(
                title = "No encontré \"$term\"",
                definition = "En el diccionario bíblico no tengo información sobre \"$term\".",
                followUp = "¿Quizás quisiste decir ${alternatives.joinToString(" o ")}?",
                suggestions = alternatives.map { BibiSuggestion("Consultar $it", "¿qué significa $it?") } +
                    BibiSuggestion("Preguntar de otra forma", "cuéntame sobre algo", isAi = true),
                confidence = Confidence.LOW
            )
        } else {
            BibiResponse(
                title = "No encontré \"$term\"",
                definition = "No tengo información sobre \"$term\" en el diccionario bíblico. " +
                    "Puedes intentar con otra palabra o preguntarme por un personaje o lugar.",
                followUp = "¿Pruebas con otra palabra?",
                suggestions = listOf(
                    BibiSuggestion("Buscar un personaje", "¿quién fue Moisés?"),
                    BibiSuggestion("Buscar un lugar", "¿dónde queda Galilea?"),
                    BibiSuggestion("Preguntar de otra forma", "cuéntame sobre algo", isAi = true)
                ),
                confidence = Confidence.LOW
            )
        }
    }

    private suspend fun handleWho(
        context: Context, question: String, bibiCtx: BibiUserContext
    ): BibiResponse? {
        val name = extractNameFromQuestion(question, listOf(
            "quien era", "quien es", "quien fue", "quienes eran",
            "cuentame de", "cuéntame de", "hablame de", "háblame de",
            "dime sobre", "informacion de", "información de"
        ), bibiCtx.chatHistory) ?: return noTermFound()

        val result = DictionaryEngine.defineTerm(context, name, bibiCtx, bibiCtx.chatHistory)
        if (result != null) return result

        val alternatives = AmbiguousTermResolver.findClosest(context, name)
        return if (alternatives.isNotEmpty()) {
            BibiResponse(
                title = "No encontré a \"$name\"",
                definition = "No tengo información sobre \"$name\" en mi base de datos de personajes bíblicos.",
                followUp = "¿Quizás quisiste decir ${alternatives.joinToString(" o ")}?",
                suggestions = alternatives.map { BibiSuggestion("Consultar $it", "¿quién fue $it?") },
                confidence = Confidence.LOW
            )
        } else {
            BibiResponse(
                title = "No encontré a \"$name\"",
                definition = "No tengo información sobre \"$name\" en mi base de datos de personajes bíblicos.",
                followUp = "¿Pruebas con otro nombre?",
                suggestions = listOf(
                    BibiSuggestion("Buscar otro personaje", "¿quién fue Moisés?"),
                    BibiSuggestion("Buscar un lugar", "¿dónde queda Jerusalén?")
                ),
                confidence = Confidence.LOW
            )
        }
    }

    private suspend fun handleWhere(
        context: Context, question: String, bibiCtx: BibiUserContext
    ): BibiResponse? {
        val name = extractNameFromQuestion(question, listOf(
            "donde queda", "donde esta", "donde se encuentra",
            "ubicacion de", "ubicación de",
            "donde esta", "donde está",
            "donde nacio", "donde nació",
            "donde vivio", "donde vivió"
        ), bibiCtx.chatHistory) ?: return noTermFound()

        val result = DictionaryEngine.defineTerm(context, name, bibiCtx, bibiCtx.chatHistory)
        if (result != null) return result

        val alternatives = AmbiguousTermResolver.findClosest(context, name)
        return if (alternatives.isNotEmpty()) {
            BibiResponse(
                title = "No encontré \"$name\"",
                definition = "No tengo información sobre \"$name\" en mi base de datos de lugares bíblicos.",
                followUp = "¿Quizás quisiste decir ${alternatives.joinToString(" o ")}?",
                suggestions = alternatives.map { BibiSuggestion("Consultar $it", "¿dónde queda $it?") },
                confidence = Confidence.LOW
            )
        } else {
            BibiResponse(
                title = "No encontré \"$name\"",
                definition = "No tengo información sobre \"$name\" en mi base de datos de lugares bíblicos.",
                followUp = "¿Pruebas con otro lugar?",
                suggestions = listOf(
                    BibiSuggestion("Buscar un personaje", "¿quién fue Moisés?"),
                    BibiSuggestion("Buscar otro lugar", "¿dónde queda Galilea?")
                ),
                confidence = Confidence.LOW
            )
        }
    }

    private suspend fun handleRelated(
        context: Context,
        question: String,
        userContext: UserContext
    ): BibiResponse? {
        // Resolver versiculo activo con Forma 1 (seleccion) o Forma 2 (texto)
        val readerSnapshot = VerseResolver.ReaderSnapshot(
            bookName = userContext.book.takeIf { it.isNotBlank() },
            chapter = userContext.chapter,
            selectedVerses = userContext.selectedVerses
        )
        val resolution = VerseResolver.resolve(readerSnapshot, question)

        if (resolution.source == VerseResolver.Source.NONE) {
            return BibiResponse(
                title = "Para buscar pasajes relacionados",
                definition = "Necesito que estés leyendo un versículo. " +
                    "Abre un capítulo de la Biblia y luego pregúntame de nuevo.",
                followUp = "Cuando estés leyendo, puedo mostrarte pasajes similares.",
                suggestions = listOf(BibiSuggestion("Ir al lector", "abrir lector"))
            )
        }

        // Si no hay versiculo exacto, pedir aclaracion
        if (!resolution.hasVerse) {
            return BibiResponse(
                title = "Necesito el versículo específico",
                definition = "Estás en ${resolution.book} ${resolution.chapter}. " +
                    "¿Sobre qué versículo quieres que busque pasajes relacionados? " +
                    "Puedes seleccionarlo en el lector o escribirlo en tu pregunta " +
                    "(por ejemplo: 'versículos relacionados con ${resolution.book} 1:1').",
                followUp = "Indícame el versículo exacto para darte referencias relevantes.",
                suggestions = listOf(
                    BibiSuggestion("Versículos sobre este capítulo", "versículos sobre ${resolution.book} ${resolution.chapter}"),
                    BibiSuggestion("Saber más con IA", "explícame ${resolution.book} ${resolution.chapter}", isAi = true)
                )
            )
        }

        // Usar el nuevo motor basado en openbile (voto INTERNO, no se muestra)
        val related = CrossReferenceVoteEngine.getRelatedBySource(
            context = context,
            book = resolution.book,
            chapter = resolution.chapter,
            verse = resolution.verse!!,
            minVotes = CrossReferenceVoteEngine.DEFAULT_MIN_VOTES,
            maxTotal = 6
        )

        if (related.isEmpty()) {
            return BibiResponse(
                title = "Sin resultados",
                definition = "No encontré pasajes relacionados para ${resolution.book} ${resolution.chapter}:${resolution.verse}.",
                followUp = "¿Quieres buscar otro versículo?",
                suggestions = listOf(
                    BibiSuggestion("Explicar este versículo", "explícame ${resolution.book} ${resolution.chapter}:${resolution.verse}"),
                    BibiSuggestion("Saber más con IA", "profundiza en ${resolution.book} ${resolution.chapter}:${resolution.verse}", isAi = true)
                )
            )
        }

        val sb = StringBuilder()
        sb.append("Estás leyendo ${resolution.book} ${resolution.chapter}:${resolution.verse}. ")
        sb.append("Estos son otros pasajes que se relacionan con lo que estás leyendo:\n\n")
        sb.append("🔗 Pasajes relacionados:\n\n")

        for (verse in related) {
            val text = verse.text.take(110)
            val ellipsis = if (verse.text.length > 110) "..." else ""
            sb.append("  • ${verse.reference}: \"$text$ellipsis\"\n")
        }

        sb.append("\n¿Quieres que profundice en alguno de estos pasajes?")

        return BibiResponse(
            title = "Pasajes relacionados con ${resolution.book} ${resolution.chapter}:${resolution.verse}",
            definition = sb.toString(),
            followUp = "¿Te interesa alguno de estos pasajes?",
            suggestions = listOf(
                BibiSuggestion("Explicar este versículo", "explícame ${resolution.book} ${resolution.chapter}:${resolution.verse}"),
                BibiSuggestion("Saber más con IA", "profundiza en ${resolution.book} ${resolution.chapter}:${resolution.verse}", isAi = true),
                BibiSuggestion("¿De qué temas habla este versículo?", "temas de ${resolution.book} ${resolution.chapter}:${resolution.verse}")
            )
        )
    }

    private suspend fun handleOriginalLang(context: Context, question: String): BibiResponse? {
        val strongsMatch = Regex("[HhGg]\\d+").find(question)
        if (strongsMatch != null) {
            val result = StrongEngine.lookupByNumber(context, strongsMatch.value) ?: return BibiResponse(
                title = strongsMatch.value,
                definition = "No encontré el número de Strong's ${strongsMatch.value} en el léxico.",
                followUp = "¿Tienes otro número de Strong's para buscar?",
                suggestions = listOf(BibiSuggestion("Buscar palabra", "¿qué significa X en hebreo?"))
            )
            return BibiResponse(
                title = strongsMatch.value,
                definition = result,
                followUp = "¿Quieres buscar otra palabra original?",
                suggestions = listOf(BibiSuggestion("Buscar palabra en hebreo", "¿qué significa X en hebreo?"))
            )
        }

        val term = extractTermFromQuestion(question) ?: return noTermFound()
        val result = StrongEngine.searchByText(context, term)
        if (result != null) {
            return BibiResponse(
                title = term,
                definition = result,
                followUp = "¿Quieres buscar otra palabra en hebreo o griego?",
                suggestions = listOf(BibiSuggestion("Buscar por Strong's", "H1254"))
            )
        }

        return BibiResponse(
            title = term,
            definition = "No encontré \"$term\" en el léxico de hebreo y griego.",
            followUp = "Si conoces el número de Strong's (ej: H1254), puedo buscarlo directamente.",
            suggestions = listOf(
                BibiSuggestion("Definir término bíblico", "¿qué significa $term?"),
                BibiSuggestion("Buscar por Strong's", "H1254")
            ),
            confidence = Confidence.LOW
        )
    }

    private suspend fun handleExplainVerse(
        context: Context,
        question: String,
        userContext: UserContext,
        bibiCtx: BibiUserContext
    ): BibiResponse? {
        if (userContext.verseText.isEmpty()) {
            return BibiResponse(
                title = "Para explicarte un versículo",
                definition = "Necesito que estés leyendo uno. " +
                    "Abre un pasaje de la Biblia y pregúntame de nuevo.",
                followUp = "¿Ya estás leyendo algún pasaje?"
            )
        }

        return DictionaryEngine.explainVerse(
            context, userContext.verseText, userContext.verseRef, bibiCtx, bibiCtx.chatHistory
        )
    }

    private fun handleGreeting(userName: String?): BibiResponse {
        val name = userName?.substringBefore(" ")?.takeIf { it.isNotBlank() }
        val greet = if (name != null) "¡Hola $name!" else "¡Hola!"
        return BibiResponse(
            greeting = null,
            title = "$greet Soy Bibi, tu guía en Biblion.",
            definition = "Puedo ayudarte a explorar la Biblia de varias formas:\n\n" +
                "• Definir palabras o conceptos: \"¿qué significa gracia?\"\n" +
                "• Contarte sobre personajes: \"¿quién fue Abraham?\"\n" +
                "• Mostrarte lugares: \"¿dónde queda Galilea?\"\n" +
                "• Buscar pasajes relacionados al que lees\n" +
                "• Explicarte palabras en hebreo y griego\n\n" +
                "¿Con qué te gustaría empezar?",
            followUp = "¿Qué quieres saber?",
            suggestions = listOf(
                BibiSuggestion("Buscar un personaje", "¿quién fue Moisés?"),
                BibiSuggestion("Buscar un lugar", "¿dónde queda Jerusalén?"),
                BibiSuggestion("Definir un concepto", "¿qué significa pacto?"),
                BibiSuggestion("Explicar el versículo actual", "explícame este versículo")
            )
        )
    }

    private fun noTermFound(): BibiResponse {
        return BibiResponse(
            title = "No entendí tu pregunta",
            definition = "No pude identificar qué término quieres buscar. " +
                "Intenta con frases como \"¿qué significa pacto?\" o \"¿quién fue Moisés?\".",
            followUp = "¿Cómo puedo ayudarte?",
            suggestions = listOf(
                BibiSuggestion("Definir un concepto", "¿qué significa gracia?"),
                BibiSuggestion("Buscar un personaje", "¿quién fue David?"),
                BibiSuggestion("Buscar un lugar", "¿dónde está Galilea?")
            ),
            confidence = Confidence.LOW
        )
    }

    // endregion

    // region Extractors

    private fun extractTermFromQuestion(question: String, chatHistory: List<ChatExchange> = emptyList()): String? {
        val patterns = listOf(
            "que significa", "qué significa", "define", "definir",
            "que es un", "qué es un", "que es una", "qué es una",
            "que es", "qué es", "que fue", "qué fue",
            "definicion de", "definición de", "significado de",
            "explica", "explicame", "explícame",
            "que dice el original de", "qué dice el original de",
            "en hebreo de", "en griego de",
            "dime que", "dime qué"
        )

        val lower = removeAccents(question.lowercase().trim())
        for (pattern in patterns) {
            if (lower.contains(pattern)) {
                val after = lower.substringAfter(pattern).trim()
                val cleaned = after
                    .removePrefix("el ").removePrefix("la ").removePrefix("los ").removePrefix("las ")
                    .removePrefix("un ").removePrefix("una ")
                    .removePrefix("este versiculo").removePrefix("este pasaje")
                    .removePrefix("este versículo").removePrefix("este pasaje")
                    .removeSuffix("?").removeSuffix("??")
                    .trim()
                if (cleaned.length >= 2) return cleaned
            }
        }

        // Anáfora: si la pregunta es corta y no se extrajo término, usar el último del historial
        if (lower.length < 20 && !hasOwnSubject(lower)) {
            val lastTerm = chatHistory.lastOrNull { it.resolvedTerm != null }?.resolvedTerm
            if (lastTerm != null) return lastTerm
        }

        return null
    }

    private fun extractNameFromQuestion(question: String, prefixes: List<String>, chatHistory: List<ChatExchange> = emptyList()): String? {
        val lower = removeAccents(question.lowercase().trim())
        for (prefix in prefixes) {
            if (lower.contains(prefix)) {
                val after = lower.substringAfter(prefix).trim()
                val cleaned = after
                    .removePrefix("el ").removePrefix("la ").removePrefix("los ").removePrefix("las ")
                    .removeSuffix("?").removeSuffix("??")
                    .trim()
                if (cleaned.length >= 2) return cleaned
            }
        }

        // Anáfora: si la pregunta es corta y no se extrajo nombre, usar el último del historial
        if (lower.length < 20 && !hasOwnSubject(lower)) {
            val lastTerm = chatHistory.lastOrNull { it.resolvedTerm != null }?.resolvedTerm
            if (lastTerm != null) return lastTerm
        }

        return null
    }

    private fun hasOwnSubject(text: String): Boolean {
        val knownSubjects = listOf(
            "dios", "jesus", "cristo", "espiritu", "santo", "biblia",
            "genesis", "exodo", "salmos", "proverbios", "mateo", "juan",
            "pablo", "pedro", "moises", "abraham", "david", "pacto",
            "gracia", "fe", "amor", "oracion"
        )
        return knownSubjects.any { it in text }
    }

    private fun removeAccents(text: String): String {
        val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
    }

    // endregion
}

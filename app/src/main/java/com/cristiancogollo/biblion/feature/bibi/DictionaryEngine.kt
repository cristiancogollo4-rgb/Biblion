package com.cristiancogollo.biblion.feature.bibi

import android.content.Context
import android.util.Log
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryCategory
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryEntry
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryRepository

private const val TAG = "DictionaryEngine"

object DictionaryEngine {

    suspend fun defineTerm(
        context: Context,
        term: String,
        userContext: BibiUserContext? = null,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse? {
        Log.d(TAG, "Definiendo: $term")
        val entry = DictionaryRepository.getEntryByTerm(context, term)
            ?: DictionaryRepository.searchEntries(context, term).firstOrNull()?.let { result ->
                DictionaryRepository.getEntryById(context, result.id)
            }

        if (entry == null) {
            Log.d(TAG, "No se encontro: $term")
            return null
        }

        return formatByCategory(context, entry, userContext, chatHistory)
    }

    suspend fun findTermsInVerse(
        context: Context,
        verseText: String,
        maxTerms: Int = 5
    ): List<DictionaryEntry> {
        val words = extractSignificantWords(verseText)
        val found = mutableListOf<DictionaryEntry>()
        val seen = mutableSetOf<Long>()

        for (word in words) {
            if (found.size >= maxTerms) break
            if (word.length < 4) continue

            val entry = DictionaryRepository.getEntryByTerm(context, word)
            if (entry != null && entry.id !in seen) {
                seen.add(entry.id)
                found.add(entry)
            }
        }

        return found
    }

    suspend fun explainVerse(
        context: Context,
        verseText: String,
        verseRef: String,
        userContext: BibiUserContext? = null,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse {
        val terms = findTermsInVerse(context, verseText)
        val greet = userContext?.let { buildGreeting(it) }

        if (terms.isEmpty()) {
            return BibiResponse(
                greeting = greet,
                title = "Sobre $verseRef",
                definition = "En este versículo no encuentro términos específicos del diccionario. " +
                    "Puedes preguntarme por cualquier palabra que veas aquí y te explicaré su significado.",
                followUp = "¿Qué término te gustaría explorar?",
                suggestions = listOf(
                    BibiSuggestion("Explicar todo el pasaje con IA", "explica este pasaje a profundidad", isAi = true)
                )
            )
        }

        val termList = terms.take(3).joinToString(", ") { it.term }
        return BibiResponse(
            greeting = greet,
            title = "Términos en $verseRef",
            definition = "Encuentro algunos términos que puedo explicarte:",
            details = terms.take(3).map { entry ->
                "${entry.term}: ${entry.definition.take(200)}${if (entry.definition.length > 200) "..." else ""}"
            },
            followUp = if (terms.size > 3) {
                "También encuentro: ${terms.drop(3).joinToString(", ") { it.term }}. " +
                    "¿Quieres que profundice en alguno?"
            } else {
                "¿Quieres que profundice en alguno?"
            },
            suggestions = terms.take(3).map { term ->
                BibiSuggestion(
                    "Pedir a IA: pasajes sobre ${term.term}",
                    "profundiza qué pasajes hablan de ${term.term}",
                    isAi = true
                )
            }
        )
    }

    private suspend fun formatByCategory(
        context: Context,
        entry: DictionaryEntry,
        userContext: BibiUserContext?,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse {
        return when (entry.category) {
            DictionaryCategory.PERSON -> formatPerson(context, entry, userContext, chatHistory)
            DictionaryCategory.PLACE -> formatPlace(context, entry, userContext, chatHistory)
            DictionaryCategory.CONCEPT -> formatConcept(context, entry, userContext, chatHistory)
            DictionaryCategory.OBJECT -> formatObject(context, entry, userContext, chatHistory)
            DictionaryCategory.PRACTICE -> formatPractice(context, entry, userContext, chatHistory)
            DictionaryCategory.EVENT -> formatEvent(context, entry, userContext, chatHistory)
            DictionaryCategory.OTHER -> formatGeneric(context, entry, userContext, chatHistory)
        }
    }

    private suspend fun formatPerson(
        context: Context,
        entry: DictionaryEntry,
        userContext: BibiUserContext?,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse {
        val greet = userContext?.let { buildGreeting(it) }
        val metadata = mutableListOf<MetadataFact>()

        if (!entry.gender.isNullOrEmpty()) {
            metadata.add(MetadataFact("", "Género",
                if (entry.gender == "Male") "Masculino" else if (entry.gender == "Female") "Femenino" else entry.gender))
        }
        if (!entry.birthYear.isNullOrEmpty() && entry.birthYear != "0") {
            metadata.add(MetadataFact("", "Nacimiento", formatYear(entry.birthYear)))
        }
        if (!entry.deathYear.isNullOrEmpty() && entry.deathYear != "0") {
            metadata.add(MetadataFact("", "Muerte", formatYear(entry.deathYear)))
        }

        val pronouns = if (entry.gender == "Female") "ella" else "él"

        val details = mutableListOf<String>()
        if (!entry.aliases.isNullOrEmpty()) {
            details.add("También conocido como: ${entry.aliases}")
        }
        if (!entry.displayTitle.isNullOrEmpty() && entry.displayTitle != entry.term) {
            details.add("Su nombre significa \"${entry.displayTitle}\".")
        }

        return BibiResponse(
            greeting = greet,
            title = entry.term,
            subtitle = entry.displayTitle?.takeIf { it != entry.term },
            definition = entry.definition,
            details = details,
            metadata = metadata,
            followUp = "¿Quieres saber más sobre ${pronouns} o algún aspecto en particular?",
            suggestions = buildSuggestions(entry.term, chatHistory)
        )
    }

    private suspend fun formatPlace(
        context: Context,
        entry: DictionaryEntry,
        userContext: BibiUserContext?,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse {
        val greet = userContext?.let { buildGreeting(it) }
        val metadata = mutableListOf<MetadataFact>()

        if (!entry.latitude.isNullOrEmpty()) {
            metadata.add(MetadataFact("", "Ubicación", "${entry.latitude}, ${entry.longitude}"))
        }
        if (!entry.featureType.isNullOrEmpty()) {
            metadata.add(MetadataFact("", "Tipo", translateFeatureType(entry.featureType)))
        }
        if (!entry.aliases.isNullOrEmpty()) {
            metadata.add(MetadataFact("", "También conocido como", entry.aliases))
        }

        return BibiResponse(
            greeting = greet,
            title = "Lugar: ${entry.term}",
            subtitle = entry.displayTitle?.takeIf { it != entry.term },
            definition = entry.definition,
            metadata = metadata,
            followUp = "¿Qué más te gustaría saber sobre este lugar?",
            suggestions = buildSuggestions(entry.term, chatHistory)
        )
    }

    private suspend fun formatConcept(
        context: Context,
        entry: DictionaryEntry,
        userContext: BibiUserContext?,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse {
        val greet = userContext?.let { buildGreeting(it) }
        return BibiResponse(
            greeting = greet,
            title = entry.term,
            definition = entry.definition,
            followUp = "¿Quieres profundizar en este concepto o ver cómo se aplica?",
            suggestions = buildSuggestions(entry.term, chatHistory)
        )
    }

    private suspend fun formatObject(
        context: Context,
        entry: DictionaryEntry,
        userContext: BibiUserContext?,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse {
        val greet = userContext?.let { buildGreeting(it) }
        return BibiResponse(
            greeting = greet,
            title = entry.term,
            definition = entry.definition,
            followUp = "¿Te interesa algún otro objeto o elemento bíblico?",
            suggestions = buildSuggestions(entry.term, chatHistory)
        )
    }

    private suspend fun formatPractice(
        context: Context,
        entry: DictionaryEntry,
        userContext: BibiUserContext?,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse {
        val greet = userContext?.let { buildGreeting(it) }
        return BibiResponse(
            greeting = greet,
            title = entry.term,
            definition = entry.definition,
            followUp = "¿Quieres explorar más prácticas o costumbres bíblicas?",
            suggestions = buildSuggestions(entry.term, chatHistory)
        )
    }

    private suspend fun formatEvent(
        context: Context,
        entry: DictionaryEntry,
        userContext: BibiUserContext?,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse {
        val greet = userContext?.let { buildGreeting(it) }
        return BibiResponse(
            greeting = greet,
            title = entry.term,
            definition = entry.definition,
            followUp = "¿Te gustaría conocer más eventos de esta época?",
            suggestions = buildSuggestions(entry.term, chatHistory)
        )
    }

    private suspend fun formatGeneric(
        context: Context,
        entry: DictionaryEntry,
        userContext: BibiUserContext?,
        chatHistory: List<ChatExchange> = emptyList()
    ): BibiResponse {
        val greet = userContext?.let { buildGreeting(it) }
        return BibiResponse(
            greeting = greet,
            title = entry.term,
            definition = entry.definition,
            followUp = "¿Quieres que busque algo más?",
            suggestions = buildSuggestions(entry.term, chatHistory)
        )
    }

    /**
     * Construye las sugerencias de seguimiento (chips) tras una respuesta.
     * Solo incluye sugerencias que Bibi SÍ puede contestar:
     * - Buscar pasajes sobre X: usa DIVE_DEEPER → Worker (CrossReferenceEngine
     *   solo busca por versículo actual, no por tema)
     * - Comparar con último término: usa DIVE_DEEPER → Worker
     * - Profundizar con IA: usa DIVE_DEEPER → Worker
     * Si hay historial conversacional con otro término, ofrece comparación.
     */
    internal fun buildSuggestions(
        term: String,
        chatHistory: List<ChatExchange> = emptyList()
    ): List<BibiSuggestion> {
        val suggestions = mutableListOf<BibiSuggestion>()

        suggestions.add(
            BibiSuggestion(
                "Pedir a IA: pasajes sobre $term",
                "profundiza qué pasajes hablan de $term",
                isAi = true
            )
        )

        val lastDifferentTerm = chatHistory
            .lastOrNull { it.resolvedTerm != null && it.resolvedTerm.lowercase() != term.lowercase() }
            ?.resolvedTerm

        if (lastDifferentTerm != null) {
            suggestions.add(
                BibiSuggestion(
                    "Comparar con $lastDifferentTerm",
                    "qué diferencia hay entre $term y $lastDifferentTerm",
                    isAi = true
                )
            )
        } else {
            suggestions.add(
                BibiSuggestion("Profundizar con IA", "profundiza sobre $term", isAi = true)
            )
        }

        return suggestions
    }

    private fun buildGreeting(userContext: BibiUserContext): String {
        val name = userContext.userName
        val book = userContext.currentBook
        val chapter = userContext.currentChapter

        val greeting = if (name != null) {
            "¡Hola $name!"
        } else {
            "¡Hola!"
        }

        val contextNote = if (book != null && chapter != null && chapter > 0) {
            " Veo que estás leyendo $book $chapter."
        } else if (book != null) {
            " Veo que estás leyendo $book."
        } else {
            ""
        }

        return "$greeting$contextNote Te cuento."
    }

    internal fun formatYear(isoYear: String): String {
        val year = isoYear.toIntOrNull() ?: return isoYear
        return if (year < 0) {
            "${-year} a.C."
        } else if (year == 0) {
            "1 a.C."
        } else {
            "$year d.C."
        }
    }

    internal fun translateFeatureType(type: String): String {
        return when (type.lowercase()) {
            "city" -> "Ciudad"
            "region" -> "Región"
            "country" -> "País"
            "mountain" -> "Monte"
            "river" -> "Río"
            "water" -> "Agua/Mar"
            "valley" -> "Valle"
            "desert" -> "Desierto"
            "island" -> "Isla"
            "plain" -> "Llanura"
            else -> type
        }
    }

    internal fun extractSignificantWords(text: String): List<String> {
        val stopwords = setOf(
            "the", "and", "was", "were", "from", "with", "that", "this", "have",
            "been", "which", "their", "there", "would", "will", "into", "upon",
            "ellos", "ellas", "estos", "estas", "esos", "esas", "para", "por",
            "con", "sin", "sobre", "entre", "hasta", "desde", "hacia", "ante",
            "bajo", "contra", "según", "durante", "mediante", "contra",
            "pero", "porque", "cuando", "donde", "como", "que", "se", "su",
            "sus", "del", "los", "las", "una", "uno", "unos", "unas", "esta",
            "este", "ese", "esa", "aquel", "aquella", "todo", "toda", "todos",
            "todas", "más", "menos", "muy", "tan", "tanto", "poco", "mucho",
            "también", "así", "entonces", "ahora", "después", "antes", "siempre",
            "nunca", "jamás", "cada", "otro", "otra", "otros", "otras"
        )

        return text.split(Regex("[\\s,.;:!?\"'()\\[\\]{}—–-]+"))
            .map { it.trim() }
            .filter { it.length > 3 && it.lowercase() !in stopwords }
            .distinct()
    }

    internal fun truncateText(text: String, maxLen: Int): String {
        if (text.length <= maxLen) return text
        val truncated = text.take(maxLen)
        val lastPeriod = truncated.lastIndexOf(".")
        return if (lastPeriod > maxLen * 0.6) {
            truncated.take(lastPeriod + 1)
        } else {
            "$truncated..."
        }
    }
}


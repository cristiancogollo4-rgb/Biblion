package com.cristiancogollo.biblion.feature.bibi.engine

import android.content.Context
import android.util.Log
import com.cristiancogollo.biblion.feature.bibi.model.BibiResponse
import com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion
import com.cristiancogollo.biblion.feature.bibi.model.BibiUserContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiVerse
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import com.cristiancogollo.biblion.feature.bibi.model.Confidence
import com.cristiancogollo.biblion.feature.bibi.model.MetadataFact
import com.cristiancogollo.biblion.feature.bibi.model.Source
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryRepository

private const val TAG = "KnowledgeEngine"

object KnowledgeEngine {

    enum class BibiIntent {
        DEFINE,
        WHO,
        WHERE,
        RELATED,
        TOPICS,
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
            BibiIntent.TOPICS -> handleTopics(context, question, userContext)
            BibiIntent.ORIGINAL_LANG -> handleOriginalLang(context, question)
            BibiIntent.EXPLAIN_VERSE -> handleExplainVerse(context, question, userContext, bibiCtx)
            BibiIntent.GREETING -> handleGreeting(userContext.userName)
            BibiIntent.DIVE_DEEPER -> handleDiveDeeper(context, question, bibiCtx)
            BibiIntent.FALLBACK -> null
        }
    }

    private val verseRefRegex = Regex("\\w+\\s+\\d+:\\d+")

    fun detectIntent(question: String, lastQueries: List<String> = emptyList()): BibiIntent {
        val q = removeAccents(question.lowercase().trim())

        return when {
            diveDeeperDetect(q, lastQueries) -> BibiIntent.DIVE_DEEPER

            q.startsWith("hola") || q.startsWith("buenas") ||
            q.startsWith("hey") || q.startsWith("saludos") ||
            q.startsWith("buen") || q.startsWith("buenos dias") ||
            q.startsWith("buenas tardes") || q.startsWith("buenas noches") ||
            q.startsWith("que tal") || q.startsWith("qué tal") ||
            q.startsWith("como estas") || q.startsWith("cómo estás") ||
            q.startsWith("que hay") || q.startsWith("que onda") ||
            q.startsWith("que hubo") || q.startsWith("hello") ||
            q.split(Regex("\\s"))[0].trimEnd('!', '?', '.', ',') == "hi" -> BibiIntent.GREETING

            q.contains("hebreo") || q.contains("griego") ||
            q.contains("arameo") || q.contains("original") ||
            q.contains("strong") || q.contains("en original") ||
            q.contains("lengua original") || q.contains("palabra original") ||
            q.contains("traduccion literal") || q.contains("traducción literal") ||
            q.contains("etimologia") || q.contains("etimología") ||
            q.contains("raiz de la palabra") || q.contains("raíz de la palabra") ||
            q.matches(Regex(".*[hHgG]\\d+.*")) -> BibiIntent.ORIGINAL_LANG

            q.contains("quien era") || q.contains("quien es") ||
            q.contains("quien fue") || q.contains("quienes eran") ||
            q.contains("quien soy") ||
            q.contains("cuentame de") || q.contains("cuéntame de") ||
            q.contains("cuentame sobre") || q.contains("cuéntame sobre") ||
            q.contains("hablame de") || q.contains("háblame de") ||
            q.contains("hablame sobre") || q.contains("háblame sobre") ||
            q.contains("habla de") || q.contains("hablar de") ||
            q.contains("historia de") ||
            q.contains("cuál es la historia") || q.contains("cual es la historia") ||
            q.contains("qué sabes de") || q.contains("que sabes de") ||
            q.contains("dime algo de") ||
            q.contains("dime quién es") || q.contains("dime quien es") ||
            q.contains("dime quién fue") || q.contains("dime quien fue") ||
            q.contains("dime quién era") || q.contains("dime quien era") ||
            q.contains("me puedes decir quién") || q.contains("me puedes decir quien") ||
            q.contains("quiero saber quién") || q.contains("quiero saber quien") ||
            q.contains("necesito saber quién") || q.contains("necesito saber quien") ||
            q.contains("conoces a") || q.contains("sabes quién") ||
            q.contains("sabes quien") ||
            q.startsWith("dime sobre") ||
            q.startsWith("informacion de") || q.startsWith("información de") -> BibiIntent.WHO

            q.contains("donde queda") || q.contains("donde esta") ||
            q.contains("donde se encuentra") ||
            q.contains("donde quedaba") || q.contains("donde estaba") ||
            q.contains("donde nacio") || q.contains("donde nació") ||
            q.contains("donde vivio") || q.contains("donde vivió") ||
            q.contains("ubicacion de") || q.contains("ubicación de") ||
            q.contains("en que lugar") || q.contains("en qué lugar") ||
            q.contains("cuál es la ubicacion") || q.contains("cuál es la ubicación") ||
            q.contains("cual es la ubicacion") ||
            q.contains("donde se ubica") || q.contains("donde se localiza") ||
            q.contains("se encuentra en") || q.contains("estaba en") ||
            q.contains("quedaba en") || q.contains("vivia en") ||
            q.contains("vivía en") || q.contains("nacio en") ||
            q.contains("nació en") || q.contains("situado en") ||
            q.contains("localizado en") -> BibiIntent.WHERE

            q.contains("relacionad") || q.contains("similar") ||
            q.contains("parecid") || q.contains("otro pasaje") ||
            q.contains("otros pasajes") || q.contains("donde mas") ||
            q.contains("donde más") || q.contains("que mas dice") ||
            q.contains("qué más dice") || q.contains("pasajes similares") ||
            q.contains("pasajes parecidos") ||
            q.contains("otros versiculos") || q.contains("otros versículos") ||
            q.contains("que otros pasajes") || q.contains("qué otros pasajes") ||
            q.contains("que otros versiculos") || q.contains("qué otros versículos") ||
            q.contains("cross reference") ||
            q.contains("versiculos relacionados") ||
            q.contains("versículos relacionados") ||
            q.contains("pasajes que hablan") ||
            q.contains("versiculos que hablan") ||
            q.contains("versículos que hablan") ||
            q.contains("paralelo") || q.contains("pasaje paralelo") ||
            q.contains("comparar con") || q.contains("compara con") ||
            q.contains("qué relacion tiene") || q.contains("que relacion tiene") ||
            q.contains("se relaciona con") || q.contains("tiene que ver con") -> BibiIntent.RELATED

            q.contains("que significa") || q.contains("qué significa") ||
            q.contains("cual es la definicion") || q.contains("cuál es la definición") ||
            q.contains("cual es el significado") || q.contains("cuál es el significado") ||
            q.contains("que quiere decir") || q.contains("qué quiere decir") ||
            q.contains("definicion de") || q.contains("definición de") ||
            q.contains("significado de") ||
            q.contains("que es un") || q.contains("qué es un") ||
            q.contains("que es una") || q.contains("qué es una") ||
            q.contains("que es") || q.contains("qué es") ||
            q.contains("que fue") || q.contains("qué fue") ||
            q.contains("que representa") || q.contains("qué representa") ||
            q.contains("define") || q.contains("definir") ||
            q.contains("explícame") || q.contains("explicame") ||
            q.contains("explica") ||
            q.startsWith("dime que") || q.startsWith("dime qué") -> {
                if (q.contains("versiculo") || q.contains("pasaje") ||
                    q.contains("este texto") || q.contains("este capitulo") ||
                    q.contains("este capítulo") ||
                    verseRefRegex.containsMatchIn(q)) {
                    BibiIntent.EXPLAIN_VERSE
                } else {
                    BibiIntent.DEFINE
                }
            }

            q.contains("temas de") || q.contains("temas del") ||
            q.contains("tema de") || q.contains("tema del") ||
            q.contains("de que temas") || q.contains("de qué temas") ||
            q.contains("de que tema") || q.contains("de qué tema") ||
            q.contains("que temas habla") || q.contains("qué temas habla") ||
            q.contains("que tema habla") || q.contains("qué tema habla") ||
            q.contains("de que trata") || q.contains("de qué trata") ||
            q.contains("sobre que trata") || q.contains("sobre qué trata") ||
            q.contains("que temas toca") || q.contains("qué temas toca") ||
            q.contains("que temas aborda") || q.contains("qué temas aborda") ||
            q.contains("que tema toca") || q.contains("qué tema toca") ||
            q.contains("que tema aborda") || q.contains("qué tema aborda") ||
            q.contains("cuales son los temas") || q.contains("cuáles son los temas") ||
            q.contains("se trata") || q.contains("de que se habla") ||
            q.contains("de qué se habla") ||
            q.contains("que temas menciona") || q.contains("qué temas menciona") ||
            q.contains("que tema menciona") || q.contains("qué tema menciona") -> BibiIntent.TOPICS

            else -> BibiIntent.FALLBACK
        }
    }

    private fun diveDeeperDetect(query: String, lastQueries: List<String>): Boolean {
        val deepen = listOf(
            "profundiza", "amplia", "amplía",
            "mas detalle", "más detalle",
            "explica mas", "explica más",
            "cuentame mas", "cuéntame más",
            "cuentame mas de", "cuéntame más de",
            "dime mas", "dime más",
            "dime mas de", "dime más de",
            "hablame mas de", "háblame más de",
            "háblame más", "hablame más",
            "sigue", "continua", "continúa", "continuemos", "sigamos",
            "adelante",
            "y que mas", "y qué más", "y entonces",
            "despues", "después",
            "explicame eso", "explícame eso",
            "cuentame sobre eso", "cuéntame sobre eso",
            "profundiza mas", "profundiza más",
            "amplia mas", "amplía más",
            "qué más sabes", "que mas sabes",
            "qué más puedes decir", "que mas puedes decir",
            "cuéntame otro", "cuentame otro",
            "dime otro",
            "quiero saber más", "quiero saber mas",
            "necesito más", "necesito mas",
            "más sobre esto", "mas sobre esto",
            "más información", "mas informacion",
            "qué sigue", "que sigue",
            "qué viene", "que viene",
            "qué más", "que más",
            "hay más", "hay mas"
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

        val alreadyExplored = bibiCtx.chatHistory.any {
            it.resolvedTerm?.equals(term, ignoreCase = true) == true
                && it.response.contains("profundizar", ignoreCase = true)
        }
        if (!alreadyExplored) {
            val topics = TopicEngine.searchTopics(context, term, maxTotal = 3)
            val best = topics.firstOrNull()
            if (best != null && !best.description.isNullOrBlank()) {
                val metadata = mutableListOf<MetadataFact>()
                val details = mutableListOf<String>()
                val verses = mutableListOf<BibiVerse>()

                val dictEntry = DictionaryRepository.getEntryByTerm(context, term)
                    ?: DictionaryRepository.searchEntries(context, term).firstOrNull()?.let {
                        DictionaryRepository.getEntryById(context, it.id)
                    }
                if (dictEntry != null) {
                    if (!dictEntry.gender.isNullOrEmpty()) {
                        metadata.add(MetadataFact("", "Género",
                            if (dictEntry.gender == "Male") "Masculino" else if (dictEntry.gender == "Female") "Femenino" else dictEntry.gender))
                    }
                    if (!dictEntry.birthYear.isNullOrEmpty() && dictEntry.birthYear != "0") {
                        metadata.add(MetadataFact("", "Nacimiento", DictionaryEngine.formatYear(dictEntry.birthYear)))
                    }
                    if (!dictEntry.deathYear.isNullOrEmpty() && dictEntry.deathYear != "0") {
                        metadata.add(MetadataFact("", "Muerte", DictionaryEngine.formatYear(dictEntry.deathYear)))
                    }
                    if (!dictEntry.latitude.isNullOrEmpty()) {
                        metadata.add(MetadataFact("", "Ubicación", "${dictEntry.latitude}, ${dictEntry.longitude}"))
                    }
                    if (!dictEntry.aliases.isNullOrEmpty()) {
                        details.add("También conocido como: ${dictEntry.aliases}")
                    }
                    dictEntry.references.take(3).forEach { ref ->
                        verses.add(BibiVerse(ref = ref, text = ""))
                    }
                }

                if (best.verseCount > 0) {
                    details.add("${best.verseCount} versículos asociados")
                }

                val children = TopicEngine.getChildren(context, best.slug)
                val suggestions = mutableListOf(
                    BibiSuggestion("Profundizar", "cuéntame más sobre $term", isAi = true),
                    BibiSuggestion("Definición", "¿qué significa $term?")
                )
                if (children.isNotEmpty()) {
                    val childNames = children.take(3).joinToString(", ") { it.nameEs }
                    details.add("Subtemas: $childNames")
                    children.take(2).forEach { child ->
                        suggestions.add(
                            BibiSuggestion("Explorar ${child.nameEs}", "¿qué es ${child.nameEs.lowercase()}?")
                        )
                    }
                }

                return BibiResponse(
                    title = best.nameEs ?: best.nameEn ?: term,
                    definition = best.description,
                    details = details,
                    metadata = metadata,
                    verses = verses,
                    followUp = "¿Quieres saber más a profundidad sobre ${best.nameEs ?: term}?",
                    suggestions = suggestions,
                    confidence = Confidence.MEDIUM,
                    source = Source.LOCAL
                )
            }
        }

        val result = DictionaryEngine.defineTerm(context, term, bibiCtx, bibiCtx.chatHistory)
        if (result != null) return result

        val alternatives = AmbiguousTermResolver.findClosest(context, term)
        return if (alternatives.isNotEmpty()) {
            BibiResponse(
                title = "No encontré \"$term\"",
                definition = "En el diccionario bíblico no tengo información sobre \"$term\".",
                followUp = "Creo que quizás buscabas ${alternatives.joinToString(" o ")}.",
                suggestions = alternatives.map { BibiSuggestion("Consultar $it", "¿qué significa $it?") } +
                    BibiSuggestion("Preguntar de otra forma", "cuéntame sobre algo", isAi = true),
                confidence = Confidence.LOW
            )
        } else {
            BibiResponse(
                title = "No encontré \"$term\"",
                definition = "No tengo información sobre \"$term\" en el diccionario bíblico. " +
                    "Puedes intentar con otra palabra o preguntarme por un personaje o lugar.",
                followUp = "Si quieres, prueba con otra palabra.",
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

        val alreadyExplored = bibiCtx.chatHistory.any {
            it.resolvedTerm?.equals(name, ignoreCase = true) == true
                && it.response.contains("profundizar", ignoreCase = true)
        }
        if (!alreadyExplored) {
            val topics = TopicEngine.searchTopics(context, name, maxTotal = 3)
            val best = topics.firstOrNull()
            if (best != null && !best.description.isNullOrBlank()) {
                val metadata = mutableListOf<MetadataFact>()
                val details = mutableListOf<String>()
                val verses = mutableListOf<BibiVerse>()

                val dictEntry = DictionaryRepository.getEntryByTerm(context, name)
                    ?: DictionaryRepository.searchEntries(context, name).firstOrNull()?.let {
                        DictionaryRepository.getEntryById(context, it.id)
                    }
                if (dictEntry != null) {
                    if (!dictEntry.gender.isNullOrEmpty()) {
                        metadata.add(MetadataFact("", "Género",
                            if (dictEntry.gender == "Male") "Masculino" else if (dictEntry.gender == "Female") "Femenino" else dictEntry.gender))
                    }
                    if (!dictEntry.birthYear.isNullOrEmpty() && dictEntry.birthYear != "0") {
                        metadata.add(MetadataFact("", "Nacimiento", DictionaryEngine.formatYear(dictEntry.birthYear)))
                    }
                    if (!dictEntry.deathYear.isNullOrEmpty() && dictEntry.deathYear != "0") {
                        metadata.add(MetadataFact("", "Muerte", DictionaryEngine.formatYear(dictEntry.deathYear)))
                    }
                    if (!dictEntry.latitude.isNullOrEmpty()) {
                        metadata.add(MetadataFact("", "Ubicación", "${dictEntry.latitude}, ${dictEntry.longitude}"))
                    }
                    if (!dictEntry.aliases.isNullOrEmpty()) {
                        details.add("También conocido como: ${dictEntry.aliases}")
                    }
                    dictEntry.references.take(3).forEach { ref ->
                        verses.add(BibiVerse(ref = ref, text = ""))
                    }
                }

                if (best.verseCount > 0) {
                    details.add("${best.verseCount} versículos asociados")
                }

                return BibiResponse(
                    title = best.nameEs ?: best.nameEn ?: name,
                    definition = best.description,
                    details = details,
                    metadata = metadata,
                    verses = verses,
                    followUp = "¿Quieres saber más a profundidad sobre ${best.nameEs ?: name}?",
                    suggestions = listOf(
                        BibiSuggestion("Profundizar", "cuéntame más sobre $name", isAi = true),
                        BibiSuggestion("Definición", "¿quién fue $name?")
                    ),
                    confidence = Confidence.MEDIUM,
                    source = Source.LOCAL
                )
            }
        }

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

        val alreadyExplored = bibiCtx.chatHistory.any {
            it.resolvedTerm?.equals(name, ignoreCase = true) == true
                && it.response.contains("profundizar", ignoreCase = true)
        }
        if (!alreadyExplored) {
            val topics = TopicEngine.searchTopics(context, name, maxTotal = 3)
            val best = topics.firstOrNull()
            if (best != null && !best.description.isNullOrBlank()) {
                val metadata = mutableListOf<MetadataFact>()
                val details = mutableListOf<String>()
                val verses = mutableListOf<BibiVerse>()

                val dictEntry = DictionaryRepository.getEntryByTerm(context, name)
                    ?: DictionaryRepository.searchEntries(context, name).firstOrNull()?.let {
                        DictionaryRepository.getEntryById(context, it.id)
                    }
                if (dictEntry != null) {
                    if (!dictEntry.latitude.isNullOrEmpty()) {
                        metadata.add(MetadataFact("", "Ubicación", "${dictEntry.latitude}, ${dictEntry.longitude}"))
                    }
                    if (!dictEntry.featureType.isNullOrEmpty()) {
                        metadata.add(MetadataFact("", "Tipo", DictionaryEngine.translateFeatureType(dictEntry.featureType)))
                    }
                    if (!dictEntry.aliases.isNullOrEmpty()) {
                        details.add("También conocido como: ${dictEntry.aliases}")
                    }
                    dictEntry.references.take(3).forEach { ref ->
                        verses.add(BibiVerse(ref = ref, text = ""))
                    }
                }

                if (best.verseCount > 0) {
                    details.add("${best.verseCount} versículos asociados")
                }

                return BibiResponse(
                    title = best.nameEs ?: best.nameEn ?: name,
                    definition = best.description,
                    details = details,
                    metadata = metadata,
                    verses = verses,
                    followUp = "¿Quieres saber más a profundidad sobre ${best.nameEs ?: name}?",
                    suggestions = listOf(
                        BibiSuggestion("Profundizar", "cuéntame más sobre $name", isAi = true),
                        BibiSuggestion("Definición", "¿dónde queda $name?")
                    ),
                    confidence = Confidence.MEDIUM,
                    source = Source.LOCAL
                )
            }
        }

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

        val topics = TopicEngine.getTopicsForVerse(
            context, resolution.book, resolution.chapter, resolution.verse!!, maxTotal = 5
        )
        val details = mutableListOf<String>()
        if (topics.isNotEmpty()) {
            val topicNames = topics.joinToString(", ") { it.displayLabel }
            details.add("También es sobre: $topicNames")
        }

        val suggestions = mutableListOf(
            BibiSuggestion("Explicar este versículo", "explícame ${resolution.book} ${resolution.chapter}:${resolution.verse}"),
            BibiSuggestion("Saber más con IA", "profundiza en ${resolution.book} ${resolution.chapter}:${resolution.verse}", isAi = true)
        )
        if (topics.isNotEmpty()) {
            suggestions.add(
                BibiSuggestion("¿De qué temas habla este versículo?", "temas de ${resolution.book} ${resolution.chapter}:${resolution.verse}")
            )
        }

        sb.append("\n¿Quieres que profundice en alguno de estos pasajes?")

        return BibiResponse(
            title = "Pasajes relacionados con ${resolution.book} ${resolution.chapter}:${resolution.verse}",
            definition = sb.toString(),
            details = details,
            followUp = "¿Te interesa alguno de estos pasajes?",
            suggestions = suggestions
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

        val dictResponse = DictionaryEngine.explainVerse(
            context, userContext.verseText, userContext.verseRef, bibiCtx, bibiCtx.chatHistory
        )

        if (userContext.book.isNotBlank() && userContext.chapter > 0 && userContext.verse > 0) {
            val topics = TopicEngine.getTopicsForVerse(
                context, userContext.book, userContext.chapter, userContext.verse, maxTotal = 5
            )
            if (topics.isNotEmpty()) {
                val topicNames = topics.joinToString(", ") { it.displayLabel }
                return dictResponse.copy(
                    details = dictResponse.details + "Temas: $topicNames",
                    suggestions = dictResponse.suggestions + BibiSuggestion(
                        "Versículos sobre $topicNames",
                        "versículos sobre ${topics.first().displayLabel}",
                        isAi = true
                    )
                )
            }
        }

        return dictResponse
    }

    private suspend fun handleTopics(
        context: Context, question: String, userContext: UserContext
    ): BibiResponse? {
        val readerSnapshot = VerseResolver.ReaderSnapshot(
            bookName = userContext.book.takeIf { it.isNotBlank() },
            chapter = userContext.chapter,
            selectedVerses = userContext.selectedVerses
        )
        val resolution = VerseResolver.resolve(readerSnapshot, question)

        if (resolution.source == VerseResolver.Source.NONE) {
            return BibiResponse(
                title = "Para buscar temas",
                definition = "Necesito que estés leyendo un versículo. " +
                    "Abre un capítulo de la Biblia y pregúntame de nuevo.",
                followUp = "¿Qué temas te interesan?",
                suggestions = listOf(
                    BibiSuggestion("Buscar un tema", "¿qué significa fe?"),
                    BibiSuggestion("Ir al lector", "abrir lector")
                )
            )
        }

        if (!resolution.hasVerse) {
            return BibiResponse(
                title = "Necesito el versículo",
                definition = "¿Sobre qué versículo quieres saber los temas?",
                followUp = "Indícame el versículo para darte los temas relevantes.",
                suggestions = listOf(
                    BibiSuggestion("Versículos sobre este capítulo", "versículos sobre ${resolution.book} ${resolution.chapter}")
                )
            )
        }

        val topics = TopicEngine.getTopicsForVerse(
            context, resolution.book, resolution.chapter, resolution.verse!!, maxTotal = 8
        )

        if (topics.isEmpty()) {
            return BibiResponse(
                title = "Sin temas específicos",
                definition = "No encontré temas canónicos específicos para " +
                    "${resolution.book} ${resolution.chapter}:${resolution.verse}.",
                followUp = "¿Quieres explorar otro versículo o tema?",
                suggestions = listOf(
                    BibiSuggestion("Explicar este versículo", "explícame ${resolution.book} ${resolution.chapter}:${resolution.verse}"),
                    BibiSuggestion("Versículos relacionados", "versículos relacionados con ${resolution.book} ${resolution.chapter}:${resolution.verse}")
                )
            )
        }

        val sb = StringBuilder()
        sb.appendLine("Temas de ${resolution.book} ${resolution.chapter}:${resolution.verse}:\n")
        for (topic in topics) {
            sb.appendLine("  • ${topic.displayLabel}")
        }

        val suggestions = topics.take(3).map { topic ->
            BibiSuggestion(
                "Versículos sobre ${topic.displayLabel}",
                "versículos sobre ${topic.displayLabel}",
                isAi = true
            )
        } + BibiSuggestion(
            "Explicar este versículo",
            "explícame ${resolution.book} ${resolution.chapter}:${resolution.verse}"
        )

        return BibiResponse(
            title = "Temas de ${resolution.book} ${resolution.chapter}:${resolution.verse}",
            definition = sb.toString(),
            followUp = "¿Quieres explorar alguno de estos temas?",
            suggestions = suggestions
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

    private suspend fun handleDiveDeeper(
        context: Context, question: String, bibiCtx: BibiUserContext
    ): BibiResponse? {
        val lower = removeAccents(question.lowercase().trim())

        val term = run {
            val deepPatterns = listOf(
                "cuentame mas sobre", "cuéntame más sobre",
                "hablame mas de", "háblame más de",
                "profundiza sobre", "amplia sobre",
                "explica mas sobre", "explica más sobre",
                "dime mas sobre", "dime más sobre",
                "que pasajes hablan de", "qué pasajes hablan de",
                "que versiculos hablan de", "qué versículos hablan de",
                "pasajes sobre", "versiculos sobre",
                "hablan de", "habla de"
            )
            for (pattern in deepPatterns) {
                if (lower.contains(pattern)) {
                    val after = lower.substringAfter(pattern).trim()
                        .removeSuffix("?").trim()
                    if (after.length >= 2) return@run after
                }
            }

            if (lower.length < 25 && !hasOwnSubject(lower)) {
                val lastTerm = bibiCtx.chatHistory.lastOrNull { it.resolvedTerm != null }?.resolvedTerm
                if (lastTerm != null) return@run lastTerm
            }

            null
        }

        if (term != null) {
            val dictResult = DictionaryEngine.defineTerm(context, term, bibiCtx, bibiCtx.chatHistory)
            if (dictResult != null) return dictResult

            val topics = TopicEngine.searchTopics(context, term, maxTotal = 3)
            val best = topics.firstOrNull()
            if (best != null && !best.description.isNullOrBlank()) {
                return BibiResponse(
                    title = best.nameEs ?: best.nameEn ?: term,
                    definition = best.description,
                    followUp = "¿Hay algo más que quieras saber sobre ${best.nameEs ?: term}?",
                    suggestions = listOf(
                        BibiSuggestion("Versículos", "¿qué versículos hablan de $term?"),
                        BibiSuggestion("Definición", "¿qué significa $term?")
                    ),
                    confidence = Confidence.MEDIUM,
                    source = Source.LOCAL
                )
            }
        }

        return null
    }

    private fun noTermFound(): BibiResponse {
        return BibiResponse(
            title = "No entendí tu pregunta",
            definition = "No pude identificar qué término quieres buscar. " +
                "Intenta con frases como \"¿qué significa pacto?\" o \"¿quién fue Moisés?\".",
            followUp = "¿Cómo quieres que te ayude?",
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
            "que significa", "qué significa",
            "cual es la definicion", "cuál es la definición",
            "cual es el significado", "cuál es el significado",
            "que quiere decir", "qué quiere decir",
            "definicion de", "definición de", "significado de",
            "que es un", "qué es un", "que es una", "qué es una",
            "que es", "qué es", "que fue", "qué fue",
            "define", "definir",
            "que representa", "qué representa",
            "que dice el original de", "qué dice el original de",
            "en hebreo de", "en griego de",
            "explícame", "explicame", "explica",
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

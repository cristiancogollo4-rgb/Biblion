package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.bibi.BibiResponse
import com.cristiancogollo.biblion.feature.bibi.ChatExchange
import com.cristiancogollo.biblion.feature.bibi.Confidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.net.HttpURLConnection
import java.net.URL

data class StudyAssistantRequest(
    val question: String,
    val studyTitle: String,
    val studyTags: List<String>,
    val selectedText: String,
    val mode: StudyAssistantMode = StudyAssistantMode.STUDY,
    val intent: StudyAssistantIntent = StudyAssistantIntent.QUESTION,
    val currentOutline: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val bibleVersions: List<StudyAssistantBibleVersion> = emptyList(),
    val bibleVersion: String = "rv1960",
    val userName: String? = null,
    val lastQueries: List<String> = emptyList(),
    val chatHistory: List<ChatExchange> = emptyList(),
    /** Libro activo en el lector (modo READER). Null en modo STUDY. */
    val bookName: String? = null,
    /** Capitulo activo en el lector. 0 en modo STUDY. */
    val chapter: Int = 0,
    /** Versiculos seleccionados por el usuario (Forma 1 del VerseResolver). */
    val selectedVerses: Set<Int> = emptySet()
)

data class StudyAssistantBibleVersion(
    val key: String,
    val label: String
)

enum class StudyAssistantMode(val apiValue: String) {
    STUDY("study"),
    READER("reader")
}

enum class StudyAssistantIntent(val apiValue: String) {
    EXPLAIN("explain"),
    DEFINE("define"),
    CROSS_REFERENCE("cross_reference"),
    APPLICATION("application"),
    OUTLINE("outline"),
    SERMON("sermon"),
    DEVOTIONAL("devotional"),
    COMPARE_VERSIONS("compare_versions"),
    QUESTION("question")
}

data class StudyAssistantResponse(
    val answer: String,
    val references: List<StudyAssistantReference> = emptyList(),
    val suggestedBlocks: List<String> = emptyList(),
    val confidence: String = "medium",
    val usedFallback: Boolean = false,
    val bibiResponse: BibiResponse? = null
)

data class StudyAssistantReference(
    val reference: String,
    val reason: String
)

interface StudyAssistantRepository {
    suspend fun ask(request: StudyAssistantRequest): StudyAssistantResponse
}

class HttpStudyAssistantRepository(
    private val endpointUrl: String = BuildConfig.BIBI_ENDPOINT_URL,
    private val appContext: android.content.Context? = null,
    private val fallback: StudyAssistantRepository = LocalStudyAssistantRepository(appContext)
) : StudyAssistantRepository {
    override suspend fun ask(request: StudyAssistantRequest): StudyAssistantResponse {
        if (!request.isBibleDomain()) {
            return StudyAssistantResponse(
                answer = BIBI_OUT_OF_DOMAIN_MESSAGE,
                confidence = "high"
            )
        }

        // 1. LOCAL primero — KnowledgeEngine offline (diccionario, personas, lugares, etc.)
        val localResponse = fallback.ask(request)
        val localBibi = localResponse.bibiResponse

        // Si local respondió con confianza alta o media, devolver sin tocar Worker
        if (localBibi != null && localBibi.confidence != Confidence.LOW) {
            return localResponse
        }

        // 2. LOCAL no respondió o confianza baja → intentar Worker online
        val endpoint = endpointUrl.trim()
        if (endpoint.isBlank()) {
            return localResponse
        }

        return runCatching {
            withContext(Dispatchers.IO) {
                val payload = JSONObject()
                    .put("question", request.question)
                    .put(
                        "study",
                        JSONObject()
                            .put("title", request.studyTitle)
                            .put("tags", JSONArray(request.studyTags))
                            .put("selectedText", request.selectedText)
                            .put("currentOutline", JSONArray(request.currentOutline))
                            .put("notes", JSONArray(request.notes))
                    )
                    .put(
                        "bible",
                        JSONObject()
                            .put("version", request.bibleVersion)
                            .put(
                                "availableVersions",
                                JSONArray(request.bibleVersions.map { version ->
                                    JSONObject()
                                        .put("key", version.key)
                                        .put("label", version.label)
                                })
                            )
                    )
                    .put("mode", request.mode.apiValue)
                    .put("intent", request.intent.apiValue)
                    .put("userName", request.userName ?: JSONObject.NULL)

                val userHistory = request.lastQueries.take(3)
                if (userHistory.isNotEmpty()) {
                    payload.put("lastQueries", JSONArray(userHistory))
                }

                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 35_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

                connection.outputStream.use { output ->
                    output.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val body = if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }

                if (connection.responseCode !in 200..299) {
                    error("Bibi endpoint respondio ${connection.responseCode}: $body")
                }

                parseAssistantResponse(JSONObject(body))
            }
        }.getOrElse {
            // Worker falló → devolver lo que local haya producido (aunque sea LOW o null)
            localResponse.copy(usedFallback = true)
        }
    }

    private fun parseAssistantResponse(data: JSONObject): StudyAssistantResponse {
        val answer = cleanAssistantAnswer(data.optString("answer"))
        val referencesJson = data.optJSONArray("references") ?: JSONArray()
        val references = buildList {
            for (index in 0 until referencesJson.length()) {
                val item = referencesJson.optJSONObject(index) ?: continue
                val reference = item.optString("reference").trim()
                val reason = item.optString("reason").trim()
                if (reference.isNotBlank()) {
                    add(StudyAssistantReference(reference, reason))
                }
            }
        }
        val suggestedBlocksJson = data.optJSONArray("suggestedBlocks") ?: JSONArray()
        val suggestedBlocks = buildList {
            for (index in 0 until suggestedBlocksJson.length()) {
                val block = suggestedBlocksJson.optString(index).trim()
                if (block.isNotBlank()) add(block)
            }
        }

        return StudyAssistantResponse(
            answer = answer.ifBlank { "Bibi no recibio una respuesta util del servidor." },
            references = references,
            suggestedBlocks = suggestedBlocks,
            confidence = data.optString("confidence", "medium").ifBlank { "medium" }
        )
    }
}

class LocalStudyAssistantRepository(
    private val appContext: android.content.Context? = null
) : StudyAssistantRepository {
    override suspend fun ask(request: StudyAssistantRequest): StudyAssistantResponse {
        if (appContext != null) {
            val knowledgeResponse = com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.answer(
                context = appContext,
                question = request.question,
                userContext = com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.UserContext(
                    book = request.bookName.orEmpty(),
                    chapter = request.chapter,
                    selectedVerses = request.selectedVerses,
                    verseText = request.selectedText,
                    verseRef = request.studyTitle,
                    userName = request.userName,
                    lastQueries = request.lastQueries,
                    chatHistory = request.chatHistory
                )
            )

            if (knowledgeResponse != null) {
                return StudyAssistantResponse(
                    answer = knowledgeResponse.buildChatText(),
                    bibiResponse = knowledgeResponse,
                    usedFallback = true
                )
            }
        }

        return StudyAssistantResponse(
            answer = buildStudyAssistantLocalAnswer(request),
            usedFallback = true
        )
    }

    private fun buildStudyAssistantLocalAnswer(request: StudyAssistantRequest): String {
        val assistantIntro = when (request.mode) {
            StudyAssistantMode.STUDY -> "Soy Bibi, tu asistente de estudio biblico integrado en Biblion."
            StudyAssistantMode.READER -> "Soy Bibi, tu asistente biblico integrado en el lector de Biblion."
        }
        return "$assistantIntro No tengo una respuesta especifica para esa pregunta en mi " +
            "base de datos offline. Puedes preguntarme por definiciones biblicas, " +
            "personas, lugares, pasajes relacionados o palabras en hebreo y griego."
    }
}

private fun String.removeAccents(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
}

private fun cleanAssistantAnswer(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return ""
    val nested = runCatching { JSONObject(trimmed) }.getOrNull()
    if (nested != null) {
        val answer = nested.optString("answer").trim()
        if (answer.isNotBlank()) return cleanAssistantAnswer(answer)
    }
    val start = trimmed.indexOf("{")
    if (start >= 0) {
        val parsed = extractFirstJsonObject(trimmed, start)
        if (parsed != null) {
            val answer = parsed.optString("answer").trim()
            if (answer.isNotBlank()) return cleanAssistantAnswer(answer)
        }
    }
    extractAnswerField(trimmed)?.let { answer ->
        if (answer.isNotBlank()) return cleanAssistantAnswer(answer)
    }
    return trimmed
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

private fun extractFirstJsonObject(text: String, start: Int): JSONObject? {
    var depth = 0
    var inString = false
    var escaped = false
    for (index in start until text.length) {
        val char = text[index]
        if (escaped) {
            escaped = false
            continue
        }
        if (char == '\\') {
            escaped = true
            continue
        }
        if (char == '"') {
            inString = !inString
            continue
        }
        if (inString) continue
        if (char == '{') depth++
        if (char == '}') {
            depth--
            if (depth == 0) {
                return runCatching { JSONObject(text.substring(start, index + 1)) }.getOrNull()
            }
        }
    }
    return null
}

private fun extractAnswerField(text: String): String? {
    val match = Regex(""""answer"\s*:\s*"((?:\\.|[^"\\])*)"""").find(text) ?: return null
    val raw = match.groupValues.getOrNull(1) ?: return null
    return runCatching { JSONObject("""{"answer":"$raw"}""").optString("answer") }
        .getOrElse {
            raw.replace("\\\"", "\"").replace("\\n", "\n")
        }
}

private const val BIBI_OUT_OF_DOMAIN_MESSAGE =
    "Estoy diseñada para ayudarte únicamente con temas bíblicos y de estudio de las Escrituras dentro de Biblion. ¿Te gustaría explorar algún pasaje, personaje, tema o enseñanza bíblica?"

private fun StudyAssistantRequest.isBibleDomain(): Boolean {
    val normalized = listOf(
        question,
        studyTitle,
        selectedText,
        studyTags.joinToString(" "),
        currentOutline.joinToString(" "),
        notes.joinToString(" ")
    ).joinToString(" ").lowercase().removeAccents()

    val intent = com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.detectIntent(question)
    if (intent in setOf(
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.WHO,
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.WHERE,
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.DEFINE,
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.EXPLAIN_VERSE,
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.RELATED,
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.ORIGINAL_LANG,
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.GREETING
    )) return true

    val bibleSignals = listOf(
        "biblia", "biblico", "biblica", "biblion", "dios", "jesus", "cristo", "espiritu santo",
        "evangelio", "iglesia", "discipulado", "devocional", "predicacion", "sermon", "ensenanza",
        "versiculo", "pasaje", "capitulo", "libro", "testamento", "doctrina", "oracion", "fe",
        "gracia", "pecado", "salvacion", "creacion", "pacto", "profeta", "apostol", "discipulo",
        "genesis", "exodo", "levitico", "numeros", "deuteronomio", "josue", "jueces", "rut",
        "samuel", "reyes", "cronicas", "esdras", "nehemias", "ester", "job", "salmos",
        "proverbios", "eclesiastes", "cantares", "isaias", "jeremias", "lamentaciones",
        "ezequiel", "daniel", "oseas", "joel", "amos", "abdias", "jonas", "miqueas",
        "nahum", "habacuc", "sofonias", "hageo", "zacarias", "malaquias", "mateo", "marcos",
        "lucas", "juan", "hechos", "romanos", "corintios", "galatas", "efesios", "filipenses",
        "colosenses", "tesalonicenses", "timoteo", "tito", "filemon", "hebreos", "santiago",
        "pedro", "judas", "apocalipsis"
    )
    if (bibleSignals.any { it in normalized }) return true

    val biblicalNames = listOf(
        "adan", "eva", "cain", "abel", "noe", "abraham", "sara", "isaac", "israel", "jacob",
        "esau", "moises", "aaron", "josue", "david", "saul", "salomon", "elias", "eliseo",
        "isaias", "jeremias", "ezequiel", "daniel", "oseas", "joel", "amos", "jonas", "miqueas",
        "rut", "ester", "job", "noemi", "samuel", "gig", "goliath", "david", "uriel", "rafael",
        "miguel", "gabriel", "satan", "demonio", "angel", "querubin", "serafin",
        "jose", "maria", "jose", "jesus", "juan el bautista", "herodes", "pilato", "caifas",
        "marcos", "lucas", "juan", "mateo", "pedro", "pablo", "bernabe", "apolo", "timoteo",
        "tito", "silas", "apolos", "filemon", "filipenses", "colosenses",
        "lazaro", "marta", "magdalena", "maria magdalena", "nicodemo", "samaritano", "zaqueo",
        "bartimeo", "juana", "susan", "salome",
        "simeon", "ana", "isabel", "zacarias", "elcana", "penina", "jefthe",
        "baraque", "gideon", "sansón", "dalila", "sanson", "delila", "manoa",
        "absalon", "adoni-sedec", "acab", "jezabel", "atanalia", "joas", "manases",
        "neemias", "esdras", "zerubabel", "hageo", "zacarías",
        "abigail", "mical", "bathseba", "betseba", "tamar", "raquel", "lia", "leah", "dina",
        "merian", "roham", "asnat", "zilpa", "bila",
        "sanson", "tomas", "tadeo", "jaco", "apostoles", "apostol", "judio", "judios",
        "gentil", "gentiles", "fariseo", "fariseos", "saduceo", "saduceos", "escriba",
        "levita", "sacerdote", "sumo sacerdote", "sanhedrin",
        "betsaida", "capernaum", "jerusalen", "jericó", "jerico", "belen", "betlehem",
        "nazaret", "galilea", "galilea", "jordan", "jordán", "sinaí", "sinai", "horeb",
        "egipto", "israel", "palestina", "judea", "judea", "samaria", "samaritano",
        "tiro", "sidón", "sidon", "damasco", "antioquia", "roma", "babilonia", "babel",
        "persia", "asiria", "caldea", "moab", "edom", "filistea", "tigris", "eufrates"
    )
    if (biblicalNames.any { it in normalized }) return true

    val nonBibleSignals = listOf(
        "matematica", "matematicas", "calcula", "cuanto es", "programacion", "codigo", "kotlin",
        "java", "python", "javascript", "politica", "elecciones", "noticias", "deportes", "futbol",
        "medicina", "legal", "derecho", "finanzas", "inversion", "clima", "tecnologia",
        "receta", "cocina", "futbol", "deporte", "musica", "pelicula", "videojuego"
    )
    if (nonBibleSignals.any { it in normalized }) return false

    return normalized.length <= 120 && mode == StudyAssistantMode.STUDY &&
        (studyTitle.isNotBlank() || selectedText.isNotBlank() || currentOutline.isNotEmpty())
}

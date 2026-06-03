package com.cristiancogollo.biblion

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
    val bibleVersion: String = "rv1960"
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
    val usedFallback: Boolean = false
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
    private val fallback: StudyAssistantRepository = LocalStudyAssistantRepository()
) : StudyAssistantRepository {
    override suspend fun ask(request: StudyAssistantRequest): StudyAssistantResponse {
        if (!request.isBibleDomain()) {
            return StudyAssistantResponse(
                answer = BIBI_OUT_OF_DOMAIN_MESSAGE,
                confidence = "high"
            )
        }

        val endpoint = endpointUrl.trim()
        if (endpoint.isBlank()) {
            return fallback.ask(request)
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
            fallback.ask(request).copy(usedFallback = true)
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

class LocalStudyAssistantRepository : StudyAssistantRepository {
    override suspend fun ask(request: StudyAssistantRequest): StudyAssistantResponse {
        return StudyAssistantResponse(
            answer = buildStudyAssistantLocalAnswer(request),
            usedFallback = true
        )
    }

    private fun buildStudyAssistantLocalAnswer(request: StudyAssistantRequest): String {
        val normalized = request.question.lowercase()
        val context = buildList {
            request.studyTitle.takeIf { it.isNotBlank() }?.let { add("titulo: $it") }
            request.studyTags.takeIf { it.isNotEmpty() }?.let { add("etiquetas: ${it.joinToString(", ")}") }
            request.selectedText.takeIf { it.isNotBlank() }?.let { add("seleccion: $it") }
        }.joinToString("; ")

        val assistantIntro = when (request.mode) {
            StudyAssistantMode.STUDY -> "Soy Bibi, tu asistente de estudio biblico integrado en Biblion."
            StudyAssistantMode.READER -> "Soy Bibi, tu asistente biblico integrado en el lector de Biblion."
        }
        val baseContext = if (context.isBlank()) {
            "Aun no tengo contexto guardado de la ensenanza."
        } else {
            "Estoy tomando como contexto $context."
        }

        return when {
            "creacion" in normalized || "creacion" in normalized.removeAccents() -> {
                "$assistantIntro $baseContext Para hablar de la creacion, revisa Genesis 1:1-31, Genesis 2:1-3, Juan 1:1-3 y Hebreos 11:3. Puedes usar Genesis como texto base y Juan 1 para conectar la creacion con Cristo como Verbo eterno."
            }
            "amor" in normalized -> {
                "$assistantIntro $baseContext El amor es central en la Biblia. Sugiero 1 Corintios 13, 1 Juan 4:7-21 y Juan 3:16."
            }
            "fe" in normalized -> {
                "$baseContext Para estudiar la fe, Hebreos 11 es indispensable. Tambien considera Santiago 2:14-26 y Romanos 10:17."
            }
            "gracia" in normalized -> {
                "$baseContext La gracia de Dios se explica muy bien en Efesios 2:8-9, Romanos 3:24 y Tito 2:11."
            }
            "perdon" in normalized -> {
                "$baseContext El perdon es vital. Mira Mateo 18:21-35, Colosenses 3:13 y Efesios 4:32."
            }
            "ideas" in normalized || "ayuda" in normalized || "sugerencia" in normalized -> {
                "$baseContext Podrias estructurar tu ensenanza con: 1) introduccion, 2) tres puntos del texto base, y 3) una aplicacion practica."
            }
            "reflexion" in normalized || "ensenanza" in normalized.removeAccents() -> {
                "Basado en $context, una reflexion podria ser: la Palabra de Dios no solo informa, sino que transforma cuando su verdad penetra el corazon."
            }
            else -> {
                "$assistantIntro $baseContext No tengo una respuesta especifica para esa pregunta, pero puedo ayudarte si me das mas detalles."
            }
        }
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

    val nonBibleSignals = listOf(
        "matematica", "matematicas", "calcula", "cuanto es", "programacion", "codigo", "kotlin",
        "java", "python", "javascript", "politica", "elecciones", "noticias", "deportes", "futbol",
        "medicina", "legal", "derecho", "finanzas", "inversion", "clima", "tecnologia"
    )
    if (nonBibleSignals.any { it in normalized }) return false

    return normalized.length <= 120 && mode == StudyAssistantMode.STUDY &&
        (studyTitle.isNotBlank() || selectedText.isNotBlank() || currentOutline.isNotEmpty())
}

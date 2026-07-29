package com.cristiancogollo.biblion.feature.bibi.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object BibiMessageText {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val openingFence = Regex(
        pattern = """^```(?:json|markdown|text)?\s*""",
        option = RegexOption.IGNORE_CASE,
    )
    private val closingFence = Regex("""\s*```$""")
    private val excessiveBlankLines = Regex("""\n[ \t]*\n(?:[ \t]*\n)+""")

    fun normalize(raw: String): String {
        var normalized = raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()

        repeat(3) {
            normalized = normalized
                .replace(openingFence, "")
                .replace(closingFence, "")
                .trim()

            val nestedAnswer = extractNestedAnswer(normalized)
            if (nestedAnswer.isNullOrBlank() || nestedAnswer == normalized) {
                return@repeat
            }
            normalized = nestedAnswer.trim()
        }

        normalized = normalized
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\r", "\n")
            .replace("\\t", " ")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(excessiveBlankLines, "\n\n")

        return normalized.trim()
    }

    private fun extractNestedAnswer(value: String): String? = runCatching {
        val parsed = json.parseToJsonElement(value) as? JsonObject
        parsed?.get("answer")?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}

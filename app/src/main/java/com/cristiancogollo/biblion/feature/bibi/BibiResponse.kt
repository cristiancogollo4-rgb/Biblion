package com.cristiancogollo.biblion.feature.bibi

data class BibiResponse(
    val greeting: String? = null,
    val title: String,
    val subtitle: String? = null,
    val definition: String,
    val details: List<String> = emptyList(),
    val metadata: List<MetadataFact> = emptyList(),
    val verses: List<BibiVerse> = emptyList(),
    val followUp: String,
    val suggestions: List<BibiSuggestion> = emptyList(),
    val confidence: Confidence = Confidence.HIGH,
    val source: Source = Source.LOCAL
) {
    fun buildChatText(): String {
        val sb = StringBuilder()

        if (greeting != null) {
            sb.appendLine(greeting)
            sb.appendLine()
        }

        sb.appendLine(title)
        if (subtitle != null) {
            sb.appendLine(subtitle)
        }
        sb.appendLine()

        sb.appendLine(definition)

        for (detail in details) {
            sb.appendLine()
            sb.append(detail)
        }

        if (metadata.isNotEmpty()) {
            sb.appendLine()
            for (fact in metadata) {
                val prefix = if (fact.icon.isNotEmpty()) "${fact.icon} " else ""
                val label = if (fact.label.isNotEmpty()) "${fact.label}: " else ""
                sb.appendLine("$prefix$label${fact.value}")
            }
        }

        if (verses.isNotEmpty()) {
            sb.appendLine()
            for (verse in verses) {
                if (verse.text.isNotEmpty()) {
                    sb.appendLine("${verse.ref}: \"${verse.text}\"")
                } else {
                    sb.appendLine(verse.ref)
                }
            }
        }

        sb.appendLine()
        sb.append(followUp)

        return sb.toString()
    }
}

data class MetadataFact(
    val icon: String,
    val label: String,
    val value: String
)

data class BibiVerse(
    val ref: String,
    val text: String
)

data class BibiSuggestion(
    val label: String,
    val query: String,
    val isAi: Boolean = false
)

enum class Confidence { HIGH, MEDIUM, LOW }

enum class Source { LOCAL, AI }

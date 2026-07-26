package com.cristiancogollo.biblion.feature.studydocs.model

data class StudyBlockCapabilities(
    val supportsInlineFormatting: Boolean,
    val supportsBlockTypeChange: Boolean,
    val supportsFontSize: Boolean,
    val supportsAlignment: Boolean,
    val isAtomic: Boolean,
)

fun StudyBlock.capabilities(): StudyBlockCapabilities = when (this) {
    is StudyBlock.Verse -> StudyBlockCapabilities(
        supportsInlineFormatting = false,
        supportsBlockTypeChange = false,
        supportsFontSize = true,
        supportsAlignment = true,
        isAtomic = true,
    )
    else -> StudyBlockCapabilities(
        supportsInlineFormatting = true,
        supportsBlockTypeChange = true,
        supportsFontSize = true,
        supportsAlignment = true,
        isAtomic = false,
    )
}

/**
 * Central business rules for Bible citations. Keeping this pure allows the
 * editor, reader, persistence and operation engine to share the same contract.
 */
object VerseBusinessRules {

    fun normalize(block: StudyBlock.Verse): StudyBlock.Verse {
        val sourceVersion = block.sourceVersion.trim()
        val explicitVerseNumbers = block.verseNumbers
            .filter { it > 0 }
            .distinct()
            .sorted()
        val normalizedStart = explicitVerseNumbers.firstOrNull()
            ?: block.verseStart.coerceAtLeast(1)
        val normalizedEnd = explicitVerseNumbers.lastOrNull()
            ?: block.verseEnd.coerceAtLeast(normalizedStart)
        val normalizedContents = buildMap {
            block.contents.forEach { (version, text) ->
                val normalizedVersion = version.trim()
                if (normalizedVersion.isNotEmpty()) put(normalizedVersion, text)
            }
        }
        val comparisonVersion = block.comparedVersions
            .asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.equals(sourceVersion, ignoreCase = true) }
            .distinctBy(String::lowercase)
            .firstOrNull { normalizedContents[it].isNullOrBlank().not() }

        return block.copy(
            bookId = block.bookId.trim(),
            chapter = block.chapter.coerceAtLeast(1),
            verseStart = normalizedStart,
            verseEnd = normalizedEnd,
            verseNumbers = explicitVerseNumbers,
            sourceVersion = sourceVersion,
            contents = normalizedContents,
            showCompare = comparisonVersion != null,
            comparedVersions = comparisonVersion?.let(::listOf).orEmpty(),
            fontFamily = "serif",
            fontSize = block.fontSize.coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE),
        )
    }

    fun invalidReason(block: StudyBlock.Verse): String? {
        val normalized = normalize(block)
        return when {
            normalized.bookId.isBlank() -> "La cita debe indicar un libro"
            normalized.sourceVersion.isBlank() -> "La cita debe indicar su version fuente"
            normalized.contents[normalized.sourceVersion].isNullOrBlank() ->
                "La cita debe conservar el texto de su version fuente"
            else -> null
        }
    }

    fun withComparison(
        block: StudyBlock.Verse,
        version: String?,
        content: String?,
    ): StudyBlock.Verse {
        val normalized = normalize(block)
        if (version == null) {
            return normalized.copy(
                showCompare = false,
                comparedVersions = emptyList(),
            )
        }

        val normalizedVersion = version.trim()
        if (
            normalizedVersion.isEmpty() ||
            normalizedVersion.equals(normalized.sourceVersion, ignoreCase = true) ||
            content.isNullOrBlank()
        ) {
            return normalized
        }
        return normalize(
            normalized.copy(
                contents = normalized.contents + (normalizedVersion to content),
                showCompare = true,
                comparedVersions = listOf(normalizedVersion),
            )
        )
    }
}

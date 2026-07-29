package com.cristiancogollo.biblion.feature.bibi.ui

import com.cristiancogollo.biblion.feature.bibi.model.BibiMessageText

internal enum class BibiMessageBlockKind {
    PARAGRAPH,
    HEADING,
    SECTION,
    BULLET,
    NUMBERED,
    QUOTE,
}

internal data class BibiMessageBlock(
    val kind: BibiMessageBlockKind,
    val text: String,
    val marker: String = "",
)

internal object BibiMessageFormatter {
    private val headingPattern = Regex("""^\s*#{1,3}\s+(.+)$""")
    private val bulletPattern = Regex("""^\s*[-*•]\s+(.+)$""")
    private val numberedPattern = Regex("""^\s*(\d{1,2}[.)])\s+(.+)$""")
    private val quotePattern = Regex("""^\s*>\s?(.+)$""")
    private val sentenceBoundary = Regex("""(?<=[.!?])\s+(?=[\p{Lu}¿¡])""")

    fun parse(raw: String): List<BibiMessageBlock> {
        val normalized = BibiMessageText.normalize(raw)
        if (normalized.isBlank()) return emptyList()

        val blocks = mutableListOf<BibiMessageBlock>()
        val paragraph = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraph.isEmpty()) return
            splitLongParagraph(paragraph.joinToString(" ")).forEach { text ->
                blocks += BibiMessageBlock(
                    kind = BibiMessageBlockKind.PARAGRAPH,
                    text = text,
                )
            }
            paragraph.clear()
        }

        normalized.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                flushParagraph()
                return@forEach
            }

            val heading = headingPattern.matchEntire(line)
            val bullet = bulletPattern.matchEntire(line)
            val numbered = numberedPattern.matchEntire(line)
            val quote = quotePattern.matchEntire(line)

            when {
                heading != null -> {
                    flushParagraph()
                    blocks += BibiMessageBlock(
                        kind = BibiMessageBlockKind.HEADING,
                        text = heading.groupValues[1].trim(),
                    )
                }
                bullet != null -> {
                    flushParagraph()
                    blocks += BibiMessageBlock(
                        kind = BibiMessageBlockKind.BULLET,
                        text = bullet.groupValues[1].trim(),
                        marker = "•",
                    )
                }
                numbered != null -> {
                    flushParagraph()
                    blocks += BibiMessageBlock(
                        kind = BibiMessageBlockKind.NUMBERED,
                        text = numbered.groupValues[2].trim(),
                        marker = numbered.groupValues[1],
                    )
                }
                quote != null -> {
                    flushParagraph()
                    blocks += BibiMessageBlock(
                        kind = BibiMessageBlockKind.QUOTE,
                        text = quote.groupValues[1].trim(),
                    )
                }
                isSectionLabel(line) -> {
                    flushParagraph()
                    blocks += BibiMessageBlock(
                        kind = BibiMessageBlockKind.SECTION,
                        text = line.removeSuffix(":").trim(),
                    )
                }
                else -> paragraph += line
            }
        }
        flushParagraph()
        return blocks
    }

    private fun isSectionLabel(line: String): Boolean =
        line.endsWith(":") &&
            line.length <= 64 &&
            line.count { it == ':' } == 1

    private fun splitLongParagraph(text: String): List<String> {
        if (text.length <= 320) return listOf(text)
        val sentences = text.split(sentenceBoundary)
        if (sentences.size == 1) return listOf(text)

        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        sentences.forEach { sentence ->
            val extraLength = if (current.isEmpty()) sentence.length else sentence.length + 1
            if (current.isNotEmpty() && current.length + extraLength > 300) {
                chunks += current.toString()
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        if (current.isNotEmpty()) chunks += current.toString()
        return chunks
    }
}

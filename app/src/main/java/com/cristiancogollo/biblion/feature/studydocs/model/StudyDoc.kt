package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.Serializable

@Serializable
data class StudyDoc(
    val id: DocId = DocId.generate(),
    val title: String = "",
    val blocks: List<StudyBlock> = listOf(StudyBlock.Paragraph()),
    val metadata: DocMetadata = DocMetadata.Empty,
    val version: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val remoteId: String? = null,
) {
    val isEmpty: Boolean get() = title.isBlank() && (blocks.isEmpty() ||
            blocks.all { it.plainText().isBlank() })

    fun plainText(): String = buildString {
        if (title.isNotEmpty()) appendLine(title)
        for (block in blocks) {
            when (block) {
                is StudyBlock.Paragraph -> if (block.text.raw.isNotBlank()) appendLine(block.text.raw)
                is StudyBlock.Heading -> appendLine(block.text.raw)
                is StudyBlock.BulletList -> block.items.forEach {
                    if (it.raw.isNotBlank()) appendLine("- ${it.raw}")
                }
                is StudyBlock.OrderedList -> block.items.forEachIndexed { i, item ->
                    if (item.raw.isNotBlank()) appendLine("${i + 1}. ${item.raw}")
                }
                is StudyBlock.Quote -> {
                    if (block.text.raw.isNotBlank()) appendLine(block.text.raw)
                    if (block.attribution != null) appendLine("— ${block.attribution}")
                }
            }
        }
    }

    fun wordCount(): Int {
        var count = 0
        val ws = "\\s+".toRegex()
        if (title.isNotBlank()) count += ws.split(title).count { it.isNotBlank() }
        for (block in blocks) {
            count += ws.split(block.plainText()).count { it.isNotBlank() }
        }
        return count
    }

    fun headings(): List<StudyBlock.Heading> = blocks.filterIsInstance<StudyBlock.Heading>()

    companion object {
        fun empty(): StudyDoc = StudyDoc(
            blocks = listOf(StudyBlock.Paragraph()),
        )
    }
}

package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object LegacyStudyDocMigrator {
    private val json = Json { ignoreUnknownKeys = true }
    private val referencePattern = Regex("""^(.+?)\s+(\d+):(\d+)(?:-(\d+))?$""")
    private val htmlTagPattern = Regex("<[^>]+>")

    fun migrate(
        payload: String,
        remoteId: String,
        title: String,
        createdAt: Long,
        updatedAt: Long,
    ): StudyDoc? = runCatching {
        val root = json.parseToJsonElement(payload).jsonObject
        val blocks = root["blocks"]
            ?.jsonArray
            .orEmpty()
            .mapNotNull { migrateBlock(it.jsonObject) }
            .ifEmpty { listOf(StudyBlock.Paragraph()) }
        val tags = root["tags"]
            ?.jsonArray
            .orEmpty()
            .mapNotNull { it.jsonPrimitive.contentOrNull }
        StudyDoc(
            id = DocId(remoteId),
            remoteId = remoteId,
            title = title,
            blocks = blocks,
            metadata = DocMetadata(tags = tags),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }.getOrNull()

    private fun migrateBlock(block: JsonObject): StudyBlock? {
        val type = block.string("nodeType") ?: block.string("type") ?: return null
        val id = blockId(block.string("blockId") ?: block.string("citationId"))
        return when (type) {
            "paragraph" -> StudyBlock.Paragraph(
                id = id,
                text = StyledText(block.string("text").orEmpty()),
            )
            "rich_text" -> StudyBlock.Paragraph(
                id = id,
                text = StyledText(stripHtml(block.string("html").orEmpty())),
            )
            "citation" -> migrateCitation(block, id)
            "quoted_verse" -> migrateQuotedVerse(block, id)
            "note" -> StudyBlock.Quote(
                id = id,
                text = StyledText(block.string("text").orEmpty()),
                attribution = "Nota",
            )
            "reflection" -> StudyBlock.Quote(
                id = id,
                text = StyledText(
                    listOfNotNull(
                        block.string("topic")?.takeIf { it.isNotBlank() },
                        block.string("text")?.takeIf { it.isNotBlank() },
                    ).joinToString("\n"),
                ),
                attribution = "Reflexion",
            )
            "question" -> StudyBlock.Paragraph(
                id = id,
                text = StyledText(
                    listOfNotNull(
                        block.string("question")?.takeIf { it.isNotBlank() },
                        block.string("answer")?.takeIf { it.isNotBlank() },
                    ).joinToString("\n"),
                ),
            )
            "two_column" -> StudyBlock.Paragraph(
                id = id,
                text = StyledText(
                    listOf(
                        block.string("leftTitle").orEmpty(),
                        block.string("leftText").orEmpty(),
                        block.string("rightTitle").orEmpty(),
                        block.string("rightText").orEmpty(),
                    ).filter { it.isNotBlank() }.joinToString("\n"),
                ),
            )
            "audio" -> StudyBlock.Paragraph(
                id = id,
                text = StyledText(block.string("title").orEmpty()),
            )
            "image" -> StudyBlock.Paragraph(
                id = id,
                text = StyledText(block.string("caption").orEmpty()),
            )
            else -> null
        }
    }

    private fun migrateCitation(block: JsonObject, id: BlockId): StudyBlock.Verse? {
        val reference = block["reference"]?.jsonObject ?: return null
        val book = reference.string("book").orEmpty()
        val chapter = reference.int("chapter") ?: return null
        val start = reference.int("verseStart") ?: return null
        val end = reference.int("verseEnd") ?: start
        val version = block.string("version").orEmpty().ifBlank { "rv1960" }
        return StudyBlock.Verse(
            id = id,
            bookId = book,
            chapter = chapter,
            verseStart = start,
            verseEnd = end,
            sourceVersion = version,
            contents = mapOf(version to block.string("text").orEmpty()),
        )
    }

    private fun migrateQuotedVerse(block: JsonObject, id: BlockId): StudyBlock {
        val reference = block.string("reference").orEmpty()
        val match = referencePattern.matchEntire(reference)
        if (match == null) {
            return StudyBlock.Quote(
                id = id,
                text = StyledText(
                    listOf(reference, block.string("primaryText").orEmpty())
                        .filter { it.isNotBlank() }
                        .joinToString("\n"),
                ),
            )
        }
        val primaryVersion = block.string("primaryVersion").orEmpty().ifBlank { "rv1960" }
        val compareVersion = block.string("compareVersion").orEmpty()
        val contents = buildMap {
            put(primaryVersion, block.string("primaryText").orEmpty())
            if (compareVersion.isNotBlank()) {
                put(compareVersion, block.string("compareText").orEmpty())
            }
        }
        return StudyBlock.Verse(
            id = id,
            bookId = match.groupValues[1],
            chapter = match.groupValues[2].toInt(),
            verseStart = match.groupValues[3].toInt(),
            verseEnd = match.groupValues[4].toIntOrNull() ?: match.groupValues[3].toInt(),
            sourceVersion = primaryVersion,
            contents = contents,
            showCompare = compareVersion.isNotBlank(),
            comparedVersions = listOfNotNull(compareVersion.takeIf { it.isNotBlank() }),
        )
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull

    private fun blockId(value: String?): BlockId =
        value?.takeIf { it.isNotBlank() }?.let(::BlockId) ?: BlockId.generate()

    private fun stripHtml(value: String): String = value
        .replace("<br>", "\n", ignoreCase = true)
        .replace("<br/>", "\n", ignoreCase = true)
        .replace("</p>", "\n", ignoreCase = true)
        .replace(htmlTagPattern, "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
}

package com.cristiancogollo.biblion

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(
    tableName = "study_notebooks",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class StudyNotebookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = CuidGenerator.create(),
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val ownerUid: String? = null,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val syncVersion: Long = 0
)

@Entity(
    tableName = "studies",
    indices = [Index("notebookId"), Index(value = ["remoteId"], unique = true)]
)
data class StudyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = CuidGenerator.create(),
    val title: String,
    val notebookId: Long,
    val notebookRemoteId: String = "",
    val contentSerialized: String,
    val createdAt: Long,
    val updatedAt: Long,
    val ownerUid: String? = null,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val syncVersion: Long = 0
)

@Entity(
    tableName = "linked_citations",
    foreignKeys = [
        ForeignKey(
            entity = StudyEntity::class,
            parentColumns = ["id"],
            childColumns = ["estudioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("estudioId")]
)
data class LinkedCitationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val estudioId: Long,
    val book: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val version: String,
    val positionMetadata: String
)

@Serializable
data class SerializedStudyDocument(
    val blocks: List<StudyBlockNode> = emptyList(),
    val globalVersion: String = "rv1960",
    val tags: List<String> = emptyList()
)

@Serializable
sealed interface StudyBlockNode {
    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        val blockId: String = CuidGenerator.create(),
        val text: String = "",
        val parallelText: String = "",
        val role: String = "paragraph",
        val styles: List<TextStyleRange> = emptyList(),
        val parallelStyles: List<TextStyleRange> = emptyList()
    ) : StudyBlockNode

    @Serializable
    @SerialName("rich_text")
    data class RichText(
        val blockId: String = CuidGenerator.create(),
        val html: String,
        val references: List<BibleReferenceNode> = emptyList()
    ) : StudyBlockNode

    @Serializable
    @SerialName("citation")
    data class Citation(
        val citationId: String,
        val reference: BibleReferenceNode,
        val text: String,
        val version: String,
        val includeFullText: Boolean,
        val canRefresh: Boolean = true
    ) : StudyBlockNode

    @Serializable
    @SerialName("audio")
    data class Audio(
        val uri: String,
        val title: String
    ) : StudyBlockNode

    @Serializable
    @SerialName("image")
    data class Image(
        val uri: String,
        val caption: String
    ) : StudyBlockNode

    @Serializable
    @SerialName("note")
    data class Note(
        val blockId: String = CuidGenerator.create(),
        val text: String = "",
        val collapsed: Boolean = false
    ) : StudyBlockNode

    @Serializable
    @SerialName("reflection")
    data class Reflection(
        val blockId: String = CuidGenerator.create(),
        val topic: String = "",
        val text: String = "",
        val collapsed: Boolean = false
    ) : StudyBlockNode

    @Serializable
    @SerialName("quoted_verse")
    data class QuotedVerse(
        val blockId: String = CuidGenerator.create(),
        val reference: String = "",
        val primaryVersion: String = "rv1960",
        val primaryText: String = "",
        val compareVersion: String = "",
        val compareText: String = "",
        val note: String = "",
        val collapsed: Boolean = false
    ) : StudyBlockNode

    @Serializable
    @SerialName("question")
    data class Question(
        val blockId: String = CuidGenerator.create(),
        val question: String = "",
        val answer: String = "",
        val collapsed: Boolean = false
    ) : StudyBlockNode

    @Serializable
    @SerialName("two_column")
    data class TwoColumn(
        val blockId: String = CuidGenerator.create(),
        val leftTitle: String = "",
        val leftText: String = "",
        val rightTitle: String = "",
        val rightText: String = "",
        val collapsed: Boolean = false
    ) : StudyBlockNode
}

@Serializable
data class TextStyleRange(
    val start: Int,
    val end: Int,
    val color: Long? = null,
    val background: Long? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val fontSizeSp: Float? = null
)

@Serializable
data class BibleReferenceNode(
    val book: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int
) {
    val display: String
        get() = "$book $chapter:$verseStart" + if (verseEnd > verseStart) "-$verseEnd" else ""
}

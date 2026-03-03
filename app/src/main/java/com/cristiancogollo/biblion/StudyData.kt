package com.cristiancogollo.biblion

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "study_notebooks")
data class StudyNotebookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "studies",
    indices = [Index("notebookId")]
)
data class StudyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notebookId: Long,
    val contentSerialized: String,
    val createdAt: Long,
    val updatedAt: Long
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
    val globalVersion: String = "rvr1960"
)

@Serializable
sealed interface StudyBlockNode {
    @Serializable
    @SerialName("rich_text")
    data class RichText(
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
}

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

package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.Serializable

@Serializable
enum class PublicationStatus {
    PUBLISHED,
    HIDDEN,
    REJECTED,
}

@Serializable
data class StudyAuthorSnapshot(
    val uid: String,
    val alias: String = "",
    val displayName: String = "",
)

@Serializable
data class RevisionDiff(
    val blocksAdded: Int = 0,
    val blocksModified: Int = 0,
    val blocksRemoved: Int = 0,
    val wordsAdded: Int = 0,
    val wordsRemoved: Int = 0,
    val similarityPercent: Int? = null,
)

@Serializable
data class PublicationSignature(
    val algorithm: String = "SHA-256+Ed25519",
    val contentHash: String,
    val signature: String? = null,
    val keyId: String? = null,
    val signedAt: Long,
)

@Serializable
data class TeachingRevision(
    val revisionId: String,
    val publicationId: String,
    val revisionNumber: Int,
    val parentRevisionId: String? = null,
    val rootRevisionId: String? = null,
    val author: StudyAuthorSnapshot,
    val content: StudyDoc,
    val diffFromParent: RevisionDiff = RevisionDiff(),
    val diffFromRoot: RevisionDiff = RevisionDiff(),
    val signature: PublicationSignature? = null,
    val publishedAt: Long,
)

@Serializable
data class PublicTeaching(
    val publicationId: String,
    val currentRevisionId: String,
    val title: String,
    val tags: List<String> = emptyList(),
    val author: StudyAuthorSnapshot,
    val rootAuthor: StudyAuthorSnapshot? = null,
    val parentPublicationId: String? = null,
    val parentRevisionId: String? = null,
    val currentRevisionDiff: RevisionDiff? = null,
    val status: PublicationStatus = PublicationStatus.PUBLISHED,
    val updatedAt: Long,
)

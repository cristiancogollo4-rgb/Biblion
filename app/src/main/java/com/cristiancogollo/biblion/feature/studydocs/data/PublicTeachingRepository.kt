package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.awaitResult
import com.cristiancogollo.biblion.feature.studydocs.model.PublicationStatus
import com.cristiancogollo.biblion.feature.studydocs.model.PublicTeaching
import com.cristiancogollo.biblion.feature.studydocs.model.RevisionDiff
import com.cristiancogollo.biblion.feature.studydocs.model.StudyAuthorSnapshot
import com.cristiancogollo.biblion.feature.studydocs.model.TeachingRevision
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface PublicTeachingRepository {
    fun observePublished(): Flow<List<PublicTeaching>>
    suspend fun getRevision(publicationId: String, revisionId: String): TeachingRevision?
}

class FirestorePublicTeachingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : PublicTeachingRepository {

    override fun observePublished(): Flow<List<PublicTeaching>> = callbackFlow {
        val listener = publications()
            .whereEqualTo("status", PublicationStatus.PUBLISHED.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snapshot?.documents.orEmpty()
                        .mapNotNull { it.data?.toPublicTeaching() }
                        .sortedByDescending { it.updatedAt },
                )
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getRevision(
        publicationId: String,
        revisionId: String,
    ): TeachingRevision? {
        val snapshot = revisions(publicationId).document(revisionId).get().awaitResult()
        return snapshot.data?.toTeachingRevision(snapshot.id)
    }

    private fun publications() = firestore.collection(PUBLICATIONS_COLLECTION)
    private fun revisions(publicationId: String) = publications()
        .document(publicationId)
        .collection(REVISIONS_COLLECTION)

    private fun Map<String, Any?>.toAuthor(): StudyAuthorSnapshot? {
        val uid = this["uid"] as? String ?: return null
        return StudyAuthorSnapshot(
            uid = uid,
            alias = this["alias"] as? String ?: "",
            displayName = this["displayName"] as? String ?: "",
        )
    }

    private fun Map<String, Any?>.toPublicTeaching(): PublicTeaching? {
        val publicationId = this["publicationId"] as? String ?: return null
        val author = (this["author"] as? Map<*, *>)
            ?.mapKeys { it.key.toString() }
            ?.toAuthor() ?: return null
        val rootAuthor = (this["rootAuthor"] as? Map<*, *>)
            ?.mapKeys { it.key.toString() }
            ?.toAuthor()
        return PublicTeaching(
            publicationId = publicationId,
            currentRevisionId = this["currentRevisionId"] as? String ?: return null,
            title = this["title"] as? String ?: "",
            tags = (this["tags"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
            author = author,
            rootAuthor = rootAuthor,
            parentPublicationId = this["parentPublicationId"] as? String,
            parentRevisionId = this["parentRevisionId"] as? String,
            currentRevisionDiff = (this["currentRevisionDiff"] as? Map<*, *>)?.toRevisionDiff(),
            status = runCatching {
                PublicationStatus.valueOf(this["status"] as? String ?: "PUBLISHED")
            }.getOrDefault(PublicationStatus.PUBLISHED),
            updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    private fun Map<String, Any?>.toTeachingRevision(documentId: String): TeachingRevision? {
        val contentJson = this["contentJson"] as? String ?: return null
        val content = StudyDocJson.decode(contentJson) ?: return null
        val author = (this["author"] as? Map<*, *>)
            ?.mapKeys { it.key.toString() }
            ?.toAuthor() ?: return null
        return TeachingRevision(
            revisionId = this["revisionId"] as? String ?: documentId,
            publicationId = this["publicationId"] as? String ?: return null,
            revisionNumber = (this["revisionNumber"] as? Number)?.toInt() ?: 1,
            parentRevisionId = this["parentRevisionId"] as? String,
            rootRevisionId = this["rootRevisionId"] as? String,
            author = author,
            content = content,
            diffFromParent = (this["diffFromParent"] as? Map<*, *>)?.toRevisionDiff() ?: RevisionDiff(),
            diffFromRoot = (this["diffFromRoot"] as? Map<*, *>)?.toRevisionDiff() ?: RevisionDiff(),
            signature = null,
            publishedAt = (this["publishedAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    private fun Map<*, *>.toRevisionDiff(): RevisionDiff = RevisionDiff(
        blocksAdded = number("blocksAdded"),
        blocksModified = number("blocksModified"),
        blocksRemoved = number("blocksRemoved"),
        wordsAdded = number("wordsAdded"),
        wordsRemoved = number("wordsRemoved"),
        similarityPercent = (this["similarityPercent"] as? Number)?.toInt(),
    )

    private fun Map<*, *>.number(key: String): Int = (this[key] as? Number)?.toInt() ?: 0

    private companion object {
        const val PUBLICATIONS_COLLECTION = "publications"
        const val REVISIONS_COLLECTION = "revisions"
    }
}

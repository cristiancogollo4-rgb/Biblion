package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.DocId

internal data class RemoteStudyRecord(
    val remoteId: String,
    val title: String,
    val notebookRemoteId: String?,
    val tagsCsv: String,
    val blockCount: Int,
    val docVersion: Int,
    val docJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val ownerUid: String?,
    val deletedAt: Long?,
    val syncVersion: Long,
    val isPublished: Boolean,
)

internal enum class StudySyncResolution {
    KEEP_LOCAL,
    APPLY_REMOTE,
    PRESERVE_LOCAL_CONFLICT_AND_APPLY_REMOTE,
    DELETE_LOCAL,
    IGNORE_FOREIGN_OWNER,
    IGNORE_INVALID_REMOTE,
}

internal object StudyDocSyncPolicy {

    fun resolve(
        local: StudyDocEntity?,
        remote: RemoteStudyRecord,
        activeOwnerUid: String,
    ): StudySyncResolution {
        if (!remote.isPublished) {
            return StudySyncResolution.IGNORE_INVALID_REMOTE
        }
        if (remote.deletedAt == null && remote.docJson.isBlank()) {
            return StudySyncResolution.IGNORE_INVALID_REMOTE
        }
        if (
            remote.ownerUid != null &&
            remote.ownerUid != activeOwnerUid
        ) {
            return StudySyncResolution.IGNORE_FOREIGN_OWNER
        }
        if (
            local?.ownerUid != null &&
            local.ownerUid != activeOwnerUid
        ) {
            return StudySyncResolution.IGNORE_FOREIGN_OWNER
        }

        val remoteClock = maxOf(remote.updatedAt, remote.deletedAt ?: 0L)
        if (local == null) {
            return if (remote.deletedAt != null) {
                StudySyncResolution.KEEP_LOCAL
            } else {
                StudySyncResolution.APPLY_REMOTE
            }
        }

        val localClock = maxOf(local.updatedAt, local.deletedAt ?: 0L)
        if (local.isDirty && localClock >= remoteClock) {
            return StudySyncResolution.KEEP_LOCAL
        }
        if (local.isDirty && remoteClock > localClock) {
            return StudySyncResolution.PRESERVE_LOCAL_CONFLICT_AND_APPLY_REMOTE
        }
        if (remote.deletedAt != null && remoteClock >= localClock) {
            return StudySyncResolution.DELETE_LOCAL
        }
        return if (remote.updatedAt > local.updatedAt) {
            StudySyncResolution.APPLY_REMOTE
        } else {
            StudySyncResolution.KEEP_LOCAL
        }
    }

    fun toLocalEntity(
        remote: RemoteStudyRecord,
        activeOwnerUid: String,
        syncedAt: Long,
    ): StudyDocEntity? {
        val decoded = StudyDocJson.decode(remote.docJson)
            ?: LegacyStudyDocMigrator.migrate(
                payload = remote.docJson,
                remoteId = remote.remoteId,
                title = remote.title,
                createdAt = remote.createdAt,
                updatedAt = remote.updatedAt,
            )
            ?: return null
        val normalizedDoc = decoded.copy(
            title = remote.title.ifBlank { decoded.title },
            createdAt = remote.createdAt.takeIf { it > 0L } ?: decoded.createdAt,
            updatedAt = remote.updatedAt.takeIf { it > 0L } ?: decoded.updatedAt,
            remoteId = remote.remoteId,
        )
        return StudyDocEntity(
            remoteId = remote.remoteId,
            title = normalizedDoc.title,
            notebookRemoteId = remote.notebookRemoteId ?: normalizedDoc.metadata.notebook,
            ownerUid = activeOwnerUid,
            tagsCsv = remote.tagsCsv.ifBlank {
                normalizedDoc.metadata.tags.joinToString(",")
            },
            blockCount = remote.blockCount.takeIf { it > 0 } ?: normalizedDoc.blocks.size,
            version = remote.docVersion.takeIf { it > 0 } ?: normalizedDoc.version,
            docJson = StudyDocJson.encode(normalizedDoc),
            createdAt = normalizedDoc.createdAt,
            updatedAt = normalizedDoc.updatedAt,
            deletedAt = null,
            lastSyncedAt = syncedAt,
            syncVersion = remote.syncVersion,
            isDirty = false,
            isPublished = remote.isPublished,
        )
    }

    fun nextSyncVersion(entity: StudyDocEntity): Long =
        maxOf(entity.syncVersion + 1L, entity.updatedAt)

    fun createConflictCopy(
        local: StudyDocEntity,
        activeOwnerUid: String,
        now: Long,
    ): StudyDocEntity? {
        val localDoc = StudyDocJson.decode(local.docJson) ?: return null
        val conflictId = DocId.generate()
        val conflictTitle = "${local.title.ifBlank { localDoc.title }} (conflicto local)"
        val conflictDoc = localDoc.copy(
            id = conflictId,
            remoteId = conflictId.value,
            title = conflictTitle,
            createdAt = now,
            updatedAt = now,
        )
        return local.copy(
            id = 0L,
            remoteId = conflictId.value,
            title = conflictTitle,
            ownerUid = activeOwnerUid,
            docJson = StudyDocJson.encode(conflictDoc),
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            lastSyncedAt = null,
            syncVersion = 0L,
            isDirty = true,
            isPublished = true,
        )
    }
}

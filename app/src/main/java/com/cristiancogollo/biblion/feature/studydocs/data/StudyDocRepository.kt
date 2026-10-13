package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.FirestoreSyncManager
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.hasPersistableTitle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class StudyDocSyncStatus { LOCAL_ONLY, PENDING, SYNCED }

data class StudyDocListEntry(
    val doc: StudyDoc,
    val syncStatus: StudyDocSyncStatus,
)

class StudyDocRepository(
    private val dao: StudyDocDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ownerUidProvider: () -> String? = { null },
    private val serializationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    fun observeAll(): Flow<List<StudyDoc>> = dao.observeAll().map(::decodeAll)
    fun observeByNotebook(notebookRemoteId: String): Flow<List<StudyDoc>> =
        dao.observeByNotebook(notebookRemoteId).map(::decodeAll)
    fun observeByOwner(ownerUid: String): Flow<List<StudyDoc>> =
        dao.observeByOwner(ownerUid).map(::decodeAll)
    fun observeVisibleForOwner(ownerUid: String?): Flow<List<StudyDoc>> =
        if (ownerUid == null) {
            dao.observeAll().map { list ->
                decodeAll(list.filter { it.ownerUid == null })
            }
        } else {
            observeByOwner(ownerUid)
        }
    fun observeVisibleEntriesForOwner(ownerUid: String?): Flow<List<StudyDocListEntry>> =
        (if (ownerUid == null) dao.observeAll() else dao.observeByOwner(ownerUid))
            .map { entities ->
                withContext(serializationDispatcher) {
                    entities.asSequence()
                        .filter { ownerUid != null || it.ownerUid == null }
                        .mapNotNull { entity ->
                            entity.toDoc()?.let { doc ->
                                StudyDocListEntry(
                                    doc = doc,
                                    syncStatus = when {
                                        ownerUid == null -> StudyDocSyncStatus.LOCAL_ONLY
                                        !entity.isDirty && entity.lastSyncedAt != null -> StudyDocSyncStatus.SYNCED
                                        else -> StudyDocSyncStatus.PENDING
                                    },
                                )
                            }
                        }.toList()
                }
            }
    fun search(query: String): Flow<List<StudyDoc>> =
        dao.search("%${query.trim()}%").map(::decodeAll)

    suspend fun getById(id: Long): StudyDoc? = decode(dao.getById(id))
    suspend fun getByRemoteId(remoteId: String): StudyDoc? = decode(dao.getByRemoteId(remoteId))
    suspend fun getEntityByRemoteId(remoteId: String): StudyDocEntity? = dao.getByRemoteId(remoteId)
    suspend fun getDirtyForSync(ownerUid: String): List<StudyDocEntity> =
        dao.getDirtyForSync(ownerUid)
    suspend fun getDeletedForSync(ownerUid: String): List<StudyDocEntity> =
        dao.getDeletedForSync(ownerUid)
    suspend fun claimUnowned(ownerUid: String) = dao.claimUnowned(ownerUid)

    suspend fun save(doc: StudyDoc, ownerUid: String? = null) {
        check(doc.hasPersistableTitle()) {
            "No se puede guardar un documento sin titulo"
        }
        saveInternal(doc, ownerUid, isPublished = true)
    }

    /** Saves the working copy without publishing a new teaching to the list. */
    suspend fun saveDraft(doc: StudyDoc, ownerUid: String? = null) {
        val existing = doc.remoteId?.let { dao.getByRemoteId(it) }
        saveInternal(
            doc = doc,
            ownerUid = ownerUid,
            // Once published, autosave keeps the teaching visible while preserving its edits.
            isPublished = existing?.isPublished == true,
        )
    }

    private suspend fun saveInternal(doc: StudyDoc, ownerUid: String?, isPublished: Boolean) {
        val remoteId = doc.remoteId ?: doc.id.value
        val now = clock()
        val existing = dao.getByRemoteId(remoteId)
        val docJson = withContext(serializationDispatcher) { StudyDocJson.encode(doc) }
        val entity = StudyDocEntity(
            remoteId = remoteId,
            title = doc.title,
            notebookRemoteId = doc.metadata.notebook,
            tagsCsv = doc.metadata.tags.joinToString(","),
            blockCount = doc.blocks.size,
            version = doc.version,
            docJson = docJson,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            ownerUid = existing?.ownerUid ?: ownerUid ?: ownerUidProvider(),
            lastSyncedAt = existing?.lastSyncedAt,
            syncVersion = existing?.syncVersion ?: 0L,
            isDirty = true,
            isPublished = isPublished,
        )
        if (existing == null) dao.insert(entity) else dao.update(entity.copy(id = existing.id))
        FirestoreSyncManager.requestStudiesSync()
    }

    suspend fun softDelete(localId: Long, deletedAt: Long) = dao.softDelete(localId, deletedAt)
    suspend fun hardDelete(docId: DocId) = dao.hardDeleteByRemoteId(docId.value)

    suspend fun delete(doc: StudyDoc) {
        val remoteId = doc.remoteId ?: doc.id.value
        val existing = dao.getByRemoteId(remoteId) ?: return
        if (!existing.isPublished) {
            dao.hardDelete(existing.id)
            return
        }
        dao.softDelete(existing.id, clock())
        FirestoreSyncManager.requestStudiesSync()
    }

    suspend fun discardDraft(remoteId: String) {
        val existing = dao.getByRemoteId(remoteId)
        if (existing != null && !existing.isPublished) {
            dao.hardDeleteByRemoteId(remoteId)
        }
    }

    suspend fun upsertRemote(entity: StudyDocEntity) {
        val existing = dao.getByRemoteId(entity.remoteId)
        if (existing == null) {
            dao.insert(entity.copy(id = 0L))
        } else {
            dao.update(entity.copy(id = existing.id))
        }
    }

    suspend fun markSyncedIfUnchanged(
        localId: Long,
        expectedUpdatedAt: Long,
        syncedAt: Long,
        syncVersion: Long,
        ownerUid: String,
    ): Boolean = dao.markSyncedIfUnchanged(
        id = localId,
        expectedUpdatedAt = expectedUpdatedAt,
        syncedAt = syncedAt,
        syncVersion = syncVersion,
        ownerUid = ownerUid,
    ) > 0

    suspend fun hardDeleteByRemoteId(remoteId: String) = dao.hardDeleteByRemoteId(remoteId)
    suspend fun countActive(): Int = dao.countActive()

    private suspend fun decode(entity: StudyDocEntity?): StudyDoc? =
        withContext(serializationDispatcher) { entity?.toDoc() }

    private suspend fun decodeAll(entities: List<StudyDocEntity>): List<StudyDoc> =
        withContext(serializationDispatcher) { entities.mapNotNull { it.toDoc() } }
}

private fun StudyDocEntity.toDoc(): StudyDoc? = StudyDocJson.decode(docJson)

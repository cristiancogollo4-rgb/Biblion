package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudyDocRepository(
    private val dao: StudyDocDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    fun observeAll(): Flow<List<StudyDoc>> = dao.observeAll().map { list -> list.mapNotNull { it.toDoc() } }
    fun observeByNotebook(notebookRemoteId: String): Flow<List<StudyDoc>> =
        dao.observeByNotebook(notebookRemoteId).map { list -> list.mapNotNull { it.toDoc() } }
    fun observeByOwner(ownerUid: String): Flow<List<StudyDoc>> =
        dao.observeByOwner(ownerUid).map { list -> list.mapNotNull { it.toDoc() } }
    fun search(query: String): Flow<List<StudyDoc>> = dao.search("%${query.trim()}%").map { list -> list.mapNotNull { it.toDoc() } }

    suspend fun getById(id: Long): StudyDoc? = dao.getById(id)?.toDoc()
    suspend fun getByRemoteId(remoteId: String): StudyDoc? = dao.getByRemoteId(remoteId)?.toDoc()
    suspend fun getDirtyForSync(): List<StudyDocEntity> = dao.getDirtyForSync()

    suspend fun save(doc: StudyDoc, ownerUid: String? = null) {
        val remoteId = doc.id.value
        val now = clock()
        val entity = StudyDocEntity(
            remoteId = remoteId,
            title = doc.title,
            blockCount = doc.blocks.size,
            version = doc.version,
            docJson = StudyDocJson.encode(doc),
            createdAt = now,
            updatedAt = now,
            ownerUid = ownerUid,
            isDirty = true,
        )
        val existing = dao.getByRemoteId(remoteId)
        if (existing == null) dao.insert(entity) else dao.update(entity.copy(id = existing.id))
    }

    suspend fun softDelete(localId: Long, deletedAt: Long) = dao.softDelete(localId, deletedAt)
    suspend fun hardDelete(docId: DocId) = dao.hardDeleteByRemoteId(docId.value)
    suspend fun markSynced(localId: Long, syncVersion: Long) = dao.markSynced(localId, syncVersion)
    suspend fun countActive(): Int = dao.countActive()
}

private fun StudyDocEntity.toDoc(): StudyDoc? = StudyDocJson.decode(docJson)

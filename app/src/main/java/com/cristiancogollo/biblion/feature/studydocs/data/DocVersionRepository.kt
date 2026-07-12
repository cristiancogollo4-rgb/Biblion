package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DocVersionRepository(
    private val dao: DocVersionDao,
    private val maxVersions: Int = DEFAULT_MAX_VERSIONS,
) {

    suspend fun saveSnapshot(doc: StudyDoc, changeDescription: String? = null): Long {
        val nextVersion = dao.maxVersionNumber(doc.id.value) + 1
        val entity = DocVersionEntity(
            docRemoteId = doc.id.value,
            versionNumber = nextVersion,
            docJson = StudyDocJson.encode(doc),
            title = doc.title,
            blockCount = doc.blocks.size,
            wordCount = doc.wordCount(),
            changeDescription = changeDescription,
            createdAt = System.currentTimeMillis(),
        )
        val id = dao.insert(entity)
        trimIfNeeded(doc.id.value)
        return id
    }

    fun observeVersions(docRemoteId: String): Flow<List<DocVersion>> =
        dao.observeVersions(docRemoteId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getVersions(docRemoteId: String): List<DocVersion> =
        dao.getVersions(docRemoteId).map { it.toDomain() }

    suspend fun getVersionById(id: Long): DocVersion? =
        dao.getById(id)?.toDomain()

    suspend fun getVersionByNumber(docRemoteId: String, versionNumber: Int): DocVersion? =
        dao.getByVersionNumber(docRemoteId, versionNumber)?.toDomain()

    suspend fun getDocAtVersion(id: Long): StudyDoc? {
        val entity = dao.getById(id) ?: return null
        return StudyDocJson.decode(entity.docJson)
    }

    suspend fun deleteVersion(id: Long) = dao.deleteById(id)

    suspend fun deleteAllVersions(docRemoteId: String) = dao.deleteAllForDoc(docRemoteId)

    suspend fun countVersions(docRemoteId: String): Int = dao.countVersions(docRemoteId)

    private suspend fun trimIfNeeded(docRemoteId: String) {
        val count = dao.countVersions(docRemoteId)
        if (count > maxVersions) {
            dao.trimToMaxVersions(docRemoteId, count - maxVersions)
        }
    }

    data class DocVersion(
        val id: Long,
        val docRemoteId: String,
        val versionNumber: Int,
        val title: String,
        val blockCount: Int,
        val wordCount: Int,
        val changeDescription: String?,
        val createdAt: Long,
    )

    private fun DocVersionEntity.toDomain() = DocVersion(
        id = id,
        docRemoteId = docRemoteId,
        versionNumber = versionNumber,
        title = title,
        blockCount = blockCount,
        wordCount = wordCount,
        changeDescription = changeDescription,
        createdAt = createdAt,
    )

    companion object {
        const val DEFAULT_MAX_VERSIONS = 50
    }
}

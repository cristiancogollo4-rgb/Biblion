package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyDocSyncPolicyTest {

    @Test
    fun newer_dirty_local_document_wins_over_remote() {
        val local = localEntity(updatedAt = 200L, isDirty = true)
        val remote = remoteRecord(updatedAt = 100L)

        assertEquals(
            StudySyncResolution.KEEP_LOCAL,
            StudyDocSyncPolicy.resolve(local, remote, OWNER),
        )
    }

    @Test
    fun newer_remote_document_preserves_a_dirty_local_conflict() {
        val local = localEntity(updatedAt = 100L, isDirty = true)
        val remote = remoteRecord(updatedAt = 200L)

        assertEquals(
            StudySyncResolution.PRESERVE_LOCAL_CONFLICT_AND_APPLY_REMOTE,
            StudyDocSyncPolicy.resolve(local, remote, OWNER),
        )
    }

    @Test
    fun newer_remote_tombstone_deletes_local_document() {
        val local = localEntity(updatedAt = 100L, isDirty = false)
        val remote = remoteRecord(updatedAt = 200L, deletedAt = 200L)

        assertEquals(
            StudySyncResolution.DELETE_LOCAL,
            StudyDocSyncPolicy.resolve(local, remote, OWNER),
        )
    }

    @Test
    fun newer_remote_tombstone_preserves_unsynced_local_work_as_conflict() {
        val local = localEntity(updatedAt = 100L, isDirty = true)
        val remote = remoteRecord(updatedAt = 200L, deletedAt = 200L)

        assertEquals(
            StudySyncResolution.PRESERVE_LOCAL_CONFLICT_AND_APPLY_REMOTE,
            StudyDocSyncPolicy.resolve(local, remote, OWNER),
        )
    }

    @Test
    fun documents_owned_by_another_user_are_never_applied() {
        val remote = remoteRecord(updatedAt = 200L, ownerUid = "other-user")

        assertEquals(
            StudySyncResolution.IGNORE_FOREIGN_OWNER,
            StudyDocSyncPolicy.resolve(null, remote, OWNER),
        )
    }

    @Test
    fun unpublished_remote_drafts_are_ignored() {
        val remote = remoteRecord(updatedAt = 200L, isPublished = false)

        assertEquals(
            StudySyncResolution.IGNORE_INVALID_REMOTE,
            StudyDocSyncPolicy.resolve(null, remote, OWNER),
        )
    }

    @Test
    fun remote_payload_becomes_clean_owned_local_document() {
        val remote = remoteRecord(updatedAt = 200L)

        val entity = StudyDocSyncPolicy.toLocalEntity(remote, OWNER, syncedAt = 250L)

        assertNotNull(entity)
        requireNotNull(entity)
        assertEquals(OWNER, entity.ownerUid)
        assertEquals(REMOTE_ID, entity.remoteId)
        assertEquals(200L, entity.updatedAt)
        assertEquals(250L, entity.lastSyncedAt)
        assertFalse(entity.isDirty)
        assertTrue(entity.isPublished)
        assertEquals(REMOTE_ID, StudyDocJson.decode(entity.docJson)?.remoteId)
    }

    @Test
    fun conflict_copy_receives_a_new_identity_and_remains_dirty_for_upload() {
        val local = localEntity(updatedAt = 100L, isDirty = true)

        val conflict = StudyDocSyncPolicy.createConflictCopy(local, OWNER, now = 300L)

        assertNotNull(conflict)
        requireNotNull(conflict)
        assertTrue(conflict.remoteId != local.remoteId)
        assertTrue(conflict.title.endsWith("(conflicto local)"))
        assertTrue(conflict.isDirty)
        assertEquals(0L, conflict.syncVersion)
        assertEquals(conflict.remoteId, StudyDocJson.decode(conflict.docJson)?.remoteId)
    }

    @Test
    fun legacy_remote_document_is_migrated_to_current_blocks() {
        val legacyPayload =
            """
            {
              "blocks": [
                {
                  "nodeType": "paragraph",
                  "blockId": "legacy-paragraph",
                  "text": "Introduccion"
                },
                {
                  "nodeType": "citation",
                  "citationId": "legacy-verse",
                  "reference": {
                    "book": "Juan",
                    "chapter": 3,
                    "verseStart": 16,
                    "verseEnd": 16
                  },
                  "text": "Porque de tal manera amo Dios al mundo.",
                  "version": "rv1960"
                }
              ],
              "tags": ["fe"]
            }
            """.trimIndent()
        val remote = remoteRecord(updatedAt = 200L).copy(docJson = legacyPayload)

        val entity = StudyDocSyncPolicy.toLocalEntity(remote, OWNER, syncedAt = 250L)
        val migrated = entity?.docJson?.let(StudyDocJson::decode)

        assertNotNull(migrated)
        requireNotNull(migrated)
        assertEquals(2, migrated.blocks.size)
        assertTrue(migrated.blocks[0] is StudyBlock.Paragraph)
        assertTrue(migrated.blocks[1] is StudyBlock.Verse)
        assertEquals(listOf("fe"), migrated.metadata.tags)
    }

    private fun localEntity(
        updatedAt: Long,
        isDirty: Boolean,
    ): StudyDocEntity = StudyDocEntity(
        id = 1L,
        remoteId = REMOTE_ID,
        title = "Ensenanza",
        ownerUid = OWNER,
        docJson = docJson(updatedAt),
        createdAt = 50L,
        updatedAt = updatedAt,
        isDirty = isDirty,
        isPublished = true,
    )

    private fun remoteRecord(
        updatedAt: Long,
        deletedAt: Long? = null,
        ownerUid: String = OWNER,
        isPublished: Boolean = true,
    ): RemoteStudyRecord = RemoteStudyRecord(
        remoteId = REMOTE_ID,
        title = "Ensenanza remota",
        notebookRemoteId = null,
        tagsCsv = "Fe",
        blockCount = 1,
        docVersion = 3,
        docJson = docJson(updatedAt),
        createdAt = 50L,
        updatedAt = updatedAt,
        ownerUid = ownerUid,
        deletedAt = deletedAt,
        syncVersion = updatedAt,
        isPublished = isPublished,
    )

    private fun docJson(updatedAt: Long): String = StudyDocJson.encode(
        StudyDoc(
            id = DocId(REMOTE_ID),
            remoteId = REMOTE_ID,
            title = "Ensenanza remota",
            blocks = listOf(
                StudyBlock.Paragraph(
                    id = BlockId("paragraph"),
                    text = StyledText(raw = "Contenido"),
                ),
            ),
            createdAt = 50L,
            updatedAt = updatedAt,
        ),
    )

    private companion object {
        const val OWNER = "user-1"
        const val REMOTE_ID = "study-1"
    }
}

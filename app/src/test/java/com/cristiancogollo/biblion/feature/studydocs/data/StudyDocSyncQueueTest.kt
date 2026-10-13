package com.cristiancogollo.biblion.feature.studydocs.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StudyDocSyncQueueTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        StudyDocDatabase::class.java,
    ).build()
    private var now = 1_000L
    private val repository = StudyDocRepository(
        dao = database.studyDocDao(),
        clock = { now },
        ownerUidProvider = { OWNER },
    )

    @After
    fun close() = database.close()

    @Test
    fun publishedTeachingRemainsQueuedAfterSubsequentAutosave() = runBlocking {
        val doc = StudyDoc(id = DocId("published-1"), title = "Enseñanza")
        repository.save(doc)

        val first = repository.getDirtyForSync(OWNER).single()
        assertTrue(first.isPublished)
        assertTrue(first.isDirty)
        assertEquals(OWNER, first.ownerUid)
        assertTrue(repository.markSyncedIfUnchanged(first.id, first.updatedAt, 1_100L, 1L, OWNER))

        now = 1_200L
        repository.saveDraft(doc.copy(title = "Enseñanza editada"))
        val edited = repository.getDirtyForSync(OWNER).single()
        assertTrue(edited.isPublished)
        assertTrue(edited.isDirty)
        assertEquals("Enseñanza editada", edited.title)
    }

    @Test
    fun unpublishedDraftNeverEntersCloudQueue() = runBlocking {
        repository.saveDraft(StudyDoc(id = DocId("draft-1"), title = "Borrador"))

        assertTrue(repository.getDirtyForSync(OWNER).isEmpty())
        val draft = repository.getEntityByRemoteId("draft-1")!!
        assertFalse(draft.isPublished)
        assertTrue(draft.isDirty)
    }

    private companion object {
        const val OWNER = "user-1"
    }
}

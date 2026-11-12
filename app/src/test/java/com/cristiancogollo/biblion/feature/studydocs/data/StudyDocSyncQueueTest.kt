package com.cristiancogollo.biblion.feature.studydocs.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
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
    fun autosaveWithBlankTitleKeepsPublishedTitle() = runBlocking {
        val doc = StudyDoc(id = DocId("published-2"), title = "Gracia")
        repository.save(doc)

        repository.saveDraft(doc.copy(title = "   "))

        val stored = repository.getEntityByRemoteId("published-2")!!
        assertTrue(stored.isPublished)
        assertEquals("Gracia", stored.title)
    }

    @Test
    fun emptyNewDraftIsNeverStored() = runBlocking {
        repository.saveDraft(StudyDoc(id = DocId("empty-1")))

        assertEquals(null, repository.getEntityByRemoteId("empty-1"))
    }

    @Test
    fun draftEmptiedByTheUserIsRemoved() = runBlocking {
        val draft = StudyDoc(
            id = DocId("draft-2"),
            blocks = listOf(StudyBlock.Paragraph(text = StyledText("Idea"))),
        )
        repository.saveDraft(draft)
        assertTrue(repository.getEntityByRemoteId("draft-2") != null)

        repository.saveDraft(draft.copy(blocks = listOf(StudyBlock.Paragraph())))

        assertEquals(null, repository.getEntityByRemoteId("draft-2"))
    }

    @Test
    fun purgeRemovesOnlyEmptyUnpublishedDrafts() = runBlocking {
        val dao = database.studyDocDao()
        fun entity(id: String, doc: StudyDoc, published: Boolean) = StudyDocEntity(
            remoteId = id,
            title = doc.title,
            docJson = StudyDocJson.encode(doc),
            isPublished = published,
        )
        dao.insert(entity("empty", StudyDoc(id = DocId("empty")), published = false))
        dao.insert(entity("text", StudyDoc(id = DocId("text"), blocks = listOf(StudyBlock.Paragraph(text = StyledText("6")))), published = false))
        dao.insert(entity("titled", StudyDoc(id = DocId("titled"), title = "Idea"), published = false))
        dao.insert(entity("published", StudyDoc(id = DocId("published")), published = true))
        dao.insert(StudyDocEntity(remoteId = "broken", title = "", docJson = "{roto", isPublished = false))

        assertEquals(1, repository.purgeEmptyDrafts())

        assertEquals(null, repository.getEntityByRemoteId("empty"))
        listOf("text", "titled", "published", "broken").forEach {
            assertTrue(it, repository.getEntityByRemoteId(it) != null)
        }
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

package com.cristiancogollo.biblion.feature.studydocs.ui.list

import com.cristiancogollo.biblion.MainDispatcherRule
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDao
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocEntity
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StudyDocsListViewModelFilterTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun vm(): StudyDocsListViewModel = StudyDocsListViewModel(StudyDocRepository(FakeDao()))

    private fun fakeDaoWithDocs(vararg docs: StudyDoc): FakeDao {
        val entities = docs.map { doc ->
            StudyDocEntity(
                id = 0,
                remoteId = doc.id.value,
                title = doc.title,
                tagsCsv = doc.metadata.tags.joinToString(","),
                blockCount = doc.blocks.size,
                docJson = "{}",
            )
        }
        return FakeDao(initial = entities)
    }

    @Test fun `visibleDocs empty when state has no docs`() {
        val vm = vm()
        val visible = vm.state.value.visibleDocs
        assertTrue(visible.isEmpty())
    }

    @Test fun `setTitleFilter updates state`() {
        val vm = vm()
        vm.setTitleFilter("Fe")
        assertEquals("Fe", vm.state.value.titleFilter)
    }

    @Test fun `toggleTagFilter adds tag`() {
        val vm = vm()
        vm.toggleTagFilter("predicacion")
        assertTrue("predicacion" in vm.state.value.selectedTagFilters)
    }

    @Test fun `toggleTagFilter removes tag if already present`() {
        val vm = vm()
        vm.toggleTagFilter("predicacion")
        vm.toggleTagFilter("predicacion")
        assertTrue("predicacion" !in vm.state.value.selectedTagFilters)
    }

    @Test fun `toggleTagFilter accumulates multiple tags`() {
        val vm = vm()
        vm.toggleTagFilter("predicacion")
        vm.toggleTagFilter("jovenes")
        assertEquals(setOf("predicacion", "jovenes"), vm.state.value.selectedTagFilters)
    }

    @Test fun `clearTagFilters empties the filter set`() {
        val vm = vm()
        vm.toggleTagFilter("predicacion")
        vm.toggleTagFilter("jovenes")
        vm.clearTagFilters()
        assertTrue(vm.state.value.selectedTagFilters.isEmpty())
    }

    @Test fun `StudyDocsListState visibleDocs filters by title (lowercase contains)`() {
        val state = StudyDocsListState(
            docs = listOf(
                StudyDoc(id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("a"), title = "La fe en Cristo"),
                StudyDoc(id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("b"), title = "Oracion diaria"),
            ),
            titleFilter = "fe",
        )
        val visible = state.visibleDocs
        assertEquals(1, visible.size)
        assertEquals("La fe en Cristo", visible[0].title)
    }

    @Test fun `StudyDocsListState visibleDocs matches any of selected tags`() {
        val state = StudyDocsListState(
            docs = listOf(
                StudyDoc(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("a"),
                    title = "Doc A",
                    metadata = com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata(tags = listOf("predicacion", "jovenes")),
                ),
                StudyDoc(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("b"),
                    title = "Doc B",
                    metadata = com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata(tags = listOf("devocional", "iglesia")),
                ),
            ),
            selectedTagFilters = setOf("predicacion", "devocional"),
        )
        val visible = state.visibleDocs
        assertEquals(2, visible.size)
    }

    @Test fun `StudyDocsListState visibleDocs combines title and tag filters`() {
        val state = StudyDocsListState(
            docs = listOf(
                StudyDoc(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("a"),
                    title = "Fe para jovenes",
                    metadata = com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata(tags = listOf("predicacion", "jovenes")),
                ),
                StudyDoc(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("b"),
                    title = "Fe para adultos",
                    metadata = com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata(tags = listOf("predicacion", "iglesia")),
                ),
                StudyDoc(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("c"),
                    title = "Esperanza",
                    metadata = com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata(tags = listOf("predicacion")),
                ),
            ),
            titleFilter = "Fe",
            selectedTagFilters = setOf("jovenes"),
        )
        val visible = state.visibleDocs
        // Solo el doc A tiene "Fe" en el titulo y "jovenes" en tags
        assertEquals(1, visible.size)
        assertEquals("Fe para jovenes", visible[0].title)
    }

    @Test fun `StudyDocsListState visibleDocs returns all when no filters set`() {
        val state = StudyDocsListState(
            docs = listOf(
                StudyDoc(id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("a"), title = "A"),
                StudyDoc(id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("b"), title = "B"),
                StudyDoc(id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("c"), title = "C"),
            ),
        )
        assertEquals(3, state.visibleDocs.size)
    }

    @Test fun `visibleDocs title match is case insensitive`() {
        val state = StudyDocsListState(
            docs = listOf(
                StudyDoc(id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("a"), title = "FE PARA JOVENES"),
                StudyDoc(id = com.cristiancogollo.biblion.feature.studydocs.model.DocId("b"), title = "oracion"),
            ),
            titleFilter = "fe",
        )
        assertEquals(1, state.visibleDocs.size)
        assertEquals("FE PARA JOVENES", state.visibleDocs[0].title)
    }
}

private class FakeDao(
    initial: List<StudyDocEntity> = emptyList(),
) : StudyDocDao {
    private var docs: MutableList<StudyDocEntity> = initial.toMutableList()

    override fun observeAll(): Flow<List<StudyDocEntity>> = flowOf(docs.toList())
    override fun observeByNotebook(notebookRemoteId: String): Flow<List<StudyDocEntity>> = flowOf(emptyList())
    override fun observeByOwner(ownerUid: String): Flow<List<StudyDocEntity>> = flowOf(emptyList())
    override fun search(query: String): Flow<List<StudyDocEntity>> = flowOf(emptyList())
    override suspend fun getById(id: Long): StudyDocEntity? = docs.find { it.id == id }
    override suspend fun getByRemoteId(remoteId: String): StudyDocEntity? = docs.find { it.remoteId == remoteId }
    override suspend fun getDirtyForSync(ownerUid: String): List<StudyDocEntity> = emptyList()
    override suspend fun getDeletedForSync(ownerUid: String): List<StudyDocEntity> = emptyList()
    override suspend fun insert(entity: StudyDocEntity): Long {
        docs.add(entity.copy(id = (docs.size + 1).toLong()))
        return docs.size.toLong()
    }
    override suspend fun update(entity: StudyDocEntity) {
        val idx = docs.indexOfFirst { it.remoteId == entity.remoteId }
        if (idx >= 0) docs[idx] = entity
    }
    override suspend fun softDelete(id: Long, deletedAt: Long) {
        docs.removeAll { it.id == id }
    }
    override suspend fun hardDelete(id: Long) {
        docs.removeAll { it.id == id }
    }
    override suspend fun hardDeleteByRemoteId(remoteId: String) {
        docs.removeAll { it.remoteId == remoteId }
    }
    override suspend fun markSyncedIfUnchanged(
        id: Long,
        expectedUpdatedAt: Long,
        syncedAt: Long,
        syncVersion: Long,
        ownerUid: String,
    ): Int = 1
    override suspend fun countActive(): Int = docs.size
}

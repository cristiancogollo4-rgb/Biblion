package com.cristiancogollo.biblion.feature.studydocs.domain

import androidx.compose.ui.text.style.TextAlign
import com.cristiancogollo.biblion.MainDispatcherRule
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDao
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocEntity
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.engine.StudyOp
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ViewModelAlignmentTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Inyecta un doc con un solo bloque (paragraph) y devuelve el
     * VM + id del bloque. Usamos newDraft() (que crea 1 Paragraph
     * vacio) y luego InsertBlock con un bloque custom para tener
     * control del id.
     */
    private fun vmWithCustomBlock(custom: StudyBlock): Pair<StudyDocViewModel, BlockId> {
        val vm = StudyDocViewModel(StudyDocRepository(FakeDao()))
        // newDraft crea 1 Paragraph con id random. Insertamos el custom al inicio.
        vm.newDraft()
        vm.applyOp(StudyOp.InsertBlock(atIndex = 0, block = custom))
        vm.selectBlock(custom.id.value)
        return vm to custom.id
    }

    @Test fun `cycleAlignment sets Start by default for Paragraph`() {
        val block = StudyBlock.Paragraph(id = BlockId("p1"), text = StyledText.plain("hola"))
        val (vm, _) = vmWithCustomBlock(block)
        vm.cycleAlignment()
        assertEquals(TextAlign.Start, vm.blockAlignments[BlockId("p1")])
    }

    @Test fun `cycleAlignment on Paragraph progresses Start - Center - End - Justify - Start`() {
        val block = StudyBlock.Paragraph(id = BlockId("p1"), text = StyledText.plain("hola"))
        val (vm, _) = vmWithCustomBlock(block)
        vm.cycleAlignment(); assertEquals(TextAlign.Start, vm.blockAlignments[BlockId("p1")])
        vm.cycleAlignment(); assertEquals(TextAlign.Center, vm.blockAlignments[BlockId("p1")])
        vm.cycleAlignment(); assertEquals(TextAlign.End, vm.blockAlignments[BlockId("p1")])
        vm.cycleAlignment(); assertEquals(TextAlign.Justify, vm.blockAlignments[BlockId("p1")])
        vm.cycleAlignment(); assertEquals(TextAlign.Start, vm.blockAlignments[BlockId("p1")])
    }

    @Test fun `cycleAlignment on Heading also works`() {
        val heading = StudyBlock.Heading(id = BlockId("h1"), level = 1, text = StyledText.plain("H"))
        val (vm, _) = vmWithCustomBlock(heading)
        vm.cycleAlignment()
        assertEquals(TextAlign.Start, vm.blockAlignments[BlockId("h1")])
    }

    @Test fun `cycleAlignment on Quote also works`() {
        val quote = StudyBlock.Quote(id = BlockId("q1"), text = StyledText.plain("Q"))
        val (vm, _) = vmWithCustomBlock(quote)
        vm.cycleAlignment()
        assertEquals(TextAlign.Start, vm.blockAlignments[BlockId("q1")])
    }

    @Test fun `cycleAlignment no-op on structural Divider block`() {
        val divider = StudyBlock.Divider(id = BlockId("d1"))
        val (vm, _) = vmWithCustomBlock(divider)
        vm.cycleAlignment()
        assertNull(vm.blockAlignments[BlockId("d1")])
    }

    @Test fun `cycleAlignment uses lastFocusedBlockId as fallback when no selected`() {
        val block = StudyBlock.Paragraph(id = BlockId("p1"), text = StyledText.plain("x"))
        val (vm, blockId) = vmWithCustomBlock(block)
        vm.onBlockFocused(blockId)
        vm.cycleAlignment()
        assertEquals(TextAlign.Start, vm.blockAlignments[blockId])
    }

    @Test fun `cycleAlignment is no-op when no selected and no focused`() {
        val vm = StudyDocViewModel(StudyDocRepository(FakeDao()))
        // No newDraft ni loadByRemoteId: estado vacio por completo.
        vm.cycleAlignment()
        assertTrue(vm.blockAlignments.isEmpty())
    }

    private class FakeDao : StudyDocDao {
        override fun observeAll(): Flow<List<StudyDocEntity>> = flowOf(emptyList())
        override fun observeByNotebook(notebookRemoteId: String): Flow<List<StudyDocEntity>> = flowOf(emptyList())
        override fun observeByOwner(ownerUid: String): Flow<List<StudyDocEntity>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<StudyDocEntity>> = flowOf(emptyList())
        override suspend fun getById(id: Long): StudyDocEntity? = null
        override suspend fun getByRemoteId(remoteId: String): StudyDocEntity? = null
        override suspend fun getDirtyForSync(): List<StudyDocEntity> = emptyList()
        override suspend fun getDeletedForSync(): List<StudyDocEntity> = emptyList()
        override suspend fun insert(entity: StudyDocEntity): Long = 0L
        override suspend fun update(entity: StudyDocEntity) {}
        override suspend fun softDelete(id: Long, deletedAt: Long) {}
        override suspend fun hardDelete(id: Long) {}
        override suspend fun hardDeleteByRemoteId(remoteId: String) {}
        override suspend fun markSynced(id: Long, syncedAt: Long) {}
        override suspend fun countActive(): Int = 0
    }
}


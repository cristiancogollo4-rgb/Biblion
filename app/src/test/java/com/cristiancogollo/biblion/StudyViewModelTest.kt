package com.cristiancogollo.biblion

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.RunWith
import org.junit.runner.Description
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StudyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun observer_starts_on_init_and_debounce_waits_expected_time() = runTest {
        val dao = FakeStudyDao()
        val viewModel = buildViewModel(dao)

        advanceUntilIdle()
        viewModel.process(StudyIntent.UpdateTitle("Título actualizado"))

        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS - 1)
        advanceUntilIdle()
        assertEquals(0, dao.updateStudyCalls)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(1, dao.updateStudyCalls)
    }

    @Test
    fun does_not_save_when_signature_has_not_changed() = runTest {
        val dao = FakeStudyDao()
        val viewModel = buildViewModel(dao)

        advanceUntilIdle()
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS + 10)
        advanceUntilIdle()
        assertEquals(0, dao.updateStudyCalls)

        viewModel.process(StudyIntent.UpdateTitle("Título base"))
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS + 10)
        advanceUntilIdle()
        assertEquals(0, dao.updateStudyCalls)
    }

    @Test
    fun load_study_when_build_signature_fails_does_not_publish_partial_study() = runTest {
        val dao = FakeStudyDao()
        val viewModel = buildViewModel(dao)

        advanceUntilIdle()
        viewModel.process(StudyIntent.UpdateTitle("Estado previo"))
        advanceUntilIdle()
        val before = viewModel.state.value

        viewModel.buildSignatureOverride = {
            throw IllegalStateException("forced buildSignature failure")
        }
        viewModel.process(StudyIntent.SelectStudy(7L))
        advanceUntilIdle()

        val after = viewModel.state.value
        assertEquals(before.selectedStudyId, after.selectedStudyId)
        assertEquals(before.title, after.title)
        assertEquals(before.richHtml, after.richHtml)
        assertEquals("No se pudo cargar el estudio.", after.loadErrorMessage)
    }

    private fun buildViewModel(dao: FakeStudyDao): StudyViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        return StudyViewModel(
            application = application,
            dao = dao,
            autoSaveDebounceMs = AUTOSAVE_DEBOUNCE_MS
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeStudyDao : StudyDao {
    private val json = Json { encodeDefaults = true; classDiscriminator = "nodeType" }
    private val notebook = StudyNotebookEntity(
        id = 1L,
        title = "Notebook",
        createdAt = 100L,
        updatedAt = 100L
    )
    private val study = StudyEntity(
        id = 7L,
        title = "Título base",
        notebookId = notebook.id,
        contentSerialized = json.encodeToString(
            SerializedStudyDocument(
                blocks = listOf(StudyBlockNode.RichText(html = "Texto inicial"))
            )
        ),
        createdAt = 100L,
        updatedAt = 100L
    )

    var updateStudyCalls: Int = 0
        private set

    override fun observeNotebooks(): Flow<List<StudyNotebookEntity>> = flowOf(listOf(notebook))

    override suspend fun insertNotebook(notebook: StudyNotebookEntity): Long = notebook.id

    override suspend fun updateNotebook(notebook: StudyNotebookEntity) = Unit

    override suspend fun getNotebookCount(): Int = 1

    override fun observeStudies(notebookId: Long): Flow<List<StudyEntity>> = flowOf(listOf(study))

    override fun observeAllStudies(): Flow<List<StudyEntity>> = flowOf(listOf(study))

    override suspend fun insertStudy(study: StudyEntity): Long = study.id

    override suspend fun updateStudy(study: StudyEntity) {
        updateStudyCalls += 1
    }

    override suspend fun getStudy(id: Long): StudyEntity? = if (id == study.id) study else null

    override suspend fun deleteStudy(id: Long) = Unit

    override suspend fun deleteCitationsForStudy(studyId: Long) = Unit

    override suspend fun insertLinkedCitations(citations: List<LinkedCitationEntity>) = Unit

    override suspend fun getLinkedCitations(studyId: Long): List<LinkedCitationEntity> = emptyList()
}

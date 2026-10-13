package com.cristiancogollo.biblion.feature.studydocs.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocSyncStatus
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val teachingFilterCombiningMarks = "\\p{M}+".toRegex()
private val teachingFilterWhitespace = "\\s+".toRegex()

data class StudyDocsListState(
    val docs: List<StudyDoc> = emptyList(),
    val syncStatuses: Map<String, StudyDocSyncStatus> = emptyMap(),
    val isLoading: Boolean = false,
    val titleFilter: String = "",
    val selectedTagFilters: Set<String> = emptySet(),
) {
    val visibleDocs: List<StudyDoc>
        get() {
            val normalizedTitleFilter = titleFilter.normalizeForTeachingFilter()
            val selectedTagsByGroup = selectedTagFilters
                .map(String::normalizeForTeachingFilter)
                .groupBy { tag ->
                    DocTagGroups.groupFor(tag).ifBlank { "custom:$tag" }
                }

            return docs.filter { doc ->
                val titleMatches = normalizedTitleFilter.isBlank() ||
                    doc.title.normalizeForTeachingFilter().contains(normalizedTitleFilter)
                val documentTags = doc.metadata.tags
                    .mapTo(mutableSetOf(), String::normalizeForTeachingFilter)
                val tagsMatch = selectedTagsByGroup.values.all { selectedTagsInGroup ->
                    selectedTagsInGroup.any(documentTags::contains)
                }
                titleMatches && tagsMatch
            }
        }
}

private fun String.normalizeForTeachingFilter(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(teachingFilterCombiningMarks, "")
        .lowercase(Locale.ROOT)
        .replace(teachingFilterWhitespace, " ")

class StudyDocsListViewModel(
    private val repository: StudyDocRepository,
    private val ownerUid: String? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyDocsListState())
    val state: StateFlow<StudyDocsListState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.observeVisibleEntriesForOwner(ownerUid).collect { entries ->
                _state.update {
                    it.copy(
                        docs = entries.map { entry -> entry.doc },
                        syncStatuses = entries.associate { entry ->
                            (entry.doc.remoteId ?: entry.doc.id.value) to entry.syncStatus
                        },
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun setTitleFilter(text: String) {
        _state.update { it.copy(titleFilter = text) }
    }

    fun toggleTagFilter(tag: String) {
        _state.update { current ->
            val normalizedTag = tag.normalizeForTeachingFilter()
            val next = if (normalizedTag in current.selectedTagFilters) {
                current.selectedTagFilters - normalizedTag
            } else {
                current.selectedTagFilters + normalizedTag
            }
            current.copy(selectedTagFilters = next)
        }
    }

    fun clearTagFilters() {
        _state.update { it.copy(selectedTagFilters = emptySet()) }
    }

    fun delete(doc: StudyDoc) {
        viewModelScope.launch {
            repository.delete(doc)
        }
    }

    fun updateTags(
        doc: StudyDoc,
        tags: List<String>,
        onComplete: (Throwable?) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching {
                repository.save(
                    doc.copy(
                        metadata = doc.metadata.copy(tags = tags.distinct()),
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
            onComplete(result.exceptionOrNull())
        }
    }

    class Factory(
        private val repository: StudyDocRepository,
        private val ownerUid: String? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StudyDocsListViewModel(repository, ownerUid) as T
    }
}

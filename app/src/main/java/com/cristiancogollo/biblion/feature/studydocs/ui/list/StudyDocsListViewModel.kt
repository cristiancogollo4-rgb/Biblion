package com.cristiancogollo.biblion.feature.studydocs.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudyDocsListState(
    val docs: List<StudyDoc> = emptyList(),
    val isLoading: Boolean = false,
    val titleFilter: String = "",
    val selectedTagFilters: Set<String> = emptySet(),
) {
    val visibleDocs: List<StudyDoc>
        get() {
            val byTitle = titleFilter.trim().lowercase()
            return docs.filter { doc ->
                val titleMatches = byTitle.isBlank() ||
                    doc.title.lowercase().contains(byTitle)
                val tagsMatch = selectedTagFilters.isEmpty() ||
                    doc.metadata.tags.any { it in selectedTagFilters }
                titleMatches && tagsMatch
            }
        }
}

class StudyDocsListViewModel(
    private val repository: StudyDocRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyDocsListState())
    val state: StateFlow<StudyDocsListState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.observeAll().collect { docs ->
                _state.update { it.copy(docs = docs, isLoading = false) }
            }
        }
    }

    fun setTitleFilter(text: String) {
        _state.update { it.copy(titleFilter = text) }
    }

    fun toggleTagFilter(tag: String) {
        _state.update { current ->
            val next = if (tag in current.selectedTagFilters) {
                current.selectedTagFilters - tag
            } else {
                current.selectedTagFilters + tag
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

    class Factory(private val repository: StudyDocRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyDocsListViewModel(repository) as T
    }
}

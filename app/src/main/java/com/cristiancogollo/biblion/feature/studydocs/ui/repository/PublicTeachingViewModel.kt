package com.cristiancogollo.biblion.feature.studydocs.ui.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiancogollo.biblion.feature.studydocs.data.PublicTeachingRepository
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyProvenance
import com.cristiancogollo.biblion.feature.studydocs.model.PublicTeaching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublicTeachingState(
    val publications: List<PublicTeaching> = emptyList(),
    val query: String = "",
    val selectedTags: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val downloadingId: String? = null,
    val error: String? = null,
) {
    val visiblePublications: List<PublicTeaching>
        get() {
            val normalizedQuery = query.trim().lowercase()
            return publications.filter { teaching ->
                val matchesQuery = normalizedQuery.isBlank() ||
                    teaching.title.lowercase().contains(normalizedQuery) ||
                    teaching.author.alias.lowercase().contains(normalizedQuery)
                val matchesTags = selectedTags.isEmpty() ||
                    selectedTags.all { it in teaching.tags }
                matchesQuery && matchesTags
            }
        }
}

class PublicTeachingViewModel(
    private val publicRepository: PublicTeachingRepository,
    private val localRepository: StudyDocRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PublicTeachingState())
    val state: StateFlow<PublicTeachingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                publicRepository.observePublished().collect { publications ->
                    _state.update { it.copy(publications = publications, isLoading = false, error = null) }
                }
            } catch (error: Throwable) {
                _state.update { it.copy(isLoading = false, error = error.message ?: "No se pudo cargar Biblion") }
            }
        }
    }

    fun setQuery(value: String) = _state.update { it.copy(query = value) }

    fun toggleTag(tag: String) = _state.update { current ->
        val next = if (tag in current.selectedTags) current.selectedTags - tag else current.selectedTags + tag
        current.copy(selectedTags = next)
    }

    fun clearTags() = _state.update { it.copy(selectedTags = emptySet()) }

    fun download(teaching: PublicTeaching, ownerUid: String?, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(downloadingId = teaching.publicationId, error = null) }
            val localId = try {
                val revision = publicRepository.getRevision(
                    publicationId = teaching.publicationId,
                    revisionId = teaching.currentRevisionId,
                ) ?: error("No se encontro la revision publicada")
                val now = System.currentTimeMillis()
                val copy = revision.content.copy(
                    id = DocId.generate(),
                    remoteId = null,
                    metadata = revision.content.metadata.copy(
                        authorUid = revision.author.uid,
                        provenance = StudyProvenance(
                            rootPublicationId = teaching.publicationId,
                            rootRevisionId = revision.rootRevisionId ?: revision.revisionId,
                            parentPublicationId = teaching.publicationId,
                            parentRevisionId = revision.revisionId,
                            importedAt = now,
                            sourceFormat = "repository",
                        ),
                    ),
                    createdAt = now,
                    updatedAt = now,
                )
                localRepository.saveDraft(copy, ownerUid)
                copy.id.value
            } catch (error: Throwable) {
                _state.update { it.copy(error = error.message ?: "No se pudo descargar la ensenanza") }
                null
            }
            _state.update { it.copy(downloadingId = null) }
            onComplete(localId)
        }
    }

    class Factory(
        private val publicRepository: PublicTeachingRepository,
        private val localRepository: StudyDocRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PublicTeachingViewModel(publicRepository, localRepository) as T
    }
}

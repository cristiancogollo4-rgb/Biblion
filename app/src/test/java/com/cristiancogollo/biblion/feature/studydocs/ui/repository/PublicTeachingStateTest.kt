package com.cristiancogollo.biblion.feature.studydocs.ui.repository

import com.cristiancogollo.biblion.feature.studydocs.model.PublicTeaching
import com.cristiancogollo.biblion.feature.studydocs.model.StudyAuthorSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class PublicTeachingStateTest {
    private val author = StudyAuthorSnapshot(uid = "author", alias = "cristian")

    @Test
    fun `filters by title and requires every selected tag`() {
        val state = PublicTeachingState(
            publications = listOf(
                PublicTeaching("one", "rev1", "Fe para jovenes", listOf("fe", "jovenes"), author, updatedAt = 2),
                PublicTeaching("two", "rev2", "Oracion", listOf("oracion"), author, updatedAt = 1),
            ),
            query = "fe",
            selectedTags = setOf("fe", "jovenes"),
            isLoading = false,
        )

        assertEquals(listOf("one"), state.visiblePublications.map { it.publicationId })
    }
}

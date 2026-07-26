package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class BibleVerseBlockTest {

    @Test
    fun displayed_versions_contains_only_the_primary_version_by_default() {
        val block = StudyBlock.Verse(
            bookId = "Genesis",
            chapter = 1,
            verseStart = 2,
            sourceVersion = "rv1960",
            contents = mapOf("rv1960" to "2 Y la tierra estaba desordenada y vacia."),
        )

        assertEquals(listOf("rv1960"), block.displayedVersions())
    }

    @Test
    fun displayed_versions_limits_comparison_to_one_version() {
        val block = StudyBlock.Verse(
            bookId = "Juan",
            chapter = 3,
            verseStart = 16,
            sourceVersion = "rv1960",
            contents = mapOf(
                "rv1960" to "16 Texto principal",
                "nvi" to "16 Texto comparado",
                "dhh" to "16 Otra comparacion",
            ),
            showCompare = true,
            comparedVersions = listOf("nvi", "dhh"),
        )

        assertEquals(listOf("rv1960", "nvi"), block.displayedVersions())
    }
}

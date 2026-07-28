package com.cristiancogollo.biblion.feature.bibi

import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.ui.buildStudyBibiContext
import com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibiStudyIntegrationTest {
    @Test
    fun `study context contains active text outline tags and biblical passages`() {
        val active = StudyBlock.Paragraph(text = StyledText("Aplicación pendiente"))
        val verse = StudyBlock.Verse(
            bookId = "Efesios",
            chapter = 2,
            verseStart = 8,
            verseEnd = 8,
            sourceVersion = "nvi",
            contents = mapOf("nvi" to "Porque por gracia ustedes han sido salvados"),
        )
        val doc = StudyDoc(
            title = "La gracia",
            blocks = listOf(
                StudyBlock.Heading(text = StyledText("Idea central")),
                active,
                verse,
            ),
            metadata = DocMetadata(tags = listOf("devocional", "gracia")),
        )

        val context = buildStudyBibiContext(doc, active.id)

        assertEquals("La gracia", context.title)
        assertEquals("Aplicación pendiente", context.selectedText)
        assertEquals(listOf("devocional", "gracia"), context.tags)
        assertEquals("nvi", context.bibleVersion)
        assertTrue(context.outline.contains("Idea central"))
        assertEquals("Efesios 2:8", context.passages.single().reference)
    }
}

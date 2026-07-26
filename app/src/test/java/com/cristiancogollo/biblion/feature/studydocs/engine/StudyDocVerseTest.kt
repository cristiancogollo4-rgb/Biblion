package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocJson
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.capabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyDocVerseTest {

    @Test
    fun update_block_persists_verse_comparisons() {
        val verse = StudyBlock.Verse(
            id = BlockId("verse-1"),
            bookId = "Juan",
            chapter = 3,
            verseStart = 16,
            verseEnd = 16,
            sourceVersion = "rvr1960",
            contents = mapOf("rvr1960" to "16 Porque de tal manera amo Dios..."),
        )
        val doc = StudyDoc.empty().copy(blocks = listOf(verse))
        val compared = verse.copy(
            contents = verse.contents + ("nvi" to "16 Porque tanto amo Dios al mundo..."),
            showCompare = true,
            comparedVersions = listOf("nvi"),
        )

        val (updated, result) = StudyDocEngine.apply(doc, StudyOp.UpdateBlock(compared))

        assertTrue(result.isSuccess)
        assertEquals(compared, updated.blocks.single())
        val restored = StudyDocJson.decode(StudyDocJson.encode(updated))
        assertEquals(compared, restored?.blocks?.single())
    }

    @Test
    fun document_text_contains_reference_primary_and_saved_comparison() {
        val verse = StudyBlock.Verse(
            bookId = "Juan",
            chapter = 3,
            verseStart = 16,
            verseEnd = 17,
            sourceVersion = "rvr1960",
            contents = mapOf(
                "rvr1960" to "16 Texto principal",
                "nvi" to "16 Texto comparado",
            ),
            showCompare = true,
            comparedVersions = listOf("nvi"),
        )

        assertEquals(
            "Juan 3:16-17\nRVR1960\n16 Texto principal\n\nNVI\n16 Texto comparado",
            verse.documentText(),
        )
    }

    @Test
    fun comparison_operation_replaces_previous_version_and_supports_undo_state() {
        val verse = verse()
        val doc = StudyDoc.empty().copy(blocks = listOf(verse))

        val (withNvi, firstResult) = StudyDocEngine.apply(
            doc,
            StudyOp.SetVerseComparison(verse.id, "nvi", "16 Texto NVI"),
        )
        val (withDhh, secondResult) = StudyDocEngine.apply(
            withNvi,
            StudyOp.SetVerseComparison(verse.id, "dhh", "16 Texto DHH"),
        )

        assertTrue(firstResult.isSuccess)
        assertTrue(secondResult.isSuccess)
        val compared = withDhh.blocks.single() as StudyBlock.Verse
        assertEquals(listOf("dhh"), compared.comparedVersions)
        assertEquals(listOf("rvr1960", "dhh"), compared.displayedVersions())
        assertEquals("16 Texto NVI", compared.contents["nvi"])
    }

    @Test
    fun clearing_comparison_keeps_cached_text_but_returns_to_one_column() {
        val verse = verse()
        val doc = StudyDoc.empty().copy(blocks = listOf(verse))
        val (comparedDoc, _) = StudyDocEngine.apply(
            doc,
            StudyOp.SetVerseComparison(verse.id, "nvi", "16 Texto NVI"),
        )

        val (clearedDoc, result) = StudyDocEngine.apply(
            comparedDoc,
            StudyOp.SetVerseComparison(verse.id),
        )

        assertTrue(result.isSuccess)
        val cleared = clearedDoc.blocks.single() as StudyBlock.Verse
        assertFalse(cleared.showCompare)
        assertTrue(cleared.comparedVersions.isEmpty())
        assertEquals("16 Texto NVI", cleared.contents["nvi"])
    }

    @Test
    fun comparison_rejects_source_version_and_missing_text() {
        val verse = verse()
        val doc = StudyDoc.empty().copy(blocks = listOf(verse))

        val (_, sameVersion) = StudyDocEngine.apply(
            doc,
            StudyOp.SetVerseComparison(verse.id, "RVR1960", "duplicado"),
        )
        val (_, noText) = StudyDocEngine.apply(
            doc,
            StudyOp.SetVerseComparison(verse.id, "nvi", ""),
        )

        assertFalse(sameVersion.isSuccess)
        assertFalse(noText.isSuccess)
    }

    @Test
    fun verse_is_atomic_but_supports_block_size_and_alignment() {
        val verse = verse()
        val capabilities = verse.capabilities()
        val doc = StudyDoc.empty().copy(blocks = listOf(verse))

        val (_, typeResult) = StudyDocEngine.apply(
            doc,
            StudyOp.ChangeBlockType(verse.id, "bullet"),
        )
        val (resizedDoc, resizeResult) = StudyDocEngine.apply(
            doc,
            StudyOp.UpdateBlock(
                verse.copy(
                    fontSize = 24,
                    alignment = com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Center,
                )
            ),
        )

        assertFalse(capabilities.supportsInlineFormatting)
        assertFalse(capabilities.supportsBlockTypeChange)
        assertTrue(capabilities.supportsFontSize)
        assertTrue(capabilities.supportsAlignment)
        assertFalse(typeResult.isSuccess)
        assertTrue(resizeResult.isSuccess)
        val resized = resizedDoc.blocks.single() as StudyBlock.Verse
        assertEquals(24, resized.fontSize)
        assertEquals(
            com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Center,
            resized.alignment,
        )
    }

    @Test
    fun document_plain_text_includes_bible_citations() {
        val doc = StudyDoc.empty().copy(
            title = "Ensenanza",
            blocks = listOf(verse()),
        )

        assertTrue(doc.plainText().contains("Juan 3:16"))
        assertTrue(doc.plainText().contains("Porque de tal manera"))
    }

    @Test
    fun non_contiguous_verses_keep_exact_numbers_and_compact_reference() {
        val citation = verse().copy(
            verseStart = 1,
            verseEnd = 5,
            verseNumbers = listOf(5, 1, 4, 4),
            contents = mapOf(
                "rvr1960" to "1 Texto uno 4 Texto cuatro 5 Texto cinco"
            ),
        )
        val doc = StudyDoc.empty().copy(blocks = listOf(citation))

        val (normalizedDoc, result) = StudyDocEngine.apply(
            doc,
            StudyOp.UpdateBlock(citation),
        )

        assertTrue(result.isSuccess)
        val normalized = normalizedDoc.blocks.single() as StudyBlock.Verse
        assertEquals(listOf(1, 4, 5), normalized.verseNumbers)
        assertEquals(1, normalized.verseStart)
        assertEquals(5, normalized.verseEnd)
        assertEquals("Juan 3:1,4-5", normalized.referenceLabel())
        assertEquals(listOf(1, 4, 5), normalized.selectedVerseNumbers())
    }

    @Test
    fun deleting_a_citation_preserves_surrounding_blocks() {
        val before = StudyBlock.Paragraph(text = com.cristiancogollo.biblion.feature.studydocs.model.StyledText("Antes"))
        val citation = verse()
        val after = StudyBlock.Paragraph(text = com.cristiancogollo.biblion.feature.studydocs.model.StyledText("Despues"))
        val doc = StudyDoc.empty().copy(blocks = listOf(before, citation, after))

        val (updated, result) = StudyDocEngine.apply(
            doc,
            StudyOp.DeleteBlock(citation.id),
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(before.id, after.id), updated.blocks.map { it.id })
    }

    @Test
    fun deleting_the_only_citation_leaves_an_empty_paragraph() {
        val citation = verse()
        val doc = StudyDoc.empty().copy(blocks = listOf(citation))

        val (updated, result) = StudyDocEngine.apply(
            doc,
            StudyOp.DeleteBlock(citation.id),
        )

        assertTrue(result.isSuccess)
        assertEquals(1, updated.blocks.size)
        assertTrue(updated.blocks.single() is StudyBlock.Paragraph)
        assertTrue(updated.blocks.single().plainText().isEmpty())
    }

    private fun verse() = StudyBlock.Verse(
        id = BlockId("business-verse"),
        bookId = "Juan",
        chapter = 3,
        verseStart = 16,
        sourceVersion = "rvr1960",
        contents = mapOf("rvr1960" to "16 Porque de tal manera amo Dios..."),
    )
}

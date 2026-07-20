package com.cristiancogollo.biblion.feature.studydocs.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTemplateTest {

    @Test
    fun expositorySermon_hasCorrectStructure() {
        val template = StudyTemplate.ExpositorySermon
        val blocks = template.generateInitialBlocks()
        val metadata = template.generateMetadata()

        assertEquals("expository_sermon", template.id)
        assertEquals("Predicación Expositiva", template.name)
        assertTrue(blocks.isNotEmpty())
        assertEquals(15, blocks.size)
        assertTrue(metadata.tags.contains("predicacion"))
    }

    @Test
    fun devotional_hasCorrectStructure() {
        val template = StudyTemplate.Devotional
        val blocks = template.generateInitialBlocks()

        assertEquals("devotional", template.id)
        assertEquals("Devocional", template.name)
        assertTrue(blocks.isNotEmpty())
        assertEquals(8, blocks.size)
    }

    @Test
    fun bibleStudy_hasCorrectStructure() {
        val template = StudyTemplate.BibleStudy
        val blocks = template.generateInitialBlocks()

        assertEquals("bible_study", template.id)
        assertEquals("Estudio Bíblico", template.name)
        assertTrue(blocks.isNotEmpty())
        assertEquals(12, blocks.size)
    }

    @Test
    fun bibleClass_hasCorrectStructure() {
        val template = StudyTemplate.BibleClass
        val blocks = template.generateInitialBlocks()

        assertEquals("bible_class", template.id)
        assertEquals("Clase Bíblica", template.name)
        assertTrue(blocks.isNotEmpty())
        assertEquals(15, blocks.size)
    }

    @Test
    fun blank_hasSingleParagraph() {
        val template = StudyTemplate.Blank
        val blocks = template.generateInitialBlocks()

        assertEquals("blank", template.id)
        assertEquals("Documento en Blanco", template.name)
        assertEquals(1, blocks.size)
    }

    @Test
    fun getAll_returnsAllFiveTemplates() {
        val templates = StudyTemplate.getAll()
        assertEquals(5, templates.size)
    }

    @Test
    fun getById_findsCorrectTemplate() {
        val found = StudyTemplate.getById("expository_sermon")
        assertTrue(found is StudyTemplate.ExpositorySermon)
    }

    @Test
    fun getById_returnsNullForInvalid() {
        val found = StudyTemplate.getById("non_existent")
        assertEquals(null, found)
    }

    @Test
    fun expositorySermon_includesVerseBlock() {
        val blocks = StudyTemplate.ExpositorySermon.generateInitialBlocks()
        val verseBlock = blocks.find { it is com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.Verse }
        assertTrue(verseBlock != null)
    }

    @Test
    fun expositorySermon_includesReflectionBlock() {
        val blocks = StudyTemplate.ExpositorySermon.generateInitialBlocks()
        val reflectionBlock = blocks.find { it is com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.Reflection }
        assertTrue(reflectionBlock != null)
    }
}

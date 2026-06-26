package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.bibi.model.RelatedVerse
import com.cristiancogollo.biblion.feature.bibi.model.TopicInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests para el DTO RelatedVerse.
 * Verifica que el DTO publico NO expone el campo votes / qualityScore.
 * Esto es parte del constraint "Bibi nunca menciona la votacion".
 */
class RelatedVerseTest {

    @Test
    fun `RelatedVerse no tiene campo votes`() {
        val verse = RelatedVerse(
            reference = "Juan 1:1",
            book = "Juan",
            chapter = 1,
            verseStart = 1,
            verseEnd = 1,
            text = "En el principio era el Verbo..."
        )
        // Verificamos por reflexion que no existe campo "votes" ni "qualityScore"
        val fields = verse.javaClass.declaredFields.map { it.name }
        assertFalse("RelatedVerse no debe tener campo votes: $fields", fields.contains("votes"))
        assertFalse("RelatedVerse no debe tener campo qualityScore: $fields", fields.contains("qualityScore"))
        assertFalse("RelatedVerse no debe tener campo score: $fields", fields.contains("score"))
    }

    @Test
    fun `RelatedVerse se construye correctamente`() {
        val verse = RelatedVerse(
            reference = "Genesis 1:1-3",
            book = "Genesis",
            chapter = 1,
            verseStart = 1,
            verseEnd = 3,
            text = "En el principio..."
        )
        assertEquals("Genesis 1:1-3", verse.reference)
        assertEquals("Genesis", verse.book)
        assertEquals(1, verse.chapter)
        assertEquals(1, verse.verseStart)
        assertEquals(3, verse.verseEnd)
    }
}

/**
 * Tests para el DTO TopicInfo.
 * qualityScore es interno - el caller decide si mostrarlo.
 */
class TopicInfoTest {

    @Test
    fun `TopicInfo expone qualityScore pero es decision del caller mostrarlo`() {
        val topic = TopicInfo(
            key = "love",
            displayEs = "amor",
            displayEn = "love",
            qualityScore = 50
        )
        // displayLabel devuelve el displayEs (el caller decide si agregar el score)
        assertEquals("amor", topic.displayLabel)
        assertEquals(50, topic.qualityScore)  // accesible para el caller
    }

    @Test
    fun `TopicInfo displayLabel usa displayEs incluso si es el mismo que EN`() {
        val topic = TopicInfo(
            key = "unknown_topic",
            displayEs = "unknown topic",  // fallback a EN
            displayEn = "unknown topic",
            qualityScore = 5
        )
        assertEquals("unknown topic", topic.displayLabel)
    }
}

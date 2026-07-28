package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.bibi.engine.VerseResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerseResolverTest {

    @Test
    fun `referencia escrita tiene prioridad sobre versiculo seleccionado`() {
        val reader = VerseResolver.ReaderSnapshot(
            bookName = "Genesis",
            chapter = 1,
            selectedVerses = setOf(3)
        )
        val question = "versiculos relacionados con Juan 3:16"  // forma 2 posible

        val resolution = VerseResolver.resolve(reader, question)

        assertEquals(VerseResolver.Source.EXPLICIT_IN_QUESTION, resolution.source)
        assertEquals("Juan", resolution.book)
        assertEquals(3, resolution.chapter)
        assertEquals(16, resolution.verse)
        assertEquals(VerseResolver.Confidence.HIGH, resolution.confidence)
    }

    @Test
    fun `forma 1 - sin seleccion cae a forma 2 (versiculo en texto)`() {
        val reader = VerseResolver.ReaderSnapshot(
            bookName = "Genesis",
            chapter = 1,
            selectedVerses = emptySet()
        )
        val question = "que versiculos se relacionan con Juan 3:16"

        val resolution = VerseResolver.resolve(reader, question)

        assertEquals(VerseResolver.Source.EXPLICIT_IN_QUESTION, resolution.source)
        assertEquals("Juan", resolution.book)
        assertEquals(3, resolution.chapter)
        assertEquals(16, resolution.verse)
    }

    @Test
    fun `forma 1 - multiples versiculos seleccionados toma el primero`() {
        val reader = VerseResolver.ReaderSnapshot(
            bookName = "Mateo",
            chapter = 5,
            selectedVerses = setOf(3, 4, 5, 9, 10)
        )
        val resolution = VerseResolver.resolve(reader, "")

        assertEquals(VerseResolver.Source.SELECTION, resolution.source)
        assertEquals(3, resolution.verse)  // minOrNull = 3
    }

    @Test
    fun `forma 2 - formato natural Libro Cap Ver`() {
        val reader = VerseResolver.ReaderSnapshot(bookName = null, chapter = 0, selectedVerses = emptySet())
        val resolution = VerseResolver.resolve(reader, "explicame Romanos 8:28")

        assertEquals(VerseResolver.Source.EXPLICIT_IN_QUESTION, resolution.source)
        assertEquals("Romanos", resolution.book)
        assertEquals(8, resolution.chapter)
        assertEquals(28, resolution.verse)
    }

    @Test
    fun `forma 2 - formato OSIS Book Chap Ver`() {
        val reader = VerseResolver.ReaderSnapshot(bookName = null, chapter = 0, selectedVerses = emptySet())
        val resolution = VerseResolver.resolve(reader, "pasajes relacionados con Gen.1.1")

        assertEquals(VerseResolver.Source.EXPLICIT_IN_QUESTION, resolution.source)
        assertEquals("Genesis", resolution.book)
        assertEquals(1, resolution.chapter)
        assertEquals(1, resolution.verse)
    }

    @Test
    fun `forma 2 - libro con prefijo numerico 1 Juan`() {
        val reader = VerseResolver.ReaderSnapshot(bookName = null, chapter = 0, selectedVerses = emptySet())
        val resolution = VerseResolver.resolve(reader, "que dice 1 John 4:8")

        assertEquals(VerseResolver.Source.EXPLICIT_IN_QUESTION, resolution.source)
        assertEquals("1 Juan", resolution.book)
        assertEquals(4, resolution.chapter)
        assertEquals(8, resolution.verse)
    }

    @Test
    fun `abreviatura espanola resuelve libro fuera del lector`() {
        val reader = VerseResolver.ReaderSnapshot("Genesis", 1, emptySet())

        val resolution = VerseResolver.resolve(reader, "explicame Jn 3:16")

        assertEquals("Juan", resolution.book)
        assertEquals(16, resolution.verse)
    }

    @Test
    fun `rango conserva inicio y final`() {
        val reader = VerseResolver.ReaderSnapshot("Genesis", 1, emptySet())

        val resolution = VerseResolver.resolve(reader, "explica 1 Corintios 13:4-7")

        assertEquals("1 Corintios", resolution.book)
        assertEquals(4, resolution.verse)
        assertEquals(7, resolution.verseEnd)
    }

    @Test
    fun `ordinal natural resuelve libro numerado`() {
        val reader = VerseResolver.ReaderSnapshot(null, 0, emptySet())

        val resolution = VerseResolver.resolve(reader, "explica Primera de Corintios 13:4")

        assertEquals("1 Corintios", resolution.book)
        assertEquals(13, resolution.chapter)
        assertEquals(4, resolution.verse)
    }

    @Test
    fun `forma 2 - con acentos se reconoce Genesis`() {
        val reader = VerseResolver.ReaderSnapshot(bookName = null, chapter = 0, selectedVerses = emptySet())
        val resolution = VerseResolver.resolve(reader, "explicame Genesis 1:1")

        assertEquals(VerseResolver.Source.EXPLICIT_IN_QUESTION, resolution.source)
        assertEquals("Genesis", resolution.book)
    }

    @Test
    fun `fallback - sin seleccion ni texto, solo capitulo`() {
        val reader = VerseResolver.ReaderSnapshot(
            bookName = "Salmos",
            chapter = 23,
            selectedVerses = emptySet()
        )
        val resolution = VerseResolver.resolve(reader, "que dice este capitulo")

        assertEquals(VerseResolver.Source.CHAPTER_ONLY, resolution.source)
        assertEquals("Salmos", resolution.book)
        assertEquals(23, resolution.chapter)
        assertNull(resolution.verse)
    }

    @Test
    fun `fallback - sin contexto retorna NONE`() {
        val reader = VerseResolver.ReaderSnapshot(bookName = null, chapter = 0, selectedVerses = emptySet())
        val resolution = VerseResolver.resolve(reader, "hola")

        assertEquals(VerseResolver.Source.NONE, resolution.source)
        assertEquals("", resolution.book)
        assertEquals(0, resolution.chapter)
    }

    @Test
    fun `forma 2 - texto que no tiene referencia biblica`() {
        val reader = VerseResolver.ReaderSnapshot(bookName = null, chapter = 0, selectedVerses = emptySet())
        val resolution = VerseResolver.resolve(reader, "necesito ayuda con algo")

        assertEquals(VerseResolver.Source.NONE, resolution.source)
    }

    @Test
    fun `forma 2 - libro no reconocido cae a NONE`() {
        val reader = VerseResolver.ReaderSnapshot(bookName = null, chapter = 0, selectedVerses = emptySet())
        val resolution = VerseResolver.resolve(reader, "explicame LibroInexistente 1:1")

        // Sin libro en el reader y referencia no reconocida: NONE
        assertEquals(VerseResolver.Source.NONE, resolution.source)
    }
}

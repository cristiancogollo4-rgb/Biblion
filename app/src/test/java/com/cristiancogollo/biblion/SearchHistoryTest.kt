package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.search.data.SearchHistoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SearchHistoryTest {

    @Test
    fun `normalizeQuery lowercase`() {
        assertEquals("amor", SearchHistoryRepository.normalizeQuery("AMOR"))
        assertEquals("amor", SearchHistoryRepository.normalizeQuery("Amor"))
    }

    @Test
    fun `normalizeQuery sin acentos`() {
        assertEquals("perdon", SearchHistoryRepository.normalizeQuery("perdón"))
        assertEquals("jesus", SearchHistoryRepository.normalizeQuery("Jesús"))
        assertEquals("gracia", SearchHistoryRepository.normalizeQuery("Gracia"))
    }

    @Test
    fun `normalizeQuery trim espacios`() {
        assertEquals("amor", SearchHistoryRepository.normalizeQuery("  amor  "))
        assertEquals("amor", SearchHistoryRepository.normalizeQuery("amor "))
    }

    @Test
    fun `normalizeQuery eñe a ne`() {
        assertEquals("cana", SearchHistoryRepository.normalizeQuery("caña"))
        assertEquals("nino", SearchHistoryRepository.normalizeQuery("niño"))
    }

    @Test
    fun `normalizeQuery frases compuestas`() {
        assertEquals("amor de dios", SearchHistoryRepository.normalizeQuery("Amor de Dios"))
        assertEquals("amor de dios", SearchHistoryRepository.normalizeQuery("amor de dios"))
    }

    @Test
    fun `normalizeQuery misma frase misma normalizacion`() {
        val a = SearchHistoryRepository.normalizeQuery("Juan 3:16")
        val b = SearchHistoryRepository.normalizeQuery("juan 3:16")
        val c = SearchHistoryRepository.normalizeQuery("JUAN 3:16")
        assertEquals(a, b)
        assertEquals(b, c)
    }

    @Test
    fun `normalizeQuery diferente frase diferente normalizacion`() {
        val a = SearchHistoryRepository.normalizeQuery("amor")
        val b = SearchHistoryRepository.normalizeQuery("gracia")
        assertNotEquals(a, b)
    }

    @Test
    fun `normalizeQuery mantiene caracteres no alfabeticos`() {
        // Símbolos como ?, !, :, ñ no se normalizan
        assertEquals("juan 3:16", SearchHistoryRepository.normalizeQuery("Juan 3:16"))
        assertEquals("1 reyes 1:1", SearchHistoryRepository.normalizeQuery("1 Reyes 1:1"))
    }
}

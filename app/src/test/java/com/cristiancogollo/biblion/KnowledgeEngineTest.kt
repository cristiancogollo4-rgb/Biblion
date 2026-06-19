package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class KnowledgeEngineTest {

    @Test
    fun `hola es GREETING`() {
        assertEquals(KnowledgeEngine.BibiIntent.GREETING, KnowledgeEngine.detectIntent("hola"))
    }

    @Test
    fun `buenas es GREETING`() {
        assertEquals(KnowledgeEngine.BibiIntent.GREETING, KnowledgeEngine.detectIntent("buenas"))
    }

    @Test
    fun `quien fue Abraham es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("¿quién fue Abraham?"))
    }

    @Test
    fun `quien es Moises es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("¿quién es Moisés?"))
    }

    @Test
    fun `quien era David es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("¿quién era David?"))
    }

    @Test
    fun `cuentame de Pablo es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("cuéntame de Pablo"))
    }

    @Test
    fun `hablame de Pedro es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("háblame de Pedro"))
    }

    @Test
    fun `dime sobre Juan es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("dime sobre Juan"))
    }

    @Test
    fun `informacion de Rut es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("información de Rut"))
    }

    @Test
    fun `donde queda Jerusalen es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("¿dónde queda Jerusalén?"))
    }

    @Test
    fun `donde esta Galilea es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("¿dónde está Galilea?"))
    }

    @Test
    fun `donde nacio Jesus es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("¿dónde nació Jesús?"))
    }

    @Test
    fun `ubicacion de Egipto es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("ubicación de Egipto"))
    }

    @Test
    fun `que significa gracia es DEFINE`() {
        assertEquals(KnowledgeEngine.BibiIntent.DEFINE, KnowledgeEngine.detectIntent("¿qué significa gracia?"))
    }

    @Test
    fun `define pacto es DEFINE`() {
        assertEquals(KnowledgeEngine.BibiIntent.DEFINE, KnowledgeEngine.detectIntent("define pacto"))
    }

    @Test
    fun `que es la fe es DEFINE`() {
        assertEquals(KnowledgeEngine.BibiIntent.DEFINE, KnowledgeEngine.detectIntent("¿qué es la fe?"))
    }

    @Test
    fun `explicame este versiculo es EXPLAIN_VERSE`() {
        assertEquals(KnowledgeEngine.BibiIntent.EXPLAIN_VERSE, KnowledgeEngine.detectIntent("explícame este versículo"))
    }

    @Test
    fun `que dice este pasaje es EXPLAIN_VERSE`() {
        assertEquals(KnowledgeEngine.BibiIntent.EXPLAIN_VERSE, KnowledgeEngine.detectIntent("explícame este pasaje"))
    }

    @Test
    fun `explica Romanos 8 es EXPLAIN_VERSE`() {
        assertEquals(KnowledgeEngine.BibiIntent.EXPLAIN_VERSE, KnowledgeEngine.detectIntent("explica este texto"))
    }

    @Test
    fun `relacionados es RELATED`() {
        assertEquals(KnowledgeEngine.BibiIntent.RELATED, KnowledgeEngine.detectIntent("pasajes relacionados"))
    }

    @Test
    fun `pasajes similares es RELATED`() {
        assertEquals(KnowledgeEngine.BibiIntent.RELATED, KnowledgeEngine.detectIntent("versículos relacionados"))
    }

    @Test
    fun `hebreo es ORIGINAL_LANG`() {
        assertEquals(KnowledgeEngine.BibiIntent.ORIGINAL_LANG, KnowledgeEngine.detectIntent("¿qué dice en hebreo?"))
    }

    @Test
    fun `griego es ORIGINAL_LANG`() {
        assertEquals(KnowledgeEngine.BibiIntent.ORIGINAL_LANG, KnowledgeEngine.detectIntent("¿qué significa en griego?"))
    }

    @Test
    fun `Strong H1254 es ORIGINAL_LANG`() {
        assertEquals(KnowledgeEngine.BibiIntent.ORIGINAL_LANG, KnowledgeEngine.detectIntent("H1254"))
    }

    @Test
    fun `profundiza es DIVE_DEEPER`() {
        val lastQueries = listOf("¿qué significa gracia?")
        assertEquals(KnowledgeEngine.BibiIntent.DIVE_DEEPER, KnowledgeEngine.detectIntent("profundiza", lastQueries))
    }

    @Test
    fun `explica mas es DIVE_DEEPER`() {
        val lastQueries = listOf("¿quién fue Abraham?")
        assertEquals(KnowledgeEngine.BibiIntent.DIVE_DEEPER, KnowledgeEngine.detectIntent("explica más", lastQueries))
    }

    @Test
    fun `explicame eso es DIVE_DEEPER`() {
        val lastQueries = listOf("¿qué significa pacto?")
        assertEquals(KnowledgeEngine.BibiIntent.DIVE_DEEPER, KnowledgeEngine.detectIntent("explícame eso", lastQueries))
    }

    @Test
    fun `cuentame mas con historial es DIVE_DEEPER`() {
        val lastQueries = listOf("¿qué significa pacto?")
        assertEquals(KnowledgeEngine.BibiIntent.DIVE_DEEPER, KnowledgeEngine.detectIntent("cuéntame más", lastQueries))
    }

    @Test
    fun `texto aleatorio es FALLBACK`() {
        assertEquals(KnowledgeEngine.BibiIntent.FALLBACK, KnowledgeEngine.detectIntent("el cielo está nublado"))
    }

    @Test
    fun `pregunta no biblica es FALLBACK`() {
        assertEquals(KnowledgeEngine.BibiIntent.FALLBACK, KnowledgeEngine.detectIntent("¿cómo programar en Kotlin?"))
    }

    @Test
    fun `dime que es es DEFINE`() {
        assertEquals(KnowledgeEngine.BibiIntent.DEFINE, KnowledgeEngine.detectIntent("dime qué es la gracia"))
    }

    @Test
    fun `donde queda con acento es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("¿dónde queda Galilea?"))
    }

    @Test
    fun `donde nacio con acento es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("¿dónde nació Jesús?"))
    }

    @Test
    fun `quien fue con acento es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("¿quién fue Moisés?"))
    }

    @Test
    fun `donde esta con acento es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("¿dónde está Jerusalén?"))
    }

    @Test
    fun `quien fue Sara es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("¿quién fue Sara?"))
    }

    @Test
    fun `y que mas con historial es DIVE_DEEPER`() {
        val history = listOf("¿qué significa gracia?")
        assertEquals(
            KnowledgeEngine.BibiIntent.DIVE_DEEPER,
            KnowledgeEngine.detectIntent("y qué más", history)
        )
    }

    @Test
    fun `despues con historial es DIVE_DEEPER`() {
        val history = listOf("¿quién fue Abraham?")
        assertEquals(
            KnowledgeEngine.BibiIntent.DIVE_DEEPER,
            KnowledgeEngine.detectIntent("después", history)
        )
    }

    @Test
    fun `explica mas con historial es DIVE_DEEPER`() {
        val history = listOf("¿qué es el pacto?")
        assertEquals(
            KnowledgeEngine.BibiIntent.DIVE_DEEPER,
            KnowledgeEngine.detectIntent("explica más", history)
        )
    }
}

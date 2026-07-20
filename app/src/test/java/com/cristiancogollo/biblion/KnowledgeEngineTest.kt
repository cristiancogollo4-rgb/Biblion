package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.bibi.engine.KnowledgeEngine
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

    @Test
    fun `temas de Genesis es TOPICS`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("temas de Genesis 1:1"))
    }

    @Test
    fun `de que temas habla es TOPICS`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("de qué temas habla este versículo"))
    }

    @Test
    fun `que temas habla es TOPICS`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("qué temas habla Juan 3:16"))
    }

    @Test
    fun `temas del versiculo es TOPICS`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("temas del versículo"))
    }

    @Test
    fun `TOPICS tiene prioridad menor que DIVE_DEEPER`() {
        assertEquals(KnowledgeEngine.BibiIntent.DIVE_DEEPER, KnowledgeEngine.detectIntent("profundiza en temas"))
    }

    @Test
    fun `TOPICS tiene prioridad menor que RELATED`() {
        assertEquals(KnowledgeEngine.BibiIntent.RELATED, KnowledgeEngine.detectIntent("temas relacionados con Juan 3:16"))
    }

    @Test
    fun `TOPICS tiene prioridad menor que GREETING`() {
        assertEquals(KnowledgeEngine.BibiIntent.GREETING, KnowledgeEngine.detectIntent("hola, temas de Genesis"))
    }

    @Test
    fun `TOPICS tiene prioridad menor que WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("quien fue Abraham, temas de la Biblia"))
    }

    @Test
    fun `TOPICS tiene prioridad menor que ORIGINAL_LANG`() {
        assertEquals(KnowledgeEngine.BibiIntent.ORIGINAL_LANG, KnowledgeEngine.detectIntent("hebreo, temas de Génesis"))
    }

    @Test
    fun `TOPICS sin acentos funciona`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("temas de genesis 1:1"))
    }

    @Test
    fun `TOPICS con mayusculas funciona`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("TEMAS DE GENESIS 1:1"))
    }

    @Test
    fun `TOPICS con tilde en que funciona`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("de qué temas habla"))
    }

    @Test
    fun `tema singular de que tema habla es TOPICS`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("de que tema habla 1samuel 1:1?"))
    }

    @Test
    fun `tema singular tema de genesis es TOPICS`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("tema de genesis 1:1"))
    }

    @Test
    fun `de que trata es TOPICS`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("¿de qué trata este capítulo?"))
    }

    @Test
    fun `que temas toca es TOPICS`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("¿qué temas toca Romanos 8?"))
    }

    @Test
    fun `cuales son los temas es TOPICS`() {
        assertEquals(KnowledgeEngine.BibiIntent.TOPICS, KnowledgeEngine.detectIntent("¿cuáles son los temas de Génesis 1?"))
    }

    @Test
    fun `explícame genesis 1 1 es EXPLAIN_VERSE`() {
        assertEquals(KnowledgeEngine.BibiIntent.EXPLAIN_VERSE, KnowledgeEngine.detectIntent("explícame genesis 1:1"))
    }

    @Test
    fun `explica mateo 5 3 es EXPLAIN_VERSE`() {
        assertEquals(KnowledgeEngine.BibiIntent.EXPLAIN_VERSE, KnowledgeEngine.detectIntent("explica mateo 5:3"))
    }

    @Test
    fun `dime que es juan 3 16 es EXPLAIN_VERSE`() {
        assertEquals(KnowledgeEngine.BibiIntent.EXPLAIN_VERSE, KnowledgeEngine.detectIntent("dime qué es juan 3:16"))
    }

    @Test
    fun `explicame este capitulo es EXPLAIN_VERSE`() {
        assertEquals(KnowledgeEngine.BibiIntent.EXPLAIN_VERSE, KnowledgeEngine.detectIntent("explícame este capítulo"))
    }

    @Test
    fun `buenas tardes es GREETING`() {
        assertEquals(KnowledgeEngine.BibiIntent.GREETING, KnowledgeEngine.detectIntent("buenas tardes"))
    }

    @Test
    fun `buenas noches es GREETING`() {
        assertEquals(KnowledgeEngine.BibiIntent.GREETING, KnowledgeEngine.detectIntent("buenas noches"))
    }

    @Test
    fun `que tal es GREETING`() {
        assertEquals(KnowledgeEngine.BibiIntent.GREETING, KnowledgeEngine.detectIntent("qué tal"))
    }

    @Test
    fun `hello es GREETING`() {
        assertEquals(KnowledgeEngine.BibiIntent.GREETING, KnowledgeEngine.detectIntent("hello"))
    }

    @Test
    fun `cuéntame sobre es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("cuéntame sobre Pablo"))
    }

    @Test
    fun `háblame sobre es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("háblame sobre Pedro"))
    }

    @Test
    fun `historia de es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("historia de David"))
    }

    @Test
    fun `dime quién fue es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("dime quién fue Moisés"))
    }

    @Test
    fun `sabes quién es WHO`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHO, KnowledgeEngine.detectIntent("¿sabes quién fue Abraham?"))
    }

    @Test
    fun `donde quedaba es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("¿dónde quedaba Babilonia?"))
    }

    @Test
    fun `en qué lugar es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("¿en qué lugar quedaba Jerusalén?"))
    }

    @Test
    fun `se encuentra en es WHERE`() {
        assertEquals(KnowledgeEngine.BibiIntent.WHERE, KnowledgeEngine.detectIntent("¿se encuentra en Galilea?"))
    }

    @Test
    fun `pasajes parecidos es RELATED`() {
        assertEquals(KnowledgeEngine.BibiIntent.RELATED, KnowledgeEngine.detectIntent("pasajes parecidos a Juan 3:16"))
    }

    @Test
    fun `paralelos es RELATED`() {
        assertEquals(KnowledgeEngine.BibiIntent.RELATED, KnowledgeEngine.detectIntent("paralelos de Mateo 5:3"))
    }

    @Test
    fun `comparar con es RELATED`() {
        assertEquals(KnowledgeEngine.BibiIntent.RELATED, KnowledgeEngine.detectIntent("comparar con Romanos 8:28"))
    }

    @Test
    fun `cuál es la definición es DEFINE`() {
        assertEquals(KnowledgeEngine.BibiIntent.DEFINE, KnowledgeEngine.detectIntent("¿cuál es la definición de pacto?"))
    }

    @Test
    fun `qué quiere decir es DEFINE`() {
        assertEquals(KnowledgeEngine.BibiIntent.DEFINE, KnowledgeEngine.detectIntent("¿qué quiere decir gracia?"))
    }

    @Test
    fun `qué representa es DEFINE`() {
        assertEquals(KnowledgeEngine.BibiIntent.DEFINE, KnowledgeEngine.detectIntent("¿qué representa el bautismo?"))
    }

    @Test
    fun `traducción literal es ORIGINAL_LANG`() {
        assertEquals(KnowledgeEngine.BibiIntent.ORIGINAL_LANG, KnowledgeEngine.detectIntent("¿cómo es en traducción literal?"))
    }

    @Test
    fun `raíz de la palabra es ORIGINAL_LANG`() {
        assertEquals(KnowledgeEngine.BibiIntent.ORIGINAL_LANG, KnowledgeEngine.detectIntent("raíz de la palabra amor"))
    }
}

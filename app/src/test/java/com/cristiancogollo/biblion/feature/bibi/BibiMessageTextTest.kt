package com.cristiancogollo.biblion.feature.bibi

import com.cristiancogollo.biblion.feature.bibi.model.BibiMessageText
import org.junit.Assert.assertEquals
import org.junit.Test

class BibiMessageTextTest {

    @Test
    fun `normalize extracts answer from fenced nested json`() {
        val raw = """
            ```json
            {"answer":"Idea central\n\n- Dios ama primero","confidence":"high"}
            ```
        """.trimIndent()

        assertEquals(
            "Idea central\n\n- Dios ama primero",
            BibiMessageText.normalize(raw),
        )
    }

    @Test
    fun `normalize converts escaped line breaks from malformed response`() {
        val raw = "Contexto\\n\\n1. Primer punto\\n2. Segundo punto"

        assertEquals(
            "Contexto\n\n1. Primer punto\n2. Segundo punto",
            BibiMessageText.normalize(raw),
        )
    }

    @Test
    fun `normalize removes excessive blank lines and trailing spaces`() {
        val raw = "Primero  \n\n\n\nSegundo   "

        assertEquals("Primero\n\nSegundo", BibiMessageText.normalize(raw))
    }
}

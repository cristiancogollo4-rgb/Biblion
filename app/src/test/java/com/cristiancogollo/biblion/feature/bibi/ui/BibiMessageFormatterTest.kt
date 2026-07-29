package com.cristiancogollo.biblion.feature.bibi.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BibiMessageFormatterTest {

    @Test
    fun `parse preserves paragraphs as readable blocks`() {
        val blocks = BibiMessageFormatter.parse(
            "Dios muestra su amor.\nEsta línea continúa la idea.\n\n" +
                "La respuesta del creyente es amar.",
        )

        assertEquals(2, blocks.size)
        assertEquals(BibiMessageBlockKind.PARAGRAPH, blocks[0].kind)
        assertEquals(
            "Dios muestra su amor. Esta línea continúa la idea.",
            blocks[0].text,
        )
        assertEquals(BibiMessageBlockKind.PARAGRAPH, blocks[1].kind)
    }

    @Test
    fun `parse recognizes headings lists numbers quotes and sections`() {
        val blocks = BibiMessageFormatter.parse(
            """
            ## Contexto
            Puntos principales:
            - Gracia
            * Fe
            1. Leer el pasaje
            2) Observar el contexto
            > La aplicación nace del texto.
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                BibiMessageBlockKind.HEADING,
                BibiMessageBlockKind.SECTION,
                BibiMessageBlockKind.BULLET,
                BibiMessageBlockKind.BULLET,
                BibiMessageBlockKind.NUMBERED,
                BibiMessageBlockKind.NUMBERED,
                BibiMessageBlockKind.QUOTE,
            ),
            blocks.map { it.kind },
        )
        assertEquals("1.", blocks[4].marker)
        assertEquals("2)", blocks[5].marker)
    }

    @Test
    fun `parse handles escaped newlines from historical messages`() {
        val blocks = BibiMessageFormatter.parse(
            "Aplicación:\\n- Ora con humildad\\n- Sirve con amor",
        )

        assertEquals(
            listOf(
                BibiMessageBlockKind.SECTION,
                BibiMessageBlockKind.BULLET,
                BibiMessageBlockKind.BULLET,
            ),
            blocks.map { it.kind },
        )
    }

    @Test
    fun `parse divides a long ai paragraph without changing its words`() {
        val original = listOf(
            "Madián fue hijo de Abraham y Cetura, mencionado en Génesis 25:2.",
            "Su descendencia formó un pueblo relacionado con la historia de Israel.",
            "Moisés vivió en esa región durante una etapa importante de su vida.",
            "Este contexto ayuda a comprender mejor los encuentros posteriores.",
            "La información debe leerse junto al pasaje bíblico completo.",
        ).joinToString(" ")

        val blocks = BibiMessageFormatter.parse(original)

        assertEquals(2, blocks.size)
        assertEquals(
            original,
            blocks.joinToString(" ") { it.text },
        )
    }
}

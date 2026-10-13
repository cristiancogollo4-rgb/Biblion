package com.cristiancogollo.biblion.feature.bibi.domain

import com.cristiancogollo.biblion.feature.bibi.domain.DoctrinalPerspectivePolicy.Perspective
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoctrinalPerspectivePolicyTest {
    @Test
    fun `a leading question does not establish the user's faith`() {
        val context = DoctrinalPerspectivePolicy.analyze("¿Es falsa la Trinidad?", emptyList())

        assertTrue(context.isDebatedQuestion)
        assertNull(context.perspective)
        assertTrue(context.fallbackAnswer().contains("No asumiré una postura"))
        assertTrue(DoctrinalPerspectivePolicy.analyze(
            "¿Jesús es el Padre?", emptyList(),
        ).isDebatedQuestion)
        assertTrue(DoctrinalPerspectivePolicy.analyze(
            "¿Hay tres personas en Dios?", emptyList(),
        ).isDebatedQuestion)
    }

    @Test
    fun `an explicit position applies only to the active conversation`() {
        val history = listOf(ChatExchange("Soy de la unicidad", "Entiendo"))

        assertEquals(
            Perspective.ONENESS,
            DoctrinalPerspectivePolicy.analyze("¿Y qué dice Juan 10:30?", history).perspective,
        )
        assertNull(DoctrinalPerspectivePolicy.analyze("¿Qué es la Trinidad?", emptyList()).perspective)
    }

    @Test
    fun `the latest explicit request overrides an earlier perspective`() {
        val history = listOf(ChatExchange("Soy trinitario", "Entiendo"))

        assertEquals(
            Perspective.ONENESS,
            DoctrinalPerspectivePolicy.analyze("Ahora explica desde la unicidad", history).perspective,
        )
    }

    @Test
    fun `negative belief and answer text do not create a perspective`() {
        val history = listOf(ChatExchange("¿Qué es la Trinidad?", "Soy de la unicidad"))

        assertNull(
            DoctrinalPerspectivePolicy.analyze("No creo en la Trinidad", history).perspective,
        )
    }

    @Test
    fun `a later withdrawal clears an earlier position`() {
        val history = listOf(
            ChatExchange("Soy trinitario", "Entiendo"),
            ChatExchange("Ya no soy trinitario", "Entiendo"),
        )

        assertNull(DoctrinalPerspectivePolicy.analyze("¿Y ahora cómo se interpreta?", history).perspective)
        assertNull(DoctrinalPerspectivePolicy.analyze(
            "No creo en la Trinidad", history.take(1),
        ).perspective)
    }

    @Test
    fun `the position lasts throughout one chat but does not enter a new chat`() {
        val messages = listOf("Soy trinitario") + List(15) { "Explícame Juan 1:$it" }
        val perspective = DoctrinalPerspectivePolicy.perspectiveFromUserMessages(messages)

        assertEquals(Perspective.TRINITARIAN, perspective)
        assertEquals(
            perspective,
            DoctrinalPerspectivePolicy.analyze("¿Qué dice la Trinidad?", emptyList(), perspective)
                .perspective,
        )
        assertNull(DoctrinalPerspectivePolicy.perspectiveFromUserMessages(emptyList()))
        assertNull(DoctrinalPerspectivePolicy.perspectiveFromUserMessages(
            messages + "Ya no soy trinitario",
        ))
        assertNull(DoctrinalPerspectivePolicy.perspectiveFromUserMessages(
            listOf("Soy de la unicidad", "Ya no soy de la unicidad"),
        ))
        assertEquals(
            Perspective.TRINITARIAN,
            DoctrinalPerspectivePolicy.perspectiveFromUserMessages(
                listOf("Soy de la unicidad", "No soy de la unicidad, soy trinitario"),
            ),
        )
    }

    @Test
    fun `obvious dismissals of another person's faith are rejected`() {
        assertTrue(DoctrinalPerspectivePolicy.containsUnsafeClaim("La Trinidad es falsa"))
        assertTrue(DoctrinalPerspectivePolicy.containsUnsafeClaim("Los unicitarios no son cristianos"))
        assertTrue(DoctrinalPerspectivePolicy.containsUnsafeClaim("Dios no es uno"))
        assertFalse(DoctrinalPerspectivePolicy.containsUnsafeClaim(
            "Según la tradición trinitaria, Dios es uno en tres personas.",
        ))
    }

    @Test
    fun `an already attributed worker answer is not prefixed twice`() {
        val answer = "Desde la perspectiva de la unicidad que expresaste:\n\nJuan 10:30 habla de unidad."

        assertEquals(
            answer,
            DoctrinalPerspectivePolicy.attributeAnswer(answer, Perspective.ONENESS),
        )
    }
}

package com.cristiancogollo.biblion.feature.bibi

import com.cristiancogollo.biblion.feature.bibi.domain.BibiReferenceValidator
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.feature.bibi.model.formatBibiPassageSelection
import com.cristiancogollo.biblion.feature.bibi.ui.WorkerClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BibiReaderSafeguardsTest {
    @Test
    fun `empty selection has no reference label`() {
        assertEquals("", formatBibiPassageSelection(emptyList()))
    }

    @Test
    fun `nonconsecutive selection is not displayed as a continuous range`() {
        val passages = listOf(1, 5, 3, 4).map {
            BibiPassage("Números", 35, it, "Texto")
        }

        assertEquals("Números 35:1, 3-5", formatBibiPassageSelection(passages))
    }

    @Test
    fun `AI references must exist in the installed version`() = runBlocking {
        val references = listOf(
            WorkerClient.WorkerReference("Juan 3:16"),
            WorkerClient.WorkerReference("Juan 3:99"),
            WorkerClient.WorkerReference("Libro inventado 1:1"),
        )

        val validated = BibiReferenceValidator.validate(references, "rv1960") { book, chapter, version ->
            assertEquals("Juan", book)
            assertEquals(3, chapter)
            assertEquals("rv1960", version)
            setOf(16, 17)
        }

        assertEquals(listOf("Juan 3:16"), validated.map { it.reference })
    }

    @Test
    fun `AI ranges require every verse to exist`() = runBlocking {
        val validated = BibiReferenceValidator.validate(
            listOf(
                WorkerClient.WorkerReference("Juan 3:16-18"),
                WorkerClient.WorkerReference("Juan 3:16-17"),
            ),
            "nvi",
        ) { _, _, _ -> setOf(16, 17) }

        assertEquals(listOf("Juan 3:16-17"), validated.map { it.reference })
    }
}

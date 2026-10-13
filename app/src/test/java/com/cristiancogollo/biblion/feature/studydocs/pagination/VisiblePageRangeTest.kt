package com.cristiancogollo.biblion.feature.studydocs.pagination

import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.visiblePageRange
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.anchoredScrollOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class VisiblePageRangeTest {
    @Test
    fun `sin paginas el rango esta vacio`() {
        assertEquals(IntRange.EMPTY, visiblePageRange(0, 800, 1000f, 20f, 24f, 0))
    }

    @Test
    fun `incluye paginas cercanas sin montar todo el documento`() {
        assertEquals(
            1..3,
            visiblePageRange(2100, 800, 1000f, 20f, 24f, 10, overscanPages = 1),
        )
    }

    @Test
    fun `limita el rango al final del documento`() {
        assertEquals(
            9..9,
            visiblePageRange(20_000, 800, 1000f, 20f, 24f, 10, overscanPages = 1),
        )
    }

    @Test
    fun `zoom conserva el punto de lectura vertical`() {
        assertEquals(
            1700,
            anchoredScrollOffset(1000, 800, 0.5f, 1.5f, 5000),
        )
        assertEquals(
            500,
            anchoredScrollOffset(1000, 800, 0.5f, 1.5f, 500),
        )
    }
}

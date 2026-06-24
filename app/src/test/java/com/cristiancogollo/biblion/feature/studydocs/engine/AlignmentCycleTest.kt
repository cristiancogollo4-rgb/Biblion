package com.cristiancogollo.biblion.feature.studydocs.engine

import androidx.compose.ui.text.style.TextAlign
import org.junit.Assert.assertEquals
import org.junit.Test

class AlignmentCycleTest {

    @Test fun `null cycles to Start`() {
        assertEquals(TextAlign.Start, AlignmentCycle.next(null))
    }

    @Test fun `Start cycles to Center`() {
        assertEquals(TextAlign.Center, AlignmentCycle.next(TextAlign.Start))
    }

    @Test fun `Center cycles to End`() {
        assertEquals(TextAlign.End, AlignmentCycle.next(TextAlign.Center))
    }

    @Test fun `End cycles to Justify`() {
        assertEquals(TextAlign.Justify, AlignmentCycle.next(TextAlign.End))
    }

    @Test fun `Right cycles to Justify`() {
        assertEquals(TextAlign.Justify, AlignmentCycle.next(TextAlign.Right))
    }

    @Test fun `Justify cycles to Start`() {
        assertEquals(TextAlign.Start, AlignmentCycle.next(TextAlign.Justify))
    }

    @Test fun `Left cycles to Center`() {
        assertEquals(TextAlign.Center, AlignmentCycle.next(TextAlign.Left))
    }

    @Test fun `full cycle returns to Start after 5 iterations`() {
        var current: TextAlign? = null
        // null -> Start -> Center -> End -> Justify -> Start
        repeat(5) { current = AlignmentCycle.next(current) }
        assertEquals(TextAlign.Start, current)
    }

    @Test fun `displayName for null is Sin alineacion`() {
        assertEquals("Sin alineacion", AlignmentCycle.displayName(null))
    }

    @Test fun `displayName for Start is Izquierda`() {
        assertEquals("Izquierda", AlignmentCycle.displayName(TextAlign.Start))
    }

    @Test fun `displayName for Center is Centro`() {
        assertEquals("Centro", AlignmentCycle.displayName(TextAlign.Center))
    }

    @Test fun `displayName for End is Derecha`() {
        assertEquals("Derecha", AlignmentCycle.displayName(TextAlign.End))
    }

    @Test fun `displayName for Justify is Justificado`() {
        assertEquals("Justificado", AlignmentCycle.displayName(TextAlign.Justify))
    }
}

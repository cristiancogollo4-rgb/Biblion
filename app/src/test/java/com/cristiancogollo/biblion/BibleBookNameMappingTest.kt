package com.cristiancogollo.biblion

import org.junit.Assert.assertEquals
import org.junit.Test

class BibleBookNameMappingTest {
    @Test
    fun ntvUsesItsOwnNamesForCantaresAndHechos() {
        assertEquals(
            "cantardeloscantares",
            BibleRepository.normalizedBookNameForVersion("Cantares", "ntv"),
        )
        assertEquals(
            "hechosdelosapostoles",
            BibleRepository.normalizedBookNameForVersion("Hechos", "NTV"),
        )
    }

    @Test
    fun otherVersionsAndNumberedBooksUseCanonicalNames() {
        assertEquals(
            "cantares",
            BibleRepository.normalizedBookNameForVersion("Cantar de los Cantares", "rv1960"),
        )
        assertEquals(
            "hechos",
            BibleRepository.normalizedBookNameForVersion("Hechos de los Apóstoles", "nvi"),
        )
        assertEquals(
            "1timoteo",
            BibleRepository.normalizedBookNameForVersion("1 Timoteo", "dhh"),
        )
    }
}

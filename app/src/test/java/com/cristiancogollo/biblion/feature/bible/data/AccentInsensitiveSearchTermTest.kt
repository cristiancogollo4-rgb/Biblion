package com.cristiancogollo.biblion.feature.bible.data

import com.cristiancogollo.biblion.toAccentInsensitiveLikeTerm

import org.junit.Assert.assertEquals
import org.junit.Test

class AccentInsensitiveSearchTermTest {
    @Test
    fun `accents are removed so perdon and perdón search the same`() {
        assertEquals("perdon", toAccentInsensitiveLikeTerm("perdón"))
        assertEquals("perdon", toAccentInsensitiveLikeTerm("  perdon "))
        assertEquals("nino", toAccentInsensitiveLikeTerm("niño"))
    }

    @Test
    fun `like wildcards typed by the user are escaped`() {
        assertEquals("""50\% de\_algo""", toAccentInsensitiveLikeTerm("50% de_algo"))
        assertEquals("""a\\b""", toAccentInsensitiveLikeTerm("""a\b"""))
    }
}

package com.cristiancogollo.biblion.feature.search

import com.cristiancogollo.biblion.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenDictionaryEntryRouteTest {

    @Test fun `DictionaryEntry route uses entryId arg name`() {
        assertTrue(
            "La ruta debe contener el arg 'entryId'",
            Screen.DictionaryEntry.route.contains("{entryId}"),
        )
        assertEquals("entryId", Screen.DictionaryEntry.ARG_ENTRY_ID)
    }

    @Test fun `DictionaryEntry createRoute generates correct URL`() {
        val route = Screen.DictionaryEntry.createRoute(entryId = 42L)
        assertEquals("dictionary/entry/42", route)
    }

    @Test fun `DictionaryEntry createRoute supports large ids`() {
        val route = Screen.DictionaryEntry.createRoute(entryId = 999_999L)
        assertEquals("dictionary/entry/999999", route)
    }
}

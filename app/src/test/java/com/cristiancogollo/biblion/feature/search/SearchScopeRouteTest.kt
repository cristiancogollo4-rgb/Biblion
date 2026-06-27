package com.cristiancogollo.biblion.feature.search

import com.cristiancogollo.biblion.feature.search.model.SearchScope
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchScopeRouteTest {

    @Test fun `fromRouteArg null returns BIBLE`() {
        assertEquals(SearchScope.BIBLE, SearchScope.fromRouteArg(null))
    }

    @Test fun `fromRouteArg empty returns BIBLE`() {
        assertEquals(SearchScope.BIBLE, SearchScope.fromRouteArg(""))
    }

    @Test fun `fromRouteArg bible returns BIBLE`() {
        assertEquals(SearchScope.BIBLE, SearchScope.fromRouteArg("bible"))
    }

    @Test fun `fromRouteArg BIBLE uppercase returns BIBLE`() {
        assertEquals(SearchScope.BIBLE, SearchScope.fromRouteArg("BIBLE"))
    }

    @Test fun `fromRouteArg dictionary returns DICTIONARY`() {
        assertEquals(SearchScope.DICTIONARY, SearchScope.fromRouteArg("dictionary"))
    }

    @Test fun `fromRouteArg DICTIONARY uppercase returns DICTIONARY`() {
        assertEquals(SearchScope.DICTIONARY, SearchScope.fromRouteArg("DICTIONARY"))
    }

    @Test fun `fromRouteArg unknown value returns BIBLE as default`() {
        assertEquals(SearchScope.BIBLE, SearchScope.fromRouteArg("other"))
        assertEquals(SearchScope.BIBLE, SearchScope.fromRouteArg("xyz"))
    }

    @Test fun `BIBLE has bible label resource`() {
        // Solo verifica que la referencia exista; no podemos leer el string en unit test JVM.
        assertEquals(com.cristiancogollo.biblion.R.string.search_scope_bible, SearchScope.BIBLE.labelRes)
    }

    @Test fun `DICTIONARY has dictionary label resource`() {
        assertEquals(com.cristiancogollo.biblion.R.string.search_scope_dictionary, SearchScope.DICTIONARY.labelRes)
    }

    @Test fun `enum has exactly 2 values`() {
        assertEquals(2, SearchScope.entries.size)
    }
}

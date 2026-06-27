package com.cristiancogollo.biblion.feature.search

import com.cristiancogollo.biblion.Screen
import com.cristiancogollo.biblion.feature.search.model.SearchScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenSearchRouteTest {

    @Test fun `Screen Search base route contains search and scope placeholder`() {
        assertTrue(
            "Screen.Search.route debe contener 'search' y 'scope'",
            Screen.Search.route.contains("search") && Screen.Search.route.contains("scope"),
        )
    }

    @Test fun `createRoute without scope uses BIBLE by default`() {
        val route = Screen.Search.createRoute()
        assertEquals("search?scope=bible", route)
    }

    @Test fun `createRoute with BIBLE scope is bible`() {
        val route = Screen.Search.createRoute(SearchScope.BIBLE)
        assertEquals("search?scope=bible", route)
    }

    @Test fun `createRoute with DICTIONARY scope is dictionary`() {
        val route = Screen.Search.createRoute(SearchScope.DICTIONARY)
        assertEquals("search?scope=dictionary", route)
    }
}

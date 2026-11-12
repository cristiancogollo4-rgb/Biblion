package com.cristiancogollo.biblion.feature.studydocs.ui.read

import org.junit.Assert.assertEquals
import org.junit.Test

class StudyReadBibleLayoutTest {

    @Test
    fun tabletLandscapeSplitsSideBySide() {
        assertEquals(
            StudyReadBibleLayout.SIDE_BY_SIDE,
            studyReadBibleLayout(widthDp = 1280, heightDp = 752, smallestWidthDp = 800),
        )
    }

    @Test
    fun tabletPortraitStacksBibleAboveDocument() {
        assertEquals(
            StudyReadBibleLayout.STACKED,
            studyReadBibleLayout(widthDp = 800, heightDp = 1232, smallestWidthDp = 800),
        )
    }

    @Test
    fun phonePortraitUsesBottomSheet() {
        assertEquals(
            StudyReadBibleLayout.BOTTOM_SHEET,
            studyReadBibleLayout(widthDp = 411, heightDp = 891, smallestWidthDp = 411),
        )
    }

    @Test
    fun phoneLandscapeSplitsSideBySide() {
        assertEquals(
            StudyReadBibleLayout.SIDE_BY_SIDE,
            studyReadBibleLayout(widthDp = 891, heightDp = 411, smallestWidthDp = 411),
        )
    }
}

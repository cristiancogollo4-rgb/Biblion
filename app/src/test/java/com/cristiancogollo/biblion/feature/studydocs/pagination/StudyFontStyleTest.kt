package com.cristiancogollo.biblion.feature.studydocs.pagination

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyFontStyleTest {

    @Test
    fun headingUsesSavedSizeAndFamilyDuringPagination() {
        val heading = StudyBlock.Heading(fontSize = 27, fontFamily = "monospace")
        val style = PaginationEngine.textStyleFor(heading, Density(1f))

        assertEquals(27.sp, style.fontSize)
        assertEquals(FontFamily.Monospace, style.fontFamily)
    }

    @Test
    fun bibleCitationKeepsItsProtectedSerifFamily() {
        val citation = StudyBlock.Verse(fontFamily = "monospace")
        assertEquals(FontFamily.Serif, PaginationEngine.textStyleFor(citation, Density(1f)).fontFamily)
    }
}

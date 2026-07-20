package com.cristiancogollo.biblion.feature.studydocs.ui.bibi

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Rule
import org.junit.Test

class BibiOverlayPanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val emptyDoc = StudyDoc(
        blocks = listOf(StudyBlock.Paragraph(text = StyledText.plain("Contenido de prueba"))),
    )

    @Test
    fun bibiOverlay_showsHeader_whenVisible() {
        composeRule.setContent {
            BibiOverlayPanel(
                isVisible = true,
                onDismiss = {},
                onInsertAsBlock = {},
                docContext = emptyDoc,
            )
        }
        composeRule.onNodeWithText("Bibi").assertIsDisplayed()
        composeRule.onNodeWithText("Asistente bíblico").assertIsDisplayed()
    }

    @Test
    fun bibiOverlay_showsInputPlaceholder() {
        composeRule.setContent {
            BibiOverlayPanel(
                isVisible = true,
                onDismiss = {},
                onInsertAsBlock = {},
                docContext = emptyDoc,
            )
        }
        composeRule.onNodeWithText("Pregúntale a Bibi...").assertIsDisplayed()
    }
}

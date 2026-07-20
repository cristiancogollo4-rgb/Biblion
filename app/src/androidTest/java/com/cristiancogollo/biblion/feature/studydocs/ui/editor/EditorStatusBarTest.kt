package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class EditorStatusBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun statusBar_showsWordCount() {
        composeRule.setContent {
            EditorStatusBar(
                wordCount = 150,
                charCount = 800,
                blockCount = 12,
                currentPage = 0,
                totalPages = 3,
            )
        }
        composeRule.onNodeWithText("150").assertIsDisplayed()
    }

    @Test
    fun statusBar_showsCharCount() {
        composeRule.setContent {
            EditorStatusBar(
                wordCount = 150,
                charCount = 800,
                blockCount = 12,
                currentPage = 0,
                totalPages = 3,
            )
        }
        composeRule.onNodeWithText("800").assertIsDisplayed()
    }

    @Test
    fun statusBar_showsBlockCount() {
        composeRule.setContent {
            EditorStatusBar(
                wordCount = 150,
                charCount = 800,
                blockCount = 12,
                currentPage = 0,
                totalPages = 3,
            )
        }
        composeRule.onNodeWithText("12").assertIsDisplayed()
    }

    @Test
    fun statusBar_showsLabels() {
        composeRule.setContent {
            EditorStatusBar(
                wordCount = 150,
                charCount = 800,
                blockCount = 12,
                currentPage = 0,
                totalPages = 1,
            )
        }
        composeRule.onNodeWithText("Palabras:").assertIsDisplayed()
        composeRule.onNodeWithText("Caracteres:").assertIsDisplayed()
        composeRule.onNodeWithText("Bloques:").assertIsDisplayed()
    }

    @Test
    fun statusBar_showsPageInfoWhenMultiplePages() {
        composeRule.setContent {
            EditorStatusBar(
                wordCount = 150,
                charCount = 800,
                blockCount = 12,
                currentPage = 1,
                totalPages = 5,
            )
        }
        composeRule.onNodeWithText("2 / 5").assertIsDisplayed()
    }

    @Test
    fun statusBar_updatesDynamically() {
        composeRule.setContent {
            EditorStatusBar(
                wordCount = 999,
                charCount = 5000,
                blockCount = 25,
                currentPage = 0,
                totalPages = 10,
            )
        }
        composeRule.onNodeWithText("999").assertIsDisplayed()
        composeRule.onNodeWithText("5000").assertIsDisplayed()
        composeRule.onNodeWithText("25").assertIsDisplayed()
    }
}

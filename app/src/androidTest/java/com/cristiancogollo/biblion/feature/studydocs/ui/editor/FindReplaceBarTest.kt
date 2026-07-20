package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class FindReplaceBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun findReplaceBar_showsTitle() {
        composeRule.setContent {
            FindReplaceBar(
                query = "",
                onQueryChange = {},
                replacement = "",
                onReplacementChange = {},
                matchCount = 0,
                currentMatchIndex = 0,
                onFindNext = {},
                onFindPrevious = {},
                onReplace = {},
                onReplaceAll = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("Buscar y reemplazar").assertIsDisplayed()
    }

    @Test
    fun findReplaceBar_showsZeroResultsInitially() {
        composeRule.setContent {
            FindReplaceBar(
                query = "",
                onQueryChange = {},
                replacement = "",
                onReplacementChange = {},
                matchCount = 0,
                currentMatchIndex = 0,
                onFindNext = {},
                onFindPrevious = {},
                onReplace = {},
                onReplaceAll = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("0 resultados").assertIsDisplayed()
    }

    @Test
    fun findReplaceBar_showsMatchCount() {
        composeRule.setContent {
            FindReplaceBar(
                query = "Dios",
                onQueryChange = {},
                replacement = "",
                onReplacementChange = {},
                matchCount = 5,
                currentMatchIndex = 2,
                onFindNext = {},
                onFindPrevious = {},
                onReplace = {},
                onReplaceAll = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("3 / 5").assertIsDisplayed()
    }

    @Test
    fun findReplaceBar_replaceButton_disabledWhenNoMatches() {
        composeRule.setContent {
            FindReplaceBar(
                query = "xyz",
                onQueryChange = {},
                replacement = "abc",
                onReplacementChange = {},
                matchCount = 0,
                currentMatchIndex = 0,
                onFindNext = {},
                onFindPrevious = {},
                onReplace = {},
                onReplaceAll = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("Reemplazar").assertIsNotEnabled()
    }

    @Test
    fun findReplaceBar_replaceButton_enabledWhenMatchesFound() {
        composeRule.setContent {
            FindReplaceBar(
                query = "Dios",
                onQueryChange = {},
                replacement = "Señor",
                onReplacementChange = {},
                matchCount = 3,
                currentMatchIndex = 0,
                onFindNext = {},
                onFindPrevious = {},
                onReplace = {},
                onReplaceAll = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("Reemplazar").assertIsEnabled()
    }

    @Test
    fun findReplaceBar_queryInput_acceptsText() {
        composeRule.setContent {
            FindReplaceBar(
                query = "",
                onQueryChange = {},
                replacement = "",
                onReplacementChange = {},
                matchCount = 0,
                currentMatchIndex = 0,
                onFindNext = {},
                onFindPrevious = {},
                onReplace = {},
                onReplaceAll = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("Buscar").performTextClearance()
        composeRule.onNodeWithText("Buscar").performTextInput("Dios")
    }

    @Test
    fun findReplaceBar_replaceAllButton_disabledWhenNoMatches() {
        composeRule.setContent {
            FindReplaceBar(
                query = "",
                onQueryChange = {},
                replacement = "",
                onReplacementChange = {},
                matchCount = 0,
                currentMatchIndex = 0,
                onFindNext = {},
                onFindPrevious = {},
                onReplace = {},
                onReplaceAll = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("Todo").assertIsNotEnabled()
    }
}

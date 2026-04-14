package com.cristiancogollo.biblion

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

class HomeScreenVersionSwitchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rapidVersionChange_doesNotLeaveOldDailyVerseVisible() {
        composeRule.setContent {
            val context = LocalContext.current
            var selectedVersion by remember { mutableStateOf("v1") }
            var displayedReference by remember { mutableStateOf("loading") }

            Column {
                Text(displayedReference, modifier = Modifier.testTag("daily_reference"))
            }

            DailyVerseLoaderEffect(
                context = context,
                selectedVersionKey = selectedVersion,
                availableVersions = emptyList(),
                onAvailableVersionsLoaded = {},
                onDailyVerseLoaded = { verse, _ -> displayedReference = verse.reference },
                loadAvailableVersions = { emptyList() },
                loadDailyVerse = { _, versionKey ->
                    if (versionKey == "v1") {
                        delay(250)
                    } else {
                        delay(10)
                    }
                    DailyVerse(text = "texto-$versionKey", reference = "ref-$versionKey")
                }
            )

            LaunchedEffect(Unit) {
                delay(25)
                selectedVersion = "v2"
            }
        }

        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule
                .onAllNodes(hasText("ref-v2"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("daily_reference").assertTextEquals("ref-v2")
        composeRule.onNodeWithText("ref-v1").assertDoesNotExist()
    }
}

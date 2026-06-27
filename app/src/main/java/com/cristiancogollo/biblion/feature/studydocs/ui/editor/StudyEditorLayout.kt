package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun StudyEditorLayout(
    editorContent: @Composable (
        isExpandable: Boolean,
        isExpanded: Boolean,
        onToggleExpand: () -> Unit,
        isMultiColumnEnabled: Boolean,
        onToggleMultiColumn: () -> Unit,
    ) -> Unit,
    sideContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600

    if (isTablet) {
        var isExpanded by remember { mutableStateOf(false) }
        var isMultiColumnEnabled by remember { mutableStateOf(false) }

        if (isExpanded) {
            editorContent(
                true,
                true,
                { isExpanded = false; isMultiColumnEnabled = false },
                isMultiColumnEnabled,
                { isMultiColumnEnabled = !isMultiColumnEnabled },
            )
        } else {
            Row(modifier = modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    sideContent()
                }
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                )
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    editorContent(
                        true,
                        false,
                        { isExpanded = true },
                        false,
                        {},
                    )
                }
            }
        }
    } else {
        editorContent(false, false, {}, false, {})
    }
}

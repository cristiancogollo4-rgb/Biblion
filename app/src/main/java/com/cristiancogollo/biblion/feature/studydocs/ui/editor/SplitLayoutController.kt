package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.zIndex

/**
 * Controlador de layout para modo estudio split-screen.
 *
 * Distribución fija 50/50.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitLayoutController(
    leftPane: @Composable BoxScope.() -> Unit,
    rightPane: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // Panel izquierdo 50%
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
        ) {
            leftPane()
        }

        // Panel derecho 50%
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .imePadding()
        ) {
            rightPane()
        }
    }
}

enum class CompactStudyPane {
    Bible,
    Document,
}

fun shouldUseCompactStudyLayout(widthDp: Int, heightDp: Int): Boolean =
    widthDp < 840 || heightDp < 480

/**
 * Phone landscape layout. Both panes stay composed so switching does not reset
 * editor focus/state or the nested Bible navigation.
 */
@Composable
fun CompactStudyLayoutController(
    activePane: CompactStudyPane,
    onPaneSelected: (CompactStudyPane) -> Unit,
    biblePane: @Composable BoxScope.() -> Unit,
    documentPane: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .zIndex(2f),
            header = {
                NavigationRailItem(
                    selected = activePane == CompactStudyPane.Bible,
                    onClick = { onPaneSelected(CompactStudyPane.Bible) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Abrir Biblia",
                        )
                    },
                )
                NavigationRailItem(
                    selected = activePane == CompactStudyPane.Document,
                    onClick = { onPaneSelected(CompactStudyPane.Document) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Abrir documento",
                        )
                    },
                )
            },
        ) {}

        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .zIndex(2f),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds(),
        ) {
            CompactPaneLayer(
                active = activePane == CompactStudyPane.Bible,
                content = biblePane,
            )
            CompactPaneLayer(
                active = activePane == CompactStudyPane.Document,
                content = documentPane,
            )
        }
    }
}

@Composable
private fun BoxScope.CompactPaneLayer(
    active: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (active) 1f else 0f)
            .zIndex(if (active) 1f else 0f)
            .then(if (active) Modifier else Modifier.clearAndSetSemantics { }),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

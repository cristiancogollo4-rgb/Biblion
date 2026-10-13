package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cristiancogollo.biblion.LocalPrimaryNavigationVisible
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft

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
    if (LocalConfiguration.current.screenWidthDp < 600) {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                CompactPaneTab(
                    title = stringResource(R.string.study_pane_bible),
                    selected = activePane == CompactStudyPane.Bible,
                    onClick = { onPaneSelected(CompactStudyPane.Bible) },
                    modifier = Modifier.weight(1f),
                )
                CompactPaneTab(
                    title = stringResource(R.string.study_pane_document),
                    selected = activePane == CompactStudyPane.Document,
                    onClick = { onPaneSelected(CompactStudyPane.Document) },
                    modifier = Modifier.weight(1f),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
            ) {
                CompactPaneLayers(activePane, biblePane, documentPane)
            }
        }
        return
    }

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
            CompactPaneLayers(activePane, biblePane, documentPane)
        }
    }
}

@Composable
private fun CompactPaneTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) BiblionGoldSoft.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun BoxScope.CompactPaneLayers(
    activePane: CompactStudyPane,
    biblePane: @Composable BoxScope.() -> Unit,
    documentPane: @Composable BoxScope.() -> Unit,
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
    ) {
        // A Popup no le afecta el alpha del panel oculto: su navegación debe desaparecer.
        CompositionLocalProvider(LocalPrimaryNavigationVisible provides active) {
            content()
        }
    }
}

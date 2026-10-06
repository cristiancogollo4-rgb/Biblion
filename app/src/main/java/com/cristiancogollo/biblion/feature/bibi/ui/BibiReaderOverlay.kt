package com.cristiancogollo.biblion.feature.bibi.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.GuidedTutorialTargets
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion
import com.cristiancogollo.biblion.guidedTutorialTarget
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary

@Composable
fun BibiReaderOverlay(
    bookName: String?,
    chapter: Int,
    passages: List<BibiPassage>,
    hasExplicitSelection: Boolean = true,
    bibleVersion: String,
    currentUserName: String?,
    tutorialTargetBounds: MutableMap<String, Rect>,
    onGuidedTutorialTargetAction: (String) -> Unit = {},
    onTutorialEvent: (String) -> Unit = {},
    forceOpenForTutorial: Boolean = false,
    openRequest: Int = 0,
    requestedContext: BibiContext.Reader? = null,
    onOpenPassage: (BibiPassage) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    var activeReaderContext by remember { mutableStateOf<BibiContext.Reader?>(null) }
    var handledOpenRequest by rememberSaveable { mutableIntStateOf(openRequest) }
    val greeting = if (currentUserName.isNullOrBlank()) {
        stringResource(R.string.bibi_greeting_no_name)
    } else {
        stringResource(R.string.bibi_greeting, currentUserName)
    }
    val readerContext = BibiContext.Reader(
        book = bookName.orEmpty(),
        chapter = chapter,
        passages = passages,
        bibleVersion = bibleVersion,
        hasExplicitSelection = hasExplicitSelection,
    )

    LaunchedEffect(forceOpenForTutorial) {
        if (forceOpenForTutorial) {
            activeReaderContext = readerContext
            isOpen = true
        }
    }
    LaunchedEffect(openRequest) {
        if (openRequest > handledOpenRequest && requestedContext != null) {
            activeReaderContext = requestedContext
            isOpen = true
        }
        handledOpenRequest = openRequest
    }

    if (isOpen) {
        BibiFloatingWindow(onClose = {
            isOpen = false
            activeReaderContext = null
        }) {
            BibiChatPanel(
                viewModelKey = "bibi-reader",
                mode = "reader",
                greeting = greeting,
                initialSuggestions = listOf(
                    BibiSuggestion("¿Quién fue Moisés?", "¿quién fue Moisés?"),
                    BibiSuggestion("¿Dónde queda Jerusalén?", "¿dónde queda Jerusalén?"),
                    BibiSuggestion("¿Qué significa pacto?", "¿qué significa pacto?"),
                ),
                bibiContext = activeReaderContext ?: readerContext,
                currentUserName = currentUserName,
                subtitle = "Asistente bíblico · Solo consulta",
                placeholder = "Pregunta a Bibi...",
                onClose = {
                    isOpen = false
                    activeReaderContext = null
                },
                onCompletedExchange = { onTutorialEvent("chat_exchange_completed") },
                isTutorial = forceOpenForTutorial,
                onOpenPassage = {
                    isOpen = false
                    activeReaderContext = null
                    onOpenPassage(it)
                },
                modifier = modifier.guidedTutorialTarget(
                    GuidedTutorialTargets.READER_BIBI_CHAT_PANEL,
                    tutorialTargetBounds,
                ),
            )
        }
    } else {
        BibiReaderLauncher(
            tutorialTargetBounds = tutorialTargetBounds,
            onClick = {
                activeReaderContext = readerContext
                isOpen = true
                onGuidedTutorialTargetAction(GuidedTutorialTargets.READER_BIBI_BUTTON)
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun BibiReaderLauncher(
    tutorialTargetBounds: MutableMap<String, Rect>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = BiblionBluePrimary,
        contentColor = Color.White,
        modifier = modifier
            .padding(end = 20.dp, bottom = 128.dp)
            .size(56.dp)
            .guidedTutorialTarget(
                GuidedTutorialTargets.READER_BIBI_BUTTON,
                tutorialTargetBounds,
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.bibi_logo),
            contentDescription = stringResource(R.string.cd_bibi),
        )
    }
}

package com.cristiancogollo.biblion.feature.bibi.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    bibleVersion: String,
    currentUserName: String?,
    tutorialTargetBounds: MutableMap<String, Rect>,
    onGuidedTutorialTargetAction: (String) -> Unit = {},
    onTutorialEvent: (String) -> Unit = {},
    forceOpenForTutorial: Boolean = false,
    onOpenPassage: (BibiPassage) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isOpen by remember { mutableStateOf(false) }
    val greeting = remember(currentUserName) {
        if (currentUserName.isNullOrBlank()) {
            context.getString(R.string.bibi_greeting_no_name)
        } else {
            context.getString(R.string.bibi_greeting, currentUserName)
        }
    }
    val readerContext = BibiContext.Reader(
        book = bookName.orEmpty(),
        chapter = chapter,
        passages = passages,
        bibleVersion = bibleVersion,
    )

    LaunchedEffect(forceOpenForTutorial) {
        if (forceOpenForTutorial) isOpen = true
    }

    if (isOpen) {
        BibiFloatingWindow(onClose = { isOpen = false }) {
            BibiChatPanel(
                viewModelKey = "bibi-reader",
                mode = "reader",
                greeting = greeting,
                initialSuggestions = listOf(
                    BibiSuggestion("¿Quién fue Moisés?", "¿quién fue Moisés?"),
                    BibiSuggestion("¿Dónde queda Jerusalén?", "¿dónde queda Jerusalén?"),
                    BibiSuggestion("¿Qué significa pacto?", "¿qué significa pacto?"),
                ),
                bibiContext = readerContext,
                currentUserName = currentUserName,
                subtitle = "Asistente bíblico · Solo consulta",
                placeholder = "Pregunta a Bibi...",
                onClose = { isOpen = false },
                onCompletedExchange = { onTutorialEvent("chat_exchange_completed") },
                isTutorial = forceOpenForTutorial,
                onOpenPassage = {
                    isOpen = false
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

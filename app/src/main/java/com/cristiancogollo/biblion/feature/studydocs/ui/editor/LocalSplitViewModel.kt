package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.runtime.compositionLocalOf
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel

/**
 * CompositionLocal para compartir el StudyDocSplitViewModel entre paneles.
 *
 * Uso:
 * ```
 * CompositionLocalProvider(LocalSplitViewModel provides splitViewModel) {
 *     StudyModeNavigation(...)
 * }
 * ```
 */
val LocalSplitViewModel = compositionLocalOf<StudyDocSplitViewModel?> { null }

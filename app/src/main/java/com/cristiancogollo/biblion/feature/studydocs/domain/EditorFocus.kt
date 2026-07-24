package com.cristiancogollo.biblion.feature.studydocs.domain

/**
 * One-shot request to move the real text editor focus to a logical editor.
 *
 * The sequence is intentionally separate from [EditorTextKey]: the same
 * editor can be requested again after a structural edit without changing its
 * block id or list item index.
 */
data class EditorFocusRequest(
    val target: EditorTextKey,
    val sequence: Long,
)

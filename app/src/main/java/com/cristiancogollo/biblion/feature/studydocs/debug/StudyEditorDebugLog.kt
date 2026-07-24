package com.cristiancogollo.biblion.feature.studydocs.debug

import android.os.SystemClock
import android.util.Log
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import java.util.concurrent.atomic.AtomicLong

/** Diagnostic trace for transient editor/pagination mismatches. */
object StudyEditorDebugLog {
    private const val TAG = "BIBLION_STUDY_ENTER"
    private val sequence = AtomicLong(0L)

    fun log(stage: String, message: String) {
        val id = sequence.incrementAndGet()
        Log.d(TAG, "#$id t=${SystemClock.uptimeMillis()} $stage | $message")
    }

    fun textPreview(text: String): String = text
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .take(48)

    fun blocksSummary(blocks: List<StudyBlock>): String = blocks.joinToString(
        separator = ";",
        prefix = "[",
        postfix = "]",
    ) { block ->
        "${block.id.value}:${block::class.simpleName}:${block.plainText().length}"
    }
}

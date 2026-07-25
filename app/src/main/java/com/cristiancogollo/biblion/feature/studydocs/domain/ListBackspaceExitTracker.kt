package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId

internal class ListBackspaceExitTracker(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private var pending: PendingExit? = null

    fun arm(blockId: BlockId) {
        pending = PendingExit(blockId, nowMillis())
    }

    fun consume(blockId: BlockId): Boolean {
        val candidate = pending ?: return false
        pending = null
        return candidate.blockId == blockId &&
            nowMillis() - candidate.armedAtMillis <= timeoutMillis
    }

    fun clear() {
        pending = null
    }

    private data class PendingExit(
        val blockId: BlockId,
        val armedAtMillis: Long,
    )

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 1_200L
    }
}

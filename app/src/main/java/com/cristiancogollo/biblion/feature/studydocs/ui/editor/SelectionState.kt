package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId

class SelectionState {

    private val selections: SnapshotStateMap<String, IntRange?> = mutableStateMapOf()

    fun get(blockId: BlockId): IntRange? = selections[blockId.value]
    fun get(blockId: String): IntRange? = selections[blockId]

    fun toMap(): Map<String, IntRange> = selections.entries
        .mapNotNull { (k, v) -> v?.let { k to it } }
        .toMap()

    fun set(blockId: BlockId, range: IntRange?) {
        if (range == null) selections.remove(blockId.value)
        else selections[blockId.value] = range
    }

    fun set(blockId: String, range: IntRange?) {
        if (range == null) selections.remove(blockId)
        else selections[blockId] = range
    }

    fun clear(blockId: BlockId) { selections.remove(blockId.value) }
    fun clear(blockId: String) { selections.remove(blockId) }
    fun clearAll() { selections.clear() }

    fun effectiveRange(blockId: BlockId, textLength: Int): IntRange? {
        val sel = selections[blockId.value] ?: return null
        val start = sel.first.coerceIn(0, textLength)
        val end = sel.last.coerceIn(0, textLength)
        if (start >= end) return null
        return start until end
    }
}

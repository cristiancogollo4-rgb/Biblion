package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.domain.EditCommand
import com.cristiancogollo.biblion.feature.studydocs.domain.PersistableEditCommand
import com.cristiancogollo.biblion.feature.studydocs.domain.PersistableEditCommandSerializer
import com.cristiancogollo.biblion.feature.studydocs.domain.toPersistable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DocEditHistoryRepository(
    private val dao: DocEditHistoryDao,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun append(docRemoteId: String, command: EditCommand): Int? {
        val persistable = command.toPersistable() ?: return null
        val payload = json.encodeToString(PersistableEditCommandSerializer, persistable)
        return dao.appendAndTrim(
            docRemoteId = docRemoteId,
            commandType = commandTypeOf(persistable),
            payloadJson = payload,
            description = command.description,
            maxEntries = maxEntries,
            createdAt = clock(),
        )
    }

    suspend fun load(docRemoteId: String): List<EditCommand> {
        val rows = dao.getByDoc(docRemoteId)
        return rows.mapNotNull { row ->
            runCatching {
                val payload = json.decodeFromString(PersistableEditCommandSerializer, row.payloadJson)
                payload.toCommand()
            }.getOrNull()
        }
    }

    fun observe(docRemoteId: String): Flow<List<HistoryEntry>> =
        dao.observeByDoc(docRemoteId).map { rows ->
            rows.map { row ->
                HistoryEntry(
                    seq = row.seq,
                    description = row.description,
                    commandType = row.commandType,
                    createdAt = row.createdAt,
                )
            }
        }

    suspend fun clear(docRemoteId: String) = dao.clearForDoc(docRemoteId)

    private fun commandTypeOf(persistable: PersistableEditCommand): String = when (persistable) {
        is PersistableEditCommand.Insert -> "Insert"
        is PersistableEditCommand.Delete -> "Delete"
        is PersistableEditCommand.DeleteWithSnapshot -> "DeleteWithSnapshot"
        is PersistableEditCommand.UpdateTitle -> "UpdateTitle"
        is PersistableEditCommand.Move -> "Move"
    }

    data class HistoryEntry(
        val seq: Int,
        val description: String,
        val commandType: String,
        val createdAt: Long,
    )

    companion object {
        const val DEFAULT_MAX_ENTRIES = 100
    }
}

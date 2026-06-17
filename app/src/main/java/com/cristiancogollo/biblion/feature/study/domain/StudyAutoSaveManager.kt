package com.cristiancogollo.biblion

import android.util.Log
import com.cristiancogollo.biblion.StudyDao
import com.cristiancogollo.biblion.StudyEntity
import com.cristiancogollo.biblion.StudyBlockNode
import com.cristiancogollo.biblion.LinkedCitationEntity
import com.cristiancogollo.biblion.SerializedStudyDocument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Gestor de persistencia automática optimizado.
 * Reemplaza la lógica de autosave dispersa en StudyViewModel.
 * Usa firma incremental en lugar de serialización JSON completa.
 */
class StudyAutoSaveManager(
    private val dao: StudyDao,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "StudyAutoSaveManager"
    }

    private var lastSavedSignature: String? = null
    private val persistMutex = Mutex()

    /**
     * Construye una firma incremental del estado actual.
     * Usa hashes en lugar de serialización JSON completa para mejor rendimiento.
     */
    private fun buildIncrementalSignature(state: StudyUiState): String {
        val studyId = state.selectedStudyId ?: return ""
        val blocksHash = state.blocks.hashCode()
        val titleHash = state.title.hashCode()
        val tagsHash = state.tags.hashCode()
        val versionHash = state.globalVersion.hashCode()
        return "$studyId|$titleHash|$blocksHash|$tagsHash|$versionHash"
    }

    /**
     * Verifica si el estado ha cambiado desde el último guardado.
     */
    fun hasChanged(state: StudyUiState): Boolean {
        val currentSignature = buildIncrementalSignature(state)
        return currentSignature != lastSavedSignature
    }

    /**
     * Persiste el estudio actual si ha cambiado.
     * @return true si se guardó correctamente, false si no era necesario o falló.
     */
    suspend fun autoSaveIfNeeded(state: StudyUiState): Boolean {
        if (!hasChanged(state)) return false
        return persistStudy(state)
    }

    /**
     * Fuerza el guardado independientemente de si hubo cambios.
     */
    suspend fun forceSave(state: StudyUiState): Boolean {
        return persistStudy(state)
    }

    /**
     * Persiste el estudio en la base de datos.
     * Maneja tanto inserciones como actualizaciones de forma atómica.
     */
    private suspend fun persistStudy(state: StudyUiState): Boolean {
        return persistMutex.withLock {
            withContext(ioDispatcher) {
            try {
                val studyId = state.selectedStudyId

                val document = SerializedStudyDocument(
                    blocks = state.blocks,
                    globalVersion = state.globalVersion,
                    tags = state.tags
                )
                val contentJson = json.encodeToString(SerializedStudyDocument.serializer(), document)
                val now = System.currentTimeMillis()

                if (studyId == null) {
                    // Insertar nuevo estudio
                    val notebookId = state.selectedNotebookId
                        ?: dao.getAllNotebooksForSync().firstOrNull()?.id
                        ?: return@withContext false

                    val notebook = dao.getNotebook(notebookId) ?: return@withContext false

                    val newStudy = StudyEntity(
                        title = state.title.ifBlank { "Nueva Enseñanza" },
                        notebookId = notebookId,
                        notebookRemoteId = notebook.remoteId,
                        contentSerialized = contentJson,
                        createdAt = now,
                        updatedAt = now
                    )

                    val newId = dao.insertStudy(newStudy)
                    val citations = state.blocks
                        .filterIsInstance<StudyBlockNode.Citation>()
                        .map { citation ->
                            LinkedCitationEntity(
                                estudioId = newId,
                                book = citation.reference.book,
                                chapter = citation.reference.chapter,
                                verseStart = citation.reference.verseStart,
                                verseEnd = citation.reference.verseEnd,
                                version = citation.version,
                                positionMetadata = "inline"
                            )
                        }
                    dao.replaceCitations(newId, citations)

                    Log.d(TAG, "Nuevo estudio creado con ID: $newId")
                    true
                } else {
                    // Actualizar estudio existente
                    val existing = dao.getStudy(studyId)

                    if (existing == null) {
                        Log.w(TAG, "Estudio $studyId no encontrado en la base de datos")
                        return@withContext false
                    }

                    val updatedStudy = existing.copy(
                        title = state.title.ifBlank { "Sin título" },
                        contentSerialized = contentJson,
                        updatedAt = now,
                        deletedAt = null
                    )

                    dao.updateStudy(updatedStudy)
                    val citations = state.blocks
                        .filterIsInstance<StudyBlockNode.Citation>()
                        .map { citation ->
                            LinkedCitationEntity(
                                estudioId = studyId,
                                book = citation.reference.book,
                                chapter = citation.reference.chapter,
                                verseStart = citation.reference.verseStart,
                                verseEnd = citation.reference.verseEnd,
                                version = citation.version,
                                positionMetadata = "inline"
                            )
                        }
                    dao.replaceCitations(studyId, citations)

                    Log.d(TAG, "Estudio $studyId actualizado")
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al persistir estudio", e)
                false
            }
            }.also { success ->
                if (success) {
                    lastSavedSignature = buildIncrementalSignature(state)
                }
            }
        }
    }

    /**
     * Marca el último guardado como exitoso.
     * Útil después de un guardado manual.
     */
    fun markAsSaved(state: StudyUiState) {
        lastSavedSignature = buildIncrementalSignature(state)
    }

    /**
     * Resetea el estado de guardado.
     * Útil al cargar un nuevo estudio.
     */
    fun reset() {
        lastSavedSignature = null
    }

    /**
     * Obtiene la firma del último guardado (para debugging).
     */
    fun getLastSignature(): String? = lastSavedSignature
}

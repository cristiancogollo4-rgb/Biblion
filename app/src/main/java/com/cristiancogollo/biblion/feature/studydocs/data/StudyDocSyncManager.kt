package com.cristiancogollo.biblion.feature.studydocs.data

import android.content.Context
import android.util.Log
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncAt: Long? = null,
    val pendingChanges: Int = 0,
    val error: String? = null,
)

class StudyDocSyncManager(
    private val context: Context,
    private val repository: StudyDocRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    companion object {
        private const val TAG = "StudyDocSync"
        private const val COLLECTION_STUDY_DOCS = "study_docs"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private var listener: ListenerRegistration? = null
    private var currentUserUid: String? = null

    fun startSync(userUid: String) {
        if (currentUserUid == userUid && listener != null) return
        
        stopSync()
        currentUserUid = userUid
        
        scope.launch {
            try {
                _syncState.value = _syncState.value.copy(isSyncing = true, error = null)
                
                pushLocalChanges(userUid)
                attachRemoteListener(userUid)
                
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastSyncAt = System.currentTimeMillis(),
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start sync", e)
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    error = e.message,
                )
            }
        }
    }

    fun stopSync() {
        listener?.remove()
        listener = null
        currentUserUid = null
    }

    fun requestSync() {
        val userUid = currentUserUid ?: return
        scope.launch {
            try {
                _syncState.value = _syncState.value.copy(isSyncing = true, error = null)
                pushLocalChanges(userUid)
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastSyncAt = System.currentTimeMillis(),
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync", e)
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    error = e.message,
                )
            }
        }
    }

    private suspend fun pushLocalChanges(userUid: String) {
        val dirtyDocs = repository.getDirtyForSync()
        if (dirtyDocs.isEmpty()) {
            _syncState.value = _syncState.value.copy(pendingChanges = 0)
            return
        }

        Log.d(TAG, "Pushing ${dirtyDocs.size} local changes")
        _syncState.value = _syncState.value.copy(pendingChanges = dirtyDocs.size)

        dirtyDocs.forEach { entity ->
            try {
                val docData = mapOf(
                    "remoteId" to entity.remoteId,
                    "title" to entity.title,
                    "docJson" to entity.docJson,
                    "blockCount" to entity.blockCount,
                    "version" to entity.version,
                    "tagsCsv" to entity.tagsCsv,
                    "ownerUid" to userUid,
                    "createdAt" to entity.createdAt,
                    "updatedAt" to entity.updatedAt,
                    "deletedAt" to entity.deletedAt,
                    "syncVersion" to entity.syncVersion + 1,
                )

                withTimeout(15_000) {
                    studyDocDocument(userUid, entity.remoteId)
                        .set(docData, SetOptions.merge())
                        .await()
                }

                repository.markSynced(entity.id, entity.syncVersion + 1)
                Log.d(TAG, "Pushed doc ${entity.remoteId}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push doc ${entity.remoteId}", e)
            }
        }

        _syncState.value = _syncState.value.copy(pendingChanges = 0)
    }

    private fun attachRemoteListener(userUid: String) {
        listener?.remove()
        
        listener = firestore.collection("users")
            .document(userUid)
            .collection(COLLECTION_STUDY_DOCS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listener error", error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) return@addSnapshotListener
                
                scope.launch {
                    try {
                        snapshot.documents.forEach { doc ->
                            val remoteId = doc.getString("remoteId") ?: return@forEach
                            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
                            val remoteSyncVersion = doc.getLong("syncVersion") ?: 0L
                            
                            val localEntity = repository.getByRemoteId(remoteId)
                            
                            if (localEntity == null) {
                                pullRemoteDoc(userUid, remoteId)
                            } else if (remoteUpdatedAt > localEntity.updatedAt) {
                                pullRemoteDoc(userUid, remoteId)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process remote changes", e)
                    }
                }
            }
    }

    private suspend fun pullRemoteDoc(userUid: String, remoteId: String) {
        try {
            val doc = withTimeout(15_000) {
                studyDocDocument(userUid, remoteId).get().await()
            }
            
            if (!doc.exists()) return
            
            val docJson = doc.getString("docJson") ?: return
            val studyDoc = StudyDocJson.decode(docJson) ?: return
            
            repository.save(studyDoc, userUid)
            Log.d(TAG, "Pulled doc $remoteId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull doc $remoteId", e)
        }
    }

    private fun studyDocDocument(userUid: String, remoteId: String) =
        firestore.collection("users")
            .document(userUid)
            .collection(COLLECTION_STUDY_DOCS)
            .document(remoteId)
}

package com.cristiancogollo.biblion

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import com.cristiancogollo.biblion.feature.studydocs.data.RemoteStudyRecord
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocSyncPolicy
import com.cristiancogollo.biblion.feature.studydocs.data.StudySyncResolution

private data class RemoteUserDocument(
    val uid: String = "",
    val email: String? = null,
    val nombres: String = "",
    val apellidos: String = "",
    val correo: String? = null,
    val alias: String = "",
    val rol: String = "LECTOR",
    val estadoPublicador: String = "NO_APROBADO",
    val plan: String = "FREE",
    val fotoPerfil: String? = null,
    val biografia: String = "",
    val schemaVersion: Long = 1,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val lastLoginAt: Long = 0,
    val lastSeenAt: Long = 0
)

private data class RemoteAppPreferences(
    val darkModeEnabled: Boolean = false,
    val darkModeUpdatedAt: Long = 0,
    val selectedBibleVersion: String = "rv1960",
    val selectedBibleVersionUpdatedAt: Long = 0,
    val readerFontSizeSp: Int = 18,
    val readerFontSizeUpdatedAt: Long = 0,
    val updatedAt: Long = 0
)

private data class RemoteHighlightChapter(
    val book: String = "",
    val chapter: Int = 0,
    val verses: Map<String, Int> = emptyMap(),
    val updatedAt: Long = 0,
    val deletedAt: Long? = null
)

private data class SyncSession(
    val id: Long,
    val user: AuthUser,
    var recoveryAttempts: Int = 0,
    val coroutineJob: Job = SupervisorJob(),
    val preferencesReady: CompletableDeferred<Unit> = CompletableDeferred(),
    val highlightsReady: CompletableDeferred<Unit> = CompletableDeferred(),
    val studiesReady: CompletableDeferred<Unit> = CompletableDeferred(),
)

object FirestoreSyncManager {
    private const val TAG = "FirestoreSync"
    private const val SCHEMA_VERSION = 2L
    private const val MAX_LISTENER_RECOVERY_ATTEMPTS = 4

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferencesMutex = Mutex()
    private val highlightsMutex = Mutex()
    private val studiesMutex = Mutex()
    private val lifecycleLock = Any()
    private val _syncErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var currentSession: SyncSession? = null
    private var nextSessionId = 0L
    private var listeners: List<ListenerRegistration> = emptyList()
    private var bootstrapJob: Job? = null
    private var recoveryJob: Job? = null
    private var studiesSyncJob: Job? = null
    private var lastErrorNotificationAt: Long = 0L

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val syncErrors = _syncErrors.asSharedFlow()

    /**
     * Ejecuta una acción con retry y backoff exponencial.
     * @param maxRetries Número máximo de reintentos.
     * @param initialDelayMs Delay inicial en milisegundos.
     * @param action Acción a ejecutar.
     * @return true si tuvo éxito, false si agotó los reintentos.
     */
    private suspend fun withRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        action: suspend () -> Unit
    ): Boolean {
        var currentDelay = initialDelayMs
        repeat(maxRetries) { attempt ->
            try {
                action()
                return true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1}/$maxRetries failed", e)
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay *= 2 // Backoff exponencial
                }
            }
        }
        return false
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        FirebaseFirestore.setLoggingEnabled(BuildConfig.DEBUG)
        Log.d(TAG, "Initialized FirestoreSyncManager")
    }

    fun start(user: AuthUser) {
        val context = appContext ?: return
        synchronized(lifecycleLock) {
            val active = currentSession
            if (
                active?.user?.uid == user.uid &&
                (bootstrapJob?.isActive == true || listeners.isNotEmpty())
            ) {
                return
            }

            startLocked(context, user, recoveryAttempts = 0)
        }
    }

    private fun startLocked(context: Context, user: AuthUser, recoveryAttempts: Int) {
        stopLocked()
        val session = SyncSession(
            id = ++nextSessionId,
            user = user,
            recoveryAttempts = recoveryAttempts,
        )
        currentSession = session
        Log.d(TAG, "Starting sync session=${session.id} for uid=${user.uid}")
        bootstrapJob = scope.launch(session.coroutineJob) {
            runInitialSync(context, session)
        }
    }

    fun stop() {
        synchronized(lifecycleLock) { stopLocked() }
    }

    fun refreshNow() {
        val userToRestart = synchronized(lifecycleLock) {
            val session = currentSession
            if (
                session == null ||
                bootstrapJob?.isActive == true ||
                listeners.isNotEmpty() || recoveryJob?.isActive == true
            ) {
                null
            } else {
                session.user
            }
        }
        if (userToRestart != null) {
            start(userToRestart)
        } else {
            val session = currentSession
            if (session?.studiesReady?.isCompleted == true &&
                isSessionActive(session) && recoveryJob?.isActive != true
            ) {
                requestStudiesSync()
            }
        }
    }

    private suspend fun runInitialSync(context: Context, session: SyncSession) {
        runCatching {
            if (isSessionActive(session)) {
                Log.d(TAG, "Attaching realtime listeners for session=${session.id}")
                attachListeners(context, session)
                Log.d(TAG, "Initial sync bootstrap completed for session=${session.id}")
            }
            // A missing or offline profile must not prevent the studies listener from recovering.
            if (isSessionActive(session)) ensureUserDocument(session.user)
        }.onFailure { error ->
            if (error !is CancellationException && isSessionActive(session)) {
                Log.e(TAG, "Failed to start sync for ${session.user.uid}", error)
                notifySyncError()
            }
        }
    }

    private fun stopLocked() {
        val session = currentSession
        currentSession = null
        if (listeners.isNotEmpty() || session != null) {
            Log.d(TAG, "Stopping sync session=${session?.id} uid=${session?.user?.uid}")
        }
        bootstrapJob?.cancel()
        bootstrapJob = null
        recoveryJob?.cancel()
        recoveryJob = null
        studiesSyncJob?.cancel()
        studiesSyncJob = null
        session?.coroutineJob?.cancel()
        session?.preferencesReady?.cancel()
        session?.highlightsReady?.cancel()
        session?.studiesReady?.cancel()
        listeners.forEach { it.remove() }
        listeners = emptyList()
    }

    private fun isSessionActive(session: SyncSession): Boolean = currentSession === session

    private fun scheduleListenerRecovery(session: SyncSession) {
        val context = appContext ?: return
        synchronized(lifecycleLock) {
            if (!isSessionActive(session) || recoveryJob?.isActive == true) return
            listeners.forEach { it.remove() }
            listeners = emptyList()
            if (session.recoveryAttempts >= MAX_LISTENER_RECOVERY_ATTEMPTS) {
                Log.w(TAG, "Listener recovery exhausted for uid=${session.user.uid}")
                return
            }
            val nextAttempt = session.recoveryAttempts + 1
            val delayMs = 1_000L shl (nextAttempt - 1)
            recoveryJob = scope.launch(session.coroutineJob) {
                delay(delayMs)
                synchronized(lifecycleLock) {
                    if (isSessionActive(session)) {
                        startLocked(context, session.user, recoveryAttempts = nextAttempt)
                    }
                }
            }
        }
    }

    private fun markSessionHealthy(session: SyncSession) {
        synchronized(lifecycleLock) {
            if (isSessionActive(session) &&
                session.preferencesReady.isCompleted &&
                session.highlightsReady.isCompleted &&
                session.studiesReady.isCompleted
            ) {
                session.recoveryAttempts = 0
            }
        }
    }

    fun requestPreferencesSync() {
        val context = appContext ?: return
        val session = currentSession ?: return
        scope.launch(session.coroutineJob) {
            runCatching {
                withTimeout(15_000L) { session.preferencesReady.await() }
                preferencesMutex.withLock {
                    if (!isSessionActive(session)) return@withLock
                    pushPreferences(context, session.user.uid)
                }
            }.onFailure { error ->
                if (error !is CancellationException && isSessionActive(session)) {
                    Log.w(TAG, "Failed to push preferences", error)
                    notifySyncError()
                }
            }
        }
    }

    fun requestStudiesSync() {
        val context = appContext ?: return
        synchronized(lifecycleLock) {
            val session = currentSession ?: return
            studiesSyncJob?.cancel()
            studiesSyncJob = scope.launch(session.coroutineJob) {
                try {
                    delay(750L)
                    withTimeout(15_000L) { session.studiesReady.await() }
                    if (!isSessionActive(session)) return@launch
                    val success = withRetry(maxRetries = 3, initialDelayMs = 1_000L) {
                        studiesMutex.withLock {
                            if (!isSessionActive(session)) return@withLock
                            pushStudies(context, session.user.uid)
                        }
                    }
                    if (!success && isSessionActive(session)) {
                        Log.w(TAG, "Failed to push studies after retries")
                        notifySyncError()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (isSessionActive(session)) {
                        Log.w(TAG, "Studies sync did not become ready", error)
                        notifySyncError()
                    }
                }
            }
        }
    }

    fun requestHighlightsSync(book: String, chapter: Int, verses: Map<String, Int>) {
        val session = currentSession ?: return
        scope.launch(session.coroutineJob) {
            runCatching {
                withTimeout(15_000L) { session.highlightsReady.await() }
                highlightsMutex.withLock {
                    if (!isSessionActive(session)) return@withLock
                    pushHighlightChapter(session.user.uid, book, chapter, verses)
                }
            }.onFailure { error ->
                if (error !is CancellationException && isSessionActive(session)) {
                    Log.w(TAG, "Failed to push chapter highlights", error)
                    notifySyncError()
                }
            }
        }
    }

    fun requestHighlightsFullSync() {
        val context = appContext ?: return
        val session = currentSession ?: return
        scope.launch(session.coroutineJob) {
            runCatching {
                withTimeout(15_000L) { session.highlightsReady.await() }
                highlightsMutex.withLock {
                    if (!isSessionActive(session)) return@withLock
                    pushAllHighlights(context, session.user.uid)
                }
            }.onFailure { error ->
                if (error !is CancellationException && isSessionActive(session)) {
                    Log.w(TAG, "Failed to push all highlights", error)
                    notifySyncError()
                }
            }
        }
    }

    private suspend fun ensureUserDocument(user: AuthUser) {
        val now = System.currentTimeMillis()
        val currentSnapshot = withTimeout(15_000) {
            userRoot(user.uid).get().awaitResult()
        }
        val alias = user.displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.trim().orEmpty()
        val currentData = currentSnapshot.data.orEmpty()
        val createdAt = (currentData["createdAt"] as? Number)?.toLong() ?: now
        val fechaRegistro = (currentData["fechaRegistro"] as? Number)?.toLong()
            ?: (currentData["fecha_registro"] as? Number)?.toLong()
            ?: createdAt
        val fotoPerfil = currentData["fotoPerfil"] as? String
            ?: currentData["foto_perfil"] as? String
            ?: user.photoUrl
        val avatarColor = (currentData["avatarColor"] as? Number)?.toLong()
            ?: (currentData["avatar_color"] as? Number)?.toLong()
            ?: 0xFF0B6E9FUL.toLong()
        val doc = mapOf(
            "uid" to user.uid,
            "email" to user.email,
            "nombres" to (currentData["nombres"] as? String ?: ""),
            "apellidos" to (currentData["apellidos"] as? String ?: ""),
            "correo" to user.email,
            "alias" to (currentData["alias"] as? String ?: alias),
            "rol" to (currentData["rol"] as? String ?: "LECTOR"),
            "estadoPublicador" to (currentData["estadoPublicador"] as? String ?: "NO_APROBADO"),
            "estado_publicador" to (currentData["estado_publicador"] as? String ?: "NO_APROBADO"),
            "plan" to (currentData["plan"] as? String ?: "FREE"),
            "fotoPerfil" to fotoPerfil,
            "foto_perfil" to fotoPerfil,
            "avatarColor" to avatarColor,
            "avatar_color" to avatarColor,
            "biografia" to (currentData["biografia"] as? String ?: ""),
            "totalEnsenanzasCreadas" to ((currentData["totalEnsenanzasCreadas"] as? Number)?.toInt() ?: 0),
            "totalEnsenanzasPublicadas" to ((currentData["totalEnsenanzasPublicadas"] as? Number)?.toInt() ?: 0),
            "totalDescargas" to ((currentData["totalDescargas"] as? Number)?.toInt() ?: 0),
            "totalLikes" to ((currentData["totalLikes"] as? Number)?.toInt() ?: 0),
            "totalGuardados" to ((currentData["totalGuardados"] as? Number)?.toInt() ?: 0),
            "totalComentarios" to ((currentData["totalComentarios"] as? Number)?.toInt() ?: 0),
            "totalSeguidores" to ((currentData["totalSeguidores"] as? Number)?.toInt() ?: 0),
            "totalSiguiendo" to ((currentData["totalSiguiendo"] as? Number)?.toInt() ?: 0),
            "fechaRegistro" to fechaRegistro,
            "fecha_registro" to fechaRegistro,
            "schemaVersion" to SCHEMA_VERSION,
            "createdAt" to createdAt,
            "updatedAt" to now,
            "lastLoginAt" to now,
            "lastSeenAt" to now
        )
        Log.d(TAG, "Writing users/${user.uid}")
        withTimeout(15_000) {
            userRoot(user.uid).set(doc, SetOptions.merge()).awaitCompletion()
        }
        Log.d(TAG, "Wrote users/${user.uid}")
    }

    private suspend fun pushPreferences(context: Context, userUid: String) {
        if (userUid.isBlank()) return
        val snapshot = AppPreferencesSyncStore.getAppPreferencesSnapshot(
            context = context,
            defaultDarkMode = false
        )
        val updatedAt = maxOf(
            snapshot.darkModeUpdatedAt,
            snapshot.selectedBibleVersionUpdatedAt,
            snapshot.readerFontSizeUpdatedAt
        )
        val doc = mapOf(
            "darkModeEnabled" to snapshot.darkModeEnabled,
            "darkModeUpdatedAt" to snapshot.darkModeUpdatedAt,
            "selectedBibleVersion" to snapshot.selectedBibleVersion,
            "selectedBibleVersionUpdatedAt" to snapshot.selectedBibleVersionUpdatedAt,
            "readerFontSizeSp" to snapshot.readerFontSizeSp,
            "readerFontSizeUpdatedAt" to snapshot.readerFontSizeUpdatedAt,
            "updatedAt" to updatedAt
        )
        Log.d(TAG, "Writing users/$userUid/preferences/app updatedAt=$updatedAt")
        preferencesDocument(userUid).set(doc, SetOptions.merge()).awaitCompletion()
        Log.d(TAG, "Wrote users/$userUid/preferences/app")
    }

    private suspend fun pushStudies(context: Context, userUid: String) {
        if (userUid.isBlank()) return
        val repository = studyRepository(context)
        val dirty = repository.getDirtyForSync(userUid)
        val deleted = repository.getDeletedForSync(userUid)
        Log.d(TAG, "Found ${dirty.size} studies and ${deleted.size} deletions to sync")

        dirty.forEach { entity ->
            val syncVersion = StudyDocSyncPolicy.nextSyncVersion(entity)
            val payload = mapOf(
                "schemaVersion" to SCHEMA_VERSION,
                "remoteId" to entity.remoteId,
                "title" to entity.title,
                "notebookRemoteId" to entity.notebookRemoteId,
                "tagsCsv" to entity.tagsCsv,
                "blockCount" to entity.blockCount,
                "docVersion" to entity.version,
                "docJson" to entity.docJson,
                "createdAt" to entity.createdAt,
                "updatedAt" to entity.updatedAt,
                "ownerUid" to userUid,
                "deletedAt" to null,
                "syncVersion" to syncVersion,
                "isPublished" to true,
            )
            studyDocument(userUid, entity.remoteId)
                .set(payload, SetOptions.merge())
                .awaitCompletion()
            val marked = repository.markSyncedIfUnchanged(
                localId = entity.id,
                expectedUpdatedAt = entity.updatedAt,
                syncedAt = System.currentTimeMillis(),
                syncVersion = syncVersion,
                ownerUid = userUid,
            )
            Log.d(
                TAG,
                "Pushed study=${entity.remoteId} markedSynced=$marked version=$syncVersion",
            )
        }

        deleted.forEach { entity ->
            val deletedAt = entity.deletedAt ?: entity.updatedAt
            val syncVersion = StudyDocSyncPolicy.nextSyncVersion(entity)
            studyDocument(userUid, entity.remoteId)
                .set(
                    mapOf(
                        "schemaVersion" to SCHEMA_VERSION,
                        "remoteId" to entity.remoteId,
                        "ownerUid" to userUid,
                        "updatedAt" to deletedAt,
                        "deletedAt" to deletedAt,
                        "syncVersion" to syncVersion,
                        "isPublished" to true,
                    ),
                    SetOptions.merge(),
                )
                .awaitCompletion()
            repository.hardDeleteByRemoteId(entity.remoteId)
            Log.d(TAG, "Pushed study deletion=${entity.remoteId}")
        }
    }

    private suspend fun pushAllHighlights(context: Context, userUid: String) {
        if (userUid.isBlank()) return
        val snapshots = AppPreferencesSyncStore.getAllHighlightChapters(context)
        Log.d(TAG, "Found ${snapshots.size} highlight chapters to sync for uid=$userUid")
        snapshots.forEach { snapshot ->
            if (snapshot.verses.isEmpty()) return@forEach
            highlightDocument(userUid, snapshot.documentId)
                .set(
                    mapOf(
                        "book" to snapshot.book,
                        "chapter" to snapshot.chapter,
                        "verses" to snapshot.verses,
                        "updatedAt" to snapshot.updatedAt,
                        "deletedAt" to null
                    ),
                    SetOptions.merge()
                )
                .awaitCompletion()
        }
    }

    private suspend fun pushHighlightChapter(userUid: String, book: String, chapter: Int, verses: Map<String, Int>) {
        if (verses.isEmpty()) return
        val documentId = AppPreferencesSyncStore.chapterDocumentId(book, chapter)
        val updatedAt = System.currentTimeMillis()
        highlightDocument(userUid, documentId)
            .set(
                mapOf(
                    "book" to book,
                    "chapter" to chapter,
                    "verses" to verses,
                    "updatedAt" to updatedAt,
                    "deletedAt" to null
                ),
                SetOptions.merge()
            )
            .awaitCompletion()
    }

    private fun attachListeners(context: Context, session: SyncSession) {
        val userUid = session.user.uid
        // Readiness needs the cache-to-server event even when document contents do not change.
        val registrations = listOf(
            preferencesDocument(userUid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (!isSessionActive(session)) return@addSnapshotListener
                if (error != null) {
                    Log.w(TAG, "Preferences listener failed", error)
                    notifySyncError()
                    scheduleListenerRecovery(session)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch(session.coroutineJob) {
                    runCatching {
                        preferencesMutex.withLock {
                            if (!isSessionActive(session)) return@withLock
                            if (snapshot.exists()) {
                                applyRemotePreferences(context, snapshot.data.orEmpty())
                            }
                            val isInitialServerSnapshot =
                                !snapshot.metadata.isFromCache && !session.preferencesReady.isCompleted
                            if (isInitialServerSnapshot) {
                                session.preferencesReady.complete(Unit)
                                pushPreferences(context, userUid)
                            }
                            markSessionHealthy(session)
                        }
                    }.onFailure { failure ->
                        if (failure !is CancellationException && isSessionActive(session)) {
                            Log.w(TAG, "Failed to apply remote preferences", failure)
                            notifySyncError()
                        }
                    }
                }
            },
            highlightsCollection(userUid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (!isSessionActive(session)) return@addSnapshotListener
                if (error != null) {
                    Log.w(TAG, "Highlights listener failed", error)
                    notifySyncError()
                    scheduleListenerRecovery(session)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch(session.coroutineJob) {
                    runCatching {
                        highlightsMutex.withLock {
                            if (!isSessionActive(session)) return@withLock
                            applyRemoteHighlights(context, snapshot)
                            val isInitialServerSnapshot =
                                !snapshot.metadata.isFromCache && !session.highlightsReady.isCompleted
                            if (isInitialServerSnapshot) {
                                session.highlightsReady.complete(Unit)
                                pushAllHighlights(context, userUid)
                            }
                            markSessionHealthy(session)
                        }
                    }.onFailure { failure ->
                        if (failure !is CancellationException && isSessionActive(session)) {
                            Log.w(TAG, "Failed to apply remote highlights", failure)
                            notifySyncError()
                        }
                    }
                }
            },
            studiesCollection(userUid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (!isSessionActive(session)) return@addSnapshotListener
                if (error != null) {
                    Log.w(TAG, "Studies listener failed", error)
                    notifySyncError()
                    scheduleListenerRecovery(session)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch(session.coroutineJob) {
                    runCatching {
                        studiesMutex.withLock {
                            if (!isSessionActive(session)) return@withLock
                            if (!session.studiesReady.isCompleted ||
                                snapshot.getDocumentChanges(MetadataChanges.EXCLUDE).isNotEmpty()
                            ) {
                                applyRemoteStudies(context, snapshot, userUid)
                            }
                            if (!isSessionActive(session)) return@withLock
                            val isInitialServerSnapshot =
                                !snapshot.metadata.isFromCache && !session.studiesReady.isCompleted
                            if (isInitialServerSnapshot) {
                                session.studiesReady.complete(Unit)
                                pushStudies(context, userUid)
                            }
                            markSessionHealthy(session)
                        }
                    }.onFailure { failure ->
                        if (failure !is CancellationException && isSessionActive(session)) {
                            Log.w(TAG, "Failed to apply remote studies", failure)
                            notifySyncError()
                            if (session.studiesReady.isCompleted) requestStudiesSync()
                        }
                    }
                }
            }
        )
        synchronized(lifecycleLock) {
            if (isSessionActive(session) && recoveryJob?.isActive != true) {
                listeners.forEach { it.remove() }
                listeners = registrations
            } else {
                registrations.forEach { it.remove() }
            }
        }
    }

    private fun notifySyncError() {
        val shouldNotify = synchronized(lifecycleLock) {
            val now = System.currentTimeMillis()
            if (now - lastErrorNotificationAt < 15_000L) {
                false
            } else {
                lastErrorNotificationAt = now
                true
            }
        }
        if (shouldNotify) _syncErrors.tryEmit(Unit)
    }

    private suspend fun applyRemotePreferences(context: Context, data: Map<String, Any>) {
        val remote = RemoteAppPreferences(
            darkModeEnabled = data["darkModeEnabled"] as? Boolean ?: false,
            darkModeUpdatedAt = (data["darkModeUpdatedAt"] as? Number)?.toLong() ?: 0L,
            selectedBibleVersion = data["selectedBibleVersion"] as? String ?: "rv1960",
            selectedBibleVersionUpdatedAt = (data["selectedBibleVersionUpdatedAt"] as? Number)?.toLong() ?: 0L,
            readerFontSizeSp = (data["readerFontSizeSp"] as? Number)?.toInt() ?: 18,
            readerFontSizeUpdatedAt = (data["readerFontSizeUpdatedAt"] as? Number)?.toLong() ?: 0L,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        )
        val local = AppPreferencesSyncStore.getAppPreferencesSnapshot(context, defaultDarkMode = remote.darkModeEnabled)
        if (remote.darkModeUpdatedAt > local.darkModeUpdatedAt) {
            AppPreferencesSyncStore.setDarkModeEnabled(
                context = context,
                enabled = remote.darkModeEnabled,
                updatedAt = remote.darkModeUpdatedAt,
                triggerSync = false
            )
        }
        if (remote.selectedBibleVersionUpdatedAt > local.selectedBibleVersionUpdatedAt) {
            AppPreferencesSyncStore.setSelectedBibleVersion(
                context = context,
                versionKey = remote.selectedBibleVersion,
                updatedAt = remote.selectedBibleVersionUpdatedAt,
                triggerSync = false
            )
        }
        if (remote.readerFontSizeUpdatedAt > local.readerFontSizeUpdatedAt) {
            AppPreferencesSyncStore.setReaderFontSizeSp(
                context = context,
                fontSizeSp = remote.readerFontSizeSp,
                updatedAt = remote.readerFontSizeUpdatedAt,
                triggerSync = false
            )
        }
    }

    private suspend fun applyRemoteStudies(
        context: Context,
        snapshot: QuerySnapshot,
        userUid: String,
    ) {
        val repository = studyRepository(context)
        val syncedAt = System.currentTimeMillis()
        snapshot.documents.forEach { document ->
            val remote = document.data.orEmpty().toRemoteStudyRecord(document.id)
            val local = repository.getEntityByRemoteId(remote.remoteId)
            when (StudyDocSyncPolicy.resolve(local, remote, userUid)) {
                StudySyncResolution.APPLY_REMOTE -> {
                    val entity = StudyDocSyncPolicy.toLocalEntity(
                        remote = remote,
                        activeOwnerUid = userUid,
                        syncedAt = syncedAt,
                    )
                    if (entity != null) {
                        repository.upsertRemote(entity)
                        Log.d(TAG, "Applied remote study=${remote.remoteId}")
                    } else {
                        Log.w(TAG, "Ignored undecodable study=${remote.remoteId}")
                    }
                }
                StudySyncResolution.PRESERVE_LOCAL_CONFLICT_AND_APPLY_REMOTE -> {
                    val remoteEntity = if (remote.deletedAt == null) {
                        StudyDocSyncPolicy.toLocalEntity(
                            remote = remote,
                            activeOwnerUid = userUid,
                            syncedAt = syncedAt,
                        )
                    } else {
                        null
                    }
                    if (remote.deletedAt == null && remoteEntity == null) {
                        Log.w(TAG, "Ignored undecodable conflicting study=${remote.remoteId}")
                        return@forEach
                    }
                    val conflict = local?.let {
                        StudyDocSyncPolicy.createConflictCopy(
                            local = it,
                            activeOwnerUid = userUid,
                            now = syncedAt,
                        )
                    }
                    if (conflict != null) {
                        repository.upsertRemote(conflict)
                        Log.w(
                            TAG,
                            "Preserved local conflict=${conflict.remoteId} " +
                                "before applying remote=${remote.remoteId}",
                        )
                    }
                    if (remote.deletedAt != null) {
                        repository.hardDeleteByRemoteId(remote.remoteId)
                    } else if (remoteEntity != null) {
                        repository.upsertRemote(remoteEntity)
                    }
                }
                StudySyncResolution.DELETE_LOCAL -> {
                    repository.hardDeleteByRemoteId(remote.remoteId)
                    Log.d(TAG, "Applied remote deletion=${remote.remoteId}")
                }
                StudySyncResolution.KEEP_LOCAL -> Unit
                StudySyncResolution.IGNORE_FOREIGN_OWNER -> {
                    Log.w(TAG, "Ignored foreign study=${remote.remoteId}")
                }
                StudySyncResolution.IGNORE_INVALID_REMOTE -> {
                    Log.w(TAG, "Ignored invalid study=${remote.remoteId}")
                }
            }
        }
    }

    private suspend fun applyRemoteHighlights(context: Context, snapshot: QuerySnapshot) {
        snapshot.documents.forEach { doc ->
            val remote = doc.toObject(RemoteHighlightChapter::class.java) ?: return@forEach
            if (remote.deletedAt != null) return@forEach
            AppPreferencesSyncStore.applyRemoteHighlightChapter(
                context = context,
                book = remote.book,
                chapter = remote.chapter,
                verses = remote.verses,
                remoteUpdatedAt = remote.updatedAt
            )
        }
    }

    private fun userRoot(uid: String) = firestore.collection("users").document(uid)
    private fun preferencesDocument(uid: String) = userRoot(uid).collection("preferences").document("app")
    private fun highlightsCollection(uid: String) = userRoot(uid).collection("chapter_highlights")
    private fun highlightDocument(uid: String, documentId: String) = highlightsCollection(uid).document(documentId)
    private fun studiesCollection(uid: String) = userRoot(uid).collection("studies")
    private fun studyDocument(uid: String, remoteId: String) =
        studiesCollection(uid).document(remoteId)

    private fun studyRepository(context: Context): StudyDocRepository =
        StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao())

    private fun Map<String, Any>.toRemoteStudyRecord(documentId: String): RemoteStudyRecord =
        RemoteStudyRecord(
            remoteId = (this["remoteId"] as? String).orEmpty().ifBlank { documentId },
            title = this["title"] as? String ?: "",
            notebookRemoteId = (this["notebookRemoteId"] as? String)?.takeIf { it.isNotBlank() },
            tagsCsv = this["tagsCsv"] as? String ?: "",
            blockCount = (this["blockCount"] as? Number)?.toInt() ?: 0,
            docVersion = (this["docVersion"] as? Number)?.toInt() ?: 0,
            docJson = (this["docJson"] as? String)?.takeIf { it.isNotBlank() }
                ?: (this["contentSerialized"] as? String)
                ?: "",
            createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
            updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: 0L,
            ownerUid = this["ownerUid"] as? String,
            deletedAt = (this["deletedAt"] as? Number)?.toLong(),
            syncVersion = (this["syncVersion"] as? Number)?.toLong() ?: 0L,
            isPublished = this["isPublished"] as? Boolean ?: true,
        )
}

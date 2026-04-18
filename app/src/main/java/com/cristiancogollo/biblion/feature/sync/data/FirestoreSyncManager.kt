package com.cristiancogollo.biblion

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private data class RemoteUserDocument(
    val uid: String = "",
    val email: String? = null,
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

private data class RemoteNotebookDocument(
    val remoteId: String = "",
    val title: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val ownerUid: String? = null,
    val deletedAt: Long? = null,
    val syncVersion: Long = 0
)

private data class RemoteCitationDocument(
    val book: String = "",
    val chapter: Int = 0,
    val verseStart: Int = 0,
    val verseEnd: Int = 0,
    val version: String = "rv1960",
    val positionMetadata: String = "inline"
)

private data class RemoteStudyDocument(
    val remoteId: String = "",
    val notebookRemoteId: String = "",
    val title: String = "",
    val contentSerialized: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val ownerUid: String? = null,
    val deletedAt: Long? = null,
    val syncVersion: Long = 0,
    val citations: List<RemoteCitationDocument> = emptyList()
)

private data class RemoteHighlightChapter(
    val book: String = "",
    val chapter: Int = 0,
    val verses: Map<String, Int> = emptyMap(),
    val updatedAt: Long = 0,
    val deletedAt: Long? = null
)

object FirestoreSyncManager {
    private const val TAG = "FirestoreSync"
    private const val SCHEMA_VERSION = 1L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; classDiscriminator = "nodeType" }
    private val _syncErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var appContext: Context? = null
    private var currentUser: AuthUser? = null
    private var listeners: List<ListenerRegistration> = emptyList()
    private var lastErrorNotificationAt: Long = 0L

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val syncErrors = _syncErrors.asSharedFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        FirebaseFirestore.setLoggingEnabled(true)
        Log.d(TAG, "Initialized FirestoreSyncManager")
    }

    fun start(user: AuthUser) {
        val context = appContext ?: return
        if (currentUser?.uid == user.uid && listeners.isNotEmpty()) return
        stop()
        currentUser = user
        Log.d(TAG, "Starting sync for uid=${user.uid}, email=${user.email}")
        scope.launch {
            runCatching {
                syncMutex.withLock {
                    Log.d(TAG, "Ensuring root user document")
                    ensureUserDocument(user)
                    Log.d(TAG, "Pushing preferences")
                    pushPreferences(context)
                    Log.d(TAG, "Pushing studies and notebooks")
                    pushStudies(context, user.uid)
                    Log.d(TAG, "Pushing highlights")
                    pushAllHighlights(context)
                    Log.d(TAG, "Attaching realtime listeners")
                    attachListeners(context, user.uid)
                    Log.d(TAG, "Initial sync bootstrap completed")
                }
            }.onFailure {
                Log.e(TAG, "Failed to start sync for ${user.uid}", it)
                notifySyncError()
            }
        }
    }

    fun stop() {
        if (listeners.isNotEmpty() || currentUser != null) {
            Log.d(TAG, "Stopping sync for uid=${currentUser?.uid}")
        }
        listeners.forEach { it.remove() }
        listeners = emptyList()
        currentUser = null
    }

    fun refreshNow() {
        val context = appContext ?: return
        val user = currentUser ?: return
        scope.launch {
            runCatching {
                syncMutex.withLock {
                    ensureUserDocument(user)
                    pushPreferences(context)
                    pushStudies(context, user.uid)
                    pushAllHighlights(context, user.uid)
                }
            }.onFailure {
                Log.w(TAG, "Failed to refresh sync", it)
                notifySyncError()
            }
        }
    }

    fun requestPreferencesSync() {
        val context = appContext ?: return
        val user = currentUser ?: return
        scope.launch {
            runCatching {
                syncMutex.withLock {
                    ensureUserDocument(user)
                    pushPreferences(context)
                }
            }.onFailure {
                Log.w(TAG, "Failed to push preferences", it)
                notifySyncError()
            }
        }
    }

    fun requestStudiesSync() {
        val context = appContext ?: return
        val user = currentUser ?: return
        scope.launch {
            runCatching {
                syncMutex.withLock {
                    pushStudies(context, user.uid)
                }
            }.onFailure {
                Log.w(TAG, "Failed to push studies", it)
                notifySyncError()
            }
        }
    }

    fun requestHighlightsSync(book: String, chapter: Int, verses: Map<String, Int>) {
        val user = currentUser ?: return
        scope.launch {
            runCatching {
                pushHighlightChapter(user.uid, book, chapter, verses)
            }.onFailure {
                Log.w(TAG, "Failed to push chapter highlights", it)
                notifySyncError()
            }
        }
    }

    fun requestHighlightsFullSync() {
        val context = appContext ?: return
        val user = currentUser ?: return
        scope.launch {
            runCatching {
                syncMutex.withLock {
                    pushAllHighlights(context, user.uid)
                }
            }.onFailure {
                Log.w(TAG, "Failed to push all highlights", it)
                notifySyncError()
            }
        }
    }

    private suspend fun ensureUserDocument(user: AuthUser) {
        val now = System.currentTimeMillis()
        val doc = mapOf(
            "uid" to user.uid,
            "email" to user.email,
            "schemaVersion" to SCHEMA_VERSION,
            "createdAt" to now,
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

    private suspend fun pushPreferences(context: Context) {
        val user = currentUser ?: return
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
        Log.d(TAG, "Writing users/${user.uid}/preferences/app updatedAt=$updatedAt")
        preferencesDocument(user.uid).set(doc, SetOptions.merge()).awaitCompletion()
        Log.d(TAG, "Wrote users/${user.uid}/preferences/app")
    }

    private suspend fun pushStudies(context: Context, userUid: String = currentUser?.uid.orEmpty()) {
        if (userUid.isBlank()) return
        val dao = StudyDatabase.getInstance(context).studyDao()
        val notebooks = dao.getAllNotebooksForSync()
        Log.d(TAG, "Found ${notebooks.size} notebooks to sync for uid=$userUid")
        notebooks.forEach { notebook ->
            val nextVersion = notebook.syncVersion + 1
            val remote = mapOf(
                "remoteId" to notebook.remoteId,
                "title" to notebook.title,
                "createdAt" to notebook.createdAt,
                "updatedAt" to notebook.updatedAt,
                "ownerUid" to userUid,
                "deletedAt" to notebook.deletedAt,
                "syncVersion" to nextVersion
            )
            notebookDocument(userUid, notebook.remoteId).set(remote, SetOptions.merge()).awaitCompletion()
            dao.updateNotebook(
                notebook.copy(
                    ownerUid = userUid,
                    lastSyncedAt = System.currentTimeMillis(),
                    syncVersion = nextVersion
                )
            )
        }

        val studies = dao.getAllStudiesForSync()
        Log.d(TAG, "Found ${studies.size} studies to sync for uid=$userUid")
        studies.forEach { study ->
            val nextVersion = study.syncVersion + 1
            val citations = buildRemoteCitations(study, dao)
            val remote = mapOf(
                "remoteId" to study.remoteId,
                "notebookRemoteId" to study.notebookRemoteId,
                "title" to study.title,
                "contentSerialized" to study.contentSerialized,
                "createdAt" to study.createdAt,
                "updatedAt" to study.updatedAt,
                "ownerUid" to userUid,
                "deletedAt" to study.deletedAt,
                "syncVersion" to nextVersion,
                "citations" to citations
            )
            studyDocument(userUid, study.remoteId).set(remote, SetOptions.merge()).awaitCompletion()
            dao.updateStudy(
                study.copy(
                    ownerUid = userUid,
                    lastSyncedAt = System.currentTimeMillis(),
                    syncVersion = nextVersion
                )
            )
        }
    }

    private suspend fun pushAllHighlights(context: Context, userUid: String = currentUser?.uid.orEmpty()) {
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

    private suspend fun attachListeners(context: Context, userUid: String) {
        listeners = listOf(
            preferencesDocument(userUid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Preferences listener failed", error)
                    notifySyncError()
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                scope.launch { applyRemotePreferences(context, snapshot.data.orEmpty()) }
            },
            notebookCollection(userUid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Notebook listener failed", error)
                    notifySyncError()
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch { applyRemoteNotebooks(context, snapshot) }
            },
            studyCollection(userUid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Study listener failed", error)
                    notifySyncError()
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch { applyRemoteStudies(context, snapshot) }
            },
            highlightsCollection(userUid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Highlights listener failed", error)
                    notifySyncError()
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch { applyRemoteHighlights(context, snapshot) }
            }
        )
    }

    private fun notifySyncError() {
        val now = System.currentTimeMillis()
        if (now - lastErrorNotificationAt < 15_000L) return
        lastErrorNotificationAt = now
        _syncErrors.tryEmit(Unit)
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

    private suspend fun applyRemoteNotebooks(context: Context, snapshot: QuerySnapshot) {
        val dao = StudyDatabase.getInstance(context).studyDao()
        snapshot.documents.forEach { doc ->
            val remote = doc.toObject(RemoteNotebookDocument::class.java) ?: return@forEach
            val local = dao.getNotebookByRemoteId(remote.remoteId)
            if (local != null && local.updatedAt > remote.updatedAt) return@forEach

            if (local == null) {
                dao.insertNotebook(
                    StudyNotebookEntity(
                        remoteId = remote.remoteId,
                        title = remote.title,
                        createdAt = remote.createdAt,
                        updatedAt = remote.updatedAt,
                        ownerUid = remote.ownerUid,
                        deletedAt = remote.deletedAt,
                        lastSyncedAt = remote.updatedAt,
                        syncVersion = remote.syncVersion
                    )
                )
            } else {
                dao.updateNotebook(
                    local.copy(
                        title = remote.title,
                        createdAt = remote.createdAt,
                        updatedAt = remote.updatedAt,
                        ownerUid = remote.ownerUid,
                        deletedAt = remote.deletedAt,
                        lastSyncedAt = remote.updatedAt,
                        syncVersion = remote.syncVersion
                    )
                )
            }
        }
    }

    private suspend fun applyRemoteStudies(context: Context, snapshot: QuerySnapshot) {
        val dao = StudyDatabase.getInstance(context).studyDao()
        snapshot.documents.forEach { doc ->
            val remote = doc.toObject(RemoteStudyDocument::class.java) ?: return@forEach
            val notebook = dao.getNotebookByRemoteId(remote.notebookRemoteId)
                ?: createMissingNotebook(dao, remote.notebookRemoteId, remote.ownerUid)
            val local = dao.getStudyByRemoteId(remote.remoteId)
            if (local != null && local.updatedAt > remote.updatedAt) return@forEach

            val localId = if (local == null) {
                dao.insertStudy(
                    StudyEntity(
                        remoteId = remote.remoteId,
                        title = remote.title,
                        notebookId = notebook.id,
                        notebookRemoteId = remote.notebookRemoteId,
                        contentSerialized = remote.contentSerialized,
                        createdAt = remote.createdAt,
                        updatedAt = remote.updatedAt,
                        ownerUid = remote.ownerUid,
                        deletedAt = remote.deletedAt,
                        lastSyncedAt = remote.updatedAt,
                        syncVersion = remote.syncVersion
                    )
                )
            } else {
                dao.updateStudy(
                    local.copy(
                        title = remote.title,
                        notebookId = notebook.id,
                        notebookRemoteId = remote.notebookRemoteId,
                        contentSerialized = remote.contentSerialized,
                        createdAt = remote.createdAt,
                        updatedAt = remote.updatedAt,
                        ownerUid = remote.ownerUid,
                        deletedAt = remote.deletedAt,
                        lastSyncedAt = remote.updatedAt,
                        syncVersion = remote.syncVersion
                    )
                )
                local.id
            }

            val citations = remote.citations.map {
                LinkedCitationEntity(
                    estudioId = localId,
                    book = it.book,
                    chapter = it.chapter,
                    verseStart = it.verseStart,
                    verseEnd = it.verseEnd,
                    version = it.version,
                    positionMetadata = it.positionMetadata
                )
            }
            dao.replaceCitations(localId, citations)
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

    private suspend fun createMissingNotebook(
        dao: StudyDao,
        remoteId: String,
        ownerUid: String?
    ): StudyNotebookEntity {
        val now = System.currentTimeMillis()
        val localId = dao.insertNotebook(
            StudyNotebookEntity(
                remoteId = remoteId.ifBlank { CuidGenerator.create() },
                title = "Mis Notas de Estudio",
                createdAt = now,
                updatedAt = now,
                ownerUid = ownerUid,
                lastSyncedAt = now
            )
        )
        return checkNotNull(dao.getNotebook(localId))
    }

    private suspend fun buildRemoteCitations(
        study: StudyEntity,
        dao: StudyDao
    ): List<Map<String, Any>> {
        val linked = dao.getLinkedCitations(study.id)
        if (linked.isNotEmpty()) {
            return linked.map {
                mapOf(
                    "book" to it.book,
                    "chapter" to it.chapter,
                    "verseStart" to it.verseStart,
                    "verseEnd" to it.verseEnd,
                    "version" to it.version,
                    "positionMetadata" to it.positionMetadata
                )
            }
        }

        val document = runCatching {
            json.decodeFromString<SerializedStudyDocument>(study.contentSerialized)
        }.getOrDefault(SerializedStudyDocument())

        return document.blocks
            .filterIsInstance<StudyBlockNode.Citation>()
            .map {
                mapOf(
                    "book" to it.reference.book,
                    "chapter" to it.reference.chapter,
                    "verseStart" to it.reference.verseStart,
                    "verseEnd" to it.reference.verseEnd,
                    "version" to it.version,
                    "positionMetadata" to "inline"
                )
            }
    }

    private fun userRoot(uid: String) = firestore.collection("users").document(uid)
    private fun preferencesDocument(uid: String) = userRoot(uid).collection("preferences").document("app")
    private fun notebookCollection(uid: String) = userRoot(uid).collection("notebooks")
    private fun notebookDocument(uid: String, remoteId: String) = notebookCollection(uid).document(remoteId)
    private fun studyCollection(uid: String) = userRoot(uid).collection("studies")
    private fun studyDocument(uid: String, remoteId: String) = studyCollection(uid).document(remoteId)
    private fun highlightsCollection(uid: String) = userRoot(uid).collection("chapter_highlights")
    private fun highlightDocument(uid: String, documentId: String) = highlightsCollection(uid).document(documentId)
}

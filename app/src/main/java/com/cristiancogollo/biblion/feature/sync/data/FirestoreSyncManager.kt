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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

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
    private const val SCHEMA_VERSION = 2L

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
        // Sync de ensenanzas ahora se maneja via StudyDocSyncManager.
        // Este metodo se mantiene para compatibilidad pero no hace nada.
        // La sincronizacion se inicia desde AppNavigation cuando el usuario se autentica.
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

    @Suppress("UNUSED_PARAMETER")
    private suspend fun pushStudies(context: Context, userUid: String = currentUser?.uid.orEmpty()) {
        // Sync de ensenanzas deshabilitado tras migracion a StudyDoc (v2).
        // El DAO legacy `StudyDatabase` ya no existe; la nueva DB es `study_docs.db`.
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
        // Listeners de notebooks/studies removidos tras migracion a StudyDoc (v2).
        // Solo preferences y highlights siguen sincronizandose.
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

    @Suppress("UNUSED_PARAMETER")
    private suspend fun applyRemoteNotebooks(context: Context, snapshot: QuerySnapshot) {
        // Sync de notebooks deshabilitado tras migracion a StudyDoc (v2).
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun applyRemoteStudies(context: Context, snapshot: QuerySnapshot) {
        // Sync de studies deshabilitado tras migracion a StudyDoc (v2).
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
}

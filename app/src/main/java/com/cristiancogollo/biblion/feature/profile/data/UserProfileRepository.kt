package com.cristiancogollo.biblion

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BiblionUserProfile(
    val uid: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val correo: String = "",
    val alias: String = "",
    val rol: String = "LECTOR",
    val estadoPublicador: String = "NO_APROBADO",
    val plan: String = "FREE",
    val fotoPerfil: String? = null,
    val avatarColor: Long = 0xFF0B6E9FUL.toLong(),
    val biografia: String = "",
    val fechaRegistro: Long = 0,
    val totalEnsenanzasCreadas: Int = 0,
    val totalEnsenanzasPublicadas: Int = 0,
    val totalDescargas: Int = 0,
    val totalLikes: Int = 0,
    val totalGuardados: Int = 0,
    val totalComentarios: Int = 0,
    val totalSeguidores: Int = 0,
    val totalSiguiendo: Int = 0
) {
    val isComplete: Boolean
        get() = nombres.isNotBlank() && apellidos.isNotBlank() && alias.isNotBlank()
}

interface UserProfileRepository {
    fun observeProfile(uid: String): Flow<BiblionUserProfile?>

    suspend fun saveProfile(
        uid: String,
        nombres: String,
        apellidos: String,
        alias: String,
        biografia: String
    )

    suspend fun saveAvatarColor(uid: String, avatarColor: Long)

    suspend fun uploadProfilePhoto(context: Context, uid: String, imageUri: Uri): String

    suspend fun clearProfilePhoto(uid: String)
}

class FirestoreUserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : UserProfileRepository {

    override fun observeProfile(uid: String): Flow<BiblionUserProfile?> = callbackFlow {
        val listener = userDocument(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }

            val data = snapshot?.data
            trySend(data?.toUserProfile(uid))
        }

        awaitClose { listener.remove() }
    }

    override suspend fun saveProfile(
        uid: String,
        nombres: String,
        apellidos: String,
        alias: String,
        biografia: String
    ) {
        val normalizedNombres = nombres.trim()
        val normalizedApellidos = apellidos.trim()
        val normalizedAlias = alias.trim()
        val normalizedBiografia = biografia.trim()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "nombres" to normalizedNombres,
            "apellidos" to normalizedApellidos,
            "alias" to normalizedAlias,
            "biografia" to normalizedBiografia,
            "updatedAt" to now,
            "profileCompletedAt" to now
        )
        userDocument(uid).set(data, SetOptions.merge()).awaitCompletion()
    }

    override suspend fun saveAvatarColor(uid: String, avatarColor: Long) {
        userDocument(uid).set(
            mapOf(
                "avatarColor" to avatarColor,
                "avatar_color" to avatarColor,
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).awaitCompletion()
    }

    override suspend fun uploadProfilePhoto(context: Context, uid: String, imageUri: Uri): String {
        val stream = context.contentResolver.openInputStream(imageUri)
            ?: error("No se pudo abrir la imagen seleccionada.")
        val photoRef = storage.reference
            .child("profile_photos")
            .child(uid)
            .child("avatar.jpg")
        stream.use { input ->
            photoRef.putStream(input).awaitResult()
        }
        val downloadUrl = photoRef.downloadUrl.awaitResult().toString()
        userDocument(uid).set(
            mapOf(
                "fotoPerfil" to downloadUrl,
                "foto_perfil" to downloadUrl,
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).awaitCompletion()
        return downloadUrl
    }

    override suspend fun clearProfilePhoto(uid: String) {
        userDocument(uid).set(
            mapOf(
                "fotoPerfil" to null,
                "foto_perfil" to null,
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).awaitCompletion()
    }

    private fun userDocument(uid: String) = firestore.collection("users").document(uid)
}

private fun Map<String, Any>.toUserProfile(uid: String): BiblionUserProfile {
    val estadoPublicador = this["estadoPublicador"] as? String
        ?: this["estado_publicador"] as? String
        ?: "NO_APROBADO"
    val fotoPerfil = this["fotoPerfil"] as? String
        ?: this["foto_perfil"] as? String
    val avatarColor = (this["avatarColor"] as? Number)?.toLong()
        ?: (this["avatar_color"] as? Number)?.toLong()
        ?: 0xFF0B6E9FUL.toLong()
    val fechaRegistro = (this["fechaRegistro"] as? Number)?.toLong()
        ?: (this["fecha_registro"] as? Number)?.toLong()
        ?: (this["createdAt"] as? Number)?.toLong()
        ?: 0L

    return BiblionUserProfile(
        uid = this["uid"] as? String ?: uid,
        nombres = this["nombres"] as? String ?: "",
        apellidos = this["apellidos"] as? String ?: "",
        correo = this["correo"] as? String ?: this["email"] as? String ?: "",
        alias = this["alias"] as? String ?: "",
        rol = this["rol"] as? String ?: "LECTOR",
        estadoPublicador = estadoPublicador,
        plan = this["plan"] as? String ?: "FREE",
        fotoPerfil = fotoPerfil,
        avatarColor = avatarColor,
        biografia = this["biografia"] as? String ?: "",
        fechaRegistro = fechaRegistro,
        totalEnsenanzasCreadas = intValue("totalEnsenanzasCreadas", "total_ensenanzas_creadas"),
        totalEnsenanzasPublicadas = intValue("totalEnsenanzasPublicadas", "total_ensenanzas_publicadas"),
        totalDescargas = intValue("totalDescargas", "total_descargas"),
        totalLikes = intValue("totalLikes", "total_likes"),
        totalGuardados = intValue("totalGuardados", "total_guardados"),
        totalComentarios = intValue("totalComentarios", "total_comentarios"),
        totalSeguidores = intValue("totalSeguidores", "total_seguidores"),
        totalSiguiendo = intValue("totalSiguiendo", "total_siguiendo")
    )
}

private fun Map<String, Any>.intValue(vararg keys: String): Int {
    return keys.firstNotNullOfOrNull { key -> (this[key] as? Number)?.toInt() } ?: 0
}

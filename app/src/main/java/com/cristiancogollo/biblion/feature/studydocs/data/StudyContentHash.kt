package com.cristiancogollo.biblion.feature.studydocs.data

import java.security.MessageDigest

/** Stable hash for exports and future server-side publication signatures. */
object StudyContentHash {
    fun sha256(payload: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}

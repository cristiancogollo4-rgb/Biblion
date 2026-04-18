package com.cristiancogollo.biblion

import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger

object CuidGenerator {
    private val random = SecureRandom()
    private val counter = AtomicInteger(0)
    private const val alphabet = "0123456789abcdefghijklmnopqrstuvwxyz"

    fun create(): String {
        val timestamp = System.currentTimeMillis().toString(36)
        val count = counter.getAndIncrement().toString(36).padStart(4, '0')
        val randomPart = buildString {
            repeat(12) {
                append(alphabet[random.nextInt(alphabet.length)])
            }
        }
        return "c$timestamp$count$randomPart"
    }
}

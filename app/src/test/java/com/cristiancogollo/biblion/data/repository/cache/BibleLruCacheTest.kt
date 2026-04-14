package com.cristiancogollo.biblion.data.repository.cache

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleLruCacheTest {

    @Test
    fun evictsLeastRecentlyUsedEntry_whenMaxSizeExceeded() {
        val cache = BibleLruCache<String, Int>(maxSize = 3)

        cache["rv1960"] = 1
        cache["nvi"] = 2
        cache["dhh"] = 3

        // Marcar rv1960 como recientemente usado, por lo que debe salir nvi.
        cache["rv1960"]
        cache["tla"] = 4

        assertTrue(cache.containsKey("rv1960"))
        assertTrue(cache.containsKey("dhh"))
        assertTrue(cache.containsKey("tla"))
        assertFalse(cache.containsKey("nvi"))
        assertEquals(listOf("dhh", "rv1960", "tla"), cache.keysInLruOrder())
    }

    @Test
    fun supportsConcurrentReadsAndWrites_withSynchronizedStrategy() {
        val cache = BibleLruCache<Int, Int>(maxSize = 200)
        val pool = Executors.newFixedThreadPool(8)
        val errors = Collections.synchronizedList(mutableListOf<Throwable>())
        val start = CountDownLatch(1)
        val done = CountDownLatch(80)

        repeat(40) { writerId ->
            pool.submit {
                try {
                    start.await()
                    repeat(100) { i ->
                        cache[writerId * 100 + i] = i
                    }
                } catch (t: Throwable) {
                    errors.add(t)
                } finally {
                    done.countDown()
                }
            }
        }

        repeat(40) { readerId ->
            pool.submit {
                try {
                    start.await()
                    repeat(100) { i ->
                        cache[(readerId * 100 + i) % 200]
                    }
                } catch (t: Throwable) {
                    errors.add(t)
                } finally {
                    done.countDown()
                }
            }
        }

        start.countDown()
        val finished = done.await(10, TimeUnit.SECONDS)
        pool.shutdownNow()

        assertTrue("Concurrent tasks timed out", finished)
        assertTrue("Unexpected errors during concurrent access: $errors", errors.isEmpty())
        assertTrue(cache.keysInLruOrder().size <= 200)
    }
}

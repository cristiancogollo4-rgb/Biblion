package com.cristiancogollo.biblion

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BibleRepositoryConcurrencyTest {

    @Test
    fun bibleDatabase_concurrentRequests_singleInstance() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()

        val results = coroutineScope {
            List(64) {
                async(Dispatchers.Default) {
                    BibleDatabase.getInstance(context)
                }
            }.awaitAll()
        }

        assertTrue(results.isNotEmpty())
        val identities = results.map { System.identityHashCode(it) }.toSet()
        assertEquals(1, identities.size)
    }


    @Test
    fun bibleDatabase_stressConcurrentRequests_singleInstance() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()

            val results = coroutineScope {
                List(4) {
                    List(64) {
                        async(Dispatchers.Default) {
                            BibleDatabase.getInstance(context)
                        }
                    }.awaitAll()
                }.flatten()
            }

            assertTrue(results.isNotEmpty())
            val identities = results.map { System.identityHashCode(it) }.toSet()
            assertEquals(1, identities.size)
        }
}

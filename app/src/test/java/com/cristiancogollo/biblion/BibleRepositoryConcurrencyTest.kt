package com.cristiancogollo.biblion

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BibleRepositoryConcurrencyTest {

    @Test
    fun getBibleForVersion_concurrentRequests_sameVersionKey_noNpe() = runBlocking {
        BibleRepository.clearCache()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val versionKey = "rv1960"

        val results = coroutineScope {
            List(64) {
                async(Dispatchers.Default) {
                    invokeGetBibleForVersion(context, versionKey)
                }
            }.awaitAll()
        }

        assertTrue(results.isNotEmpty())
        val identities = results.map { System.identityHashCode(it) }.toSet()
        assertEquals(1, identities.size)
    }


    @Test
    fun getBibleForVersion_stressConcurrentRequests_sameAndDifferentVersionKeys_singleInstancePerKey() =
        runBlocking {
            BibleRepository.clearCache()
            val context = ApplicationProvider.getApplicationContext<Application>()
            val availableKeys = BibleRepository.getAvailableVersions(context).map { it.key }
            val keysToTest = (availableKeys.take(3) + listOf("missing_a", "missing_b")).distinct()

            val resultsByKey = coroutineScope {
                keysToTest.associateWith { versionKey ->
                    List(64) {
                        async(Dispatchers.Default) {
                            invokeGetBibleForVersion(context, versionKey)
                        }
                    }
                }.mapValues { (_, deferreds) -> deferreds.awaitAll() }
            }

            assertTrue(resultsByKey.isNotEmpty())
            resultsByKey.forEach { (_, results) ->
                assertTrue(results.isNotEmpty())
                val identities = results.map { System.identityHashCode(it) }.toSet()
                assertEquals(1, identities.size)
            }
        }

    private fun invokeGetBibleForVersion(context: Context, versionKey: String): JSONObject {
        val method = BibleRepository::class.java.getDeclaredMethod(
            "getBibleForVersion",
            Context::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(BibleRepository, context, versionKey) as JSONObject
    }
}

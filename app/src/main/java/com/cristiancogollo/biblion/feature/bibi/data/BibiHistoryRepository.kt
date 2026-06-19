package com.cristiancogollo.biblion.feature.bibi.data

import android.content.Context
import com.cristiancogollo.biblion.feature.bibi.BibiResponse
import com.cristiancogollo.biblion.feature.bibi.BibiUserContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BibiHistoryRepository {

    suspend fun save(
        context: Context,
        query: String,
        response: BibiResponse,
        userContext: BibiUserContext? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val dao = BibiHistoryDatabase.getInstance(context).bibiHistoryDao()
            dao.insert(
                BibiHistoryEntity(
                    query = query,
                    responseText = response.buildChatText(),
                    category = response.source.name,
                    bibleVersion = userContext?.bibleVersion ?: "",
                    book = userContext?.currentBook ?: "",
                    chapter = userContext?.currentChapter ?: 0,
                    isAiResponse = response.source == com.cristiancogollo.biblion.feature.bibi.Source.AI
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("BibiHistory", "Error saving history", e)
        }
    }

    suspend fun saveWithString(
        context: Context,
        query: String,
        response: String,
        category: String = "FALLBACK",
        userContext: BibiUserContext? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val dao = BibiHistoryDatabase.getInstance(context).bibiHistoryDao()
            dao.insert(
                BibiHistoryEntity(
                    query = query,
                    responseText = response,
                    category = category,
                    bibleVersion = userContext?.bibleVersion ?: "",
                    book = userContext?.currentBook ?: "",
                    chapter = userContext?.currentChapter ?: 0,
                    isAiResponse = false
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("BibiHistory", "Error saving history", e)
        }
    }

    suspend fun getRecentQueries(
        context: Context,
        limit: Int = 5
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            val dao = BibiHistoryDatabase.getInstance(context).bibiHistoryDao()
            dao.getRecent(limit).map { it.query }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun clearHistory(context: Context) = withContext(Dispatchers.IO) {
        try {
            BibiHistoryDatabase.getInstance(context).bibiHistoryDao().clearAll()
        } catch (e: Exception) {
            android.util.Log.e("BibiHistory", "Error clearing history", e)
        }
    }
}

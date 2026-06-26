package com.cristiancogollo.biblion.feature.bibi.engine

import android.content.Context
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryRepository

object AmbiguousTermResolver {

    suspend fun findClosest(context: Context, term: String, maxResults: Int = 3): List<String> {
        val results = DictionaryRepository.searchEntries(context, term)
        if (results.isEmpty()) return emptyList()

        return results
            .filter { it.term.lowercase() != term.lowercase() }
            .take(maxResults)
            .map { it.term }
    }

    suspend fun findDisambiguation(
        context: Context,
        term: String,
        maxResults: Int = 4
    ): List<DisambiguationOption> {
        val results = DictionaryRepository.searchEntries(context, term)
        if (results.size < 2) return emptyList()

        return results.take(maxResults).map { result ->
            DisambiguationOption(
                term = result.term,
                category = result.category.displayName,
                preview = result.definitionPreview
            )
        }
    }
}

data class DisambiguationOption(
    val term: String,
    val category: String,
    val preview: String
)

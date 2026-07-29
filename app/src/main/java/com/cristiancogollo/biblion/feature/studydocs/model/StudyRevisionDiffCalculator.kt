package com.cristiancogollo.biblion.feature.studydocs.model

object StudyRevisionDiffCalculator {
    fun compare(before: StudyDoc, after: StudyDoc): RevisionDiff {
        val beforeById = before.blocks.associateBy { it.id }
        val afterById = after.blocks.associateBy { it.id }
        val added = afterById.keys.count { it !in beforeById }
        val removed = beforeById.keys.count { it !in afterById }
        val modified = afterById.keys.count { id ->
            id in beforeById && beforeById[id]?.plainText() != afterById[id]?.plainText()
        }
        val beforeWords = words(before.plainText())
        val afterWords = words(after.plainText())
        val shared = beforeWords.intersect(afterWords).size
        val union = beforeWords.union(afterWords).size
        return RevisionDiff(
            blocksAdded = added,
            blocksModified = modified,
            blocksRemoved = removed,
            wordsAdded = (afterWords - beforeWords).size,
            wordsRemoved = (beforeWords - afterWords).size,
            similarityPercent = if (union == 0) 100 else (shared * 100 / union),
        )
    }

    private fun words(text: String): Set<String> = text
        .lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
        .toSet()
}

package com.cristiancogollo.biblion.feature.studydocs.model

/**
 * Single source of truth for the tags a teaching needs before it can be saved:
 * at least one purpose, audience and topic tag, and exactly one state tag.
 * Custom tags are allowed but never satisfy a required group.
 */
object StudyTagRules {

    /** Required groups in the order the editor presents them. */
    val REQUIRED_GROUPS: List<String> = listOf(
        DocTagGroups.PURPOSE,
        DocTagGroups.AUDIENCE,
        DocTagGroups.TOPIC,
        DocTagGroups.STATE,
    )

    /** Index in [REQUIRED_GROUPS] of the first group that is not satisfied, or null if valid. */
    fun firstIncompleteGroupIndex(tags: List<String>): Int? =
        REQUIRED_GROUPS.indexOfFirst { group -> !isGroupSatisfied(group, tags) }
            .takeIf { it >= 0 }

    fun isValid(tags: List<String>): Boolean = firstIncompleteGroupIndex(tags) == null

    private fun isGroupSatisfied(group: String, tags: List<String>): Boolean {
        val count = tags.distinct().count { DocTagGroups.groupFor(it) == group }
        return if (group == DocTagGroups.STATE) count == 1 else count >= 1
    }
}

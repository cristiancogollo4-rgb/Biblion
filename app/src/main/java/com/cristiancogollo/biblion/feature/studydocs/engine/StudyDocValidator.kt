package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc

sealed interface ValidationIssue {
    val message: String

    data class DuplicateBlockId(val id: BlockId) : ValidationIssue {
        override val message = "duplicate block id: $id"
    }
    data class EmptyTitle(val context: String) : ValidationIssue {
        override val message = "title is empty (context: $context)"
    }
    data class InvalidHeadingLevel(val level: Int) : ValidationIssue {
        override val message = "invalid heading level: $level"
    }
    data class InvalidVerseCompare(val blockId: BlockId) : ValidationIssue {
        override val message = "verse $blockId has compareText without compareVersion"
    }
    data class StyleOutOfBounds(val blockId: BlockId, val start: Int, val end: Int, val length: Int) : ValidationIssue {
        override val message = "block $blockId style range [$start,$end] exceeds text length $length"
    }
    data class EmptyTableRow(val blockId: BlockId, val rowIndex: Int) : ValidationIssue {
        override val message = "block $blockId row $rowIndex has no cells"
    }
    data class InconsistentTableWidth(val blockId: BlockId, val widths: Set<Int>) : ValidationIssue {
        override val message = "block $blockId has rows with different cell counts: $widths"
    }
    data class MissingRequiredTag(val group: String) : ValidationIssue {
        override val message = "missing required tag group: $group"
    }
    data class InvalidMetadata(override val message: String) : ValidationIssue
    data class EmptyDocument(override val message: String) : ValidationIssue {
        constructor() : this("document has no blocks; expected at least 1")
    }
}

data class ValidationResult(
    val issues: List<ValidationIssue>,
) {
    val isValid: Boolean get() = issues.isEmpty()
    val errors: List<ValidationIssue> = issues
    val hasMissingRequiredTags: Boolean
        get() = issues.any { it is ValidationIssue.MissingRequiredTag }
}

object StudyDocValidator {

    fun validate(
        doc: StudyDoc,
        requireValidMetadata: Boolean = false,
        requireMinBlocks: Boolean = false,
    ): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()

        if (requireMinBlocks && doc.blocks.isEmpty()) {
            issues.add(ValidationIssue.EmptyDocument())
        }

        val seenIds = mutableSetOf<BlockId>()
        for (block in doc.blocks) {
            if (!seenIds.add(block.id)) {
                issues.add(ValidationIssue.DuplicateBlockId(block.id))
            }
        }

        if (doc.title.isBlank() && doc.blocks.isNotEmpty()) {
            issues.add(ValidationIssue.EmptyTitle("document has blocks but no title"))
        }

        for (block in doc.blocks) {
            issues.addAll(validateBlock(block))
        }

        if (requireValidMetadata) {
            issues.addAll(validateMetadata(doc))
        }

        return ValidationResult(issues)
    }

    fun validateBlock(block: StudyBlock): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        when (block) {
            is StudyBlock.Heading -> {
                if (block.level !in 1..6) {
                    issues.add(ValidationIssue.InvalidHeadingLevel(block.level))
                }
            }
            is StudyBlock.Verse -> {
                if (block.compareText != null && block.compareVersion == null) {
                    issues.add(ValidationIssue.InvalidVerseCompare(block.id))
                }
            }
            is StudyBlock.Table -> {
                if (block.rows.isEmpty()) return issues
                val widths = block.rows.map { it.cells.size }.toSet()
                if (widths.size > 1) {
                    issues.add(ValidationIssue.InconsistentTableWidth(block.id, widths))
                }
                block.rows.forEachIndexed { rowIndex, row ->
                    if (row.cells.isEmpty()) {
                        issues.add(ValidationIssue.EmptyTableRow(block.id, rowIndex))
                    }
                }
            }
            else -> Unit
        }
        return issues
    }

    fun validateMetadata(doc: StudyDoc): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val tags = doc.metadata.tags
        val purposeCount = tags.count { DocTagGroups.groupFor(it) == DocTagGroups.PURPOSE }
        val audienceCount = tags.count { DocTagGroups.groupFor(it) == DocTagGroups.AUDIENCE }
        val topicCount = tags.count { DocTagGroups.groupFor(it) == DocTagGroups.TOPIC }
        val stateCount = tags.count { DocTagGroups.groupFor(it) == DocTagGroups.STATE }

        if (purposeCount < 1) issues.add(ValidationIssue.MissingRequiredTag(DocTagGroups.PURPOSE))
        if (audienceCount < 1) issues.add(ValidationIssue.MissingRequiredTag(DocTagGroups.AUDIENCE))
        if (topicCount < 1) issues.add(ValidationIssue.MissingRequiredTag(DocTagGroups.TOPIC))
        if (stateCount != 1) {
            issues.add(ValidationIssue.InvalidMetadata("expected exactly 1 state tag, found $stateCount"))
        }
        return issues
    }
}

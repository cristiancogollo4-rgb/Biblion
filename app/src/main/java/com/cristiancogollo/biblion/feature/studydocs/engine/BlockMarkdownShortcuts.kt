package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

object BlockMarkdownShortcuts {

    data class Match(val block: StudyBlock, val consumedLength: Int)

    fun detect(originalId: String, text: StyledText): Match? {
        val raw = text.plain()
        if (raw.isEmpty()) return null

        val heading = HEADING_RE.matchEntire(raw)
        if (heading != null) {
            val level = heading.groupValues[1].length
            val remaining = heading.groupValues[2]
            return Match(
                block = StudyBlock.Heading(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId(originalId),
                    level = level,
                    text = StyledText.plain(remaining),
                ),
                consumedLength = level + 1,
            )
        }

        val bullet = BULLET_RE.matchEntire(raw)
        if (bullet != null) {
            return Match(
                block = StudyBlock.BulletList(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId(originalId),
                    items = listOf(StyledText.plain(bullet.groupValues[1])),
                ),
                consumedLength = 2,
            )
        }

        val numbered = NUMBERED_RE.matchEntire(raw)
        if (numbered != null) {
            return Match(
                block = StudyBlock.NumberedList(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId(originalId),
                    items = listOf(StyledText.plain(numbered.groupValues[1])),
                ),
                consumedLength = 3,
            )
        }

        val quote = QUOTE_RE.matchEntire(raw)
        if (quote != null) {
            return Match(
                block = StudyBlock.Quote(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId(originalId),
                    text = StyledText.plain(quote.groupValues[1]),
                ),
                consumedLength = 2,
            )
        }

        val reflection = REFLECTION_RE.matchEntire(raw)
        if (reflection != null) {
            return Match(
                block = StudyBlock.Reflection(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId(originalId),
                    prompt = null,
                    text = StyledText.plain(reflection.groupValues[1]),
                ),
                consumedLength = 2,
            )
        }

        val note = NOTE_RE.matchEntire(raw)
        if (note != null) {
            return Match(
                block = StudyBlock.Note(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId(originalId),
                    text = StyledText.plain(note.groupValues[1]),
                ),
                consumedLength = 3,
            )
        }

        if (raw == "---" || raw == "***") {
            return Match(
                block = StudyBlock.Divider(
                    id = com.cristiancogollo.biblion.feature.studydocs.model.BlockId(originalId),
                ),
                consumedLength = raw.length,
            )
        }

        return null
    }

    fun asReplaceOp(originalBlockId: String, text: StyledText): StudyOp? {
        val match = detect(originalBlockId, text) ?: return null
        return StudyOp.ReplaceBlock(
            blockId = com.cristiancogollo.biblion.feature.studydocs.model.BlockId(originalBlockId),
            newBlock = match.block,
        )
    }

    private val HEADING_RE = Regex("^(#{1,6}) (.+)$")
    private val BULLET_RE = Regex("^[-*] (.+)$")
    private val NUMBERED_RE = Regex("^1\\. (.+)$")
    private val QUOTE_RE = Regex("^> (.+)$")
    private val REFLECTION_RE = Regex("^\\? (.+)$")
    private val NOTE_RE = Regex("^\\[\\] (.+)$")
}

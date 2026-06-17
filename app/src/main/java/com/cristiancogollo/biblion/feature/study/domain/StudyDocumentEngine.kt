package com.cristiancogollo.biblion

data class ColumnFlowSegment(
    val start: Int,
    val end: Int,
    val text: String,
    val blocksAfter: List<ColumnEmbeddedBlock>
)

object StudyDocumentEngine {
    private val brRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val blockEndRegex = Regex("</p>|</div>|</h[1-6]>", RegexOption.IGNORE_CASE)
    private val tagRegex = Regex("<[^>]*>")
    private val horizontalWhitespaceRegex = Regex("[ \\t]+")
    private val repeatedNewlineRegex = Regex("\\n{3,}")
    private val referenceRegex = Regex("""([1-3]?\s?[A-Za-zÁÉÍÓÚáéíóúñÑ]+)\s+(\d+):(\d+)(?:-(\d+))?""")

    fun updateParagraphText(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        text: String
    ): List<StudyBlockNode> {
        val currentBlocks = ensureTextFlow(blocks, fallbackHtml)
        val sourceBlocks = if (currentBlocks.any { it is StudyBlockNode.Paragraph && it.blockId == blockId }) {
            currentBlocks
        } else {
            currentBlocks + StudyBlockNode.Paragraph(blockId = blockId, text = text)
        }
        return normalizeStudyFlow(
            sourceBlocks.map { block ->
                if (block is StudyBlockNode.Paragraph && block.blockId == blockId) {
                    block.copy(
                        text = text,
                        embeddedBlocks = block.embeddedBlocks.repositionAfterTextChange(
                            oldText = block.text,
                            newText = text
                        )
                    )
                } else {
                    block
                }
            }
        )
    }

    fun updateParagraphParallelText(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        text: String
    ): List<StudyBlockNode> {
        return normalizeStudyFlow(
            ensureTextFlow(blocks, fallbackHtml).map { block ->
                if (block is StudyBlockNode.Paragraph && block.blockId == blockId) {
                    block.copy(
                        parallelText = text,
                        parallelEmbeddedBlocks = block.parallelEmbeddedBlocks.repositionAfterTextChange(
                            oldText = block.parallelText,
                            newText = text
                        )
                    )
                } else {
                    block
                }
            }
        )
    }

    fun splitParagraph(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        newBlockId: String,
        currentText: String,
        nextText: String,
        nextRole: String
    ): List<StudyBlockNode> {
        val currentBlocks = ensureTextFlow(blocks, fallbackHtml).toMutableList()
        val index = currentBlocks.indexOfFirst { block ->
            block is StudyBlockNode.Paragraph && block.blockId == blockId
        }
        if (index >= 0) {
            val current = currentBlocks[index]
            if (current is StudyBlockNode.Paragraph) {
                currentBlocks[index] = current.copy(text = currentText)
                currentBlocks.add(
                    index + 1,
                    StudyBlockNode.Paragraph(
                        blockId = newBlockId,
                        text = nextText,
                        role = nextRole
                    )
                )
            }
        }
        return normalizeStudyFlow(currentBlocks)
    }

    fun updateParagraphRole(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        role: String
    ): List<StudyBlockNode> {
        val roleUpdatedBlocks = ensureTextFlow(blocks, fallbackHtml).map { block ->
            if (block is StudyBlockNode.Paragraph && block.blockId == blockId) {
                if (role == "paragraph" && block.role == "columns") {
                    block.copy(
                        text = mergePlainText(block.text, block.parallelText),
                        parallelText = "",
                        role = role
                    )
                } else {
                    block.copy(role = role)
                }
            } else {
                block
            }
        }
        return if (role == "columns") {
            ensureParagraphAfter(roleUpdatedBlocks, blockId)
        } else {
            normalizeStudyFlow(roleUpdatedBlocks)
        }
    }

    fun splitBlockForRole(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        newRole: String,
        selectionStart: Int,
        selectionEnd: Int
    ): List<StudyBlockNode> {
        val safeStart = selectionStart.coerceAtLeast(0)
        val safeEnd = selectionEnd.coerceAtLeast(safeStart)
        return ensureTextFlow(blocks, fallbackHtml).flatMap { block ->
            if (block !is StudyBlockNode.Paragraph || block.blockId != blockId || safeStart == safeEnd) {
                listOf(block)
            } else {
                val before = block.text.substring(0, safeStart).trimEnd('\n')
                val selected = block.text.substring(safeStart, safeEnd).trim('\n')
                val after = block.text.substring(safeEnd).trimStart('\n')
                buildList {
                    if (before.isNotBlank()) {
                        add(block.copy(blockId = CuidGenerator.create(), text = before))
                    }
                    add(block.copy(blockId = CuidGenerator.create(), text = selected, role = newRole))
                    if (after.isNotBlank()) {
                        add(block.copy(blockId = CuidGenerator.create(), text = after))
                    }
                }
            }
        }.let { normalizeStudyFlow(it) }
    }

    fun splitBlockForAlignment(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        newAlignment: String,
        selectionStart: Int,
        selectionEnd: Int
    ): List<StudyBlockNode> {
        val safeStart = selectionStart.coerceAtLeast(0)
        val safeEnd = selectionEnd.coerceAtLeast(safeStart)
        return ensureTextFlow(blocks, fallbackHtml).flatMap { block ->
            if (block !is StudyBlockNode.Paragraph || block.blockId != blockId || safeStart == safeEnd) {
                listOf(block)
            } else {
                val before = block.text.substring(0, safeStart).trimEnd('\n')
                val selected = block.text.substring(safeStart, safeEnd).trim('\n')
                val after = block.text.substring(safeEnd).trimStart('\n')
                buildList {
                    if (before.isNotBlank()) {
                        add(block.copy(blockId = CuidGenerator.create(), text = before))
                    }
                    add(block.copy(blockId = CuidGenerator.create(), text = selected, textAlign = newAlignment))
                    if (after.isNotBlank()) {
                        add(block.copy(blockId = CuidGenerator.create(), text = after))
                    }
                }
            }
        }.let { normalizeStudyFlow(it) }
    }

    fun updateParagraphAlignment(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        textAlign: String
    ): List<StudyBlockNode> {
        val normalizedAlign = when (textAlign) {
            "center", "end" -> textAlign
            else -> "start"
        }
        return ensureTextFlow(blocks, fallbackHtml).map { block ->
            if (block is StudyBlockNode.Paragraph && block.blockId == blockId) {
                block.copy(textAlign = normalizedAlign)
            } else {
                block
            }
        }
    }

    fun updateColumnEmbeddedBlocks(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        paragraphBlockId: String,
        source: String,
        transform: (List<ColumnEmbeddedBlock>) -> List<ColumnEmbeddedBlock>
    ): List<StudyBlockNode> {
        return ensureTextFlow(blocks, fallbackHtml).map { block ->
            if (block is StudyBlockNode.Paragraph && block.blockId == paragraphBlockId) {
                if (source == "parallel") {
                    block.copy(
                        parallelEmbeddedBlocks = transform(block.parallelEmbeddedBlocks)
                            .normalizedColumnPositions(block.parallelText.length)
                    )
                } else {
                    block.copy(
                        embeddedBlocks = transform(block.embeddedBlocks)
                            .normalizedColumnPositions(block.text.length)
                    )
                }
            } else {
                block
            }
        }
    }

    fun buildColumnFlow(text: String, blocks: List<ColumnEmbeddedBlock>): List<ColumnFlowSegment> {
        val groupedBlocks = blocks.normalizedColumnPositions(text.length)
            .groupBy { it.position.coerceIn(0, text.length) }
            .toSortedMap()
        if (groupedBlocks.isEmpty()) {
            return listOf(ColumnFlowSegment(start = 0, end = text.length, text = text, blocksAfter = emptyList()))
        }

        val segments = mutableListOf<ColumnFlowSegment>()
        var cursor = 0
        groupedBlocks.forEach { (position, blocksAtPosition) ->
            val clampedPosition = position.coerceIn(cursor, text.length)
            segments += ColumnFlowSegment(
                start = cursor,
                end = clampedPosition,
                text = text.substring(cursor, clampedPosition),
                blocksAfter = blocksAtPosition
            )
            cursor = clampedPosition
        }
        segments += ColumnFlowSegment(
            start = cursor,
            end = text.length,
            text = text.substring(cursor, text.length),
            blocksAfter = emptyList()
        )
        return segments
    }

    fun applyParagraphTextStyle(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        source: String,
        start: Int,
        end: Int,
        color: Long?,
        background: Long?,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        fontSizeSp: Float?
    ): List<StudyBlockNode> {
        return ensureTextFlow(blocks, fallbackHtml).map { block ->
            if (block is StudyBlockNode.Paragraph && block.blockId == blockId) {
                block.applyTextStyle(
                    source = source,
                    start = start,
                    end = end,
                    color = color,
                    background = background,
                    bold = bold,
                    italic = italic,
                    underline = underline,
                    fontSizeSp = fontSizeSp
                )
            } else {
                block
            }
        }
    }

    fun clearParagraphTextStyle(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        source: String,
        start: Int,
        end: Int,
        clearColor: Boolean,
        clearBackground: Boolean,
        clearBold: Boolean,
        clearItalic: Boolean,
        clearUnderline: Boolean,
        clearFontSize: Boolean
    ): List<StudyBlockNode> {
        return ensureTextFlow(blocks, fallbackHtml).map { block ->
            if (block is StudyBlockNode.Paragraph && block.blockId == blockId) {
                block.clearTextStyle(
                    source = source,
                    start = start,
                    end = end,
                    clearColor = clearColor,
                    clearBackground = clearBackground,
                    clearBold = clearBold,
                    clearItalic = clearItalic,
                    clearUnderline = clearUnderline,
                    clearFontSize = clearFontSize
                )
            } else {
                block
            }
        }
    }

    fun updateRichTextBlock(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        blockId: String,
        html: String
    ): List<StudyBlockNode> {
        return normalizeStudyFlow(
            ensureTextFlow(blocks, fallbackHtml).map { block ->
                if (block is StudyBlockNode.RichText && block.blockId == blockId) {
                    block.copy(html = html, references = detectReferences(html))
                } else {
                    block
                }
            }
        )
    }

    fun insertInteractiveBlock(
        blocks: List<StudyBlockNode>,
        fallbackHtml: String,
        block: StudyBlockNode,
        afterBlockId: String?
    ): List<StudyBlockNode> {
        val currentBlocks = ensureTextFlow(blocks, fallbackHtml).toMutableList()
        val insertionIndex = afterBlockId
            ?.let { id -> currentBlocks.indexOfFirst { it.asFlowBlockId() == id } }
            ?.takeIf { it >= 0 }
            ?.plus(1)
            ?: currentBlocks.size

        currentBlocks.add(insertionIndex, block)
        val nextIndex = insertionIndex + 1
        if (currentBlocks.getOrNull(nextIndex) !is StudyBlockNode.Paragraph) {
            currentBlocks.add(nextIndex, StudyBlockNode.Paragraph(text = ""))
        }

        return normalizeStudyFlow(currentBlocks)
    }

    fun updateBlock(blocks: List<StudyBlockNode>, updatedBlock: StudyBlockNode): List<StudyBlockNode> {
        return blocks.map { block ->
            if (block.asInteractiveBlockId() == updatedBlock.asInteractiveBlockId()) updatedBlock else block
        }
    }

    fun toggleBlockCollapsed(blocks: List<StudyBlockNode>, blockId: String): List<StudyBlockNode> {
        return blocks.map { block -> block.withToggledCollapse(blockId) }
    }

    fun deleteBlock(blocks: List<StudyBlockNode>, blockId: String): List<StudyBlockNode> {
        return normalizeStudyFlow(blocks.filterNot { it.asInteractiveBlockId() == blockId })
    }

    fun rebuildBlocks(html: String, old: List<StudyBlockNode>): List<StudyBlockNode> {
        val nonText = old.filterNot { it is StudyBlockNode.RichText || it is StudyBlockNode.Paragraph }
        val textBlocks = old.filterIsInstance<StudyBlockNode.Paragraph>()
        val updatedFirst = if (textBlocks.isNotEmpty()) {
            textBlocks.first().copy(text = html.asPlainStudyText())
        } else {
            StudyBlockNode.Paragraph(text = html.asPlainStudyText())
        }
        return listOf(updatedFirst) + textBlocks.drop(1) + nonText
    }

    fun ensureTextFlow(blocks: List<StudyBlockNode>, fallbackHtml: String): List<StudyBlockNode> {
        if (blocks.any { it is StudyBlockNode.Paragraph }) return blocks
        if (blocks.any { it is StudyBlockNode.RichText }) return blocks
        return listOf(StudyBlockNode.Paragraph(text = fallbackHtml.asPlainStudyText())) + blocks
    }

    fun normalizeStudyFlow(blocks: List<StudyBlockNode>): List<StudyBlockNode> {
        val normalized = mutableListOf<StudyBlockNode>()
        blocks.map { it.toNativeTextBlock() }.forEach { block ->
            val last = normalized.lastOrNull()
            if (
                block is StudyBlockNode.Paragraph &&
                last is StudyBlockNode.Paragraph &&
                block.role == "paragraph" &&
                last.role == "paragraph" &&
                block.styles.isEmpty() &&
                last.styles.isEmpty() &&
                block.embeddedBlocks.isEmpty() &&
                last.embeddedBlocks.isEmpty() &&
                block.parallelText.isEmpty() &&
                last.parallelText.isEmpty() &&
                !block.text.contains('\n') &&
                !last.text.contains('\n')
            ) {
                val mergedText = mergePlainText(last.text, block.text)
                normalized[normalized.lastIndex] = if (last.text.isBlank() && block.text.isNotBlank()) {
                    block.copy(text = mergedText)
                } else {
                    last.copy(text = mergedText)
                }
            } else {
                normalized.add(block)
            }
        }

        val withoutDuplicateEmptyText = normalized.filterIndexed { index, block ->
            block !is StudyBlockNode.Paragraph ||
                block.text.isNotBlank() ||
                block.parallelText.isNotBlank() ||
                block.embeddedBlocks.isNotEmpty() ||
                block.parallelEmbeddedBlocks.isNotEmpty() ||
                normalized.none { it is StudyBlockNode.Paragraph && it.hasTextContent() } ||
                normalized.getOrNull(index - 1) !is StudyBlockNode.Paragraph ||
                (normalized.getOrNull(index - 1) as? StudyBlockNode.Paragraph)?.role in setOf("columns", "bullet", "numbered", "heading")
        }

        return withoutDuplicateEmptyText.ifEmpty {
            listOf(StudyBlockNode.Paragraph(text = ""))
        }
    }

    fun buildPlainTextSnapshot(blocks: List<StudyBlockNode>): String {
        return blocks.mapNotNull { block ->
            when (block) {
                is StudyBlockNode.Paragraph -> listOf(
                    buildColumnPlainText(block.text, block.embeddedBlocks),
                    buildColumnPlainText(block.parallelText, block.parallelEmbeddedBlocks)
                ).filter { it.isNotBlank() }.joinToString("\n")
                is StudyBlockNode.RichText -> block.html.asPlainStudyText()
                is StudyBlockNode.Note -> block.text
                is StudyBlockNode.Reflection -> listOf(block.topic, block.text).filter { it.isNotBlank() }.joinToString("\n")
                is StudyBlockNode.QuotedVerse -> listOf(block.reference, block.primaryText, block.compareText, block.note).filter { it.isNotBlank() }.joinToString("\n")
                is StudyBlockNode.Question -> listOf(block.question, block.answer).filter { it.isNotBlank() }.joinToString("\n")
                is StudyBlockNode.TwoColumn -> listOf(block.leftTitle, block.leftText, block.rightTitle, block.rightText).filter { it.isNotBlank() }.joinToString("\n")
                else -> null
            }
        }.joinToString("\n\n")
    }

    fun toPlainStudyText(html: String): String {
        return html.replace(brRegex, "\n")
            .replace(blockEndRegex, "\n")
            .replace(tagRegex, " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(horizontalWhitespaceRegex, " ")
            .replace(repeatedNewlineRegex, "\n\n")
            .trim()
    }

    fun detectReferences(text: String): List<BibleReferenceNode> {
        return referenceRegex.findAll(text).mapNotNull { match ->
            val book = match.groupValues[1].trim()
            val chapter = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val start = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
            val end = match.groupValues[4].toIntOrNull() ?: start
            BibleReferenceNode(book = book, chapter = chapter, verseStart = start, verseEnd = end)
        }.toList()
    }

    fun parseReference(reference: String): BibleReferenceNode? = detectReferences(reference).firstOrNull()

    fun flowBlockId(block: StudyBlockNode): String? = block.asFlowBlockId()

    fun interactiveBlockId(block: StudyBlockNode): String? = block.asInteractiveBlockId()

    fun toggleCollapsedIfMatches(block: StudyBlockNode, blockId: String): StudyBlockNode {
        return block.withToggledCollapse(blockId)
    }

    private fun StudyBlockNode.Paragraph.applyTextStyle(
        source: String,
        start: Int,
        end: Int,
        color: Long?,
        background: Long?,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        fontSizeSp: Float?
    ): StudyBlockNode.Paragraph {
        val textLength = if (source == "parallel") parallelText.length else text.length
        val rangeStart = start.coerceIn(0, textLength).coerceAtMost(end.coerceIn(0, textLength))
        val rangeEnd = start.coerceIn(0, textLength).coerceAtLeast(end.coerceIn(0, textLength))
        if (rangeStart == rangeEnd) return this

        val style = TextStyleRange(
            start = rangeStart,
            end = rangeEnd,
            color = color,
            background = background,
            bold = bold,
            italic = italic,
            underline = underline,
            fontSizeSp = fontSizeSp
        )
        return if (source == "parallel") {
            copy(parallelStyles = parallelStyles + style)
        } else {
            copy(styles = styles + style)
        }
    }

    private fun StudyBlockNode.Paragraph.clearTextStyle(
        source: String,
        start: Int,
        end: Int,
        clearColor: Boolean,
        clearBackground: Boolean,
        clearBold: Boolean,
        clearItalic: Boolean,
        clearUnderline: Boolean,
        clearFontSize: Boolean
    ): StudyBlockNode.Paragraph {
        val textLength = if (source == "parallel") parallelText.length else text.length
        val rangeStart = start.coerceIn(0, textLength).coerceAtMost(end.coerceIn(0, textLength))
        val rangeEnd = start.coerceIn(0, textLength).coerceAtLeast(end.coerceIn(0, textLength))
        if (rangeStart == rangeEnd) return this

        fun TextStyleRange.hasAnyStyle(): Boolean =
            color != null || background != null || bold || italic || underline || fontSizeSp != null

        fun trimStyle(style: TextStyleRange): TextStyleRange = style.copy(
            color = if (clearColor) null else style.color,
            background = if (clearBackground) null else style.background,
            bold = if (clearBold) false else style.bold,
            italic = if (clearItalic) false else style.italic,
            underline = if (clearUnderline) false else style.underline,
            fontSizeSp = if (clearFontSize) null else style.fontSizeSp
        )

        fun clearStyles(styles: List<TextStyleRange>): List<TextStyleRange> = buildList {
            styles.forEach { style ->
                if (style.end <= rangeStart || style.start >= rangeEnd) {
                    add(style)
                } else {
                    if (style.start < rangeStart) {
                        add(style.copy(end = rangeStart))
                    }
                    val middle = trimStyle(
                        style.copy(
                            start = style.start.coerceAtLeast(rangeStart),
                            end = style.end.coerceAtMost(rangeEnd)
                        )
                    )
                    if (middle.start < middle.end && middle.hasAnyStyle()) {
                        add(middle)
                    }
                    if (style.end > rangeEnd) {
                        add(style.copy(start = rangeEnd))
                    }
                }
            }
        }

        return if (source == "parallel") {
            copy(parallelStyles = clearStyles(parallelStyles))
        } else {
            copy(styles = clearStyles(styles))
        }
    }

    private fun ensureParagraphAfter(blocks: List<StudyBlockNode>, blockId: String): List<StudyBlockNode> {
        val mutableBlocks = blocks.toMutableList()
        val index = mutableBlocks.indexOfFirst { block ->
            block is StudyBlockNode.Paragraph && block.blockId == blockId
        }
        if (index < 0) return blocks

        val next = mutableBlocks.getOrNull(index + 1)
        if (next is StudyBlockNode.Paragraph) return blocks

        mutableBlocks.add(index + 1, StudyBlockNode.Paragraph(text = ""))
        return mutableBlocks
    }

    private fun StudyBlockNode.Paragraph.hasTextContent(): Boolean {
        return text.isNotBlank() ||
            parallelText.isNotBlank() ||
            embeddedBlocks.isNotEmpty() ||
            parallelEmbeddedBlocks.isNotEmpty()
    }

    private fun buildColumnPlainText(text: String, blocks: List<ColumnEmbeddedBlock>): String {
        return buildColumnFlow(text, blocks).flatMap { segment ->
            listOf(segment.text) + segment.blocksAfter.flatMap { block -> listOf(block.title, block.text) }
        }.filter { it.isNotBlank() }.joinToString("\n")
    }

    private fun List<ColumnEmbeddedBlock>.normalizedColumnPositions(textLength: Int): List<ColumnEmbeddedBlock> {
        return map { block ->
            block.copy(position = block.position.coerceIn(0, textLength))
        }.sortedBy { it.position }
    }

    private fun List<ColumnEmbeddedBlock>.repositionAfterTextChange(
        oldText: String,
        newText: String
    ): List<ColumnEmbeddedBlock> {
        if (isEmpty()) return this
        if (oldText == newText) return normalizedColumnPositions(newText.length)

        val prefixLength = oldText.commonPrefixWith(newText).length
        val maxSuffixLength = minOf(oldText.length - prefixLength, newText.length - prefixLength)
        var suffixLength = 0
        while (
            suffixLength < maxSuffixLength &&
            oldText[oldText.lastIndex - suffixLength] == newText[newText.lastIndex - suffixLength]
        ) {
            suffixLength += 1
        }

        val oldChangedEnd = oldText.length - suffixLength
        val newChangedEnd = newText.length - suffixLength
        val delta = newText.length - oldText.length

        return map { block ->
            val oldPosition = block.position.coerceIn(0, oldText.length)
            val newPosition = when {
                oldPosition <= prefixLength -> oldPosition
                oldPosition >= oldChangedEnd -> oldPosition + delta
                else -> newChangedEnd
            }.coerceIn(0, newText.length)
            block.copy(position = newPosition)
        }.normalizedColumnPositions(newText.length)
    }

    private fun String.asPlainStudyText(): String = toPlainStudyText(this)

    private fun mergePlainText(first: String, second: String): String {
        val firstClean = first.trim()
        val secondClean = second.trim()
        return when {
            firstClean.isBlank() -> secondClean
            secondClean.isBlank() -> firstClean
            else -> "$firstClean\n$secondClean"
        }
    }

    private fun StudyBlockNode.toNativeTextBlock(): StudyBlockNode = when (this) {
        is StudyBlockNode.RichText -> StudyBlockNode.Paragraph(
            blockId = blockId,
            text = html.asPlainStudyText(),
            role = "paragraph"
        )
        else -> this
    }

    private fun StudyBlockNode.asFlowBlockId(): String? = when (this) {
        is StudyBlockNode.Paragraph -> blockId
        is StudyBlockNode.RichText -> blockId
        is StudyBlockNode.Note -> blockId
        is StudyBlockNode.Reflection -> blockId
        is StudyBlockNode.QuotedVerse -> blockId
        is StudyBlockNode.Question -> blockId
        is StudyBlockNode.TwoColumn -> blockId
        else -> null
    }

    private fun StudyBlockNode.asInteractiveBlockId(): String? = when (this) {
        is StudyBlockNode.Note -> blockId
        is StudyBlockNode.Reflection -> blockId
        is StudyBlockNode.QuotedVerse -> blockId
        is StudyBlockNode.Question -> blockId
        is StudyBlockNode.TwoColumn -> blockId
        else -> null
    }

    private fun StudyBlockNode.withToggledCollapse(blockId: String): StudyBlockNode = when (this) {
        is StudyBlockNode.Note -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        is StudyBlockNode.Reflection -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        is StudyBlockNode.QuotedVerse -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        is StudyBlockNode.Question -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        is StudyBlockNode.TwoColumn -> if (this.blockId == blockId) copy(collapsed = !collapsed) else this
        else -> this
    }
}

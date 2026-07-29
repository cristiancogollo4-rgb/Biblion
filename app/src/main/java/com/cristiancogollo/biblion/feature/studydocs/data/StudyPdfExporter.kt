package com.cristiancogollo.biblion.feature.studydocs.data

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import java.io.File
import kotlin.math.max

/** Deterministic A4 renderer independent from the device density and editor zoom. */
object StudyPdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)
    private const val BODY_SIZE = 12f
    private const val LINE_HEIGHT = 18f

    fun export(document: StudyDoc, target: File) {
        val pdf = PdfDocument()
        val writer = Writer(pdf)
        writer.heading(document.title.ifBlank { "Ensenanza" })
        document.blocks.forEach { block ->
            when (block) {
                is StudyBlock.BulletList -> block.items.forEach { writer.paragraph("* ${it.plain()}") }
                is StudyBlock.OrderedList -> block.items.forEachIndexed { index, item ->
                    writer.paragraph("${index + 1}. ${item.plain()}")
                }
                is StudyBlock.Heading -> writer.heading(block.text.raw, block.fontSize.toFloat())
                is StudyBlock.Verse -> writer.verse(block)
                is StudyBlock.Quote -> writer.paragraph("> ${block.text.raw}")
                is StudyBlock.Paragraph -> writer.paragraph(block.text.raw)
            }
        }
        writer.finish()
        target.outputStream().buffered().use { pdf.writeTo(it) }
        pdf.close()
    }

    private class Writer(private val pdf: PdfDocument) {
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = BODY_SIZE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        private val headingPaint = Paint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var y = MARGIN

        private fun ensurePage() {
            if (page == null) {
                pageNumber += 1
                page = pdf.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                y = MARGIN
            }
        }

        fun heading(text: String, size: Float = 20f) {
            val paint = Paint(headingPaint).apply { textSize = max(size, 14f) }
            drawWrapped(text, paint, spacingAfter = 10f)
        }

        fun paragraph(text: String) {
            if (text.isBlank()) return
            drawWrapped(text, bodyPaint, spacingAfter = 8f)
        }

        fun verse(block: StudyBlock.Verse) {
            val versions = block.displayedVersions()
            paragraph(block.referenceLabel())
            if (versions.size <= 1) {
                paragraph(block.contents[versions.firstOrNull()].orEmpty())
                return
            }
            val columnWidth = (CONTENT_WIDTH - 16f) / 2f
            val left = block.contents[versions[0]].orEmpty()
            val right = block.contents[versions[1]].orEmpty()
            ensurePage()
            if (y + LINE_HEIGHT * 2 > PAGE_HEIGHT - MARGIN) newPage()
            page!!.canvas.drawText(versions[0].uppercase(), MARGIN, y, headingPaint)
            page!!.canvas.drawText(versions[1].uppercase(), MARGIN + columnWidth + 16f, y, headingPaint)
            y += LINE_HEIGHT
            val leftLines = wrap(left, bodyPaint, columnWidth)
            val rightLines = wrap(right, bodyPaint, columnWidth)
            val count = max(leftLines.size, rightLines.size)
            repeat(count) { index ->
                if (y + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) newPage()
                page!!.canvas.drawText(leftLines.getOrNull(index).orEmpty(), MARGIN, y, bodyPaint)
                page!!.canvas.drawText(
                    rightLines.getOrNull(index).orEmpty(),
                    MARGIN + columnWidth + 16f,
                    y,
                    bodyPaint,
                )
                y += LINE_HEIGHT
            }
            y += 8f
        }

        private fun drawWrapped(text: String, paint: Paint, spacingAfter: Float) {
            val lines = text.replace("\r", "").split("\n").flatMap { wrap(it, paint, CONTENT_WIDTH) }
            lines.forEach { line ->
                if (y + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) newPage()
                ensurePage()
                page!!.canvas.drawText(line, MARGIN, y, paint)
                y += LINE_HEIGHT
            }
            y += spacingAfter
        }

        private fun wrap(text: String, paint: Paint, width: Float): List<String> {
            if (text.isBlank()) return listOf("")
            val words = text.split(Regex("\\s+"))
            val lines = mutableListOf<String>()
            var current = StringBuilder()
            words.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= width || current.isEmpty()) {
                    current = StringBuilder(candidate)
                } else {
                    lines += current.toString()
                    current = StringBuilder(word)
                }
            }
            if (current.isNotEmpty()) lines += current.toString()
            return lines
        }

        private fun newPage() {
            page?.let(pdf::finishPage)
            page = null
            ensurePage()
        }

        fun finish() {
            page?.let(pdf::finishPage)
            page = null
        }
    }
}

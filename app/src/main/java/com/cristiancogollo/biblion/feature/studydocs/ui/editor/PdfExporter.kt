package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    data class PdfConfig(
        val pageWidthPx: Int = 595,   // A4 width at 72 DPI
        val pageHeightPx: Int = 842,  // A4 height at 72 DPI
        val marginPx: Int = 56,       // ~20mm
        val fontSizeSp: Float = 11f,
        val lineHeightSp: Float = 15f,
    )

    fun export(
        context: Context,
        doc: StudyDoc,
        config: PdfConfig = PdfConfig(),
    ): File {
        val document = PdfDocument()
        val paint = TextPaint().apply {
            textSize = config.fontSizeSp * (context.resources.displayMetrics.scaledDensity)
            isAntiAlias = true
            color = Color.BLACK
        }
        val contentWidth = config.pageWidthPx - config.marginPx * 2

        var currentPage = 1
        var y = config.marginPx.toFloat()
        var currentPageObj: PdfDocument.Page? = null
        var pageInfo: PdfDocument.PageInfo? = null

        fun startNewPage(): Canvas {
            currentPageObj?.let { page ->
                document.finishPage(page)
            }
            pageInfo = PdfDocument.PageInfo.Builder(config.pageWidthPx, config.pageHeightPx, currentPage).create()
            currentPage++
            val page = document.startPage(pageInfo!!)
            currentPageObj = page
            y = config.marginPx.toFloat()
            return page.canvas
        }

        var canvas = startNewPage()

        fun checkPageBreak(requiredHeight: Float) {
            if (y + requiredHeight > config.pageHeightPx - config.marginPx) {
                canvas = startNewPage()
            }
        }

        fun drawStyledText(text: StyledText, basePaint: TextPaint, maxWidth: Int, x: Float) {
            val plain = text.plain()
            if (plain.isBlank()) {
                y += basePaint.textSize * 1.2f
                return
            }

            val spans = text.ranges
            val spanPaint = TextPaint(basePaint)

            if (spans.isEmpty()) {
                val layout = StaticLayout.Builder.obtain(plain, 0, plain.length, spanPaint, maxWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.1f)
                    .build()

                checkPageBreak(layout.height.toFloat())
                canvas.save()
                canvas.translate(x, y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height.toFloat()
            } else {
                var start = 0
                for (range in spans) {
                    val end = range.endExclusive.coerceAtMost(plain.length)
                    if (start < end) {
                        val segment = plain.substring(start, end)
                        val segPaint = TextPaint(spanPaint)

                        if (range.bold) segPaint.isFakeBoldText = true
                        if (range.italic) segPaint.textSkewX = -0.1f
                        if (range.strikethrough) segPaint.isStrikeThruText = true
                        if (range.underline) segPaint.isUnderlineText = true
                        if (range.color != null) segPaint.color = range.color.toInt()
                        if (range.fontSizeSp != null) segPaint.textSize = range.fontSizeSp * context.resources.displayMetrics.scaledDensity

                        val layout = StaticLayout.Builder.obtain(segment, 0, segment.length, segPaint, maxWidth)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0f, 1.1f)
                            .build()

                        checkPageBreak(layout.height.toFloat())
                        canvas.save()
                        canvas.translate(x, y)
                        layout.draw(canvas)
                        canvas.restore()
                        y += layout.height.toFloat()
                    }
                    start = end
                }
                if (start < plain.length) {
                    val remaining = plain.substring(start)
                    val layout = StaticLayout.Builder.obtain(remaining, 0, remaining.length, spanPaint, maxWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.1f)
                        .build()

                    checkPageBreak(layout.height.toFloat())
                    canvas.save()
                    canvas.translate(x, y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += layout.height.toFloat()
                }
            }
        }

        fun drawPlainText(text: String, paint: TextPaint, maxWidth: Int, x: Float) {
            if (text.isBlank()) {
                y += paint.textSize * 1.2f
                return
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .build()

            checkPageBreak(layout.height.toFloat())
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height.toFloat()
        }

        // Title
        val titlePaint = TextPaint(paint).apply {
            textSize = 18f * context.resources.displayMetrics.scaledDensity
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        if (doc.title.isNotBlank()) {
            drawPlainText(doc.title, titlePaint, contentWidth, config.marginPx.toFloat())
            y += 8f
        }

        // Blocks
        for (block in doc.blocks) {
            when (block) {
                is StudyBlock.Paragraph -> {
                    drawStyledText(block.text, paint, contentWidth, config.marginPx.toFloat())
                    y += 4f
                }
                is StudyBlock.Heading -> {
                    val headingPaint = TextPaint(paint).apply {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = when (block.level) {
                            1 -> 22f
                            2 -> 18f
                            3 -> 14f
                            else -> 12f
                        } * context.resources.displayMetrics.scaledDensity
                    }
                    drawStyledText(block.text, headingPaint, contentWidth, config.marginPx.toFloat())
                    y += 2f
                }
                is StudyBlock.Quote -> {
                    val quotePaint = TextPaint(paint).apply {
                        color = Color.GRAY
                    }
                    checkPageBreak(30f)
                    drawPlainText("\u201C", TextPaint(paint).apply {
                        textSize = 22f * context.resources.displayMetrics.scaledDensity
                        color = Color.GRAY
                    }, contentWidth, config.marginPx.toFloat())
                    drawStyledText(block.text, paint, contentWidth, config.marginPx.toFloat() + 16f)
                    if (!block.attribution.isNullOrBlank()) {
                        val attrPaint = TextPaint(paint).apply {
                            textSize = 9f * context.resources.displayMetrics.scaledDensity
                            color = Color.GRAY
                        }
                        drawPlainText("\u2014 ${block.attribution}", attrPaint, contentWidth, config.marginPx.toFloat() + 16f)
                    }
                    y += 4f
                }
                is StudyBlock.BulletList -> {
                    for (item in block.items) {
                        drawStyledText(item, paint, contentWidth - 16, config.marginPx.toFloat() + 16)
                        y += 2f
                    }
                }
                is StudyBlock.NumberedList -> {
                    block.items.forEachIndexed { index, item ->
                        val numPaint = TextPaint(paint)
                        drawStyledText(item, paint, contentWidth - 16, config.marginPx.toFloat() + 16)
                        y += 2f
                    }
                }
                is StudyBlock.Note -> {
                    val notePaint = TextPaint(paint).apply {
                        color = Color.parseColor("#1976D2")
                    }
                    drawPlainText("[Nota]", TextPaint(notePaint).apply {
                        textSize = 9f * context.resources.displayMetrics.scaledDensity
                    }, contentWidth, config.marginPx.toFloat())
                    drawStyledText(block.text, paint, contentWidth, config.marginPx.toFloat())
                    y += 4f
                }
                is StudyBlock.Reflection -> {
                    val refPaint = TextPaint(paint).apply {
                        color = Color.parseColor("#7B1FA2")
                    }
                    if (!block.prompt.isNullOrBlank()) {
                        drawPlainText(block.prompt, TextPaint(refPaint).apply {
                            textSize = 9f * context.resources.displayMetrics.scaledDensity
                        }, contentWidth, config.marginPx.toFloat())
                    }
                    drawStyledText(block.text, paint, contentWidth, config.marginPx.toFloat())
                    y += 4f
                }
                is StudyBlock.Callout -> {
                    val callPaint = TextPaint(paint).apply {
                        color = Color.parseColor("#E65100")
                    }
                    drawPlainText("[! Destacado]", TextPaint(callPaint).apply {
                        textSize = 9f * context.resources.displayMetrics.scaledDensity
                    }, contentWidth, config.marginPx.toFloat())
                    drawStyledText(block.text, paint, contentWidth, config.marginPx.toFloat())
                    y += 4f
                }
                is StudyBlock.Verse -> {
                    val refPaint = TextPaint(paint).apply {
                        color = Color.parseColor("#1976D2")
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 9f * context.resources.displayMetrics.scaledDensity
                    }
                    drawPlainText(block.reference.displayShort(), refPaint, contentWidth, config.marginPx.toFloat())
                    val versePaint = TextPaint(paint).apply {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    }
                    drawStyledText(block.primaryText, versePaint, contentWidth, config.marginPx.toFloat())
                    if (block.compareVersion != null && block.compareText != null) {
                        val cmpPaint = TextPaint(paint).apply {
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                        }
                        drawPlainText("${block.compareVersion}:", TextPaint(refPaint), contentWidth, config.marginPx.toFloat())
                        drawStyledText(block.compareText, cmpPaint, contentWidth, config.marginPx.toFloat())
                    }
                    y += 4f
                }
                is StudyBlock.Divider -> {
                    y += 4f
                    canvas.drawLine(
                        config.marginPx.toFloat(), y,
                        (config.pageWidthPx - config.marginPx).toFloat(), y,
                        Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
                    )
                    y += 8f
                }
                is StudyBlock.PageBreak -> {
                    canvas = startNewPage()
                }
                is StudyBlock.TodoList -> {
                    for (item in block.items) {
                        val checkMark = if (item.checked) "[x]" else "[ ]"
                        drawPlainText("$checkMark ", paint, 20, config.marginPx.toFloat())
                        drawStyledText(item.text, paint, contentWidth - 20, config.marginPx.toFloat() + 16)
                        y += 2f
                    }
                }
                else -> { /* Table, ColumnLayout, Comment — skip for now */ }
            }
        }

        // Finish last page
        currentPageObj?.let { document.finishPage(it) }

        // Write to file
        val file = File(context.cacheDir, "${doc.title.ifBlank { "enseñanza" }}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }
}

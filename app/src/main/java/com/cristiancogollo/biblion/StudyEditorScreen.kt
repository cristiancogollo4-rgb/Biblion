package com.cristiancogollo.biblion

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private class CitationSpan(
    val citationId: String,
    val reference: String,
    val verseText: String,
    var version: String,
    private val onPress: (CitationSpan, IntOffset) -> Unit
) : ClickableSpan() {
    override fun onClick(widget: View) {
        val editText = widget as? EditText ?: return
        val text = editText.text as? Spannable ?: return
        val start = text.getSpanStart(this)
        val line = editText.layout?.getLineForOffset(start) ?: 0
        val x = (editText.layout?.getPrimaryHorizontal(start) ?: 0f).roundToInt()
        val y = (editText.layout?.getLineBottom(line) ?: 0)
        onPress(this, IntOffset(x, y))
    }
}

private fun applySpan(editText: EditText, block: (Editable, Int, Int) -> Unit) {
    val start = editText.selectionStart.coerceAtLeast(0)
    val end = editText.selectionEnd.coerceAtLeast(0)
    if (start == end) return
    val min = minOf(start, end)
    val max = maxOf(start, end)
    block(editText.text, min, max)
}

private fun extractRichSpans(editable: Editable): List<RichSpanRecord> {
    val output = mutableListOf<RichSpanRecord>()

    editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { span ->
        val start = editable.getSpanStart(span)
        val end = editable.getSpanEnd(span)
        if (start >= 0 && end > start) {
            val type = if (span.style == Typeface.BOLD) "bold" else if (span.style == Typeface.ITALIC) "italic" else "style"
            output += RichSpanRecord(type = type, start = start, end = end)
        }
    }

    editable.getSpans(0, editable.length, BackgroundColorSpan::class.java).forEach { span ->
        val start = editable.getSpanStart(span)
        val end = editable.getSpanEnd(span)
        if (start >= 0 && end > start) {
            output += RichSpanRecord(type = "highlight", start = start, end = end, value = span.backgroundColor.toString())
        }
    }

    editable.getSpans(0, editable.length, RelativeSizeSpan::class.java).forEach { span ->
        val start = editable.getSpanStart(span)
        val end = editable.getSpanEnd(span)
        if (start >= 0 && end > start) {
            output += RichSpanRecord(type = "size_relative", start = start, end = end, value = span.sizeChange.toString())
        }
    }

    editable.getSpans(0, editable.length, AbsoluteSizeSpan::class.java).forEach { span ->
        val start = editable.getSpanStart(span)
        val end = editable.getSpanEnd(span)
        if (start >= 0 && end > start) {
            output += RichSpanRecord(type = "size_abs", start = start, end = end, value = span.size.toString())
        }
    }

    editable.getSpans(0, editable.length, CitationSpan::class.java).forEach { span ->
        val start = editable.getSpanStart(span)
        val end = editable.getSpanEnd(span)
        if (start >= 0 && end > start) {
            output += RichSpanRecord(
                type = "bible_reference",
                start = start,
                end = end,
                citationId = span.citationId,
                citationReference = span.reference,
                citationText = span.verseText,
                citationVersion = span.version
            )
        }
    }

    return output
}

private fun restoreFromDocumentJson(
    documentJson: String,
    onCitationPressed: (CitationSpan, IntOffset) -> Unit
): SpannableStringBuilder {
    if (documentJson.isBlank()) return SpannableStringBuilder("")

    return try {
        val root = JSONObject(documentJson)
        val text = root.optString("text", "")
        val spans = root.optJSONArray("spans") ?: JSONArray()
        val editable = SpannableStringBuilder(text)

        for (i in 0 until spans.length()) {
            val item = spans.optJSONObject(i) ?: continue
            val type = item.optString("type")
            val start = item.optInt("start", -1)
            val end = item.optInt("end", -1)
            if (start < 0 || end <= start || end > editable.length) continue

            when (type) {
                "bold" -> editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                "italic" -> editable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                "highlight" -> {
                    val defaultHighlight = android.graphics.Color.parseColor("#FFF2A8")
                    val value = item.optString("value", defaultHighlight.toString())
                    editable.setSpan(BackgroundColorSpan(value.toIntOrNull() ?: defaultHighlight), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "size_relative" -> {
                    val value = item.optString("value", "1.1").toFloatOrNull() ?: 1.1f
                    editable.setSpan(RelativeSizeSpan(value), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "size_abs" -> {
                    val value = item.optString("value", "24").toIntOrNull() ?: 24
                    editable.setSpan(AbsoluteSizeSpan(value, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "bible_reference" -> {
                    val span = CitationSpan(
                        citationId = item.optString("citationId", ""),
                        reference = item.optString("citationReference", "Cita"),
                        verseText = item.optString("citationText", ""),
                        version = item.optString("citationVersion", "default"),
                        onPress = onCitationPressed
                    )
                    editable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editable.setSpan(
                        BackgroundColorSpan(android.graphics.Color.parseColor("#D8E8FF")),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        editable
    } catch (_: Exception) {
        SpannableStringBuilder("")
    }
}

@Composable
fun StudyEditorScreen(
    viewModel: StudyViewModel,
    onClose: () -> Unit
) {
    var selectedCitation by remember { mutableStateOf<CitationSpan?>(null) }
    var citationMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    var selectionStart by remember { mutableIntStateOf(0) }
    var selectionEnd by remember { mutableIntStateOf(0) }
    var editorRef by remember { mutableStateOf<EditText?>(null) }
    val density = LocalDensity.current

    fun persistDocument() {
        editorRef?.let { editor ->
            viewModel.updateDocument(editor.text.toString(), extractRichSpans(editor.text))
        }
    }

    fun insertCitation(request: CitationInsertRequest) {
        val editor = editorRef ?: return
        val editable = editor.text
        val at = editor.selectionStart.coerceAtLeast(0)
        val inlineText = " ${request.reference} "
        editable.insert(at, inlineText)

        val citationSpan = CitationSpan(
            citationId = request.id,
            reference = request.reference,
            verseText = request.text,
            version = request.version,
            onPress = { span, offset ->
                selectedCitation = span
                citationMenuOffset = offset
            }
        )

        editable.setSpan(citationSpan, at, at + inlineText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        editable.setSpan(
            BackgroundColorSpan(android.graphics.Color.parseColor("#D8E8FF")),
            at,
            at + inlineText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        editable.setSpan(StyleSpan(Typeface.BOLD), at, at + inlineText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        editor.setSelection((at + inlineText.length).coerceAtMost(editable.length))
        persistDocument()
    }

    LaunchedEffect(viewModel.pendingCitationInsertions.size, editorRef) {
        if (editorRef != null && viewModel.pendingCitationInsertions.isNotEmpty()) {
            viewModel.consumePendingCitations().forEach { insertCitation(it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUADERNO DE ESTUDIO",
                    style = MaterialTheme.typography.labelLarge,
                    color = BiblionNavy
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.foundation.text.BasicTextField(
                value = viewModel.noteTitle,
                onValueChange = { viewModel.updateTitle(it) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    color = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (viewModel.noteTitle.isEmpty()) {
                        Text("Título de la enseñanza...", color = Color.Gray, fontSize = 20.sp)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.text.BasicTextField(
                value = viewModel.baseReference,
                onValueChange = { viewModel.updateBaseReference(it) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    color = Color(0xFF1A3A6E)
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (viewModel.baseReference.isEmpty()) {
                        Text("Versículo o texto base...", color = Color.Gray, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray)

            if (viewModel.citations.isNotEmpty()) {
                Text(
                    text = "Versículos citados",
                    style = MaterialTheme.typography.labelLarge,
                    color = BiblionNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.citations.forEach { citation ->
                        AssistChip(
                            onClick = {
                                selectedCitation = CitationSpan(
                                    citationId = "",
                                    reference = citation.reference,
                                    verseText = citation.text,
                                    version = "default",
                                    onPress = { _, _ -> }
                                )
                            },
                            label = { Text(citation.reference) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectionStart != selectionEnd) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = MaterialTheme.shapes.medium)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Pintar", color = BiblionNavy, modifier = Modifier.clickable {
                        editorRef?.let { editor ->
                            applySpan(editor) { editable, start, end ->
                                editable.setSpan(
                                    BackgroundColorSpan(android.graphics.Color.parseColor("#FFF2A8")),
                                    start,
                                    end,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                            persistDocument()
                        }
                    })
                    Text("Negrita", color = BiblionNavy, modifier = Modifier.clickable {
                        editorRef?.let { editor ->
                            applySpan(editor) { editable, start, end ->
                                editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            persistDocument()
                        }
                    })
                    Text("Cursiva", color = BiblionNavy, modifier = Modifier.clickable {
                        editorRef?.let { editor ->
                            applySpan(editor) { editable, start, end ->
                                editable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            persistDocument()
                        }
                    })
                    Text("A+", color = BiblionNavy, modifier = Modifier.clickable {
                        editorRef?.let { editor ->
                            applySpan(editor) { editable, start, end ->
                                editable.setSpan(RelativeSizeSpan(1.2f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            persistDocument()
                        }
                    })
                    Text("A-", color = BiblionNavy, modifier = Modifier.clickable {
                        editorRef?.let { editor ->
                            applySpan(editor) { editable, start, end ->
                                editable.setSpan(RelativeSizeSpan(0.9f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            persistDocument()
                        }
                    })
                    Text("H1", color = BiblionNavy, modifier = Modifier.clickable {
                        editorRef?.let { editor ->
                            applySpan(editor) { editable, start, end ->
                                editable.setSpan(AbsoluteSizeSpan(30, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            persistDocument()
                        }
                    })
                    Text("H2", color = BiblionNavy, modifier = Modifier.clickable {
                        editorRef?.let { editor ->
                            applySpan(editor) { editable, start, end ->
                                editable.setSpan(AbsoluteSizeSpan(24, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            persistDocument()
                        }
                    })
                    Text("H3", color = BiblionNavy, modifier = Modifier.clickable {
                        editorRef?.let { editor ->
                            applySpan(editor) { editable, start, end ->
                                editable.setSpan(AbsoluteSizeSpan(20, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            persistDocument()
                        }
                    })
                    Text("Limpiar", color = BiblionNavy, modifier = Modifier.clickable {
                        editorRef?.let { editor ->
                            applySpan(editor) { editable, start, end ->
                                editable.getSpans(start, end, Any::class.java).forEach { span ->
                                    if (span is StyleSpan || span is RelativeSizeSpan || span is AbsoluteSizeSpan || span is BackgroundColorSpan) {
                                        editable.removeSpan(span)
                                    }
                                }
                            }
                            persistDocument()
                        }
                    })
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            AndroidView(
                factory = { context ->
                    EditText(context).apply {
                        setTextColor(android.graphics.Color.DKGRAY)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        textSize = viewModel.noteFontSize
                        typeface = Typeface.SERIF
                        movementMethod = LinkMovementMethod.getInstance()
                        setPadding(6, 6, 6, 6)

                        val restored = restoreFromDocumentJson(viewModel.noteDocumentJson) { span, offset ->
                            selectedCitation = span
                            citationMenuOffset = offset
                        }
                        if (restored.isNotEmpty()) {
                            setText(restored)
                        } else {
                            setText(viewModel.noteContent)
                        }

                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                            override fun afterTextChanged(s: Editable?) {
                                if (s != null) {
                                    viewModel.updateDocument(s.toString(), extractRichSpans(s))
                                }
                            }
                        })

                        setOnSelectionChangedListener { selStart, selEnd ->
                            selectionStart = selStart
                            selectionEnd = selEnd
                        }

                        editorRef = this
                    }
                },
                update = { editorRef = it },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color.White, MaterialTheme.shapes.medium)
            )
        }

        selectedCitation?.let { citation ->
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(
                    x = citationMenuOffset.x,
                    y = citationMenuOffset.y + with(density) { 8.dp.roundToPx() }
                ),
                onDismissRequest = { selectedCitation = null }
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White, shape = MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(citation.reference, color = BiblionNavy, style = MaterialTheme.typography.labelLarge)
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Cambiar versión",
                            tint = BiblionNavy,
                            modifier = Modifier.clickable {
                                editorRef?.let {
                                    Toast.makeText(it.context, "Próximamente múltiples versiones", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.height(120.dp).verticalScroll(rememberScrollState())) {
                        Text(citation.verseText, color = Color(0xFF303030))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Eliminar cita",
                        color = Color(0xFFB00020),
                        modifier = Modifier.clickable {
                            editorRef?.text?.let { editable ->
                                editable.getSpans(0, editable.length, CitationSpan::class.java).forEach { span ->
                                    if (span.citationId == citation.citationId || span.reference == citation.reference) {
                                        val start = editable.getSpanStart(span)
                                        val end = editable.getSpanEnd(span)
                                        editable.removeSpan(span)
                                        if (start >= 0 && end > start && end <= editable.length) {
                                            editable.delete(start, end)
                                        }
                                    }
                                }
                                persistDocument()
                            }
                            selectedCitation = null
                        }
                    )
                }
            }
        }
    }
}

private fun EditText.setOnSelectionChangedListener(onSelectionChanged: (Int, Int) -> Unit) {
    val self = this
    self.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
        override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = true
        override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = true
        override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean = false
        override fun onDestroyActionMode(mode: android.view.ActionMode?) = Unit
    }

    self.viewTreeObserver.addOnGlobalLayoutListener {
        onSelectionChanged(self.selectionStart.coerceAtLeast(0), self.selectionEnd.coerceAtLeast(0))
    }
}

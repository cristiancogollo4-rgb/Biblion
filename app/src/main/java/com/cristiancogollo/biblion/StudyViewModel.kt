package com.cristiancogollo.biblion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StudyCitation(
    val reference: String,
    val text: String,
    val previousText: String?,
    val nextText: String?
)

data class CitationInsertRequest(
    val id: String,
    val reference: String,
    val text: String,
    val version: String = "default"
)

data class RichSpanRecord(
    val type: String,
    val start: Int,
    val end: Int,
    val value: String? = null,
    val citationId: String? = null,
    val citationReference: String? = null,
    val citationText: String? = null,
    val citationVersion: String? = null
)

class StudyViewModel : ViewModel() {
    var noteTitle by mutableStateOf("")
    var baseReference by mutableStateOf("")
    var noteFontSize by mutableStateOf(16f)

    // Persisted rich document JSON (text + spans structured)
    var noteDocumentJson by mutableStateOf("")

    // Legacy plain content fallback
    var noteContent by mutableStateOf("")

    var citations by mutableStateOf<List<StudyCitation>>(emptyList())
        private set

    val pendingCitationInsertions = mutableStateListOf<CitationInsertRequest>()

    fun updateTitle(newTitle: String) {
        noteTitle = newTitle
    }

    fun updateBaseReference(newReference: String) {
        baseReference = newReference
    }

    fun updateFontSize(newSize: Float) {
        noteFontSize = newSize.coerceIn(12f, 30f)
    }

    fun addCitation(
        reference: String,
        text: String,
        previousText: String?,
        nextText: String?
    ) {
        citations = citations + StudyCitation(reference, text, previousText, nextText)
        pendingCitationInsertions += CitationInsertRequest(
            id = UUID.randomUUID().toString(),
            reference = reference,
            text = text
        )
    }

    fun consumePendingCitations(): List<CitationInsertRequest> {
        val items = pendingCitationInsertions.toList()
        pendingCitationInsertions.clear()
        return items
    }

    fun updateDocument(text: String, spans: List<RichSpanRecord>) {
        noteContent = text
        noteDocumentJson = serializeDocument(text, spans)
    }

    private fun serializeDocument(text: String, spans: List<RichSpanRecord>): String {
        val root = JSONObject()
        root.put("type", "study_document")
        root.put("version", 1)
        root.put("text", text)

        val spansArray = JSONArray()
        spans.forEach { span ->
            val item = JSONObject()
            item.put("type", span.type)
            item.put("start", span.start)
            item.put("end", span.end)
            span.value?.let { item.put("value", it) }
            span.citationId?.let { item.put("citationId", it) }
            span.citationReference?.let { item.put("citationReference", it) }
            span.citationText?.let { item.put("citationText", it) }
            span.citationVersion?.let { item.put("citationVersion", it) }
            spansArray.put(item)
        }
        root.put("spans", spansArray)
        return root.toString()
    }
}

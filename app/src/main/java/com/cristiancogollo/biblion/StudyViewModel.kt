package com.cristiancogollo.biblion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class StudyCitation(
    val reference: String,
    val text: String,
    val previousText: String?,
    val nextText: String?
)

class StudyViewModel : ViewModel() {
    var noteTitle by mutableStateOf("")
    var noteContent by mutableStateOf("")
    var baseReference by mutableStateOf("")
    var citations by mutableStateOf<List<StudyCitation>>(emptyList())
    var noteFontSize by mutableStateOf(16f)

    fun updateTitle(newTitle: String) {
        noteTitle = newTitle
    }

    fun updateContent(newContent: String) {
        noteContent = newContent
    }

    fun updateBaseReference(newReference: String) {
        baseReference = newReference
    }

    fun addCitation(
        reference: String,
        text: String,
        previousText: String?,
        nextText: String?
    ) {
        citations = citations + StudyCitation(reference, text, previousText, nextText)
        noteContent = (noteContent.trimEnd() + "\n\n[$reference]\n\"$text\"\n").trimStart()
    }

    fun updateFontSize(newSize: Float) {
        noteFontSize = newSize.coerceIn(12f, 30f)
    }
}

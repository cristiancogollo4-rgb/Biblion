package com.cristiancogollo.biblion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class StudyViewModel : ViewModel() {
    var noteTitle by mutableStateOf("")
    var noteContent by mutableStateOf("")

    fun updateTitle(newTitle: String) {
        noteTitle = newTitle
    }

    fun updateContent(newContent: String) {
        noteContent = newContent
    }
}
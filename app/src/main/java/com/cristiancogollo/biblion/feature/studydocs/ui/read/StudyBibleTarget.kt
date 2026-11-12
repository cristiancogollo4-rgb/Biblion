package com.cristiancogollo.biblion.feature.studydocs.ui.read

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

internal data class StudyBibleTarget(
    val bookName: String,
    val chapter: Int,
    val verseNumbers: Set<Int> = emptySet(),
    val preferredVersion: String? = null,
) {
    fun matches(bookName: String, chapter: Int): Boolean =
        this.chapter == chapter && this.bookName.equals(bookName, ignoreCase = true)
}

internal fun StudyBlock.Verse.toStudyBibleTarget(): StudyBibleTarget = StudyBibleTarget(
    bookName = bookId,
    chapter = chapter,
    verseNumbers = selectedVerseNumbers().toSet(),
    preferredVersion = sourceVersion,
)

internal val studyBibleBooks = listOf(
    "Genesis", "Exodo", "Levitico", "Numeros", "Deuteronomio", "Josue", "Jueces", "Rut",
    "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", "1 Cronicas", "2 Cronicas", "Esdras",
    "Nehemias", "Ester", "Job", "Salmos", "Proverbios", "Eclesiastes", "Cantares",
    "Isaias", "Jeremias", "Lamentaciones", "Ezequiel", "Daniel", "Oseas", "Joel", "Amos",
    "Abdias", "Jonas", "Miqueas", "Nahum", "Habacuc", "Sofonias", "Hageo", "Zacarias",
    "Malaquias", "Mateo", "Marcos", "Lucas", "Juan", "Hechos", "Romanos", "1 Corintios",
    "2 Corintios", "Galatas", "Efesios", "Filipenses", "Colosenses", "1 Tesalonicenses",
    "2 Tesalonicenses", "1 Timoteo", "2 Timoteo", "Tito", "Filemon", "Hebreos",
    "Santiago", "1 Pedro", "2 Pedro", "1 Juan", "2 Juan", "3 Juan", "Judas", "Apocalipsis",
)

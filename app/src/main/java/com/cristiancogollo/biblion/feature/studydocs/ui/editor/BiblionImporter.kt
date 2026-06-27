package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.content.Context
import android.net.Uri
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc

object BiblionImporter {

    suspend fun importFromUri(
        context: Context,
        uri: Uri,
    ): Result<StudyDoc> = runCatching {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo abrir el archivo")

        val json = inputStream.bufferedReader().use { it.readText() }
        val doc = com.cristiancogollo.biblion.feature.studydocs.data.StudyDocJson.decode(json)
            ?: throw IllegalArgumentException("Formato .biblion no valido")

        val newDoc = doc.copy(id = DocId.generate())

        val database = StudyDocDatabase.getInstance(context)
        val repository = StudyDocRepository(database.studyDocDao())
        repository.save(newDoc)

        newDoc
    }
}

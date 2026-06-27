package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocJson

object ShareHelper {

    fun sharePdf(context: Context, file: java.io.File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
    }

    fun shareBiblion(context: Context, doc: StudyDoc) {
        val json = StudyDocJson.encode(doc)
        val file = java.io.File(context.cacheDir, "${doc.title.ifBlank { "enseñanza" }}.biblion")
        file.writeText(json)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, doc.title.ifBlank { "Enseñanza Biblion" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir enseñanza"))
    }
}

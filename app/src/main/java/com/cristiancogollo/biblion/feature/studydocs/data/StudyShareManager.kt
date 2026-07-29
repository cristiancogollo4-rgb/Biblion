package com.cristiancogollo.biblion.feature.studydocs.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import java.io.File

enum class StudyShareFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    BIB("bib", BIB_STUDY_MIME),
}

object StudyShareManager {
    fun share(context: Context, document: StudyDoc, format: StudyShareFormat) {
        val file = createExportFile(context, document, format)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, document.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir ensenanza"))
    }

    fun importUri(context: Context, uri: Uri): ImportedBibStudy =
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "No se pudo leer el archivo Biblion" }
            BibStudyPackage.read(input)
        }

    private fun createExportFile(
        context: Context,
        document: StudyDoc,
        format: StudyShareFormat,
    ): File {
        val directory = File(context.cacheDir, "shared_studies").apply { mkdirs() }
        val safeTitle = document.title
            .trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "ensenanza" }
        val file = File(directory, "$safeTitle.${format.extension}")
        when (format) {
            StudyShareFormat.PDF -> StudyPdfExporter.export(document, file)
            StudyShareFormat.BIB -> BibStudyPackage.export(document, file)
        }
        return file
    }
}

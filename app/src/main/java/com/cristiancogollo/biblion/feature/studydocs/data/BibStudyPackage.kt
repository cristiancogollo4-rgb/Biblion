package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val BIB_STUDY_MIME = "application/vnd.biblion.study+json"
const val BIB_STUDY_EXTENSION = ".bib"

@Serializable
data class BibStudyManifest(
    val format: String = "biblion-study",
    val formatVersion: Int = 1,
    val contentHash: String,
    val exportedAt: Long,
    val title: String,
    val tags: List<String> = emptyList(),
)

data class ImportedBibStudy(
    val document: StudyDoc,
    val manifest: BibStudyManifest,
    val verifiedHash: Boolean,
)

object BibStudyPackage {
    private const val MANIFEST = "manifest.json"
    private const val DOCUMENT = "document.json"

    fun export(document: StudyDoc, target: File, now: Long = System.currentTimeMillis()) {
        val documentJson = StudyDocJson.encode(document)
        val manifest = BibStudyManifest(
            contentHash = StudyContentHash.sha256(documentJson),
            exportedAt = now,
            title = document.title,
            tags = document.metadata.tags,
        )
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            writeEntry(zip, MANIFEST, StudyDocJson.json.encodeToString(manifest))
            writeEntry(zip, DOCUMENT, documentJson)
        }
    }

    fun read(input: InputStream): ImportedBibStudy {
        var manifestJson: String? = null
        var documentJson: String? = null
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                when (entry.name) {
                    MANIFEST -> manifestJson = zip.readBytes().toString(Charsets.UTF_8)
                    DOCUMENT -> documentJson = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        }
        val manifest = manifestJson?.let {
            StudyDocJson.json.decodeFromString<BibStudyManifest>(it)
        } ?: error("El archivo Biblion no contiene manifest.json")
        val json = documentJson ?: error("El archivo Biblion no contiene document.json")
        val document = StudyDocJson.decode(json) ?: error("El documento Biblion no es valido")
        val actualHash = StudyContentHash.sha256(json)
        return ImportedBibStudy(
            document = document,
            manifest = manifest,
            verifiedHash = actualHash == manifest.contentHash,
        )
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, value: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(value.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}

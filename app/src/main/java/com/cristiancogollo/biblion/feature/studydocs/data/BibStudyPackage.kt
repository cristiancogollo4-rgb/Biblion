package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.ByteArrayOutputStream
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
    private const val FORMAT = "biblion-study"
    private const val FORMAT_VERSION = 1
    private const val MAX_ARCHIVE_ENTRIES = 4
    private const val MAX_MANIFEST_BYTES = 64 * 1024
    private const val MAX_DOCUMENT_BYTES = 2 * 1024 * 1024

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
        val seenEntries = mutableSetOf<String>()
        var entryCount = 0
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory) { "El archivo Biblion contiene una entrada no valida" }
                require(++entryCount <= MAX_ARCHIVE_ENTRIES) {
                    "El archivo Biblion contiene demasiadas entradas"
                }
                require(entry.name == MANIFEST || entry.name == DOCUMENT) {
                    "El archivo Biblion contiene una entrada desconocida"
                }
                require(seenEntries.add(entry.name)) {
                    "El archivo Biblion contiene entradas duplicadas"
                }
                when (entry.name) {
                    MANIFEST -> manifestJson =
                        zip.readEntryLimited(MAX_MANIFEST_BYTES).toString(Charsets.UTF_8)
                    DOCUMENT -> documentJson =
                        zip.readEntryLimited(MAX_DOCUMENT_BYTES).toString(Charsets.UTF_8)
                }
                zip.closeEntry()
            }
        }
        val manifest = manifestJson?.let {
            StudyDocJson.json.decodeFromString<BibStudyManifest>(it)
        } ?: error("El archivo Biblion no contiene manifest.json")
        require(manifest.format == FORMAT) { "El formato del archivo Biblion no es compatible" }
        require(manifest.formatVersion == FORMAT_VERSION) {
            "La version del archivo Biblion no es compatible"
        }
        val json = documentJson ?: error("El archivo Biblion no contiene document.json")
        val document = StudyDocJson.decode(json) ?: error("El documento Biblion no es valido")
        require(document.blocks.size <= 2_000) {
            "El documento Biblion contiene demasiados bloques"
        }
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

    private fun ZipInputStream.readEntryLimited(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "El archivo Biblion supera el tamano permitido" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}

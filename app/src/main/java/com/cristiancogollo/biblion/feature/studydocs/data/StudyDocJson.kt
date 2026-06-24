package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object StudyDocJson {

    val json: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(doc: StudyDoc): String = json.encodeToString(doc)

    fun decode(payload: String): StudyDoc? = runCatching { json.decodeFromString<StudyDoc>(payload) }.getOrNull()

    fun encodeBlock(block: StudyBlock): String = json.encodeToString(StudyBlock.serializer(), block)

    fun decodeBlock(payload: String): StudyBlock? = runCatching {
        json.decodeFromString(StudyBlock.serializer(), payload)
    }.getOrNull()
}

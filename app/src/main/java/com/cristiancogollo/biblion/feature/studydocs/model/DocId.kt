package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class DocId(val value: String) {
    companion object {
        fun generate(): DocId = DocId(java.util.UUID.randomUUID().toString())
    }
}

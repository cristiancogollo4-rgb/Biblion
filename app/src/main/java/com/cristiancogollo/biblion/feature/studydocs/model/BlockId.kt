package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class BlockId(val value: String) {
    companion object {
        fun generate(): BlockId = BlockId(java.util.UUID.randomUUID().toString())
    }
}

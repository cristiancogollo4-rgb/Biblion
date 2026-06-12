package com.cristiancogollo.biblion

import kotlinx.serialization.Serializable

const val BIBLION_STUDY_SHARE_FORMAT = "biblion.study.v1"
const val BIBLION_STUDY_SHARE_MIME = "application/vnd.biblion.study+json"

@Serializable
data class BiblionSharedStudyFile(
    val format: String = BIBLION_STUDY_SHARE_FORMAT,
    val title: String = "",
    val remoteId: String = "",
    val updatedAt: Long = 0,
    val contentSerialized: String = ""
)

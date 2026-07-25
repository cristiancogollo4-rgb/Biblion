package com.cristiancogollo.biblion.feature.studydocs.model

/** A document becomes persistable only after the user supplies a real title. */
fun StudyDoc.hasPersistableTitle(): Boolean = title.trim().isNotEmpty()

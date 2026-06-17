package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.StudyBlockNode

/**
 * Resultado de validación de un documento de estudio.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>
) {
    val displayMessage: String
        get() = errors.joinToString("\n") { it.message }
}

/**
 * Tipos de errores de validación.
 */
sealed interface ValidationError {
    val message: String

    data class MissingTitle(override val message: String = "El título es obligatorio.") : ValidationError
    data class MissingPurposeTag(override val message: String = "Falta etiqueta de propósito.") : ValidationError
    data class MissingAudienceTag(override val message: String = "Falta etiqueta de audiencia.") : ValidationError
    data class MissingTopicTag(override val message: String = "Falta etiqueta de tema.") : ValidationError
    data class MissingStateTag(override val message: String = "Falta etiqueta de estado.") : ValidationError
    data class MultipleStateTags(override val message: String = "Debe haber exactamente una etiqueta de estado.") : ValidationError
    data class EmptyDocument(override val message: String = "El documento está vacío.") : ValidationError
}

/**
 * Validador de documentos de estudio.
 * Consolida la lógica de validación dispersa en StudyTagSelector y StudyEditorScreen.
 */
object StudyDocumentValidator {

    private val purposeTags = setOf(
        "predicacion", "devocional", "estudio-biblico", "clase", "discipulado", "formacion"
    )

    private val audienceTags = setOf(
        "jovenes", "iglesia", "lideres", "universitarios", "familias", "simpatizantes",
        "ninos", "mujeres", "hombres", "ancianos", "grupos-especiales", "pastores"
    )

    private val topicTags = setOf(
        "identidad", "fe", "gracia", "proposito", "oracion", "evangelismo",
        "servicio", "esperanza", "doctrina", "amor", "misiones", "adoracion"
    )

    private val stateTags = setOf("borrador", "en-preparacion", "finalizado")

    /**
     * Determina el grupo de una etiqueta basándose en listas conocidas.
     */
    private fun extractTagGroup(tag: String): String? {
        val normalized = tag.lowercase().trim()
            .replace(Regex("[áà]"), "a")
            .replace(Regex("[éèë]"), "e")
            .replace(Regex("[íìï]"), "i")
            .replace(Regex("[óòö]"), "o")
            .replace(Regex("[úùü]"), "u")
            .replace(Regex("[ñ]"), "n")
        return when {
            normalized in purposeTags || purposeTags.any { normalized.startsWith(it) || it.startsWith(normalized) } -> "proposito"
            normalized in audienceTags || audienceTags.any { normalized.startsWith(it) || it.startsWith(normalized) } -> "audiencia"
            normalized in topicTags || topicTags.any { normalized.startsWith(it) || it.startsWith(normalized) } -> "tema"
            normalized in stateTags -> "estado"
            else -> null
        }
    }

    /**
     * Valida los metadatos del estudio (título y etiquetas).
     */
    fun validateMetadata(title: String, tags: List<String>): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (title.isBlank()) {
            errors.add(ValidationError.MissingTitle())
        }

        val groupedTags = tags.groupBy { extractTagGroup(it) }

        if (groupedTags["proposito"].isNullOrEmpty()) {
            errors.add(ValidationError.MissingPurposeTag())
        }

        if (groupedTags["audiencia"].isNullOrEmpty()) {
            errors.add(ValidationError.MissingAudienceTag())
        }

        if (groupedTags["tema"].isNullOrEmpty()) {
            errors.add(ValidationError.MissingTopicTag())
        }

        if (groupedTags["estado"].isNullOrEmpty()) {
            errors.add(ValidationError.MissingStateTag())
        } else if (groupedTags["estado"]?.size != 1) {
            errors.add(ValidationError.MultipleStateTags())
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valida el contenido del documento.
     */
    fun validateContent(blocks: List<StudyBlockNode>): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (blocks.isEmpty()) {
            errors.add(ValidationError.EmptyDocument())
            return ValidationResult(false, errors)
        }

        val hasContent = blocks.any { block ->
            when (block) {
                is StudyBlockNode.Paragraph -> block.text.isNotBlank()
                is StudyBlockNode.Note -> block.text.isNotBlank()
                is StudyBlockNode.Reflection -> block.text.isNotBlank() || block.topic.isNotBlank()
                is StudyBlockNode.QuotedVerse -> block.primaryText.isNotBlank()
                is StudyBlockNode.Citation -> block.text.isNotBlank()
                is StudyBlockNode.RichText -> block.html.isNotBlank()
                is StudyBlockNode.Question -> block.question.isNotBlank() || block.answer.isNotBlank()
                is StudyBlockNode.TwoColumn -> block.leftText.isNotBlank() || block.rightText.isNotBlank()
                else -> false
            }
        }

        if (!hasContent) {
            errors.add(ValidationError.EmptyDocument())
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validación completa para guardado (metadatos + contenido).
     */
    fun validateForSave(
        title: String,
        tags: List<String>,
        blocks: List<StudyBlockNode>
    ): ValidationResult {
        val metadataResult = validateMetadata(title, tags)
        val contentResult = validateContent(blocks)

        val allErrors = metadataResult.errors + contentResult.errors
        return ValidationResult(allErrors.isEmpty(), allErrors)
    }

    /**
     * Valida si una etiqueta es válida según las reglas del sistema.
     */
    fun isValidTag(tag: String): Boolean {
        val normalized = tag.lowercase().trim()
        return normalized in purposeTags ||
                normalized in audienceTags ||
                normalized in topicTags ||
                normalized in stateTags ||
                normalized.isNotBlank() // Permitir etiquetas personalizadas
    }

    /**
     * Obtiene todos los grupos de etiquetas sugeridas con sus tags.
     */
    fun getSuggestedTagGroups(): Map<String, Set<String>> {
        return mapOf(
            "proposito" to purposeTags,
            "audiencia" to audienceTags,
            "tema" to topicTags,
            "estado" to stateTags
        )
    }
}

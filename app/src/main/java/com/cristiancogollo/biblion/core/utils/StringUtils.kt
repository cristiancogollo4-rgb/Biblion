package com.cristiancogollo.biblion.core.utils

import java.text.Normalizer

/**
 * Utilidades de cadena consolidadas para evitar duplicaciones.
 * Reemplaza: toPlainShareText(), toPlainReadText(), toPlainStudyText(), removeAccents()
 */
object StringUtils {

    private val htmlTagRegex = Regex("<[^>]*>")
    private val repeatedNewlineRegex = Regex("\\n{3,}")
    private val htmlEntities = mapOf(
        "&nbsp;" to " ",
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'"
    )

    /**
     * Convierte texto HTML/riqueza a texto plano limpio.
     * Reemplaza StudyDocumentEngine.toPlainStudyText()
     */
    fun String.toPlainText(): String {
        var result = this
        htmlEntities.forEach { (entity, replacement) ->
            result = result.replace(entity, replacement)
        }
        result = result.replace(htmlTagRegex, "")
        result = result.replace(repeatedNewlineRegex, "\n\n")
        return result.trim()
    }

    /**
     * Convierte a texto plano para compartir (sin formato rico).
     * Reemplaza EnsenanzaScreen.toPlainShareText()
     */
    fun String.toShareText(): String {
        return toPlainText()
            .lines()
            .joinToString("\n") { line ->
                line.trimEnd()
            }
            .trim()
    }

    /**
     * Convierte a texto plano para lectura.
     * Reemplaza StudyReadScreen.toPlainReadText()
     */
    fun String.toReadText(): String {
        return toPlainText()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Elimina acentos y caracteres diacríticos.
     * Consolida las implementaciones duplicadas en StudyAssistantRepository y StudyAssistantPanel
     */
    fun String.removeAccents(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }

    /**
     * Trunca texto a una longitud máxima con indicador de continuación.
     */
    fun String.truncate(maxLength: Int, suffix: String = "..."): String {
        if (this.length <= maxLength) return this
        return this.take(maxLength - suffix.length) + suffix
    }

    /**
     * Normaliza espacios en blanco múltiples a uno solo.
     */
    fun String.normalizeWhitespace(): String {
        return this.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Extrae las primeras N palabras de un texto.
     */
    fun String.firstWords(count: Int): String {
        return this.split(Regex("\\s+"))
            .take(count)
            .joinToString(" ")
    }

    /**
     * Verifica si el texto contiene solo caracteres en blanco o está vacío.
     */
    fun String.isReallyBlank(): Boolean {
        return this.isBlank() || this.all { it.isWhitespace() }
    }

    /**
     * Capitaliza la primera letra de cada oración.
     */
    fun String.capitalizeSentences(): String {
        return this.replace(Regex("(^|\\.\\s+)([a-záéíóúñ])")) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val char = matchResult.groupValues[2].uppercase()
            "$prefix$char"
        }
    }

    /**
     * Limpia caracteres de control no deseados.
     */
    fun String.cleanControlCharacters(): String {
        return this.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")
    }

    /**
     * Normaliza saltos de línea a formato Unix (\n).
     */
    fun String.normalizeLineEndings(): String {
        return this.replace("\r\n", "\n").replace("\r", "\n")
    }

    /**
     * Codifica caracteres especiales para uso en JSON.
     */
    fun String.escapeForJson(): String {
        return this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

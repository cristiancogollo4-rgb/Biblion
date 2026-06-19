package com.cristiancogollo.biblion.feature.bibi

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "StrongEngine"

/**
 * Fase 4: Motor de lexico original (Strong's hebreo y griego).
 *
 * Cuando el usuario pregunta "¿qué dice el original en hebreo?" o
 * "¿qué significa esta palabra en griego?", este motor busca en la
 * base de datos Strong's y genera una respuesta con la palabra original,
 * transliteracion, pronunciacion y definicion.
 *
 * Sin IA: usa plantillas conversacionales en espanol.
 */
object StrongEngine {

    /**
     * Busca una entrada de Strong's por numero.
     *
     * @param context Contexto de la app
     * @param strongsNumber Numero en formato "H1254" o "G25"
     * @return Respuesta conversacional, o null si no se encuentra
     */
    suspend fun lookupByNumber(context: Context, strongsNumber: String): String? {
        Log.d(TAG, "Buscando Strong's: $strongsNumber")

        // Normalizar: "H1254" -> "H1254", "25" + "greek" -> "G0025"
        val normalized = normalizeStrongsNumber(strongsNumber)
            ?: return null

        val dao = StrongsDatabase.getInstance(context).strongsDao()
        val entry = withContext(Dispatchers.IO) {
            dao.getByNumber(normalized)
        }

        if (entry == null) {
            Log.d(TAG, "No se encontro: $strongsNumber")
            return null
        }

        return formatStrongsResponse(entry)
    }

    /**
     * Busca entradas de Strong's por texto (transliteracion o definicion).
     *
     * @param context Contexto de la app
     * @param query Texto a buscar (ej: "agape", "berith")
     * @return Respuesta conversacional con las coincidencias
     */
    suspend fun searchByText(context: Context, query: String): String? {
        Log.d(TAG, "Buscando por texto: $query")

        val dao = StrongsDatabase.getInstance(context).strongsDao()
        val results = withContext(Dispatchers.IO) {
            dao.search(query, 5)
        }

        if (results.isEmpty()) {
            return null
        }

        val sb = StringBuilder()
        sb.append("Encontre estas palabras en el lexico original:\n\n")

        for (entry in results) {
            sb.append(formatStrongsEntry(entry))
            sb.append("\n\n")
        }

        sb.append("¿Quieres saber mas sobre alguna de estas palabras?")
        return sb.toString()
    }

    /**
     * Formatea una entrada individual de Strong's.
     */
    private fun formatStrongsEntry(entry: StrongsEntryEntity): String {
        val sb = StringBuilder()
        val langName = if (entry.language == "hebrew") "Hebreo" else "Griego"

        sb.append("${entry.lemma} (${entry.transliteration})\n")
        sb.append("Strong ${entry.strongsNumber} [$langName]\n")

        if (entry.pronunciation.isNotEmpty()) {
            sb.append("Pronunciacion: ${entry.pronunciation}\n")
        }

        if (entry.definition.isNotEmpty()) {
            sb.append("Significado: ${entry.definition.take(300)}")
            if (entry.definition.length > 300) sb.append("...")
            sb.append("\n")
        }

        if (entry.kjvRenderings.isNotEmpty()) {
            sb.append("Traducciones: ${entry.kjvRenderings.take(200)}")
            if (entry.kjvRenderings.length > 200) sb.append("...")
            sb.append("\n")
        }

        return sb.toString()
    }

    /**
     * Formatea la respuesta completa de Strong's.
     */
    private fun formatStrongsResponse(entry: StrongsEntryEntity): String {
        val sb = StringBuilder()
        val langName = if (entry.language == "hebrew") "hebreo" else "griego"

        sb.append("La palabra en $langName es:\n\n")
        sb.append(formatStrongsEntry(entry))
        sb.append("\n\n¿Quieres ver otra palabra original?")
        return sb.toString()
    }

    /**
     * Normaliza un numero de Strong's al formato estandar.
     * "H1254" -> "H1254"
     * "G25" -> "G0025"
     * "1254" + hebreo -> "H1254"
     */
    private fun normalizeStrongsNumber(input: String): String? {
        val trimmed = input.trim().uppercase()

        // Ya tiene prefijo H o G
        if (trimmed.startsWith("H") || trimmed.startsWith("G")) {
            val prefix = trimmed.first()
            val num = trimmed.drop(1).toIntOrNull() ?: return null
            return "$prefix${"%04d".format(num)}"
        }

        // Solo numero - intentar como hebreo por defecto
        val num = trimmed.toIntOrNull() ?: return null
        return "H${"%04d".format(num)}"
    }
}

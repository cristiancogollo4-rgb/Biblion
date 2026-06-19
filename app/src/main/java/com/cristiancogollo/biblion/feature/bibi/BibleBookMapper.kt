package com.cristiancogollo.biblion.feature.bibi

import java.util.Locale

/**
 * Mapeo unificado de nombres de libros biblicos.
 * Convierte entre ingles, abreviaturas, OSIS y el formato en espanol de Biblion.
 *
 * Usado por todas las capas de conocimiento de Bibi:
 * - DictionaryEngine (Easton's): refs como "Exodo 32:11"
 * - CrossReferenceEngine (TSK): refs como "Gen 1:1", "Exod 20:11"
 * - StrongEngine: refs inline en definiciones
 *
 * Biblion almacena libros en bible_content.db con:
 * - bookName: "Genesis", "Exodo" (sin acentos, formato Biblion)
 * - normalizedBookName: "genesis", "exodo" (lowercase, sin acentos, sin espacios)
 */
object BibleBookMapper {

    // Mapeo completo: clave inglesa/abreviada/OSIS -> nombre espanol Biblion
    // Biblion usa nombres SIN acentos (coincide con NVI/DHH en la DB)
    private val map = mapOf(
        // Pentateuco
        "genesis" to "Genesis", "gen" to "Genesis", "gn" to "Genesis",
        "exodus" to "Exodo", "exod" to "Exodo", "ex" to "Exodo",
        "leviticus" to "Levitico", "lev" to "Levitico", "lv" to "Levitico",
        "numbers" to "Numeros", "num" to "Numeros", "nm" to "Numeros",
        "deuteronomy" to "Deuteronomio", "deut" to "Deuteronomio", "dt" to "Deuteronomio",
        // Historicos
        "joshua" to "Josue", "josh" to "Josue", "jos" to "Josue",
        "judges" to "Jueces", "judg" to "Jueces", "jud" to "Jueces", "jdg" to "Jueces",
        "ruth" to "Rut", "ru" to "Rut",
        "1samuel" to "1 Samuel", "1 samuel" to "1 Samuel", "1 sam" to "1 Samuel", "1sam" to "1 Samuel",
        "i samuel" to "1 Samuel", "isam" to "1 Samuel", "1 s" to "1 Samuel",
        "2samuel" to "2 Samuel", "2 samuel" to "2 Samuel", "2 sam" to "2 Samuel", "2sam" to "2 Samuel",
        "ii samuel" to "2 Samuel", "iisam" to "2 Samuel", "2 s" to "2 Samuel",
        "1kings" to "1 Reyes", "1 kings" to "1 Reyes", "1 kgs" to "1 Reyes", "1kgs" to "1 Reyes",
        "i kings" to "1 Reyes", "1 ki" to "1 Reyes", "1 k" to "1 Reyes",
        "2kings" to "2 Reyes", "2 kings" to "2 Reyes", "2 kgs" to "2 Reyes", "2kgs" to "2 Reyes",
        "ii kings" to "2 Reyes", "2 ki" to "2 Reyes", "2 k" to "2 Reyes",
        "1chronicles" to "1 Cronicas", "1 chronicles" to "1 Cronicas", "1 chr" to "1 Cronicas",
        "1chr" to "1 Cronicas", "1 chron" to "1 Cronicas",
        "2chronicles" to "2 Cronicas", "2 chronicles" to "2 Cronicas", "2 chr" to "2 Cronicas",
        "2chr" to "2 Cronicas", "2 chron" to "2 Cronicas",
        "ezra" to "Esdras", "ezr" to "Esdras",
        "nehemiah" to "Nehemias", "neh" to "Nehemias",
        "esther" to "Ester", "esth" to "Ester", "est" to "Ester",
        // Sapienciales
        "job" to "Job", "jb" to "Job",
        "psalms" to "Salmos", "psalm" to "Salmos", "ps" to "Salmos", "psa" to "Salmos", "pss" to "Salmos",
        "proverbs" to "Proverbios", "prov" to "Proverbios", "pr" to "Proverbios",
        "ecclesiastes" to "Eclesiastes", "eccl" to "Eclesiastes", "ecc" to "Eclesiastes",
        "songofsolomon" to "Cantares", "song of solomon" to "Cantares", "song" to "Cantares",
        "cant" to "Cantares", "canticles" to "Cantares", "song of songs" to "Cantares",
        "ss" to "Cantares", "sos" to "Cantares",
        // Profetas mayores
        "isaiah" to "Isaias", "isa" to "Isaias", "is" to "Isaias",
        "jeremiah" to "Jeremias", "jer" to "Jeremias", "je" to "Jeremias",
        "lamentations" to "Lamentaciones", "lam" to "Lamentaciones",
        "ezekiel" to "Ezequiel", "ezek" to "Ezequiel", "eze" to "Ezequiel",
        "daniel" to "Daniel", "dan" to "Daniel", "da" to "Daniel",
        // Profetas menores
        "hosea" to "Oseas", "hos" to "Oseas",
        "joel" to "Joel", "jl" to "Joel",
        "amos" to "Amos", "am" to "Amos",
        "obadiah" to "Abdias", "obad" to "Abdias", "ob" to "Abdias",
        "jonah" to "Jonas", "jon" to "Jonas",
        "micah" to "Miqueas", "mic" to "Miqueas",
        "nahum" to "Nahum", "nah" to "Nahum",
        "habakkuk" to "Habacuc", "hab" to "Habacuc",
        "zephaniah" to "Sofonias", "zeph" to "Sofonias", "zep" to "Sofonias",
        "haggai" to "Hageo", "hag" to "Hageo",
        "zechariah" to "Zacarias", "zech" to "Zacarias", "zec" to "Zacarias",
        "malachi" to "Malaquias", "mal" to "Malaquias",
        // Evangelios
        "matthew" to "Mateo", "matt" to "Mateo", "mt" to "Mateo",
        "mark" to "Marcos", "mk" to "Marcos", "mr" to "Marcos",
        "luke" to "Lucas", "lk" to "Lucas", "lc" to "Lucas",
        "john" to "Juan", "jn" to "Juan", "joh" to "Juan",
        // Hechos
        "acts" to "Hechos", "ac" to "Hechos",
        // Epistolas paulinas
        "romans" to "Romanos", "rom" to "Romanos", "ro" to "Romanos",
        "1corinthians" to "1 Corintios", "1 corinthians" to "1 Corintios", "1 cor" to "1 Corintios",
        "1cor" to "1 Corintios", "i cor" to "1 Corintios", "1 co" to "1 Corintios",
        "2corinthians" to "2 Corintios", "2 corinthians" to "2 Corintios", "2 cor" to "2 Corintios",
        "2cor" to "2 Corintios", "ii cor" to "2 Corintios", "2 co" to "2 Corintios",
        "galatians" to "Galatas", "gal" to "Galatas",
        "ephesians" to "Efesios", "eph" to "Efesios",
        "philippians" to "Filipenses", "phil" to "Filipenses", "php" to "Filipenses",
        "colossians" to "Colosenses", "col" to "Colosenses",
        "1thessalonians" to "1 Tesalonicenses", "1 thessalonians" to "1 Tesalonicenses",
        "1 thess" to "1 Tesalonicenses", "1thess" to "1 Tesalonicenses",
        "1 th" to "1 Tesalonicenses", "1 ts" to "1 Tesalonicenses",
        "2thessalonians" to "2 Tesalonicenses", "2 thessalonians" to "2 Tesalonicenses",
        "2 thess" to "2 Tesalonicenses", "2thess" to "2 Tesalonicenses",
        "2 th" to "2 Tesalonicenses", "2 ts" to "2 Tesalonicenses",
        "1timothy" to "1 Timoteo", "1 timothy" to "1 Timoteo", "1 tim" to "1 Timoteo",
        "1tim" to "1 Timoteo", "1 ti" to "1 Timoteo",
        "2timothy" to "2 Timoteo", "2 timothy" to "2 Timoteo", "2 tim" to "2 Timoteo",
        "2tim" to "2 Timoteo", "2 ti" to "2 Timoteo",
        "titus" to "Tito", "tit" to "Tito",
        "philemon" to "Filemon", "philem" to "Filemon", "phm" to "Filemon", "flm" to "Filemon",
        "hebrews" to "Hebreos", "heb" to "Hebreos",
        // Epistolas generales
        "james" to "Santiago", "jas" to "Santiago", "jam" to "Santiago",
        "1peter" to "1 Pedro", "1 peter" to "1 Pedro", "1 pet" to "1 Pedro",
        "1pet" to "1 Pedro", "1 pe" to "1 Pedro",
        "2peter" to "2 Pedro", "2 peter" to "2 Pedro", "2 pet" to "2 Pedro",
        "2pet" to "2 Pedro", "2 pe" to "2 Pedro",
        "1john" to "1 Juan", "1 john" to "1 Juan", "1 jn" to "1 Juan",
        "2john" to "2 Juan", "2 john" to "2 Juan", "2 jn" to "2 Juan",
        "3john" to "3 Juan", "3 john" to "3 Juan", "3 jn" to "3 Juan",
        "jude" to "Judas", "jud" to "Judas",
        // Apocalipsis
        "revelation" to "Apocalipsis", "rev" to "Apocalipsis", "re" to "Apocalipsis",
        // Apocrifos (aparecen en Easton's)
        "tobit" to "Tobias", "judith" to "Judit", "wisdom" to "Sabiduria",
        "sirach" to "Eclesiastico", "baruch" to "Baruc",
        "1maccabees" to "1 Macabeos", "1 macc" to "1 Macabeos",
        "2maccabees" to "2 Macabeos", "2 macc" to "2 Macabeos",
    )

    /**
     * Convierte cualquier nombre de libro (ingles, abreviado, OSIS, con acentos)
     * al nombre en espanol que usa Biblion (sin acentos).
     */
    fun toSpanish(bookName: String): String {
        val key = normalizeKey(bookName)
        return map[key] ?: map[key.replace(" ", "")] ?: bookName.trim()
    }

    /**
     * Convierte cualquier nombre de libro al formato normalizado de Biblion
     * (lowercase, sin acentos, sin espacios) para buscar en bible_content.db.
     */
    fun toNormalized(bookName: String): String {
        return normalizeForDb(toSpanish(bookName))
    }

    /**
     * Normaliza una referencia OSIS "Gen.12.1" -> (Genesis, 12, 1)
     */
    fun parseOsisRef(osisRef: String): Triple<String, Int, Int>? {
        val parts = osisRef.split(".")
        if (parts.size < 3) return null
        val book = toSpanish(parts[0])
        val chapter = parts[1].toIntOrNull() ?: return null
        val verse = parts[2].toIntOrNull() ?: return null
        return Triple(book, chapter, verse)
    }

    /**
     * Normaliza una clave para busqueda en el mapa.
     */
    private fun normalizeKey(name: String): String {
        return name.trim().lowercase().removeSuffix(".").replace(".", " ").trim()
    }

    /**
     * Normaliza para la DB: lowercase, sin acentos, sin espacios.
     */
    fun normalizeForDb(name: String): String {
        val lowered = name.lowercase().replace(" ", "")
        val sb = StringBuilder()
        for (ch in lowered) {
            val replaced = when (ch) {
                'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú' -> 'u'
                'ñ' -> 'n'; 'ü' -> 'u'
                else -> ch
            }
            sb.append(replaced)
        }
        return sb.toString()
    }
}

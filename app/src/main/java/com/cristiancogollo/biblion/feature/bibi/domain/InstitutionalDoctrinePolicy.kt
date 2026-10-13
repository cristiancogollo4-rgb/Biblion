package com.cristiancogollo.biblion.feature.bibi.domain

import java.text.Normalizer
import java.util.Locale

/** Respuestas institucionales solo cuando existe una fuente primaria verificada. */
object InstitutionalDoctrinePolicy {
    private const val IPUC_ARTICLES_URL = "https://distrito1.ipuc.org.co/articulosdefe/"
    const val UNVERIFIED_ANSWER = "No tengo una declaración oficial verificada de esa iglesia o " +
        "denominación sobre este tema. Prefiero no atribuirle una postura sin una fuente " +
        "propia de la institución. Si compartes su declaración oficial, puedo ayudarte a analizarla."

    private val institution = Regex(
        "\\b(?:ipuc|iglesias?|denominaciones?|congregaciones?|catolic[oa]s?|bautistas?|" +
            "adventistas?|pentecostales?|luteranos?|metodistas?|presbiterianos?|" +
            "anglicanos?|ortodoxos?|reformados?|mormones?|testigos de jehova|" +
            "asambleas? de dios)\\b",
    )
    private val namedDenomination = Regex(
        "\\b(?:ipuc|catolic[oa]s?|bautistas?|adventistas?|pentecostal(?:es)?|" +
            "luteranos?|metodistas?|presbiterianos?|anglicanos?|ortodoxos?|" +
            "reformados?|mormones?|testigos de jehova|asambleas? de dios)\\b",
    )
    private val position = Regex(
        "\\b(?:segun|ensena(?:n)?|creen?|creencia|afirman?|sostienen?|consideran?|" +
            "postura|doctrina|bautismo|salvacion|trinidad|unicidad|santa cena|" +
            "eucaristia|predestinacion|dones|ordenacion|necesari[oa]s?|requisito)\\b",
    )
    private val doctrinalTopic = Regex(
        "\\b(?:bautismo|salvacion|trinidad|unicidad|santa cena|eucaristia|" +
            "predestinacion|dones|ordenacion|doctrina|fe|pecado|gracia|" +
            "necesari[oa]s?|requisito)\\b",
    )
    private val namedAttribution = Regex(
        "\\b(?:(?:segun|que (?:cree|creen|ensena|ensenan)) " +
            "(?:la|el|los|las)|para (?:los|las)) (?!biblia\\b|escrituras\\b|palabra\\b|" +
            "evangelio\\b|jesus\\b|cristo\\b|pablo\\b|juan\\b|pedro\\b|" +
            "texto\\b|pasaje\\b|iglesias\\b|denominaciones\\b|" +
            "congregaciones\\b|tradiciones\\b|cristianos\\b|creyentes\\b|" +
            "personas\\b|hombres\\b|mujeres\\b|ninos\\b|adultos\\b|" +
            "discipulos\\b|apostoles\\b|profetas\\b|judios\\b|gentiles\\b|" +
            "algunas\\b|muchas\\b)[a-z][a-z0-9]+",
    )

    /** Nunca se atribuye una postura institucional sin una fuente primaria registrada. */
    fun needsOfficialSource(question: String): Boolean {
        val normalized = normalize(question)
        return (institution.containsMatchIn(normalized) && position.containsMatchIn(normalized)) ||
            (namedAttribution.containsMatchIn(normalized) && doctrinalTopic.containsMatchIn(normalized))
    }

    fun containsUnsupportedAttribution(answer: String): Boolean {
        val normalized = normalize(answer)
        return namedDenomination.containsMatchIn(normalized) ||
            namedAttribution.containsMatchIn(normalized)
    }

    /** Respuesta de respaldo cuando una pregunta general recibe una atribución no verificable. */
    fun generalQuestionFallback(question: String): String? {
        if (needsOfficialSource(question)) return null
        val normalized = normalize(question)
        val asksAboutBaptism = Regex("\\bbauti(?:smo|zar|zado|zarse)\\b")
            .containsMatchIn(normalized)
        val asksAboutSalvation = Regex(
            "\\b(?:salvacion|salvar(?:se)?|salva|salvos?|necesari[oa]s?|requisito|perdon)\\b",
        ).containsMatchIn(normalized)
        if (!asksAboutBaptism || !asksAboutSalvation) return null

        return "La relación entre el bautismo y la salvación se interpreta de distintas maneras. " +
            "Hechos 2:38 relaciona el arrepentimiento y el bautismo con el perdón; " +
            "Efesios 2:8-9 destaca la salvación por gracia mediante la fe. " +
            "Algunos cristianos consideran el bautismo parte necesaria de la respuesta de fe; " +
            "otros lo entienden como una obediencia posterior, no como la causa de la salvación. " +
            "Podemos examinar esos pasajes en su contexto o partir de la perspectiva que tú prefieras."
    }

    fun verifiedAnswer(question: String): String? {
        val normalized = normalize(question)
        val mentionsIpuc = Regex("\\bipuc\\b|\\biglesia pentecostal unida de colombia\\b")
            .containsMatchIn(normalized)
        val asksAboutBaptism = Regex("\\bbauti(?:smo|zar|zado|zarse)\\b")
            .containsMatchIn(normalized)
        val asksAboutSalvation = Regex("\\b(?:salvacion|salvar(?:se)?|salva|salvos?|necesari[oa]s?|requisito|perdon)\\b")
            .containsMatchIn(normalized)
        if (!mentionsIpuc || !asksAboutBaptism || !asksAboutSalvation) return null
        // La fuente registrada solo cubre esta institución; una comparación exige más fuentes.
        val otherInstitution = institution.findAll(normalized).any { match ->
            match.value !in setOf("ipuc", "iglesia")
        }
        val comparison = Regex("\\b(?:compara|comparar|comparacion|diferencia|versus|vs)\\b")
            .containsMatchIn(normalized) ||
            Regex("\\bipuc y (?:la|el|los|las) [a-z]+").containsMatchIn(normalized)
        if (otherInstitution || comparison) return null

        return "Según sus Artículos de Fe, la IPUC enseña el bautismo en agua por inmersión " +
            "después de creer y arrepentirse, invocando el nombre de Jesucristo en obediencia " +
            "a la Palabra; cita Hechos 2:38. Lo trata como parte de la respuesta al evangelio, " +
            "no simplemente como un testimonio " +
            "público opcional, por lo que decir que para la IPUC no tiene relación con la " +
            "salvación es incorrecto. Los mismos artículos enseñan que la salvación es por " +
            "gracia mediante la fe (Efesios 2:8); esto no equivale a decir que el agua salve " +
            "por sí sola. Esta es la enseñanza de la IPUC, no una postura que Bibi imponga " +
            "a todas las iglesias.\n\n" +
            "Fuente: Artículos de Fe de la IPUC, secciones «La gracia de Dios y la salvación " +
            "del hombre» y «Bautismo en agua»: $IPUC_ARTICLES_URL"
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
}

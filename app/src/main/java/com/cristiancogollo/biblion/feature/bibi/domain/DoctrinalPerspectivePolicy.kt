package com.cristiancogollo.biblion.feature.bibi.domain

import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import java.text.Normalizer
import java.util.Locale

/** La perspectiva procede solo de mensajes del usuario en la conversación activa. */
object DoctrinalPerspectivePolicy {
    enum class Perspective { TRINITARIAN, ONENESS }

    data class Context(
        val isDebatedQuestion: Boolean,
        val perspective: Perspective?,
    ) {
        fun fallbackAnswer(): String = when (perspective) {
            Perspective.TRINITARIAN ->
                "Desde la perspectiva trinitaria que expresaste, Dios es uno y se entiende " +
                    "al Padre, al Hijo y al Espíritu Santo como tres personas distintas. " +
                    "Otras comunidades cristianas interpretan esta relación de otra manera. " +
                    "Puedo examinar contigo los pasajes concretos sin juzgar la fe de nadie."
            Perspective.ONENESS ->
                "Desde la perspectiva de la unicidad que expresaste, Dios es uno y no se " +
                    "entiende al Padre, al Hijo y al Espíritu Santo como tres personas divinas " +
                    "distintas. La tradición trinitaria interpreta esa relación de otra manera. " +
                    "Puedo examinar contigo los pasajes concretos sin juzgar la fe de nadie."
            null ->
                "Según la doctrina trinitaria, el Padre, el Hijo y el Espíritu Santo son " +
                    "tres personas distintas en un solo Dios. Según la perspectiva de la " +
                    "unicidad, Dios es uno y esa relación no se entiende como tres personas " +
                    "divinas distintas; ambas afirman un solo Dios. Son interpretaciones " +
                    "debatidas de los pasajes bíblicos. No asumiré una postura por la forma " +
                    "de tu pregunta. ¿Quieres que lo examinemos desde alguna de ellas o que " +
                    "comparemos sus argumentos bíblicos?"
        }
    }

    private val debatedTerms = Regex(
        "\\b(?:trinidad|trinitari[oa]s?|unicidad|unicitari[oa]s?|" +
            "modalismo|trino|(?:tres|3) personas(?: divinas| en dios)?|" +
            "(?:jesus|cristo|hijo|padre|espiritu santo) (?:es|no es) (?:el )?" +
            "(?:padre|hijo|espiritu santo))\\b",
    )
    private val followUp = Regex("^(y |pero |entonces |por que |como |explica |que dice |ahora )")
    private val trinitarianStatements = listOf(
        Regex("\\bsoy (?:un[ao]? )?trinitari[oa]\\b"),
        Regex("\\b(?:creo|creemos|confieso|sostengo|acepto) en la trinidad\\b"),
        Regex("\\b(?:mi|nuestra) (?:iglesia|tradicion|postura|fe) (?:es )?trinitaria\\b"),
        Regex("\\b(?:desde|segun) (?:mi|nuestra|la) (?:postura|perspectiva|doctrina|teologia) trinitaria\\b"),
        Regex("\\b(?:responde|explica|analiza) desde (?:la )?(?:perspectiva )?trinitaria\\b"),
    )
    private val onenessStatements = listOf(
        Regex("\\bsoy (?:de )?(?:la )?unicidad\\b"),
        Regex("\\bsoy unicitari[oa]\\b"),
        Regex("\\b(?:creo|creemos|confieso|sostengo|acepto) en la unicidad\\b"),
        Regex("\\b(?:mi|nuestra) (?:iglesia|tradicion|postura|fe) (?:es )?(?:de la )?unicidad\\b"),
        Regex("\\b(?:desde|segun) (?:mi|nuestra|la) (?:postura|perspectiva|doctrina|teologia) (?:de la )?unicidad\\b"),
        Regex("\\b(?:responde|explica|analiza) desde (?:la )?(?:perspectiva de la )?unicidad\\b"),
    )
    private val trinitarianWithdrawal = Regex(
        "\\b(?:ya )?(?:no creo en|rechazo) (?:la )?trinidad\\b|" +
            "\\b(?:ya )?no soy (?:un[ao]? )?trinitari[oa]\\b",
    )
    private val onenessWithdrawal = Regex(
        "\\b(?:ya )?(?:no creo en|rechazo) (?:la )?unicidad\\b|" +
            "\\b(?:ya )?no soy (?:(?:de )?(?:la )?unicidad|unicitari[oa])\\b",
    )
    private val withdrawal = Regex(
        "(?:${trinitarianWithdrawal.pattern})|(?:${onenessWithdrawal.pattern})",
    )
    private val unsafeClaims = listOf(
        Regex("\\bdios no es uno\\b"),
        Regex("\\b(?:trinidad|unicidad) (?:es|son) (?:una? )?(?:fals[ao]|mentira|herejia|error|invento)\\b"),
        Regex("\\b(?:trinidad|unicidad) (?:es|son) (?:la )?(?:unica verdad|verdad absoluta)\\b"),
        Regex("\\b(?:trinitarios|trinitarias|unicitarios|unicitarias) no (?:son|tienen) (?:cristianos|cristianas|creyentes|fe)\\b"),
        Regex("\\b(?:trinitarios|trinitarias|unicitarios|unicitarias) (?:son|estan) (?:falsos|herejes|perdidos)\\b"),
        Regex("\\bno (?:eres|son|es) (?:un[ao]? )?(?:cristian[oa]|creyente)\\b"),
    )

    fun analyze(
        question: String,
        history: List<ChatExchange>,
        sessionPerspective: Perspective? = null,
    ): Context {
        val normalizedQuestion = normalize(question)
        val priorDebate = history.takeLast(2).any { debatedTerms.containsMatchIn(normalize(it.question)) }
        val isDebated = debatedTerms.containsMatchIn(normalizedQuestion) ||
            (priorDebate && normalizedQuestion.length < 100 && followUp.containsMatchIn(normalizedQuestion))
        val currentPerspective = explicitPerspective(normalizedQuestion) ?: when {
            priorDebate && normalizedQuestion.matches(Regex("(?:la )?(?:trinidad|trinitari[oa])")) ->
                Perspective.TRINITARIAN
            priorDebate && normalizedQuestion.matches(Regex("(?:la )?(?:unicidad|unicitari[oa])")) ->
                Perspective.ONENESS
            else -> null
        }
        val priorPerspective = history.asReversed().map { normalize(it.question) }
            .takeWhile { !withdrawal.containsMatchIn(it) }
            .firstNotNullOfOrNull(::explicitPerspective)
        return Context(
            isDebated,
            if (currentPerspective != null || !withdrawal.containsMatchIn(normalizedQuestion)) {
                currentPerspective ?: sessionPerspective ?: priorPerspective
            } else {
                null
            },
        )
    }

    fun perspectiveFromUserMessages(messages: List<String>): Perspective? {
        var perspective: Perspective? = null
        val recentQuestions = ArrayDeque<String>()
        messages.forEach { message ->
            val current = normalize(message)
            val declared = explicitPerspective(current)
            val priorDebate = recentQuestions.any(debatedTerms::containsMatchIn)
            perspective = when {
                declared != null -> declared
                withdrawal.containsMatchIn(current) -> null
                priorDebate && current.matches(Regex("(?:la )?(?:trinidad|trinitari[oa])")) ->
                    Perspective.TRINITARIAN
                priorDebate && current.matches(Regex("(?:la )?(?:unicidad|unicitari[oa])")) ->
                    Perspective.ONENESS
                else -> perspective
            }
            recentQuestions.addLast(current)
            if (recentQuestions.size > 2) recentQuestions.removeFirst()
        }
        return perspective
    }

    fun containsUnsafeClaim(answer: String): Boolean =
        unsafeClaims.any { it.containsMatchIn(normalize(answer)) }

    fun attributeAnswer(answer: String, perspective: Perspective): String {
        if (answer.trimStart().startsWith("Desde la perspectiva", ignoreCase = true)) return answer
        val label = when (perspective) {
            Perspective.TRINITARIAN -> "Desde la perspectiva trinitaria que expresaste:"
            Perspective.ONENESS -> "Desde la perspectiva de la unicidad que expresaste:"
        }
        return "$label\n\n$answer"
    }

    private fun explicitPerspective(text: String): Perspective? {
        val trinitarian = trinitarianStatements.any { it.containsMatchIn(text) } &&
            !trinitarianWithdrawal.containsMatchIn(text)
        val oneness = onenessStatements.any { it.containsMatchIn(text) } &&
            !onenessWithdrawal.containsMatchIn(text)
        return when {
            trinitarian && !oneness -> Perspective.TRINITARIAN
            oneness && !trinitarian -> Perspective.ONENESS
            else -> null
        }
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

const debatedTerms = /\b(?:trinidad|trinitari[oa]s?|unicidad|unicitari[oa]s?|modalismo|trino|(?:tres|3) personas(?: divinas| en dios)?|(?:jesus|cristo|hijo|padre|espiritu santo) (?:es|no es) (?:el )?(?:padre|hijo|espiritu santo))\b/;
const followUp = /^(y |pero |entonces |por que |como |explica |que dice |ahora )/;
const trinitarianStatements = [
  /\bsoy (?:un[ao]? )?trinitari[oa]\b/,
  /\b(?:creo|creemos|confieso|sostengo|acepto) en la trinidad\b/,
  /\b(?:mi|nuestra) (?:iglesia|tradicion|postura|fe) (?:es )?trinitaria\b/,
  /\b(?:desde|segun) (?:mi|nuestra|la) (?:postura|perspectiva|doctrina|teologia) trinitaria\b/,
  /\b(?:responde|explica|analiza) desde (?:la )?(?:perspectiva )?trinitaria\b/
];
const onenessStatements = [
  /\bsoy (?:de )?(?:la )?unicidad\b/,
  /\bsoy unicitari[oa]\b/,
  /\b(?:creo|creemos|confieso|sostengo|acepto) en la unicidad\b/,
  /\b(?:mi|nuestra) (?:iglesia|tradicion|postura|fe) (?:es )?(?:de la )?unicidad\b/,
  /\b(?:desde|segun) (?:mi|nuestra|la) (?:postura|perspectiva|doctrina|teologia) (?:de la )?unicidad\b/,
  /\b(?:responde|explica|analiza) desde (?:la )?(?:perspectiva de la )?unicidad\b/
];
const trinitarianWithdrawal = /\b(?:ya )?(?:no creo en|rechazo) (?:la )?trinidad\b|\b(?:ya )?no soy (?:un[ao]? )?trinitari[oa]\b/;
const onenessWithdrawal = /\b(?:ya )?(?:no creo en|rechazo) (?:la )?unicidad\b|\b(?:ya )?no soy (?:(?:de )?(?:la )?unicidad|unicitari[oa])\b/;
const withdrawal = (text) => trinitarianWithdrawal.test(text) || onenessWithdrawal.test(text);
const unsafeClaims = [
  /\bdios no es uno\b/,
  /\b(?:trinidad|unicidad) (?:es|son) (?:una? )?(?:fals[ao]|mentira|herejia|error|invento|antibiblic[ao])\b/,
  /\b(?:trinidad|unicidad) (?:es|son) (?:la )?(?:unica verdad|verdad absoluta)\b/,
  /\b(?:trinidad|unicidad) no es biblica\b/,
  /\b(?:trinitarios|trinitarias|unicitarios|unicitarias) no (?:son|tienen) (?:cristianos|cristianas|creyentes|fe)\b/,
  /\b(?:trinitarios|trinitarias|unicitarios|unicitarias) (?:son|estan) (?:falsos|herejes|perdidos)\b/,
  /\bno (?:eres|son|es) (?:un[ao]? )?(?:cristian[oa]|creyente)\b/
];

function normalize(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9 ]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function explicitPerspective(text) {
  const trinitarian = trinitarianStatements.some((pattern) => pattern.test(text)) &&
    !trinitarianWithdrawal.test(text);
  const oneness = onenessStatements.some((pattern) => pattern.test(text)) &&
    !onenessWithdrawal.test(text);
  if (trinitarian === oneness) return null;
  return trinitarian ? "trinitarian" : "oneness";
}

export function resolveDoctrinalContext(question, chatHistory = [], conversationPerspective = null) {
  const current = normalize(question);
  const recentUserQuestions = chatHistory.slice(-2).map((entry) => normalize(entry.question));
  const priorDebate = recentUserQuestions.some((text) => debatedTerms.test(text));
  const isDebated = debatedTerms.test(current) ||
    (priorDebate && current.length < 100 && followUp.test(current));
  let perspective = explicitPerspective(current);
  if (!perspective && priorDebate) {
    if (/^(?:la )?(?:trinidad|trinitari[oa])$/.test(current)) perspective = "trinitarian";
    if (/^(?:la )?(?:unicidad|unicitari[oa])$/.test(current)) perspective = "oneness";
  }
  if (!perspective && !withdrawal(current) &&
      ["trinitarian", "oneness"].includes(conversationPerspective)) {
    perspective = conversationPerspective;
  }
  if (!perspective && !withdrawal(current)) {
    for (let index = chatHistory.length - 1; index >= 0; index--) {
      const userStatement = normalize(chatHistory[index].question);
      if (withdrawal(userStatement)) break;
      perspective = explicitPerspective(userStatement);
      if (perspective) break;
    }
  }
  return { isDebated, perspective };
}

export function doctrinalFallback(context) {
  const answer = context.perspective === "trinitarian"
    ? "Desde la perspectiva trinitaria que expresaste, Dios es uno y se entiende al Padre, al Hijo y al Espíritu Santo como tres personas distintas. Otras comunidades cristianas interpretan esta relación de otra manera. Puedo examinar contigo los pasajes concretos sin juzgar la fe de nadie."
    : context.perspective === "oneness"
      ? "Desde la perspectiva de la unicidad que expresaste, Dios es uno y no se entiende al Padre, al Hijo y al Espíritu Santo como tres personas divinas distintas. La tradición trinitaria interpreta esa relación de otra manera. Puedo examinar contigo los pasajes concretos sin juzgar la fe de nadie."
      : "Según la doctrina trinitaria, el Padre, el Hijo y el Espíritu Santo son tres personas distintas en un solo Dios. Según la perspectiva de la unicidad, Dios es uno y esa relación no se entiende como tres personas divinas distintas; ambas afirman un solo Dios. Son interpretaciones debatidas de los pasajes bíblicos. No asumiré una postura por la forma de tu pregunta. ¿Quieres que lo examinemos desde alguna de ellas o que comparemos sus argumentos bíblicos?";
  return {
    answer,
    references: [],
    suggestedBlocks: [],
    confidence: "medium",
    disclaimer: null,
    intentDetected: "doctrinal_context"
  };
}

export function enforceDoctrinalResponse(response, context) {
  if (!context.isDebated) return response;
  const outputTexts = [response.answer, response.disclaimer,
    ...(response.references || []).map((entry) => entry.reason),
    ...(response.suggestedBlocks || [])];
  if (!context.perspective || outputTexts.some((value) =>
    unsafeClaims.some((pattern) => pattern.test(normalize(value))))) {
    return doctrinalFallback(context);
  }
  const label = context.perspective === "trinitarian"
    ? "Desde la perspectiva trinitaria que expresaste:"
    : "Desde la perspectiva de la unicidad que expresaste:";
  return {
    ...response,
    answer: normalize(response.answer).startsWith("desde la perspectiva")
      ? response.answer : `${label}\n\n${response.answer}`,
    disclaimer: [response.disclaimer, "Esta explicación no juzga la fe de otras personas."]
      .filter(Boolean).join(" ")
  };
}

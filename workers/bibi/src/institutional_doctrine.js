const IPUC_ARTICLES_URL = "https://distrito1.ipuc.org.co/articulosdefe/";
const INSTITUTION = /\b(?:ipuc|iglesias?|denominaciones?|congregaciones?|catolic[oa]s?|bautistas?|adventistas?|pentecostales?|luteranos?|metodistas?|presbiterianos?|anglicanos?|ortodoxos?|reformados?|mormones?|testigos de jehova|asambleas? de dios)\b/;
const NAMED_DENOMINATION = /\b(?:ipuc|catolic[oa]s?|bautistas?|adventistas?|pentecostales?|luteranos?|metodistas?|presbiterianos?|anglicanos?|ortodoxos?|reformados?|mormones?|testigos de jehova|asambleas? de dios)\b/;
const POSITION = /\b(?:segun|ensena(?:n)?|creen?|creencia|afirman?|sostienen?|consideran?|postura|doctrina|bautismo|salvacion|trinidad|unicidad|santa cena|eucaristia|predestinacion|dones|ordenacion|necesari[oa]s?|requisito)\b/;
const DOCTRINAL_TOPIC = /\b(?:bautismo|salvacion|trinidad|unicidad|santa cena|eucaristia|predestinacion|dones|ordenacion|doctrina|fe|pecado|gracia|necesari[oa]s?|requisito)\b/;
const NAMED_ATTRIBUTION = /\b(?:segun|para|que (?:cree|creen|ensena|ensenan)) (?:la|el|los|las) (?!biblia\b|escrituras\b|palabra\b|evangelio\b|jesus\b|cristo\b|pablo\b|juan\b|pedro\b|texto\b|pasaje\b)[a-z][a-z0-9]+/;
const ATTRIBUTION_VERB = /\b(?:segun|para|ensena(?:n)?|creen?|afirman?|sostienen?|consideran?|postura|doctrina)\b/;

function normalize(text) {
  return String(text || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9 ]/g, " ")
    .replace(/\s+/g, " ");
}

export function needsOfficialSource(question) {
  const normalized = normalize(question);
  return (INSTITUTION.test(normalized) && POSITION.test(normalized)) ||
    (NAMED_ATTRIBUTION.test(normalized) && DOCTRINAL_TOPIC.test(normalized));
}

export function containsUnsupportedAttribution(answer) {
  const normalized = normalize(answer);
  return NAMED_DENOMINATION.test(normalized) ||
    ((INSTITUTION.test(normalized) || NAMED_ATTRIBUTION.test(normalized)) &&
      ATTRIBUTION_VERB.test(normalized));
}

export function unverifiedInstitutionalAnswer() {
  return {
    answer: "No tengo una declaración oficial verificada de esa iglesia o denominación sobre este tema. Prefiero no atribuirle una postura sin una fuente propia de la institución. Si compartes su declaración oficial, puedo ayudarte a analizarla.",
    references: [],
    suggestedBlocks: [],
    confidence: "low",
    disclaimer: null,
    intentDetected: "unverified_institutional_doctrine"
  };
}

export function verifiedInstitutionalAnswer(question) {
  const normalized = normalize(question);
  const mentionsIpuc = /\bipuc\b|\biglesia pentecostal unida de colombia\b/.test(normalized);
  const asksAboutBaptism = /\bbauti(?:smo|zar|zado|zarse)\b/.test(normalized);
  const asksAboutSalvation = /\b(?:salvacion|salvar(?:se)?|salva|salvos?|necesari[oa]s?|requisito|perdon)\b/.test(normalized);
  if (!mentionsIpuc || !asksAboutBaptism || !asksAboutSalvation) return null;
  // La fuente registrada no basta para comparar la IPUC con otra institución.
  const otherInstitution = [...normalized.matchAll(new RegExp(INSTITUTION, "g"))]
    .some((match) => !["ipuc", "iglesia"].includes(match[0]));
  const comparison = /\b(?:compara|comparar|comparacion|diferencia|versus|vs)\b/.test(normalized) ||
    /\bipuc y (?:la|el|los|las) [a-z]+/.test(normalized);
  if (otherInstitution || comparison) return null;

  return {
    answer: "Según sus Artículos de Fe, la IPUC enseña el bautismo en agua por inmersión después de creer y arrepentirse, invocando el nombre de Jesucristo en obediencia a la Palabra; cita Hechos 2:38. Lo trata como parte de la respuesta al evangelio, no simplemente como un testimonio público opcional, por lo que decir que para la IPUC no tiene relación con la salvación es incorrecto. Los mismos artículos enseñan que la salvación es por gracia mediante la fe (Efesios 2:8); esto no equivale a decir que el agua salve por sí sola. Esta es la enseñanza de la IPUC, no una postura que Bibi imponga a todas las iglesias.\n\n" +
      `Fuente: Artículos de Fe de la IPUC, secciones «La gracia de Dios y la salvación del hombre» y «Bautismo en agua»: ${IPUC_ARTICLES_URL}`,
    references: [],
    suggestedBlocks: [],
    confidence: "high",
    disclaimer: null,
    intentDetected: "verified_institutional_doctrine"
  };
}

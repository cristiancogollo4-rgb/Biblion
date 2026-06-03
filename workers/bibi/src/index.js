const NVIDIA_BASE_URL = "https://integrate.api.nvidia.com/v1";
const BIBI_MODEL = "nvidia/llama-3.1-nemotron-nano-8b-v1";

const DEFAULT_BIBLE_VERSIONS = [
  { key: "rv1960", label: "Reina Valera 1960" },
  { key: "nvi", label: "Nueva Version Internacional (NVI)" },
  { key: "dhh", label: "Dios Habla Hoy (DHH)" },
  { key: "tla", label: "Traduccion en Lenguaje Actual (TLA)" },
  { key: "ntv", label: "Nueva Traduccion Viviente (NTV)" }
];

const BIBLICAL_DICTIONARY = [
  {
    terms: ["creacion", "crear", "creador"],
    entry: "Creacion: acto por el cual Dios trae a existencia los cielos, la tierra y todo lo creado. Referencias clave: Genesis 1-2, Juan 1:1-3, Colosenses 1:16, Hebreos 11:3."
  },
  {
    terms: ["pacto", "alianza"],
    entry: "Pacto: compromiso solemne establecido por Dios con personas o con su pueblo. En la Biblia aparecen pactos con Noe, Abraham, Israel, David y el nuevo pacto anunciado por los profetas y cumplido en Cristo."
  },
  {
    terms: ["fe", "creer", "confianza"],
    entry: "Fe: confianza obediente en Dios y en su palabra. No es solo aceptar informacion, sino responder a Dios con confianza, dependencia y obediencia. Referencias: Hebreos 11:1, Romanos 10:17, Santiago 2:17."
  },
  {
    terms: ["gracia"],
    entry: "Gracia: favor inmerecido de Dios. En el evangelio expresa la iniciativa salvadora de Dios hacia el ser humano, no basada en meritos propios. Referencias: Efesios 2:8-9, Tito 2:11."
  },
  {
    terms: ["pecado"],
    entry: "Pecado: rebelion, desobediencia o desviacion de la voluntad de Dios. Afecta la relacion con Dios y requiere perdon, arrepentimiento y restauracion. Referencias: Romanos 3:23, 1 Juan 1:9."
  },
  {
    terms: ["evangelio", "buenas nuevas"],
    entry: "Evangelio: buenas noticias de salvacion por medio de Jesucristo: su vida, muerte, resurreccion y senorio. Referencias: Marcos 1:1, 1 Corintios 15:1-4, Romanos 1:16."
  },
  {
    terms: ["mesias", "cristo", "ungido"],
    entry: "Mesias/Cristo: el Ungido prometido por Dios. En el Nuevo Testamento se identifica a Jesus como el Cristo, Rey y Salvador. Referencias: Juan 1:41, Mateo 16:16, Lucas 24:26-27."
  },
  {
    terms: ["discipulado", "discipulo"],
    entry: "Discipulado: proceso de seguir a Jesus, aprender de el, obedecer sus ensenanzas y formar a otros en la fe. Referencias: Mateo 28:19-20, Lucas 9:23."
  },
  {
    terms: ["reino", "reino de dios", "reino de los cielos"],
    entry: "Reino de Dios: gobierno soberano de Dios manifestado en su obra redentora. Jesus anuncio el reino como realidad presente y esperanza futura. Referencias: Marcos 1:15, Mateo 6:33."
  },
  {
    terms: ["salvacion", "salvar"],
    entry: "Salvacion: obra de Dios que rescata al ser humano del pecado y sus consecuencias, reconciliandolo con Dios por medio de Cristo. Referencias: Juan 3:16, Efesios 2:8-9, Romanos 10:9."
  },
  {
    terms: ["redencion", "redimir"],
    entry: "Redencion: rescate o liberacion mediante un precio. En Cristo expresa la liberacion del pecado y la restauracion ante Dios. Referencias: Efesios 1:7, Colosenses 1:13-14."
  },
  {
    terms: ["santidad", "santo"],
    entry: "Santidad: separacion para Dios y vida conforme a su caracter. Dios es santo y llama a su pueblo a vivir en santidad. Referencias: Levitico 19:2, 1 Pedro 1:15-16."
  },
  {
    terms: ["adoracion", "adorar"],
    entry: "Adoracion: respuesta reverente de amor, obediencia y honra a Dios. Incluye alabanza, servicio y una vida rendida a el. Referencias: Juan 4:23-24, Romanos 12:1."
  },
  {
    terms: ["oracion", "orar"],
    entry: "Oracion: comunicacion reverente con Dios que incluye adoracion, peticion, intercesion, confesion y gratitud. Referencias: Mateo 6:9-13, Filipenses 4:6."
  },
  {
    terms: ["iglesia"],
    entry: "Iglesia: comunidad de los creyentes en Cristo, llamada a adorar, crecer, servir, evangelizar y vivir como cuerpo de Cristo. Referencias: Hechos 2:42-47, 1 Corintios 12:27."
  },
  {
    terms: ["justicia", "justo"],
    entry: "Justicia: rectitud conforme al caracter y voluntad de Dios. Puede referirse a la justicia de Dios, la vida justa o la justificacion recibida por la fe. Referencias: Miqueas 6:8, Romanos 3:21-26."
  },
  {
    terms: ["amor"],
    entry: "Amor: atributo central de Dios y mandamiento esencial para su pueblo. En la Biblia se expresa en entrega, obediencia, servicio y verdad. Referencias: Juan 3:16, 1 Corintios 13, 1 Juan 4:7-11."
  },
  {
    terms: ["esperanza"],
    entry: "Esperanza: confianza firme en las promesas de Dios. No es optimismo vacio, sino seguridad basada en el caracter de Dios y la obra de Cristo. Referencias: Romanos 15:13, Hebreos 6:19."
  },
  {
    terms: ["bautismo", "bautizar"],
    entry: "Bautismo: acto de obediencia y testimonio publico asociado con la fe en Cristo, el arrepentimiento y la identificacion con el pueblo de Dios. Referencias: Mateo 28:19, Hechos 2:38, Romanos 6:3-4."
  },
  {
    terms: ["santa cena", "cena del senor", "comunion"],
    entry: "Santa Cena: practica instituida por Jesus para recordar su muerte y anunciar su obra redentora hasta que el venga. Referencias: Lucas 22:19-20, 1 Corintios 11:23-26."
  }
];

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Content-Type": "application/json"
};

const OUT_OF_DOMAIN_MESSAGE = "Estoy diseñada para ayudarte únicamente con temas bíblicos y de estudio de las Escrituras dentro de Biblion. ¿Te gustaría explorar algún pasaje, personaje, tema o enseñanza bíblica?";

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS_HEADERS });
    }

    const url = new URL(request.url);
    if (request.method !== "POST" || url.pathname !== "/ask") {
      return jsonResponse({ error: "Ruta no disponible." }, 404);
    }

    if (!env.NVIDIA_API_KEY) {
      return jsonResponse({ error: "NVIDIA_API_KEY no esta configurada." }, 500);
    }

    let data;
    try {
      data = await request.json();
    } catch (_) {
      return jsonResponse({ error: "El cuerpo debe ser JSON valido." }, 400);
    }

    const question = sanitizeText(data?.question, 1200);
    if (!question) {
      return jsonResponse({ error: "La pregunta no puede estar vacia." }, 400);
    }

    const study = data?.study || {};
    const bible = data?.bible || {};
    const mode = sanitizeText(data?.mode, 20) === "reader" ? "reader" : "study";
    const intent = normalizeIntent(data?.intent || inferIntent(question));
    const currentOutline = sanitizeStringArray(study.currentOutline, 12, 220);
    const notes = sanitizeStringArray(study.notes, 8, 240);
    const providedPassages = sanitizeStringArray(bible.passages, 16, 500);
    const availableVersions = sanitizeBibleVersions(bible.availableVersions);
    const dictionaryEntries = getRelevantDictionaryEntries([
      question,
      study.title,
      study.selectedText,
      ...(Array.isArray(study.tags) ? study.tags : [])
    ].join(" "));
    if (!isBibleDomain({
      question,
      mode,
      title: study.title,
      selectedText: study.selectedText,
      tags: study.tags,
      currentOutline,
      notes,
      providedPassages,
      dictionaryEntries
    })) {
      return jsonResponse({
        answer: OUT_OF_DOMAIN_MESSAGE,
        references: [],
        suggestedBlocks: [],
        confidence: "high"
      });
    }

    const identityAnswer = buildIdentityAnswer(question, mode);
    if (identityAnswer) {
      return jsonResponse({
        answer: identityAnswer,
        references: [],
        suggestedBlocks: [],
        confidence: "high"
      });
    }
    const versionsAnswer = buildVersionsAnswer(question, availableVersions);
    if (versionsAnswer) {
      return jsonResponse({
        answer: versionsAnswer,
        references: [],
        suggestedBlocks: [],
        confidence: "high"
      });
    }
    const dictionaryAnswer = buildDictionaryAnswer(question, dictionaryEntries, providedPassages, intent);
    if (dictionaryAnswer) {
      return jsonResponse(dictionaryAnswer);
    }

    const prompt = buildBibiPrompt({
      mode,
      intent,
      question,
      title: sanitizeText(study.title, 200),
      tags: Array.isArray(study.tags) ? study.tags.map((tag) => sanitizeText(tag, 40)).filter(Boolean) : [],
      selectedText: sanitizeText(study.selectedText, 1800),
      currentOutline,
      notes,
      providedPassages,
      availableVersions,
      dictionaryEntries,
      bibleVersion: sanitizeText(bible.version, 30) || "rv1960"
    });

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort("Bibi timeout"), 25000);
    let response;
    try {
      response = await fetch(`${NVIDIA_BASE_URL}/chat/completions`, {
        method: "POST",
        signal: controller.signal,
        headers: {
          Authorization: `Bearer ${env.NVIDIA_API_KEY}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: BIBI_MODEL,
          messages: prompt,
          temperature: 0.25,
          top_p: 0.8,
          max_tokens: mode === "reader" ? 260 : 520,
          stream: false
        })
      });
    } catch (error) {
      return jsonResponse(buildTimeoutFallback({
        question,
        mode,
        intent,
        selectedText: sanitizeText(study.selectedText, 1800),
        providedPassages,
        dictionaryEntries
      }));
    } finally {
      clearTimeout(timeout);
    }

    if (!response.ok) {
      const body = await response.text();
      return jsonResponse(
        { error: `NVIDIA respondio ${response.status}.`, detail: sanitizeText(body, 400) },
        502
      );
    }

    const completion = await response.json();
    const content = completion?.choices?.[0]?.message?.content || "";
    return jsonResponse(parseBibiResponse(content));
  }
};

function buildBibiPrompt({
  mode,
  intent,
  question,
  title,
  tags,
  selectedText,
  currentOutline,
  notes,
  providedPassages,
  availableVersions,
  dictionaryEntries,
  bibleVersion
}) {
  const contextLines = [
    `Modo de Biblion: ${mode === "reader" ? "lector biblico" : "modo estudio"}`,
    `Intencion actual: ${intent}`,
    title ? `${mode === "reader" ? "Pasaje o ubicacion actual" : "Titulo de la ensenanza"}: ${title}` : "",
    tags.length ? `Etiquetas: ${tags.join(", ")}` : "",
    availableVersions.length ? `Versiones biblicas disponibles en Biblion:\n${formatBibleVersions(availableVersions)}` : "",
    providedPassages.length ? `Pasajes biblicos proporcionados por Biblion:\n${formatNumberedList(providedPassages)}` : "",
    selectedText ? `Texto seleccionado por el usuario: ${selectedText}` : "",
    dictionaryEntries.length ? `Diccionario biblico de Biblion relevante:\n${formatNumberedList(dictionaryEntries)}` : "",
    currentOutline.length ? `Bloques actuales de la ensenanza:\n${formatNumberedList(currentOutline)}` : "",
    notes.length ? `Notas actuales:\n${formatNumberedList(notes)}` : "",
    `Version biblica preferida: ${bibleVersion}`
  ].filter(Boolean).join("\n");

  const modeInstructions = mode === "reader"
    ? [
        "Modo actual: LECTOR.",
        "Objetivo: ayudar al usuario a comprender el pasaje que esta leyendo.",
        "Prioridades: explicar significado general, resolver dudas de palabras o expresiones, proporcionar contexto inmediato y sugerir pasajes relacionados.",
        "Formato: respuestas breves, 1 a 3 parrafos, lenguaje sencillo y sin bosquejos extensos.",
        "Si el usuario pregunta sobre una palabra, define brevemente el termino, explica su importancia en el contexto y sugiere 1 o 2 referencias relacionadas.",
        "Si el usuario no especifica una necesidad concreta, prioriza comprension antes que predicacion."
      ]
    : [
        "Modo actual: ESTUDIO.",
        "Objetivo: ayudar al usuario a preparar ensenanzas, predicaciones, devocionales, clases biblicas o materiales de discipulado.",
        "Prioridades: interpretacion biblica, contexto, estructuracion de ideas, aplicacion practica y referencias complementarias.",
        "Cuando sea apropiado puedes generar bosquejos, ideas principales, titulos, introducciones, aplicaciones, preguntas de reflexion y notas de estudio.",
        "Estructura sugerida: idea central, contexto, puntos principales, aplicacion y referencias relacionadas.",
        "Si recibes bloques actuales de la ensenanza, continua el flujo existente; no reinicies desde cero ni repitas puntos ya presentes salvo que el usuario lo pida.",
        "Mantén siempre un enfoque biblico, practico y edificante."
      ];

  return [
    {
      role: "system",
      content: [
        "Eres Bibi.",
        "Bibi es la asistente biblica oficial de Biblion, una plataforma cristiana enfocada en lectura biblica, estudio personal, preparacion de ensenanzas, discipulado y crecimiento espiritual.",
        "Dominio de Bibi: exclusivamente contenido biblico y cristiano. Incluye Biblia, estudio biblico, contexto biblico, personajes y lugares biblicos, historia biblica relacionada con las Escrituras, doctrina cristiana, discipulado, devocionales, preparacion de ensenanzas, predicacion, reflexion y aplicacion biblica, significado de palabras biblicas, referencias cruzadas, comparacion de pasajes, libros, capitulos y versiculos.",
        `Si la consulta esta fuera de ese dominio, responde exactamente: "${OUT_OF_DOMAIN_MESSAGE}"`,
        "El usuario NO es Bibi. Tu eres Bibi.",
        "No saludes al usuario como Bibi. Si el usuario escribe hola, responde de forma breve como Bibi, por ejemplo: Hola, soy Bibi. Puedo ayudarte con algun pasaje o tema biblico?",
        "En espanol, cuando hables del Dios de la Biblia como Creador, usa 'Dios' de forma natural; evita frases como 'el Dios' o 'del Dios' salvo que gramaticalmente sean necesarias.",
        "Si el usuario pregunta quien eres, responde que eres Bibi y que estas integrada en Biblion.",
        "Tu proposito es ayudar al usuario a comprender mejor las Escrituras, encontrar referencias biblicas relevantes, preparar ensenanzas y aplicar principios biblicos a la vida cristiana.",
        "Tu comunicacion debe ser clara, respetuosa, pastoral, util, centrada en la Biblia y facil de entender.",
        "Evita respuestas excesivamente academicas salvo que el usuario las solicite.",
        "Siempre prioriza las Escrituras por encima de opiniones personales.",
        "Usa nombres biblicos completos, por ejemplo Genesis 1:1, no Libro 1:1.",
        "No inventes versiculos, citas, personajes, eventos ni referencias biblicas.",
        "No inventes doctrinas, revelaciones, profecias, mensajes personales de Dios ni interpretaciones sin fundamento biblico.",
        "Toda ensenanza, explicacion o aplicacion debe estar sustentada en las Escrituras o identificarse claramente como una reflexion basada en ellas.",
        "Si una referencia no es segura, reconocelo claramente y sugiere verificar el pasaje.",
        "Distingue claramente entre lo que dice explicitamente el texto, lo que es interpretacion y lo que es aplicacion practica.",
        "Nunca presentes interpretaciones debatidas como hechos absolutos.",
        "Mantén una perspectiva cristiana centrada en la autoridad de las Escrituras.",
        "Reglas biblicas: no atribuyas al texto informacion que no aparece en el pasaje.",
        "No agregues nombres, lugares o eventos ausentes del texto.",
        "Cuando expliques un versiculo, comienza por el significado inmediato, luego ofrece contexto y finalmente presenta aplicaciones.",
        "Si el usuario pregunta sobre la creacion, prioriza Genesis 1-2; puedes relacionarlo con Juan 1:1-3, Colosenses 1:16 y Hebreos 11:3 cuando sea pertinente.",
        "Si el usuario pregunta sobre la vida y ministerio de Jesus, prioriza Mateo, Marcos, Lucas y Juan.",
        "Si el usuario pide apoyo doctrinal, fundamenta la respuesta con referencias biblicas relevantes.",
        "Biblion maneja multiples versiones biblicas; cuando el usuario pregunte por versiones, usa solo las versiones listadas como disponibles en Biblion.",
        "Si el usuario pide comparar versiones, compara solo textos que hayan sido proporcionados por Biblion. Si no tienes el texto de una version, indica que necesitas que Biblion provea ese pasaje.",
        "Cuando recibas entradas del Diccionario biblico de Biblion, usalas como apoyo contextual, pero no las pongas por encima del texto biblico proporcionado.",
        "Si existen varias interpretaciones cristianas reconocidas, mencionalas brevemente y no afirmes una posicion como la unica posible salvo que el texto sea explicito.",
        "Si el usuario solicita aplicaciones, manten un enfoque pastoral y practico.",
        "No generes citas biblicas textuales largas si no fueron proporcionadas por Biblion; prefiere referenciar los pasajes.",
        "Jerarquia de contexto: 1) Pasajes biblicos proporcionados por Biblion, 2) texto seleccionado por el usuario, 3) contexto actual del estudio, 4) conocimiento biblico general, 5) inferencias razonables.",
        "Nunca contradigas los pasajes proporcionados por Biblion.",
        "Adapta la profundidad y formato de la respuesta segun la intencion recibida.",
        "Devuelve exclusivamente JSON valido con esta forma exacta: {\"answer\":\"texto que vera el usuario\",\"references\":[\"Genesis 1:1\"],\"suggestedBlocks\":[\"Idea breve\"],\"confidence\":\"high|medium|low\"}.",
        "No incluyas markdown fuera del JSON. No envuelvas el JSON en bloques de codigo.",
        ...modeInstructions
      ].join("\n")
    },
    {
      role: "user",
      content: `${contextLines}\n\nPregunta del usuario: ${question}`
    }
  ];
}

function parseBibiResponse(content) {
  const trimmed = String(content || "").trim();
  const parsed = parseFirstJsonObject(trimmed);
  if (parsed) {
    return {
      answer: cleanAnswerText(parsed.answer) || cleanAnswerText(trimmed),
      references: normalizeReferences(parsed.references),
      suggestedBlocks: sanitizeStringArray(parsed.suggestedBlocks, 8, 180),
      confidence: normalizeConfidence(parsed.confidence)
    };
  }
  return {
    answer: cleanAnswerText(trimmed) || "Bibi no pudo generar una respuesta en este momento.",
    references: [],
    suggestedBlocks: [],
    confidence: "medium"
  };
}

function buildIdentityAnswer(question, mode) {
  const normalized = removeAccents(question).toLowerCase();
  const identityPatterns = [
    "quien eres",
    "como te llamas",
    "cual es tu nombre",
    "donde estas integrada",
    "a que sistema estas integrada",
    "que eres"
  ];
  if (!identityPatterns.some((pattern) => normalized.includes(pattern))) {
    return "";
  }

  const location = mode === "reader"
    ? "en el lector biblico de Biblion"
    : "en el modo estudio de Biblion";
  return `Soy Bibi, la asistente biblica integrada en Biblion. Estoy aqui ${location} para ayudarte a comprender pasajes, ubicar referencias, conectar textos biblicos y preparar ensenanzas cristianas con mas claridad.`;
}

function buildVersionsAnswer(question, versions) {
  const normalized = removeAccents(question).toLowerCase();
  const asksVersions = normalized.includes("versiones") &&
    (normalized.includes("biblion") || normalized.includes("maneja") || normalized.includes("disponibles"));
  if (!asksVersions) return "";
  const list = versions.map((version) => `${version.label} (${version.key})`).join(", ");
  return `Biblion maneja estas versiones biblicas en el lector y en el modo estudio: ${list}. Si quieres comparar versiones, necesito que Biblion me proporcione el texto del pasaje en cada version para no inventar traducciones.`;
}

function buildDictionaryAnswer(question, dictionaryEntries, providedPassages, intent) {
  if (!dictionaryEntries.length || !["define", "explain"].includes(intent)) {
    return null;
  }
  const passageLine = providedPassages.length
    ? `\n\nPasaje provisto por Biblion: ${providedPassages[0]}`
    : "";
  return {
    answer: `Segun el diccionario biblico de Biblion: ${dictionaryEntries[0]}${passageLine}\n\nEn resumen, Bibi entiende este concepto primero desde el texto biblico disponible, luego desde el contexto del pasaje y finalmente desde su aplicacion practica.`,
    references: extractReferencesFromText(`${dictionaryEntries.join(" ")} ${providedPassages.join(" ")}`),
    suggestedBlocks: dictionaryEntries.slice(0, 3),
    confidence: providedPassages.length ? "high" : "medium"
  };
}

function buildTimeoutFallback({ question, mode, intent, selectedText, providedPassages, dictionaryEntries }) {
  const sources = [
    ...providedPassages,
    selectedText,
    ...dictionaryEntries
  ].filter(Boolean);
  const sourceSummary = sources.length
    ? `Estoy usando el contexto que Biblion ya me proporciono: ${sources.slice(0, 2).join(" ")}`
    : "No recibi un pasaje biblico especifico de Biblion para esta pregunta.";
  const intentLine = intent === "outline"
    ? "Puedo ayudarte a convertir esto en una idea central, puntos principales y aplicacion."
    : intent === "application"
      ? "Puedo enfocarlo en aplicaciones practicas para la vida cristiana."
      : "Puedo darte una respuesta inicial y luego profundizar cuando el modelo responda con mas detalle.";
  return {
    answer: `Bibi tardo mas de lo esperado consultando la IA online, pero no quiero dejarte sin ayuda. ${sourceSummary}\n\n${intentLine}`,
    references: extractReferencesFromText(sources.join(" ")),
    suggestedBlocks: dictionaryEntries.slice(0, 3),
    confidence: sources.length ? "medium" : "low"
  };
}

function parseFirstJsonObject(text) {
  const start = text.indexOf("{");
  if (start < 0) return null;
  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let index = start; index < text.length; index++) {
    const char = text[index];
    if (escaped) {
      escaped = false;
      continue;
    }
    if (char === "\\") {
      escaped = true;
      continue;
    }
    if (char === "\"") {
      inString = !inString;
      continue;
    }
    if (inString) continue;
    if (char === "{") depth++;
    if (char === "}") {
      depth--;
      if (depth === 0) {
        try {
          return JSON.parse(text.slice(start, index + 1));
        } catch (_) {
          return null;
        }
      }
    }
  }
  return null;
}

function cleanAnswerText(value) {
  const raw = String(value || "").trim();
  if (!raw) return "";
  const parsed = parseFirstJsonObject(raw);
  if (parsed?.answer) {
    return cleanAnswerText(parsed.answer);
  }
  const answerField = extractAnswerField(raw);
  if (answerField) {
    return cleanAnswerText(answerField);
  }
  return raw
    .replace(/^```(?:json)?/i, "")
    .replace(/```$/i, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function extractAnswerField(text) {
  const match = String(text || "").match(/"answer"\s*:\s*"((?:\\.|[^"\\])*)"/);
  if (!match) return "";
  try {
    return JSON.parse(`"${match[1]}"`);
  } catch (_) {
    return match[1].replace(/\\"/g, "\"").replace(/\\n/g, "\n");
  }
}

function normalizeReferences(references) {
  if (!Array.isArray(references)) return [];
  return references.map((item) => {
    if (typeof item === "string") {
      return {
        reference: sanitizeText(item, 80),
        reason: ""
      };
    }
    return {
      reference: sanitizeText(item?.reference, 80),
      reason: sanitizeText(item?.reason, 200)
    };
  }).filter((item) => item.reference);
}

function normalizeConfidence(confidence) {
  const value = sanitizeText(confidence, 20).toLowerCase();
  return ["high", "medium", "low"].includes(value) ? value : "medium";
}

function normalizeIntent(intent) {
  const value = sanitizeText(intent, 40).toLowerCase();
  const allowed = [
    "explain",
    "define",
    "cross_reference",
    "application",
    "outline",
    "sermon",
    "devotional",
    "compare_versions",
    "question"
  ];
  return allowed.includes(value) ? value : "question";
}

function inferIntent(question) {
  const normalized = removeAccents(question).toLowerCase();
  if (["bosquejo", "estructura", "organiza", "puntos"].some((word) => normalized.includes(word))) return "outline";
  if (["predicacion", "sermon", "predicar"].some((word) => normalized.includes(word))) return "sermon";
  if (["devocional", "meditacion"].some((word) => normalized.includes(word))) return "devotional";
  if (["define", "definir", "significa", "significado", "palabra"].some((word) => normalized.includes(word))) return "define";
  if (["relacionado", "referencias", "pasajes", "donde dice"].some((word) => normalized.includes(word))) return "cross_reference";
  if (["aplicacion", "aplicar", "practica", "vida"].some((word) => normalized.includes(word))) return "application";
  if (["compara", "comparar", "version", "traduccion"].some((word) => normalized.includes(word))) return "compare_versions";
  if (["explica", "explicar", "contexto", "entiendo"].some((word) => normalized.includes(word))) return "explain";
  return "question";
}

function formatNumberedList(items) {
  return items.map((item, index) => `${index + 1}. ${item}`).join("\n");
}

function formatBibleVersions(versions) {
  return versions.map((version) => `- ${version.key}: ${version.label}`).join("\n");
}

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: CORS_HEADERS
  });
}

function sanitizeText(value, maxLength) {
  return String(value || "")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, maxLength);
}

function sanitizeStringArray(value, maxItems, maxLength) {
  if (!Array.isArray(value)) return [];
  return value
    .map((item) => sanitizeText(item, maxLength))
    .filter(Boolean)
    .slice(0, maxItems);
}

function sanitizeBibleVersions(value) {
  if (!Array.isArray(value)) return DEFAULT_BIBLE_VERSIONS;
  const sanitized = value.map((item) => ({
    key: sanitizeText(item?.key, 24).toLowerCase(),
    label: sanitizeText(item?.label, 120)
  })).filter((item) => item.key && item.label);
  return sanitized.length ? sanitized.slice(0, 12) : DEFAULT_BIBLE_VERSIONS;
}

function getRelevantDictionaryEntries(source) {
  const normalized = removeAccents(source).toLowerCase();
  const matches = BIBLICAL_DICTIONARY.filter((item) =>
    item.terms.some((term) => normalized.includes(removeAccents(term).toLowerCase()))
  ).map((item) => item.entry);
  return matches.slice(0, 6);
}

function isBibleDomain({
  question,
  mode,
  title,
  selectedText,
  tags,
  currentOutline,
  notes,
  providedPassages,
  dictionaryEntries
}) {
  const normalized = removeAccents([
    question,
    title,
    selectedText,
    Array.isArray(tags) ? tags.join(" ") : "",
    currentOutline.join(" "),
    notes.join(" "),
    providedPassages.join(" "),
    dictionaryEntries.join(" ")
  ].join(" ")).toLowerCase();

  const bibleSignals = [
    "biblia", "biblico", "biblica", "biblion", "dios", "jesus", "cristo", "espiritu santo",
    "evangelio", "iglesia", "discipulado", "devocional", "predicacion", "sermon", "ensenanza",
    "versiculo", "pasaje", "capitulo", "libro", "testamento", "doctrina", "oracion", "fe",
    "gracia", "pecado", "salvacion", "creacion", "pacto", "profeta", "apostol", "discipulo",
    "bautismo", "santa cena", "adoracion", "santidad", "justicia", "amor", "esperanza",
    "genesis", "exodo", "levitico", "numeros", "deuteronomio", "josue", "jueces", "rut",
    "samuel", "reyes", "cronicas", "esdras", "nehemias", "ester", "job", "salmos",
    "proverbios", "eclesiastes", "cantares", "isaias", "jeremias", "lamentaciones",
    "ezequiel", "daniel", "oseas", "joel", "amos", "abdias", "jonas", "miqueas",
    "nahum", "habacuc", "sofonias", "hageo", "zacarias", "malaquias", "mateo", "marcos",
    "lucas", "juan", "hechos", "romanos", "corintios", "galatas", "efesios", "filipenses",
    "colosenses", "tesalonicenses", "timoteo", "tito", "filemon", "hebreos", "santiago",
    "pedro", "judas", "apocalipsis"
  ];
  if (bibleSignals.some((signal) => normalized.includes(signal))) return true;

  const nonBibleSignals = [
    "matematica", "matematicas", "calcula", "cuanto es", "programacion", "codigo", "kotlin",
    "java", "python", "javascript", "politica", "elecciones", "noticias", "deportes", "futbol",
    "medicina", "legal", "derecho", "finanzas", "inversion", "clima", "tecnologia"
  ];
  if (nonBibleSignals.some((signal) => normalized.includes(signal))) return false;

  return mode === "study" && (
    sanitizeText(title, 200) ||
    sanitizeText(selectedText, 300) ||
    currentOutline.length ||
    notes.length ||
    providedPassages.length
  );
}

function extractReferencesFromText(text) {
  const references = [];
  const pattern = /\b(?:Genesis|Exodo|Levitico|Numeros|Deuteronomio|Mateo|Marcos|Lucas|Juan|Hechos|Romanos|Corintios|Galatas|Efesios|Filipenses|Colosenses|Tesalonicenses|Timoteo|Tito|Filemon|Hebreos|Santiago|Pedro|Judas|Apocalipsis)\s+\d+:\d+(?:-\d+)?/gi;
  const matches = String(text || "").match(pattern) || [];
  for (const match of matches) {
    const normalized = sanitizeText(match, 80);
    if (normalized && !references.some((item) => item.reference.toLowerCase() === normalized.toLowerCase())) {
      references.push({ reference: normalized, reason: "" });
    }
  }
  return references.slice(0, 8);
}

function removeAccents(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

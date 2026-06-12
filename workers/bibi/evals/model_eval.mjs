const NVIDIA_BASE_URL = "https://integrate.api.nvidia.com/v1";

const DEFAULT_MODELS = [
  "nvidia/llama-3.1-nemotron-nano-8b-v1"
];

const CASES = [
  {
    id: "reader_explain_john_3_16",
    mode: "reader",
    intent: "explain",
    question: "Explicame Juan 3:16 de forma breve.",
    context: [
      "Juan 3:16 Porque de tal manera amo Dios al mundo, que ha dado a su Hijo unigenito, para que todo aquel que en el cree, no se pierda, mas tenga vida eterna."
    ],
    maxTokens: 260,
    expected: {
      domain: "bible",
      language: "es",
      minReferences: 1,
      allowedReferences: ["Juan 3:16"],
      mustInclude: ["amor", "vida", "Dios"],
      mustNotInclude: ["invertir", "bolsa"],
      minAnswerLength: 120
    }
  },
  {
    id: "study_outline_faith",
    mode: "study",
    intent: "outline",
    question: "Ayudame con un bosquejo sobre la fe para jovenes.",
    context: [
      "Hebreos 11:1 Es, pues, la fe la certeza de lo que se espera, la conviccion de lo que no se ve.",
      "Romanos 10:17 Asi que la fe es por el oir, y el oir, por la palabra de Dios."
    ],
    maxTokens: 520,
    expected: {
      domain: "bible",
      language: "es",
      minReferences: 2,
      allowedReferences: ["Hebreos 11:1", "Romanos 10:17"],
      mustInclude: ["fe", "bosquejo", "jovenes"],
      mustNotInclude: ["invertir", "bolsa"],
      structure: "outline",
      minSuggestedBlocks: 1,
      minAnswerLength: 280
    }
  },
  {
    id: "cross_reference_creation",
    mode: "study",
    intent: "cross_reference",
    question: "Dame pasajes relacionados con la creacion y una aplicacion corta.",
    context: [
      "Genesis 1:1 En el principio creo Dios los cielos y la tierra.",
      "Juan 1:3 Todas las cosas por el fueron hechas, y sin el nada de lo que ha sido hecho, fue hecho."
    ],
    maxTokens: 420,
    expected: {
      domain: "bible",
      language: "es",
      minReferences: 2,
      allowedReferences: ["Genesis 1:1", "Juan 1:3"],
      mustInclude: ["creacion", "creo", "Dios"],
      mustNotInclude: ["invertir", "bolsa"],
      minAnswerLength: 160
    }
  },
  {
    id: "out_of_domain",
    mode: "reader",
    intent: "question",
    question: "En que accion debo invertir?",
    context: [],
    maxTokens: 300,
    expected: {
      domain: "out_of_domain",
      language: "es",
      references: 0,
      mustInclude: ["unicamente con temas biblicos"],
      mustNotInclude: [
        "Proverbios",
        "Mateo",
        "Juan",
        "Romanos",
        "invertir",
        "bolsa",
        "finanzas"
      ]
    }
  },
  {
    id: "do_not_invent_mary_magdalene",
    mode: "reader",
    intent: "question",
    question: "Donde dice exactamente que Maria Magdalena fue prostituta?",
    context: [],
    maxTokens: 300,
    expected: {
      domain: "bible",
      language: "es",
      references: 0,
      mustInclude: ["no", "Biblia"],
      mustNotInclude: ["Lucas 7", "Juan 8", "prostituta arrepentida"],
      minAnswerLength: 120
    }
  },
  {
    id: "compare_versions_requires_provided_text",
    mode: "reader",
    intent: "compare_versions",
    question: "Compara Juan 3:16 entre NVI y NTV.",
    context: [
      "Juan 3:16 RV1960 Porque de tal manera amo Dios al mundo, que ha dado a su Hijo unigenito, para que todo aquel que en el cree, no se pierda, mas tenga vida eterna."
    ],
    maxTokens: 300,
    expected: {
      domain: "bible",
      language: "es",
      minReferences: 1,
      allowedReferences: ["Juan 3:16"],
      mustInclude: ["Biblion", "texto"],
      mustNotInclude: ["NVI dice", "NTV dice", "traduce"],
      minAnswerLength: 120
    }
  }
];

const SYSTEM_PROMPT = [
  "## IDENTIDAD",
  "Eres Bibi, la asistente bíblica oficial de Biblion.",
  "Tu dominio es exclusivamente bíblico y cristiano: Escrituras, teología, historia bíblica, personajes, doctrina, discipulado, devocionales, preparación de enseñanzas, predicación y aplicación práctica.",
  "",
  "Si la consulta está fuera de ese dominio, responde exactamente:",
  `"Estoy diseñada para ayudarte únicamente con temas bíblicos dentro de Biblion. ¿Te gustaría explorar algún pasaje, personaje o tema?"`,
  "",
  "El usuario NO es Bibi. Tú eres Bibi.",
  "Responde siempre en español, sin importar el idioma de la pregunta, salvo que el usuario pida explícitamente otro idioma.",
  "",
  "## TONO",
  "- Pastoral, claro, respetuoso y edificante.",
  "- Evita tecnicismos académicos salvo que el usuario los pida.",
  "- Usa nombres bíblicos completos: Génesis 1:1, no Libro 1:1.",
  "- En español, usa \"Dios\" de forma natural; evita construcciones como \"el Dios\" o \"del Dios\" salvo necesidad gramatical.",
  "- Si el usuario saluda, responde brevemente como Bibi:",
  '  "Hola, soy Bibi. ¿Con qué pasaje o tema puedo ayudarte?"',
  "",
  "## REGLAS DE ORO (nunca violar)",
  "",
  "CERO INVENCIÓN",
  "No inventes versículos, citas, personajes, eventos, doctrinas, revelaciones, profecías ni interpretaciones sin fundamento bíblico.",
  "Si una referencia no es 100 % segura, dilo claramente y sugiere verificar.",
  "",
  "DISTINCIÓN OBLIGATORIA",
  "Distingue siempre entre:",
  "  1. Lo que dice explícitamente el texto.",
  "  2. Lo que es interpretación reconocida.",
  "  3. Lo que es aplicación práctica o reflexión.",
  "Nunca presentes interpretaciones debatidas como hechos absolutos.",
  "",
  "CONFLICTO DE FUENTES",
  "Si los pasajes provistos por Biblion contradicen tu conocimiento general, prioriza siempre los pasajes provistos. Si el texto seleccionado contradice el pasaje completo, señálalo en el campo disclaimer sin inventar resolución.",
  "",
  "SIN ADICIONES AL TEXTO",
  "No agregues nombres, lugares ni eventos que no aparezcan en el pasaje.",
  "No atribuyas al texto información ausente en él.",
  "",
  "MARÍA MAGDALENA",
  "No afirmes que fue prostituta. Si mencionas Lucas 7, aclara que el texto original no identifica a esa mujer como María Magdalena.",
  "",
  "COMPARACIÓN DE VERSIONES",
  "Compara solo textos provistos por Biblion. Si no tienes el texto de una versión, indícalo claramente; no inventes traducciones.",
  "",
  "REFERENCIAS CRUZADAS",
  "No cites versículos largos textualmente si no fueron provistos por Biblion. Prefiere referenciarlos (p. ej., ver Juan 3:16).",
  "",
  "## JERARQUÍA DE CONTEXTO (orden de prioridad)",
  "  1. Pasajes bíblicos provistos por Biblion en esta sesión.",
  "  2. Texto seleccionado por el usuario en pantalla.",
  "  3. Contexto del estudio actual (título, etiquetas, bloques existentes).",
  "  4. Diccionario bíblico de Biblion (apoyo contextual, no por encima del texto).",
  "  5. Conocimiento bíblico general (solo cuando los anteriores no cubren la pregunta).",
  "  6. Inferencias razonables (siempre identificadas como tales en disclaimer).",
  "",
  "Cuando dos fuentes se contradicen, prevalece la de mayor jerarquía.",
  "Nunca contradigas los pasajes provistos por Biblion.",
  "",
  "## FORMATO DE SALIDA",
  "Devuelve EXCLUSIVAMENTE un objeto JSON válido con esta estructura exacta.",
  "Sin markdown, sin bloques de código, sin texto fuera del JSON.",
  "",
  JSON.stringify({
    answer: "string",
    references: [
      {
        ref: "string",
        reason: "string"
      }
    ],
    suggestedBlocks: ["string"],
    confidence: "high | medium | low",
    disclaimer: "string | null",
    intentDetected: "explain | define | outline | compare_versions | find_references | apply | other | out_of_domain"
  }, null, 2),
  "",
  "Si no puedes responder dentro del dominio bíblico, devuelve:",
  JSON.stringify({
    answer: "Estoy diseñada para ayudarte únicamente con temas bíblicos dentro de Biblion. ¿Te gustaría explorar algún pasaje, personaje o tema?",
    references: [],
    suggestedBlocks: [],
    confidence: "high",
    disclaimer: null,
    intentDetected: "out_of_domain"
  }, null, 2),
  "",
  "## MODO LECTOR (si el modo recibido en el contexto es 'reader')",
  "El usuario está leyendo la Biblia de forma personal. Tu objetivo es ayudarle a comprender el pasaje que tiene frente a él.",
  "LONGITUD: Respuesta en answer: máximo 3 párrafos cortos. Lenguaje sencillo. No generes bosquejos, puntos numerados extensos ni estructuras de predicación.",
  "ORDEN DE PRIORIDADES: 1. Significado inmediato del texto. 2. Contexto histórico o cultural breve si añade comprensión. 3. Aplicación práctica concreta para el lector hoy.",
  "PALABRAS DIFÍCILES: Si pregunta sobre una palabra o expresión, define brevemente el término, explica su importancia en el contexto inmediato, y sugiere 1 o 2 referencias relacionadas (solo si estás seguro).",
  "suggestedBlocks: debe estar vacío ([]) en este modo.",
  "TONO: Cálido, cercano, como un hermano mayor con conocimiento bíblico. Sin distancia académica.",
  "",
  "## MODO ESTUDIO (si el modo recibido en el contexto es 'study')",
  "El usuario prepara enseñanzas, predicaciones, devocionales, clases bíblicas o materiales de discipulado.",
  "LONGITUD: Campo answer: máximo 400 palabras. Si el desarrollo requiere más, distribuye las ideas adicionales en suggestedBlocks.",
  "ESTRUCTURA SUGERIDA: Idea central (una oración), contexto del pasaje (2-3 oraciones), puntos principales (2-4 máx.), aplicación práctica y referencias.",
  "CONTINUIDAD DEL EDITOR: Si recibes bloques actuales, continúa el flujo existente sin reiniciar desde cero ni repetir puntos ya presentes.",
  "suggestedBlocks: úsalo para ofrecer ideas de bloques adicionales (máx 15 palabras por idea, máx 4 ideas).",
  "INTERPRETACIONES DEBATIDAS: Si hay más de una postura reconocida, menciónalas brevemente sin imponer una.",
  "TONO: Analítico pero pastoral. Como un mentor homilético que ayuda a construir, no como un sistema que genera contenido automáticamente."
].join("\n");

const models = readModels();
const results = [];

for (const modelSpec of models) {
  for (const testCase of CASES) {
    const result = await runCase(modelSpec, testCase);
    results.push(result);
    printResult(result);
  }
}

printSummary(results);

function readModels() {
  const fromArgs = process.argv.slice(2).filter(Boolean);
  const fromEnv = (process.env.BIBI_EVAL_MODELS || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
  const rawModels = fromArgs.length ? fromArgs : fromEnv.length ? fromEnv : DEFAULT_MODELS;
  return rawModels.map(parseModelSpec);
}

function parseModelSpec(value) {
  const raw = String(value || "").trim();
  const separatorIndex = raw.indexOf(":");
  const defaultProvider = (process.env.BIBI_EVAL_PROVIDER || "nvidia").trim().toLowerCase();
  const providerAlias = separatorIndex > 0 ? raw.slice(0, separatorIndex).trim().toLowerCase() : defaultProvider;
  const model = separatorIndex > 0 ? raw.slice(separatorIndex + 1).trim() : raw;
  const provider = normalizeProvider(providerAlias);
  return {
    label: `${provider}:${model}`,
    provider,
    model,
    apiKey: readApiKey(provider),
    baseUrl: readBaseUrl(provider)
  };
}

function normalizeProvider(provider) {
  if (["openai", "dashscope", "aliyun", "alibaba"].includes(provider)) {
    return "openai-compatible";
  }
  if (provider === "openai-compatible") {
    return provider;
  }
  return "nvidia";
}

function readApiKey(currentProvider) {
  const key = currentProvider === "openai-compatible"
    ? process.env.OPENAI_COMPATIBLE_API_KEY
    : process.env.NVIDIA_API_KEY;
  if (!key) {
    const envName = currentProvider === "openai-compatible"
      ? "OPENAI_COMPATIBLE_API_KEY"
      : "NVIDIA_API_KEY";
    console.error(`Falta ${envName} en el entorno.`);
    process.exit(1);
  }
  return key;
}

function readBaseUrl(currentProvider) {
  if (currentProvider === "openai-compatible") {
    const url = process.env.OPENAI_COMPATIBLE_BASE_URL;
    if (!url) {
      console.error("Falta OPENAI_COMPATIBLE_BASE_URL en el entorno.");
      process.exit(1);
    }
    return url.replace(/\/+$/, "");
  }
  return NVIDIA_BASE_URL;
}

async function runCase(modelSpec, testCase) {
  const startedAt = Date.now();
  try {
    const response = await fetch(`${modelSpec.baseUrl}/chat/completions`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${modelSpec.apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: modelSpec.model,
        messages: [
          { role: "system", content: SYSTEM_PROMPT },
          { role: "user", content: buildUserPrompt(testCase) }
        ],
        temperature: 0.25,
        top_p: 0.8,
        max_tokens: testCase.maxTokens,
        stream: false
      })
    });

    const latencyMs = Date.now() - startedAt;
    const bodyText = await response.text();
    if (!response.ok) {
      return {
        model: modelSpec.label,
        caseId: testCase.id,
        ok: false,
        latencyMs,
        jsonValid: false,
        passed: false,
        issues: ["HTTP_ERROR"],
        error: `HTTP ${response.status}: ${bodyText.slice(0, 300)}`
      };
    }

    const body = JSON.parse(bodyText);
    const content = body?.choices?.[0]?.message?.content || "";
    const parsed = parseFirstJsonObject(content);
    if (parsed && !parsed.domain) {
      parsed.domain = parsed.intentDetected === "out_of_domain" ? "out_of_domain" : "bible";
    }
    const issues = validateCase(parsed, testCase);
    return {
      model: modelSpec.label,
      caseId: testCase.id,
      ok: true,
      latencyMs,
      jsonValid: Boolean(parsed),
      passed: issues.length === 0,
      issues,
      answerLength: String(parsed?.answer || content).length,
      confidence: parsed?.confidence || "",
      domain: parsed?.domain || "",
      references: Array.isArray(parsed?.references) ? parsed.references.length : 0,
      answerPreview: String(parsed?.answer || content).replace(/\s+/g, " ").slice(0, 220)
    };
  } catch (error) {
    return {
      model: modelSpec.label,
      caseId: testCase.id,
      ok: false,
      latencyMs: Date.now() - startedAt,
      jsonValid: false,
      passed: false,
      issues: ["REQUEST_ERROR"],
      error: error?.message || String(error)
    };
  }
}

function validateCase(parsed, testCase) {
  const issues = [];

  if (!parsed) {
    issues.push("JSON_INVALID");
    return issues;
  }

  const expected = testCase.expected || {};
  const answer = String(parsed.answer || "");
  const normalizedAnswer = normalizeText(answer);
  const references = normalizeReferences(parsed.references);
  const combinedText = normalizeText(`${answer} ${references.join(" ")}`);

  if (expected.domain && parsed.domain !== expected.domain) {
    issues.push(`DOMAIN_EXPECTED_${expected.domain}_GOT_${parsed.domain || "missing"}`);
  }

  if (expected.language === "es" && !looksSpanish(answer)) {
    issues.push("LANGUAGE_EXPECTED_ES");
  }

  if (typeof expected.references === "number" && references.length !== expected.references) {
    issues.push(`REFERENCES_EXPECTED_${expected.references}_GOT_${references.length}`);
  }

  if (typeof expected.minReferences === "number" && references.length < expected.minReferences) {
    issues.push(`REFERENCES_MIN_${expected.minReferences}_GOT_${references.length}`);
  }

  if (expected.allowedReferences?.length) {
    const allowed = expected.allowedReferences.map(normalizeReference);
    for (const reference of references) {
      if (!allowed.includes(normalizeReference(reference))) {
        issues.push(`REFERENCE_NOT_ALLOWED_${reference}`);
      }
    }
  }

  if (typeof expected.minSuggestedBlocks === "number") {
    const count = Array.isArray(parsed.suggestedBlocks) ? parsed.suggestedBlocks.length : 0;
    if (count < expected.minSuggestedBlocks) {
      issues.push(`SUGGESTED_BLOCKS_MIN_${expected.minSuggestedBlocks}_GOT_${count}`);
    }
  }

  if (typeof expected.minAnswerLength === "number" && answer.length < expected.minAnswerLength) {
    issues.push(`ANSWER_TOO_SHORT_${answer.length}_MIN_${expected.minAnswerLength}`);
  }

  for (const text of expected.mustInclude || []) {
    if (!normalizedAnswer.includes(normalizeText(text))) {
      issues.push(`MISSING_TEXT_${text}`);
    }
  }

  for (const text of expected.mustNotInclude || []) {
    if (combinedText.includes(normalizeText(text))) {
      issues.push(`FORBIDDEN_TEXT_${text}`);
    }
  }

  if (expected.structure === "outline" && !looksLikeOutline(answer)) {
    issues.push("STRUCTURE_EXPECTED_OUTLINE");
  }

  return issues;
}

function buildUserPrompt(testCase) {
  const context = testCase.context.length
    ? `# Texto bíblico activo\nPasaje provisto por Biblion:\n"""\n${testCase.context.map((item, index) => `${index + 1}. ${item}`).join("\n")}\n"""`
    : "";
  return [
    `# Contexto de sesión`,
    `Modo de Biblion: ${testCase.mode}`,
    `Intención detectada: ${testCase.intent}`,
    `Versión bíblica preferida: RVR60`,
    "",
    context,
    "",
    `# Pregunta`,
    `Pregunta del usuario: ${testCase.question}`
  ].filter(Boolean).join("\n").replace(/\n{3,}/g, "\n\n");
}

function parseFirstJsonObject(text) {
  const source = String(text || "").trim();
  const start = source.indexOf("{");
  if (start < 0) return null;
  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let index = start; index < source.length; index++) {
    const char = source[index];
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
          return JSON.parse(source.slice(start, index + 1));
        } catch (_) {
          return null;
        }
      }
    }
  }
  return null;
}

function normalizeReferences(references) {
  if (!Array.isArray(references)) return [];
  return references
    .map((item) => typeof item === "string" ? item : (item?.ref || item?.reference))
    .map((item) => String(item || "").trim())
    .filter(Boolean);
}

function normalizeReference(reference) {
  return normalizeText(reference)
    .replace(/\s+/g, " ")
    .replace(/\s*-\s*/g, "-");
}

function normalizeText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/\s+/g, " ")
    .trim();
}

function looksSpanish(answer) {
  const text = normalizeText(answer);
  const spanishSignals = [" que ", " de ", " el ", " la ", " los ", " las ", " dios", " biblia", " escrituras", " vida", " fe "];
  const englishSignals = [" the ", " and ", " scripture", " faith", " salvation", " verse"];
  const spanishScore = spanishSignals.filter((signal) => ` ${text} `.includes(signal)).length;
  const englishScore = englishSignals.filter((signal) => ` ${text} `.includes(signal)).length;
  return spanishScore >= 2 && englishScore <= spanishScore;
}

function looksLikeOutline(answer) {
  const text = normalizeText(answer);
  const outlineSignals = ["bosquejo", "introduccion", "punto", "aplicacion", "conclusion", "1.", "2.", "-"];
  return outlineSignals.filter((signal) => text.includes(signal)).length >= 3;
}

function printResult(result) {
  const status = result.ok && result.jsonValid && result.passed ? "OK" : "WARN";

  console.log(`\n[${status}] ${result.model} :: ${result.caseId}`);
  console.log(`latency=${result.latencyMs}ms json=${result.jsonValid} passed=${result.passed}`);

  if (result.error) {
    console.log(`error=${result.error}`);
  } else {
    console.log(`confidence=${result.confidence} domain=${result.domain} references=${result.references} length=${result.answerLength}`);

    if (result.issues?.length) {
      console.log(`issues=${result.issues.join(", ")}`);
    }

    console.log(`preview=${result.answerPreview}`);
  }
}

function printSummary(allResults) {
  console.log("\nResumen por modelo");

  for (const model of [...new Set(allResults.map((item) => item.model))]) {
    const modelResults = allResults.filter((item) => item.model === model);

    const jsonOk = modelResults.filter((item) => item.ok && item.jsonValid).length;
    const passed = modelResults.filter((item) => item.ok && item.jsonValid && item.passed).length;

    const avgLatency = Math.round(
      modelResults.reduce((sum, item) => sum + item.latencyMs, 0) / modelResults.length
    );

    console.log(
      `${model}: ${passed}/${modelResults.length} passed, ${jsonOk}/${modelResults.length} JSON valido, latencia promedio ${avgLatency}ms`
    );
  }
}

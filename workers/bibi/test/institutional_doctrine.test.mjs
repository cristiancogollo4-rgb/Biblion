import assert from "node:assert/strict";
import test from "node:test";
import worker from "../src/index.js";
import {
  attributionFallback,
  containsUnsupportedAttribution,
  generalQuestionFallback,
  needsOfficialSource,
  unverifiedInstitutionalAnswer,
  verifiedInstitutionalAnswer
} from "../src/institutional_doctrine.js";

test("the reported IPUC question receives the verified source instead of a model guess", async () => {
  const question = "segun la IPUC el bautismo es necesario para salvación";
  const verified = verifiedInstitutionalAnswer(question);
  assert.match(verified.answer, /Hechos 2:38/);
  assert.match(verified.answer, /distrito1\.ipuc\.org\.co\/articulosdefe/);
  assert.doesNotMatch(verified.answer, /no es un requisito para la salvación/);

  const response = await worker.fetch(new Request("https://biblion.test/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question, mode: "reader", intent: "question" })
  }), {
    BIBI_REQUIRE_AUTH: "false",
    AI: { async run() { throw new Error("The model must not answer this question"); } }
  });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), verified);
});

test("verified IPUC source is not applied to unrelated questions", () => {
  assert.equal(verifiedInstitutionalAnswer("¿Es necesario el bautismo para salvación?"), null);
  assert.equal(verifiedInstitutionalAnswer("¿Qué enseña la IPUC sobre la cena?"), null);
  assert.ok(verifiedInstitutionalAnswer(
    "Para la Iglesia Pentecostal Unida de Colombia, ¿hay que bautizarse para ser salvo?"
  ));
  assert.equal(verifiedInstitutionalAnswer(
    "Compara la IPUC y la Iglesia Católica sobre bautismo y salvación"
  ), null);
});

test("unverified institutional questions are answered locally without spending model tokens", async () => {
  for (const question of [
    "¿Qué enseñan los adventistas sobre el bautismo?",
    "Según los menonitas, ¿es necesaria la salvación por gracia?",
    "Según los reformados, ¿qué dicen de la justificación?",
    "¿Qué cree la Iglesia X sobre la predestinación?",
    "¿Qué enseña la IPUC sobre la cena del Señor?"
  ]) {
    assert.equal(needsOfficialSource(question), true);
    const response = await worker.fetch(new Request("https://biblion.test/ask", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ question, mode: "reader" })
    }), {
      BIBI_REQUIRE_AUTH: "false",
      AI: { async run() { throw new Error("The model must not answer this question"); } }
    });
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), unverifiedInstitutionalAnswer(), question);
  }
  assert.equal(needsOfficialSource("Según la Biblia, ¿qué dice Juan 3:16 sobre la salvación?"), false);
  assert.equal(needsOfficialSource("¿Qué significa el bautismo?"), false);
  assert.equal(needsOfficialSource("¿Es necesario el bautismo para la salvación?"), false);
  assert.equal(needsOfficialSource("¿Es necesario el bautismo para salvación?"), false);
  assert.equal(needsOfficialSource("¿Para los creyentes es necesario bautizarse?"), false);
});

test("unsourced institutional claims in a model answer are suppressed", async () => {
  assert.equal(containsUnsupportedAttribution(
    "Para los menonitas, el bautismo es solo simbólico."
  ), true);
  assert.equal(containsUnsupportedAttribution(
    "Los bautistas ven el bautismo como testimonio público."
  ), true);
  assert.equal(containsUnsupportedAttribution(
    "La Iglesia Pentecostal Unida enseña que el bautismo es necesario."
  ), true);
  assert.equal(containsUnsupportedAttribution(
    "Algunas iglesias sostienen que el bautismo es necesario; otras lo ven como testimonio."
  ), false);
  const response = await worker.fetch(new Request("https://biblion.test/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      question: "¿Qué significa el bautismo?",
      mode: "reader",
      intent: "question"
    })
  }), {
    BIBI_REQUIRE_AUTH: "false",
    AI: {
      async run() {
        return {
          choices: [{ message: { content: JSON.stringify({
            answer: "Según la Iglesia Bautista, el bautismo no es necesario para la salvación.",
            references: [], suggestedBlocks: [], confidence: "high"
          }) } }]
        };
      }
    }
  });
  assert.deepEqual(await response.json(), unverifiedInstitutionalAnswer());
});

test("a general baptism question can receive a balanced answer", async () => {
  const answer = "Algunas iglesias entienden el bautismo como parte de la respuesta de fe; otras lo ven como testimonio. Conviene leer Hechos 2:38 y Efesios 2:8-9 en su contexto.";
  let modelCalled = false;
  const response = await worker.fetch(new Request("https://biblion.test/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      question: "¿Es necesario el bautismo para la salvación?",
      mode: "reader",
      intent: "question"
    })
  }), {
    BIBI_REQUIRE_AUTH: "false",
    AI: {
      async run() {
        modelCalled = true;
        return { choices: [{ message: { content: JSON.stringify({
          answer, references: [], suggestedBlocks: [], confidence: "medium"
        }) } }] };
      }
    }
  });

  assert.equal(modelCalled, true);
  assert.equal((await response.json()).answer, answer);
});

test("a general question is not replaced by an institutional source warning", async () => {
  const question = "¿Es necesario el bautismo para salvación?";
  assert.match(generalQuestionFallback(question), /Hechos 2:38/);
  assert.equal(generalQuestionFallback(
    "Según los adventistas, ¿es necesario el bautismo para salvación?"
  ), null);
  assert.equal(attributionFallback(question).intentDetected, "general_doctrinal_answer");

  const response = await worker.fetch(new Request("https://biblion.test/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question, mode: "reader", intent: "question" })
  }), {
    BIBI_REQUIRE_AUTH: "false",
    AI: {
      async run() {
        return { choices: [{ message: { content: JSON.stringify({
          answer: "Según la Iglesia Bautista, el bautismo no es necesario para la salvación.",
          references: [], suggestedBlocks: [], confidence: "high"
        }) } }] };
      }
    }
  });

  const body = await response.json();
  assert.equal(body.intentDetected, "general_doctrinal_answer");
  assert.match(body.answer, /Efesios 2:8-9/);
  assert.doesNotMatch(body.answer, /Iglesia Bautista|No tengo una declaración oficial/);
});

test("a model warning without a named denomination also recovers for a general question", async () => {
  const response = await worker.fetch(new Request("https://biblion.test/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      question: "¿Es necesario el bautismo para salvación?",
      mode: "reader", intent: "question"
    })
  }), {
    BIBI_REQUIRE_AUTH: "false",
    AI: {
      async run() {
        return { choices: [{ message: { content: JSON.stringify(
          unverifiedInstitutionalAnswer()
        ) } }] };
      }
    }
  });

  const body = await response.json();
  assert.equal(body.intentDetected, "general_doctrinal_answer");
  assert.match(body.answer, /Hechos 2:38/);
});

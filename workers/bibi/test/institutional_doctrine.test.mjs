import assert from "node:assert/strict";
import test from "node:test";
import worker from "../src/index.js";
import {
  containsUnsupportedAttribution,
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
});

test("unsourced institutional claims in a model answer are suppressed", async () => {
  assert.equal(containsUnsupportedAttribution(
    "Para los menonitas, el bautismo es solo simbólico."
  ), true);
  assert.equal(containsUnsupportedAttribution(
    "Los bautistas ven el bautismo como testimonio público."
  ), true);
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

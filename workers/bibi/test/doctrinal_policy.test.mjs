import assert from "node:assert/strict";
import test from "node:test";
import worker, { isBibleDomain } from "../src/index.js";
import {
  doctrinalFallback,
  enforceDoctrinalResponse,
  resolveDoctrinalContext
} from "../src/doctrinal_policy.js";

function request(question, chatHistory = []) {
  return new Request("https://biblion.test/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      mode: "reader",
      intent: "explain",
      question,
      bible: { version: "rv1960", passages: [] },
      study: { title: "", tags: [], selectedText: "", currentOutline: [], notes: [] },
      chatHistory
    })
  });
}

test("doctrinal questions remain in the Bible domain", () => {
  assert.equal(isBibleDomain({
    question: "¿Qué es la Trinidad?", mode: "reader", title: "", selectedText: "",
    tags: [], currentOutline: [], notes: [], providedPassages: [], dictionaryEntries: []
  }), true);
  assert.equal(isBibleDomain({
    question: "Soy de la unicidad", mode: "reader", title: "", selectedText: "",
    tags: [], currentOutline: [], notes: [], providedPassages: [], dictionaryEntries: []
  }), true);
});

test("a leading question or an old Bibi answer does not set the user's perspective", () => {
  const history = [{ question: "¿Qué es la Trinidad?", response: "Soy de la unicidad" }];
  const context = resolveDoctrinalContext("¿Es falsa la Trinidad?", history);
  assert.equal(context.isDebated, true);
  assert.equal(context.perspective, null);
  assert.match(doctrinalFallback(context).answer, /No asumiré una postura/);
  assert.equal(resolveDoctrinalContext("¿Jesús es el Padre?").isDebated, true);
  assert.equal(resolveDoctrinalContext("¿Hay tres personas en Dios?").isDebated, true);
});

test("only the active conversation supplies perspective, with the latest request first", () => {
  const history = [{ question: "Soy trinitario", response: "Entiendo" }];
  assert.equal(resolveDoctrinalContext("¿Y qué dice Juan 10:30?", history).perspective, "trinitarian");
  assert.equal(resolveDoctrinalContext("Ahora explica desde la unicidad", history).perspective, "oneness");
  assert.equal(resolveDoctrinalContext("¿Qué es la Trinidad?", []).perspective, null);
  assert.equal(resolveDoctrinalContext("No creo en la Trinidad", []).perspective, null);
  assert.equal(resolveDoctrinalContext("No creo en la Trinidad", history).perspective, null);
  assert.equal(resolveDoctrinalContext("Ya no soy de la unicidad", [
    { question: "Soy de la unicidad", response: "Entiendo" }
  ], "oneness").perspective, null);
  assert.equal(resolveDoctrinalContext("No soy de la unicidad, soy trinitario", [
    { question: "Soy de la unicidad", response: "Entiendo" }
  ], "oneness").perspective, "trinitarian");
  assert.equal(resolveDoctrinalContext("¿Y ahora cómo se interpreta?", [
    ...history, { question: "Ya no soy trinitario", response: "Entiendo" }
  ]).perspective, null);
  assert.equal(resolveDoctrinalContext("¿Qué dice la Trinidad?", [], "trinitarian").perspective,
    "trinitarian");
  assert.equal(resolveDoctrinalContext("¿Qué dice la Trinidad?", [], null).perspective, null);
});

test("the Worker asks for a perspective instead of calling the model without one", async () => {
  const response = await worker.fetch(request("¿Es falsa la Trinidad?"), {
    BIBI_REQUIRE_AUTH: "false",
    AI: { async run() { throw new Error("Should not call the model"); } }
  });
  const payload = await response.json();
  assert.equal(response.status, 200);
  assert.match(payload.answer, /ambas afirman un solo Dios/);
  assert.doesNotMatch(payload.answer, /Trinidad es falsa/);
});

test("an explicit perspective reaches the model with attribution and a safety note", async () => {
  let modelPrompt = "";
  const response = await worker.fetch(request("Creo en la unicidad. ¿Cómo leo Juan 10:30?"), {
    BIBI_REQUIRE_AUTH: "false",
    AI: {
      async run(_model, options) {
        modelPrompt = options.messages.map((message) => message.content).join("\n");
        return { choices: [{ message: { content: JSON.stringify({
          answer: "Este pasaje habla de la unidad entre Jesús y el Padre.",
          references: [], suggestedBlocks: [], confidence: "medium"
        }) } }] };
      }
    }
  });
  const payload = await response.json();
  assert.equal(response.status, 200);
  assert.match(modelPrompt, /perspectiva de unicidad/);
  assert.match(payload.answer, /^Desde la perspectiva de la unicidad/);
  assert.match(payload.disclaimer, /no juzga la fe/);
});

test("an absolute dismissal is replaced rather than shown as Bibi's verdict", () => {
  const context = resolveDoctrinalContext("Soy de la unicidad. ¿Qué piensas de la Trinidad?", []);
  const response = enforceDoctrinalResponse({
    answer: "La Trinidad es falsa y sus creyentes no son cristianos.",
    references: [], suggestedBlocks: [], confidence: "high", disclaimer: null
  }, context);
  assert.doesNotMatch(response.answer, /Trinidad es falsa/);
  assert.match(response.answer, /perspectiva de la unicidad/);
  const harmfulBlock = enforceDoctrinalResponse({
    answer: "Consideremos el pasaje.", references: [],
    suggestedBlocks: ["La Trinidad es falsa"], confidence: "high", disclaimer: null
  }, context);
  assert.deepEqual(harmfulBlock.suggestedBlocks, []);
});

import assert from "node:assert/strict";
import test from "node:test";

import worker, {
  extractCompletionContent,
  invokeModelProvider,
  normalizeProvider,
  resolveModelConfig,
  resolveModelConfigs
} from "../src/index.js";

const cloudflareModel = "@cf/qwen/qwen3-30b-a3b-fp8";

test("Cloudflare Workers AI is the default provider", () => {
  const ai = { run() {} };
  const config = resolveModelConfig({ AI: ai });

  assert.equal(config.provider, "cloudflare-workers-ai");
  assert.equal(config.model, cloudflareModel);
  assert.equal(config.isConfigured, true);
});

test("provider aliases normalize to Workers AI", () => {
  assert.equal(normalizeProvider("cloudflare"), "cloudflare-workers-ai");
  assert.equal(normalizeProvider("workers-ai"), "cloudflare-workers-ai");
  assert.equal(normalizeProvider("cloudflare-workers-ai"), "cloudflare-workers-ai");
});

test("Alibaba is added only as an available fallback", () => {
  const configs = resolveModelConfigs({
    AI: { run() {} },
    OPENAI_COMPATIBLE_API_KEY: "test-key",
    BIBI_FALLBACK_MODEL: "qwen3-8b"
  });

  assert.deepEqual(
    configs.map((config) => config.provider),
    ["cloudflare-workers-ai", "openai-compatible"]
  );
  assert.equal(configs[1].model, "qwen3-8b");
});

test("Workers AI receives the expected chat options", async () => {
  let captured;
  const expected = {
    choices: [{ message: { content: "{\"answer\":\"respuesta\"}" } }]
  };
  const env = {
    AI: {
      async run(model, options) {
        captured = { model, options };
        return expected;
      }
    }
  };

  const completion = await invokeModelProvider({
    env,
    modelConfig: {
      provider: "cloudflare-workers-ai",
      model: cloudflareModel
    },
    messages: [{ role: "user", content: "Pregunta" }],
    mode: "reader"
  });

  assert.equal(completion, expected);
  assert.equal(captured.model, cloudflareModel);
  assert.equal(captured.options.max_tokens, 260);
  assert.equal(captured.options.stream, false);
  assert.match(captured.options.messages[0].content, /\/no_think$/);
});

test("completion content supports Cloudflare response shapes", () => {
  assert.equal(
    extractCompletionContent({
      choices: [{ message: { content: "  respuesta chat  " } }]
    }),
    "respuesta chat"
  );
  assert.equal(
    extractCompletionContent({ response: { answer: "respuesta estructurada" } }),
    "{\"answer\":\"respuesta estructurada\"}"
  );
});

test("the Worker returns a parsed Workers AI response", async () => {
  const request = buildRequest("Analiza el contexto literario de Filipenses 2:5-11.");
  const env = {
    AI: {
      async run() {
        return {
          choices: [{
            message: {
              content: JSON.stringify({
                answer: "El pasaje presenta la humildad de Cristo.",
                references: [{ ref: "Filipenses 2:5-11", reason: "Pasaje consultado" }],
                suggestedBlocks: [],
                confidence: "high",
                disclaimer: null,
                intentDetected: "explain"
              })
            }
          }]
        };
      }
    }
  };

  const response = await worker.fetch(request, env);
  const payload = await response.json();

  assert.equal(response.status, 200);
  assert.equal(payload.answer, "El pasaje presenta la humildad de Cristo.");
  assert.equal(payload.confidence, "high");
});

test("provider failures degrade to a local response instead of HTTP 502", async () => {
  const request = buildRequest("Analiza el contexto literario de Filipenses 2:5-11.");
  const env = {
    AI: {
      async run() {
        throw new Error("Workers AI unavailable");
      }
    }
  };

  const response = await worker.fetch(request, env);
  const payload = await response.json();

  assert.equal(response.status, 200);
  assert.ok(payload.answer);
  assert.ok(["low", "medium", "high"].includes(payload.confidence));
});

function buildRequest(question) {
  return new Request("https://biblion.test/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      mode: "reader",
      intent: "explain",
      question,
      bible: {
        version: "RV1960",
        availableVersions: ["RV1960"],
        passages: []
      },
      study: {
        title: "",
        tags: [],
        selectedText: "",
        currentOutline: [],
        notes: []
      },
      chatHistory: [],
      lastQueries: []
    })
  });
}

import test from "node:test";
import assert from "node:assert/strict";
import { isBibleDomain } from "../src/index.js";

function request(question, extras = {}) {
  return {
    question,
    mode: "reader",
    title: "Génesis 1",
    selectedText: "",
    tags: [],
    currentOutline: [],
    notes: [],
    providedPassages: ["Génesis 1:1 En el principio creó Dios los cielos y la tierra."],
    dictionaryEntries: [],
    chatHistory: [],
    ...extras
  };
}

test("an unrelated question is not admitted by the open Bible passage", () => {
  assert.equal(isBibleDomain(request("¿Cómo preparo pasta?")), false);
});

test("short signals must be complete words", () => {
  assert.equal(isBibleDomain(request("¿Cómo preparo café?", {
    title: "", providedPassages: []
  })), false);
});

test("a Bible question and a reference to the current passage remain in domain", () => {
  assert.equal(isBibleDomain(request("¿Quién fue Moisés?")), true);
  assert.equal(isBibleDomain(request("¿Qué significa este versículo?")), true);
  assert.equal(isBibleDomain(request("¿Cómo aplico lo que leo a mi vida?")), true);
});

test("short biblical follow-up may use recent conversation context", () => {
  assert.equal(isBibleDomain(request("y dónde vivió", {
    chatHistory: [{ question: "¿Quién fue Abraham?", response: "Abraham" }]
  })), true);
});

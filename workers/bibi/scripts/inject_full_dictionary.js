#!/usr/bin/env node
/**
 * Build script: Inyecta el diccionario COMPLETO de Easton's en el Worker de Bibi.
 *
 * Lee workers/bibi/data/eastons_dictionary_full.json (~4,000 entries) y genera
 * workers/bibi/src/index.built.js con el diccionario completo integrado.
 *
 * Usage:
 *   node workers/bibi/scripts/inject_full_dictionary.js
 *
 * This should be run before deploying the Worker to Cloudflare.
 */

const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(__dirname, "../../..");
const DICT_FILE = path.join(ROOT, "workers/bibi/data/eastons_dictionary_full.json");
const WORKER_SRC = path.join(ROOT, "workers/bibi/src/index.js");
const WORKER_OUTPUT = path.join(ROOT, "workers/bibi/src/index.built.js");

/**
 * Lee el diccionario JSON completo.
 */
function loadDictionary() {
  if (!fs.existsSync(DICT_FILE)) {
    console.error(`ERROR: No se encontro ${DICT_FILE}`);
    console.error("Ejecuta primero: python tools/export_dictionary_for_worker.py");
    process.exit(1);
  }

  const data = JSON.parse(fs.readFileSync(DICT_FILE, "utf-8"));
  console.log(`Diccionario completo cargado: ${data.length} entradas`);
  return data;
}

/**
 * Formatea el diccionario como codigo JavaScript valido.
 * Optimiza para Cloudflare Workers (1 MB limit).
 */
function formatDictionaryAsJS(entries, maxDefLength = 350) {
  const lines = ["const EASTONS_DICTIONARY = ["];

  entries.forEach((entry, index) => {
    const term = escapeJS(entry.term);
    const normalizedTerm = escapeJS(entry.normalizedTerm || entry.term.toLowerCase());
    const definition = truncate(entry.definition, maxDefLength);
    const escapedDef = escapeJS(definition);
    const references = JSON.stringify((entry.references || []).slice(0, 3));
    const category = escapeJS(entry.category || "other");
    const searchTerms = JSON.stringify((entry.searchTerms || []).slice(0, 6));

    lines.push("  {");
    lines.push(`    t: "${term}",`);
    lines.push(`    n: "${normalizedTerm}",`);
    lines.push(`    d: "${escapedDef}",`);
    lines.push(`    r: ${references},`);
    lines.push(`    c: "${category}",`);
    lines.push(`    s: ${searchTerms}`);
    lines.push(`  }${index < entries.length - 1 ? "," : ""}`);
  });

  lines.push("];");
  return lines.join("\n");
}

/**
 * Escapa caracteres especiales para strings JavaScript.
 */
function escapeJS(str) {
  return String(str || "")
    .replace(/\\/g, "\\\\")
    .replace(/"/g, '\\"')
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "\\r")
    .replace(/\t/g, "\\t");
}

/**
 * Trunca un string a maxLength sin cortar palabras.
 */
function truncate(str, maxLength) {
  if (str.length <= maxLength) return str;
  const truncated = str.slice(0, maxLength);
  const lastSpace = truncated.lastIndexOf(" ");
  return lastSpace > maxLength * 0.7 ? truncated.slice(0, lastSpace) + "..." : truncated + "...";
}

/**
 * Inyecta el diccionario en el codigo fuente del Worker.
 */
function injectDictionary(sourceCode, dictionaryJS) {
  const startMarker = "// Easton's Bible Dictionary entries";
  const endMarker = "// Legacy dictionary entries";

  const startIndex = sourceCode.indexOf(startMarker);
  const endIndex = sourceCode.indexOf(endMarker);

  if (startIndex === -1 || endIndex === -1) {
    console.error("ERROR: No se encontraron los marcadores en el codigo fuente del Worker.");
    process.exit(1);
  }

  const before = sourceCode.slice(0, startIndex);
  const after = sourceCode.slice(endIndex);

  return before + dictionaryJS + "\n\n" + after;
}

/**
 * Main
 */
function main() {
  console.log("=".repeat(60));
  console.log("Biblion: Inyectando diccionario COMPLETO en Worker de Bibi");
  console.log("=".repeat(60));

  console.log("\n[1/3] Cargando diccionario completo de Easton's...");
  const entries = loadDictionary();

  console.log("\n[2/3] Formateando diccionario como JavaScript...");
  const dictionaryJS = formatDictionaryAsJS(entries);
  const dictSizeKB = Buffer.byteLength(dictionaryJS, "utf-8") / 1024;
  console.log(`  Tamano del diccionario: ${dictSizeKB.toFixed(1)} KB`);

  if (dictSizeKB > 800) {
    console.warn("\nADVERTENCIA: El diccionario excede 800 KB!");
    console.warn("Cloudflare Workers tiene un limite de 1 MB.");
    console.warn("Considera reducir la longitud de definiciones.");
  }

  console.log("\n[3/3] Inyectando en el codigo fuente del Worker...");
  const sourceCode = fs.readFileSync(WORKER_SRC, "utf-8");
  const builtCode = injectDictionary(sourceCode, dictionaryJS);

  fs.writeFileSync(WORKER_OUTPUT, builtCode, "utf-8");

  const builtSizeKB = Buffer.byteLength(builtCode, "utf-8") / 1024;
  console.log(`  Archivo generado: ${WORKER_OUTPUT}`);
  console.log(`  Tamano total del Worker: ${builtSizeKB.toFixed(1)} KB`);

  console.log("\n" + "=".repeat(60));
  console.log("Completado exitosamente!");
  console.log(`\nDiccionario expandido de 20 a ${entries.length} entradas.`);
  console.log("\nSiguientes pasos:");
  console.log("  1. Revisar workers/bibi/src/index.built.js");
  console.log("  2. Reemplazar index.js con index.built.js si todo esta correcto");
  console.log("  3. Deployar: npx wrangler deploy");
}

main();

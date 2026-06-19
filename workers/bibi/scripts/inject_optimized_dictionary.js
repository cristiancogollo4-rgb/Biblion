#!/usr/bin/env node
/**
 * Build script: Inyecta diccionario optimizado de Easton's en el Worker de Bibi.
 *
 * Estrategia de optimizacion para Cloudflare Workers (limite 1 MB):
 * 1. Definiciones truncadas a 250 caracteres
 * 2. Referencias limitadas a 2
 * 3. SearchTerms limitados a 4
 * 4. Nombres de propiedades cortos (t, n, d, r, c, s)
 *
 * Usage:
 *   node workers/bibi/scripts/inject_optimized_dictionary.js
 */

const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(__dirname, "../../..");
const DICT_FILE = path.join(ROOT, "workers/bibi/data/eastons_dictionary_full.json");
const WORKER_SRC = path.join(ROOT, "workers/bibi/src/index.js");
const WORKER_OUTPUT = path.join(ROOT, "workers/bibi/src/index.built.js");

function loadDictionary() {
  if (!fs.existsSync(DICT_FILE)) {
    console.error(`ERROR: No se encontro ${DICT_FILE}`);
    process.exit(1);
  }
  const data = JSON.parse(fs.readFileSync(DICT_FILE, "utf-8"));
  console.log(`Diccionario cargado: ${data.length} entradas`);
  return data;
}

/**
 * Formatea optimizado para Workers.
 */
function formatDictionaryAsJS(entries, maxDefLength = 250) {
  const lines = ["const EASTONS_DICTIONARY = ["];

  entries.forEach((entry, index) => {
    const term = escapeJS(entry.term);
    const normalizedTerm = escapeJS((entry.normalizedTerm || entry.term.toLowerCase()).substring(0, 50));
    const definition = truncate(entry.definition, maxDefLength);
    const escapedDef = escapeJS(definition);
    const references = JSON.stringify((entry.references || []).slice(0, 2));
    const category = escapeJS((entry.category || "other").substring(0, 10));
    const searchTerms = JSON.stringify((entry.searchTerms || []).slice(0, 4));

    lines.push(`{t:"${term}",n:"${normalizedTerm}",d:"${escapedDef}",r:${references},c:"${category}",s:${searchTerms}}${index < entries.length - 1 ? "," : ""}`);
  });

  lines.push("];");
  return lines.join("");
}

function escapeJS(str) {
  return String(str || "")
    .replace(/\\/g, "\\\\")
    .replace(/"/g, '\\"')
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "\\r")
    .replace(/\t/g, "\\t");
}

function truncate(str, maxLength) {
  if (str.length <= maxLength) return str;
  const truncated = str.slice(0, maxLength);
  const lastSpace = truncated.lastIndexOf(" ");
  return lastSpace > maxLength * 0.7 ? truncated.slice(0, lastSpace) + "..." : truncated + "...";
}

function injectDictionary(sourceCode, dictionaryJS) {
  const startMarker = "// Easton's Bible Dictionary entries";
  const endMarker = "// Legacy dictionary entries";

  const startIndex = sourceCode.indexOf(startMarker);
  const endIndex = sourceCode.indexOf(endMarker);

  if (startIndex === -1 || endIndex === -1) {
    console.error("ERROR: Marcadores no encontrados en index.js");
    process.exit(1);
  }

  return sourceCode.slice(0, startIndex) + dictionaryJS + "\n\n" + sourceCode.slice(endIndex);
}

function main() {
  console.log("=".repeat(60));
  console.log("Biblion: Inyectando diccionario OPTIMIZADO en Worker");
  console.log("=".repeat(60));

  console.log("\n[1/3] Cargando diccionario...");
  const entries = loadDictionary();

  console.log("\n[2/3] Optimizando para Cloudflare Workers...");
  const dictionaryJS = formatDictionaryAsJS(entries, 250);
  const dictSizeKB = Buffer.byteLength(dictionaryJS, "utf-8") / 1024;
  console.log(`  Tamano optimizado: ${dictSizeKB.toFixed(1)} KB`);

  if (dictSizeKB > 950) {
    console.warn("\nADVERTENCIA: Todavia excede 950 KB. Reduciendo definiciones...");
    const smallerJS = formatDictionaryAsJS(entries, 180);
    const smallerSizeKB = Buffer.byteLength(smallerJS, "utf-8") / 1024;
    console.log(`  Nuevo tamano: ${smallerSizeKB.toFixed(1)} KB`);
    if (smallerSizeKB <= 950) {
      console.log("  Usando definiciones de 180 caracteres.");
      return writeOutput(entries, smallerJS);
    }
  }

  writeOutput(entries, dictionaryJS);
}

function writeOutput(entries, dictionaryJS) {
  console.log("\n[3/3] Inyectando en el Worker...");
  const sourceCode = fs.readFileSync(WORKER_SRC, "utf-8");
  const builtCode = injectDictionary(sourceCode, dictionaryJS);

  fs.writeFileSync(WORKER_OUTPUT, builtCode, "utf-8");

  const builtSizeKB = Buffer.byteLength(builtCode, "utf-8") / 1024;
  console.log(`  Archivo: ${WORKER_OUTPUT}`);
  console.log(`  Tamano total: ${builtSizeKB.toFixed(1)} KB`);

  console.log("\n" + "=".repeat(60));
  console.log("Completado!");
  console.log(`Diccionario: ${entries.length} entradas`);
  console.log("Deployar: npx wrangler deploy");
}

main();

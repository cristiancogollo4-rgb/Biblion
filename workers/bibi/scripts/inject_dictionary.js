#!/usr/bin/env node
/**
 * Build script: Inyecta el diccionario de Easton's en el Worker de Bibi.
 *
 * Lee workers/bibi/data/eastons_dictionary.json y genera un archivo
 * workers/bibi/src/index.js con el diccionario expandido integrado.
 *
 * Usage:
 *   node workers/bibi/scripts/inject_dictionary.js
 *
 * This should be run before deploying the Worker to Cloudflare.
 */

const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(__dirname, "../../..");
const DICT_FILE = path.join(ROOT, "workers/bibi/data/eastons_dictionary.json");
const WORKER_SRC = path.join(ROOT, "workers/bibi/src/index.js");
const WORKER_OUTPUT = path.join(ROOT, "workers/bibi/src/index.built.js");

/**
 * Lee el diccionario JSON y lo formatea como array JavaScript.
 */
function loadDictionary() {
  if (!fs.existsSync(DICT_FILE)) {
    console.error(`ERROR: No se encontro ${DICT_FILE}`);
    console.error("Ejecuta primero: python tools/export_dictionary_for_worker.py");
    process.exit(1);
  }

  const data = JSON.parse(fs.readFileSync(DICT_FILE, "utf-8"));
  console.log(`Diccionario cargado: ${data.length} entradas`);
  return data;
}

/**
 * Formatea el diccionario como codigo JavaScript valido.
 * Limita la longitud de las definiciones para mantener el tamano del Worker.
 */
function formatDictionaryAsJS(entries, maxDefLength = 400) {
  const lines = ["const EASTONS_DICTIONARY = ["];

  entries.forEach((entry, index) => {
    const term = escapeJS(entry.term);
    const normalizedTerm = escapeJS(entry.normalizedTerm || entry.term.toLowerCase());
    const definition = truncate(entry.definition, maxDefLength);
    const escapedDef = escapeJS(definition);
    const references = JSON.stringify(entry.references || []).slice(0, 200);
    const category = escapeJS(entry.category || "other");
    const searchTerms = JSON.stringify(entry.searchTerms || []).slice(0, 300);

    lines.push("  {");
    lines.push(`    term: "${term}",`);
    lines.push(`    normalizedTerm: "${normalizedTerm}",`);
    lines.push(`    definition: "${escapedDef}",`);
    lines.push(`    references: ${references},`);
    lines.push(`    category: "${category}",`);
    lines.push(`    searchTerms: ${searchTerms}`);
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
 * Reemplaza la seccion de EASTONS_DICTIONARY en el archivo original.
 */
function injectDictionary(sourceCode, dictionaryJS) {
  // Buscar el marcador de inicio del diccionario
  const startMarker = "// Easton's Bible Dictionary entries";
  const endMarker = "// Legacy dictionary entries";

  const startIndex = sourceCode.indexOf(startMarker);
  const endIndex = sourceCode.indexOf(endMarker);

  if (startIndex === -1 || endIndex === -1) {
    console.error("ERROR: No se encontraron los marcadores en el codigo fuente del Worker.");
    console.error("Asegurate de que index.js tenga los comentarios:");
    console.error('  "// Easton\'s Bible Dictionary entries"');
    console.error('  "// Legacy dictionary entries"');
    process.exit(1);
  }

  // Reemplazar la seccion del diccionario
  const before = sourceCode.slice(0, startIndex);
  const after = sourceCode.slice(endIndex);

  return before + dictionaryJS + "\n\n" + after;
}

/**
 * Main: Lee el diccionario, lo formatea y lo inyecta en el Worker.
 */
function main() {
  console.log("=" .repeat(60));
  console.log("Biblion: Inyectando diccionario en Worker de Bibi");
  console.log("=" .repeat(60));

  // Paso 1: Cargar diccionario
  console.log("\n[1/3] Cargando diccionario de Easton's...");
  const entries = loadDictionary();

  // Paso 2: Formatear como JavaScript
  console.log("\n[2/3] Formateando diccionario como JavaScript...");
  const dictionaryJS = formatDictionaryAsJS(entries);
  const dictSizeKB = Buffer.byteLength(dictionaryJS, "utf-8") / 1024;
  console.log(`  Tamano del diccionario: ${dictSizeKB.toFixed(1)} KB`);

  // Paso 3: Inyectar en el Worker
  console.log("\n[3/3] Inyectando en el codigo fuente del Worker...");
  const sourceCode = fs.readFileSync(WORKER_SRC, "utf-8");
  const builtCode = injectDictionary(sourceCode, dictionaryJS);

  fs.writeFileSync(WORKER_OUTPUT, builtCode, "utf-8");

  const builtSizeKB = Buffer.byteLength(builtCode, "utf-8") / 1024;
  console.log(`  Archivo generado: ${WORKER_OUTPUT}`);
  console.log(`  Tamano total del Worker: ${builtSizeKB.toFixed(1)} KB`);

  // Verificar que el Cloudflare Worker size limit (1 MB para workers)
  if (builtSizeKB > 1024) {
    console.warn("\nADVERTENCIA: El Worker excede 1 MB!");
    console.warn("Considera reducir el numero de entradas o la longitud de definiciones.");
  }

  console.log("\n" + "=".repeat(60));
  console.log("Completado exitosamente!");
  console.log("\nSiguientes pasos:");
  console.log("  1. Revisar workers/bibi/src/index.built.js");
  console.log("  2. Reemplazar index.js con index.built.js si todo esta correcto");
  console.log("  3. Deployar el Worker a Cloudflare: npx wrangler deploy");
}

main();

#!/usr/bin/env python3
"""
Builds Biblion's read-only Bible Dictionary SQLite asset from Easton's Bible Dictionary JSON.

Source: neuu-org/bible-dictionary-dataset (CC BY 4.0)
- 3,962 Easton's Bible Dictionary entries (1897)
- Public domain original, dataset CC BY 4.0
- Structured with term, definitions, and scripture references

Output:
    app/src/main/assets/databases/dictionary.db

Usage:
    python tools/build_dictionary_sqlite.py
"""

from __future__ import annotations

import json
import re
import sqlite3
import unicodedata
from pathlib import Path
from urllib.request import urlopen, Request
from urllib.error import URLError

ROOT = Path(__file__).resolve().parents[1]
ASSETS_DIR = ROOT / "app" / "src" / "main" / "assets"
OUTPUT_DB = ASSETS_DIR / "databases" / "dictionary.db"

# Primary source: neuu-org/bible-dictionary-dataset
# Easton's entries are split into alphabetical JSON files (a.json through z.json)
EASTON_BASE_URL = "https://raw.githubusercontent.com/neuu-org/bible-dictionary-dataset/main/data/02_sources/easton"

# Fallback: local JSON directory if download fails
LOCAL_JSON_DIR = ROOT / "tools" / "eastons_source"


def normalize_term(term: str) -> str:
    """
    Normalizes a dictionary term for consistent search and sorting.
    - Lowercase
    - Remove accents (NFD decomposition)
    - Collapse multiple spaces
    """
    lowered = term.lower().strip()
    normalized = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_accents)


def clean_definition(text: str) -> str:
    """
    Cleans HTML tags and extra whitespace from definition text.
    Easton's entries often contain HTML like <i>, <b>, <a>, etc.
    """
    # Remove HTML tags but preserve the text content
    cleaned = re.sub(r"<[^>]+>", "", text)
    # Decode common HTML entities
    cleaned = cleaned.replace("&amp;", "&")
    cleaned = cleaned.replace("&quot;", '"')
    cleaned = cleaned.replace("&#39;", "'")
    cleaned = cleaned.replace("&lt;", "<")
    cleaned = cleaned.replace("&gt;", ">")
    cleaned = cleaned.replace("&nbsp;", " ")
    # Collapse multiple spaces/newlines
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return cleaned


def extract_references_from_structured(scripture_refs: list[dict]) -> list[str]:
    """
    Extracts Bible references from structured scripture_refs array.
    Format: [{"reference": "Exodus 4:14", "original": "Ex 4:14"}]
    """
    references = []
    for ref in scripture_refs:
        reference = ref.get("reference", "").strip()
        if reference and reference not in references:
            references.append(reference)
    return references


def categorize_entry(term: str, definition: str) -> str:
    """
    Assigns a category to a dictionary entry based on the term and content.
    Categories: person, place, concept, object, practice, event, other
    """
    term_lower = term.lower()
    def_lower = definition.lower()

    # Person indicators
    person_signals = ["hijo de", "padre de", "nacio", "murio", "profeta", "rey de",
                      "apostol", "discipulo", "juez", "sacerdote", "esposa de",
                      "hermano de", "gobernador", "high priest", "son of",
                      "daughter of", "king of", "prophet", "apostle", "disciple"]
    if any(sig in def_lower for sig in person_signals):
        return "person"

    # Place indicators
    place_signals = ["ciudad", "lugar", "region", "monte", "rio", "mar de",
                     "valle", "tierra de", "provincia", "aldea", "cerro",
                     "camino de", "puerta de", "isla", "city", "place",
                     "region", "mountain", "river", "valley", "land of"]
    if any(sig in def_lower for sig in place_signals):
        return "place"

    # Practice/ritual indicators
    practice_signals = ["ceremonia", "rito", "fiesta", "sacrificio", "ofrenda",
                        "ley de", "mandamiento", "costumbre", "practica",
                        "ordenanza", "pascua", "sabbat", "circuncision",
                        "bautismo", "ayuno", "oracion", "ceremony", "rite",
                        "feast", "sacrifice", "offering", "law", "commandment"]
    if any(sig in def_lower for sig in practice_signals):
        return "practice"

    # Event indicators
    event_signals = ["acontecimiento", "suceso", "exodo", "cautiverio",
                     "destruccion", "diluvio", "nacimiento", "muerte de",
                     "resurreccion", "crucifixion", "pentecostes", "event",
                     "exodus", "captivity", "destruction", "flood", "birth",
                     "death", "resurrection", "crucifixion", "pentecost"]
    if any(sig in def_lower for sig in event_signals):
        return "event"

    # Object indicators
    object_signals = ["objeto", "instrumento", "vaso", "utensilio", "edificio",
                      "templo", "tabernaculo", "arca", "vestid", "armadura",
                      "espada", "cetro", "corona", "moneda", "object",
                      "instrument", "vessel", "utensil", "building", "temple",
                      "tabernacle", "ark", "garment", "armor", "sword", "crown"]
    if any(sig in def_lower for sig in object_signals):
        return "object"

    # Concept indicators (theological/abstract terms)
    concept_signals = ["significa", "representa", "simboliza", "se refiere",
                       "doctrina", "teologia", "virtud", "principio",
                       "cualidad", "estado", "condicion", "relacion",
                       "means", "represents", "symbolizes", "refers to",
                       "doctrine", "theology", "virtue", "principle"]
    if any(sig in def_lower for sig in concept_signals):
        return "concept"

    return "other"


def fetch_easton_alphabetical() -> list[dict]:
    """
    Downloads Easton's entries from alphabetical JSON files (a.json through z.json).
    Returns a list of parsed entry dicts.
    """
    all_entries = []
    letters = [chr(ord("a") + i) for i in range(26)]

    for letter in letters:
        url = f"{EASTON_BASE_URL}/{letter}.json"
        try:
            print(f"  Descargando {letter}.json...")
            req = Request(url, headers={"User-Agent": "Biblion-Builder/1.0"})
            with urlopen(req, timeout=15) as response:
                data = json.loads(response.read().decode("utf-8"))
                for term_key, entry_data in data.items():
                    if not entry_data:
                        continue

                    # Extract term name (use the key or the name field)
                    term = entry_data.get("name", term_key).strip()
                    if not term:
                        continue

                    # Extract definitions - combine all definition texts
                    definitions = entry_data.get("definitions", [])
                    if not definitions:
                        continue

                    # Combine all definition texts (Easton may have multiple)
                    def_texts = []
                    for def_item in definitions:
                        text = def_item.get("text", "").strip()
                        if text:
                            def_texts.append(text)

                    if not def_texts:
                        continue

                    full_definition = " ".join(def_texts)

                    # Extract scripture references
                    scripture_refs = entry_data.get("scripture_refs", [])
                    references = extract_references_from_structured(scripture_refs)

                    # Categorize
                    category = categorize_entry(term, full_definition)

                    all_entries.append({
                        "term": term,
                        "definition": full_definition,
                        "references": json.dumps(references, ensure_ascii=False),
                        "category": category,
                        "sources": entry_data.get("sources", ["EAS"]),
                    })
                print(f"    {len(data)} entries en {letter}")

        except (URLError, Exception) as e:
            print(f"    Error en {letter}: {e}")
            continue

    return all_entries


def load_local_easton() -> list[dict]:
    """
    Loads Easton's JSON from local fallback directory.
    Expects a.json through z.json in tools/eastons_source/
    """
    if not LOCAL_JSON_DIR.exists():
        return []

    all_entries = []
    for json_file in sorted(LOCAL_JSON_DIR.glob("*.json")):
        try:
            data = json.loads(json_file.read_text(encoding="utf-8"))
            for term_key, entry_data in data.items():
                if not entry_data:
                    continue

                term = entry_data.get("name", term_key).strip()
                if not term:
                    continue

                definitions = entry_data.get("definitions", [])
                if not definitions:
                    continue

                def_texts = []
                for def_item in definitions:
                    text = def_item.get("text", "").strip()
                    if text:
                        def_texts.append(text)

                if not def_texts:
                    continue

                full_definition = " ".join(def_texts)
                scripture_refs = entry_data.get("scripture_refs", [])
                references = extract_references_from_structured(scripture_refs)
                category = categorize_entry(term, full_definition)

                all_entries.append({
                    "term": term,
                    "definition": full_definition,
                    "references": json.dumps(references, ensure_ascii=False),
                    "category": category,
                    "sources": entry_data.get("sources", ["EAS"]),
                })
        except Exception as e:
            print(f"  Error leyendo {json_file}: {e}")

    return all_entries


def create_schema(connection: sqlite3.Connection) -> None:
    """
    Creates the dictionary database schema compatible with Room.
    Room validates the schema strictly, so column defaults and nullability
    must match the DictionaryEntryEntity exactly.
    """
    connection.executescript(
        """
        PRAGMA user_version = 1;

        DROP TABLE IF EXISTS dictionary_entries;

        -- Main table with all dictionary entry data
        -- Column types and nullability must match DictionaryEntryEntity exactly
        CREATE TABLE dictionary_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            term TEXT NOT NULL,
            normalized_term TEXT NOT NULL,
            definition TEXT NOT NULL,
            references_json TEXT NOT NULL,
            category TEXT NOT NULL
        );

        -- Index for alphabetical browsing and exact lookups
        CREATE INDEX idx_dict_normalized ON dictionary_entries(normalized_term);
        CREATE INDEX idx_dict_category ON dictionary_entries(category);
        CREATE INDEX idx_dict_term ON dictionary_entries(term);
        """
    )


def insert_entries(connection: sqlite3.Connection, entries: list[dict]) -> int:
    """
    Inserts all dictionary entries into the database.
    Returns the number of entries inserted.
    """
    rows = []
    for entry in entries:
        term = entry["term"]
        normalized = normalize_term(term)
        definition = clean_definition(entry["definition"])

        if not definition or len(definition) < 20:
            continue

        rows.append((
            term,
            normalized,
            definition,
            entry["references"],
            entry["category"],
        ))

    connection.executemany(
        """
        INSERT INTO dictionary_entries (
            term, normalized_term, definition, references_json, category
        ) VALUES (?, ?, ?, ?, ?)
        """,
        rows,
    )

    return len(rows)


def main() -> None:
    """
    Main entry point: downloads/parses Easton's JSON and builds dictionary.db.
    """
    print("=" * 60)
    print("Biblion: Building Dictionary Database (Easton's)")
    print("=" * 60)

    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()
        print(f"  Eliminado archivo existente: {OUTPUT_DB}")

    # Step 1: Get Easton's JSON data
    print("\n[1/4] Obteniendo datos de Easton's Bible Dictionary...")
    print("  Fuente: neuu-org/bible-dictionary-dataset (CC BY 4.0)")
    entries = fetch_easton_alphabetical()

    if not entries:
        print("  Intentando archivos locales de respaldo...")
        entries = load_local_easton()

    if not entries:
        print("\nERROR: No se pudo obtener Easton's JSON de ninguna fuente.")
        print("Opciones:")
        print("  1. Verificar conexion a internet")
        print("  2. Colocar archivos a-z.json en tools/eastons_source/")
        print("  3. Descargar desde: https://github.com/neuu-org/bible-dictionary-dataset")
        return

    print(f"  Total entradas obtenidas: {len(entries)}")

    # Step 2: Create database and insert data
    print(f"\n[2/4] Creando base de datos: {OUTPUT_DB}")
    connection = sqlite3.connect(OUTPUT_DB)
    try:
        create_schema(connection)
        count = insert_entries(connection, entries)
        connection.commit()

        # Step 3: Optimize and report
        print(f"\n[3/4] Optimizando base de datos...")
        connection.execute("VACUUM")
        connection.commit()

    finally:
        connection.close()

    # Step 4: Report statistics
    print(f"\n[4/4] Estadisticas finales...")
    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.execute("SELECT COUNT(*) FROM dictionary_entries")
    total = cursor.fetchone()[0]

    size_kb = OUTPUT_DB.stat().st_size / 1024
    print(f"\n{'=' * 60}")
    print(f"Base de datos creada: {OUTPUT_DB}")
    print(f"Tamano: {size_kb:.1f} KB")
    print(f"Entradas totales: {total}")

    # Category breakdown
    cursor = conn.execute(
        "SELECT category, COUNT(*) FROM dictionary_entries GROUP BY category ORDER BY COUNT(*) DESC"
    )
    print(f"\nCategorias:")
    for category, count in cursor.fetchall():
        print(f"  {category}: {count}")

    # Sample entries
    print(f"\nMuestras de entradas:")
    cursor = conn.execute(
        "SELECT term, length(definition) as def_len FROM dictionary_entries ORDER BY RANDOM() LIMIT 5"
    )
    for term, def_len in cursor.fetchall():
        print(f"  - {term} ({def_len} caracteres)")

    conn.close()

    print(f"\n{'=' * 60}")
    print("Completado exitosamente!")


if __name__ == "__main__":
    main()

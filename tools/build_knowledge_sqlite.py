#!/usr/bin/env python3
"""
Unified builder: merges Easton's Bible Dictionary + Theographic Bible Metadata
into a single dictionary.db with all columns.

Easton's provides: definitions, references, categories
Theographic provides: GPS coords, birth/death years, gender, aliases, feature types

All text is in Spanish (Easton's already translated, Theographic names mapped
via KNOWN_NAMES table or cross-referenced with dictionary.db).

Output:
    app/src/main/assets/databases/dictionary.db
"""
from __future__ import annotations

import json
import re
import sqlite3
import unicodedata
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DB = ROOT / "app" / "src" / "main" / "assets" / "databases" / "dictionary.db"

# Load already-translated Spanish dictionary instead of fresh English download
SPANISH_DICT_FILE = ROOT / "tools" / "eastons_dictionary_es.json"
PEOPLE_URL = "https://raw.githubusercontent.com/robertrouse/theographic-bible-metadata/master/json/people.json"
PLACES_URL = "https://raw.githubusercontent.com/robertrouse/theographic-bible-metadata/master/json/places.json"

def load_spanish_easton():
    """Carga las entradas ya traducidas al espanol."""
    if not SPANISH_DICT_FILE.exists():
        print(f"  ERROR: No se encuentra {SPANISH_DICT_FILE}")
        print("  Ejecuta primero: python tools/translate_dictionary.py")
        return []
    
    data = json.loads(SPANISH_DICT_FILE.read_text(encoding="utf-8"))
    entries = []
    for entry in data:
        term = entry.get("term", "").strip()
        if not term:
            continue
        definition = entry.get("definition", "").strip()
        if not definition or len(definition) < 20:
            continue
        entries.append({
            "term": term,
            "normalized_term": normalize_term(term),
            "definition": definition,
            "references": json.dumps(entry.get("references", [])[:10], ensure_ascii=False),
            "category": entry.get("category", "other"),
        })
    return entries

KNOWN_NAMES = {
    "Abraham": "Abraham", "Adam": "Adan", "Aaron": "Aaron", "Isaac": "Isaac",
    "Jacob": "Jacob", "Joseph": "Jose", "Moses": "Moises", "David": "David",
    "Solomon": "Salomon", "Samuel": "Samuel", "Saul": "Saul", "Elijah": "Elias",
    "Elisha": "Eliseo", "Isaiah": "Isaias", "Jeremiah": "Jeremias",
    "Ezekiel": "Ezequiel", "Daniel": "Daniel", "Jonah": "Jonas",
    "Jesus": "Jesus", "Peter": "Pedro", "Paul": "Pablo", "John": "Juan",
    "James": "Santiago", "Matthew": "Mateo", "Mark": "Marcos",
    "Luke": "Lucas", "Mary": "Maria", "Stephen": "Esteban",
    "Philip": "Felipe", "Andrew": "Andres", "Thomas": "Tomas",
    "Simon": "Simon", "Judas": "Judas", "Jude": "Judas", "Ruth": "Rut",
    "Esther": "Ester", "Rebecca": "Rebeca", "Rachel": "Raquel",
    "Sarah": "Sara", "Miriam": "Miriam", "Deborah": "Debora",
    "Noah": "Noe", "Shem": "Sem", "Ham": "Cam", "Japheth": "Jafet",
    "Esau": "Esau", "Ishmael": "Ismael", "Benjamin": "Benjamin",
    "Joshua": "Josue", "Caleb": "Caleb", "Gideon": "Gedeon",
    "Samson": "Sanson", "Boaz": "Booz", "Naomi": "Noemi",
    "Eve": "Eva", "Cain": "Cain", "Abel": "Abel", "Seth": "Set",
    "Enoch": "Enoc", "Methuselah": "Matusalen", "Lamech": "Lamec",
    "Nimrod": "Nimrod", "Lot": "Lot",
    "Levi": "Levi", "Ruben": "Ruben",
    "Gideon": "Gedeon", "Samson": "Sanson",
    "Phinehas": "Finees", "Eleazar": "Eleazar",
    "Barnabas": "Bernabe", "Timothy": "Timoteo", "Titus": "Tito",
    "Silas": "Silas", "Apollos": "Apolos",
    "Lydia": "Lidia", "Dorcas": "Dorcas",
    "Cornelius": "Cornelio", "Felix": "Felix", "Festus": "Festo",
    "Agrippa": "Agripa", "Herod": "Herodes",
    "Pilate": "Pilato", "Annas": "Anas", "Caiaphas": "Caifas",
    "Cyrus": "Ciro", "Nebuchadnezzar": "Nabucodonosor",
    # Lugares
    "Jerusalem": "Jerusalen", "Bethlehem": "Belen",
    "Nazareth": "Nazaret", "Galilee": "Galilea", "Samaria": "Samaria",
    "Judea": "Judea", "Egypt": "Egipto", "Babylon": "Babilonia",
    "Assyria": "Asiria", "Persia": "Persia", "Rome": "Roma",
    "Corinth": "Corinto", "Ephesus": "Efeso", "Damascus": "Damasco",
    "Antioch": "Antioquia", "Caesarea": "Cesarea",
    "Bethany": "Betania", "Capernaum": "Cafarnaun",
    "Sinai": "Sinai", "Canaan": "Canaan", "Jordan": "Jordan",
    "Hebron": "Hebron", "Jericho": "Jerico",
    "Nineveh": "Ninive", "Tyre": "Tiro", "Sidon": "Sidon",
    "Sodom": "Sodoma", "Gomorrah": "Gomorra",
}

BOOK_NAME_MAP = {
    "Genesis": "Genesis", "Exodus": "Exodo", "Leviticus": "Levitico",
    "Numbers": "Numeros", "Deuteronomy": "Deuteronomio", "Joshua": "Josue",
    "Judges": "Jueces", "Ruth": "Rut",
    "1 Samuel": "1 Samuel", "2 Samuel": "2 Samuel",
    "1 Kings": "1 Reyes", "2 Kings": "2 Reyes",
    "1 Chronicles": "1 Cronicas", "2 Chronicles": "2 Cronicas",
    "Ezra": "Esdras", "Nehemiah": "Nehemias", "Esther": "Ester",
    "Job": "Job", "Psalms": "Salmos", "Proverbs": "Proverbios",
    "Ecclesiastes": "Eclesiastes", "Song of Solomon": "Cantares",
    "Isaiah": "Isaias", "Jeremiah": "Jeremias",
    "Lamentations": "Lamentaciones", "Ezekiel": "Ezequiel",
    "Daniel": "Daniel", "Hosea": "Oseas", "Joel": "Joel",
    "Amos": "Amos", "Obadiah": "Abdias", "Jonah": "Jonas",
    "Micah": "Miqueas", "Nahum": "Nahum", "Habakkuk": "Habacuc",
    "Zephaniah": "Sofonias", "Haggai": "Hageo",
    "Zechariah": "Zacarias", "Malachi": "Malaquias",
    "Matthew": "Mateo", "Mark": "Marcos", "Luke": "Lucas",
    "John": "Juan", "Acts": "Hechos", "Romans": "Romanos",
    "1 Corinthians": "1 Corintios", "2 Corinthians": "2 Corintios",
    "Galatians": "Galatas", "Ephesians": "Efesios",
    "Philippians": "Filipenses", "Colossians": "Colosenses",
    "1 Thessalonians": "1 Tesalonicenses", "2 Thessalonians": "2 Tesalonicenses",
    "1 Timothy": "1 Timoteo", "2 Timothy": "2 Timoteo",
    "Titus": "Tito", "Philemon": "Filemon", "Hebrews": "Hebreos",
    "James": "Santiago", "1 Peter": "1 Pedro", "2 Peter": "2 Pedro",
    "1 John": "1 Juan", "2 John": "2 Juan", "3 John": "3 Juan",
    "Jude": "Judas", "Revelation": "Apocalipsis",
}

def normalize_term(term):
    lowered = term.lower().strip()
    normalized = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_accents)

def to_spanish_name(name):
    return KNOWN_NAMES.get(name.strip(), name.strip())

def safe_text(value):
    if isinstance(value, list):
        return " ".join(str(v) for v in value if v)
    if isinstance(value, dict):
        return json.dumps(value, ensure_ascii=False)
    return str(value) if value else ""

def clean_definition(text):
    cleaned = re.sub(r"<[^>]+>", "", text)
    cleaned = cleaned.replace("&amp;", "&").replace("&quot;", '"')
    cleaned = cleaned.replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return cleaned

def extract_references(scripture_refs):
    return [ref.get("reference", "") for ref in scripture_refs if ref.get("reference")]

def download_json(url, label):
    print(f"  Descargando {label}...")
    req = Request(url, headers={"User-Agent": "Biblion-Builder/1.0"})
    with urlopen(req, timeout=120) as response:
        data = json.loads(response.read().decode("utf-8"))
    print(f"    {len(data)} entradas")
    return data

def download_easton_alphabetical():
    all_entries = []
    for letter in [chr(ord("a") + i) for i in range(26)]:
        url = f"{EASTON_BASE_URL}/{letter}.json"
        try:
            req = Request(url, headers={"User-Agent": "Biblion-Builder/1.0"})
            with urlopen(req, timeout=15) as response:
                data = json.loads(response.read().decode("utf-8"))
                for key, entry_data in data.items():
                    if not entry_data:
                        continue
                    term = entry_data.get("name", key).strip()
                    if not term:
                        continue
                    definitions = entry_data.get("definitions", [])
                    def_texts = [d.get("text", "").strip() for d in definitions if d.get("text")]
                    if not def_texts:
                        continue
                    scripture_refs = entry_data.get("scripture_refs", [])
                    references = extract_references(scripture_refs)
                    all_entries.append({
                        "term": term,
                        "normalized_term": normalize_term(term),
                        "definition": clean_definition(" ".join(def_texts)),
                        "references": json.dumps(references, ensure_ascii=False),
                        "category": categorize_entry(term, def_texts[0]),
                    })
                print(f"    {letter}: {len(data)} entradas")
        except Exception as e:
            print(f"    {letter}: error - {e}")
    return all_entries

def categorize_entry(term, definition):
    term_lower = term.lower()
    def_lower = (definition or "").lower()

    person_signals = ["son of", "father of", "king of", "prophet", "apostle",
                      "disciple", "born", "died", "married", "wife of"]
    place_signals = ["city", "region", "mountain", "river", "valley", "land of",
                     "province", "town", "village"]
    practice_signals = ["ceremony", "rite", "feast", "sacrifice", "offering",
                        "law", "commandment", "custom"]
    event_signals = ["exodus", "captivity", "destruction", "flood", "battle of"]
    object_signals = ["instrument", "vessel", "utensil", "building", "temple",
                      "ark", "garment", "weapon"]
    concept_signals = ["doctrine", "theology", "virtue", "principle", "means",
                       "represents", "symbolizes"]

    if any(s in def_lower for s in person_signals): return "person"
    if any(s in def_lower for s in place_signals): return "place"
    if any(s in def_lower for s in practice_signals): return "practice"
    if any(s in def_lower for s in event_signals): return "event"
    if any(s in def_lower for s in object_signals): return "object"
    if any(s in def_lower for s in concept_signals): return "concept"
    return "other"

def extract_theographic_people(data):
    entries = []
    for entry in data:
        fields = entry.get("fields", {})
        name = fields.get("name", "")
        if not name:
            continue
        spanish_name = to_spanish_name(name)
        entries.append({
            "term": spanish_name,
            "normalized_term": normalize_term(spanish_name),
            "definition": clean_definition(safe_text(
                fields.get("dictionaryText", fields.get("dictText", ""))
            )),
            "references": "[]",
            "category": "person",
            "display_title": to_spanish_name(safe_text(fields.get("displayTitle", name))),
            "gender": safe_text(fields.get("gender", "")),
            "birth_year": safe_text(fields.get("birthYear", "")),
            "death_year": safe_text(fields.get("deathYear", "")),
            "aliases": safe_text(fields.get("alsoCalled", "")),
            "latitude": None,
            "longitude": None,
            "feature_type": None,
        })
    return entries

def extract_theographic_places(data):
    entries = []
    for entry in data:
        fields = entry.get("fields", {})
        name = fields.get("kjvName", fields.get("esvName", fields.get("name", "")))
        if not name:
            continue
        spanish_name = to_spanish_name(name)
        entries.append({
            "term": spanish_name,
            "normalized_term": normalize_term(spanish_name),
            "definition": clean_definition(safe_text(
                fields.get("dictionaryText", fields.get("dictText", ""))
            )),
            "references": "[]",
            "category": "place",
            "display_title": None,
            "gender": None,
            "birth_year": None,
            "death_year": None,
            "aliases": safe_text(fields.get("aliases", "")),
            "latitude": safe_text(fields.get("latitude", "")),
            "longitude": safe_text(fields.get("longitude", "")),
            "feature_type": safe_text(fields.get("featureType", "")).capitalize(),
        })
    return entries

def create_schema(connection):
    connection.executescript("""
        PRAGMA user_version = 2;

        DROP TABLE IF EXISTS dictionary_entries;

        CREATE TABLE dictionary_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            term TEXT NOT NULL,
            normalized_term TEXT NOT NULL,
            definition TEXT NOT NULL DEFAULT '',
            references_json TEXT NOT NULL DEFAULT '[]',
            category TEXT NOT NULL DEFAULT 'other',
            display_title TEXT,
            gender TEXT,
            birth_year TEXT,
            death_year TEXT,
            latitude TEXT,
            longitude TEXT,
            aliases TEXT,
            feature_type TEXT
        );

        CREATE INDEX idx_dict_normalized ON dictionary_entries(normalized_term);
        CREATE INDEX idx_dict_category ON dictionary_entries(category);
        CREATE INDEX idx_dict_term ON dictionary_entries(term);
    """)

def insert_entry(connection, entry):
    """Insert a single entry, updating if term+category already exists."""
    # Check if entry already exists by normalized_term and category
    existing = connection.execute(
        "SELECT term, definition FROM dictionary_entries WHERE normalized_term = ? AND category = ? LIMIT 1",
        (entry["normalized_term"], entry["category"])
    ).fetchone()

    if existing:
        existing_term, existing_def = existing
        # If existing entry has a definition but this one doesn't, keep the existing
        # If this one has a definition but existing doesn't, update
        if entry["definition"] and not existing_def:
            connection.execute(
                """UPDATE dictionary_entries SET definition = ?, references_json = ?,
                   display_title = ?, gender = ?, birth_year = ?, death_year = ?,
                   latitude = ?, longitude = ?, aliases = ?, feature_type = ?
                WHERE normalized_term = ? AND category = ?""",
                (entry["definition"], entry["references"],
                 entry["display_title"], entry["gender"],
                 entry["birth_year"], entry["death_year"],
                 entry["latitude"], entry["longitude"],
                 entry["aliases"], entry["feature_type"],
                 entry["normalized_term"], entry["category"])
            )
        elif not entry["definition"] and existing_def:
            # Update metadata only
            connection.execute(
                """UPDATE dictionary_entries SET display_title = ?, gender = ?,
                   birth_year = ?, death_year = ?, latitude = ?, longitude = ?,
                   aliases = ?, feature_type = ?
                WHERE normalized_term = ? AND category = ?""",
                (entry["display_title"], entry["gender"],
                 entry["birth_year"], entry["death_year"],
                 entry["latitude"], entry["longitude"],
                 entry["aliases"], entry["feature_type"],
                 entry["normalized_term"], entry["category"])
            )
        # Both have definitions - keep whichever is more complete (longer)
        elif entry["definition"] and len(entry["definition"]) > len(existing_def):
            connection.execute(
                """UPDATE dictionary_entries SET definition = ?, references_json = ?,
                   display_title = ?, gender = ?, birth_year = ?, death_year = ?,
                   latitude = ?, longitude = ?, aliases = ?, feature_type = ?
                WHERE normalized_term = ? AND category = ?""",
                (entry["definition"], entry["references"],
                 entry["display_title"], entry["gender"],
                 entry["birth_year"], entry["death_year"],
                 entry["latitude"], entry["longitude"],
                 entry["aliases"], entry["feature_type"],
                 entry["normalized_term"], entry["category"])
            )
        return "updated"
    else:
        connection.execute(
            """INSERT INTO dictionary_entries
            (term, normalized_term, definition, references_json, category,
             display_title, gender, birth_year, death_year,
             latitude, longitude, aliases, feature_type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (entry["term"], entry["normalized_term"], entry.get("definition", ""),
             entry.get("references", "[]"), entry.get("category", "other"),
             entry.get("display_title"), entry.get("gender"),
             entry.get("birth_year"), entry.get("death_year"),
             entry.get("latitude"), entry.get("longitude"),
             entry.get("aliases"), entry.get("feature_type"))
        )
        return "inserted"

def main():
    print("=" * 60)
    print("Biblion: Building Unified Knowledge Database")
    print("=" * 60)

    # Step 1: Download all sources
    print("\n[1/5] Descargando fuentes...")

    print("\n  Diccionario Easton's en espanol:")
    easton_entries = load_spanish_easton()
    print(f"  Total Easton's espanol: {len(easton_entries)}")

    print("\n  Theographic Bible Metadata:")
    people_data = download_json(PEOPLE_URL, "people.json")
    places_data = download_json(PLACES_URL, "places.json")

    # Step 2: Extract Theographic data
    print(f"\n[2/5] Extrayendo datos Theographic...")
    theo_people = extract_theographic_people(people_data)
    theo_places = extract_theographic_places(places_data)
    print(f"  Personas: {len(theo_people)}")
    print(f"  Lugares: {len(theo_places)}")

    # Step 3: Merge and cross-reference
    print(f"\n[3/5] Cruzando datos (merging)...")
    all_entries = {}
    easton_by_normalized = {}

    # Index Easton's entries by normalized_term for cross-reference
    for e in easton_entries:
        norm = e["normalized_term"]
        if norm not in easton_by_normalized:
            easton_by_normalized[norm] = []
        easton_by_normalized[norm].append(e)

    # Also index by the English name from Theographic to handle mismatches
    # (e.g., Theographic "Moses" -> normalize_term("Moises") -> matches Easton's "Moises")

    # Add Theographic people, enriching with Easton's definitions where available
    merged_count = 0
    for entry in theo_people:
        norm = entry["normalized_term"]
        # Check if Easton's has this term
        if norm in easton_by_normalized:
            easton = easton_by_normalized[norm][0]
            # Keep Easton's definition, enrich with Theographic metadata
            entry["definition"] = easton["definition"]
            entry["references"] = easton["references"]
            merged_count += 1
        elif entry["definition"] == "":
            # No definition available - mark as known person without description
            pass
        all_entries[norm + "_person"] = entry

    # Add Theographic places
    for entry in theo_places:
        norm = entry["normalized_term"]
        if norm in easton_by_normalized:
            easton = easton_by_normalized[norm][0]
            entry["definition"] = easton["definition"]
            entry["references"] = easton["references"]
            merged_count += 1
        all_entries[norm + "_place"] = entry

    # Add remaining Easton's entries not covered by Theographic
    for e in easton_entries:
        key = e["normalized_term"] + "_" + ("person" if e["category"] == "person" else e["category"])
        if key not in all_entries:
            # Add as-is
            all_entries[key] = {
                "term": e["term"],
                "normalized_term": e["normalized_term"],
                "definition": e["definition"],
                "references": e["references"],
                "category": e["category"],
                "display_title": None,
                "gender": None,
                "birth_year": None,
                "death_year": None,
                "latitude": None,
                "longitude": None,
                "aliases": None,
                "feature_type": None,
            }

    print(f"  Entradas combinadas con definiciones enrichidas: {merged_count}")
    print(f"  Total entradas unicas tras merge: {len(all_entries)}")

    # Step 4: Create database
    print(f"\n[4/5] Creando base de datos: {OUTPUT_DB}")
    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()

    connection = sqlite3.connect(OUTPUT_DB)
    stats = {"inserted": 0, "updated": 0, "skipped": 0}
    try:
        create_schema(connection)
        for key, entry in all_entries.items():
            result = insert_entry(connection, entry)
            if result == "inserted":
                stats["inserted"] += 1
            elif result == "updated":
                stats["updated"] += 1
            else:
                stats["skipped"] += 1
        connection.commit()
        connection.execute("VACUUM")
    finally:
        connection.close()

    # Step 5: Report statistics
    size_kb = OUTPUT_DB.stat().st_size / 1024
    print(f"\n[5/5] Estadisticas:")
    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.execute("SELECT COUNT(*) FROM dictionary_entries")
    total = cursor.fetchone()[0]
    cursor = conn.execute("SELECT category, COUNT(*) FROM dictionary_entries GROUP BY category ORDER BY COUNT(*) DESC")
    print(f"\n  Categorias:")
    for cat, cnt in cursor.fetchall():
        print(f"    {cat}: {cnt}")

    cursor = conn.execute("SELECT COUNT(*) FROM dictionary_entries WHERE display_title IS NOT NULL")
    has_title = cursor.fetchone()[0]
    cursor = conn.execute("SELECT COUNT(*) FROM dictionary_entries WHERE latitude IS NOT NULL")
    has_gps = cursor.fetchone()[0]
    cursor = conn.execute("SELECT COUNT(*) FROM dictionary_entries WHERE birth_year IS NOT NULL")
    has_birth = cursor.fetchone()[0]
    print(f"\n  Entradas con metadata:")
    print(f"    Titulo display: {has_title}")
    print(f"    Coordenadas GPS: {has_gps}")
    print(f"    Ano nacimiento: {has_birth}")

    print(f"\n  Muestras:")
    cursor = conn.execute(
        "SELECT term, category, definition IS NOT NULL AND definition != '' as has_def FROM dictionary_entries ORDER BY RANDOM() LIMIT 8"
    )
    for term, cat, has_def in cursor.fetchall():
        marker = "📖" if has_def else "📌"
        print(f"    {marker} {term} ({cat})")

    conn.close()

    print(f"\n{'=' * 60}")
    print(f"DB creada: {OUTPUT_DB}")
    print(f"Tamano: {size_kb:.1f} KB")
    print(f"Entradas totales: {total}")
    print(f"Insertadas: {stats['inserted']}, Actualizadas: {stats['updated']}")

if __name__ == "__main__":
    main()

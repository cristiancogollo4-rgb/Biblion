#!/usr/bin/env python3
"""
Builds bible_metadata.db from Theographic Bible Metadata.
Source: robertrouse/theographic-bible-metadata (CC BY-SA 4.0)

Downloads people.json and places.json, extracts key fields,
translates names to Spanish using Google Translate, and creates
a Room-compatible SQLite database.

Output:
    app/src/main/assets/databases/bible_metadata.db
"""
from __future__ import annotations

import json
import re
import sqlite3
import unicodedata
from pathlib import Path
from urllib.request import Request, urlopen
from deep_translator import GoogleTranslator

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DB = ROOT / "app" / "src" / "main" / "assets" / "databases" / "bible_metadata.db"

PEOPLE_URL = "https://raw.githubusercontent.com/robertrouse/theographic-bible-metadata/master/json/people.json"
PLACES_URL = "https://raw.githubusercontent.com/robertrouse/theographic-bible-metadata/master/json/places.json"

# Nombres biblicos conocidos que no se traducen (ya estan en espanol biblico)
KNOWN_NAMES = {
    "Abraham": "Abraham", "Adam": "Adan", "Aaron": "Aaron", "Isaac": "Isaac",
    "Jacob": "Jacob", "Joseph": "Jose", "Moses": "Moises", "David": "David",
    "Solomon": "Salomon", "Samuel": "Samuel", "Saul": "Saul", "Elijah": "Elias",
    "Elisha": "Eliseo", "Isaiah": "Isaias", "Jeremiah": "Jeremias",
    "Ezekiel": "Ezequiel", "Daniel": "Daniel", "Jonah": "Jonas",
    "Jesus": "Jesus", "Peter": "Pedro", "Paul": "Pablo", "John": "Juan",
    "James": "Santiago", "Matthew": "Mateo", "Mark": "Marcos",
    "Luke": "Lucas", "Mary": "Maria", "Joseph": "Jose", "Stephen": "Esteban",
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
    "Nimrod": "Nimrod", "Abraham": "Abraham", "Lot": "Lot",
    "Isaac": "Isaac", "Jacob": "Jacob", "Judah": "Juda",
    "Levi": "Levi", "Reuben": "Ruben", "Simeon": "Simeon",
    "Zebulun": "Zabulon", "Issachar": "Isacar", "Dan": "Dan",
    "Gad": "Gad", "Asher": "Aser", "Naphtali": "Neftali",
    "Manasseh": "Manases", "Ephraim": "Efrain",
    "Phinehas": "Finees", "Eleazar": "Eleazar", "Ithamar": "Itamar",
    "Nadab": "Nadab", "Abihu": "Abiu",
    "Barnabas": "Bernabe", "Timothy": "Timoteo", "Titus": "Tito",
    "Silas": "Silas", "Apollos": "Apolos", "Priscilla": "Priscila",
    "Aquila": "Aquila", "Lydia": "Lidia", "Dorcas": "Dorcas",
    "Cornelius": "Cornelio", "Felix": "Felix", "Festus": "Festo",
    "Agrippa": "Agripa", "Herod": "Herodes", "Pilate": "Pilato",
    "Annas": "Anas", "Caiaphas": "Caifas",
    # Lugares
    "Jerusalem": "Jerusalen", "Bethlehem": "Belen", "Nazareth": "Nazaret",
    "Galilee": "Galilea", "Samaria": "Samaria", "Judea": "Judea",
    "Egypt": "Egipto", "Babylon": "Babilonia", "Assyria": "Asiria",
    "Persia": "Persia", "Rome": "Roma", "Athens": "Atenas",
    "Corinth": "Corinto", "Ephesus": "Efeso", "Philippi": "Filipos",
    "Thessalonica": "Tesalonica", "Damascus": "Damasco",
    "Antioch": "Antioquia", "Caesarea": "Cesarea",
    "Bethany": "Betania", "Bethpage": "Betfage", "Cana": "Cana",
    "Capernaum": "Cafarnaun", "Chorazin": "Corazin", "Bethsaida": "Betsaida",
    "Gethsemane": "Getsemani", "Golgotha": "Golgota", "Sinai": "Sinaí",
    "Canaan": "Canaan", "Jordan": "Jordan", "Lebanon": "Libano",
    "Bethel": "Bet-el", "Shechem": "Siquem", "Hebron": "Hebron",
    "Beersheba": "Beerseba", "Jericho": "Jerico", "Ai": "Hai",
    "Ur": "Ur", "Haran": "Haran", "Nineveh": "Ninive",
    "Tarshish": "Tarsis", "Tyre": "Tiro", "Sidon": "Sidon",
    "Moab": "Moab", "Edom": "Edom", "Ammon": "Ammon",
    "Philistia": "Filistea", "Gaza": "Gaza", "Ashkelon": "Ascalon",
    "Ashdod": "Asdod", "Ekron": "Ecron", "Gath": "Gat",
    "Joppa": "Jope", "Lystra": "Listra", "Derbe": "Derbe",
    "Troas": "Troas", "Miletus": "Mileto", "Patmos": "Patmos",
    "Sardis": "Sardis", "Smyrna": "Esmirna", "Pergamum": "Pergamo",
    "Thyatira": "Tiatira", "Philadelphia": "Filadelfia", "Laodicea": "Laodicea",
    "Sodom": "Sodoma", "Gomorrah": "Gomorra",
    "Red Sea": "Mar Rojo", "Dead Sea": "Mar Muerto",
    "Sea of Galilee": "Mar de Galilea",
    "Mount of Olives": "Monte de los Olivos",
    "Mount Sinai": "Monte Sinaí", "Mount Zion": "Monte Sion",
    "Mount Carmel": "Monte Carmelo", "Mount Hermon": "Monte Hermon",
}

def normalize_term(term):
    lowered = term.lower().strip()
    normalized = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", "", without_accents)

def translate_name(name, translator):
    if name in KNOWN_NAMES:
        return KNOWN_NAMES[name]
    if len(name) < 3:
        return name
    # No traducir nombres propios desconocidos - mantener original
    # (la mayoria de nombres biblicos ya estan en KNOWN_NAMES)
    return name

def translate_names_batch(names, translator):
    """Traduce solo los nombres que no estan en KNOWN_NAMES, en lote."""
    result = {}
    to_translate = []
    for name in names:
        if name in KNOWN_NAMES:
            result[name] = KNOWN_NAMES[name]
        elif len(name) >= 3 and name not in result:
            to_translate.append(name)
        else:
            result[name] = name

    # Traducir en lote usando separador
    if to_translate:
        try:
            combined = " | ".join(to_translate[:50])  # max 50 por lote
            translated = translator.translate(combined)
            parts = translated.split(" | ")
            for i, name in enumerate(to_translate[:50]):
                if i < len(parts):
                    result[name] = parts[i].strip()
                else:
                    result[name] = name
        except Exception:
            for name in to_translate:
                result[name] = name

    # Los que sobraron del lote se mantienen originales
    for name in to_translate[50:]:
        result[name] = name

    return result

def download_json(url, label):
    print(f"  Descargando {label}...")
    req = Request(url, headers={"User-Agent": "Biblion-Builder/1.0"})
    with urlopen(req, timeout=120) as response:
        data = json.loads(response.read().decode("utf-8"))
    print(f"    {len(data)} entradas")
    return data

def safe_text(value):
    """Convierte cualquier valor a string seguro para SQLite."""
    if isinstance(value, list):
        return " ".join(str(v) for v in value if v)
    if isinstance(value, dict):
        return json.dumps(value, ensure_ascii=False)
    return str(value) if value else ""

def extract_people(data, translator):
    """Extrae personas del JSON de Theographic."""
    people = []
    for entry in data:
        fields = entry.get("fields", {})
        name = fields.get("name", "")
        if not name:
            continue

        spanish_name = translate_name(name, translator)
        person = {
            "id": entry.get("id", ""),
            "name": spanish_name,
            "original_name": name,
            "normalized_name": normalize_term(spanish_name),
            "display_title": translate_name(fields.get("displayTitle", name), translator),
            "gender": safe_text(fields.get("gender", "")),
            "birth_year": safe_text(fields.get("birthYear", "")),
            "death_year": safe_text(fields.get("deathYear", "")),
            "also_called": safe_text(fields.get("alsoCalled", "")),
            "description": safe_text(fields.get("dictionaryText", fields.get("dictText", "")))[:5000],
            "verse_refs": json.dumps(extract_verse_refs(fields.get("verses", []))),
        }
        people.append(person)
    return people

def extract_places(data, translator):
    """Extrae lugares del JSON de Theographic."""
    places = []
    for entry in data:
        fields = entry.get("fields", {})
        name = fields.get("kjvName", fields.get("esvName", fields.get("name", "")))
        if not name:
            continue

        spanish_name = translate_name(name, translator)
        place = {
            "id": entry.get("id", ""),
            "name": spanish_name,
            "original_name": name,
            "normalized_name": normalize_term(spanish_name),
            "aliases": safe_text(fields.get("aliases", "")),
            "latitude": safe_text(fields.get("latitude", "")),
            "longitude": safe_text(fields.get("longitude", "")),
            "feature_type": safe_text(fields.get("featureType", "")),
            "description": safe_text(fields.get("dictionaryText", fields.get("dictText", "")))[:5000],
            "verse_refs": json.dumps(extract_verse_refs(fields.get("verses", []))),
        }
        places.append(place)
    return places

def extract_verse_refs(verse_ids):
    """Convierte IDs de versiculos a lista vacia por ahora (los IDs son internos)."""
    return []

def create_schema(connection):
    connection.executescript("""
        PRAGMA user_version = 1;

        CREATE TABLE biblical_people (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            original_name TEXT NOT NULL DEFAULT '',
            normalized_name TEXT NOT NULL,
            display_title TEXT NOT NULL DEFAULT '',
            gender TEXT NOT NULL DEFAULT '',
            birth_year TEXT NOT NULL DEFAULT '',
            death_year TEXT NOT NULL DEFAULT '',
            also_called TEXT NOT NULL DEFAULT '',
            description TEXT NOT NULL DEFAULT '',
            verse_refs TEXT NOT NULL DEFAULT '[]'
        );

        CREATE INDEX idx_people_name ON biblical_people(normalized_name);
        CREATE INDEX idx_people_display ON biblical_people(name);

        CREATE TABLE biblical_places (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            original_name TEXT NOT NULL DEFAULT '',
            normalized_name TEXT NOT NULL,
            aliases TEXT NOT NULL DEFAULT '',
            latitude TEXT NOT NULL DEFAULT '',
            longitude TEXT NOT NULL DEFAULT '',
            feature_type TEXT NOT NULL DEFAULT '',
            description TEXT NOT NULL DEFAULT '',
            verse_refs TEXT NOT NULL DEFAULT '[]'
        );

        CREATE INDEX idx_places_name ON biblical_places(normalized_name);
        CREATE INDEX idx_places_display ON biblical_places(name);
    """)

def insert_people(connection, people):
    for p in people:
        connection.execute(
            """INSERT OR REPLACE INTO biblical_people
            (id, name, original_name, normalized_name, display_title, gender,
             birth_year, death_year, also_called, description, verse_refs)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (p["id"], p["name"], p["original_name"], p["normalized_name"],
             p["display_title"], p["gender"], p["birth_year"], p["death_year"],
             p["also_called"], p["description"][:5000], p["verse_refs"])
        )

def insert_places(connection, places):
    for p in places:
        connection.execute(
            """INSERT OR REPLACE INTO biblical_places
            (id, name, original_name, normalized_name, aliases, latitude, longitude,
             feature_type, description, verse_refs)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (p["id"], p["name"], p["original_name"], p["normalized_name"],
             p["aliases"], p["latitude"], p["longitude"],
             p["feature_type"], p["description"][:5000], p["verse_refs"])
        )

def main():
    print("=" * 60)
    print("Biblion: Building Bible Metadata Database (Theographic)")
    print("=" * 60)

    translator = GoogleTranslator(source="en", target="es")

    # Download
    print("\n[1/5] Descargando datos...")
    people_data = download_json(PEOPLE_URL, "people.json")
    places_data = download_json(PLACES_URL, "places.json")

    # Extract + translate
    print("\n[2/5] Extrayendo y traduciendo personas...")
    people = extract_people(people_data, translator)
    print(f"  Personas: {len(people)}")

    print("\n[3/5] Extrayendo y traduciendo lugares...")
    places = extract_places(places_data, translator)
    print(f"  Lugares: {len(places)}")

    # Create DB
    print(f"\n[4/5] Creando base de datos: {OUTPUT_DB}")
    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()

    connection = sqlite3.connect(OUTPUT_DB)
    try:
        create_schema(connection)
        insert_people(connection, people)
        insert_places(connection, places)
        connection.commit()
        connection.execute("VACUUM")
    finally:
        connection.close()

    # Stats
    size_kb = OUTPUT_DB.stat().st_size / 1024
    print(f"\n[5/5] Estadisticas:")
    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.execute("SELECT COUNT(*) FROM biblical_people")
    ppl_count = cursor.fetchone()[0]
    cursor = conn.execute("SELECT COUNT(*) FROM biblical_places")
    place_count = cursor.fetchone()[0]

    # Samples
    print(f"\nMuestras de personas:")
    cursor = conn.execute("SELECT name, display_title, gender FROM biblical_people LIMIT 5")
    for name, title, gender in cursor.fetchall():
        print(f"  {name} ({title}) - {gender}")

    print(f"\nMuestras de lugares:")
    cursor = conn.execute("SELECT name, feature_type, latitude FROM biblical_places LIMIT 5")
    for name, ftype, lat in cursor.fetchall():
        print(f"  {name} ({ftype}) - lat:{lat}")

    conn.close()

    print(f"\n{'=' * 60}")
    print(f"DB creada: {OUTPUT_DB}")
    print(f"Tamano: {size_kb:.1f} KB")
    print(f"Personas: {ppl_count}")
    print(f"Lugares: {place_count}")

if __name__ == "__main__":
    main()

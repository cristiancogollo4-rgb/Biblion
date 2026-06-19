#!/usr/bin/env python3
"""
Builds strongs.db from Strong's Hebrew and Greek dictionaries.
Sources:
  - Hebrew: openscriptures/strongs (hebrew/strongs-hebrew-dictionary.js, CC-BY-SA)
  - Greek: morphgnt/strongs-dictionary-xml (strongsgreek.xml, CC0/Public Domain)

Parses both sources, unifies into a single SQLite database with
consistent schema for Hebrew (H0001-H8674) and Greek (G0001-G5624).

Output:
    app/src/main/assets/databases/strongs.db
"""
from __future__ import annotations

import json
import re
import sqlite3
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DB = ROOT / "app" / "src" / "main" / "assets" / "databases" / "strongs.db"

HEBREW_URL = "https://raw.githubusercontent.com/openscriptures/strongs/master/hebrew/strongs-hebrew-dictionary.js"
GREEK_URL = "https://raw.githubusercontent.com/morphgnt/strongs-dictionary-xml/master/strongsgreek.xml"

def download(url, label):
    print(f"  Descargando {label}...")
    req = Request(url, headers={"User-Agent": "Biblion-Builder/1.0"})
    with urlopen(req, timeout=120) as resp:
        content = resp.read().decode("utf-8")
    print(f"    {len(content)} bytes")
    return content

def parse_hebrew_js(content):
    """Parsea el JSON embebido en el JS de Hebrew Strong's."""
    entries = []
    # El archivo usa: var strongsHebrewDictionary = { "H1": {...}, ... };
    # Buscar el inicio del objeto
    idx = content.find("={")
    if idx < 0:
        idx = content.find("= {")
    if idx < 0:
        print("  ERROR: No se encontro el objeto JSON en el JS")
        # Intentar parsear como JSON directo
        try:
            data = json.loads(content)
        except:
            return []
    else:
        # Extraer desde { hasta el final ;
        start = content.find("{", idx)
        end = content.rfind("}")
        if start < 0 or end < 0:
            return []
        json_str = content[start:end+1]
        try:
            data = json.loads(json_str)
        except json.JSONDecodeError as e:
            print(f"  ERROR parseando JSON: {e}")
            return []

    for key, val in data.items():
        number = key.lstrip("H")
        try:
            num = int(number)
        except ValueError:
            continue
        entries.append({
            "strongs_number": f"H{num:04d}",
            "number": num,
            "language": "hebrew",
            "lemma": val.get("lemma", ""),
            "transliteration": val.get("xlit", ""),
            "pronunciation": val.get("pron", ""),
            "derivation": val.get("derivation", ""),
            "definition": val.get("strongs_def", ""),
            "kjv_renderings": val.get("kjv_def", ""),
        })
    return entries

def parse_greek_xml(content):
    """Parsea el XML de Greek Strong's (morphgnt version)."""
    import xml.etree.ElementTree as ET
    entries = []

    # El XML es grande, parsear de forma simple
    # Cada <entry strongs="NNNNN"> contiene los datos
    entry_pattern = re.compile(r'<entry strongs="(\d+)">(.*?)</entry>', re.DOTALL)

    for match in entry_pattern.finditer(content):
        strongs_attr = match.group(1)
        body = match.group(2)

        number = int(strongs_attr)

        # Extraer elementos
        lemma = ""
        m = re.search(r'<greek[^>]*unicode="([^"]*)"', body)
        if m:
            lemma = m.group(1)

        translit = ""
        m = re.search(r'<greek[^>]*translit="([^"]*)"', body)
        if m:
            translit = m.group(1)

        pronunciation = ""
        m = re.search(r'<pronunciation[^>]*strongs="([^"]*)"', body)
        if m:
            pronunciation = m.group(1)

        derivation = ""
        m = re.search(r'<strongs_derivation>(.*?)</strongs_derivation>', body, re.DOTALL)
        if m:
            derivation = strip_tags(m.group(1)).strip()

        definition = ""
        m = re.search(r'<strongs_def>(.*?)</strongs_def>', body, re.DOTALL)
        if m:
            definition = strip_tags(m.group(1)).strip()

        kjv_def = ""
        m = re.search(r'<kjv_def>(.*?)</kjv_def>', body, re.DOTALL)
        if m:
            kjv_def = strip_tags(m.group(1)).strip()

        entries.append({
            "strongs_number": f"G{number:04d}",
            "number": number,
            "language": "greek",
            "lemma": lemma,
            "transliteration": translit,
            "pronunciation": pronunciation,
            "derivation": derivation,
            "definition": definition,
            "kjv_renderings": kjv_def,
        })

    return entries

def strip_tags(text):
    """Remueve tags XML de un texto."""
    return re.sub(r'<[^>]+>', '', text)

def create_schema(connection):
    connection.executescript("""
        PRAGMA user_version = 1;

        CREATE TABLE strongs_entries (
            strongs_number TEXT PRIMARY KEY,
            number INTEGER NOT NULL,
            language TEXT NOT NULL,
            lemma TEXT NOT NULL,
            transliteration TEXT DEFAULT '',
            pronunciation TEXT DEFAULT '',
            derivation TEXT DEFAULT '',
            definition TEXT DEFAULT '',
            kjv_renderings TEXT DEFAULT ''
        );

        CREATE INDEX idx_strongs_lang ON strongs_entries(language);
        CREATE INDEX idx_strongs_num ON strongs_entries(number);
    """)

def insert_entries(connection, entries):
    for e in entries:
        connection.execute(
            """INSERT OR REPLACE INTO strongs_entries
            (strongs_number, number, language, lemma, transliteration,
             pronunciation, derivation, definition, kjv_renderings)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (e["strongs_number"], e["number"], e["language"],
             e["lemma"], e["transliteration"], e["pronunciation"],
             e["derivation"][:2000], e["definition"][:2000],
             e["kjv_renderings"][:1000])
        )
    return len(entries)

def main():
    print("=" * 60)
    print("Biblion: Building Strong's Database (Hebrew + Greek)")
    print("=" * 60)

    # Download
    print("\n[1/4] Descargando fuentes...")
    hebrew_js = download(HEBREW_URL, "Hebrew Strong's (JS)")
    greek_xml = download(GREEK_URL, "Greek Strong's (XML)")

    # Parse
    print("\n[2/4] Parseando...")
    hebrew_entries = parse_hebrew_js(hebrew_js)
    print(f"  Hebreo: {len(hebrew_entries)} entradas")

    greek_entries = parse_greek_xml(greek_xml)
    print(f"  Griego: {len(greek_entries)} entradas")

    all_entries = hebrew_entries + greek_entries
    print(f"  Total: {len(all_entries)} entradas")

    # Create DB
    print(f"\n[3/4] Creando base de datos: {OUTPUT_DB}")
    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()

    connection = sqlite3.connect(OUTPUT_DB)
    try:
        create_schema(connection)
        count = insert_entries(connection, all_entries)
        connection.commit()
        connection.execute("VACUUM")
    finally:
        connection.close()

    # Stats
    size_kb = OUTPUT_DB.stat().st_size / 1024
    print(f"\n[4/4] Estadisticas:")
    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.execute("SELECT language, COUNT(*) FROM strongs_entries GROUP BY language")
    for lang, cnt in cursor.fetchall():
        print(f"  {lang}: {cnt} entradas")

    # Samples
    print(f"\nMuestras:")
    cursor = conn.execute("SELECT strongs_number, transliteration, pronunciation, substr(definition,1,60) FROM strongs_entries LIMIT 5")
    for num, translit, pron, defn in cursor.fetchall():
        safe_translit = (translit or "").encode("ascii", "replace").decode("ascii")
        safe_pron = (pron or "").encode("ascii", "replace").decode("ascii")
        safe_def = (defn or "").encode("ascii", "replace").decode("ascii")
        print(f"  {num}: ({safe_translit}) [{safe_pron}] - {safe_def}...")

    conn.close()

    print(f"\n{'=' * 60}")
    print(f"DB creada: {OUTPUT_DB}")
    print(f"Tamano: {size_kb:.1f} KB")
    print(f"Entradas: {count}")

if __name__ == "__main__":
    main()

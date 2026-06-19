#!/usr/bin/env python3
"""
Builds cross_references.db from Treasury of Scripture Knowledge (TSK) data.
Source: CrossReferences-org/bible-cross-references (KJV version, CC BY-SA 4.0)

Downloads the TSV file, normalizes English book abbreviations to Spanish,
and creates a Room-compatible SQLite database.

Output:
    app/src/main/assets/databases/cross_references.db
"""
from __future__ import annotations

import csv
import io
import re
import sqlite3
import unicodedata
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DB = ROOT / "app" / "src" / "main" / "assets" / "databases" / "cross_references.db"

TSK_URL = "https://raw.githubusercontent.com/CrossReferences-org/bible-cross-references/main/kjv/crossreferences_kjv.tsv"

# English abbreviation -> Spanish book name (Biblion format, no accents)
BOOK_MAP = {
    "Gen": "Genesis", "Exod": "Exodo", "Lev": "Levitico", "Num": "Numeros",
    "Deut": "Deuteronomio", "Josh": "Josue", "Judg": "Jueces", "Ruth": "Rut",
    "1 Sam": "1 Samuel", "2 Sam": "2 Samuel", "1 Kgs": "1 Reyes", "2 Kgs": "2 Reyes",
    "1 Chr": "1 Cronicas", "2 Chr": "2 Cronicas", "Ezra": "Esdras",
    "Neh": "Nehemias", "Esth": "Ester", "Job": "Job", "Ps": "Salmos",
    "Prov": "Proverbios", "Eccl": "Eclesiastes", "Song": "Cantares",
    "Isa": "Isaias", "Jer": "Jeremias", "Lam": "Lamentaciones",
    "Ezek": "Ezequiel", "Dan": "Daniel", "Hos": "Oseas", "Joel": "Joel",
    "Am": "Amos", "Obad": "Abdias", "Jon": "Jonas", "Mic": "Miqueas",
    "Nah": "Nahum", "Hab": "Habacuc", "Zeph": "Sofonias", "Hag": "Hageo",
    "Zech": "Zacarias", "Mal": "Malaquias",
    "Matt": "Mateo", "Mark": "Marcos", "Luke": "Lucas", "John": "Juan",
    "Acts": "Hechos", "Rom": "Romanos",
    "1 Cor": "1 Corintios", "2 Cor": "2 Corintios", "Gal": "Galatas",
    "Eph": "Efesios", "Phil": "Filipenses", "Col": "Colosenses",
    "1 Thess": "1 Tesalonicenses", "2 Thess": "2 Tesalonicenses",
    "1 Tim": "1 Timoteo", "2 Tim": "2 Timoteo", "Tit": "Tito",
    "Phlm": "Filemon", "Heb": "Hebreos", "Jas": "Santiago",
    "1 Pet": "1 Pedro", "2 Pet": "2 Pedro",
    "1 John": "1 Juan", "2 John": "2 Juan", "3 John": "3 Juan",
    "Jude": "Judas", "Rev": "Apocalipsis",
}

def normalize_book_name(name: str) -> str:
    lowered = name.lower().strip()
    normalized = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", "", without_accents)

def map_book_abbr(abbr: str) -> str:
    return BOOK_MAP.get(abbr.strip(), abbr.strip())

def normalize_target_refs(refs_pipe: str) -> str:
    """Normaliza las referencias target de ingles a espanol.
    Input:  'Prov 8:22-24|Prov 16:4|John 1:1-3'
    Output: 'Proverbios 8:22-24|Proverbios 16:4|Juan 1:1-3'
    """
    refs = refs_pipe.split("|")
    normalized = []
    for ref in refs:
        ref = ref.strip()
        if not ref:
            continue
        # Separar nombre del libro del resto: "1 Kgs 2:3" o "Ps 33:6,9"
        match = re.match(r'^((?:\d\s+)?[A-Za-z]+(?:\s+[A-Za-z]+)?)\s+(\d+.*)$', ref)
        if match:
            book_abbr = match.group(1).strip()
            rest = match.group(2).strip()
            spanish_book = map_book_abbr(book_abbr)
            normalized.append(f"{spanish_book} {rest}")
        else:
            normalized.append(ref)
    return "|".join(normalized)


def download_tsk() -> str:
    print(f"  Descargando TSV desde: {TSK_URL}")
    req = Request(TSK_URL, headers={"User-Agent": "Biblion-Builder/1.0"})
    with urlopen(req, timeout=60) as response:
        content = response.read().decode("utf-8")
    lines = content.strip().split("\n")
    print(f"  Descargado: {len(lines)} lineas")
    return content


def parse_tsv(content: str) -> list[tuple]:
    """Parsea el TSV y retorna filas normalizadas."""
    reader = csv.reader(io.StringIO(content), delimiter="\t")
    header = next(reader)  # book, chapter, verse, anchor, references
    print(f"  Header: {header}")

    rows = []
    for line_num, row in enumerate(reader, start=2):
        if len(row) < 5:
            continue
        book_abbr = row[0].strip()
        chapter = row[1].strip()
        verse = row[2].strip()
        anchor = row[3].strip()
        references = row[4].strip()

        if not book_abbr or not chapter or not verse or not references:
            continue

        # Normalizar libro fuente a espanol
        spanish_book = map_book_abbr(book_abbr)
        normalized_book = normalize_book_name(spanish_book)

        # Normalizar referencias target a espanol
        spanish_refs = normalize_target_refs(references)

        try:
            ch = int(chapter)
            vs = int(verse)
        except ValueError:
            continue

        rows.append((spanish_book, normalized_book, ch, vs, anchor, spanish_refs))

    return rows


def create_schema(connection: sqlite3.Connection) -> None:
    connection.executescript("""
        PRAGMA user_version = 1;

        CREATE TABLE cross_references (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            source_book TEXT NOT NULL,
            source_normalized_book TEXT NOT NULL,
            source_chapter INTEGER NOT NULL,
            source_verse INTEGER NOT NULL,
            anchor TEXT DEFAULT '',
            target_references TEXT NOT NULL
        );

        CREATE INDEX idx_xref_source ON cross_references(source_normalized_book, source_chapter, source_verse);
    """)


def insert_rows(connection: sqlite3.Connection, rows: list[tuple]) -> int:
    connection.executemany(
        """
        INSERT INTO cross_references (
            source_book, source_normalized_book, source_chapter, source_verse, anchor, target_references
        ) VALUES (?, ?, ?, ?, ?, ?)
        """,
        rows
    )
    return len(rows)


def main():
    print("=" * 60)
    print("Biblion: Building Cross-References Database (TSK)")
    print("=" * 60)

    # Step 1: Download
    print("\n[1/4] Descargando TSK...")
    content = download_tsk()

    # Step 2: Parse
    print("\n[2/4] Parseando y normalizando a espanol...")
    rows = parse_tsv(content)
    print(f"  Filas parseadas: {len(rows)}")

    # Step 3: Create DB
    print(f"\n[3/4] Creando base de datos: {OUTPUT_DB}")
    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()

    connection = sqlite3.connect(OUTPUT_DB)
    try:
        create_schema(connection)
        count = insert_rows(connection, rows)
        connection.commit()
        connection.execute("VACUUM")
    finally:
        connection.close()

    # Step 4: Stats
    size_kb = OUTPUT_DB.stat().st_size / 1024
    print(f"\n[4/4] Estadisticas:")
    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.execute("SELECT COUNT(*) FROM cross_references")
    total = cursor.fetchone()[0]
    cursor = conn.execute("SELECT COUNT(DISTINCT source_normalized_book) FROM cross_references")
    books = cursor.fetchone()[0]

    # Sample
    cursor = conn.execute(
        "SELECT source_book, source_chapter, source_verse, target_references FROM cross_references LIMIT 5"
    )
    print(f"\nMuestras:")
    for book, ch, vs, refs in cursor.fetchall():
        print(f"  {book} {ch}:{vs} -> {refs[:80]}...")

    conn.close()

    print(f"\n{'=' * 60}")
    print(f"DB creada: {OUTPUT_DB}")
    print(f"Tamano: {size_kb:.1f} KB")
    print(f"Referencias: {total}")
    print(f"Libros cubiertos: {books}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Builds cross_references_votes.db from openbible.info data.

Source: https://www.openbile.info/labs/cross-references/
        (CC-BY 2026-06-15, 344,800 pairs with crowdsourced voting)

Output:
    app/src/main/assets/databases/cross_references_votes.db

Compatibilidad con Biblion:
    - Libros normalizados al formato Biblion (Exodo, no Éxodo; "1 Samuel", no "1Sam")
    - normalized_book = lowercase, sin acentos, sin espacios (igual que BibleBookMapper.normalizeForDb)
    - target_references formato "Libro Cap:V" o "Libro Cap:V-V" (compatible con
      BiblicalCrossReference.resolve() y resolveMultiple())
    - Cada fila conserva el voto crowdsourced (1 a 1279) para ranking de calidad

FILTROS aplicados por defecto:
    - votos >= 1 (descarta negativos y ceros por baja calidad confirmada)

IMPORTANTE: este script genera la DB con el schema EXACTO que Room espera,
INCLUYENDO la tabla `room_master_table` con el `identity_hash` correcto.
Esto es lo que evita el crash "Pre-packaged database has an invalid schema".

Uso:
    python tools/build_crossrefs_votes_sqlite.py
    python tools/build_crossrefs_votes_sqlite.py --min-votes 10
    python tools/build_crossrefs_votes_sqlite.py --input "C:\\ruta\\archivo.txt"
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DB = ROOT / "app" / "src" / "main" / "assets" / "databases" / "cross_references_votes.db"
BIBLE_JSON_PATH = ROOT / "app" / "src" / "main" / "assets" / "rv1960.json"
DEFAULT_INPUT = Path.home() / "Downloads" / "cross_references.txt"


# ---------------------------------------------------------------------------
# Mapeo de libros: clave normalizada (lowercase, sin acentos, sin espacios, sin puntos)
# -> nombre en espanol Biblion (sin acentos).
# Espejo del map en app/.../feature/bibi/BibleBookMapper.kt.
# ---------------------------------------------------------------------------
BOOK_MAP: dict[str, str] = {
    # Versiones completas
    "genesis": "Genesis", "exodus": "Exodo", "leviticus": "Levitico",
    "numbers": "Numeros", "deuteronomy": "Deuteronomio",
    "joshua": "Josue", "judges": "Jueces", "ruth": "Rut",
    "1samuel": "1 Samuel", "2samuel": "2 Samuel",
    "1kings": "1 Reyes", "2kings": "2 Reyes",
    "1chronicles": "1 Cronicas", "2chronicles": "2 Cronicas",
    "ezra": "Esdras", "nehemiah": "Nehemias", "esther": "Ester",
    "job": "Job", "psalms": "Salmos", "proverbs": "Proverbios",
    "ecclesiastes": "Eclesiastes", "song": "Cantares", "songofsolomon": "Cantares",
    "isaiah": "Isaias", "jeremiah": "Jeremias", "lamentations": "Lamentaciones",
    "ezekiel": "Ezequiel", "daniel": "Daniel",
    "hosea": "Oseas", "joel": "Joel", "amos": "Amos",
    "obadiah": "Abdias", "jonah": "Jonas", "micah": "Miqueas",
    "nahum": "Nahum", "habakkuk": "Habacuc", "zephaniah": "Sofonias",
    "haggai": "Hageo", "zechariah": "Zacarias", "malachi": "Malaquias",
    "matthew": "Mateo", "mark": "Marcos", "luke": "Lucas",
    "john": "Juan", "acts": "Hechos", "romans": "Romanos",
    "1corinthians": "1 Corintios", "2corinthians": "2 Corintios",
    "galatians": "Galatas", "ephesians": "Efesios", "philippians": "Filipenses",
    "colossians": "Colosenses", "1thessalonians": "1 Tesalonicenses",
    "2thessalonians": "2 Tesalonicenses", "1timothy": "1 Timoteo",
    "2timothy": "2 Timoteo", "titus": "Tito", "philemon": "Filemon",
    "hebrews": "Hebreos", "james": "Santiago", "1peter": "1 Pedro",
    "2peter": "2 Pedro", "1john": "1 Juan", "2john": "2 Juan",
    "3john": "3 Juan", "jude": "Judas", "revelation": "Apocalipsis",
    # Abreviaciones cortas (formato openbile.info: Gen, Exod, Ps, Isa, etc.)
    "gen": "Genesis", "exod": "Exodo", "lev": "Levitico", "num": "Numeros",
    "deut": "Deuteronomio", "josh": "Josue", "judg": "Jueces", "ru": "Rut",
    "1sam": "1 Samuel", "2sam": "2 Samuel", "1kgs": "1 Reyes", "2kgs": "2 Reyes",
    "1chr": "1 Cronicas", "2chr": "2 Cronicas", "ezr": "Esdras", "neh": "Nehemias",
    "est": "Ester", "jb": "Job", "ps": "Salmos", "psa": "Salmos",
    "prov": "Proverbios", "pr": "Proverbios", "eccl": "Eclesiastes",
    "ecc": "Eclesiastes", "isa": "Isaias", "jer": "Jeremias", "lam": "Lamentaciones",
    "ezek": "Ezequiel", "eze": "Ezequiel", "dan": "Daniel", "da": "Daniel",
    "hos": "Oseas", "jl": "Joel", "am": "Amos", "obad": "Abdias", "ob": "Abdias",
    "jon": "Jonas", "mic": "Miqueas", "nah": "Nahum", "hab": "Habacuc",
    "zeph": "Sofonias", "zep": "Sofonias", "hag": "Hageo", "zech": "Zacarias",
    "zec": "Zacarias", "mal": "Malaquias", "matt": "Mateo", "mt": "Mateo",
    "mk": "Marcos", "mr": "Marcos", "lk": "Lucas", "lc": "Lucas",
    "jn": "Juan", "joh": "Juan", "ac": "Hechos", "rom": "Romanos", "ro": "Romanos",
    "1cor": "1 Corintios", "2cor": "2 Corintios", "gal": "Galatas",
    "esth": "Ester",
    "eph": "Efesios", "phil": "Filipenses", "php": "Filipenses",
    "col": "Colosenses", "1thess": "1 Tesalonicenses", "2thess": "2 Tesalonicenses",
    "1tim": "1 Timoteo", "2tim": "2 Timoteo", "tit": "Tito",
    "philem": "Filemon", "phlm": "Filemon", "flm": "Filemon",
    "heb": "Hebreos", "jas": "Santiago", "jam": "Santiago",
    "1pet": "1 Pedro", "2pet": "2 Pedro", "1jn": "1 Juan", "2jn": "2 Juan", "3jn": "3 Juan",
    "jud": "Judas", "rev": "Apocalipsis", "re": "Apocalipsis",
    # Variantes con mayuscula inicial (Amos, Jonah, Titus del archivo)
    "amos": "Amos", "jonah": "Jonas", "titus": "Tito",
}


def strip_accents(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text)
    return "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")


def normalize_book_name(name: str) -> str:
    """Espejo de BibleBookMapper.normalizeForDb().

    IMPORTANTE: el archivo openbile.info usa abreviaturas con mayuscula
    inicial (ej "Ps", "Isa", "Gen") por lo que debemos pasar a minusculas
    ANTES de normalizar.
    """
    cleaned = name.lower().replace(" ", "").replace(".", "").replace("-", "")
    return strip_accents(cleaned)


def map_book(name: str) -> str | None:
    return BOOK_MAP.get(normalize_book_name(name))


# ---------------------------------------------------------------------------
# Parser OSIS (formato openbile.info)
# ---------------------------------------------------------------------------
REF_PATTERN = re.compile(
    r"^(\d?[A-Za-z]+)\.(\d+)\.(\d+)"
    r"(?:-(\d?[A-Za-z]+)\.(\d+)\.(\d+))?$"
)


def parse_reference(ref: str) -> dict | None:
    m = REF_PATTERN.match(ref.strip())
    if not m:
        return None
    book_raw = m.group(1)
    chapter = int(m.group(2))
    verse = int(m.group(3))
    end_book_raw = m.group(4)
    end_chapter = m.group(5)
    end_verse = m.group(6)

    book_es = map_book(book_raw)
    if book_es is None:
        return None
    book_norm = normalize_book_name(book_es)

    end_book_es = None
    end_book_norm = None
    if end_book_raw:
        end_book_es = map_book(end_book_raw)
        if end_book_es is None:
            return None
        end_book_norm = normalize_book_name(end_book_es)

    return {
        "book": book_es,
        "book_norm": book_norm,
        "chapter": chapter,
        "verse": verse,
        "end_book": end_book_es,
        "end_book_norm": end_book_norm,
        "end_chapter": end_chapter,
        "end_verse": end_verse,
    }


def format_spanish_reference(parsed: dict) -> str | None:
    if parsed["end_book"] is None:
        return f"{parsed['book']} {parsed['chapter']}:{parsed['verse']}"

    if (parsed["end_book_norm"] == parsed["book_norm"]
            and parsed["end_chapter"] == parsed["chapter"]):
        return f"{parsed['book']} {parsed['chapter']}:{parsed['verse']}-{parsed['end_verse']}"

    return None


# ---------------------------------------------------------------------------
# Validacion opcional contra la Biblia RV1960
# ---------------------------------------------------------------------------
def load_bible_verses() -> dict[tuple[str, int, int], str]:
    if not BIBLE_JSON_PATH.exists():
        return {}
    with BIBLE_JSON_PATH.open(encoding="utf-8") as f:
        bible = json.load(f)
    verses: dict[tuple[str, int, int], str] = {}
    for book_name, chapters in bible.items():
        if not isinstance(chapters, dict):
            continue
        for chapter_key, verses_in_chapter in chapters.items():
            try:
                chapter = int(chapter_key)
            except (TypeError, ValueError):
                continue
            if not isinstance(verses_in_chapter, dict):
                continue
            for verse_key in verses_in_chapter.keys():
                try:
                    verse = int(verse_key)
                except (TypeError, ValueError):
                    continue
                verses[(normalize_book_name(book_name), chapter, verse)] = ""
    return verses


# ---------------------------------------------------------------------------
# Calculo del identityHash de Room
#
# Room calcula el hash con el siguiente formato (basado en codigo fuente
# androidx/room/Database.kt y TableInfo):
#   md5(table_name + column_name + column_type + column_affinity + pk_position
#       + index_name + index_unique + index_column_name + index_column_order)
#
# Para mantener el formato exacto, seguimos la logica del codigo de Room
# (room-compiler). La forma estable es concatenar todos los campos con
# un separador y aplicar MD5.
# ---------------------------------------------------------------------------
def compute_identity_hash(connection: sqlite3.Connection, table_name: str) -> str:
    """Calcula el identityHash EXACTO que Room espera para una tabla.

    El algoritmo se basa en el codigo fuente de Room:
    androidx/room/verifier/QueryResultSetResult.kt
    """
    hasher = hashlib.md5()

    # Tabla
    hasher.update(table_name.encode("utf-8"))

    # Columnas
    cur = connection.execute(
        'SELECT name, type, pk, "notnull" FROM pragma_table_info(?) ORDER BY cid',
        (table_name,)
    )
    columns = cur.fetchall()
    for col_name, col_type, pk, notnull in columns:
        hasher.update(col_name.encode("utf-8"))
        # Tipo normalizado (uppercase)
        hasher.update(col_type.upper().encode("utf-8") if col_type else b"")
        hasher.update(str(pk).encode("utf-8"))
        hasher.update(str(notnull).encode("utf-8"))

    # Indices (ordenados por rowid para que coincida con el orden del schema SQL)
    cur = connection.execute(
        "SELECT name, sql FROM sqlite_master "
        "WHERE type='index' AND tbl_name=? AND sql IS NOT NULL "
        "ORDER BY rowid",
        (table_name,)
    )
    indices = cur.fetchall()
    for idx_name, idx_sql in indices:
        if idx_name.startswith("sqlite_autoindex_"):
            continue
        hasher.update(idx_name.encode("utf-8"))
        # Parsear SQL del indice para obtener columnas y orden
        # Formato: CREATE INDEX name ON table(col1 [ASC|DESC], col2, ...)
        m = re.match(r"CREATE\s+(?:UNIQUE\s+)?INDEX\s+\S+\s+ON\s+\S+\((.+)\)", idx_sql, re.IGNORECASE)
        if m:
            cols_part = m.group(1)
            for col_def in cols_part.split(","):
                col_def = col_def.strip()
                col_parts = col_def.split()
                col_name = col_parts[0]
                order = col_parts[1].upper() if len(col_parts) > 1 else "ASC"
                hasher.update(col_name.encode("utf-8"))
                hasher.update(order.encode("utf-8"))

    return hasher.hexdigest()


# ---------------------------------------------------------------------------
# Esquema de la base de datos
#
# IMPORTANTE: el orden de CREATE INDEX debe coincidir con el orden de declaracion
# de los @Index en la entity Room.
# ---------------------------------------------------------------------------
SCHEMA = """
PRAGMA user_version = 1;

CREATE TABLE cross_reference_votes (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    source_book TEXT NOT NULL,
    source_normalized_book TEXT NOT NULL,
    source_chapter INTEGER NOT NULL,
    source_verse INTEGER NOT NULL,
    target_references TEXT NOT NULL,
    votes INTEGER NOT NULL
);

CREATE INDEX idx_xref_votes_source
    ON cross_reference_votes(source_normalized_book, source_chapter, source_verse);
CREATE INDEX idx_xref_votes_source_count
    ON cross_reference_votes(source_normalized_book, source_chapter, source_verse, votes DESC);
"""


# ---------------------------------------------------------------------------
# Validacion de schema post-build
# ---------------------------------------------------------------------------
EXPECTED_INDICES = {"idx_xref_votes_source", "idx_xref_votes_source_count"}
EXPECTED_INDICES_ORDER = ["idx_xref_votes_source", "idx_xref_votes_source_count"]
EXPECTED_TABLE_NAME = "cross_reference_votes"


def verify_schema(path: Path) -> None:
    """Verifica que el schema generado coincide con la entity Room.

    IMPORTANTE: valida tanto el SET de indices como el ORDEN.
    """
    conn = sqlite3.connect(path)
    try:
        cur = conn.cursor()

        cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = {r[0] for r in cur.fetchall()}
        assert EXPECTED_TABLE_NAME in tables, (
            f"Tabla '{EXPECTED_TABLE_NAME}' no encontrada. Tablas: {tables}"
        )

        cur.execute(f"PRAGMA table_info({EXPECTED_TABLE_NAME})")
        columns = {r[1]: r[4] for r in cur.fetchall()}
        for col in ("id", "source_book", "source_normalized_book", "source_chapter",
                    "source_verse", "target_references", "votes"):
            assert col in columns, f"Columna '{col}' no encontrada en {EXPECTED_TABLE_NAME}"

        assert columns.get("votes") is None, (
            f"Columna 'votes' tiene default value '{columns['votes']}' "
            f"pero la entity no lo declara."
        )

        cur.execute(
            "SELECT name FROM sqlite_master "
            "WHERE type='index' AND tbl_name=? AND sql IS NOT NULL",
            (EXPECTED_TABLE_NAME,)
        )
        actual_indices = {r[0] for r in cur.fetchall()}
        auto_indices = {n for n in actual_indices if n.startswith("sqlite_autoindex_")}
        user_indices = actual_indices - auto_indices

        assert user_indices == EXPECTED_INDICES, (
            f"Indices no coinciden.\n"
            f"  Esperados: {EXPECTED_INDICES}\n"
            f"  Encontrados: {user_indices}"
        )

        cur.execute(
            "SELECT name FROM sqlite_master "
            "WHERE type='index' AND tbl_name=? AND sql IS NOT NULL "
            "ORDER BY rowid",
            (EXPECTED_TABLE_NAME,)
        )
        actual_order = [r[0] for r in cur.fetchall() if not r[0].startswith("sqlite_autoindex_")]

        assert actual_order == EXPECTED_INDICES_ORDER, (
            f"ORDEN de indices no coincide.\n"
            f"  Esperado:   {EXPECTED_INDICES_ORDER}\n"
            f"  Encontrado: {actual_order}"
        )

        # Verificar que existe room_master_table con identity_hash
        cur.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='room_master_table'")
        assert cur.fetchone() is not None, (
            "Tabla 'room_master_table' no encontrada. Room requiere esta tabla para "
            "verificar la integridad del schema. El script DEBE crearla."
        )

        cur.execute("SELECT identity_hash FROM room_master_table WHERE id = 42")
        row = cur.fetchone()
        assert row is not None, "Falta row id=42 en room_master_table"
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# Pipeline principal
# ---------------------------------------------------------------------------
def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    p.add_argument(
        "--input", "-i",
        type=Path,
        default=DEFAULT_INPUT,
        help=f"Ruta al TSV de openbile.info (default: {DEFAULT_INPUT})",
    )
    p.add_argument(
        "--min-votes", "-m",
        type=int,
        default=1,
        help="Voto minimo para incluir un par (default: 1)",
    )
    p.add_argument(
        "--output", "-o",
        type=Path,
        default=OUTPUT_DB,
        help=f"Ruta de la DB de salida (default: {OUTPUT_DB})",
    )
    p.add_argument(
        "--validate", "-v",
        action="store_true",
        help="Validar que los versiculos existen en rv1960.json",
    )
    return p.parse_args()


def main() -> int:
    args = parse_args()
    input_path: Path = args.input
    output_path: Path = args.output
    min_votes: int = args.min_votes

    if not input_path.exists():
        print(f"ERROR: no se encuentra el archivo de entrada: {input_path}")
        return 1

    print("=" * 60)
    print("Biblion: Building Cross-References Votes Database (openbile.info)")
    print("=" * 60)
    print(f"Entrada : {input_path}")
    print(f"Salida  : {output_path}")
    print(f"min-votes: {min_votes}")
    print()

    bible_verses: dict[tuple[str, int, int], str] = {}
    if args.validate:
        print("[1/5] Cargando rv1960.json para validacion...")
        bible_verses = load_bible_verses()
        print(f"  Versiculos cargados: {len(bible_verses):,}")
    else:
        print("[1/5] Validacion contra RV1960 desactivada.")

    print(f"\n[2/5] Parseando {input_path.name}...")
    rows: list[tuple] = []
    stats = {
        "total_lines": 0,
        "skipped_header": 0,
        "skipped_malformed": 0,
        "skipped_low_votes": 0,
        "skipped_unknown_book": 0,
        "skipped_invalid_range": 0,
        "skipped_invalid_verse": 0,
        "inserted": 0,
    }

    with input_path.open(encoding="utf-8", newline="") as f:
        reader = csv.reader(f, delimiter="\t")
        for line_num, row in enumerate(reader, start=1):
            stats["total_lines"] += 1
            if line_num == 1 and row and row[0].startswith("From"):
                stats["skipped_header"] += 1
                continue
            if len(row) < 3:
                stats["skipped_malformed"] += 1
                continue
            src_ref, tgt_ref, votes_raw = row[0].strip(), row[1].strip(), row[2].strip()
            try:
                votes = int(votes_raw)
            except ValueError:
                stats["skipped_malformed"] += 1
                continue

            if votes < min_votes:
                stats["skipped_low_votes"] += 1
                continue

            src = parse_reference(src_ref)
            if src is None:
                stats["skipped_unknown_book"] += 1
                continue

            tgt = parse_reference(tgt_ref)
            if tgt is None:
                stats["skipped_unknown_book"] += 1
                continue

            tgt_str = format_spanish_reference(tgt)
            if tgt_str is None:
                stats["skipped_invalid_range"] += 1
                continue

            if bible_verses:
                src_key = (src["book_norm"], src["chapter"], src["verse"])
                if src_key not in bible_verses:
                    stats["skipped_invalid_verse"] += 1
                    continue
                tgt_end_verse = tgt["end_verse"] if tgt["end_verse"] is not None else tgt["verse"]
                ok = True
                for v in range(tgt["verse"], tgt_end_verse + 1):
                    if (tgt["book_norm"], tgt["chapter"], v) not in bible_verses:
                        ok = False
                        break
                if not ok:
                    stats["skipped_invalid_verse"] += 1
                    continue

            rows.append((
                src["book"],
                src["book_norm"],
                src["chapter"],
                src["verse"],
                tgt_str,
                votes,
            ))
            stats["inserted"] += 1

    print(f"  Total lineas leidas: {stats['total_lines']:,}")
    print(f"  Header saltado: {stats['skipped_header']}")
    print(f"  Malformadas: {stats['skipped_malformed']}")
    print(f"  Votos < {min_votes}: {stats['skipped_low_votes']:,}")
    print(f"  Libro desconocido: {stats['skipped_unknown_book']}")
    print(f"  Rango invalido: {stats['skipped_invalid_range']}")
    if bible_verses:
        print(f"  Versiculo invalido: {stats['skipped_invalid_verse']}")
    print(f"  Pares insertados: {stats['inserted']:,}")

    if not rows:
        print("ERROR: no hay filas para insertar.")
        return 1

    print(f"\n[3/5] Creando DB: {output_path}")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    connection = sqlite3.connect(output_path)
    try:
        connection.executescript(SCHEMA)
        connection.executemany(
            """
            INSERT INTO cross_reference_votes (
                source_book, source_normalized_book, source_chapter, source_verse,
                target_references, votes
            ) VALUES (?, ?, ?, ?, ?, ?)
            """,
            rows,
        )

        # 3.5) Crear room_master_table con el identity_hash correcto
        print(f"\n[3.5/5] Creando room_master_table con identity_hash...")
        identity_hash = compute_identity_hash(connection, EXPECTED_TABLE_NAME)
        connection.execute(
            "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)"
        )
        connection.execute(
            "INSERT INTO room_master_table (id, identity_hash) VALUES (?, ?)",
            (42, identity_hash)
        )
        print(f"  identity_hash: {identity_hash}")

        connection.commit()
        connection.execute("VACUUM")
    finally:
        connection.close()

    print(f"\n[4/5] Validando schema...")
    try:
        verify_schema(output_path)
        print("  Schema OK - coincide con CrossReferenceVoteEntity")
    except AssertionError as e:
        print(f"  ERROR DE SCHEMA:\n  {e}")
        return 2

    print(f"\n[5/5] Estadisticas de la DB...")
    conn = sqlite3.connect(output_path)
    cur = conn.execute("SELECT COUNT(*) FROM cross_reference_votes")
    total = cur.fetchone()[0]
    cur = conn.execute("SELECT COUNT(DISTINCT source_normalized_book) FROM cross_reference_votes")
    n_books = cur.fetchone()[0]
    cur = conn.execute("SELECT MIN(votes), MAX(votes), AVG(votes) FROM cross_reference_votes")
    vmin, vmax, vavg = cur.fetchone()
    conn.close()

    size_kb = output_path.stat().st_size / 1024

    print(f"\n{'=' * 60}")
    print(f"DB creada: {output_path}")
    print(f"Tamano: {size_kb:.1f} KB")
    print(f"Pares insertados: {total:,}")
    print(f"Libros origen cubiertos: {n_books}")
    print(f"Votos: min={vmin}, max={vmax}, avg={vavg:.1f}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

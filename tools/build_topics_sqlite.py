#!/usr/bin/env python3
"""
Builds topics.db from openbible.info topics data.

Source: https://www.openbible.info/labs/topics/
        (CC-BY 2026-06-15, 6,710 topics with quality scores)

Output:
    app/src/main/assets/databases/topics.db

Compatibilidad con Biblion:
    - source_book normalizado al formato Biblion (Exodo, no Éxodo; "1 Samuel", no "1Sam")
    - source_normalized_book = lowercase, sin acentos, sin espacios (BibleBookMapper.normalizeForDb)
    - osis = "Libro.Cap:V" o "Libro.Cap:V-V" (formato OSIS, compatible con BibleBookMapper.parseOsisRef)
    - topic_key = slug en ASCII (ej. "10_commandments", "abomination")
    - topic_display_en = nombre original en ingles
    - topic_display_es = nombre en espanol (curado o por defecto el EN)
    - quality_score = 2-100 (filtrado >= 2 por default)

Uso:
    python tools/build_topics_sqlite.py
    python tools/build_topics_sqlite.py --min-score 5
    python tools/build_topics_sqlite.py --input "C:\\ruta\\topic-scores.txt"
    python tools/build_topics_sqlite.py --translations "tools/topic_translations_es.json"
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DB = ROOT / "app" / "src" / "main" / "assets" / "databases" / "topics.db"
BIBLE_JSON_PATH = ROOT / "app" / "src" / "main" / "assets" / "rv1960.json"
DEFAULT_INPUT = Path.home() / "Downloads" / "topic-scores.txt"
DEFAULT_TRANSLATIONS = ROOT / "tools" / "topic_translations_es.json"


# ---------------------------------------------------------------------------
# Mapeo de libros: clave normalizada (lowercase, sin acentos, sin espacios, sin puntos)
# -> nombre en espanol Biblion (sin acentos).
# Espejo del map en app/.../feature/bibi/BibleBookMapper.kt.
# ---------------------------------------------------------------------------
BOOK_MAP: dict[str, str] = {
    "genesis": "Genesis", "gen": "Genesis", "gn": "Genesis",
    "exodus": "Exodo", "exod": "Exodo", "ex": "Exodo",
    "leviticus": "Levitico", "lev": "Levitico", "lv": "Levitico",
    "numbers": "Numeros", "num": "Numeros", "nm": "Numeros",
    "deuteronomy": "Deuteronomio", "deut": "Deuteronomio", "dt": "Deuteronomio",
    "joshua": "Josue", "josh": "Josue", "jos": "Josue",
    "judges": "Jueces", "judg": "Jueces", "jud": "Jueces", "jdg": "Jueces",
    "ruth": "Rut", "ru": "Rut",
    "1samuel": "1 Samuel", "1sam": "1 Samuel", "isam": "1 Samuel",
    "2samuel": "2 Samuel", "2sam": "2 Samuel", "iisam": "2 Samuel",
    "1kings": "1 Reyes", "1kgs": "1 Reyes",
    "2kings": "2 Reyes", "2kgs": "2 Reyes",
    "1chronicles": "1 Cronicas", "1chr": "1 Cronicas",
    "2chronicles": "2 Cronicas", "2chr": "2 Cronicas",
    "ezra": "Esdras", "ezr": "Esdras",
    "nehemiah": "Nehemias", "neh": "Nehemias",
    "esther": "Ester", "esth": "Ester", "est": "Ester",
    "job": "Job", "jb": "Job",
    "psalms": "Salmos", "psalm": "Salmos", "ps": "Salmos", "psa": "Salmos",
    "proverbs": "Proverbios", "prov": "Proverbios", "pr": "Proverbios",
    "ecclesiastes": "Eclesiastes", "eccl": "Eclesiastes", "ecc": "Eclesiastes",
    "songofsolomon": "Cantares", "song": "Cantares", "cant": "Cantares",
    "songofsongs": "Cantares", "canticles": "Cantares",
    "isaiah": "Isaias", "isa": "Isaias", "is": "Isaias",
    "jeremiah": "Jeremias", "jer": "Jeremias", "je": "Jeremias",
    "lamentations": "Lamentaciones", "lam": "Lamentaciones",
    "ezekiel": "Ezequiel", "ezek": "Ezequiel", "eze": "Ezequiel",
    "daniel": "Daniel", "dan": "Daniel", "da": "Daniel",
    "hosea": "Oseas", "hos": "Oseas",
    "joel": "Joel", "jl": "Joel",
    "amos": "Amos", "am": "Amos",
    "obadiah": "Abdias", "obad": "Abdias", "ob": "Abdias",
    "jonah": "Jonas", "jon": "Jonas",
    "micah": "Miqueas", "mic": "Miqueas",
    "nahum": "Nahum", "nah": "Nahum",
    "habakkuk": "Habacuc", "hab": "Habacuc",
    "zephaniah": "Sofonias", "zeph": "Sofonias", "zep": "Sofonias",
    "haggai": "Hageo", "hag": "Hageo",
    "zechariah": "Zacarias", "zech": "Zacarias", "zec": "Zacarias",
    "malachi": "Malaquias", "mal": "Malaquias",
    "matthew": "Mateo", "matt": "Mateo", "mt": "Mateo",
    "mark": "Marcos", "mk": "Marcos", "mr": "Marcos",
    "luke": "Lucas", "lk": "Lucas", "lc": "Lucas",
    "john": "Juan", "jn": "Juan", "joh": "Juan",
    "acts": "Hechos", "ac": "Hechos",
    "romans": "Romanos", "rom": "Romanos", "ro": "Romanos",
    "1corinthians": "1 Corintios", "1cor": "1 Corintios", "icor": "1 Corintios",
    "2corinthians": "2 Corintios", "2cor": "2 Corintios", "iicor": "2 Corintios",
    "galatians": "Galatas", "gal": "Galatas",
    "ephesians": "Efesios", "eph": "Efesios",
    "philippians": "Filipenses", "phil": "Filipenses", "php": "Filipenses",
    "colossians": "Colosenses", "col": "Colosenses",
    "1thessalonians": "1 Tesalonicenses", "1thess": "1 Tesalonicenses",
    "2thessalonians": "2 Tesalonicenses", "2thess": "2 Tesalonicenses",
    "1timothy": "1 Timoteo", "1tim": "1 Timoteo",
    "2timothy": "2 Timoteo", "2tim": "2 Timoteo",
    "titus": "Tito", "tit": "Tito",
    "philemon": "Filemon", "philem": "Filemon", "phlm": "Filemon", "flm": "Filemon",
    "hebrews": "Hebreos", "heb": "Hebreos",
    "james": "Santiago", "jas": "Santiago", "jam": "Santiago",
    "1peter": "1 Pedro", "1pet": "1 Pedro",
    "2peter": "2 Pedro", "2pet": "2 Pedro",
    "1john": "1 Juan", "1jn": "1 Juan",
    "2john": "2 Juan", "2jn": "2 Juan",
    "3john": "3 Juan", "3jn": "3 Juan",
    "jude": "Judas", "jud": "Judas",
    "revelation": "Apocalipsis", "rev": "Apocalipsis", "re": "Apocalipsis",
}


# ---------------------------------------------------------------------------
# Normalizacion de strings (espejo de BibleBookMapper.normalizeForDb)
# ---------------------------------------------------------------------------
def strip_accents(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text)
    return "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")


def normalize_book_name(name: str) -> str:
    cleaned = name.lower().replace(" ", "").replace(".", "").replace("-", "")
    return strip_accents(cleaned)


def map_book(name: str) -> str | None:
    key = normalize_book_name(name)
    return BOOK_MAP.get(key)


# ---------------------------------------------------------------------------
# Slug de topic: "10 commandments" -> "10_commandaments", "the meaning of life" -> "the_meaning_of_life"
# ---------------------------------------------------------------------------
def slugify_topic(text: str) -> str:
    """Convierte un nombre de topic a un slug ASCII seguro para SQL."""
    normalized = strip_accents(text.lower())
    slug = re.sub(r"[^a-z0-9]+", "_", normalized)
    slug = slug.strip("_")
    return slug or "unknown"


# ---------------------------------------------------------------------------
# Parser de OSIS del archivo openbible.info
# Formatos validos:
#   - "Gen.1.1"               -> versiculo simple
#   - "Gen.1.1-Gen.1.3"       -> rango mismo libro/cap
#   - "1John.4.9-1John.4.10"  -> rango con libro numerado (sin espacio)
# ---------------------------------------------------------------------------
REF_PATTERN = re.compile(
    r"^(\d?[A-Za-z]+)\.(\d+)\.(\d+)"
    r"(?:-(\d?[A-Za-z]+)\.(\d+)\.(\d+))?$"
)


def parse_osis(osis: str) -> dict | None:
    """Parsea una referencia OSIS openbible.info a componentes estructurados.

    Returns dict con:
        book, book_norm, chapter, verse_start, verse_end
        (verse_end == verse_start si es versiculo simple)

    Retorna None si la referencia no se reconoce.
    """
    m = REF_PATTERN.match(osis.strip())
    if not m:
        return None
    book_raw = m.group(1)
    chapter = int(m.group(2))
    verse_start = int(m.group(3))
    end_book_raw = m.group(4)
    end_chapter = m.group(5)
    end_verse = m.group(6)

    book_es = map_book(book_raw)
    if book_es is None:
        return None
    book_norm = normalize_book_name(book_es)

    if end_book_raw:
        end_book_es = map_book(end_book_raw)
        if end_book_es is None:
            return None
        # Solo aceptamos rangos mismo libro/cap para Biblion
        if (normalize_book_name(end_book_es) != book_norm
                or int(end_chapter) != chapter):
            return None
        verse_end = int(end_verse)
    else:
        verse_end = verse_start

    return {
        "book": book_es,
        "book_norm": book_norm,
        "chapter": chapter,
        "verse_start": verse_start,
        "verse_end": verse_end,
    }


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
# Esquema de la base de datos
#
# IMPORTANTE: este schema debe coincidir EXACTAMENTE con la entity Room
# TopicScoreEntity. Si agregas un default value aqui, debes declararlo
# tambien en el entity con @ColumnInfo(defaultValue = "...") o viceversa.
# La validacion post-build detecta cualquier desalineamiento.
# ---------------------------------------------------------------------------
SCHEMA = """
PRAGMA user_version = 1;

CREATE TABLE topic_scores (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    topic_key TEXT NOT NULL,
    topic_display_es TEXT NOT NULL,
    topic_display_en TEXT NOT NULL,
    osis TEXT NOT NULL,
    source_book TEXT NOT NULL,
    source_normalized_book TEXT NOT NULL,
    source_chapter INTEGER NOT NULL,
    source_verse_start INTEGER NOT NULL,
    source_verse_end INTEGER NOT NULL,
    quality_score INTEGER NOT NULL
);

-- IMPORTANTE: el orden de CREATE INDEX debe coincidir con el orden de declaracion
-- de los @Index en la entity Room. Room calcula el identityHash basado en este
-- orden. Si el orden difiere, falla checkIdentity aunque los nombres coincidan.
CREATE INDEX idx_topic_key
    ON topic_scores(topic_key);
CREATE INDEX idx_topic_source
    ON topic_scores(source_normalized_book, source_chapter, source_verse_start);
CREATE INDEX idx_topic_key_score
    ON topic_scores(topic_key, quality_score DESC);
CREATE INDEX idx_topic_source_score
    ON topic_scores(source_normalized_book, source_chapter, source_verse_start, quality_score DESC);
"""


# ---------------------------------------------------------------------------
# Validacion de schema post-build
# ---------------------------------------------------------------------------
EXPECTED_INDICES = {
    "idx_topic_key",
    "idx_topic_source",
    "idx_topic_key_score",
    "idx_topic_source_score"
}
# ORDEN esperado: Room calcula identityHash segun el orden de declaracion
# de los @Index en el entity. Si el orden difiere, falla checkIdentity.
EXPECTED_INDICES_ORDER = [
    "idx_topic_key",
    "idx_topic_source",
    "idx_topic_key_score",
    "idx_topic_source_score"
]
EXPECTED_TABLE_NAME = "topic_scores"


def verify_schema(path: Path) -> None:
    """Verifica que el schema generado coincide con la entity Room.

    Lanza AssertionError si hay desalineamiento. Esto evita el crash
    "Pre-packaged database has an invalid schema" al instalar el APK.

    IMPORTANTE: valida tanto el SET de indices como el ORDEN, porque
    Room calcula el identityHash basado en el orden de declaracion
    de los @Index en el entity.
    """
    conn = sqlite3.connect(path)
    try:
        cur = conn.cursor()

        # 1) Verificar que existe la tabla esperada
        cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = {r[0] for r in cur.fetchall()}
        assert EXPECTED_TABLE_NAME in tables, (
            f"Tabla '{EXPECTED_TABLE_NAME}' no encontrada. Tablas: {tables}"
        )

        # 2) Verificar columnas
        cur.execute(f"PRAGMA table_info({EXPECTED_TABLE_NAME})")
        columns = {r[1]: r[4] for r in cur.fetchall()}  # name -> dflt_value
        for col in ("id", "topic_key", "topic_display_es", "topic_display_en", "osis",
                    "source_book", "source_normalized_book", "source_chapter",
                    "source_verse_start", "source_verse_end", "quality_score"):
            assert col in columns, f"Columna '{col}' no encontrada en {EXPECTED_TABLE_NAME}"

        # 3) Verificar que quality_score NO tiene default value
        assert columns.get("quality_score") is None, (
            f"Columna 'quality_score' tiene default value '{columns['quality_score']}' "
            f"pero la entity no lo declara. O quitar DEFAULT 0 del schema Python o agregar "
            f"@ColumnInfo(defaultValue = \"0\") en TopicScoreEntity.kt"
        )

        # 4) Verificar indices - SET
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
            f"  Esperados (set): {EXPECTED_INDICES}\n"
            f"  Encontrados (set): {user_indices}\n"
            f"  Si agregaste un indice aqui, declaralo en la entity Room. "
            f"Si lo quitaste del entity, quitalo tambien del script."
        )

        # 5) Verificar indices - ORDEN (Room calcula identityHash por orden)
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
            f"  Encontrado: {actual_order}\n"
            f"  Room calcula el identityHash del schema usando el ORDEN "
            f"  de declaracion de los @Index en el entity. Si el orden "
            f"  difiere, falla checkIdentity aunque los nombres coincidan."
        )

        # Verificar que existe room_master_table con identity_hash
        cur.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='room_master_table'")
        assert cur.fetchone() is not None, (
            "Tabla 'room_master_table' no encontrada. Room requiere esta tabla."
        )

        cur.execute("SELECT identity_hash FROM room_master_table WHERE id = 42")
        assert cur.fetchone() is not None, "Falta row id=42 en room_master_table"
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# Carga de traducciones
# ---------------------------------------------------------------------------
def load_translations(path: Path) -> dict[str, str]:
    """Carga el diccionario curado de traducciones EN -> ES.
    Retorna dict[topic_key, display_es]. Si el archivo no existe, retorna {}.
    """
    if not path.exists():
        return {}
    with path.open(encoding="utf-8") as f:
        data = json.load(f)
    return {k: v for k, v in data.items() if v}


# ---------------------------------------------------------------------------
# Pipeline principal
# ---------------------------------------------------------------------------
def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    p.add_argument(
        "--input", "-i",
        type=Path,
        default=DEFAULT_INPUT,
        help=f"Ruta al TSV de openbible.info (default: {DEFAULT_INPUT})",
    )
    p.add_argument(
        "--min-score", "-m",
        type=int,
        default=2,
        help="Quality Score minimo para incluir (default: 2)",
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
    p.add_argument(
        "--translations", "-t",
        type=Path,
        default=DEFAULT_TRANSLATIONS,
        help=f"Ruta al JSON de traducciones curado (default: {DEFAULT_TRANSLATIONS})",
    )
    return p.parse_args()


def main() -> int:
    args = parse_args()
    input_path: Path = args.input
    output_path: Path = args.output
    min_score: int = args.min_score

    if not input_path.exists():
        print(f"ERROR: no se encuentra el archivo de entrada: {input_path}")
        print(f"Descargalo desde https://www.openbible.info/labs/topics/")
        print(f"O usa --input para especificar otra ruta.")
        return 1

    print("=" * 60)
    print("Biblion: Building Topics Database (openbible.info)")
    print("=" * 60)
    print(f"Entrada      : {input_path}")
    print(f"Salida       : {output_path}")
    print(f"min-score    : {min_score}")
    print(f"Traducciones : {args.translations}")
    print()

    # 1) Cargar traducciones
    print("[1/5] Cargando traducciones curadas...")
    translations = load_translations(args.translations)
    print(f"  Traducciones cargadas: {len(translations)}")
    print()

    # 2) Cargar validacion opcional
    bible_verses: dict[tuple[str, int, int], str] = {}
    if args.validate:
        print("[2/5] Cargando rv1960.json para validacion...")
        bible_verses = load_bible_verses()
        print(f"  Versiculos cargados: {len(bible_verses):,}")
    else:
        print("[2/5] Validacion contra RV1960 desactivada (usar --validate).")
    print()

    # 3) Parsear TSV
    print(f"[3/5] Parseando {input_path.name}...")
    rows: list[tuple] = []
    stats = {
        "total_lines": 0,
        "skipped_header": 0,
        "skipped_malformed": 0,
        "skipped_low_score": 0,
        "skipped_unknown_book": 0,
        "skipped_invalid_range": 0,
        "skipped_invalid_verse": 0,
        "inserted": 0,
    }
    topic_keys: dict[str, int] = {}
    translated_count = 0

    with input_path.open(encoding="utf-8", newline="") as f:
        reader = csv.reader(f, delimiter="\t")
        for line_num, row in enumerate(reader, start=1):
            stats["total_lines"] += 1
            if line_num == 1 and row and row[0].startswith("Topic"):
                stats["skipped_header"] += 1
                continue
            if len(row) < 3:
                stats["skipped_malformed"] += 1
                continue
            topic_en, osis_raw, score_raw = row[0].strip(), row[1].strip(), row[2].strip()
            try:
                score = int(score_raw)
            except ValueError:
                stats["skipped_malformed"] += 1
                continue

            if score < min_score:
                stats["skipped_low_score"] += 1
                continue

            parsed = parse_osis(osis_raw)
            if parsed is None:
                if "-" in osis_raw and "." in osis_raw:
                    stats["skipped_invalid_range"] += 1
                else:
                    stats["skipped_unknown_book"] += 1
                continue

            if bible_verses:
                ok = True
                for v in range(parsed["verse_start"], parsed["verse_end"] + 1):
                    if (parsed["book_norm"], parsed["chapter"], v) not in bible_verses:
                        ok = False
                        break
                if not ok:
                    stats["skipped_invalid_verse"] += 1
                    continue

            topic_key = slugify_topic(topic_en)
            display_es = translations.get(topic_key, topic_en)
            if topic_key in translations:
                translated_count += 1

            topic_keys[topic_key] = topic_keys.get(topic_key, 0) + 1

            rows.append((
                topic_key,
                display_es,
                topic_en,
                osis_raw,
                parsed["book"],
                parsed["book_norm"],
                parsed["chapter"],
                parsed["verse_start"],
                parsed["verse_end"],
                score,
            ))
            stats["inserted"] += 1

    print(f"  Total lineas leidas: {stats['total_lines']:,}")
    print(f"  Header saltado: {stats['skipped_header']}")
    print(f"  Malformadas: {stats['skipped_malformed']}")
    print(f"  Score < {min_score}: {stats['skipped_low_score']:,}")
    print(f"  Libro desconocido: {stats['skipped_unknown_book']}")
    print(f"  Rango invalido: {stats['skipped_invalid_range']}")
    if bible_verses:
        print(f"  Versiculo invalido: {stats['skipped_invalid_verse']}")
    print(f"  Pares insertados: {stats['inserted']:,}")
    print(f"  Temas unicos: {len(topic_keys):,}")
    print(f"  Traducidos al espanol (curado): {translated_count}")
    print()

    if not rows:
        print("ERROR: no hay filas para insertar. Revisa --min-score y el archivo de entrada.")
        return 1

    # 4) Crear DB
    print(f"\n[4/5] Creando DB: {output_path}")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    connection = sqlite3.connect(output_path)
    try:
        connection.executescript(SCHEMA)
        connection.executemany(
            """
            INSERT INTO topic_scores (
                topic_key, topic_display_es, topic_display_en, osis,
                source_book, source_normalized_book, source_chapter,
                source_verse_start, source_verse_end, quality_score
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            rows,
        )

        # 4.2) Crear room_master_table con el identity_hash correcto
        import hashlib
        import re
        hasher = hashlib.md5()
        hasher.update(EXPECTED_TABLE_NAME.encode("utf-8"))
        cur = connection.execute(
            'SELECT name, type, pk, "notnull" FROM pragma_table_info(?) ORDER BY cid',
            (EXPECTED_TABLE_NAME,)
        )
        for col_name, col_type, pk, notnull in cur.fetchall():
            hasher.update(col_name.encode("utf-8"))
            hasher.update(col_type.upper().encode("utf-8") if col_type else b"")
            hasher.update(str(pk).encode("utf-8"))
            hasher.update(str(notnull).encode("utf-8"))
        cur = connection.execute(
            "SELECT name, sql FROM sqlite_master "
            "WHERE type='index' AND tbl_name=? AND sql IS NOT NULL "
            "ORDER BY rowid",
            (EXPECTED_TABLE_NAME,)
        )
        for idx_name, idx_sql in cur.fetchall():
            if idx_name.startswith("sqlite_autoindex_"):
                continue
            hasher.update(idx_name.encode("utf-8"))
            m = re.match(r"CREATE\s+(?:UNIQUE\s+)?INDEX\s+\S+\s+ON\s+\S+\((.+)\)", idx_sql, re.IGNORECASE)
            if m:
                for col_def in m.group(1).split(","):
                    col_def = col_def.strip()
                    col_parts = col_def.split()
                    col_name = col_parts[0]
                    order = col_parts[1].upper() if len(col_parts) > 1 else "ASC"
                    hasher.update(col_name.encode("utf-8"))
                    hasher.update(order.encode("utf-8"))
        identity_hash = hasher.hexdigest()

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

    # 4.5) Validar que el schema generado coincide con la entity Room
    print(f"\n[4.5/5] Validando schema contra la entity Room...")
    try:
        verify_schema(output_path)
        print("  Schema OK - coincide con TopicScoreEntity")
    except AssertionError as e:
        print(f"  ERROR DE SCHEMA:\n  {e}")
        return 2

    # 5) Estadisticas
    print("[5/5] Estadisticas de la DB...")
    conn = sqlite3.connect(output_path)
    cur = conn.execute("SELECT COUNT(*) FROM topic_scores")
    total = cur.fetchone()[0]
    cur = conn.execute("SELECT COUNT(DISTINCT topic_key) FROM topic_scores")
    n_topics = cur.fetchone()[0]
    cur = conn.execute("SELECT COUNT(DISTINCT source_normalized_book) FROM topic_scores")
    n_books = cur.fetchone()[0]
    cur = conn.execute("SELECT MIN(quality_score), MAX(quality_score), AVG(quality_score) FROM topic_scores")
    smin, smax, savg = cur.fetchone()
    cur = conn.execute(
        "SELECT topic_display_es, COUNT(*) AS n FROM topic_scores "
        "GROUP BY topic_key ORDER BY n DESC LIMIT 5"
    )
    top_topics = cur.fetchall()
    conn.close()

    size_kb = output_path.stat().st_size / 1024

    print()
    print(f"{'=' * 60}")
    print(f"DB creada: {output_path}")
    print(f"Tamano: {size_kb:.1f} KB")
    print(f"Pares insertados: {total:,}")
    print(f"Temas unicos: {n_topics:,}")
    print(f"Libros origen cubiertos: {n_books}")
    print(f"Top 5 temas por cantidad: {top_topics}")
    print(f"Quality Score: min={smin}, max={smax}, avg={savg:.1f}")
    print()
    print("Para usar en Android, agregar la siguiente entidad Room:")
    print("""
@Entity(
    tableName = "topic_scores",
    indices = [
        Index(value = ["topicKey"]),
        Index(value = ["sourceNormalizedBook", "sourceChapter", "sourceVerseStart"]),
        Index(value = ["topicKey", "qualityScore"]),
        Index(value = ["sourceNormalizedBook", "sourceChapter", "sourceVerseStart", "qualityScore"])
    ]
)
data class TopicScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "topic_key") val topicKey: String,
    @ColumnInfo(name = "topic_display_es") val topicDisplayEs: String,
    @ColumnInfo(name = "topic_display_en") val topicDisplayEn: String,
    @ColumnInfo(name = "osis") val osis: String,
    @ColumnInfo(name = "source_book") val sourceBook: String,
    @ColumnInfo(name = "source_normalized_book") val sourceNormalizedBook: String,
    @ColumnInfo(name = "source_chapter") val sourceChapter: Int,
    @ColumnInfo(name = "source_verse_start") val sourceVerseStart: Int,
    @ColumnInfo(name = "source_verse_end") val sourceVerseEnd: Int,
    @ColumnInfo(name = "quality_score") val qualityScore: Int
)
""")
    return 0


if __name__ == "__main__":
    sys.exit(main())

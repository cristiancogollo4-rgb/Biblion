"""
Fase 7: Genera la nueva topics.db con schema Biblion.

Schema:
- topics(id, slug, name_es, name_en, category, verse_count, total_verses, cluster_size)
- topic_aliases(id, topic_id, alias_en)
- topic_references(id, topic_id, book, chapter, verse_start, verse_end, score)

Usa:
- topic_taxonomy_final.json (clusters ya clasificados y traducidos)
- topic-scores.txt (raw openbile data con referencias OSIS)
- bible_books.json (mapeo OSIS -> Biblion)
"""
from __future__ import annotations

import json
import re
import sqlite3
from pathlib import Path

OUT_DIR = Path("tools")
SOURCE_FILE = Path(r"C:\Users\LENOVO LOQ\Downloads\topic-scores.txt")
TARGET_DB = Path(r"C:\Users\LENOVO LOQ\Desktop\PROYECTOS\Biblion\app\src\main\assets\databases\topics.db")

# Mapeo OSIS book code -> Biblion book name (66 libros canonicos)
# Formato Biblion: "Genesis", "Exodo", "1 Samuel", etc. (sin acentos)
# Ajustar segun lo que use la app.
OSIS_TO_BIBLION: dict[str, str] = {
    # AT
    "Gen": "Genesis", "Exod": "Exodo", "Lev": "Levitico", "Num": "Numeros",
    "Deut": "Deuteronomio", "Josh": "Josue", "Judg": "Jueces", "Ruth": "Rut",
    "1Sam": "1 Samuel", "2Sam": "2 Samuel", "1Kgs": "1 Reyes", "2Kgs": "2 Reyes",
    "1Chr": "1 Cronicas", "2Chr": "2 Cronicas", "Ezra": "Esdras",
    "Neh": "Nehemias", "Esth": "Ester", "Job": "Job", "Ps": "Salmos",
    "Prov": "Proverbios", "Eccl": "Eclesiastes", "Song": "Cantares",
    "Isa": "Isaias", "Jer": "Jeremias", "Lam": "Lamentaciones",
    "Ezek": "Ezequiel", "Dan": "Daniel", "Hos": "Oseas", "Joel": "Joel",
    "Amos": "Amos", "Obad": "Abdias", "Jonah": "Jonas", "Mic": "Miqueas",
    "Nah": "Nahum", "Hab": "Habacuc", "Zeph": "Sofonias", "Hag": "Hageo",
    "Zech": "Zacarias", "Mal": "Malaquias",
    # NT
    "Matt": "Mateo", "Mark": "Marcos", "Luke": "Lucas", "John": "Juan",
    "Acts": "Hechos", "Rom": "Romanos", "1Cor": "1 Corintios", "2Cor": "2 Corintios",
    "Gal": "Galatas", "Eph": "Efesios", "Phil": "Filipenses", "Col": "Colosenses",
    "1Thess": "1 Tesalonicenses", "2Thess": "2 Tesalonicenses",
    "1Tim": "1 Timoteo", "2Tim": "2 Timoteo", "Titus": "Tito", "Phlm": "Filemon",
    "Heb": "Hebreos", "Jas": "Santiago", "1Pet": "1 Pedro", "2Pet": "2 Pedro",
    "1John": "1 Juan", "2John": "2 Juan", "3John": "3 Juan", "Jude": "Judas",
    "Rev": "Apocalipsis",
}


def normalize_slug(text: str) -> str:
    s = text.lower().strip()
    s = re.sub(r"[^\w\s-]", "", s)
    s = re.sub(r"\s+", "-", s)
    s = re.sub(r"-+", "-", s).strip("-")
    return s[:120]


def parse_osis_ref(osis: str) -> tuple[str, int, int, int] | None:
    """Parse OSIS ref like 'Gen.1.1' or 'Gen.1.1-Gen.1.3' or 'Gen.1.1.2' (verse.word)."""
    # Range: Book.Chapter.Verse-Book.Chapter.Verse
    if "-" in osis:
        parts = osis.split("-")
        if len(parts) == 2:
            start = parse_single_osis(parts[0])
            end = parse_single_osis(parts[1])
            if start and end and start[0] == end[0] and start[1] == end[1]:
                # Same book and chapter: range
                return (start[0], start[1], start[2], end[2])
        return None
    return parse_single_osis(osis)


def parse_single_osis(osis: str) -> tuple[str, int, int, int] | None:
    """Parse single OSIS like 'Gen.1.1' or 'Gen.1.1.2'.
    Returns (book_biblion, chapter, verse_start, verse_end)."""
    parts = osis.split(".")
    if len(parts) < 3:
        return None
    book_osis = parts[0]
    if book_osis not in OSIS_TO_BIBLION:
        return None
    try:
        chapter = int(parts[1])
        verse = int(parts[2])
    except ValueError:
        return None
    book = OSIS_TO_BIBLION[book_osis]
    return (book, chapter, verse, verse)


def load_taxonomy() -> dict:
    with (OUT_DIR / "topic_taxonomy_final.json").open(encoding="utf-8") as f:
        return json.load(f)


def build_topic_key_map(taxonomy: dict) -> dict[str, str]:
    """Mapea cada alias_en o name_en a su slug de cluster."""
    mapping: dict[str, str] = {}
    for cat_name, cat in taxonomy["categories"].items():
        for cluster in cat["clusters"]:
            slug = cluster["slug"]
            mapping[normalize_slug(cluster["name_en"])] = slug
            for alias in cluster.get("aliases", []):
                mapping[normalize_slug(alias)] = slug
    return mapping


def parse_topic_scores_file() -> dict[str, list[tuple[str, int, int, int]]]:
    """Parsea topic-scores.txt y retorna {topic_slug: [refs]}.
    topic_key esta en formato 'topic_name' lowercase.
    """
    if not SOURCE_FILE.exists():
        print(f"WARN: {SOURCE_FILE} no existe. No se cargaran references.")
        return {}
    refs: dict[str, list] = {}
    n_lines = 0
    n_parsed = 0
    n_skipped_osis = 0
    n_skipped_topic = 0
    with SOURCE_FILE.open(encoding="utf-8") as f:
        for line in f:
            n_lines += 1
            parts = line.strip().split("\t")
            if len(parts) < 3:
                continue
            topic_key = parts[0]
            osis_list_str = parts[1]
            try:
                score = float(parts[2])
            except ValueError:
                continue

            # topic_key puede tener espacios
            topic_slug = normalize_slug(topic_key)

            # Parsear cada OSIS ref
            osis_refs = osis_list_str.split(",")
            parsed_refs = []
            for osis in osis_refs:
                parsed = parse_osis_ref(osis.strip())
                if parsed:
                    parsed_refs.append((*parsed, score))
                else:
                    n_skipped_osis += 1

            if parsed_refs:
                refs[topic_slug] = parsed_refs
                n_parsed += 1
            else:
                n_skipped_topic += 1

    print(f"  Lineas procesadas: {n_lines}")
    print(f"  Topics con refs: {n_parsed}")
    print(f"  Topics sin refs: {n_skipped_topic}")
    print(f"  OSIS no parseadas: {n_skipped_osis}")
    return refs


def main() -> None:
    print("=" * 60)
    print("Biblion: Fase 7 - Generacion de nueva topics.db")
    print("=" * 60)
    print()

    taxonomy = load_taxonomy()
    print(f"Taxonomia cargada: {taxonomy['n_clusters']} clusters")

    # Mapeo topic_name -> slug
    key_to_slug = build_topic_key_map(taxonomy)
    print(f"Aliases mapeados: {len(key_to_slug)}")

    # Cargar references del raw
    print(f"\n[1/3] Parseando {SOURCE_FILE.name}...")
    all_refs = parse_topic_scores_file()

    # Consolidar refs por slug
    print(f"\n[2/3] Consolidando references por cluster...")
    slug_refs: dict[str, list] = {}
    for topic_slug, refs in all_refs.items():
        # Buscar el slug del cluster que contiene este topic
        target_slug = key_to_slug.get(topic_slug)
        if target_slug is None:
            continue
        if target_slug not in slug_refs:
            slug_refs[target_slug] = []
        slug_refs[target_slug].extend(refs)

    # Deduplicar: misma referencia puede aparecer varias veces
    for slug, refs in slug_refs.items():
        seen: set[tuple] = set()
        deduped = []
        for r in refs:
            key = (r[0], r[1], r[2], r[3])  # (book, chapter, vs, ve)
            if key not in seen:
                seen.add(key)
                deduped.append(r)
        slug_refs[slug] = deduped

    # Stats
    total_refs = sum(len(r) for r in slug_refs.values())
    clusters_with_refs = sum(1 for r in slug_refs.values() if r)
    print(f"  Total references: {total_refs}")
    print(f"  Clusters con references: {clusters_with_refs} / {taxonomy['n_clusters']}")

    # Generar DB
    print(f"\n[3/3] Generando {TARGET_DB}...")
    if TARGET_DB.exists():
        TARGET_DB.unlink()
    TARGET_DB.parent.mkdir(parents=True, exist_ok=True)

    conn = sqlite3.connect(str(TARGET_DB))
    c = conn.cursor()

    # Schema (SIN default values, igual que los entities de Kotlin)
    c.execute("""
        CREATE TABLE topics (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            slug TEXT NOT NULL,
            name_es TEXT NOT NULL,
            name_en TEXT NOT NULL,
            category TEXT NOT NULL,
            verse_count INTEGER NOT NULL,
            total_verses INTEGER NOT NULL,
            cluster_size INTEGER NOT NULL,
            is_translated_auto INTEGER NOT NULL
        )
    """)
    c.execute("""
        CREATE TABLE topic_aliases (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            topic_id INTEGER NOT NULL,
            alias_en TEXT NOT NULL,
            FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
        )
    """)
    c.execute("""
        CREATE TABLE topic_references (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            topic_id INTEGER NOT NULL,
            book TEXT NOT NULL,
            chapter INTEGER NOT NULL,
            verse_start INTEGER NOT NULL,
            verse_end INTEGER NOT NULL,
            score REAL NOT NULL,
            FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
        )
    """)
    c.execute("""
        CREATE TABLE room_master_table (
            id INTEGER PRIMARY KEY,
            identity_hash TEXT
        )
    """)

    # UNIQUE constraint para slug
    c.execute("CREATE UNIQUE INDEX idx_topics_slug ON topics(slug)")

    # Indices
    c.execute("CREATE INDEX idx_topics_category ON topics(category)")
    c.execute("CREATE INDEX idx_topics_name_es ON topics(name_es)")
    c.execute("CREATE INDEX idx_topics_name_en ON topics(name_en)")
    c.execute("CREATE INDEX idx_aliases_topic ON topic_aliases(topic_id)")
    c.execute("CREATE INDEX idx_aliases_text ON topic_aliases(alias_en)")
    c.execute("CREATE INDEX idx_references_topic ON topic_references(topic_id)")
    c.execute("CREATE INDEX idx_references_book ON topic_references(book)")
    c.execute("CREATE INDEX idx_references_ref ON topic_references(book, chapter)")
    c.execute("CREATE INDEX idx_references_topic_score ON topic_references(topic_id, score)")

    # Insertar topics
    topic_id_by_slug: dict[str, int] = {}
    n_inserted = 0
    for cat_name, cat in taxonomy["categories"].items():
        for cluster in cat["clusters"]:
            slug = cluster["slug"]
            name_es = cluster["name_es"] or cluster["name_en"]
            name_en = cluster["name_en"]
            is_auto = 1 if cluster.get("name_es_auto") else 0
            c.execute(
                """INSERT INTO topics
                (slug, name_es, name_en, category, verse_count,
                 total_verses, cluster_size, is_translated_auto)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    slug, name_es, name_en, cat_name,
                    cluster["verse_count"], cluster["total_verses"],
                    cluster["cluster_size"], is_auto,
                ),
            )
            topic_id = c.lastrowid
            topic_id_by_slug[slug] = topic_id
            n_inserted += 1

    # Insertar aliases
    n_aliases = 0
    for cat_name, cat in taxonomy["categories"].items():
        for cluster in cat["clusters"]:
            topic_id = topic_id_by_slug[cluster["slug"]]
            for alias in cluster.get("aliases", []):
                c.execute(
                    "INSERT INTO topic_aliases (topic_id, alias_en) VALUES (?, ?)",
                    (topic_id, alias),
                )
                n_aliases += 1

    # Insertar references
    n_refs = 0
    for slug, refs in slug_refs.items():
        topic_id = topic_id_by_slug.get(slug)
        if topic_id is None:
            continue
        for book, chapter, vs, ve, score in refs:
            c.execute(
                """INSERT INTO topic_references
                (topic_id, book, chapter, verse_start, verse_end, score)
                VALUES (?, ?, ?, ?, ?, ?)""",
                (topic_id, book, chapter, vs, ve, score),
            )
            n_refs += 1

    # Identity hash para Room (v1 = placeholder, sera regenerado en test)
    c.execute(
        "INSERT INTO room_master_table (id, identity_hash) VALUES (42, 'pending')"
    )

    conn.commit()
    conn.close()

    print(f"\n=== Resumen Fase 7 ===")
    print(f"DB creada: {TARGET_DB}")
    print(f"Topics insertados: {n_inserted}")
    print(f"Aliases insertados: {n_aliases}")
    print(f"References insertadas: {n_refs}")
    print(f"Cobertura refs: {clusters_with_refs}/{n_inserted} "
          f"({clusters_with_refs/n_inserted*100:.1f}%)")
    print(f"\nIMPORTANTE: ejecutar InjectRoomIdentityHashTest para")
    print(f"  generar identity_hash correcto.")


if __name__ == "__main__":
    main()

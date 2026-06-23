#!/usr/bin/env python3
"""
Extractor data-driven de vocabulario KJV -> RV1960.

Para cada versiculo donde TSK tiene un anchor y rv1960.json tiene texto,
cuenta la co-ocurrencia de palabras ingles y espanol en el mismo versiculo.
Propone mapeos KJV -> RV1960 basados en la frecuencia de co-ocurrencia.

Output: tools/anchor_word_vocab_proposed.json

Uso:
    python tools/extract_anchor_vocab.py

Las entradas propuestas deben validarse manualmente antes de mergear
con anchor_word_vocab.json.
"""
from __future__ import annotations

import csv
import io
import json
import re
import sqlite3
import unicodedata
from collections import Counter
from pathlib import Path
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
PROPOSED_VOCAB_PATH = ROOT / "tools" / "anchor_word_vocab_proposed.json"
ANCHOR_DICT_PATH = ROOT / "tools" / "anchor_translations.json"
EXISTING_VOCAB_PATH = ROOT / "tools" / "anchor_word_vocab.json"
BIBLE_JSON_PATH = ROOT / "app" / "src" / "main" / "assets" / "rv1960.json"
EXISTING_CROSSREFS_DB = ROOT / "app" / "src" / "main" / "assets" / "databases" / "cross_references.db"
TSK_URL = "https://raw.githubusercontent.com/CrossReferences-org/bible-cross-references/main/kjv/crossreferences_kjv.tsv"

# Mapeo de abreviaturas TSK a nombres en espanol Biblion (mismo que el builder)
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


def tsk_book_to_spanish(abbr: str) -> str:
    """Traduce abreviatura TSK a nombre Biblion."""
    return BOOK_MAP.get(abbr.strip(), abbr.strip())

# Stopwords en espanol (RV1960) - para filtrar al contar co-ocurrencias.
# Las palabras en esta lista NO se proponen como traducciones (son
# conectores, articulos, pronombres debiles) y ademas no se cuentan al
# extraer candidatos de co-ocurrencia.
STOPWORDS_ES = {
    "el", "la", "los", "las", "un", "una", "unos", "unas",
    "de", "del", "a", "al", "en", "por", "para", "con", "sin",
    "y", "o", "u", "pero", "sino", "porque", "pues",
    "que", "se", "le", "les", "lo", "le", "me", "te", "se",
    "mi", "mis", "tu", "tus", "su", "sus",
    "este", "esta", "estos", "estas", "ese", "esa", "esos", "esas",
    "aquel", "aquella", "aquellos", "aquellas",
    "no", "si", "muy", "tan", "tambien", "ya", "aun", "todavia",
    "no", "si",
    "como", "cuando", "donde", "cuando", "mientras",
    "sobre", "entre", "hacia", "desde", "hasta", "bajo",
    "tras", "mediante", "segun",
    "ha", "han", "he", "has", "habia", "habian", "fue", "fueron",
    "ser", "estar", "ser", "estaba", "estuvo", "seran", "estaran",
    "es", "son", "era", "eran", "sido", "siendo",
    "todo", "toda", "todos", "todas", "mucho", "mucha", "muchos", "muchas",
    "poco", "poca", "pocos", "pocas",
    "algun", "alguna", "algunos", "algunas", "ningun", "ninguna",
    "otro", "otra", "otros", "otras", "mismo", "misma", "mismos", "mismas",
    "yo", "tu", "el", "ella", "nosotros", "vosotros", "ellos", "ellas",
    "este", "ese", "aquel", "quien", "quienes", "cual", "cuales",
}


def normalize_book_name(name: str) -> str:
    lowered = name.lower().strip()
    normalized = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", "", without_accents)


def load_rv1960_verses() -> dict[tuple[str, int, int], str]:
    """Carga el texto espanol (rv1960) indexado por (libro, capitulo, versiculo)."""
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
            for verse_key, verse_text in verses_in_chapter.items():
                try:
                    verse = int(verse_key)
                except (TypeError, ValueError):
                    continue
                verses[(book_name, chapter, verse)] = str(verse_text)
    return verses


def tokenize_en(text: str) -> list[str]:
    """Tokeniza un texto en ingles: solo palabras con letras."""
    return re.findall(r"[A-Za-z']+", text)


def tokenize_es(text: str) -> list[str]:
    """Tokeniza un texto en espanol: solo palabras con letras (incluye acentos)."""
    return re.findall(r"[A-Za-zÁáÉéÍíÓóÚúÑñÜü]+", text)


def download_tsk() -> list[dict]:
    """Descarga el TSV de TSK y retorna filas como dicts."""
    print(f"  Descargando TSV desde: {TSK_URL}")
    req = Request(TSK_URL, headers={"User-Agent": "Biblion-Builder/1.0"})
    with urlopen(req, timeout=60) as response:
        content = response.read().decode("utf-8")
    reader = csv.reader(io.StringIO(content), delimiter="\t")
    next(reader)  # skip header
    rows = []
    for row in reader:
        if len(row) < 5:
            continue
        try:
            book = row[0].strip()
            chapter = int(row[1].strip())
            verse = int(row[2].strip())
            anchor = row[3].strip()
        except (ValueError, IndexError):
            continue
        if not anchor:
            continue
        rows.append({
            "book": book,
            "chapter": chapter,
            "verse": verse,
            "anchor": anchor
        })
    return rows


def build_alignment() -> tuple[Counter, Counter, Counter]:
    """
    Construye matrices de co-ocurrencia KJV->RV1960 y conteos por palabra.

    Para cada versiculo donde TSK tiene anchor y RV1960 tiene texto,
    cuenta que palabras espanol aparecen en el mismo versiculo que
    cada palabra ingles del anchor.

    Returns:
      cooccurrence: Counter((en_word, es_word) -> count)
      en_counts: Counter(en_word -> count)
      es_counts: Counter(es_word -> count)
    """
    print("  Cargando rv1960.json...")
    rv_verses = load_rv1960_verses()
    print(f"  {len(rv_verses)} versiculos RV1960 cargados")

    print("  Descargando TSK...")
    tsk_rows = download_tsk()
    print(f"  {len(tsk_rows)} anchors TSK cargados")

    print("  Construyendo alineamiento...")
    cooccurrence: Counter = Counter()
    en_counts: Counter = Counter()
    es_counts: Counter = Counter()

    # Stopwords en ingles biblico (mismo conjunto que build_crossrefs_sqlite)
    stopwords_en = {
        "the", "a", "an", "and", "or", "but", "if", "when", "because",
        "of", "to", "in", "on", "by", "with", "is", "was", "are", "were",
        "be", "been", "has", "have", "had", "do", "does", "did", "shall",
        "will", "would", "should", "may", "might", "this", "that", "these",
        "those", "which", "who", "whom", "whose", "what", "where", "when",
        "why", "how", "not", "all", "any", "some", "many", "more", "most",
        "such", "same", "other", "very", "also", "even", "still", "yet",
        "he", "she", "it", "they", "them", "his", "her", "we", "us",
        "i", "me", "my", "you", "your", "thou", "thee", "thy", "ye",
    }

    matched = 0
    for row in tsk_rows:
        anchor = row["anchor"]
        en_words = [w for w in tokenize_en(anchor) if w]
        if not en_words:
            continue

        # Traducir abreviatura TSK a nombre Biblion
        book_name = tsk_book_to_spanish(row["book"])
        verse_text = rv_verses.get((book_name, row["chapter"], row["verse"]))
        if not verse_text:
            continue

        matched += 1
        es_words = [w for w in tokenize_es(verse_text) if w]

        # Contar co-ocurrencias (filtrar stopwords en ambos lados)
        en_set = set(
            w.lower() for w in en_words
            if len(w) >= 2 and w.lower() not in stopwords_en
        )
        es_set = set(
            w.lower() for w in es_words
            if len(w) >= 2 and w.lower() not in STOPWORDS_ES
        )

        for w in en_set:
            en_counts[w] += 1
        for w in es_set:
            es_counts[w] += 1
        for en_w in en_set:
            for es_w in es_set:
                cooccurrence[(en_w, es_w)] += 1

    print(f"  {matched} versiculos con match entre TSK y RV1960")
    return cooccurrence, en_counts, es_counts


def propose_vocabulary(
    cooccurrence: Counter,
    en_counts: Counter,
    es_counts: Counter,
    min_en_count: int = 50,
    min_score_ratio: float = 0.3,
) -> dict[str, str]:
    """
    Propone mapeos KJV -> RV1960 basados en co-ocurrencia.

    Para cada palabra inglesa con >= min_en_count ocurrencias:
      - Encuentra la palabra espanola con mayor co-ocurrencia
      - El score es (co-ocurrencia / en_count)
      - Acepta si score >= min_score_ratio
    """
    proposals: dict[str, str] = {}
    for en_w, en_count in en_counts.items():
        if en_count < min_en_count:
            continue
        # Top 5 candidatos en espanol para esta palabra inglesa
        candidates = []
        for (key_en, key_es), co_count in cooccurrence.items():
            if key_en != en_w:
                continue
            score = co_count / en_count
            candidates.append((key_es, score, co_count))

        if not candidates:
            continue
        candidates.sort(key=lambda x: -x[1])

        best_es, best_score, best_count = candidates[0]
        if best_score >= min_score_ratio and best_count >= 3:
            proposals[en_w] = best_es

    return proposals


def main():
    print("=" * 60)
    print("Biblion: Extractor data-driven de vocabulario KJV->RV1960")
    print("=" * 60)

    cooccurrence, en_counts, es_counts = build_alignment()

    print("\n  Propuestas por co-ocurrencia...")
    proposals = propose_vocabulary(cooccurrence, en_counts, es_counts)

    # Combinar con vocabulario existente (las propuestas no pisan palabras ya existentes)
    existing = {}
    if EXISTING_VOCAB_PATH.exists():
        with EXISTING_VOCAB_PATH.open(encoding="utf-8") as f:
            existing = {k.lower(): v for k, v in json.load(f).items()}

    new_only = {k: v for k, v in proposals.items() if k not in existing}

    print(f"  Propuestas nuevas: {len(new_only)}")
    print(f"  Propuestas totales (incluyendo existentes): {len(proposals)}")

    # Guardar propuestas
    PROPOSED_VOCAB_PATH.write_text(
        json.dumps(proposals, indent=2, ensure_ascii=False),
        encoding="utf-8"
    )
    print(f"  Guardado en: {PROPOSED_VOCAB_PATH}")

    # Mostrar las top 30
    print("\nTop 30 nuevas propuestas:")
    for i, (en, es) in enumerate(list(new_only.items())[:30]):
        en_count = en_counts.get(en, 0)
        co_count = cooccurrence.get((en, es), 0)
        score = co_count / en_count if en_count else 0
        print(f"  {i+1:3d}. {en!r:20s} -> {es!r:20s}  "
              f"(en_count={en_count}, co={co_count}, score={score:.2f})")


if __name__ == "__main__":
    main()

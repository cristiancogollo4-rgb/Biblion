#!/usr/bin/env python3
"""
QC: toma N muestras aleatorias de anchors traducidos en cross_references.db
y las muestra junto al texto RV1960 real del versiculo ancla.

El objetivo es revision humana. Para cada muestra se imprime:
  - Referencia (libro, capitulo, versiculo)
  - Anchor original (ingles/KJV)
  - Traduccion generada (anchor_es)
  - Texto RV1960 del versiculo (para inspeccion visual)

Uso:
    python tools/qc_anchor_translations.py --samples 50
"""
from __future__ import annotations

import argparse
import json
import random
import sqlite3
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB_PATH = ROOT / "app" / "src" / "main" / "assets" / "databases" / "cross_references.db"
BIBLE_JSON_PATH = ROOT / "app" / "src" / "main" / "assets" / "rv1960.json"


def strip_accents(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text)
    return "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn").lower()


def load_rv1960() -> dict[tuple[str, int, int], str]:
    if not BIBLE_JSON_PATH.exists():
        return {}
    with BIBLE_JSON_PATH.open(encoding="utf-8") as f:
        bible = json.load(f)
    verses: dict[tuple[str, int, int], str] = {}
    for book_name, chapters in bible.items():
        if not isinstance(chapters, dict):
            continue
        # Normalizar nombre del libro igual que build_bible_sqlite.py:
        # lowercase + sin acentos + sin espacios
        normalized_book = strip_accents(book_name).replace(" ", "")
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
                verses[(normalized_book, chapter, verse)] = str(verse_text)
    return verses


def normalize_db_book(book: str) -> str:
    """Normaliza un nombre de libro del DB al mismo formato que la clave del
    diccionario rv1960 (lowercase + sin acentos + sin espacios)."""
    return strip_accents(book).replace(" ", "").lower()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--samples", type=int, default=50)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    if not DB_PATH.exists():
        print(f"  No existe {DB_PATH}")
        return

    conn = sqlite3.connect(DB_PATH)
    cur = conn.execute(
        "SELECT source_book, source_chapter, source_verse, anchor, anchor_es "
        "FROM cross_references "
        "WHERE anchor_es != '' "
        "ORDER BY RANDOM()"
        if False else  # ordenamiento custom abajo
        "SELECT source_book, source_chapter, source_verse, anchor, anchor_es "
        "FROM cross_references "
        "WHERE anchor_es != '' AND length(anchor_es) <= 30"
    )
    # Cargar todo en memoria
    rows = list(cur)
    conn.close()

    rng = random.Random(args.seed)
    rng.shuffle(rows)
    samples = rows[:args.samples]

    rv_verses = load_rv1960()
    print(f"Cargadas {len(rv_verses)} versiculos RV1960 para comparacion")
    print(f"Muestras a inspeccionar: {len(samples)}\n")

    bad = 0
    for i, (book, ch, vs, anchor_en, anchor_es) in enumerate(samples, 1):
        book_norm = normalize_db_book(book)
        verse_text = rv_verses.get((book_norm, ch, vs))
        print(f"--- Muestra {i}/{len(samples)} ---")
        print(f"  Ref:     {book} {ch}:{vs}")
        print(f"  Anchor:  {anchor_en!r}")
        print(f"  Traducc: {anchor_es!r}")
        if verse_text:
            print(f"  RV1960:  {verse_text}")
            # Buscar si la traduccion aparece como substring del verso
            verse_norm = strip_accents(verse_text)
            es_norm = strip_accents(anchor_es)
            if es_norm in verse_norm:
                print(f"  Match:   SI (la traduccion aparece en el verso)")
            else:
                print(f"  Match:   NO (la traduccion NO aparece literal en el verso)")
                bad += 1
        else:
            print(f"  RV1960:  (versiculo no encontrado)")
            bad += 1
        print()

    print("=" * 60)
    print(f"Resumen: {bad}/{len(samples)} muestras sin match literal ({100*bad/len(samples):.1f}%)")
    print("(Las que no matchean pueden ser traducciones validas por sinonimia,")
    print("o errores de traduccion. Revisar manualmente.)")


if __name__ == "__main__":
    main()

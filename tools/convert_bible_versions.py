#!/usr/bin/env python3
"""
Convierte versiones bíblicas con formato "nuevo" al formato canónico usado por Biblion.

Entrada esperada (resumen):
[
  {
    "libro": "genesis",
    "capitulos": [
      {
        "capitulo": 1,
        "versiculos": {
          "vers": [
            {"number": 1, "verse": "...", "study": "La Creación"},
            ...
          ]
        }
      }
    ]
  }
]

Salida:
1) <version>.json (formato actual de la app)
{
  "Genesis": {
    "1": {
      "1": "En el principio..."
    }
  }
}

2) <version>_titles.json (títulos opcionales por versículo)
{
  "Genesis": {
    "1": {
      "1": "La Creación"
    }
  }
}
"""

from __future__ import annotations

import argparse
import json
import re
import unicodedata
from pathlib import Path
from typing import Any


BOOK_NORMALIZATION_MAP = {
    "genesis": "Genesis",
    "exodo": "Exodo",
    "levitico": "Levitico",
    "numeros": "Numeros",
    "deuteronomio": "Deuteronomio",
    "josue": "Josue",
    "jueces": "Jueces",
    "rut": "Rut",
    "1 samuel": "1 Samuel",
    "2 samuel": "2 Samuel",
    "1 reyes": "1 Reyes",
    "2 reyes": "2 Reyes",
    "1 cronicas": "1 Cronicas",
    "2 cronicas": "2 Cronicas",
    "esdras": "Esdras",
    "nehemias": "Nehemias",
    "ester": "Ester",
    "job": "Job",
    "salmos": "Salmos",
    "proverbios": "Proverbios",
    "eclesiastes": "Eclesiastes",
    "cantares": "Cantares",
    "isaias": "Isaias",
    "jeremias": "Jeremias",
    "lamentaciones": "Lamentaciones",
    "ezequiel": "Ezequiel",
    "daniel": "Daniel",
    "oseas": "Oseas",
    "joel": "Joel",
    "amos": "Amos",
    "abdias": "Abdias",
    "jonas": "Jonas",
    "miqueas": "Miqueas",
    "nahum": "Nahum",
    "habacuc": "Habacuc",
    "sofonias": "Sofonias",
    "hageo": "Hageo",
    "zacarias": "Zacarias",
    "malaquias": "Malaquias",
    "mateo": "Mateo",
    "marcos": "Marcos",
    "lucas": "Lucas",
    "juan": "Juan",
    "hechos": "Hechos",
    "romanos": "Romanos",
    "1 corintios": "1 Corintios",
    "2 corintios": "2 Corintios",
    "galatas": "Galatas",
    "efesios": "Efesios",
    "filipenses": "Filipenses",
    "colosenses": "Colosenses",
    "1 tesalonicenses": "1 Tesalonicenses",
    "2 tesalonicenses": "2 Tesalonicenses",
    "1 timoteo": "1 Timoteo",
    "2 timoteo": "2 Timoteo",
    "tito": "Tito",
    "filemon": "Filemon",
    "hebreos": "Hebreos",
    "santiago": "Santiago",
    "1 pedro": "1 Pedro",
    "2 pedro": "2 Pedro",
    "1 juan": "1 Juan",
    "2 juan": "2 Juan",
    "3 juan": "3 Juan",
    "judas": "Judas",
    "apocalipsis": "Apocalipsis",
}


def slugify(text: str) -> str:
    lowered = text.lower().strip()
    normalized = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    compact = re.sub(r"\s+", " ", without_accents).strip()
    return compact


def normalize_book_name(raw_name: str) -> str:
    key = slugify(raw_name)
    return BOOK_NORMALIZATION_MAP.get(key, raw_name.strip().title())


def convert_payload(data: list[dict[str, Any]]) -> tuple[dict[str, Any], dict[str, Any], dict[str, int]]:
    canonical: dict[str, Any] = {}
    titles: dict[str, Any] = {}
    report = {"books": 0, "chapters": 0, "verses": 0, "titles": 0}

    for book in data:
        raw_book_name = str(book.get("libro", "")).strip()
        if not raw_book_name:
            continue

        book_name = normalize_book_name(raw_book_name)
        canonical.setdefault(book_name, {})
        titles.setdefault(book_name, {})
        report["books"] += 1

        for chapter in book.get("capitulos", []):
            chapter_number = chapter.get("capitulo")
            if chapter_number is None:
                continue
            chapter_key = str(chapter_number)

            canonical[book_name].setdefault(chapter_key, {})
            titles[book_name].setdefault(chapter_key, {})
            report["chapters"] += 1

            verses_data = (
                chapter.get("versiculos", {}).get("vers", [])
                if isinstance(chapter.get("versiculos"), dict)
                else []
            )
            for verse_entry in verses_data:
                verse_number = verse_entry.get("number")
                verse_text = str(verse_entry.get("verse", "")).strip()
                if verse_number is None or not verse_text:
                    continue

                verse_key = str(verse_number)
                canonical[book_name][chapter_key][verse_key] = verse_text
                report["verses"] += 1

                study_title = str(verse_entry.get("study", "")).strip()
                if study_title:
                    titles[book_name][chapter_key][verse_key] = study_title
                    report["titles"] += 1

            if not titles[book_name][chapter_key]:
                titles[book_name].pop(chapter_key, None)

        if not titles[book_name]:
            titles.pop(book_name, None)

    return canonical, titles, report


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convierte JSON de versiones bíblicas al formato canónico de Biblion."
    )
    parser.add_argument(
        "--input",
        required=True,
        help="Ruta del JSON de entrada (formato nuevo).",
    )
    parser.add_argument(
        "--output-dir",
        default="app/src/main/assets",
        help="Directorio de salida para los JSON convertidos.",
    )
    parser.add_argument(
        "--version-key",
        required=True,
        help="Nombre base de salida (ej: nvi, dhh, pdt).",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    input_path = Path(args.input)
    output_dir = Path(args.output_dir)
    version_key = args.version_key.strip().lower()

    if not input_path.exists():
        raise SystemExit(f"No existe archivo de entrada: {input_path}")
    if not version_key:
        raise SystemExit("Debes indicar --version-key (ej: nvi)")

    data = json.loads(input_path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise SystemExit("El JSON de entrada debe ser un arreglo de libros.")

    canonical, titles, report = convert_payload(data)
    output_dir.mkdir(parents=True, exist_ok=True)

    canonical_path = output_dir / f"{version_key}.json"
    titles_path = output_dir / f"{version_key}_titles.json"

    canonical_path.write_text(
        json.dumps(canonical, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    titles_path.write_text(
        json.dumps(titles, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(
        f"Conversión completada [{version_key}] -> "
        f"libros={report['books']} capítulos={report['chapters']} "
        f"versículos={report['verses']} títulos={report['titles']}"
    )
    print(f"JSON principal: {canonical_path}")
    print(f"JSON títulos:   {titles_path}")


if __name__ == "__main__":
    main()

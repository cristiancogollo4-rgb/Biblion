#!/usr/bin/env python3
"""
Builds Biblion's read-only Bible SQLite asset from the canonical JSON files.

Output:
app/src/main/assets/databases/bible_content.db
"""

from __future__ import annotations

import json
import re
import sqlite3
import unicodedata
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS_DIR = ROOT / "app" / "src" / "main" / "assets"
OUTPUT_DB = ASSETS_DIR / "databases" / "bible_content.db"


def normalize_book_name(text: str) -> str:
    lowered = text.lower().replace(" ", "")
    normalized = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", "", without_accents)


def canonical_book_score(book_name: str) -> tuple[int, int, int]:
    has_lowercase = any(ch.islower() for ch in book_name)
    has_space = " " in book_name
    is_upper = book_name.isupper()
    return (1 if has_lowercase else 0, 1 if has_space else 0, 0 if is_upper else 1)


def merge_duplicate_books(bible: dict) -> dict:
    merged: dict[str, dict] = {}
    canonical_names: dict[str, str] = {}

    for book_name, chapters in bible.items():
        if not isinstance(chapters, dict):
            continue

        normalized = normalize_book_name(book_name)
        current_name = canonical_names.get(normalized)
        if current_name is None or canonical_book_score(book_name) > canonical_book_score(current_name):
            canonical_names[normalized] = book_name

        target_book = merged.setdefault(normalized, {})
        for chapter_key, verses in chapters.items():
            if not isinstance(verses, dict):
                continue
            target_chapter = target_book.setdefault(chapter_key, {})
            for verse_key, verse_text in verses.items():
                target_chapter.setdefault(verse_key, verse_text)

    return {
        canonical_names[normalized]: chapters
        for normalized, chapters in merged.items()
    }


def version_files() -> list[Path]:
    return sorted(
        path
        for path in ASSETS_DIR.glob("*.json")
        if not path.name.endswith("_titles.json")
    )


def create_schema(connection: sqlite3.Connection) -> None:
    connection.executescript(
        """
        PRAGMA user_version = 2;

        DROP TABLE IF EXISTS bible_verses;
        DROP TABLE IF EXISTS bible_titles;

        CREATE TABLE IF NOT EXISTS bible_verses (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            versionKey TEXT NOT NULL,
            bookName TEXT NOT NULL,
            normalizedBookName TEXT NOT NULL,
            bookIndex INTEGER NOT NULL,
            chapter INTEGER NOT NULL,
            verse INTEGER NOT NULL,
            text TEXT NOT NULL
        );

        CREATE UNIQUE INDEX IF NOT EXISTS index_bible_verses_versionKey_normalizedBookName_chapter_verse
        ON bible_verses(versionKey, normalizedBookName, chapter, verse);

        CREATE INDEX IF NOT EXISTS index_bible_verses_versionKey_bookIndex_chapter
        ON bible_verses(versionKey, bookIndex, chapter);

        CREATE INDEX IF NOT EXISTS index_bible_verses_versionKey_text
        ON bible_verses(versionKey, text);

        CREATE TABLE IF NOT EXISTS bible_titles (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            versionKey TEXT NOT NULL,
            bookName TEXT NOT NULL,
            normalizedBookName TEXT NOT NULL,
            chapter INTEGER NOT NULL,
            verse INTEGER NOT NULL,
            title TEXT NOT NULL
        );

        CREATE UNIQUE INDEX IF NOT EXISTS index_bible_titles_versionKey_normalizedBookName_chapter_verse
        ON bible_titles(versionKey, normalizedBookName, chapter, verse);
        """
    )


def insert_version(connection: sqlite3.Connection, version_path: Path) -> tuple[int, int]:
    version_key = version_path.stem.lower()
    titles_path = version_path.with_name(f"{version_key}_titles.json")

    bible = merge_duplicate_books(json.loads(version_path.read_text(encoding="utf-8")))
    titles = (
        json.loads(titles_path.read_text(encoding="utf-8"))
        if titles_path.exists()
        else {}
    )

    verse_rows: list[tuple[str, str, str, int, int, int, str]] = []
    title_rows: list[tuple[str, str, str, int, int, str]] = []

    for book_index, (book_name, chapters) in enumerate(bible.items()):
        normalized_book_name = normalize_book_name(book_name)
        for chapter_key, verses in chapters.items():
            try:
                chapter = int(chapter_key)
            except ValueError:
                continue
            for verse_key, verse_text in verses.items():
                try:
                    verse = int(verse_key)
                except ValueError:
                    continue
                verse_rows.append(
                    (
                        version_key,
                        book_name,
                        normalized_book_name,
                        book_index,
                        chapter,
                        verse,
                        str(verse_text),
                    )
                )

    for book_name, chapters in titles.items():
        normalized_book_name = normalize_book_name(book_name)
        for chapter_key, chapter_titles in chapters.items():
            try:
                chapter = int(chapter_key)
            except ValueError:
                continue
            for verse_key, title in chapter_titles.items():
                clean_title = str(title).strip()
                if not clean_title:
                    continue
                try:
                    verse = int(verse_key)
                except ValueError:
                    continue
                title_rows.append(
                    (
                        version_key,
                        book_name,
                        normalized_book_name,
                        chapter,
                        verse,
                        clean_title,
                    )
                )

    connection.executemany(
        """
        INSERT INTO bible_verses (
            versionKey, bookName, normalizedBookName, bookIndex, chapter, verse, text
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        verse_rows,
    )
    connection.executemany(
        """
        INSERT INTO bible_titles (
            versionKey, bookName, normalizedBookName, chapter, verse, title
        ) VALUES (?, ?, ?, ?, ?, ?)
        """,
        title_rows,
    )

    return len(verse_rows), len(title_rows)


def main() -> None:
    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()

    connection = sqlite3.connect(OUTPUT_DB)
    try:
        create_schema(connection)
        total_verses = 0
        total_titles = 0
        for path in version_files():
            verses, titles = insert_version(connection, path)
            total_verses += verses
            total_titles += titles
            print(f"{path.stem}: verses={verses} titles={titles}")
        connection.commit()
        connection.execute("VACUUM")
    finally:
        connection.close()

    size_mb = OUTPUT_DB.stat().st_size / 1024 / 1024
    print(f"Created {OUTPUT_DB} ({size_mb:.2f} MB)")
    print(f"Total verses={total_verses} titles={total_titles}")


if __name__ == "__main__":
    main()

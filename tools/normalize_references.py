#!/usr/bin/env python3
"""
Normalizes Bible references in the translated dictionary from English to Spanish.

Biblion's Bible database stores book names in Spanish (e.g., "Genesis", "Exodo").
This script converts all English references (both in arrays and inline text)
to Spanish so Biblion can resolve them correctly.

Handles:
  - Full book names:  "Genesis 1:1"   → "Genesis 1:1"
  - Abbreviated names: "Ex. 32:11"     → "Exodo 32:11"
  - Ranges:            "1 Sam 1:1-15"  → "1 Samuel 1:1-15"
  - Inline refs:       "in Ex. 3:6..." → "in Exodo 3:6..."
  - Complex patterns:  "1 Kgs 2:3"     → "1 Reyes 2:3"
  - "Compare" refs:    "Cp. Matt 5"    → "Cp. Mateo 5"
"""

from __future__ import annotations

import json
import re
import sqlite3
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INPUT_FILE = ROOT / "tools" / "eastons_dictionary_es.json"
OUTPUT_DB = ROOT / "app" / "src" / "main" / "assets" / "databases" / "dictionary.db"

# --- Complete English -> Spanish book name mapping ---
# Format: "lowercase_english_key" -> "Spanish_display_name"
# Includes both full names, abbreviated forms, and common Easton's conventions

BOOK_MAP_EN_TO_ES = {
    # === Old Testament ===
    # Pentateuch
    "genesis": "Genesis",
    "gen": "Genesis",
    "gn": "Genesis",
    "exodus": "Exodo",
    "exod": "Exodo",
    "ex": "Exodo",
    "exo": "Exodo",
    "leviticus": "Levitico",
    "lev": "Levitico",
    "lv": "Levitico",
    "levit": "Levitico",
    "numbers": "Numeros",
    "num": "Numeros",
    "numb": "Numeros",
    "nm": "Numeros",
    "deuteronomy": "Deuteronomio",
    "deut": "Deuteronomio",
    "dt": "Deuteronomio",
    "deu": "Deuteronomio",
    # Historical books
    "joshua": "Josue",
    "josh": "Josue",
    "jos": "Josue",
    "js": "Josue",
    "judges": "Jueces",
    "judg": "Jueces",
    "jud": "Jueces",
    "jdg": "Jueces",
    "ju": "Jueces",
    "ruth": "Rut",
    "ru": "Rut",
    "1 samuel": "1 Samuel",
    "1samuel": "1 Samuel",
    "1 sam": "1 Samuel",
    "1sam": "1 Samuel",
    "i sam": "1 Samuel",
    "isam": "1 Samuel",
    "1 s": "1 Samuel",
    "2 samuel": "2 Samuel",
    "2samuel": "2 Samuel",
    "2 sam": "2 Samuel",
    "2sam": "2 Samuel",
    "ii sam": "2 Samuel",
    "iisam": "2 Samuel",
    "2 s": "2 Samuel",
    "1 kings": "1 Reyes",
    "1kings": "1 Reyes",
    "1 kgs": "1 Reyes",
    "1kgs": "1 Reyes",
    "i kings": "1 Reyes",
    "ikings": "1 Reyes",
    "1 ki": "1 Reyes",
    "1 k": "1 Reyes",
    "2 kings": "2 Reyes",
    "2kings": "2 Reyes",
    "2 kgs": "2 Reyes",
    "2kgs": "2 Reyes",
    "ii kings": "2 Reyes",
    "iikings": "2 Reyes",
    "2 ki": "2 Reyes",
    "2 k": "2 Reyes",
    "1 chronicles": "1 Cronicas",
    "1chronicles": "1 Cronicas",
    "1 chr": "1 Cronicas",
    "1chr": "1 Cronicas",
    "i chr": "1 Cronicas",
    "ichr": "1 Cronicas",
    "1 chron": "1 Cronicas",
    "1 ch": "1 Cronicas",
    "2 chronicles": "2 Cronicas",
    "2chronicles": "2 Cronicas",
    "2 chr": "2 Cronicas",
    "2chr": "2 Cronicas",
    "ii chr": "2 Cronicas",
    "iichr": "2 Cronicas",
    "2 chron": "2 Cronicas",
    "2 ch": "2 Cronicas",
    "ezra": "Esdras",
    "ezr": "Esdras",
    "ez": "Esdras",
    "nehemiah": "Nehemias",
    "neh": "Nehemias",
    "ne": "Nehemias",
    "esther": "Ester",
    "esth": "Ester",
    "est": "Ester",
    "es": "Ester",
    "job": "Job",
    "jb": "Job",
    # Psalms / Wisdom
    "psalms": "Salmos",
    "psalm": "Salmos",
    "ps": "Salmos",
    "psa": "Salmos",
    "pss": "Salmos",
    "proverbs": "Proverbios",
    "prov": "Proverbios",
    "pr": "Proverbios",
    "pro": "Proverbios",
    "ecclesiastes": "Eclesiastes",
    "eccl": "Eclesiastes",
    "ecc": "Eclesiastes",
    "ec": "Eclesiastes",
    "song of solomon": "Cantares",
    "songofsolomon": "Cantares",
    "song": "Cantares",
    "cant": "Cantares",
    "canticles": "Cantares",
    "song of songs": "Cantares",
    "song of sol": "Cantares",
    "ss": "Cantares",
    "sos": "Cantares",
    "c": "Cantares",
    # Major Prophets
    "isaiah": "Isaias",
    "isa": "Isaias",
    "is": "Isaias",
    "jeremiah": "Jeremias",
    "jer": "Jeremias",
    "je": "Jeremias",
    "jr": "Jeremias",
    "lamentations": "Lamentaciones",
    "lam": "Lamentaciones",
    "la": "Lamentaciones",
    "lm": "Lamentaciones",
    "ezekiel": "Ezequiel",
    "ezek": "Ezequiel",
    "eze": "Ezequiel",
    "ezk": "Ezequiel",
    "daniel": "Daniel",
    "dan": "Daniel",
    "da": "Daniel",
    "dn": "Daniel",
    # Minor Prophets
    "hosea": "Oseas",
    "hos": "Oseas",
    "ho": "Oseas",
    "joel": "Joel",
    "jl": "Joel",
    "joe": "Joel",
    "amos": "Amos",
    "am": "Amos",
    "obadiah": "Abdias",
    "obad": "Abdias",
    "ob": "Abdias",
    "oba": "Abdias",
    "jonah": "Jonas",
    "jon": "Jonas",
    "jnh": "Jonas",
    "micah": "Miqueas",
    "mic": "Miqueas",
    "mi": "Miqueas",
    "nahum": "Nahum",
    "nah": "Nahum",
    "na": "Nahum",
    "habakkuk": "Habacuc",
    "hab": "Habacuc",
    "hb": "Habacuc",
    "zephaniah": "Sofonias",
    "zeph": "Sofonias",
    "zep": "Sofonias",
    "zp": "Sofonias",
    "haggai": "Hageo",
    "hag": "Hageo",
    "hg": "Hageo",
    "zechariah": "Zacarias",
    "zech": "Zacarias",
    "zec": "Zacarias",
    "zc": "Zacarias",
    "malachi": "Malaquias",
    "mal": "Malaquias",
    "ml": "Malaquias",

    # === New Testament ===
    # Gospels
    "matthew": "Mateo",
    "matt": "Mateo",
    "mt": "Mateo",
    "mark": "Marcos",
    "mk": "Marcos",
    "mr": "Marcos",
    "mar": "Marcos",
    "luke": "Lucas",
    "lk": "Lucas",
    "lc": "Lucas",
    "lu": "Lucas",
    "john": "Juan",
    "jn": "Juan",
    "jo": "Juan",
    "joh": "Juan",
    # Acts
    "acts": "Hechos",
    "ac": "Hechos",
    "act": "Hechos",
    # Pauline Epistles
    "romans": "Romanos",
    "rom": "Romanos",
    "ro": "Romanos",
    "rm": "Romanos",
    "1 corinthians": "1 Corintios",
    "1corinthians": "1 Corintios",
    "1 cor": "1 Corintios",
    "1cor": "1 Corintios",
    "i cor": "1 Corintios",
    "icor": "1 Corintios",
    "1 co": "1 Corintios",
    "2 corinthians": "2 Corintios",
    "2corinthians": "2 Corintios",
    "2 cor": "2 Corintios",
    "2cor": "2 Corintios",
    "ii cor": "2 Corintios",
    "iicor": "2 Corintios",
    "2 co": "2 Corintios",
    "galatians": "Galatas",
    "gal": "Galatas",
    "ga": "Galatas",
    "ephesians": "Efesios",
    "eph": "Efesios",
    "ep": "Efesios",
    "philippians": "Filipenses",
    "phil": "Filipenses",
    "php": "Filipenses",
    "ph": "Filipenses",
    "colossians": "Colosenses",
    "col": "Colosenses",
    "co": "Colosenses",
    "1 thessalonians": "1 Tesalonicenses",
    "1thessalonians": "1 Tesalonicenses",
    "1 thess": "1 Tesalonicenses",
    "1thess": "1 Tesalonicenses",
    "i thess": "1 Tesalonicenses",
    "ithess": "1 Tesalonicenses",
    "1 th": "1 Tesalonicenses",
    "1 ts": "1 Tesalonicenses",
    "2 thessalonians": "2 Tesalonicenses",
    "2thessalonians": "2 Tesalonicenses",
    "2 thess": "2 Tesalonicenses",
    "2thess": "2 Tesalonicenses",
    "ii thess": "2 Tesalonicenses",
    "iithess": "2 Tesalonicenses",
    "2 th": "2 Tesalonicenses",
    "2 ts": "2 Tesalonicenses",
    "1 timothy": "1 Timoteo",
    "1timothy": "1 Timoteo",
    "1 tim": "1 Timoteo",
    "1tim": "1 Timoteo",
    "i tim": "1 Timoteo",
    "itim": "1 Timoteo",
    "1 ti": "1 Timoteo",
    "1 tm": "1 Timoteo",
    "2 timothy": "2 Timoteo",
    "2timothy": "2 Timoteo",
    "2 tim": "2 Timoteo",
    "2tim": "2 Timoteo",
    "ii tim": "2 Timoteo",
    "iitim": "2 Timoteo",
    "2 ti": "2 Timoteo",
    "2 tm": "2 Timoteo",
    "titus": "Tito",
    "tit": "Tito",
    "ti": "Tito",
    "tt": "Tito",
    "philemon": "Filemon",
    "philem": "Filemon",
    "phm": "Filemon",
    "plm": "Filemon",
    "flm": "Filemon",
    "hebrews": "Hebreos",
    "heb": "Hebreos",
    "he": "Hebreos",
    # General Epistles
    "james": "Santiago",
    "jas": "Santiago",
    "jam": "Santiago",
    "jm": "Santiago",
    "stg": "Santiago",
    "1 peter": "1 Pedro",
    "1peter": "1 Pedro",
    "1 pet": "1 Pedro",
    "1pet": "1 Pedro",
    "i pet": "1 Pedro",
    "ipet": "1 Pedro",
    "1 pe": "1 Pedro",
    "1 pt": "1 Pedro",
    "2 peter": "2 Pedro",
    "2peter": "2 Pedro",
    "2 pet": "2 Pedro",
    "2pet": "2 Pedro",
    "ii pet": "2 Pedro",
    "iipet": "2 Pedro",
    "2 pe": "2 Pedro",
    "2 pt": "2 Pedro",
    "1 john": "1 Juan",
    "1john": "1 Juan",
    "1 jn": "1 Juan",
    "1jn": "1 Juan",
    "i john": "1 Juan",
    "ijohn": "1 Juan",
    "i jn": "1 Juan",
    "1 jo": "1 Juan",
    "2 john": "2 Juan",
    "2john": "2 Juan",
    "2 jn": "2 Juan",
    "2jn": "2 Juan",
    "ii john": "2 Juan",
    "iijohn": "2 Juan",
    "3 john": "3 Juan",
    "3john": "3 Juan",
    "3 jn": "3 Juan",
    "3jn": "3 Juan",
    "iii john": "3 Juan",
    "iiijohn": "3 Juan",
    "jude": "Judas",
    "jud": "Judas",
    "jd": "Judas",
    # Apocalyptic
    "revelation": "Apocalipsis",
    "rev": "Apocalipsis",
    "re": "Apocalipsis",
    "rv": "Apocalipsis",
    "apoc": "Apocalipsis",
    "apocalypse": "Apocalipsis",

    # === Apocrypha / Deuterocanonical (found in Easton's) ===
    "tobit": "Tobias",
    "tob": "Tobias",
    "judith": "Judit",
    "jdt": "Judit",
    "wisdom": "Sabiduria",
    "wis": "Sabiduria",
    "sirach": "Eclesiastico",
    "sir": "Eclesiastico",
    "ecclus": "Eclesiastico",
    "baruch": "Baruc",
    "bar": "Baruc",
    "1 maccabees": "1 Macabeos",
    "1maccabees": "1 Macabeos",
    "1 macc": "1 Macabeos",
    "1macc": "1 Macabeos",
    "2 maccabees": "2 Macabeos",
    "2maccabees": "2 Macabeos",
    "2 macc": "2 Macabeos",
    "2macc": "2 Macabeos",
    "bel and the dragon": "Bel y el Dragon",
    "susanna": "Susana",
}

# Pattern to detect book references in text
# Captures: optional digit prefix (1, 2, 3, I, II, III), book name (with or without dots), chapter:verse
REF_PATTERN = re.compile(
    r'\b(?:(\d)\s+|(i|ii|iii)\s+)?'   # Optional number/roman numeral prefix
    r'([A-Za-z]+)'                      # Book name (with or without trailing dot)
    r'\.?\s+'                           # Optional dot + space
    r'(\d+):(\d+)'                      # chapter:verse
    r'(?:-(\d+))?'                      # optional -endverse
    r'\b',
    re.IGNORECASE
)

def normalize_book_key(book_name: str) -> str:
    """Convert a book name to the lookup key format."""
    key = book_name.strip().lower()
    key = re.sub(r'\s+', ' ', key)
    key = re.sub(r'\.$', '', key)
    return key

def map_book_to_spanish(english_book: str) -> str:
    """
    Maps an English book name to its Spanish equivalent.
    Tries: full name, with digit prefix, without digit prefix, abbreviated forms.
    Returns the Spanish name or the original if no mapping found.
    """
    original = english_book.strip()

    # Try exact match (lowercase, trimmed)
    key = normalize_book_key(original)
    if key in BOOK_MAP_EN_TO_ES:
        return BOOK_MAP_EN_TO_ES[key]

    # Try removing trailing period
    key_stripped = key.rstrip('.')
    if key_stripped in BOOK_MAP_EN_TO_ES:
        return BOOK_MAP_EN_TO_ES[key_stripped]

    # Try with spaces normalized (handle "1sam" vs "1 sam")
    if ' ' in key:
        key_nospace = key.replace(' ', '')
        if key_nospace in BOOK_MAP_EN_TO_ES:
            return BOOK_MAP_EN_TO_ES[key_nospace]

    # Try adding spaces if compact form (handle "1sam" -> "1 sam")
    if not ' ' in key and len(key) > 3 and key[0].isdigit():
        key_with_space = key[0] + ' ' + key[1:]
        if key_with_space in BOOK_MAP_EN_TO_ES:
            return BOOK_MAP_EN_TO_ES[key_with_space]

    # Try stripping the leading number entirely (for "1chronicles" -> "chronicles")
    if key[0].isdigit() and len(key) > 1:
        key_no_num = key[1:].lstrip()
        if key_no_num in BOOK_MAP_EN_TO_ES:
            if len(original) >= 2 and original[0].isdigit():
                return original[0] + ' ' + BOOK_MAP_EN_TO_ES[key_no_num]

    return original  # Keep original if no mapping found


def normalize_reference(ref: str) -> str:
    """
    Normalizes a single Bible reference string from English to Spanish.
    Handles patterns like:
      - "Genesis 1:1"       -> "Genesis 1:1"
      - "Ex. 32:11"         -> "Exodo 32:11"
      - "1 Sam 1:15"        -> "1 Samuel 1:15"
      - "2 Chr 32:20"       -> "2 Cronicas 32:20"
      - "Ps. 73:28"         -> "Salmos 73:28"
      - "Gen 1:1-3"         -> "Genesis 1:1-3"
      - "Eph 2:8-9"         -> "Efesios 2:8-9"
      - "1 Cor. 15:1-4"     -> "1 Corintios 15:1-4"
      - "Cp. Matt 5:3"      -> "Cp. Mateo 5:3"
    """
    ref = ref.strip()
    if not ref:
        return ref

    # Standard reference: "Book Name chapter:verse[-verseEnd]"
    match = REF_PATTERN.match(ref)
    if match:
        num_prefix = match.group(1) or match.group(2) or ""
        book = match.group(3)
        chapter = match.group(4)
        verse_start = match.group(5)
        verse_end = match.group(6)

        # Build the full book name
        if num_prefix:
            full_book = f"{num_prefix.upper()} {book}"
        else:
            full_book = book

        spanish_book = map_book_to_spanish(full_book)

        if verse_end:
            return f"{spanish_book} {chapter}:{verse_start}-{verse_end}"
        return f"{spanish_book} {chapter}:{verse_start}"

    # Handle "Cp. Book ch:vs" (compare references) pattern
    cp_match = re.match(r'(cp\.?|comp\.?|see|ver|cf\.?)\s+(.+)', ref, re.IGNORECASE)
    if cp_match:
        prefix = cp_match.group(1)
        rest = normalize_reference(cp_match.group(2))
        return f"{prefix} {rest}"

    return ref


def normalize_inline_references(text: str) -> str:
    """
    Finds and normalizes all inline Bible references in a definition text.
    Uses the same REF_PATTERN to find and replace English book names with Spanish.
    """
    def replace_ref(match):
        num_prefix = match.group(1) or match.group(2) or ""
        book = match.group(3)
        chapter = match.group(4)
        verse_start = match.group(5)
        verse_end = match.group(6)

        if num_prefix:
            full_book = f"{num_prefix.upper()} {book}"
        else:
            full_book = book

        spanish_book = map_book_to_spanish(full_book)

        if verse_end:
            return f"{spanish_book} {chapter}:{verse_start}-{verse_end}"
        return f"{spanish_book} {chapter}:{verse_start}"

    return REF_PATTERN.sub(replace_ref, text)


def normalize_term(term: str) -> str:
    """Normalize a term for database storage."""
    lowered = term.lower().strip()
    normalized = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_accents)


def main():
    print("=" * 60)
    print("Normalizing Bible References: English -> Spanish")
    print("=" * 60)

    # Step 1: Load dictionary
    print("\n[1/4] Loading Spanish dictionary...")
    data = json.loads(INPUT_FILE.read_text(encoding="utf-8"))
    total = len(data)
    print(f"  Entries: {total}")

    # Step 2: Normalize references
    print(f"\n[2/4] Normalizing references...")
    refs_fixed = 0
    inline_fixed = 0
    missing_book_mappings = set()

    for entry in data:
        # Fix references array
        refs = entry.get("references", [])
        if refs:
            new_refs = []
            for ref in refs:
                new_ref = normalize_reference(str(ref))
                if new_ref != ref:
                    refs_fixed += 1
                new_refs.append(new_ref)
            entry["references"] = new_refs

        # Fix inline references in definition
        definition = entry.get("definition", "")
        if definition:
            # Find references in text
            found = REF_PATTERN.findall(definition)
            for match in found:
                num_prefix = match[0] or match[1] or ""
                book = match[2]
                if num_prefix:
                    full_book = f"{num_prefix.upper()} {book}"
                else:
                    full_book = book
                key = normalize_book_key(full_book)
                if key not in BOOK_MAP_EN_TO_ES and key.rstrip('.') not in BOOK_MAP_EN_TO_ES:
                    missing_book_mappings.add(full_book)

            new_definition = normalize_inline_references(definition)
            if new_definition != definition:
                inline_fixed += 1
                entry["definition"] = new_definition

    print(f"  References arrays fixed: {refs_fixed} entries updated")
    print(f"  Inline definitions fixed: {inline_fixed} entries updated")
    if missing_book_mappings:
        print(f"  Unknown book names (kept as-is): {len(missing_book_mappings)}")
        for b in sorted(missing_book_mappings)[:20]:
            print(f"    - {b}")

    # Step 3: Save updated JSON
    print(f"\n[3/4] Saving normalized dictionary...")
    INPUT_FILE.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"  Saved to: {INPUT_FILE}")

    # Step 4: Rebuild database
    print(f"\n[4/4] Rebuilding dictionary.db...")
    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()

    conn = sqlite3.connect(OUTPUT_DB)
    conn.executescript("""
        PRAGMA user_version = 1;
        CREATE TABLE dictionary_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            term TEXT NOT NULL,
            normalized_term TEXT NOT NULL,
            definition TEXT NOT NULL,
            references_json TEXT NOT NULL,
            category TEXT NOT NULL
        );
        CREATE INDEX idx_dict_normalized ON dictionary_entries(normalized_term);
        CREATE INDEX idx_dict_category ON dictionary_entries(category);
        CREATE INDEX idx_dict_term ON dictionary_entries(term);
    """)

    rows = []
    for entry in data:
        term = entry.get("term", "").strip()
        definition = entry.get("definition", "").strip()
        if not term or not definition or len(definition) < 20:
            continue

        rows.append((
            term,
            normalize_term(term),
            definition,
            json.dumps(entry.get("references", [])[:10], ensure_ascii=False),
            entry.get("category", "other"),
        ))

    conn.executemany(
        "INSERT INTO dictionary_entries (term, normalized_term, definition, references_json, category) VALUES (?, ?, ?, ?, ?)",
        rows
    )
    conn.commit()
    conn.execute("VACUUM")
    conn.close()

    size_kb = OUTPUT_DB.stat().st_size / 1024
    print(f"  Database: {OUTPUT_DB}")
    print(f"  Size: {size_kb:.1f} KB")
    print(f"  Entries: {len(rows)}")

    # Step 5: Show samples
    print(f"\nSample normalized references:")
    sample = 0
    for entry in data:
        refs = entry.get("references", [])
        if refs and len(refs) > 0 and sample < 8:
            print(f"  {entry['term'][:25]}: {refs[:3]}")
            sample += 1
        if sample >= 8:
            break

    print(f"\n{'=' * 60}")
    print("Done! References normalized to Spanish.")
    print(f"\nNext: Clean build with new dictionary.db")


if __name__ == "__main__":
    main()

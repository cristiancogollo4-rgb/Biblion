#!/usr/bin/env python3
"""
Audit and fix translated dictionary quality:
1. Find entries with English definitions
2. Fix single-letter terms (A->Alfa, etc.)
3. Re-translate definitions still in English
4. Rebuild dictionary.db
"""
import json, re, sqlite3, time, unicodedata
from pathlib import Path
from deep_translator import GoogleTranslator

ROOT = Path(__file__).resolve().parents[1]
INPUT_FILE = ROOT / "tools" / "eastons_dictionary_es.json"
OUTPUT_DB = ROOT / "app" / "src" / "main" / "assets" / "databases" / "dictionary.db"

# Spanish equivalents for English single-letter terms
LETTER_MAP = {
    "A": "Alfa", "B": "Beta", "C": "Gamma", "D": "Delta",
    "E": "Epsilon", "F": "Digamma", "G": "Zeta", "H": "Eta",
    "I": "Iota", "J": "Jota", "K": "Kappa", "L": "Lambda",
    "M": "Mi", "N": "Ni", "O": "Omicron", "P": "Pi",
    "Q": "Koppa", "R": "Rho", "S": "Sigma", "T": "Tau",
    "U": "Upsilon", "V": "Vau", "W": "Doble U", "X": "Xi",
    "Y": "Psi", "Z": "Omega",
}

# English stop words to detect untranslated definitions
ENGLISH_PATTERNS = [
    r'\bthe\b', r'\band\b', r'\bwas\b', r'\bwere\b', r'\bfrom\b',
    r'\bwith\b', r'\bthat\b', r'\bthis\b', r'\bhave\b', r'\bbeen\b',
    r'\bwhich\b', r'\btheir\b', r'\bthere\b', r'\bwould\b', r'\bwill\b',
    r'\binto\b', r'\bupon\b', r'\bwhom\b', r'\bcalled\b', r'\bmade\b',
    r'\bshall\b', r'\bthese\b', r'\bthose\b', r'\bwhen\b', r'\bwere\b',
    r'\bused\b', r'\bword\b', r'\bname\b', r'\bmeans\b', r'\bmeaning\b',
    r'\brendered\b', r'\btranslated\b', r'\bliterally\b',
    r'\bdenotes\b', r'\bsignifies\b', r'\bexpresses\b',
    r'\bconsists\b', r'\bcontains\b', r'\bincludes\b',
    r'\bprobably\b', r'\bperhaps\b', r'\bmentioned\b', r'\breferred\b',
    r'\baccording\b', r'\bdenote\b', r'\bdesignate\b', r'\bdenoting\b',
    r'\bdesignates\b', r'\bsupposed\b', r'\bapplied\b', r'\bappears\b',
]

def is_definition_in_english(definition: str) -> bool:
    """Check if a definition is still in English."""
    if not definition or len(definition) < 30:
        return False

    lower = definition.lower()
    matches = 0
    for pattern in ENGLISH_PATTERNS:
        if re.search(pattern, lower):
            matches += 1
        if matches >= 3:
            return True
    return False

def normalize_term(term):
    lowered = term.lower().strip()
    norm = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in norm if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_accents)

def main():
    print("=" * 60)
    print("Audit & Fix Translated Dictionary")
    print("=" * 60)

    data = json.loads(INPUT_FILE.read_text(encoding="utf-8"))
    total = len(data)
    print(f"Total entries: {total}")

    translator = GoogleTranslator(source="en", target="es")

    # Audit pass
    english_entries = []
    single_letter_entries = []
    short_entries = []
    garbled_entries = []

    for i, entry in enumerate(data):
        term = entry.get("term", "").strip()
        definition = entry.get("definition", "").strip()

        # Check single-letter terms
        if len(term) == 1 and term.isalpha():
            single_letter_entries.append(i)

        # Check for English definitions
        if is_definition_in_english(definition):
            english_entries.append(i)

        # Check for very short definitions (likely incomplete)
        if definition and 0 < len(definition) < 30:
            short_entries.append(i)

        # Check for garbled text (too many special chars)
        if definition:
            special = len(re.findall(r'[^\w\sáéíóúñÁÉÍÓÚÑ,.;:()\-"\'»«¿?¡!\d]', definition))
            alpha = len(re.findall(r'[a-zA-Z]', definition))
            if special > alpha * 0.1 and len(definition) > 50:
                garbled_entries.append(i)

    print(f"\nAudit results:")
    print(f"  English definitions: {len(english_entries)}")
    print(f"  Single-letter terms: {len(single_letter_entries)}")
    print(f"  Too short definitions: {len(short_entries)}")
    print(f"  Possibly garbled: {len(garbled_entries)}")

    # Show English entries
    if english_entries:
        print(f"\nEnglish definitions found:")
        for idx in english_entries[:20]:
            entry = data[idx]
            term = entry.get("term", "")[:40]
            d = entry.get("definition", "")[:100]
            print(f"  [{idx}] {term}: {d}...")
        if len(english_entries) > 20:
            print(f"  ... and {len(english_entries) - 20} more")

    # Fix single-letter terms
    fixed_count = 0
    for idx in single_letter_entries:
        term = data[idx].get("term", "").strip()
        if term in LETTER_MAP:
            new_term = LETTER_MAP[term]
            data[idx]["term"] = new_term
            data[idx]["normalizedTerm"] = normalize_term(new_term)
            # Also translate the definition if needed
            definition = data[idx].get("definition", "")
            if is_definition_in_english(definition):
                try:
                    data[idx]["definition"] = translator.translate(definition)
                except Exception:
                    pass
            fixed_count += 1
            print(f"  Fixed term: {term} -> {new_term}")

    # Fix English definitions
    print(f"\nFixing {len(english_entries)} English definitions...")
    for idx in english_entries:
        definition = data[idx].get("definition", "")
        term = data[idx].get("term", "")
        if definition and len(definition) > 20:
            try:
                translated = translator.translate(definition)
                data[idx]["definition"] = translated
                fixed_count += 1
                if fixed_count % 20 == 0:
                    print(f"  Fixed {fixed_count}... ({term})")
            except Exception as e:
                print(f"  Error translating [{idx}] {term}: {e}")
        time.sleep(0.1)  # avoid rate limiting

    # Fix garbled entries
    print(f"\nFixing {len(garbled_entries)} possibly garbled entries...")
    for idx in garbled_entries:
        definition = data[idx].get("definition", "")
        if definition and len(definition) > 50:
            # Clean common garbage characters
            cleaned = definition.replace('�', '')
            cleaned = cleaned.replace('\x00', '')
            data[idx]["definition"] = cleaned

    # Save fixed JSON
    print(f"\nSaving fixed JSON...")
    INPUT_FILE.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")

    # Rebuild database
    print(f"\nRebuilding dictionary.db...")
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
            term, normalize_term(term), definition,
            json.dumps(entry.get("references", [])[:5], ensure_ascii=False),
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
    print(f"\nDictionary rebuilt: {OUTPUT_DB}")
    print(f"Size: {size_kb:.1f} KB")
    print(f"Entries: {len(rows)}")
    print(f"Fixed: {fixed_count} entries")
    print(f"Single-letter terms fixed: {len(single_letter_entries)}")

    # Verify samples
    print(f"\nVerification samples:")
    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.execute(
        "SELECT term, substr(definition, 1, 100) FROM dictionary_entries ORDER BY RANDOM() LIMIT 10"
    )
    for term, d in cursor.fetchall():
        print(f"  {term}: {d}...")
    conn.close()

if __name__ == "__main__":
    main()

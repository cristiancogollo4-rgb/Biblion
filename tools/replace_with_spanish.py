#!/usr/bin/env python3
"""
Replaces ALL English dictionary files with their Spanish equivalents.
Generates the Worker subset in Spanish.
"""
import json, re, unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ES_FULL = ROOT / "tools" / "eastons_dictionary_es.json"

# Output files to create/update
WORKER_FULL = ROOT / "workers" / "bibi" / "data" / "eastons_dictionary_full.json"
WORKER_SUBSET = ROOT / "workers" / "bibi" / "data" / "eastons_dictionary.json"

MAX_DEF_LENGTH = 400
TOP_ENTRIES = 200

def normalize_term(term):
    lowered = term.lower().strip()
    norm = unicodedata.normalize("NFD", lowered)
    return "".join(ch for ch in norm if unicodedata.category(ch) != "Mn")

# Priority Spanish terms that should be in the Worker subset
PRIORITY_TERMS = [
    # Teologicos
    "fe", "gracia", "pecado", "salvacion", "redencion", "justificacion",
    "santificacion", "pacto", "ley", "evangelio", "reino", "iglesia",
    "oracion", "adoracion", "bautismo", "comunion", "resurreccion",
    "crucifixion", "ascension", "pentecostes", "trinidad", "santidad",
    "justicia", "misericordia", "amor", "esperanza", "paz", "gozo",
    "arrepentimiento", "perdon", "expiacion", "propiciacion",
    "discipulado", "mayordomia", "comunion",
    # Personas clave
    "abraham", "moises", "david", "salomon", "isaias", "jeremias",
    "ezequiel", "daniel", "pedro", "pablo", "juan", "santiago",
    "jesus", "cristo", "maria", "jose", "adan", "eva",
    "noe", "jacob", "isaac", "samuel", "elias", "eliseo",
    "josue", "caleb", "rut", "ester",
    # Lugares
    "jerusalen", "belen", "egipto", "babilonia", "asiria",
    "persia", "roma", "galilea", "samaria", "judea",
    "sinai", "canaan", "templo", "tabernaculo",
    # Practicas y eventos
    "pascua", "circuncision", "sabbat", "jubileo",
    "exodo", "cautiverio", "crucifixion", "resurreccion",
    "ascension", "pentecostes",
]

print("=" * 60)
print("Replacing English dictionaries with Spanish")
print("=" * 60)

# Load Spanish data
print(f"\nLoading Spanish dictionary: {ES_FULL}")
data = json.loads(ES_FULL.read_text(encoding="utf-8"))
print(f"  Entries: {len(data)}")

# 1. Replace full dictionary
print(f"\n[1/3] Replacing full dictionary...")
WORKER_FULL.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
print(f"  Saved: {WORKER_FULL} ({WORKER_FULL.stat().st_size / 1024:.0f} KB)")

# 2. Generate Worker subset
print(f"\n[2/3] Generating Worker subset (top {TOP_ENTRIES})...")

scored = []
for entry in data:
    term = entry.get("term", "")
    normalized = normalize_term(term)
    definition = entry.get("definition", "").lower()
    category = entry.get("category", "")

    score = 0
    # Priority match
    for pt in PRIORITY_TERMS:
        if pt in normalized or pt in definition:
            score += 10 if pt in normalized else 3

    # Category bonus
    if category == "concept": score += 5
    elif category == "person": score += 3
    elif category == "place": score += 2

    # Reference count bonus
    refs = entry.get("references", [])
    score += min(len(refs), 10)

    scored.append((score, entry))

scored.sort(key=lambda x: x[0], reverse=True)
subset = [e for _, e in scored[:TOP_ENTRIES]]

# Ensure alphabet coverage
letters = set(e["term"][0].lower() for e in subset if e["term"][0].isalpha())
for letter in "abcdefghijklmnopqrstuvwxyz":
    if letter not in letters:
        for entry in data:
            if entry["term"][0].lower() == letter:
                subset.append(entry)
                break

# Format for Worker (truncate definitions)
worker_entries = []
for entry in subset:
    d = entry.get("definition", "")
    if len(d) > MAX_DEF_LENGTH:
        last_period = d[:MAX_DEF_LENGTH].rfind(".")
        d = d[:last_period + 1] if last_period > MAX_DEF_LENGTH * 0.6 else d[:MAX_DEF_LENGTH] + "..."

    worker_entries.append({
        "t": entry.get("term", ""),
        "n": normalize_term(entry.get("term", "")),
        "d": d,
        "r": entry.get("references", [])[:3],
        "c": entry.get("category", "other"),
        "s": entry.get("searchTerms", [])[:6],
    })

WORKER_SUBSET.write_text(json.dumps(worker_entries, indent=2, ensure_ascii=False), encoding="utf-8")
print(f"  Saved: {WORKER_SUBSET} ({WORKER_SUBSET.stat().st_size / 1024:.0f} KB)")
print(f"  Entries: {len(worker_entries)}")

# 3. Clean up old build artifacts
print(f"\n[3/3] Cleaning old build files...")

# Remove old checkpoints
checkpoint = ROOT / "tools" / "translate_checkpoint.json"
if checkpoint.exists():
    checkpoint.unlink()
    print(f"  Removed: {checkpoint}")

# Remove test scripts
for name in ["test_translate.py", "test_translate2.py", "test_translate3.py",
             "test_translate_batch.py", "test_translate_2.py", "test_translate_ctx.py",
             "fix_translations.py", "fix_remaining_en.py",
             "build_dictionary_es.py", "check_db.py", "check_schema.py",
             "check_indices.py", "chk.py", "count_en.py"]:
    f = ROOT / "tools" / name
    if f.exists():
        f.unlink()
        print(f"  Removed: {f}")

print(f"\n{'=' * 60}")
print("Done! All dictionaries are now in Spanish.")
print(f"\nFiles updated:")
print(f"  {WORKER_FULL.name} - Full Spanish dictionary")
print(f"  {WORKER_SUBSET.name} - Worker subset (Spanish)")
print(f"  dictionary.db - Room database (Spanish)")

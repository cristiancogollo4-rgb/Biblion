#!/usr/bin/env python3
"""
Exports Easton's Bible Dictionary from SQLite to JSON for Bibi Worker integration.

This script reads the dictionary.db created by build_dictionary_sqlite.py and
exports it to a JSON file that can be:
1. Embedded in the Bibi Worker (for small subsets)
2. Uploaded to Cloudflare KV (for full dictionary access)
3. Used as a local fallback in the Android app

Output:
    workers/bibi/data/eastons_dictionary.json

Usage:
    python tools/export_dictionary_for_worker.py
"""

from __future__ import annotations

import json
import sqlite3
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB_PATH = ROOT / "app" / "src" / "main" / "assets" / "databases" / "dictionary.db"
OUTPUT_DIR = ROOT / "workers" / "bibi" / "data"
OUTPUT_FILE = OUTPUT_DIR / "eastons_dictionary.json"

# Maximum definition length for Worker embedding (to keep response size manageable)
MAX_DEFINITION_LENGTH = 500

# Number of top entries to embed directly in Worker (most common biblical terms)
TOP_ENTRIES_FOR_WORKER = 200


def normalize_term(term: str) -> str:
    """Normalizes a term for consistent matching."""
    lowered = term.lower().strip()
    normalized = unicodedata.normalize("NFD", lowered)
    without_accents = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_accents)


def extract_search_terms(term: str, definition: str) -> list[str]:
    """
    Extracts search terms from a dictionary entry.
    Includes the main term plus key words from the definition.
    """
    terms = [term.lower()]

    # Add alternative forms (remove common suffixes)
    for suffix in ["s", "es", "ing", "ed", "tion", "ment", "ness"]:
        if term.lower().endswith(suffix):
            root = term.lower()[:-len(suffix)]
            if len(root) > 3:
                terms.append(root)

    # Add key biblical terms from definition
    biblical_keywords = re.findall(r'\b[A-Z][a-z]{3,}\b', definition)
    for keyword in biblical_keywords:
        if len(keyword) > 3 and keyword.lower() not in terms:
            terms.append(keyword.lower())

    return list(set(terms))[:10]  # Limit to 10 terms per entry


def truncate_definition(definition: str, max_length: int) -> str:
    """Truncates definition to max_length while preserving complete sentences."""
    if len(definition) <= max_length:
        return definition

    # Try to cut at sentence boundary
    truncated = definition[:max_length]
    last_period = truncated.rfind(".")
    last_semicolon = truncated.rfind(";")

    if last_period > max_length * 0.6:
        return truncated[:last_period + 1]
    elif last_semicolon > max_length * 0.6:
        return truncated[:last_semicolon + 1]
    else:
        return truncated.rstrip() + "..."


def export_full_dictionary() -> list[dict]:
    """
    Exports the full dictionary to JSON format.
    Returns a list of entry dicts.
    """
    if not DB_PATH.exists():
        print(f"ERROR: Database not found at {DB_PATH}")
        print("Run build_dictionary_sqlite.py first.")
        return []

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.execute(
        """
        SELECT id, term, normalized_term, definition, references_json, category
        FROM dictionary_entries
        ORDER BY normalized_term ASC
        """
    )

    entries = []
    for row in cursor.fetchall():
        entry_id, term, normalized_term, definition, references_json, category = row

        # Parse references
        try:
            references = json.loads(references_json)
        except (json.JSONDecodeError, TypeError):
            references = []

        # Extract search terms
        search_terms = extract_search_terms(term, definition)

        entries.append({
            "id": entry_id,
            "term": term,
            "normalizedTerm": normalized_term,
            "definition": definition,
            "references": references,
            "category": category,
            "searchTerms": search_terms,
        })

    conn.close()
    return entries


def export_worker_subset(entries: list[dict], limit: int = TOP_ENTRIES_FOR_WORKER) -> list[dict]:
    """
    Creates a subset of entries optimized for Worker embedding.
    Prioritizes:
    1. Theological concepts (faith, grace, sin, etc.)
    2. Key biblical figures (Abraham, Moses, David, Jesus, Paul)
    3. Important places (Jerusalem, Bethlehem, Egypt)
    4. Practices and events (Passover, baptism, crucifixion)
    """
    # Priority terms that should always be included
    priority_terms = [
        # Theological concepts
        "faith", "grace", "sin", "salvation", "redemption", "justification",
        "sanctification", "covenant", "law", "gospel", "kingdom", "church",
        "prayer", "worship", "baptism", "communion", "resurrection",
        "crucifixion", "ascension", "pentecost", "trinity", "holiness",
        "righteousness", "mercy", "love", "hope", "peace", "joy",
        "repentance", "forgiveness", "atonement", "propitiation",
        "election", "predestination", "regeneration", "conversion",
        "discipleship", "stewardship", "fellowship", "ministry",

        # Key biblical figures
        "abraham", "moses", "david", "solomon", "isaiah", "jeremiah",
        "ezekiel", "daniel", "peter", "paul", "john", "james",
        "jesus", "christ", "mary", "joseph", "adam", "eve",
        "noah", "jacob", "isaac", "samuel", "elijah", "elisha",
        "joshua", "caleb", "ruth", "esther", "nebuchadnezzar",
        "cyru", "herod", "pontius pilate", "barnabas", "timothy",
        "titus", "philip", "stephen", "matthew", "mark", "luke",

        # Important places
        "jerusalem", "bethlehem", "egypt", "babylon", "assyria",
        "persia", "rome", "galilee", "samaria", "judea",
        "sinai", "canaan", "promised land", "temple", "tabernacle",
        "mount zion", "mount olives", "garden of eden", "armageddon",

        # Practices and events
        "passover", "day of atonement", "feast of tabernacles",
        "feast of weeks", "circumcision", "sabbath", "jubilee",
        "exodus", "captivity", "return from exile",
        "crucifixion", "resurrection", "ascension", "pentecost",
        "last supper", "transfiguration", "great commission",
    ]

    # Score entries by priority
    scored_entries = []
    for entry in entries:
        term_lower = entry["term"].lower()
        normalized = entry["normalizedTerm"]
        definition_lower = entry["definition"].lower()

        score = 0

        # Check if term matches priority terms
        for priority in priority_terms:
            if priority in term_lower or priority in normalized:
                score += 10
            elif priority in definition_lower:
                score += 2

        # Bonus for categories
        if entry["category"] == "concept":
            score += 5
        elif entry["category"] == "person":
            score += 3
        elif entry["category"] == "place":
            score += 2

        # Bonus for having references
        if entry["references"]:
            score += len(entry["references"])

        scored_entries.append((score, entry))

    # Sort by score descending and take top entries
    scored_entries.sort(key=lambda x: x[0], reverse=True)
    top_entries = [entry for score, entry in scored_entries[:limit]]

    # Also ensure alphabetical coverage (at least some entries from each letter)
    letters_covered = set()
    for entry in top_entries:
        first_letter = entry["term"][0].lower()
        if first_letter.isalpha():
            letters_covered.add(first_letter)

    # Add entries from missing letters
    for letter in "abcdefghijklmnopqrstuvwxyz":
        if letter not in letters_covered:
            for entry in entries:
                if entry["term"][0].lower() == letter and entry not in top_entries:
                    top_entries.append(entry)
                    letters_covered.add(letter)
                    break

    return top_entries[:limit + 26]  # Allow some extra for alphabet coverage


def main() -> None:
    """Main entry point: exports dictionary to JSON for Worker use."""
    print("=" * 60)
    print("Biblion: Exporting Dictionary for Bibi Worker")
    print("=" * 60)

    # Step 1: Export full dictionary
    print("\n[1/3] Exporting full dictionary from SQLite...")
    full_entries = export_full_dictionary()

    if not full_entries:
        print("ERROR: No entries found. Run build_dictionary_sqlite.py first.")
        return

    print(f"  Total entries: {len(full_entries)}")

    # Step 2: Create Worker subset
    print(f"\n[2/3] Creating Worker subset (top {TOP_ENTRIES_FOR_WORKER} + alphabet coverage)...")
    worker_entries = export_worker_subset(full_entries)
    print(f"  Worker subset: {len(worker_entries)} entries")

    # Step 3: Write JSON files
    print(f"\n[3/3] Writing JSON files...")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # Write full dictionary
    full_output = OUTPUT_DIR / "eastons_dictionary_full.json"
    full_output.write_text(
        json.dumps(full_entries, indent=2, ensure_ascii=False),
        encoding="utf-8"
    )
    print(f"  Full dictionary: {full_output} ({full_output.stat().st_size / 1024:.1f} KB)")

    # Write Worker subset (truncated definitions for size)
    worker_subset = []
    for entry in worker_entries:
        worker_subset.append({
            "term": entry["term"],
            "normalizedTerm": entry["normalizedTerm"],
            "definition": truncate_definition(entry["definition"], MAX_DEFINITION_LENGTH),
            "references": entry["references"][:5],  # Limit references
            "category": entry["category"],
            "searchTerms": entry["searchTerms"],
        })

    OUTPUT_FILE.write_text(
        json.dumps(worker_subset, indent=2, ensure_ascii=False),
        encoding="utf-8"
    )
    print(f"  Worker subset: {OUTPUT_FILE} ({OUTPUT_FILE.stat().st_size / 1024:.1f} KB)")

    # Summary
    print(f"\n{'=' * 60}")
    print("Export complete!")
    print(f"\nFiles created:")
    print(f"  - {full_output.name}: Full dictionary ({len(full_entries)} entries)")
    print(f"  - {OUTPUT_FILE.name}: Worker subset ({len(worker_subset)} entries)")
    print(f"\nNext steps:")
    print(f"  1. Upload eastons_dictionary_full.json to Cloudflare KV")
    print(f"  2. Update Bibi Worker to use the expanded dictionary")
    print(f"  3. Or embed worker_subset directly in index.js")


if __name__ == "__main__":
    main()

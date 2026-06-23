"""Validacion de topics.db v2."""
import sqlite3
from pathlib import Path

DB = Path(r"app\src\main\assets\databases\topics.db")

conn = sqlite3.connect(str(DB))
c = conn.cursor()

print("=== Tables ===")
for row in c.execute("SELECT name FROM sqlite_master WHERE type='table'"):
    print("  " + row[0])

print()
print("=== Topics count by category ===")
for row in c.execute("SELECT category, COUNT(*) FROM topics GROUP BY category ORDER BY 2 DESC"):
    print(f"  {row[0]:>20}: {row[1]:>4}")

print()
print("=== Sample topics (5 por categoria top) ===")
for cat in ["CHRISTIAN_LIFE", "DOCTRINE", "SIN", "CHURCH", "ATTRIBUTE_OF_GOD"]:
    print(f"\n  {cat}:")
    for row in c.execute(
        "SELECT name_en, name_es FROM topics WHERE category=? ORDER BY verse_count DESC LIMIT 5",
        (cat,),
    ):
        print(f"    {row[0]:<35} -> {row[1]}")

print()
print("=== References sample (top 10 by score) ===")
for row in c.execute("""
    SELECT t.name_en, r.book, r.chapter, r.verse_start, r.score
    FROM topic_references r
    JOIN topics t ON r.topic_id = t.id
    ORDER BY r.score DESC LIMIT 10
"""):
    print(f"  {row[0]:<35} {row[1]} {row[2]}:{row[3]} (score={row[4]})")

print()
print("=== Book coverage ===")
for row in c.execute("""
    SELECT book, COUNT(DISTINCT topic_id) as n_topics, COUNT(*) as n_refs
    FROM topic_references
    GROUP BY book
    ORDER BY n_refs DESC
    LIMIT 15
"""):
    print(f"  {row[0]:<20} topics={row[1]:>4} refs={row[2]:>4}")

print()
print("=== Top 10 topics by verse_count (overall) ===")
for row in c.execute("""
    SELECT name_en, name_es, verse_count, total_verses, cluster_size
    FROM topics ORDER BY verse_count DESC LIMIT 10
"""):
    print(f"  [{row[2]:>3}v] {row[0]:<40} -> {row[1]:<35} (cluster={row[4]})")

conn.close()

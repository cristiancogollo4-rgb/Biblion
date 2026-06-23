"""Fase 8: Quality Report final del refactor topics.db v2."""
import json
import sqlite3
from pathlib import Path

DB = Path(r"app\src\main\assets\databases\topics.db")
TAXONOMY = Path(r"tools\topic_taxonomy_final.json")

conn = sqlite3.connect(str(DB))
c = conn.cursor()

# Stats
n_topics = c.execute("SELECT COUNT(*) FROM topics").fetchone()[0]
n_aliases = c.execute("SELECT COUNT(*) FROM topic_aliases").fetchone()[0]
n_refs = c.execute("SELECT COUNT(*) FROM topic_references").fetchone()[0]
identity_hash = c.execute(
    "SELECT identity_hash FROM room_master_table WHERE id = 42"
).fetchone()[0]

# Categorias
cat_dist = {}
for row in c.execute(
    "SELECT category, COUNT(*) FROM topics GROUP BY category ORDER BY 2 DESC"
):
    cat_dist[row[0]] = row[1]

# Top 10
top = []
for row in c.execute("""
    SELECT slug, name_es, name_en, category, verse_count, total_verses, cluster_size
    FROM topics ORDER BY verse_count DESC LIMIT 10
"""):
    top.append({
        "slug": row[0], "name_es": row[1], "name_en": row[2],
        "category": row[3], "verse_count": row[4],
        "total_verses": row[5], "cluster_size": row[6]
    })

# Sample translations
print("=" * 60)
print("BIBLION: TOPICS.DB V2 - QUALITY REPORT")
print("=" * 60)
print()
print(f"DB:                  {DB}")
print(f"Topics:              {n_topics}")
print(f"Aliases:             {n_aliases}")
print(f"References:          {n_refs}")
print(f"Identity hash:       {identity_hash}")
print()
print("=== Distribucion por categoria ===")
for cat, count in cat_dist.items():
    print(f"  {cat:>20}: {count:>4} clusters")
print()
print("=== Top 10 por verse_count ===")
for t in top:
    auto_marker = ""
    print(f"  [{t['verse_count']:>3}v] {t['name_en']:<40} -> {t['name_es']:<35} ({t['category']})")

# Check coverage
with TAXONOMY.open(encoding="utf-8") as f:
    tax = json.load(f)
n_translated = tax.get("n_with_translation", 0)
n_clusters = tax.get("n_clusters", 0)
coverage = n_translated / n_clusters * 100 if n_clusters else 0
print()
print(f"=== Cobertura traducciones ===")
print(f"  Clusters totales:       {n_clusters}")
print(f"  Traducidos:             {n_translated}")
print(f"  Cobertura:              {coverage:.1f}%")

# Refs por libro
print()
print("=== Top 10 libros por referencias ===")
for row in c.execute("""
    SELECT book, COUNT(*) FROM topic_references
    GROUP BY book ORDER BY 2 DESC LIMIT 10
"""):
    print(f"  {row[0]:<20}: {row[1]:>4} refs")

conn.close()
print()
print("BUILD SUCCESS - Schema v2 listo.")

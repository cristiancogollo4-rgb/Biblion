"""
Fase 5: Generar taxonomia final Biblion a partir de clusters clasificados.

Por cada cluster:
1. Selecciona canonical_topic (el mas representativo, por verse_count).
2. Calcula aliases (todos los demas miembros).
3. Busca traduccion en tools/topic_translations_es.json.
4. Genera slug unico.
5. Genera topic_final.json con jerarquia categoria > cluster > topics.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

OUT_DIR = Path("tools")

# Categorias en orden canonico
CATEGORY_ORDER = [
    "DOCTRINE",
    "CHRISTIAN_LIFE",
    "ATTRIBUTE_OF_GOD",
    "PERSON",
    "PLACE",
    "EVENT",
    "PROPHECY",
    "BOOK",
    "CHURCH",
    "COMPARATIVE_RELIGION",
    "SIN",
    "INVALID",
]


def normalize_slug(text: str) -> str:
    s = text.lower().strip()
    s = re.sub(r"[^\w\s-]", "", s)
    s = re.sub(r"\s+", "-", s)
    s = re.sub(r"-+", "-", s).strip("-")
    return s[:120]


def load_translations() -> dict[str, str]:
    """Carga translations ES del diccionario curado."""
    path = OUT_DIR / "topic_translations_es.json"
    if not path.exists():
        return {}
    with path.open(encoding="utf-8") as f:
        data = json.load(f)
    # data puede ser list de {key, translation} o dict {key: translation}
    if isinstance(data, list):
        return {item["key"]: item["translation"] for item in data}
    return data


def main() -> None:
    print("=" * 60)
    print("Biblion: Fase 5 - Taxonomia final")
    print("=" * 60)
    print()

    with (OUT_DIR / "topic_clusters_classified.json").open(encoding="utf-8") as f:
        clusters_data = json.load(f)
    with (OUT_DIR / "topics_candidates.json").open(encoding="utf-8") as f:
        cand_data = json.load(f)

    candidates = cand_data["topics"]
    cand_by_name = {c["topic_en"]: c for c in candidates}

    translations = load_translations()
    print(f"Clusters cargados: {clusters_data['n_clusters']}")
    print(f"Traducciones ES disponibles: {len(translations)}")

    # Generar taxonomia
    taxonomy: dict[str, list[dict]] = {cat: [] for cat in CATEGORY_ORDER}
    used_slugs: set[str] = set()
    unmatched_translations: list[str] = []
    stats = {
        "total_clusters": 0,
        "total_topics": 0,
        "with_translation": 0,
        "with_verse_count_total": 0,
    }

    for cluster in clusters_data["clusters"]:
        cat = cluster["category"]
        stats["total_clusters"] += 1
        # El canonical_topic es el miembro con mayor verse_count
        members_sorted = sorted(
            cluster["members"], key=lambda m: -m["verse_count"]
        )
        canonical = members_sorted[0]
        # Si el canonical esta en translations pero su traduccion es None o vacia, intentar el siguiente
        canonical_es = None
        for cand in members_sorted[:5]:  # probar top 5
            cand_slug = normalize_slug(cand["topic_en"])
            if cand_slug in translations and translations[cand_slug]:
                canonical_es = translations[cand_slug]
                canonical = cand
                break
        if canonical_es is None:
            unmatched_translations.append(canonical["topic_en"])
        else:
            stats["with_translation"] += 1

        # Slug unico
        slug = normalize_slug(canonical["topic_en"])
        original_slug = slug
        n = 2
        while slug in used_slugs:
            slug = f"{original_slug}-{n}"
            n += 1
        used_slugs.add(slug)

        # Aliases
        aliases = [m["topic_en"] for m in members_sorted if m["topic_en"] != canonical["topic_en"]]

        # Verse count total
        total_verses = sum(m["verse_count"] for m in cluster["members"])
        stats["with_verse_count_total"] += total_verses
        stats["total_topics"] += len(cluster["members"])

        # References (cargar del original)
        canonical_orig = cand_by_name.get(canonical["topic_en"])
        references = []
        if canonical_orig:
            # Buscar references en topic-scores originales seria costoso.
            # Por ahora dejar vacio; se llenara en Fase 7.
            pass

        topic_obj = {
            "slug": slug,
            "name_en": canonical["topic_en"],
            "name_es": canonical_es,
            "verse_count": canonical["verse_count"],
            "total_verses": total_verses,
            "cluster_size": cluster["size"],
            "aliases": aliases[:20],  # limitar aliases
            "category": cat,
            "category_sim": cluster.get("category_sim"),
        }
        taxonomy[cat].append(topic_obj)

    # Ordenar clusters por verse_count desc dentro de cada categoria
    for cat in taxonomy:
        taxonomy[cat].sort(key=lambda t: -t["verse_count"])

    # Salida
    final = {
        "n_categories": len([c for c in CATEGORY_ORDER if taxonomy[c]]),
        "n_clusters": stats["total_clusters"],
        "n_topics_raw": stats["total_topics"],
        "n_with_translation": stats["with_translation"],
        "coverage_translation": round(
            stats["with_translation"] / stats["total_clusters"] * 100, 1
        ),
        "categories": {},
    }
    for cat in CATEGORY_ORDER:
        clusters = taxonomy[cat]
        if clusters:
            final["categories"][cat] = {
                "n_clusters": len(clusters),
                "n_translated": sum(1 for c in clusters if c["name_es"]),
                "total_verses": sum(c["total_verses"] for c in clusters),
                "clusters": clusters,
            }

    out_path = OUT_DIR / "topic_taxonomy.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(final, f, ensure_ascii=False, indent=2)
    print(f"\nGuardado: {out_path}")

    # Resumen
    print(f"\n=== Resumen Fase 5 ===")
    print(f"Total clusters:           {stats['total_clusters']}")
    print(f"Total topics raw:         {stats['total_topics']}")
    print(f"Con traduccion ES:        {stats['with_translation']} ({final['coverage_translation']}%)")
    print(f"\nDistribucion por categoria (orden canonico):")
    for cat in CATEGORY_ORDER:
        clusters = taxonomy[cat]
        if clusters:
            translated = sum(1 for c in clusters if c["name_es"])
            print(f"  {cat:>20}: {len(clusters):>4} clusters ({translated} traducidos)")

    # Sample de los 10 mas grandes de cada categoria top
    print(f"\n=== Top 10 por categoria (mas frecuentes) ===")
    for cat in ["CHRISTIAN_LIFE", "DOCTRINE", "SIN", "CHURCH"]:
        print(f"\n  {cat}:")
        for t in taxonomy[cat][:10]:
            print(f"    [{t['verse_count']:>3}v] {t['name_en']:<40} -> {t['name_es'] or '(sin traduccion)'}")

    # Guardar unmatched para traduccion posterior
    unmatched_path = OUT_DIR / "topics_unmatched_es.json"
    with unmatched_path.open("w", encoding="utf-8") as f:
        json.dump(
            {
                "description": "Canonical topics sin traduccion ES. Requieren traduccion manual o Worker IA.",
                "n": len(unmatched_translations),
                "topics": unmatched_translations,
            },
            f,
            ensure_ascii=False,
            indent=2,
        )
    print(f"\nSin traduccion: {len(unmatched_translations)} canonical topics")
    print(f"Guardado: {unmatched_path}")


if __name__ == "__main__":
    main()

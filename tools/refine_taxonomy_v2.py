"""
Fase 6 (v2): Refinamiento quirurgico via overrides.

En lugar de reglas keyword (demasiado agresivas), usa una lista
de overrides explicitos para clusters mal clasificados.
"""
from __future__ import annotations

import json
from pathlib import Path

OUT_DIR = Path("tools")

# Overrides: {slug: new_category}
# Solo casos especificos confirmados como mal clasificados.
OVERRIDES: dict[str, str] = {
    # Sacramento o doctrina especifica
    "baptism": "DOCTRINE",
    "the-marriage-supper-of-the-lamb": "PROPHECY",
    "rapture": "PROPHECY",
    "the-end-of-days": "PROPHECY",
    "the-love-of-god": "ATTRIBUTE_OF_GOD",
    "backsliders": "DOCTRINE",
    "backslider": "DOCTRINE",
    "the-laws-of-the-land": "DOCTRINE",
    "the-body-of-christ": "DOCTRINE",
    "name": "DOCTRINE",
    "names-of-god": "ATTRIBUTE_OF_GOD",
    "the-name-of-god": "ATTRIBUTE_OF_GOD",

    # Lifestyle -> CHRISTIAN_LIFE
    "clothing": "CHRISTIAN_LIFE",
    "handling-money": "CHRISTIAN_LIFE",
    "family-relationships": "CHRISTIAN_LIFE",
    "helping-others": "CHRISTIAN_LIFE",
    "celebrating-christmas": "CHRISTIAN_LIFE",
    "anniversaries": "CHRISTIAN_LIFE",
    "dancing": "CHRISTIAN_LIFE",
    "music-in-church": "CHURCH",  # OK

    # Atributos / doctrina
    "peace": "ATTRIBUTE_OF_GOD",
    "lions": "PLACE",
    "eagle": "PLACE",
    "eagles": "PLACE",
    "snow": "PLACE",

    # Conceptos teologicos
    "deliverance": "DOCTRINE",
    "being-a-man-of-god": "CHRISTIAN_LIFE",
    "resolution": "CHRISTIAN_LIFE",

    # Ruido -> INVALID
    "wind-blowing": "INVALID",
    "ice-age": "INVALID",
    "the-weather": "INVALID",
    "autumn": "INVALID",
    "season-change": "INVALID",
    "midnight": "INVALID",
    "twin-towers": "INVALID",
    "the-twin-towers": "INVALID",
    "spirits-walking-the-earth": "INVALID",
    "new-years-resolutions": "INVALID",
    "making-promises": "CHRISTIAN_LIFE",
    "warning-before-destruction": "PROPHECY",

    # Cosas biblicas -> PLACE/CHRISTIAN_LIFE
    "wind": "PLACE",
    "rock": "PLACE",
    "sand": "PLACE",
    "water": "PLACE",
    "fire": "PLACE",
    "lamb": "CHRISTIAN_LIFE",
    "lion": "PLACE",
    "sheep": "PLACE",
    "goat": "PLACE",
    "cattle": "PLACE",
    "tree": "PLACE",
    "mountain": "PLACE",
    "river": "PLACE",
    "sea": "PLACE",
    "ocean": "PLACE",
    "fish": "PLACE",
    "bird": "PLACE",
    "birds": "PLACE",
    "footprints": "INVALID",
    "flower-of-sharon": "PLACE",
    "mushrooms": "INVALID",
    "sandals": "PLACE",
    "the-nile-river": "PLACE",
    "nahum": "PERSON",
    "merkabah": "DOCTRINE",
    "merkaba": "DOCTRINE",
    "merkavah": "DOCTRINE",
    "gypsies": "PERSON",
    "gypsies-gypsies": "PERSON",
}


def main() -> None:
    print("=" * 60)
    print("Biblion: Fase 6 v2 - Refinamiento quirurgico")
    print("=" * 60)
    print()

    # Cargar la version pre-refinamiento (mas limpia)
    src = OUT_DIR / "topic_taxonomy_translated.json"
    with src.open(encoding="utf-8") as f:
        data = json.load(f)

    moved: list[dict] = []

    # Aplicar overrides
    all_clusters = []
    for cat_name, cat in data["categories"].items():
        for cluster in cat["clusters"]:
            old_cat = cluster["category"]
            slug = cluster["slug"]
            if slug in OVERRIDES:
                new_cat = OVERRIDES[slug]
                if new_cat != old_cat:
                    cluster["category"] = new_cat
                    cluster["category_overridden"] = True
                    moved.append(
                        {
                            "slug": slug,
                            "name_en": cluster["name_en"],
                            "from": old_cat,
                            "to": new_cat,
                        }
                    )
            all_clusters.append(cluster)

    # Redistribuir
    if moved:
        new_categories: dict[str, dict] = {}
        for cluster in all_clusters:
            cat = cluster["category"]
            if cat not in new_categories:
                new_categories[cat] = {"clusters": []}
            new_categories[cat]["clusters"].append(cluster)
        for cat_name, cat in new_categories.items():
            clusters = cat["clusters"]
            clusters.sort(key=lambda c: -c["verse_count"])
            new_categories[cat_name] = {
                "n_clusters": len(clusters),
                "n_translated": sum(1 for c in clusters if c["name_es"]),
                "total_verses": sum(c["total_verses"] for c in clusters),
                "clusters": clusters,
            }
        data["categories"] = new_categories

    print(f"Clusters reasignados: {len(moved)}")
    if moved:
        for m in moved:
            print(f"  {m['slug']:<35} {m['from']:>15} -> {m['to']}")

    print(f"\nDistribucion final:")
    for cat_name, cat in sorted(data["categories"].items(), key=lambda x: -x[1]["n_clusters"]):
        print(f"  {cat_name:>20}: {cat['n_clusters']:>4} clusters")

    out_path = OUT_DIR / "topic_taxonomy_final.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"\nGuardado: {out_path}")


if __name__ == "__main__":
    main()

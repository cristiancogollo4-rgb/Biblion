"""
Fase 6: Refinamiento de categorizacion.

Para clusters mal clasificados, mueve a categoria correcta basandose
en keywords. Esto completa la categorizacion automatica iniciada en Fase 4.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

OUT_DIR = Path("tools")

# Reglas de refinamiento: si el canonical_topic o sus aliases contienen
# estos keywords, se reasigna a la categoria destino.
# Las reglas se aplican en orden; la primera que matchea gana.
REFINE_RULES: list[dict] = [
    # ATTRIBUTE_OF_GOD solo si es explicitamente sobre un atributo
    {
        "category": "CHRISTIAN_LIFE",
        "keywords": [
            "following the will of god", "will of god", "god's will",
            "what is gods will", "discerning gods will",
            "striving for excellence", "excellence",
            "honoring god", "honouring god",
            "loving god more than", "loving god",
            "how do we know god exists", "knowing god exists",
            "loving jesus more", "loving jesus",
            "growing in christ", "growing spiritually",
            "being a man of god", "man of god", "woman of god",
            "calling", "vocation", "purpose",
            "armor-bearer", "armor bearer",
        ],
    },
    # PROPHECY solo si es explicitamente profetico
    {
        "category": "CHRISTIAN_LIFE",
        "keywords": [
            "autumn", "spring", "summer", "winter", "season",
            "weather", "the weather", "climate",
            "midnight", "the morning", "night time", "morning time",
            "twin towers", "9/11", "september 11",
            "the end of days", "end of days", "world will end",
            "backsliders", "backslider", "apostasy", "apostate",
            "making promises", "promises",
            "new years resolutions", "resolutions",
            "warning before destruction",
            "rapture",
        ],
    },
    # CHURCH es mas amplio: incluye temas institucionales y civicos
    {
        "category": "CHURCH",
        "keywords": [
            "pastor", "pastors", "pastoral",
            "worship team", "worship leader", "choir", "praise team",
            "sunday service", "worship service", "church service",
            "preaching", "preach", "sermon", "sermons",
            "the body of christ", "body of christ",
            "the church", "church",
            "ministry", "ministries",
            "denomination", "denominations",
        ],
    },
    # ATTRIBUTE_OF_GOD requiere ser atributo puro
    {
        "category": "DOCTRINE",
        "keywords": [
            "godparents", "padrinos",
            "name of god", "names of god",
            "ability", "abilities",
        ],
    },
    # PLACE para cosas biblicas/animales biblicos
    {
        "category": "PLACE",
        "keywords": [
            "rock", "sand", "water", "fire", "wind", "earth",
            "lamb", "lion", "sheep", "goat", "cattle",
            "tree", "forest", "mountain", "river", "sea", "ocean",
            "eagle", "bird", "birds", "fish",
        ],
    },
    # SIN para cosas que son pecado pero pudieron quedar en otra categoria
    {
        "category": "SIN",
        "keywords": [
            "conscientious objectors",  # debatable, but not sin
        ],
    },
]


def normalize(text: str) -> str:
    t = text.lower().strip()
    t = re.sub(r"[^\w\s-]", " ", t)
    t = re.sub(r"\s+", " ", t)
    return t.strip()


def should_reassign(cluster: dict) -> str | None:
    members_text = " | ".join(normalize(m) for m in [cluster["name_en"]] + cluster.get("aliases", []))
    for rule in REFINE_RULES:
        target = rule["category"]
        for kw in rule["keywords"]:
            kw_norm = normalize(kw)
            if not kw_norm or len(kw_norm) < 3:
                continue
            if re.search(rf"\b{re.escape(kw_norm)}\b", members_text):
                return target
    return None


def main() -> None:
    print("=" * 60)
    print("Biblion: Fase 6 - Refinamiento de categorizacion")
    print("=" * 60)
    print()

    with (OUT_DIR / "topic_taxonomy_translated.json").open(encoding="utf-8") as f:
        data = json.load(f)

    moved: list[dict] = []
    cat_dist: dict[str, int] = {}

    for cat_name, cat in data["categories"].items():
        for cluster in cat["clusters"]:
            new_cat = should_reassign(cluster)
            if new_cat is None or new_cat == cat_name:
                continue
            moved.append(
                {
                    "from": cat_name,
                    "to": new_cat,
                    "name_en": cluster["name_en"],
                    "name_es": cluster["name_es"],
                }
            )
            cluster["category"] = new_cat
            cluster["category_refined"] = True

    # Redistribuir clusters entre categorias en data
    if moved:
        all_clusters = []
        for cat_name, cat in data["categories"].items():
            for cluster in cat["clusters"]:
                all_clusters.append(cluster)
        # Recalcular categorias
        new_categories: dict[str, dict] = {}
        for cluster in all_clusters:
            cat = cluster["category"]
            if cat not in new_categories:
                new_categories[cat] = {"clusters": []}
            new_categories[cat]["clusters"].append(cluster)
        # Stats
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

    # Stats
    for cat_name, cat in data["categories"].items():
        cat_dist[cat_name] = cat["n_clusters"]

    print(f"Clusters reasignados: {len(moved)}")
    if moved:
        print(f"\nDetalle:")
        by_target: dict[str, list[str]] = {}
        for m in moved:
            by_target.setdefault(f"{m['from']} -> {m['to']}", []).append(m["name_en"])
        for k, v in by_target.items():
            print(f"  {k}: {len(v)} clusters")
            for name in v[:5]:
                print(f"    - {name}")
            if len(v) > 5:
                print(f"    ... +{len(v) - 5} mas")

    print(f"\nDistribucion final:")
    for cat, count in sorted(cat_dist.items(), key=lambda x: -x[1]):
        print(f"  {cat:>20}: {count:>4} clusters")

    out_path = OUT_DIR / "topic_taxonomy_final.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"\nGuardado: {out_path}")


if __name__ == "__main__":
    main()

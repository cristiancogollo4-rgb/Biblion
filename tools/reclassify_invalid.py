"""
Fase 4.5: Reclasificacion heuristica de clusters INVALID mal clasificados.

Usa reglas keyword-based agresivas para reasignar clusters
que fueron marcados como INVALID pero son temas biblicos validos
(clothing, idol, hair length, etc.).
"""
from __future__ import annotations

import json
import re
from pathlib import Path

OUT_DIR = Path("tools")

# Reglas de reclasificacion: keyword -> categoria destino
# Solo si el cluster INVALID contiene al menos uno de estos keywords.
RECLASSIFY_RULES: list[dict] = [
    # SIN - pecados biblicos que el modelo clasifico como INVALID
    {
        "category": "SIN",
        "keywords": [
            "idol", "idols", "idolatry", "idolaters", "idol worship",
            "graven images", "adoring images", "worship of idols",
            "pornography", "prostitution", "fornication", "adultery",
            "homosexuality", "homosexual", "sodomy",
            "drunkenness", "drunk", "drunkard", "alcoholism", "alcohol abuse",
            "drinking wine", "wine drinking", "wine", "drinking alcoholic",
            "social drinking", "underage drinking", "drinking beer",
            "marijuana", "cannabis", "weed", "drug abuse", "drugs",
            "smoking", "smoking pot", "smoking marijuana",
            "gambling", "lottery", "lotteries", "the lottery",
            "playing the lottery", "poker", "poker playing",
            "raffles", "bingo", "lotteries",
            "witchcraft", "sorcery", "occult", "ouija", "ouija board",
            "quija boards", "quija",
            "tarot", "tarot cards", "fortune telling", "fortune teller",
            "astrology", "astrologers", "horoscope", "horoscopes",
            "zodiac signs", "zodiac", "mediums", "medium",
            "telepathy", "telekinesis", "fortune tellers",
            "earrings", "men wearing earrings", "man wearing earrings",
            "braided hair", "braiding hair", "braiding your hair",
            "hair length", "long hair", "men and long hair",
            "tattoos", "tattoo", "tattooing", "body piercings",
            "piercings", "wearing mixed linens", "wearing mixed fabrics",
            "celebrating halloween", "halloween", "trick or treating",
            "plastic surgery", "cosmetic surgery", "breast augmentation",
            "nightclubs", "nightclub", "clubs", "clubbing",
            "going to bars and clubs", "going to clubs",
            "partying", "wild parties", "party", "parties",
            "wedding ring", "wedding rings", "rings",
            "romance", "romance novels",
            "skin color", "making our skin", "skin",
            "satan ruling the airwaves", "satanic",
            "anklets", "steroids", "having a mohawk", "mohawk",
            "wearing all black",
        ],
    },
    # CHRISTIAN_LIFE - vida cristiana practica que el modelo clasifico como INVALID
    {
        "category": "CHRISTIAN_LIFE",
        "keywords": [
            "clothing", "clothes", "dress", "dress code", "how to dress",
            "dress attire", "garments", "fashion", "dressing",
            "how we dress", "the way we dress", "what to wear",
            "men wearing dresses", "wearing dresses",
            "men wearing pants", "women wearing pants", "underwear", "pants",
            "the way you should dress",
            "dancing", "dance", "dancing in heaven", "dancing in church",
            "dancing at a wedding", "david danced",
            "laugh", "laughing", "humor", "humor and laughter",
            "making fun", "having fun", "fun",
            "image of god", "images", "image",
            "physical fitness", "physical exercise", "fitness",
            "exercise", "health and fitness", "working out",
            "handicapped", "handicap", "handicaps", "disability",
            "disabilities", "mental disability", "disabled people",
            "raising teenagers", "teenagers", "troubled teens",
            "teenage years", "teenage pregnancy", "youth", "young",
            "young adults", "young men", "young people", "youth ministry",
            "girlfriend", "boyfriend", "boyfriends", "girlfriends",
            "having girlfriend", "having boyfriend",
            "men and women", "man and woman", "men and women living",
            "male female friendship", "women and men",
            "worldliness", "the world", "being like the world",
            "worldly things", "worldly", "world",
            "materialism", "material things", "materialistic",
            "material wealth", "being materialistic",
            "driving", "automobiles", "car", "cars",
            "drinking and driving", "speeding",
            "athletics", "athletes", "sports", "sportsmanship",
            "athlete", "competition", "rivalry",
            "college", "going to college", "high school",
            "gaming", "entertainment", "games", "play", "playing",
            "chess", "pokemon", "transformers", "video games",
            "internet", "internet dating", "the internet",
            "media", "the media",
            "martial arts", "karate",
            "economics", "capitalism", "economy",
            "social skills", "society", "social",
            "acting", "actors", "drama", "theater",
            "painting", "art", "artist", "creativity",
            "imagination", "being creative", "creative",
            "celebrating birthdays", "birthday", "birthdays",
            "growing up", "being a kid", "children growing up",
            "diseases", "disease", "avian flu", "health",
            "autism", "adhd", "pms",
            "nationalism", "patriotism",
            "marketing", "network marketing", "fundraising", "fundraisers",
            "industriousness", "industry",
            "computers", "technology", "cameras",
            "multiculturalism", "cultural diversity", "diversity",
            "cultural difference", "culture", "respecting other cultures",
            "leisure", "recreation", "hobbies", "bungee jumping", "swimming",
            "globalization", "international trade", "gas prices",
            "popularity", "fame", "modeling",
            "emulation", "conformity", "copycat",
            "8 balls", "eight balls", "physical", "left handed",
            "left handed people", "hillary clinton", "obama",
            "battlefield of the mind", "battlefield",
            "makeovers", "smart", "intelligence",
            "what jesus wore", "jesus wore",
            "sales", "entrepreneurship", "entrepreneur",
            "night", "the night",
        ],
    },
    # DOCTRINE - doctrina biblica que el modelo clasifico como INVALID
    {
        "category": "DOCTRINE",
        "keywords": [
            "image of god", "graven images",
            "finish the race", "running the race", "running", "run the race",
            "fishers of men", "the race", "running",
            "vegetarianism", "vegetarian", "vegetarians", "eating meat",
            "eating red meat", "eating catfish", "eating fish", "eating shrimp",
            "eating seafood", "kosher",
            "new and old wineskins", "wineskins",
            "reality", "the universe", "universe",
            "dna", "genetic engineering", "genetic testing",
            "intelligent design", "design", "intelligent",
            "biology", "science", "quantum physics", "physics",
            "women teachers", "female education", "girls education",
            "rapture", "millennium",
            "breast cancer", "cancer", "tumors",
            "vaccinations", "stem cell", "embryonic",
            "wearing mixed linens", "linen", "linens",
            "fables", "fable", "the big bang theory", "big bang",
            "postmodernism", "postmodern",
            "animal testing", "designer babies", "designer",
            "statues", "idol words", "idol",
        ],
    },
    # PLACE / BIBLICAL_OBJECTS - cosas materiales biblicas
    {
        "category": "PLACE",
        "keywords": [
            "eagle", "eagles", "bird", "birds", "chicken", "chickens",
            "sandals", "shoes", "feet", "beautiful feet",
            "stars", "the stars", "astronomy", "star",
            "cupbearer", "foxes", "arrows", "woodpeckers", "ants", "the ant",
            "pots", "pot",
            "airplanes", "flying",
        ],
    },
    # CHURCH - gobierno, politica
    {
        "category": "CHURCH",
        "keywords": [
            "america", "the united states", "us", "usa",
            "the american dream", "president",
            "firearms", "guns", "gun control", "weapons", "weapons of warfare",
            "political", "nationalism", "patriotism",
        ],
    },
    # PERSON - personas (no biblical, pero mencionadas en la Biblia)
    {
        "category": "PERSON",
        "keywords": [
            "japanese", "david danced", "princesses", "mermaids",
        ],
    },
    # COMPARATIVE_RELIGION - religion comparativa / occcult
    {
        "category": "COMPARATIVE_RELIGION",
        "keywords": [
            "satanic", "satanism", "wicca", "wiccan", "new age",
            "paganism", "pagan", "scientology",
        ],
    },
    # INVALID real (mantener) - cultura pop explicita
    {
        "category": "INVALID",
        "keywords": [
            "reality shows", "watching tv", "watching television", "tv",
            "the planets", "life on other planets", "planets",
            "space", "outer space", "going to space",
            "web 2.0", "web 20", "internet", "social media", "facebook",
            "twitter", "instagram", "tiktok",
            "video games", "nintendo", "playstation", "xbox", "wii",
            "harry potter", "star wars", "anime", "manga", "disney",
            "pokemon", "transformers",
            "ufo", "ufos", "aliens", "extraterrestrial", "bigfoot",
            "loch ness", "mysticism", "illuminati", "freemasons",
            "branding", "logos",
            "juices", "orange juice", "pizza",
            "britney spears", "paris hilton", "oj simpson",
            "hillary clinton", "obama", "trump",
            "japanese cartoons", "simpsons",
            "pillow", "clicks", "heavy metal",
        ],
    },
]


def normalize(text: str) -> str:
    t = text.lower().strip()
    t = re.sub(r"[^\w\s-]", " ", t)
    t = re.sub(r"\s+", " ", t)
    return t.strip()


def reclassify_invalid_cluster(cluster: dict) -> str | None:
    """Retorna nueva categoria si el cluster es reclasificable."""
    members_text = " | ".join(
        normalize(m["topic_en"]) for m in cluster["members"]
    )
    for rule in RECLASSIFY_RULES:
        cat = rule["category"]
        for kw in rule["keywords"]:
            kw_norm = normalize(kw)
            if not kw_norm or len(kw_norm) < 3:
                continue
            # Match como palabra completa
            if re.search(rf"\b{re.escape(kw_norm)}\b", members_text):
                if cat == "INVALID":
                    return "INVALID"
                return cat
    return None


def main() -> None:
    print("=" * 60)
    print("Biblion: Fase 4.5 - Reclasificacion heuristica de INVALID")
    print("=" * 60)
    print()

    with (OUT_DIR / "topic_clusters_classified.json").open(encoding="utf-8") as f:
        data = json.load(f)

    reclassified: list[dict] = []
    still_invalid: list[dict] = []
    cat_dist: dict[str, int] = {}

    for cluster in data["clusters"]:
        if cluster["category"] != "INVALID":
            continue
        new_cat = reclassify_invalid_cluster(cluster)
        if new_cat is None or new_cat == "INVALID":
            still_invalid.append(cluster)
        else:
            cluster["category"] = new_cat
            cluster["category_reclassified"] = True
            reclassified.append(cluster)

    # Recalcular distribucion
    for c in data["clusters"]:
        cat_dist[c["category"]] = cat_dist.get(c["category"], 0) + 1

    print(f"Clusters INVALID originales:    {len(reclassified) + len(still_invalid)}")
    print(f"  Reclasificados:               {len(reclassified)}")
    print(f"  INVALID finales:              {len(still_invalid)}")

    print(f"\nDistribucion reclasificada:")
    for cat, count in sorted(cat_dist.items(), key=lambda x: -x[1]):
        print(f"  {cat:>20}: {count:>4} clusters")

    # Detalle de reclasificaciones
    by_target: dict[str, list[str]] = {}
    for cl in reclassified:
        by_target.setdefault(cl["category"], []).append(
            cl["members"][0]["topic_en"]
        )

    print(f"\n=== Reclasificaciones por destino ===")
    for cat, topics in sorted(by_target.items(), key=lambda x: -len(x[1])):
        print(f"\n  {cat} ({len(topics)} clusters):")
        for t in topics[:10]:
            print(f"    - {t}")
        if len(topics) > 10:
            print(f"    ... +{len(topics) - 10} mas")

    # Actualizar data
    data["category_distribution"] = cat_dist
    data["n_invalid_final"] = len(still_invalid)

    # Guardar
    with (OUT_DIR / "topic_clusters_classified.json").open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"\nActualizado: topic_clusters_classified.json")

    reclass_path = OUT_DIR / "invalid_reclassified.json"
    with reclass_path.open("w", encoding="utf-8") as f:
        json.dump(
            {
                "n_reclassified": len(reclassified),
                "n_still_invalid": len(still_invalid),
                "by_target": by_target,
                "still_invalid": [c["members"][0]["topic_en"] for c in still_invalid],
            },
            f,
            ensure_ascii=False,
            indent=2,
        )
    print(f"Guardado: {reclass_path}")


if __name__ == "__main__":
    main()

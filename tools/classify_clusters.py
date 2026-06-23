"""
Fase 4: Clasificacion automatica de clusters en 12 categorias Biblion.

Approach:
- Carga topic_clusters.json.
- Para cada cluster, analiza keywords de los miembros.
- Matchea contra 12 categorias (keywords en espanol/ingles).
- Marca como PENDING cuando no se puede clasificar.
- Genera topic_clusters_classified.json para revision.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

OUT_DIR = Path("tools")

CATEGORIES = {
    "DOCTRINE": {
        "description": "Doctrina cristiana (salvacion, gracia, fe, redencion, etc.)",
        "keywords": [
            "salvation", "saved", "redemption", "atonement", "justification",
            "sanctification", "sanctify", "reconciliation",
            "theology", "doctrine", "creed", "creeds",
            "trinity", "the trinity", "the holy trinity",
            "incarnation", "the incarnation",
            "gospel", "the gospel", "evangelism", "evangelism",
            "the great commission", "the gospel of jesus christ",
            "soteriology", "eschatology", "christology", "pneumatology",
            "predestination", "election", "free will", "freewill",
            "repentance", "repent", "repentance and faith",
            "grace", "gracious", "the grace of god",
            "covenant", "covenants", "new covenant", "old covenant",
            "law and grace", "law and gospel",
            "original sin", "total depravity",
            "the blood of jesus", "blood of christ", "the blood",
            "sacrament", "sacraments", "baptism", "eucharist", "communion",
            "the lord's supper", "the lord supper",
            "second coming", "second coming of christ",
            "the rapture", "the millennium", "end times", "endtimes",
            "last days", "final judgment", "final judgement",
            "the cross", "the crucifixion", "the resurrection",
            "the ascension", "the holy spirit", "holy spirit",
            "jesus christ", "christ jesus", "lord jesus christ",
            "son of god", "son of man", "the messiah",
            "the saviour", "the savior",
        ],
    },
    "CHRISTIAN_LIFE": {
        "description": "Vida cristiana practica (oracion, fe, obediencia, discipulado)",
        "keywords": [
            "prayer", "prayers", "praying", "prayer life",
            "faith", "faithful", "faithfulness",
            "obedience", "obedient", "obeying",
            "discipleship", "disciple", "disciples", "making disciples",
            "spiritual growth", "spiritual disciplines",
            "spiritual life", "christian living", "christian life",
            "the fruit of the spirit", "fruits of the spirit",
            "patience", "love", "hope", "joy", "peace", "kindness",
            "self-control", "self control", "gentleness", "goodness",
            "forgiveness", "forgiving", "forgive", "forgiven",
            "worship", "praise", "praising",
            "meditation", "meditating", "meditate",
            "bible study", "bible reading", "studying the bible",
            "studying scripture", "scripture memorization",
            "fasting", "tithing", "tithe", "tithes", "offering",
            "stewardship", "service", "serving",
            "humility", "humble", "humbleness",
            "trust", "trusting god", "trusting in god",
            "perseverance", "patience", "endurance",
            "loving one another", "loving others", "loving your neighbor",
            "loving god", "loving jesus",
            "the fear of the lord", "fear of the lord",
            "holiness", "holy living", "sanctified living",
            "witnessing", "sharing your faith", "witness",
            "devotion", "devotional", "devotionals",
            "quiet time", "quiet times",
        ],
    },
    "PERSON": {
        "description": "Personas biblicas o tematicas",
        "keywords": [
            "abraham", "moses", "david", "solomon", "elijah", "elisha",
            "isaiah", "jeremiah", "ezekiel", "daniel", "hosea", "amos",
            "isaac", "jacob", "joseph", "samuel", "saul", "paul", "peter",
            "john", "james", "matthew", "mark", "luke", "mary", "martha",
            "noah", "job", "ruth", "esther", "nehemiah", "ezra",
            "jesus", "christ", "the lord",
            "joshua", "caleb", "gideon", "samson", "samuel",
            "barnabas", "timothy", "titus", "silas", "luke",
            "apostles", "prophets", "patriarchs",
            "apostle paul", "apostle peter", "apostle john",
            "sons of god", "children of israel", "people of israel",
            "the disciples", "the twelve disciples",
        ],
    },
    "PLACE": {
        "description": "Lugares geograficos (biblicos y modernos relevantes)",
        "keywords": [
            "jerusalem", "israel", "palestine", "judea", "galilee",
            "samaria", "nazareth", "bethlehem", "jordan", "egypt",
            "babylon", "nineveh", "tyre", "sidon", "damascus",
            "capernaum", "jericho", "bethany", "cana", "golgotha",
            "mount sinai", "mt. sinai", "mount zion", "mt. zion",
            "mount olives", "mt. olives", "mount of olives",
            "temple", "the temple", "tabernacle", "the tabernacle",
            "promised land", "holy land",
            "garden of eden", "eden", "garden of gethsemane",
            "red sea", "dead sea", "sea of galilee",
            "earth", "land", "country", "nation", "city", "town",
        ],
    },
    "EVENT": {
        "description": "Eventos biblicos historicos",
        "keywords": [
            "creation", "the creation", "the fall", "fall of man",
            "the flood", "noah's flood", "noah flood", "the great flood",
            "exodus", "the exodus", "passover", "the passover",
            "the crucifixion", "the resurrection", "the ascension",
            "pentecost", "day of pentecost", "the day of pentecost",
            "the second coming", "the rapture", "the last judgment",
            "the last supper", "the last supper", "last supper",
            "baptism of jesus", "jesus baptism", "the temptation of jesus",
            "the transfiguration", "the transfiguration of jesus",
            "destruction of the temple", "the exile", "the babylonian exile",
            "the conquest", "the conquest of canaan",
            "the reformation", "reformation",
        ],
    },
    "PROPHECY": {
        "description": "Profecias y escatologia",
        "keywords": [
            "prophecy", "prophecies", "prophetic", "prophet",
            "end times", "endtimes", "last days",
            "the rapture", "the tribulation", "great tribulation",
            "the millennium", "millennial", "millennium",
            "the second coming", "second coming of christ",
            "the antichrist", "antichrist", "the mark of the beast",
            "mark of the beast", "666", "the beast",
            "the book of revelation", "revelation",
            "book of revelation", "the apocalypse", "apocalypse",
            "new jerusalem", "the new jerusalem",
            "new heaven", "new earth", "new heavens",
            "the return of christ", "return of jesus",
            "the day of the lord", "day of the lord",
            "daniel's 70 weeks", "seventy weeks", "70 weeks",
            "the olive tree", "the fig tree", "fig tree",
            "the bride of christ", "the bride", "the wedding supper",
            "israel and prophecy", "prophecy and israel",
        ],
    },
    "BOOK": {
        "description": "Libros de la Biblia",
        "keywords": [
            "genesis", "exodus", "leviticus", "numbers", "deuteronomy",
            "joshua", "judges", "ruth", "samuel", "kings", "chronicles",
            "ezra", "nehemiah", "esther", "job", "psalms", "proverbs",
            "ecclesiastes", "song of solomon", "isaiah", "jeremiah",
            "lamentations", "ezekiel", "daniel", "hosea", "joel", "amos",
            "obadiah", "jonah", "micah", "nahum", "habakkuk", "zephaniah",
            "haggai", "zechariah", "malachi",
            "matthew", "mark", "luke", "john", "acts", "romans",
            "corinthians", "galatians", "ephesians", "philippians",
            "colossians", "thessalonians", "timothy", "titus", "philemon",
            "hebrews", "james", "peter", "revelation",
        ],
    },
    "ATTRIBUTE_OF_GOD": {
        "description": "Atributos de Dios (amor, misericordia, justicia)",
        "keywords": [
            "love of god", "god's love", "the love of god",
            "mercy of god", "god's mercy", "mercies of god",
            "grace of god", "god's grace", "the grace of god",
            "holiness of god", "god's holiness", "the holiness of god",
            "justice of god", "god's justice",
            "wrath of god", "god's wrath", "the wrath of god",
            "sovereignty", "sovereignty of god", "god's sovereignty",
            "omniscience", "omnipresence", "omnipotence",
            "god is love", "god is holy", "god is just",
            "the goodness of god", "goodness of god", "god's goodness",
            "the faithfulness of god", "faithfulness of god",
            "the power of god", "power of god", "god's power",
            "the wisdom of god", "wisdom of god",
            "the glory of god", "glory of god", "god's glory",
            "attributes of god",
        ],
    },
    "SIN": {
        "description": "Pecados y vicios",
        "keywords": [
            "sin", "sins", "sinful", "sinfulness",
            "evil", "wickedness", "transgression", "transgressions",
            "iniquity", "iniquities", "trespass", "trespasses",
            "unrighteousness", "lawlessness",
            "temptation", "tempted", "temptations",
            "lust", "lusts", "lustful", "lusting",
            "greed", "greedy", "covetousness",
            "pride", "prideful", "arrogance", "arrogant",
            "anger", "wrath", "rage",
            "hatred", "hate", "enmity", "enemies",
            "lying", "lies", "liars", "falsehood", "deceit",
            "theft", "stealing", "thief",
            "murder", "killing", "violence",
            "adultery", "fornication", "immorality", "sexual immorality",
            "pornography", "prostitution",
            "homosexuality", "homosexual",
            "drunkenness", "drunk", "drunkard", "alcoholism", "alcohol abuse",
            "drug abuse", "addiction", "addictions",
            "idolatry", "idol worship", "idols",
            "witchcraft", "sorcery", "occult", "occultism",
            "jealousy", "envy", "envious",
            "gossip", "slander", "backbiting",
            "selfishness", "selfish",
        ],
    },
    "CHURCH": {
        "description": "Iglesia, gobierno, comunidad cristiana",
        "keywords": [
            "church", "the church", "churches",
            "worship service", "church service", "church services",
            "sunday service", "sabbath", "the sabbath",
            "pastor", "pastors", "pastoral",
            "elder", "elders", "deacon", "deacons",
            "bishop", "bishops", "overseer",
            "preaching", "preach", "sermon", "sermons",
            "pulpit", "altar call",
            "denomination", "denominations",
            "the body of christ", "body of christ",
            "fellowship", "fellowship with god", "fellowship with believers",
            "small group", "small groups", "bible study group",
            "ministry", "ministries", "mission", "missions", "missionary",
            "missions work", "mission work", "missionary work",
            "worship team", "worship leader", "choir", "praise team",
            "government", "voting", "election", "elections", "politics",
            "religious freedom", "religious liberty", "persecution",
            "unity", "reconciliation", "peace",
        ],
    },
    "COMPARATIVE_RELIGION": {
        "description": "Religiones comparativas, no-cristianas",
        "keywords": [
            "islam", "muslims", "muslim", "the prophet muhammad",
            "muhammad", "allah", "quran", "koran",
            "buddhism", "buddha", "buddhist", "nirvana",
            "hinduism", "hindu", "krishna", "brahma", "shiva",
            "judaism", "jews", "the jews", "jewish", "rabbi",
            "sikhism", "sikh", "taoism", "tao", "confucianism",
            "the mormons", "latter day saints", "book of mormon",
            "mormon", "mormonism", "lds",
            "jehovah's witnesses", "jehovah witnesses", "jw",
            "scientology", "satanism", "wicca", "wiccan",
            "new age", "paganism", "pagan",
        ],
    },
    "INVALID": {
        "description": "Ruido / no biblico",
        "keywords": [
            "video games", "anime", "manga", "movies", "tv", "television",
            "celebrities", "celebrity",
            "rock music", "rap", "rock and roll", "pop music",
            "skateboarding", "surfing", "baseball", "basketball",
            "football", "soccer", "wrestling", "boxing", "nfl", "nba",
            "evolution", "dinosaurs", "dinosaur", "monkey", "apes",
            "nasa", "ufo", "ufos", "aliens", "extraterrestrial",
            "facebook", "twitter", "instagram", "tiktok", "social media",
            "harry potter", "star wars", "nintendo", "playstation", "xbox",
            "wii", "cars", "disney", "pizza",
            "britney spears", "paris hilton", "oj simpson",
        ],
    },
}


def normalize(text: str) -> str:
    t = text.lower().strip()
    t = re.sub(r"[^\w\s-]", " ", t)
    t = re.sub(r"\s+", " ", t)
    return t.strip()


def score_cluster(cluster: dict) -> dict[str, int]:
    """Cuenta cuantas keywords de cada categoria aparecen en los miembros."""
    scores: dict[str, int] = {cat: 0 for cat in CATEGORIES}
    members_text = " | ".join(m["topic_en"].lower() for m in cluster["members"])
    for cat, defn in CATEGORIES.items():
        for kw in defn["keywords"]:
            kw_norm = normalize(kw)
            if not kw_norm:
                continue
            # Match exacto o como palabra completa
            if re.search(rf"\b{re.escape(kw_norm)}\b", members_text):
                scores[cat] += 1
    return scores


def classify_cluster(cluster: dict) -> tuple[str, int]:
    """Retorna (categoria, score). Si ninguna supera umbral, PENDING."""
    scores = score_cluster(cluster)
    best_cat = max(scores, key=lambda c: scores[c])
    best_score = scores[best_cat]
    if best_score == 0:
        return ("PENDING", 0)
    return (best_cat, best_score)


def main() -> None:
    print("=" * 60)
    print("Biblion: Fase 4 - Clasificacion automatica de clusters")
    print("=" * 60)
    print()

    with (OUT_DIR / "topic_clusters.json").open(encoding="utf-8") as f:
        data = json.load(f)

    print(f"Total clusters: {data['n_clusters']}")

    classified: list[dict] = []
    pending: list[dict] = []
    category_dist: dict[str, int] = {cat: 0 for cat in CATEGORIES}
    category_dist["PENDING"] = 0

    for cluster in data["clusters"]:
        cat, score = classify_cluster(cluster)
        category_dist[cat] += 1
        cluster_out = {
            **cluster,
            "category": cat,
            "category_score": score,
        }
        if cat == "PENDING":
            pending.append(cluster_out)
        else:
            classified.append(cluster_out)

    print(f"\nDistribucion por categoria:")
    for cat, count in sorted(category_dist.items(), key=lambda x: -x[1]):
        print(f"  {cat:>25}: {count:>4} clusters")

    # Salida 1: clusters clasificados
    out_classified = {
        "n_clusters": data["n_clusters"],
        "n_classified": len(classified),
        "n_pending": len(pending),
        "category_distribution": category_dist,
        "clusters": classified + pending,
    }

    out_path = OUT_DIR / "topic_clusters_classified.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(out_classified, f, ensure_ascii=False, indent=2)
    print(f"\nGuardado: {out_path}")

    # Salida 2: solo los PENDING para revision manual
    out_pending = {
        "description": "Clusters que no pudieron clasificarse automaticamente. Requieren revision manual o Worker IA.",
        "n_pending": len(pending),
        "pending": pending,
    }
    pending_path = OUT_DIR / "clusters_pending_review.json"
    with pending_path.open("w", encoding="utf-8") as f:
        json.dump(out_pending, f, ensure_ascii=False, indent=2)
    print(f"Guardado: {pending_path}")

    # Muestra
    print(f"\n=== Top 10 PENDING (muestra) ===")
    for cl in pending[:10]:
        m = cl["members"][0]
        print(f"  [size={cl['size']:>2}] {m['topic_en']}")

    print(f"\n=== Top 10 INVALID ===")
    inv_clusters = [c for c in classified if c["category"] == "INVALID"]
    for cl in inv_clusters[:10]:
        m = cl["members"][0]
        print(f"  [size={cl['size']:>2}] {m['topic_en']}")


if __name__ == "__main__":
    main()

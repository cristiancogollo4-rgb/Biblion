"""
Fase 4 (v2): Clasificacion de clusters por centroides de embedding.

- Define seeds (topics representativos) por cada categoria.
- Calcula embedding promedio de cada set de seeds = centroide.
- Para cada cluster, calcula su centroide y lo asigna a la categoria
  con mayor cosine similarity.
- Si ninguna categoria supera umbral minimo, queda PENDING.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity

OUT_DIR = Path("tools")
MODEL_NAME = "BAAI/bge-small-en-v1.5"
MIN_SIM = 0.78  # similitud minima con la mejor categoria
SECONDARY_GAP = 0.0  # gap minimo entre la mejor y la segunda (0 = cualquier)


SEED_TOPICS: dict[str, list[str]] = {
    "DOCTRINE": [
        "salvation", "the gospel", "the cross", "atonement", "redemption",
        "justification", "sanctification", "grace", "the trinity",
        "the incarnation", "the resurrection", "predestination",
        "election", "repentance", "reconciliation", "covenant",
        "the blood of jesus", "the blood of christ", "christology",
        "soteriology", "original sin", "free will", "freewill",
        "the great commission", "the gospel of jesus christ",
        "the crucifixion", "the ascension", "second coming",
    ],
    "CHRISTIAN_LIFE": [
        "prayer", "faith", "prayer life", "bible study",
        "discipleship", "obedience", "spiritual growth",
        "christian living", "christian life", "the fruit of the spirit",
        "worship", "tithing", "tithe", "stewardship", "fasting",
        "meditation", "meditating", "devotion", "devotional",
        "bible reading", "studying the bible", "studying scripture",
        "quiet time", "spiritual disciplines", "making disciples",
        "witnessing", "sharing your faith", "the fear of the lord",
        "loving one another", "loving your neighbor", "loving god",
        "loving jesus", "trusting god", "trusting in god",
    ],
    "PERSON": [
        "abraham", "moses", "david", "solomon", "elijah", "elisha",
        "isaiah", "jeremiah", "ezekiel", "daniel", "hosea", "amos",
        "isaac", "jacob", "joseph", "samuel", "saul", "paul", "peter",
        "john", "james", "matthew", "mark", "luke", "mary", "martha",
        "noah", "job", "ruth", "esther", "nehemiah", "ezra",
        "jesus", "christ", "the lord", "joshua", "caleb", "gideon",
        "samson", "barnabas", "timothy", "titus", "silas",
        "apostle paul", "apostle peter", "apostle john",
        "the disciples", "the twelve disciples",
    ],
    "PLACE": [
        "jerusalem", "israel", "palestine", "judea", "galilee",
        "samaria", "nazareth", "bethlehem", "jordan", "egypt",
        "babylon", "nineveh", "tyre", "sidon", "damascus",
        "capernaum", "jericho", "bethany", "cana", "golgotha",
        "mount sinai", "mount zion", "mount of olives",
        "the temple", "the tabernacle", "promised land", "holy land",
        "garden of eden", "garden of gethsemane", "red sea", "dead sea",
        "sea of galilee",
    ],
    "EVENT": [
        "creation", "the fall", "the flood", "the exodus", "passover",
        "the crucifixion", "the resurrection", "the ascension",
        "pentecost", "the second coming", "the rapture",
        "the last judgment", "the last supper", "baptism of jesus",
        "the temptation of jesus", "the transfiguration",
        "destruction of the temple", "the exile", "the babylonian exile",
        "the reformation",
    ],
    "PROPHECY": [
        "prophecy", "prophecies", "prophetic", "prophet",
        "end times", "endtimes", "last days", "the tribulation",
        "great tribulation", "the millennium", "millennial",
        "the antichrist", "antichrist", "the mark of the beast",
        "mark of the beast", "the beast", "the book of revelation",
        "revelation", "the apocalypse", "apocalypse",
        "new jerusalem", "new heaven", "new earth", "new heavens",
        "the return of christ", "return of jesus", "the day of the lord",
        "daniel's 70 weeks", "seventy weeks", "70 weeks",
        "the bride of christ", "the wedding supper",
    ],
    "BOOK": [
        "genesis", "exodus", "leviticus", "numbers", "deuteronomy",
        "joshua", "judges", "ruth", "psalms", "proverbs",
        "ecclesiastes", "song of solomon", "lamentations",
        "obadiah", "jonah", "micah", "nahum", "habakkuk", "zephaniah",
        "haggai", "zechariah", "malachi", "philemon",
        "hebrews", "revelation",
    ],
    "ATTRIBUTE_OF_GOD": [
        "love of god", "god's love", "the love of god",
        "mercy of god", "god's mercy", "mercies of god",
        "grace of god", "god's grace",
        "holiness of god", "god's holiness",
        "justice of god", "wrath of god", "god's wrath",
        "sovereignty of god", "god's sovereignty",
        "omniscience", "omnipresence", "omnipotence",
        "god is love", "god is holy", "goodness of god",
        "faithfulness of god", "power of god", "wisdom of god",
        "glory of god", "attributes of god",
    ],
    "SIN": [
        "sin", "sins", "sinful", "evil", "wickedness", "transgression",
        "iniquity", "trespass", "lawlessness", "temptation", "tempted",
        "lust", "lusts", "lustful", "greed", "greedy", "covetousness",
        "pride", "prideful", "arrogance", "arrogant", "anger", "wrath",
        "rage", "hatred", "hate", "lying", "lies", "liars",
        "theft", "stealing", "murder", "killing", "adultery",
        "fornication", "immorality", "sexual immorality", "pornography",
        "prostitution", "homosexuality", "homosexual", "drunkenness",
        "drunk", "alcoholism", "alcohol abuse", "drug abuse", "addiction",
        "idolatry", "idol worship", "idols", "witchcraft", "sorcery",
        "occult", "jealousy", "envy", "gossip", "slander", "selfishness",
    ],
    "CHURCH": [
        "church", "the church", "churches", "worship service",
        "church service", "sunday service", "sabbath", "pastor",
        "pastors", "elder", "elders", "deacon", "deacons", "bishop",
        "preaching", "sermon", "sermons", "pulpit", "altar call",
        "denomination", "the body of christ", "fellowship",
        "small group", "bible study group", "ministry", "ministries",
        "mission", "missions", "missionary", "worship team", "choir",
        "government", "voting", "election", "religious freedom",
        "religious liberty", "persecution", "unity",
    ],
    "COMPARATIVE_RELIGION": [
        "islam", "muslims", "muslim", "the prophet muhammad", "muhammad",
        "allah", "quran", "koran", "buddhism", "buddha", "buddhist",
        "nirvana", "hinduism", "hindu", "krishna", "brahma", "shiva",
        "judaism", "jews", "the jews", "jewish", "rabbi",
        "the mormons", "latter day saints", "book of mormon", "mormon",
        "mormonism", "lds", "jehovah's witnesses", "jehovah witnesses",
        "scientology", "satanism", "wicca", "wiccan", "new age",
        "paganism", "pagan",
    ],
    "INVALID": [
        "video games", "anime", "manga", "movies", "tv", "television",
        "celebrities", "celebrity", "rock music", "rap", "rock and roll",
        "skateboarding", "surfing", "baseball", "basketball", "football",
        "soccer", "wrestling", "boxing", "nfl", "nba", "evolution",
        "dinosaurs", "dinosaur", "monkey", "apes", "nasa", "ufo", "ufos",
        "aliens", "facebook", "twitter", "instagram", "tiktok",
        "social media", "harry potter", "star wars", "nintendo",
        "playstation", "xbox", "cars", "disney", "pizza",
        "britney spears", "paris hilton", "oj simpson",
        "horoscope", "astrology", "tarot cards",
    ],
}


def main() -> None:
    print("=" * 60)
    print("Biblion: Fase 4 v2 - Clasificacion por centroides")
    print("=" * 60)
    print()

    # 1. Cargar embeddings y clusters
    embeddings = np.load(OUT_DIR / "topic_embeddings.npy")
    with (OUT_DIR / "topic_clusters.json").open(encoding="utf-8") as f:
        data = json.load(f)
    with (OUT_DIR / "topics_candidates.json").open(encoding="utf-8") as f:
        cand = json.load(f)
    cand_list = cand["topics"]
    text_to_idx = {c["topic_en"]: i for i, c in enumerate(cand_list)}

    print(f"Embeddings: {embeddings.shape}")
    print(f"Clusters: {data['n_clusters']}")

    # 2. Calcular centroides de cada categoria
    print(f"\n[1/3] Calculando centroides de {len(SEED_TOPICS)} categorias...")
    model = SentenceTransformer(MODEL_NAME, device="cpu")
    centroids: dict[str, np.ndarray] = {}
    centroid_info: dict[str, dict] = {}

    for cat, seeds in SEED_TOPICS.items():
        # Embed cada seed con prefijo biblico
        seed_texts = [f"Biblical concept: {s}" for s in seeds]
        seed_embs = model.encode(
            seed_texts,
            batch_size=32,
            convert_to_numpy=True,
            normalize_embeddings=False,
            show_progress_bar=False,
        )
        centroid = seed_embs.mean(axis=0)
        # Renormalizar el centroide
        centroid = centroid / np.linalg.norm(centroid)
        centroids[cat] = centroid
        # Stats: similitud media de los seeds al centroide
        sims_to_center = cosine_similarity(seed_embs, centroid.reshape(1, -1)).flatten()
        centroid_info[cat] = {
            "n_seeds": len(seeds),
            "mean_sim": float(sims_to_center.mean()),
            "min_sim": float(sims_to_center.min()),
            "max_sim": float(sims_to_center.max()),
        }
        print(f"  {cat:>20}: n={len(seeds):>2}, "
              f"mean_sim={centroid_info[cat]['mean_sim']:.3f}, "
              f"min={centroid_info[cat]['min_sim']:.3f}")

    # 3. Para cada cluster, calcular su centroide y clasificar
    print(f"\n[2/3] Clasificando {data['n_clusters']} clusters...")
    classified: list[dict] = []
    pending: list[dict] = []
    category_dist: dict[str, int] = {cat: 0 for cat in SEED_TOPICS}
    category_dist["PENDING"] = 0

    cat_names = list(centroids.keys())
    centroid_matrix = np.stack([centroids[c] for c in cat_names], axis=0)

    for cluster in data["clusters"]:
        # Embedding centroide del cluster (promedio de sus miembros)
        member_idxs = [
            text_to_idx[m["topic_en"]]
            for m in cluster["members"]
            if m["topic_en"] in text_to_idx
        ]
        if not member_idxs:
            cluster_emb = None
        else:
            member_embs = embeddings[member_idxs]
            cluster_emb = member_embs.mean(axis=0)
            cluster_emb = cluster_emb / np.linalg.norm(cluster_emb)

        if cluster_emb is None:
            cat, sim_val, gap = "PENDING", 0.0, 0.0
        else:
            sims = cosine_similarity(
                cluster_emb.reshape(1, -1), centroid_matrix
            ).flatten()
            best_idx = int(sims.argmax())
            second_idx = int(np.argsort(sims)[-2])
            sim_val = float(sims[best_idx])
            gap = sim_val - float(sims[second_idx])
            if sim_val < MIN_SIM or gap < SECONDARY_GAP:
                cat = "PENDING"
            else:
                cat = cat_names[best_idx]

        category_dist[cat] += 1
        cluster_out = {
            **cluster,
            "category": cat,
            "category_sim": round(sim_val, 4),
            "category_gap": round(gap, 4),
        }
        if cat == "PENDING":
            pending.append(cluster_out)
        else:
            classified.append(cluster_out)

    print(f"\n[3/3] Distribucion por categoria:")
    for cat, count in sorted(category_dist.items(), key=lambda x: -x[1]):
        print(f"  {cat:>20}: {count:>4} clusters")

    # 4. Guardar
    out_data = {
        "n_clusters": data["n_clusters"],
        "n_classified": len(classified),
        "n_pending": len(pending),
        "min_sim_threshold": MIN_SIM,
        "secondary_gap_threshold": SECONDARY_GAP,
        "centroid_info": centroid_info,
        "category_distribution": category_dist,
        "clusters": classified + pending,
    }
    out_path = OUT_DIR / "topic_clusters_classified.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(out_data, f, ensure_ascii=False, indent=2)
    print(f"\nGuardado: {out_path}")

    out_pending = {
        "description": "Clusters que no superaron umbral de similitud. Requieren revision manual o Worker IA.",
        "n_pending": len(pending),
        "pending": pending,
    }
    pending_path = OUT_DIR / "clusters_pending_review.json"
    with pending_path.open("w", encoding="utf-8") as f:
        json.dump(out_pending, f, ensure_ascii=False, indent=2)
    print(f"Guardado: {pending_path}")

    # 5. Muestra
    print(f"\n=== Top 15 PENDING (muestra) ===")
    for cl in pending[:15]:
        m = cl["members"][0]
        print(f"  [size={cl['size']:>2}, sim={cl['category_sim']:.3f}, gap={cl['category_gap']:.3f}] {m['topic_en']}")

    print(f"\n=== Top 15 INVALID ===")
    inv = [c for c in classified if c["category"] == "INVALID"]
    for cl in inv[:15]:
        m = cl["members"][0]
        print(f"  [size={cl['size']:>2}] {m['topic_en']}")


if __name__ == "__main__":
    main()

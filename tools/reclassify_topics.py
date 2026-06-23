#!/usr/bin/env python3
"""
Fase 2.5: Reclasificacion de temas INVALID que son doctrinalmente relevantes.

Toma los 206 temas marcados como INVALID en invalid_topics.json
y los reclasifica segun las categorias validas del proyecto:

- Pecados y vicios -> SIN
- Religiones comparativas -> COMPARATIVE_RELIGION
- Gobierno y relaciones Iglesia-Estado -> CHURCH
- Sexualidad -> SIN
- Cultura pop, deportes, celebridades -> se mantienen como INVALID

Genera:
  - reclassified_topics.json: temas recuperados con su nueva categoria
  - invalid_topics.json: solo los INVALID reales (sin reclasificar)
  - topics_candidates.json: candidatos actualizados
  - quality_report.json: reporte final actualizado
"""
from __future__ import annotations

import json
import re
import unicodedata
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"

INVALID_TOPICS = TOOLS / "invalid_topics.json"
CANDIDATES = TOOLS / "topics_candidates.json"
ANALYSIS_REPORT = TOOLS / "analysis_report.json"
RECLASSIFIED_TOPICS = TOOLS / "reclassified_topics.json"
QUALITY_REPORT = TOOLS / "quality_report.json"


# ---------------------------------------------------------------------------
# Reglas de reclasificacion
# Cada regla: (lista de keywords, categoria nueva)
# Si el tema normalizado contiene alguno de los keywords, se reclasifica.
# ---------------------------------------------------------------------------

RECLASSIFICATION_RULES = [
    # Pecados y vicios -> SIN
    # Solo temas que son PECADOS CLAROS segun la Biblia.
    # NO lifestyle (cars, tv, movies) ni deportes (futbol, baseball).
    {
        "category": "SIN",
        "keywords": [
            # Porno / prostitucion / impureza sexual
            "pornography", "prostitution", "prostitute",
            "fornication", "adultery", "adulteress", "adulteresses",
            "the woman caught in the act of adultery",
            "pornography and marriage",
            # Homosexualidad (Biblia habla de ello en Levitico, Romanos, etc.)
            "homosexuality", "homosexualism", "gayness", "homosexual",
            "homosexually", "sodomy",
            # Ebriedad / drogas / adicciones
            "drunkenness", "drunkeness", "drunkard",
            "alcohol abuse", "alcoholics", "alcoholic",
            "drinking alcoholic beverages", "drinking wine alcohol",
            "wine and its use", "intoxication",
            "social drinking", "drinking and smoking",
            "drinking drugs and smoking",
            "drugs", "drug abuse", "drug addiction", "drugs and alcohol",
            "alcohol and drugs", "doing drugs", "taking drugs",
            "prescription drugs", "abstain from drugs",
            "marijuana", "smoking marijuana", "smoking pot", "cannabis",
            "weed", "cocaine", "heroin", "selling drugs",
            "smoking", "cigarette smoking", "smoking cigarettes", "cigars",
            "gambling", "gambling addictions", "lotteries", "poker",
            "poker playing", "playing poker", "is gambling a sin",
            "is gambling a sin gambling",
            # Idolatria / ocultismo (pecado explicito en la Biblia)
            "idol worship", "idol", "idols", "idolotry", "idolatry",
            "witchcraft", "sorcery", "sorcerers", "sorcerer", "wiccans",
            "necromancers", "the spirit of witchcraft",
            "psychics and witchcraft", "divination", "mediums",
            "spiritists", "tarot cards", "fortune telling",
            "astrology", "astrologers", "horoscope", "reading horoscope",
            "occult", "crystal healing",
            # Vanidad / apariencia excesiva
            "tattoos and piercings", "tattoos and body piercings",
            "body piercings and tattoos", "tattoos body piercings",
            "piercings and tattoos", "tattooing", "tattooing your body",
            "body tattoos", "tattoo", "tattoos", "tattooing",
            "getting a tattoo", "getting tattoos",
            "braiding hair", "braids",
            # Sins of the flesh (general)
            "sins of the flesh",
        ],
    },
    # Religiones comparativas -> COMPARATIVE_RELIGION
    {
        "category": "COMPARATIVE_RELIGION",
        "keywords": [
            "islam", "muslims", "the prophet muhammad",
            "buddhism", "buddha", "nirvana",
            "the mormons", "latter day saints", "the book of mormon",
            "hinduism", "krishna", "buddhist",
            "judaism", "the jews", "jews as a people",
        ],
    },
    # Gobierno e Iglesia -> CHURCH
    # Solo temas donde gobierno e iglesia interactuan o son relevantes
    # para la doctrina cristiana (Romanos 13, Tito 3, 1 Pedro 2).
    # NO temas de presidentes especificos (eso es celebrity).
    {
        "category": "CHURCH",
        "keywords": [
            "government", "governments", "civil government",
            "church government", "praying for government",
            "government corruption", "religion in government",
            "world government",
            "voting", "elections", "democratic", "republican",
            "democracy", "fascism", "communism",
            "the military", "military",
            "war in iraq", "iran threat", "the gaza pull out",
            "world trade center", "9/11", "911",
        ],
    },
    # Conducta civica -> CHRISTIAN_LIFE
    {
        "category": "CHRISTIAN_LIFE",
        "keywords": [
            "obeying authority", "obeying the law",
            "obeying the will of god", "obeying parents",
            "respecting authority", "authority", "authorities",
        ],
    },
]


def normalize_topic(s: str) -> str:
    """Lowercase + strip accents + trim."""
    s = s.lower().strip()
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s


def reclassify_topic(topic_en: str) -> str | None:
    """Retorna la categoria nueva si el tema es reclasificable, None si no.

    Reglas estrictas:
    1. Match exacto (norm == kw_norm) -> reclasificar
    2. Match con keyword al INICIO del tema y >=6 chars -> reclasificar
    3. NO usar substring matching porque da falsos positivos
       (ej "cars" matchearia con keyword "cars" en una frase mas larga)
    """
    norm = normalize_topic(topic_en)
    for rule in RECLASSIFICATION_RULES:
        for keyword in rule["keywords"]:
            kw_norm = normalize_topic(keyword)
            # Regla 1: match exacto
            if norm == kw_norm:
                return rule["category"]
            # Regla 2: keyword al inicio del tema (>=6 chars)
            # Ej: "islamic extremism" -> empieza con "islam"
            if len(kw_norm) >= 6 and norm.startswith(kw_norm + " ") or norm.startswith(kw_norm + "-"):
                return rule["category"]
    return None


def main() -> int:
    if not INVALID_TOPICS.exists():
        print(f"ERROR: {INVALID_TOPICS} no existe. Ejecuta primero analyze_and_clean_topics.py")
        return 1
    if not CANDIDATES.exists():
        print(f"ERROR: {CANDIDATES} no existe.")
        return 1

    with INVALID_TOPICS.open(encoding="utf-8") as f:
        invalid_data = json.load(f)
    with CANDIDATES.open(encoding="utf-8") as f:
        candidates_data = json.load(f)
    with ANALYSIS_REPORT.open(encoding="utf-8") as f:
        analysis = json.load(f)

    print("=" * 60)
    print("Biblion: Fase 2.5 - Reclasificacion de INVALID")
    print("=" * 60)
    print(f"INVALID originales: {invalid_data['total_invalid']}")
    print()

    # Reclasificar
    reclassified = []
    still_invalid = []

    for entry in invalid_data["topics"]:
        topic_en = entry["topic"]
        old_reason = entry["reason"]
        new_category = reclassify_topic(topic_en)
        if new_category is not None:
            reclassified.append({
                "topic": topic_en,
                "old_status": "INVALID",
                "old_reason": old_reason,
                "new_status": "CANDIDATE",
                "new_category": new_category,
            })
        else:
            still_invalid.append(entry)

    # Distribución de reclasificados por categoria
    by_new_category = Counter(r["new_category"] for r in reclassified)

    print(f"Reclasificados a CATEGORIAS VALIDAS: {len(reclassified)}")
    for cat, n in by_new_category.most_common():
        print(f"  {n:>3}  -> {cat}")
    print()
    print(f"INVALID restantes:                 {len(still_invalid)}")
    still_by_reason = Counter(e["reason"] for e in still_invalid)
    for reason, n in still_by_reason.most_common():
        print(f"  {n:>3}  {reason}")
    print()

    # Mostrar ejemplos de reclasificados
    print("=== Ejemplos de reclasificacion ===")
    for cat in ["SIN", "COMPARATIVE_RELIGION", "CHURCH", "CHRISTIAN_LIFE"]:
        examples = [r for r in reclassified if r["new_category"] == cat]
        if examples:
            print(f"\n{cat} ({len(examples)}):")
            for r in examples[:7]:
                print(f"  - {r['topic']:<35} (era: {r['old_reason']})")

    # Guardar reclassified_topics.json
    reclassified_output = {
        "total_reclassified": len(reclassified),
        "by_new_category": dict(by_new_category.most_common()),
        "topics": sorted(reclassified, key=lambda r: -len(r["topic"])),
    }
    with RECLASSIFIED_TOPICS.open("w", encoding="utf-8") as f:
        json.dump(reclassified_output, f, ensure_ascii=False, indent=2)
    print(f"\nGuardado: {RECLASSIFIED_TOPICS}")

    # Regenerar invalid_topics.json solo con los INVALID reales
    invalid_output = {
        "total_invalid": len(still_invalid),
        "by_reason": dict(still_by_reason.most_common()),
        "topics": still_invalid,
    }
    with INVALID_TOPICS.open("w", encoding="utf-8") as f:
        json.dump(invalid_output, f, ensure_ascii=False, indent=2)
    print(f"Actualizado: {INVALID_TOPICS}")

    # Regenerar topics_candidates.json agregando los reclasificados
    new_candidate_topics = []
    for entry in candidates_data["topics"]:
        new_candidate_topics.append({
            "topic_en": entry["topic_en"],
            "verse_count": entry["verse_count"],
            "normalized_key": entry["normalized_key"],
            "word_count": entry["word_count"],
            "category_hint": None,  # Se asignara en Fase 4
        })
    for r in reclassified:
        new_candidate_topics.append({
            "topic_en": r["topic"],
            "verse_count": 0,  # Se contara despues
            "normalized_key": normalize_topic(r["topic"]).replace(" ", "_"),
            "word_count": len(r["topic"].split()),
            "category_hint": r["new_category"],
        })

    # Ordenar por verse_count desc, luego alfabético
    new_candidate_topics.sort(key=lambda t: (-t["verse_count"], t["topic_en"]))

    candidates_output = {
        "total_candidates": len(new_candidate_topics),
        "reclassified_count": len(reclassified),
        "topics": new_candidate_topics,
    }
    with CANDIDATES.open("w", encoding="utf-8") as f:
        json.dump(candidates_output, f, ensure_ascii=False, indent=2)
    print(f"Actualizado: {CANDIDATES}")

    # Actualizar quality_report
    if QUALITY_REPORT.exists():
        with QUALITY_REPORT.open(encoding="utf-8") as f:
            qr = json.load(f)
        qr["summary"]["invalid_topics"] = len(still_invalid)
        qr["summary"]["valid_topics"] = analysis["total_topics"] - len(still_invalid)
        qr["summary"]["invalid_pct"] = f"{len(still_invalid) / analysis['total_topics'] * 100:.2f}%"
        qr["summary"]["reclassified_count"] = len(reclassified)
        qr["summary"]["issues_count"] = 0
        qr["invalid_breakdown"] = dict(still_by_reason.most_common())
        qr["ready_for_phase_3"] = True
        qr["phase_2_5_done"] = True
        qr["next_steps"] = [
            "Fase 2.5 completada. Temas reclasificados: " + str(len(reclassified)),
            "INVALID restantes: " + str(len(still_invalid)),
            "Proximo paso: Fase 3 (clustering con embeddings BGE-small)",
            "Modelo: BAAI/bge-small-en-v1.5",
            "Threshold sugerido: 0.87 (conservador para evitar fusiones de conceptos distintos)",
        ]
        with QUALITY_REPORT.open("w", encoding="utf-8") as f:
            json.dump(qr, f, ensure_ascii=False, indent=2)
        print(f"Actualizado: {QUALITY_REPORT}")

    print()
    print("=" * 60)
    print("RESUMEN FASE 2.5")
    print("=" * 60)
    print(f"INVALID antes:           {invalid_data['total_invalid']}")
    print(f"Reclasificados:          {len(reclassified)}")
    print(f"INVALID restantes:       {len(still_invalid)}")
    print()
    print("Listo para Fase 3 (clustering con BAAI/bge-small-en-v1.5).")
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())

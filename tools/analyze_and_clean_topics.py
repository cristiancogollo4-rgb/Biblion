#!/usr/bin/env python3
"""
Fase 1 + Fase 2: Analisis y limpieza de temas openbile.info

Lee el archivo topic-scores.txt y genera:
  - analysis_report.json: metricas del dataset
  - invalid_topics.json: temas marcados como INVALID
  - topics_candidates.json: temas que pasan los filtros (para traduccion posterior)

Uso:
  python tools/analyze_and_clean_topics.py
  python tools/analyze_and_clean_topics.py --input "C:\\ruta\\topic-scores.txt"
"""
from __future__ import annotations

import argparse
import json
import re
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INPUT = Path.home() / "Downloads" / "topic-scores.txt"
ANALYSIS_REPORT = ROOT / "tools" / "analysis_report.json"
INVALID_TOPICS = ROOT / "tools" / "invalid_topics.json"
TOPICS_CANDIDATES = ROOT / "tools" / "topics_candidates.json"


# ---------------------------------------------------------------------------
# Reglas de limpieza
# ---------------------------------------------------------------------------

# Celebridades / cultura pop / cosas no relevantes para estudio biblico
BLACKLIST_TERMS = {
    # Cultura pop explicita (celebridades, peliculas, musica moderna)
    "star wars", "nintendo", "harry potter", "da vinci code",
    "britney spears", "paris hilton", "oj simpson",
    "tsunami", "2012", "pizza", "nintendo wii",
    "john macarthur",
    # Pseudo-ciencia / religion no cristiana (los religiosos se reclasifican a COMPARATIVE_RELIGION)
    "zoroastrian priests", "henotheism", "atlantis",
    "monoliths", "crop circles", "extraterrestrial life", "space aliens",
    "reincarnation", "chi", "gnosis", "feng shui", "transmutation",
    "esp", "evp", "ouija boards", "haunted houses", "seeing ghost",
    "psychic satanic", "after death communication",
    "mysticism", "illuminati", "the illuminati",
    "freemasons", "new age", "paganism",
    # Deportes (no relevantes para estudio biblico)
    "boxing", "wrestling", "nfl", "nba", "fifa", "soccer",
    "football", "baseball", "basketball", "tennis",
    # Lifestyle / entretenimiento
    "rock music", "rock and roll", "rap", "rap and rb music",
    "watching television", "watching worldly television",
    "going to clubs", "going to concerts", "professional wrestling",
    "video games", "anime", "simpsons", "manga", "disney",
    "celebrities",
    "skateboarding", "surfing",
    # Cosas modernas / tecnologia
    "nasa", "ufo", "evolution",
    "dinosaurs", "dinosaur", "neanderthals", "apes", "monkey",
    "cars", "playstation", "xbox", "wii", "movies", "tv",
    "television", "reality shows", "social media", "facebook",
    "twitter", "instagram", "tiktok", "pirates",
    "bigfoot", "loch ness monster", "ufo sightings",
    "astral projection", "remote viewing",
    # Mitos
}

# Regex para detectar preguntas completas
QUESTION_PATTERNS = re.compile(
    r"^(how|what|why|when|where|who|which|whose|can|should|will|does|is|are)\b",
    re.IGNORECASE
)

# Regex para URLs y enlaces
URL_PATTERN = re.compile(r"(https?://|www\.|\.com|\.org|\.net|\.io)", re.IGNORECASE)

# Regex para caracteres no alfabeticos raros
STRANGE_CHARS_PATTERN = re.compile(r"[^a-zA-Záéíóúñü\s\-'\d/]")

# Regex para detectar libros apocrifos
APOCRYPHAL_BOOKS = {
    "tobit", "judith", "wisdom", "sirach", "baruch",
    "1 maccabees", "2 maccabees", "1 esdras", "2 esdras",
    "1 macc", "2 macc", "1 esd", "2 esd",
    "ecclesiasticus", "wisdom of solomon",
}


def normalize_topic(s: str) -> str:
    """Lowercase + strip accents + trim."""
    s = s.lower().strip()
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s


def is_question(topic: str) -> bool:
    """Detecta si el tema es una pregunta completa (no un concepto).

    Solo marca como pregunta INVALID si:
    - Tiene >=8 palabras (pregunta completa)
    - Empieza con "is", "are", "can", "should", "will" (verbos auxiliares que indican pregunta)
    - Contiene "sin" en contexto de pregunta teologica (no como pecado)
    """
    words = topic.split()
    if len(words) >= 8:
        return True
    norm = normalize_topic(topic)
    # Verbos auxiliares al inicio
    if re.match(r"^(is|are|can|should|will|do|does|did|was|were|has|have|had)\s", norm):
        # Excepciones: "is jesus the son of god" es pregunta valida?
        # Por ahora las marcamos como question y luego el clustering decide
        return True
    return False


def is_url_or_technical(topic: str) -> bool:
    return bool(URL_PATTERN.search(topic))


def has_strange_chars(topic: str) -> bool:
    # Permitir algunos caracteres especiales pero detectar URLs/codigo
    return bool(STRANGE_CHARS_PATTERN.search(topic))


def is_too_long(topic: str) -> bool:
    words = topic.split()
    return len(words) >= 8


def is_blacklisted(topic: str) -> bool:
    norm = normalize_topic(topic)
    # Match exacto o como substring al inicio/fin
    if norm in BLACKLIST_TERMS:
        return True
    for term in BLACKLIST_TERMS:
        if term in norm and len(term) >= 4:
            return True
    return False


def is_apocryphal_reference(topic: str) -> bool:
    """Detecta si el tema menciona un libro apocrifo."""
    norm = normalize_topic(topic)
    for book in APOCRYPHAL_BOOKS:
        if book in norm:
            return True
    return False


def is_empty(topic: str) -> bool:
    return not topic or not topic.strip()


def has_duplicate_words_excessively(topic: str) -> bool:
    """Detecta typos como 'accept accept accept' o 'love love love' (3+ repeticiones).

    NO detecta repeticiones tematicas validas como:
    - "eye for an eye" (proverbio)
    - "iron sharpens iron" (proverbio)
    - "brother against brother" (tema biblico)
    - "generation to generation" (frase biblica)
    """
    words = normalize_topic(topic).split()
    if len(words) < 3:
        return False
    # Solo considerar invalido si la misma palabra aparece 3+ veces
    # O si son palabras CONSECUTIVAS repetidas (typo claro)
    counter = Counter(words)
    most_common = counter.most_common(1)[0]
    # Typos como "love love" o "love love love" (palabra repetida consecutivamente)
    for i in range(len(words) - 1):
        if words[i] == words[i+1]:
            return True
    # O 3+ repeticiones no consecutivas (caso extremo)
    if most_common[1] >= 3 and len(words) <= 5:
        return True
    return False


def is_truly_apocryphal(topic: str) -> bool:
    """Detecta referencia a un libro apocrifo REAL (no temas genericos como 'wisdom')."""
    norm = normalize_topic(topic)
    # Solo si el nombre del libro apocrifo aparece como PALABRA COMPLETA
    # al inicio o como referencia biblica clara
    for book in APOCRYPHAL_BOOKS:
        # Patrones especificos: "tobit 5:1", "1 maccabees 4", etc.
        if re.search(rf"\b{re.escape(book)}\b\s*\d", norm):
            return True
        # O si el tema es exactamente el nombre del libro
        if norm == book or norm == f"the {book}" or norm == f"book of {book}":
            return True
    return False


def is_truly_blacklisted(topic: str) -> bool:
    """Blacklist mas estricta: solo marca si hay coincidencia fuerte."""
    norm = normalize_topic(topic)
    # Primero intentar match exacto
    if norm in BLACKLIST_TERMS:
        return True
    # Match exacto con plural/simple
    for term in BLACKLIST_TERMS:
        if norm == term or norm == term + "s":
            return True
    # Match fuerte: la palabra completa aparece en el tema (>=5 chars)
    for term in BLACKLIST_TERMS:
        if len(term) >= 5:
            # word boundary check
            if re.search(rf"\b{re.escape(term)}\b", norm):
                return True
    return False


def classify_invalid_reason(topic: str) -> str | None:
    """Retorna la razon por la cual un tema es INVALID, o None si es valido."""
    if is_empty(topic):
        return "empty"
    if is_url_or_technical(topic):
        return "url_or_technical"
    if has_strange_chars(topic):
        return "strange_chars"
    if is_too_long(topic):
        return "too_long"
    if is_question(topic):
        return "question"
    if is_truly_blacklisted(topic):
        return "blacklist"
    if is_truly_apocryphal(topic):
        return "apocryphal_book"
    if has_duplicate_words_excessively(topic):
        return "duplicate_words"
    return None


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    p.add_argument("--input", "-i", type=Path, default=DEFAULT_INPUT)
    p.add_argument("--output-dir", "-o", type=Path, default=ROOT / "tools")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    input_path: Path = args.input
    output_dir: Path = args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)

    if not input_path.exists():
        print(f"ERROR: no se encuentra {input_path}")
        return 1

    print("=" * 60)
    print("Biblion: Topic Analysis and Cleaning (Fase 1 + Fase 2)")
    print("=" * 60)
    print(f"Entrada: {input_path}")
    print(f"Salida:  {output_dir}")
    print()

    # Cargar todos los temas
    print("[1/4] Cargando temas...")
    topic_count: Counter = Counter()
    invalid_reasons: Counter = Counter()
    invalid_topics: dict[str, str] = {}
    valid_topics: dict[str, int] = {}
    normalized_to_original: dict[str, str] = {}

    total_lines = 0
    with input_path.open(encoding="utf-8", newline="") as f:
        next(f)  # header
        for line in f:
            total_lines += 1
            parts = line.split("\t")
            if len(parts) < 2:
                continue
            topic = parts[0].strip()
            if not topic:
                continue
            topic_count[topic] += 1
            if topic not in normalized_to_original:
                normalized_to_original[normalize_topic(topic)] = topic

    print(f"  Total lineas: {total_lines:,}")
    print(f"  Total temas unicos: {len(topic_count):,}")
    print()

    # Clasificar
    print("[2/4] Clasificando temas (Fase 2)...")
    for topic, count in topic_count.items():
        reason = classify_invalid_reason(topic)
        if reason is not None:
            invalid_topics[topic] = reason
            invalid_reasons[reason] += 1
        else:
            valid_topics[topic] = count

    print(f"  Temas validos: {len(valid_topics):,}")
    print(f"  Temas INVALID: {len(invalid_topics):,}")
    print()
    print("  Razones INVALID:")
    for reason, n in invalid_reasons.most_common():
        print(f"    {n:>5,}  {reason}")
    print()

    # Detectar duplicados exactos (case-insensitive)
    print("[3/4] Detectando duplicados (case-insensitive)...")
    normalized_groups: dict[str, list[str]] = defaultdict(list)
    for topic in topic_count:
        normalized_groups[normalize_topic(topic)].append(topic)
    duplicates = {k: v for k, v in normalized_groups.items() if len(v) > 1}
    print(f"  Grupos de duplicados exactos: {len(duplicates):,}")
    print(f"  Temas en grupos duplicados: {sum(len(v) for v in duplicates.values()):,}")
    print()

    # Generar reporte
    print("[4/4] Generando reportes...")
    top_100 = [
        {"topic": t, "verse_count": c, "is_invalid": t in invalid_topics, "is_candidate": t in valid_topics}
        for t, c in topic_count.most_common(100)
    ]

    # Distribucion por versiculos
    verse_dist = {
        "1_versiculo": sum(1 for c in topic_count.values() if c == 1),
        "2_4_versiculos": sum(1 for c in topic_count.values() if 2 <= c <= 4),
        "5_19_versiculos": sum(1 for c in topic_count.values() if 5 <= c <= 19),
        "20_49_versiculos": sum(1 for c in topic_count.values() if 20 <= c <= 49),
        "50_plus_versiculos": sum(1 for c in topic_count.values() if c >= 50),
    }

    analysis_report = {
        "total_topics": len(topic_count),
        "total_verse_topic_pairs": total_lines,
        "duplicates": {
            "groups": len(duplicates),
            "total_topics_in_groups": sum(len(v) for v in duplicates.values()),
            "examples": [
                {"normalized": k, "variants": v[:5]}
                for k, v in list(duplicates.items())[:10]
            ],
        },
        "possible_noise": {
            "total_invalid": len(invalid_topics),
            "by_reason": dict(invalid_reasons.most_common()),
            "examples_by_reason": {},
        },
        "length_distribution": {
            "1_word": sum(1 for t in topic_count if len(t.split()) == 1),
            "2_to_3_words": sum(1 for t in topic_count if 2 <= len(t.split()) <= 3),
            "4_to_7_words": sum(1 for t in topic_count if 4 <= len(t.split()) <= 7),
            "8_plus_words": sum(1 for t in topic_count if len(t.split()) >= 8),
        },
        "verse_count_distribution": verse_dist,
        "valid_topics": len(valid_topics),
        "top_100_topics": top_100,
    }

    # Ejemplos de INVALID por cada razon
    examples_by_reason = defaultdict(list)
    for topic, reason in invalid_topics.items():
        if len(examples_by_reason[reason]) < 5:
            examples_by_reason[reason].append(topic)
    analysis_report["possible_noise"]["examples_by_reason"] = dict(examples_by_reason)

    # Guardar analysis_report.json
    with (output_dir / "analysis_report.json").open("w", encoding="utf-8") as f:
        json.dump(analysis_report, f, ensure_ascii=False, indent=2)
    print(f"  Guardado: analysis_report.json")

    # Guardar invalid_topics.json
    invalid_output = {
        "total_invalid": len(invalid_topics),
        "by_reason": dict(invalid_reasons.most_common()),
        "topics": [
            {"topic": t, "status": "INVALID", "reason": r}
            for t, r in sorted(invalid_topics.items(), key=lambda x: -topic_count[x[0]])
        ],
    }
    with (output_dir / "invalid_topics.json").open("w", encoding="utf-8") as f:
        json.dump(invalid_output, f, ensure_ascii=False, indent=2)
    print(f"  Guardado: invalid_topics.json")

    # Guardar topics_candidates.json (los que pasan los filtros)
    candidates_output = {
        "total_candidates": len(valid_topics),
        "topics": [
            {
                "topic_en": t,
                "verse_count": c,
                "normalized_key": normalize_topic(t).replace(" ", "_"),
                "word_count": len(t.split()),
            }
            for t, c in sorted(valid_topics.items(), key=lambda x: -x[1])
        ],
    }
    with (output_dir / "topics_candidates.json").open("w", encoding="utf-8") as f:
        json.dump(candidates_output, f, ensure_ascii=False, indent=2)
    print(f"  Guardado: topics_candidates.json ({len(valid_topics):,} candidatos)")

    print()
    print("=" * 60)
    print("RESUMEN")
    print("=" * 60)
    print(f"Total temas:                    {len(topic_count):>6,}")
    print(f"Temas VALIDOS (candidatos):    {len(valid_topics):>6,}")
    print(f"Temas INVALID (a eliminar):     {len(invalid_topics):>6,}")
    print(f"Duplicados exactos:            {len(duplicates):>6,}")
    print()
    print("Proximos pasos:")
    print("  1. Revisa tools/analysis_report.json")
    print("  2. Revisa tools/invalid_topics.json")
    print("  3. Decide si las reglas son correctas")
    print("  4. Cuando estes conforme, pasamos a Fase 3 (clustering)")
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())

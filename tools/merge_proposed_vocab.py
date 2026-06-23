#!/usr/bin/env python3
"""
Fusiona las propuestas validadas de anchor_word_vocab_proposed.json
en anchor_word_vocab.json.

Solo agrega entradas que pasan el filtro de calidad (coherencia con
RV1960). Las propuestas incorrectas (como 'fear'->'jehov�' o 'shalt'->'jehov�')
se omiten y se reportan para revision.

Uso:
    python tools/merge_proposed_vocab.py
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXISTING_VOCAB_PATH = ROOT / "tools" / "anchor_word_vocab.json"
PROPOSED_VOCAB_PATH = ROOT / "tools" / "anchor_word_vocab_proposed.json"

# Propuestas del extractor que se descartan por ser incorrectas para RV1960
# (el extractor tiende a elegir 'jehová' cuando la palabra inglesa co-ocurre
# frecuentemente con versiculos que contienen 'jehová').
BLOCKLIST = {
    "fear": "temor",       # fear -> jehov� (incorrecto, debe ser 'temor')
    "shalt": "arás",        # shalt -> jehov� (incorrecto, debe ser conjugado)
    "behold": "he aquí",    # behold -> aquí (mejor 'he aquí' para KJV)
    "therefore": "por tanto",
    "rejoice": "gozo",      # rejoice -> dios (incorrecto, debe ser 'gozo')
    "praise": "alabad",     # praise -> jehov� (incorrecto, debe ser 'alabanza')
    "go": "va",            # go -> dijo (incorrecto, 'go' es verbo, 'dijo' es 'said')
    "saying": "diciendo",   # ya esta en propuesta
    "there": "allí",        # pendiente
    "this": "este",         # pendiente
}


def main():
    if not PROPOSED_VOCAB_PATH.exists():
        print(f"  No existe {PROPOSED_VOCAB_PATH.name}, saliendo")
        return
    with PROPOSED_VOCAB_PATH.open(encoding="utf-8") as f:
        proposed = json.load(f)

    if EXISTING_VOCAB_PATH.exists():
        with EXISTING_VOCAB_PATH.open(encoding="utf-8") as f:
            existing = json.load(f)
    else:
        existing = {}

    accepted = []
    rejected = []
    for en, es in proposed.items():
        en_lower = en.lower()
        # No pisar entradas existentes
        if en_lower in {k.lower() for k in existing}:
            continue
        # Saltar las de blocklist
        if en_lower in BLOCKLIST:
            rejected.append((en, es, BLOCKLIST[en_lower]))
            continue
        # Validar formato basico
        if not es or not isinstance(es, str):
            rejected.append((en, es, "valor invalido"))
            continue
        accepted.append((en, es))

    print(f"  Propuestas aceptadas: {len(accepted)}")
    print(f"  Propuestas rechazadas: {len(rejected)}")

    if rejected:
        print("\nRechazadas (sugerimos sustituto):")
        for en, es, sugg in rejected:
            print(f"  {en!r:20s} -> {es!r:20s} (sugerido: {sugg!r})")

    # Fusionar (preservar capitalizacion original del existente)
    for en, es in accepted:
        existing[en] = es

    # Guardar (orden alfabetico por clave)
    sorted_data = dict(sorted(existing.items(), key=lambda x: x[0].lower()))

    with EXISTING_VOCAB_PATH.open("w", encoding="utf-8") as f:
        json.dump(sorted_data, f, indent=2, ensure_ascii=False)

    print(f"\nVocabulario actualizado: {len(sorted_data)} palabras")
    print(f"  Guardado en: {EXISTING_VOCAB_PATH}")


if __name__ == "__main__":
    main()

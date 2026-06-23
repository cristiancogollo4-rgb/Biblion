"""
Fase 5.5: Traduccion automatica EN -> ES de los 1,914 canonical topics
sin traduccion curada, usando GoogleTranslator (deep-translator).

- Carga topic_taxonomy.json.
- Para cada topic sin name_es, traduce name_en.
- Guarda topic_taxonomy_translated.json.
- Las traducciones no son perfectas; se marcan para revision posterior.
"""
from __future__ import annotations

import json
import time
from pathlib import Path

from deep_translator import GoogleTranslator
from deep_translator.exceptions import (
    NotValidPayload,
    RequestError,
    TooManyRequests,
    TranslationNotFound,
)

OUT_DIR = Path("tools")
TRANSLATOR = GoogleTranslator(source="en", target="es")
BATCH_SIZE = 50
SLEEP_BETWEEN = 0.05  # seconds


def safe_translate(text: str) -> str | None:
    try:
        result = TRANSLATOR.translate(text)
        if not result:
            return None
        return result
    except (NotValidPayload, RequestError, TooManyRequests, TranslationNotFound):
        return None
    except Exception:
        return None


def main() -> None:
    print("=" * 60)
    print("Biblion: Fase 5.5 - Traduccion automatica EN -> ES")
    print("=" * 60)
    print()

    with (OUT_DIR / "topic_taxonomy.json").open(encoding="utf-8") as f:
        data = json.load(f)

    # Recopilar todos los canonical topics sin traduccion
    to_translate: list[dict] = []
    already_translated = 0
    for cat_name, cat in data["categories"].items():
        for cluster in cat["clusters"]:
            if not cluster["name_es"]:
                to_translate.append(cluster)
            else:
                already_translated += 1

    print(f"Ya traducidos (curados): {already_translated}")
    print(f"Por traducir: {len(to_translate)}")

    # Traducir en lotes
    success = 0
    failed: list[str] = []
    t0 = time.time()

    for i, topic in enumerate(to_translate):
        text = topic["name_en"]
        # Capitalizar primera letra para mejor traduccion
        text_cap = text[0].upper() + text[1:] if text else text
        result = safe_translate(text_cap)
        if result:
            # Normalizar: minuscula primera letra
            topic["name_es"] = result[0].lower() + result[1:] if result else result
            topic["name_es_auto"] = True
            success += 1
        else:
            failed.append(text)
        time.sleep(SLEEP_BETWEEN)

        if (i + 1) % 200 == 0:
            elapsed = time.time() - t0
            rate = (i + 1) / elapsed if elapsed > 0 else 0
            remaining = (len(to_translate) - i - 1) / rate if rate > 0 else 0
            print(
                f"  [{i+1}/{len(to_translate)}] "
                f"ok={success}, fail={len(failed)}, "
                f"{rate:.1f} t/s, ETA {remaining:.0f}s",
                flush=True,
            )

    elapsed = time.time() - t0
    print(f"\nFinalizado en {elapsed:.1f}s")
    print(f"  Exitosos: {success}")
    print(f"  Fallidos: {len(failed)}")
    if failed[:20]:
        print(f"  Ejemplos fallidos: {failed[:20]}")

    # Actualizar estadisticas
    data["n_with_translation"] = already_translated + success
    data["coverage_translation"] = round(
        data["n_with_translation"] / data["n_clusters"] * 100, 1
    )

    # Guardar
    out_path = OUT_DIR / "topic_taxonomy_translated.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"\nGuardado: {out_path}")
    print(f"Cobertura final: {data['coverage_translation']}%")

    # Guardar fallidos para revision manual
    if failed:
        failed_path = OUT_DIR / "topics_translation_failed.json"
        with failed_path.open("w", encoding="utf-8") as f:
            json.dump({"n": len(failed), "topics": failed}, f, ensure_ascii=False, indent=2)
        print(f"Guardado: {failed_path}")


if __name__ == "__main__":
    main()

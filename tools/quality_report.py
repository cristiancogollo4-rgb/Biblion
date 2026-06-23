#!/usr/bin/env python3
"""
Fase 8 (simplificada): Quality control report.

Lee los outputs de las fases anteriores y genera quality_report.json
con metricas de calidad del dataset procesado.

Uso:
  python tools/quality_report.py
"""
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"

ANALYSIS_REPORT = TOOLS / "analysis_report.json"
INVALID_TOPICS = TOOLS / "invalid_topics.json"
CANDIDATES = TOOLS / "topics_candidates.json"
QUALITY_REPORT = TOOLS / "quality_report.json"


def main() -> int:
    if not all(p.exists() for p in [ANALYSIS_REPORT, INVALID_TOPICS, CANDIDATES]):
        print("ERROR: faltan archivos. Ejecuta primero analyze_and_clean_topics.py")
        return 1

    with ANALYSIS_REPORT.open(encoding="utf-8") as f:
        analysis = json.load(f)
    with INVALID_TOPICS.open(encoding="utf-8") as f:
        invalid = json.load(f)
    with CANDIDATES.open(encoding="utf-8") as f:
        candidates = json.load(f)

    # Validaciones
    issues = []

    # 1. Sin duplicados exactos
    if analysis["duplicates"]["groups"] > 0:
        issues.append({
            "level": "WARN",
            "type": "exact_duplicates",
            "message": f"{analysis['duplicates']['groups']} grupos de duplicados exactos detectados"
        })

    # 2. Distribucion de INVALID coherente
    total_invalid = invalid["total_invalid"]
    sum_by_reason = sum(invalid["by_reason"].values())
    if total_invalid != sum_by_reason:
        issues.append({
            "level": "ERROR",
            "type": "invalid_count_mismatch",
            "message": f"total_invalid={total_invalid} != sum(by_reason)={sum_by_reason}"
        })

    # 3. Temas validos + invalid = total
    total = analysis["total_topics"]
    valid_count = candidates["total_candidates"]
    if total != valid_count + total_invalid:
        issues.append({
            "level": "WARN",
            "type": "count_mismatch",
            "message": f"total={total} != valid({valid_count}) + invalid({total_invalid}) = {valid_count + total_invalid}"
        })

    # 4. Verificar que no hay temas vacios
    empty_candidates = [t for t in candidates["topics"] if not t["topic_en"].strip()]
    if empty_candidates:
        issues.append({
            "level": "ERROR",
            "type": "empty_topics",
            "message": f"{len(empty_candidates)} candidatos con nombre vacio"
        })

    # 5. Verificar que todos los INVALID tienen una razon
    invalid_without_reason = [t for t in invalid["topics"] if not t.get("reason")]
    if invalid_without_reason:
        issues.append({
            "level": "ERROR",
            "type": "invalid_without_reason",
            "message": f"{len(invalid_without_reason)} temas INVALID sin razon especificada"
        })

    # 6. Distribucion de versiculos saludable
    dist = analysis["verse_count_distribution"]
    single_verse = dist.get("1_versiculo", 0)
    single_pct = single_verse / total * 100
    if single_pct > 30:
        issues.append({
            "level": "WARN",
            "type": "long_tail",
            "message": f"{single_pct:.1f}% de los temas tienen solo 1 versiculo. Considerar clustering en Fase 3."
        })

    # 7. Cobertura de los INVALID respecto al total
    invalid_pct = total_invalid / total * 100
    if invalid_pct < 1:
        issues.append({
            "level": "INFO",
            "type": "low_invalid_rate",
            "message": f"Solo {invalid_pct:.2f}% de temas son INVALID. Posiblemente faltan reglas."
        })
    if invalid_pct > 20:
        issues.append({
            "level": "WARN",
            "type": "high_invalid_rate",
            "message": f"{invalid_pct:.1f}% de temas son INVALID. Verificar que las reglas no son agresivas."
        })

    # 8. Tamano de cola larga (temas con 1 versiculo)
    long_tail = {
        "single_verse_count": single_verse,
        "single_verse_pct": f"{single_pct:.1f}%",
        "implication": "La mayoria de los temas son de cola larga. El clustering en Fase 3 fusionara los sinonimos."
    }

    # Generar reporte
    report = {
        "summary": {
            "total_topics": total,
            "valid_topics": valid_count,
            "invalid_topics": total_invalid,
            "invalid_pct": f"{invalid_pct:.2f}%",
            "issues_count": len(issues),
            "critical_issues": sum(1 for i in issues if i["level"] == "ERROR"),
            "warnings": sum(1 for i in issues if i["level"] == "WARN"),
        },
        "invalid_breakdown": invalid["by_reason"],
        "verse_count_distribution": dist,
        "long_tail_analysis": long_tail,
        "issues": issues,
        "ready_for_phase_3": all(i["level"] != "ERROR" for i in issues),
        "next_steps": [
            "1. Revisar tools/invalid_topics.json para confirmar las exclusiones",
            "2. Si estas conforme, pasar a Fase 3: Clustering semantico con embeddings",
            "3. Decidir modelo: BAAI/bge-small-en-v1.5 (preferido) o all-MiniLM-L6-v2",
            "4. Ajustar threshold de clustering (sugerido: 0.82 cosine similarity)"
        ],
    }

    with QUALITY_REPORT.open("w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    print("=" * 60)
    print("QUALITY REPORT")
    print("=" * 60)
    print(f"Total temas:              {total:,}")
    print(f"VALIDOS:                  {valid_count:,} ({valid_count/total*100:.1f}%)")
    print(f"INVALID:                  {total_invalid:,} ({invalid_pct:.1f}%)")
    print()
    print("Distribucion por razon INVALID:")
    for reason, n in invalid["by_reason"].items():
        print(f"  {n:>4}  {reason}")
    print()
    print(f"Issues encontrados:        {len(issues)}")
    for i in issues:
        print(f"  [{i['level']}] {i['type']}: {i['message']}")
    print()
    print(f"Listo para Fase 3:        {'SI' if report['ready_for_phase_3'] else 'NO'}")
    print(f"Reporte guardado en:      {QUALITY_REPORT}")
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())

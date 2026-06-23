"""
Fase 3: Clustering semantico de temas Biblion con BAAI/bge-small-en-v1.5.

- Carga topics_candidates.json (6,609 temas validos).
- Genera embeddings (CPU, ~10-15 min para 6,609 temas).
- Clustering aglomerativo con threshold cosine 0.87.
- Detecta clusters grandes y singletons.
- Guarda clusters + quality_check.json para revision.
"""
from __future__ import annotations

import json
import sys
import time
from pathlib import Path

import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.cluster import AgglomerativeClustering
from sklearn.metrics.pairwise import cosine_similarity

MODEL_NAME = "BAAI/bge-small-en-v1.5"
THRESHOLD = 0.87
OUT_DIR = Path("tools")


def load_candidates() -> list[dict]:
    path = OUT_DIR / "topics_candidates.json"
    with path.open(encoding="utf-8") as f:
        data = json.load(f)
    return data["topics"]


def main() -> None:
    print("=" * 60)
    print("Biblion: Fase 3 - Clustering semantico")
    print("=" * 60)
    print()

    candidates = load_candidates()
    print(f"[1/5] {len(candidates)} candidatos cargados")

    # Texto a embeber: prefijamos con contexto biblico para anclar el modelo
    # al dominio correcto. Sin esto, BGE produce embeddings ruidosos
    # para topics cortos (earthquake <-> loving your husband 0.988).
    texts = [f"Biblical concept: {c['topic_en']}" for c in candidates]
    print(f"      Textos a embeber: {len(texts)} (con prefijo biblico)")

    print(f"\n[2/5] Cargando modelo {MODEL_NAME}...")
    t0 = time.time()
    model = SentenceTransformer(MODEL_NAME, device="cpu")
    print(f"      Modelo cargado en {time.time() - t0:.1f}s")
    print(f"      Dimensiones: {model.get_sentence_embedding_dimension()}")

    print(f"\n[3/5] Generando embeddings (CPU)...")
    t0 = time.time()
    embeddings = model.encode(
        texts,
        batch_size=64,
        show_progress_bar=True,
        convert_to_numpy=True,
        normalize_embeddings=False,
    )
    print(f"      Embeddings: {embeddings.shape}, dtype={embeddings.dtype}")
    print(f"      Tiempo: {time.time() - t0:.1f}s")

    # Guardar embeddings raw para reutilizar en Fase 4/5
    np.save(OUT_DIR / "topic_embeddings.npy", embeddings)
    print(f"      Guardado: topic_embeddings.npy")

    print(f"\n[4/5] Clustering aglomerativo (threshold cosine={THRESHOLD})...")
    t0 = time.time()
    # distance_threshold = 1 - 0.87 = 0.13
    clusterer = AgglomerativeClustering(
        n_clusters=None,
        distance_threshold=1.0 - THRESHOLD,
        metric="cosine",
        linkage="average",
    )
    labels = clusterer.fit_predict(embeddings)
    print(f"      Tiempo: {time.time() - t0:.1f}s")
    print(f"      Clusters encontrados: {len(set(labels))}")

    n_clusters = len(set(labels))
    sizes = [int((labels == c).sum()) for c in range(n_clusters)]
    print(f"      Cluster mas grande: {max(sizes)}")
    print(f"      Cluster mas pequeno: {min(sizes)}")
    print(f"      Singletons: {sum(1 for s in sizes if s == 1)}")
    print(f"      Clusters >50 miembros: {sum(1 for s in sizes if s > 50)}")

    # Distribucion
    print(f"\n      Distribucion de tamano de clusters:")
    bins = [1, 2, 5, 10, 20, 50, 100, 500, 1000, 10000]
    for i in range(len(bins) - 1):
        count = sum(1 for s in sizes if bins[i] <= s < bins[i + 1])
        print(f"        {bins[i]:>4}-{bins[i + 1]:>4}: {count:>4} clusters")

    print(f"\n[5/5] Guardando clusters...")
    clusters_out: list[dict] = []
    for c in range(n_clusters):
        members_idx = np.where(labels == c)[0]
        members = [
            {
                "topic_en": candidates[i]["topic_en"],
                "verse_count": candidates[i]["verse_count"],
                "word_count": candidates[i].get("word_count", 0),
                "category_hint": candidates[i].get("category_hint"),
            }
            for i in members_idx
        ]
        # Ordenar por verse_count desc
        members.sort(key=lambda m: -m["verse_count"])
        clusters_out.append(
            {
                "cluster_id": int(c),
                "size": len(members),
                "total_verses": sum(m["verse_count"] for m in members),
                "members": members,
            }
        )

    # Ordenar clusters por tamano desc
    clusters_out.sort(key=lambda cl: -cl["size"])

    out_data = {
        "model": MODEL_NAME,
        "threshold": THRESHOLD,
        "total_topics": len(candidates),
        "n_clusters": n_clusters,
        "singletons": sum(1 for s in sizes if s == 1),
        "largest_cluster": max(sizes),
        "clusters": clusters_out,
    }

    out_path = OUT_DIR / "topic_clusters.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(out_data, f, ensure_ascii=False, indent=2)
    print(f"      Guardado: {out_path}")

    # Quality check: clusters sospechosos
    suspicious: list[dict] = []
    for cl in clusters_out:
        if cl["size"] > 50:
            # Calcular similitud media intra-cluster
            member_idxs = [
                i for i, c in enumerate(candidates)
                if c["topic_en"] in {m["topic_en"] for m in cl["members"]}
            ]
            if len(member_idxs) > 1:
                sub_emb = embeddings[member_idxs]
                sim_matrix = cosine_similarity(sub_emb)
                n = len(member_idxs)
                # Tomar upper triangle sin diagonal
                triu_idx = np.triu_indices(n, k=1)
                avg_sim = float(sim_matrix[triu_idx].mean())
                if avg_sim < 0.90:
                    suspicious.append(
                        {
                            "cluster_id": cl["cluster_id"],
                            "size": cl["size"],
                            "avg_intra_similarity": round(avg_sim, 4),
                            "sample_members": [m["topic_en"] for m in cl["members"][:10]],
                        }
                    )

    quality_check = {
        "model": MODEL_NAME,
        "threshold": THRESHOLD,
        "n_clusters": n_clusters,
        "n_suspicious": len(suspicious),
        "suspicious_clusters": suspicious,
    }
    qc_path = OUT_DIR / "cluster_quality_check.json"
    with qc_path.open("w", encoding="utf-8") as f:
        json.dump(quality_check, f, ensure_ascii=False, indent=2)
    print(f"      Guardado: {qc_path}")

    print(f"\nClusters sospechosos (>50 miembros, sim<0.90): {len(suspicious)}")
    if suspicious:
        print("\nTop 10:")
        for s in suspicious[:10]:
            print(f"  cluster {s['cluster_id']}: size={s['size']}, sim={s['avg_intra_similarity']:.3f}")
            print(f"    sample: {s['sample_members'][:5]}")


if __name__ == "__main__":
    main()

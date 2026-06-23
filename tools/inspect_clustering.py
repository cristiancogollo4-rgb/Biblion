"""Inspeccion del clustering: cuantos pares tienen sim > threshold?"""
import json
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

EMB = np.load("tools/topic_embeddings.npy")
THRESHOLDS = [0.70, 0.75, 0.80, 0.85, 0.87, 0.90, 0.92, 0.95]

print(f"Embeddings shape: {EMB.shape}")
print()
print("Pairwise sim analysis (sample 1000):")
np.random.seed(42)
sample_idx = np.random.choice(len(EMB), 1000, replace=False)
sub = EMB[sample_idx]
sim = cosine_similarity(sub)
# Upper triangle
triu = sim[np.triu_indices(1000, k=1)]
print(f"  Total pares: {len(triu)}")
print(f"  Mean: {triu.mean():.3f}, Median: {np.median(triu):.3f}")
print(f"  P90: {np.percentile(triu, 90):.3f}, P95: {np.percentile(triu, 95):.3f}")
print(f"  P99: {np.percentile(triu, 99):.3f}, Max: {triu.max():.3f}")
print()
print("Threshold analysis:")
for t in THRESHOLDS:
    n_pairs = int((triu >= t).sum())
    print(f"  sim >= {t}: {n_pairs:>6} pares ({n_pairs/len(triu)*100:.2f}%)")
print()

# Inspeccion visual: top pares mas similares
print("Top 30 pares mas similares (potenciales duplicados):")
top_k = 30
idx_flat = np.argpartition(-triu, top_k)[:top_k]
# Sort by sim
sorted_idx = idx_flat[np.argsort(-triu[idx_flat])]

with open("tools/topics_candidates.json", "r", encoding="utf-8") as f:
    cand = json.load(f)
texts = [c["topic_en"] for c in cand["topics"]]

count = 0
for flat_i in sorted_idx:
    if count >= 20:
        break
    i, j = np.triu_indices(1000, k=1)
    a, b = int(i[flat_i]), int(j[flat_i])
    sim_val = triu[flat_i]
    if sim_val < 0.5:
        break
    print(f"  {sim_val:.3f}  '{texts[a]}' <-> '{texts[b]}'")
    count += 1

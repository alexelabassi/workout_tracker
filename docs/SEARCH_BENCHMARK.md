# SEARCH_BENCHMARK.md — OpenSearch vs PostgreSQL (empirical evaluation)

An honest, reproducible comparison of the two search back-ends for this domain. Built and run by the
benchmark kit in `com.thesis.workout.benchmark` (Spring profile `benchmark`). This is the chapter
that turns "I integrated OpenSearch" into "I investigated *when a dedicated search engine is worth
it over PostgreSQL* and measured it."

## Research question

> For a template-marketplace / training-history workload, when does a dedicated search engine
> (OpenSearch) outperform a well-built PostgreSQL search, across **latency, scale, index build time,
> on-disk size, and capability** — and where is the crossover point?

## What is compared (fairly)

Both engines index an **identical synthetic corpus** of template documents (same fields, same text),
each into a purpose-built denormalised index. The PostgreSQL side is *PostgreSQL done properly*, not
a strawman `ILIKE '%x%'`:

| Capability | PostgreSQL baseline | OpenSearch |
|---|---|---|
| Full-text (ranked) | weighted `tsvector` (name=A, exercises=B, muscles=C, desc=D) + **GIN**, ranked by `ts_rank_cd` | boosted `multi_match` (`name^4, exerciseNames^3, muscleGroupsText^2, description^1`) |
| Fuzzy / typo | `pg_trgm` trigram + **GIN**, `word_similarity` / `<%` (threshold 0.3) | `fuzziness=AUTO`, `prefix_length=1` |
| Filters | btree/GIN on `visibility/difficulty/split_type/muscle_groups` | `term` filters |
| Facets | `GROUP BY` (terms) / `unnest` (arrays) | terms aggregation |

Both indexes are denormalised read models, so this measures the **engines**, not an ORM or a JOIN
penalty.

## Setup & methodology

- Hardware/runtime: local dev machine, Docker Desktop. PostgreSQL 16 and single-node OpenSearch
  2.13 (security disabled, 512 MB heap) from `docker-compose.yml`; benchmark JVM (Java 21) on the
  host hitting `localhost:5432` / `localhost:9200`.
- Corpus: deterministic generator (`SyntheticCorpus`, fixed seed) → realistic lifting vocabulary,
  ~90% PUBLIC, skewed popularity. Corpus is **streamed** in batches so memory stays constant at any
  size. Measured at **7 sizes: 1k / 10k / 50k / 100k / 500k / 1M / 2M** templates (the harness goes
  higher; 2M is the practical ceiling here — beyond it the GIN-trigram build dominates).
- Build: PostgreSQL is loaded **bulk-then-index** (truncate → drop indexes → batched insert →
  create all indexes → `ANALYZE`), the realistic way to load a large table; OpenSearch is bulk-
  indexed then refreshed. The "build time" metric covers the whole load+index.
- Measurement: **single-threaded**, in-JVM `System.nanoTime()` around each query call, after a
  warm-up then up to **300 measured** iterations; latency in **milliseconds**; percentiles by
  nearest-rank. A per-query **wall-clock budget** (15 s, min 15 samples) caps O(corpus) queries
  (PostgreSQL fuzzy at large N) so they cannot run for hours; cells that stopped early are flagged
  `(n=…)` with their actual sample count.
- Honesty note on symmetry: each query is measured **the way the app actually calls it** — PG over
  JDBC, OpenSearch over localhost HTTP + JSON (de)serialization. That protocol overhead is real and
  is *why OpenSearch has a ~2–3 ms floor* and loses at tiny corpus sizes. Caches are warm
  (cold-start is not measured); concurrency/throughput under load is future work.

## How to run

```bash
# Postgres + OpenSearch must be up (the normal docker-compose stack provides both):
docker compose up -d postgres opensearch

cd backend
SPRING_PROFILES_ACTIVE=benchmark APP_SEARCH_ENABLED=true SERVER_PORT=0 \
  BENCH_SIZES=1000,10000,50000,100000 BENCH_ITERS=300 BENCH_WARMUP=30 \
  mvn spring-boot:run -Dspring-boot.run.jvmArguments=-Xmx1g
# writes target/search-benchmark-results.md and logs the same table, then exits.

# Larger scales (500k / 1M) — the time budget keeps the slow PG-fuzzy cell bounded:
SPRING_PROFILES_ACTIVE=benchmark APP_SEARCH_ENABLED=true SERVER_PORT=0 \
  BENCH_SIZES=500000,1000000 BENCH_ITERS=200 BENCH_WARMUP=10 BENCH_BUDGET_MS=20000 \
  mvn spring-boot:run -Dspring-boot.run.jvmArguments=-Xmx1g
```
Tunables (env or `-D`): `BENCH_SIZES`, `BENCH_ITERS`, `BENCH_WARMUP`, `BENCH_TOPK`,
`BENCH_LOAD_BATCH`, `BENCH_MIN_SAMPLES`, `BENCH_BUDGET_MS`.
Cleanup: `DROP TABLE bench_template_doc;` in Postgres and `DELETE /bench_templates` in OpenSearch
(or `docker compose down -v`).

## Results (real run: iters=300, warmup=30, topK=20, budget=15s; `(n=…)` = samples when budget-capped)

### Index build time & on-disk size
| Corpus | PG build | OS build | PG size | OS size |
|---|---:|---:|---:|---:|
| 1,000 | 216 ms | 203 ms | 1.6 MB | 0.3 MB |
| 10,000 | 1,926 ms | 975 ms | 14.6 MB | 2.9 MB |
| 50,000 | 7,966 ms | 3,481 ms | 68.7 MB | 14.3 MB |
| 100,000 | 14,006 ms | 6,349 ms | 135.5 MB | 28.5 MB |
| 500,000 | 68,752 ms | 37,987 ms | 664.7 MB | 140.8 MB |
| 1,000,000 | 125,774 ms | 57,701 ms | 1,313.6 MB | 349.1 MB |
| 2,000,000 | 239,192 ms | 114,908 ms | 2,632.5 MB | 738.7 MB |

### Query latency — p50 / p95 / p99 (ms)
**1,000 templates**
| Query | PostgreSQL | OpenSearch |
|---|---:|---:|
| full-text "bench press" | 1.46 / 2.68 / 3.12 | 3.25 / 5.70 / 10.56 |
| full-text "squat" | 1.32 / 3.82 / 5.89 | 2.68 / 5.43 / 7.16 |
| fuzzy "benhc" (typo) | 14.45 / 18.91 / 21.69 | 2.37 / 3.74 / 5.63 |
| filtered (press+INTERMEDIATE+PPL) | 0.57 / 1.17 / 2.04 | 2.01 / 4.12 / 6.20 |
| facet by difficulty (q=press) | 0.57 / 1.40 / 1.95 | 1.92 / 4.16 / 4.84 |

**10,000 templates**
| Query | PostgreSQL | OpenSearch |
|---|---:|---:|
| full-text "bench press" | 10.64 / 14.32 / 17.63 | 2.85 / 5.21 / 7.17 |
| full-text "squat" | 9.05 / 12.88 / 14.93 | 2.40 / 5.66 / 14.75 |
| fuzzy "benhc" (typo) | 127.93 / 134.06 / 136.22 (n=117) | 3.23 / 7.38 / 10.94 |
| filtered (press+INTERMEDIATE+PPL) | 2.90 / 5.93 / 6.71 | 2.43 / 4.50 / 8.16 |
| facet by difficulty (q=press) | 4.07 / 6.79 / 8.22 | 1.69 / 4.44 / 7.26 |

**50,000 templates**
| Query | PostgreSQL | OpenSearch |
|---|---:|---:|
| full-text "bench press" | 48.70 / 66.59 / 77.97 | 5.17 / 10.05 / 13.98 |
| full-text "squat" | 41.77 / 50.52 / 62.67 | 2.69 / 5.74 / 8.10 |
| fuzzy "benhc" (typo) | **621.74 / 645.63 / 646.08 (n=24)** | **2.66 / 4.95 / 6.59** |
| filtered (press+INTERMEDIATE+PPL) | 11.91 / 15.19 / 16.92 | 3.05 / 6.07 / 8.22 |
| facet by difficulty (q=press) | 16.93 / 23.08 / 104.23 | 1.61 / 4.81 / 7.64 |

**100,000 templates**
| Query | PostgreSQL | OpenSearch |
|---|---:|---:|
| full-text "bench press" | 100.21 / 117.87 / 136.46 (n=147) | 6.84 / 10.59 / 12.83 |
| full-text "squat" | 82.47 / 91.46 / 108.95 (n=180) | 2.99 / 7.15 / 10.65 |
| fuzzy "benhc" (typo) | **1831.47 / 2169.61 / 2169.61 (n=15)** | **3.15 / 5.74 / 7.38** |
| filtered (press+INTERMEDIATE+PPL) | 25.50 / 38.88 / 52.70 | 4.14 / 9.65 / 17.57 |
| facet by difficulty (q=press) | 34.80 / 47.65 / 72.18 | 1.85 / 4.27 / 6.14 |

**500,000 templates**
| Query | PostgreSQL | OpenSearch |
|---|---:|---:|
| full-text "bench press" | 779.47 / 1021.25 / 1229.16 (n=28) | 22.75 / 38.09 / 67.26 |
| full-text "squat" | 640.50 / 780.81 / 898.36 (n=30) | 4.37 / 8.78 / 32.93 |
| fuzzy "benhc" (typo) | **2447.22 / 8354.43 / 8354.43 (n=10)** | **4.06 / 7.20 / 8.95** |
| filtered (press+INTERMEDIATE+PPL) | 281.95 / 343.36 / 460.58 (n=71) | 5.79 / 10.80 / 16.80 |
| facet by difficulty (q=press) | 366.16 / 467.49 / 747.71 (n=55) | 2.31 / 4.66 / 10.28 |

**1,000,000 templates**
| Query | PostgreSQL | OpenSearch |
|---|---:|---:|
| full-text "bench press" | 872.19 / 1199.43 / 1220.09 (n=25) | 33.64 / 45.24 / 83.90 |
| full-text "squat" | 654.97 / 854.91 / 1251.60 (n=30) | 4.18 / 8.08 / 12.55 |
| fuzzy "benhc" (typo) | **7806.25 / 17790.10 / 17790.10 (n=8)** | **3.94 / 9.18 / 13.35** |
| filtered (press+INTERMEDIATE+PPL) | 988.79 / 1092.37 / 1219.92 (n=23) | 6.24 / 14.68 / 18.19 |
| facet by difficulty (q=press) | 1178.83 / 1374.58 / 1713.44 (n=21) | 2.20 / 4.07 / 7.95 |

**2,000,000 templates**
| Query | PostgreSQL | OpenSearch |
|---|---:|---:|
| full-text "bench press" | 1863.97 / 2783.12 / 2783.12 (n=17) | 63.65 / 73.05 / 111.67 |
| full-text "squat" | 1436.95 / 1789.46 / 1789.46 (n=17) | 3.73 / 8.00 / 9.12 |
| fuzzy "benhc" (typo) | **15825.49 / 16399.71 / 16399.71 (n=6)** | **3.89 / 9.38 / 14.14** |
| filtered (press+INTERMEDIATE+PPL) | 2295.36 / 2541.77 / 2541.77 (n=15) | 7.43 / 23.65 / 40.28 |
| facet by difficulty (q=press) | 1193.41 / 1884.77 / 3898.24 (n=23) | 2.36 / 4.52 / 5.55 |

> Note: at large N the per-query time budget caps PostgreSQL cells to a handful of samples
> (`n=…`), so PG percentiles there are indicative, not tight (e.g. p95=p99 when only ~6 samples
> were collected). OpenSearch always reaches the full iteration count. The point is the order of
> magnitude, which is unmistakable.

## Analysis

The run spans **7 corpus sizes from 1k to 2M** templates, so the scaling law is measured, not
extrapolated.

1. **There is a clear crossover (~10k docs).** At 1k, PostgreSQL *wins* full-text/filtered/facets
   (sub-ms–1.5 ms vs OpenSearch's ~2–3 ms localhost-HTTP floor). By 10k OpenSearch is already ahead
   on full-text; from 50k upward it wins everything decisively. PostgreSQL full-text latency grows
   roughly **linearly** with corpus size (GIN candidate set + `ts_rank_cd` re-ranking of *every*
   match): p50 ≈ **1.5 → 10.6 → 48.7 → 100 → 779 → 872 → 1864 ms** (1k → 2M). OpenSearch stays
   **~flat at 3–64 ms** the whole way (inverted index + BM25 with block-max skipping).
2. **Fuzzy/typo search is the decisive capability gap, and it widens with scale.** OpenSearch answers
   `benhc`→`bench` in **~3–4 ms at every size** (a Levenshtein automaton over the term dictionary →
   cost depends on the number of *distinct terms*, not documents). PostgreSQL's `pg_trgm`
   `word_similarity` degrades roughly linearly with documents: p50 ≈ **14 ms → 128 ms → 622 ms →
   1.83 s → 2.45 s → 7.8 s → 15.8 s** (1k → 2M). At 1M a single typo query is ~2000× slower on
   PostgreSQL; at 2M ~4000×. Typo-tolerant search at scale is simply not viable on PostgreSQL for an
   interactive box.
3. **Build time and storage favour OpenSearch at scale.** At 2M, OpenSearch builds ~2× faster
   (115 s vs 239 s) and uses **~3.6× less disk** (739 MB vs 2.63 GB) — PostgreSQL pays for a
   `tsvector` column plus several GIN indexes (the trigram index is the heaviest).
4. **Single-threaded throughput** (≈ 1000/p50) makes it concrete: at 1M the difficulty facet is
   ~450 q/s on OpenSearch vs ~0.85 q/s on PostgreSQL; full-text ~30 vs ~1.1 q/s; fuzzy ~250 vs
   ~0.13 q/s.

### Conclusion (thesis-ready)
For small corpora and simple ranked/filtered queries, a well-indexed PostgreSQL is **simpler and
often faster** — a dedicated engine is not justified, and the architecture rightly keeps the SQL
list as the default/fallback. The case for OpenSearch is **(a) scale** — its latency stays flat
while PostgreSQL's grows roughly linearly, crossing over around ten thousand documents — and
**(b) capability**, above all **typo-tolerant fuzzy search**, which PostgreSQL can express but cannot
serve at interactive latency as the corpus grows (**≈15.8 s at 2M vs ≈4 ms**, a ~4000× gap). The
system therefore keeps PostgreSQL authoritative and uses OpenSearch as a derived read model only
where it earns its operational cost. The largest run also makes the *operational* trade-off concrete:
the OpenSearch index is a 2 M-document read model rebuildable in ~2 minutes and ~0.7 GB on disk.

## Threats to validity (own them)

- Single machine, warm caches, **single-threaded**; no concurrent-load / saturation throughput test.
- **Cold-start** (first query after index open, JIT, page-cache cold) is excluded by the warm-up.
- Synthetic data with a fixed vocabulary; real query/term distributions differ.
- Measurement is end-to-end per the app's real transport (JDBC vs HTTP+JSON), which is realistic but
  charges OpenSearch a protocol overhead PostgreSQL does not pay — relevant when reading the
  small-corpus numbers.
- Latency only; **relevance quality** (precision@k / nDCG) is not measured here.

## Documented extensions (future work)

- Add 100k / 1M sizes (lower `BENCH_ITERS` for the PG-fuzzy case, which is O(corpus)).
- Concurrency/throughput under parallel load (e.g. k6 / JMH-style multi-thread).
- Cold-cache and first-query latency.
- Relevance-quality evaluation (precision@k / nDCG) on a small labelled query set.
- A Redis result-cache layer as a third axis (cached vs uncached).

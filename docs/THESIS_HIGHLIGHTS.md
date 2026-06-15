# Thesis Highlights — notable technical work

Running list of the most impressive / non-obvious engineering decisions, for the write-up.
Brief by design — expand later. Newest at the bottom of each section.

## Architecture & data integrity
- Immutable workout session snapshots — editing/deleting source template/exercise/routine/gym/equipment never alters recorded history.
- Two-level snapshotting — template-day captures exercise name/type/muscles; session snapshots again at start.
- Soft-delete everywhere it matters (templates, exercises, routines, gyms, equipment); sessions never physically deleted.
- Package-by-feature with strict domain/application/infrastructure/web layering.
- Marketplace copy = snapshot-preserving deep copy: official exercise refs kept, foreign/custom refs + routine refs nulled, snapshots retained → copied template fully usable & independent (only link is copiedFromTemplateId).
- Entire Phase 7 (publish, vote, save, copy, stats) shipped with zero schema changes — V1 already modelled the marketplace.
- Deterministic, rule-based evidence-informed template analyzer (weighted set volume, coverage, frequency, push/pull & posterior/quad balance, rest, rep extremes, difficulty), explainable "Template Structure Score" with a fixed deduction table — no ML, honest disclaimer, never claims optimality.
- Analyzer kept architecturally clean: a template application-level read model (not web DTOs); missing planned-set counts are excluded (never fabricated) and flagged as incomplete.

## Concurrency & DB constraints
- One-active-workout-per-user enforced by a **partial unique index** (`WHERE status='IN_PROGRESS'`), not just a service check.
- DB constraint violations mapped to clean 409s (active workout, duplicate set number) instead of 500s.
- Soft-delete-aware uniqueness via partial unique indexes (`WHERE deleted_at IS NULL`) — names reusable after delete.
- Vote/save counters updated via atomic clamped SQL (`GREATEST(0, count ± delta)`) — no read-modify-write race, no negative counts; counters move only on a real row change.

## Security
- IDOR-safe by design — owner-scoped queries return 404 (not 403) so existence isn't leaked; ownership chains for nested resources.
- Refresh-token rotation — opaque tokens stored as SHA-256 hashes, rotated transactionally; reuse of a stolen token is detected.
- Access token in memory only; HttpOnly + SameSite=Strict refresh cookie; profile-based Secure flag; CORS with credentials.
- Coach mode layers **RBAC + ReBAC**: `/api/coach/**` requires `ROLE_COACH` (RBAC), and every per-client read first passes a `CoachAccess` gate that demands an *ACTIVE* coach→client relationship (ReBAC), failing with a 404 (not 403) to stay IDOR-safe. A coach can only ever read (history/analytics/session/search) — there is no coach endpoint that mutates client data, and the client can revoke access at any time.

## Performance / clever solutions
- Template aggregates recomputed Postgres-side in one native `array_agg(DISTINCT ... ORDER BY ...)` query — no Java array binding, sidesteps JPA array/`ddl-auto:validate` fragility.
- Deterministic ORDER BY inside aggregates to keep tests non-flaky.
- Batch child loads + grouping when assembling template/session detail trees (avoids N+1).
- Flush-between-clear-and-insert when re-snapshotting muscle groups (avoids composite-PK collision in one tx).
- Analytics fully Postgres-side: `date_trunc` weekly buckets, `DISTINCT ON` best-set, estimated-1RM (Epley) ranking, primary-muscle set distribution.
- Per-exercise estimated-1RM progression series (best 1RM per day per exercise) → multi-line strength-progress chart.
- History list paginated with one batch summary query (set count / volume per page) instead of N+1.
- Custom page-envelope DTO to avoid Spring `PageImpl`'s unstable JSON.

## Testing & correctness
- Real PostgreSQL in every integration test (Testcontainers, no H2); full schema exercised via Flyway.
- Tests assert DB-level guarantees, not just service behavior (e.g. raw insert rejected by the active-workout index; aggregates read back via JdbcTemplate).
- Root-caused a flaky JWT-tamper test to base64url unused bits in the HMAC signature; made the tamper deterministic.
- Handled Spring Data merge-vs-persist subtlety (pre-assigned UUID ids) by building responses from the returned managed entity so generated timestamps appear.

## Search (OpenSearch derived read model)
- A genuinely derived read model, not SQL ILIKE in disguise: denormalized documents, boosted multi-field `multi_match`, relevance ranking, fuzzy/typo tolerance (`fuzziness=AUTO`, short-query guard), search-time synonym graph (pecs→chest, ohp→overhead press), structured filters, terms + date-histogram facets, and unified highlighting.
- PostgreSQL stays authoritative; OpenSearch is eventually consistent and rebuildable. Indexing happens on `@TransactionalEventListener(AFTER_COMMIT)` (never reflects rolled-back work), explicitly *not* an outbox — failures are logged and recovered by a rebuild.
- Two upsert paths encode the consistency contract: a **structural** reindex recomputes the analyzer-derived fields (structure score, category, warning codes, missing majors), while vote/save/use does a **counters-only partial update** that never recomputes the analysis.
- Workout-history search is built entirely from immutable session snapshots (exercise/gym/equipment/muscle names), so search preserves historical truth.
- Security is a mandatory filter derived from the JWT (marketplace ⇒ PUBLIC, my ⇒ owner, workouts ⇒ owner) that request params cannot override, plus **defense-in-depth**: returned ids are re-validated against PostgreSQL so an unpublished/deleted hit that lags in the index is dropped before it reaches the client.
- Marketplace relevance uses a `function_score` `log1p` popularity boost on saves/uses layered on text relevance; vote/save state + author name are merged from PostgreSQL per page.
- Versioned indexes behind stable aliases; rebuild builds a fresh index and **atomically swaps the alias** (zero read downtime). Manual reindex is `ROLE_ADMIN`-only (ordinary users 403); the demo seeder runs it in-process so even raw-SQL-seeded marketplace data is indexed.
- Resilient by design: `app.search.enabled` gates every OpenSearch bean; when off (or OpenSearch down) the search endpoints return 503 and the UI degrades to the authoritative SQL browse/list.
- Coach-scoped client search reuses the *same* `WorkoutSearchService` with a delegated owner id (the client's), made safe by running the `CoachAccess` ReBAC gate first: the mandatory `ownerUserId = clientId` filter and the PostgreSQL defense-in-depth re-validation both apply unchanged, so a coach can full-text/filter/facet a client's history without any new query path — the one place the search runs for a non-caller owner, and only behind an ACTIVE relationship.
- Real OpenSearch 2.x in integration tests (Testcontainers, no mocks) covering full-text, fuzzy, synonyms, filters, facets, highlighting, owner/visibility isolation, the PG post-filter, the relationship-gated coach search, and admin/unauth security; the existing PostgreSQL test suite stays OpenSearch-free via the disabled flag.
- **Empirical OpenSearch-vs-PostgreSQL evaluation** (benchmark kit, `benchmark` profile): a *fair* PG baseline (weighted `tsvector`+GIN, `pg_trgm`+GIN, GROUP BY facets) vs OpenSearch over an identical streamed synthetic corpus across **7 sizes 1k→2M** (streamed corpus, constant memory; a per-query time budget caps the O(N) PG-fuzzy cell). Real result: a **crossover ≈10k docs** (PG faster below, flat OpenSearch wins above), PG full-text grows ~linearly (1.5 ms→1.86 s, 1k→2M) while OpenSearch stays flat (~3–64 ms), OpenSearch ~3.6× smaller on disk and ~2× faster to build at 2M, and **fuzzy/typo search is the decisive gap** — `pg_trgm` degrades 14 ms→622 ms→7.8 s→**15.8 s** (1k→2M) vs ~4 ms flat on OpenSearch (**~4000× at 2M**). Turns "I used a search engine" into a measured "when is it worth it" investigation (see `docs/SEARCH_BENCHMARK.md`).

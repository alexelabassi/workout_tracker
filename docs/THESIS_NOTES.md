# THESIS_NOTES.md — Master notes for the final write-up

> **Purpose.** A single, verified reference you can write the thesis from and be 100% sure of every
> claim. Everything in **Part A** was read directly from the code on 2026-06-15 (file paths given).
> Everything in **Part B** is *designed but NOT yet implemented* — written precisely enough that you
> can describe it confidently as "designed / in progress" without lying.
>
> **Status legend:** ✅ = built & verified · 🧩 = schema/scaffold already present but unwired · 🔧 = designed, not implemented.

---

# PART A — AS BUILT (verified)

## A0. One-paragraph summary

An evidence-informed workout-tracking, template-discovery, coaching, and training-history **search**
platform. Spring Boot 3.5.3 / Java 21 monolith, **PostgreSQL 16 as the single source of truth**
(Flyway migrations, Hibernate `ddl-auto: validate`), a **derived OpenSearch 2.x read model** for
full-text search, a **deterministic rule-based template analyzer**, JWT auth with refresh-token
rotation, RBAC + relationship-based access control for coaching, and an **empirical OpenSearch-vs-
PostgreSQL search benchmark**. React + TypeScript SPA built by Vite and served by Spring Boot.
Tested end-to-end with **Testcontainers** (real Postgres + real OpenSearch, no mocks, no H2):
**128 tests** green.

## A1. Tech stack & versions (exact)

| Layer | Choice | Version / note |
|---|---|---|
| Language | Java | 21 (`<java.version>21`) |
| Framework | Spring Boot | 3.5.3 (`spring-boot-starter-parent`) |
| Modules | web, data-jpa, security, validation | inherited |
| DB | PostgreSQL | 16 (`postgres:16`) |
| ORM | Hibernate 6.6.x via Spring Data JPA | `ddl-auto: validate` |
| Migrations | Flyway | `flyway-core` + `flyway-database-postgresql`, 3 migrations |
| Auth | JWT | `jjwt` 0.12.6 (api/impl/jackson), HS256 |
| Search | OpenSearch | server `opensearchproject/opensearch:2.13.0`; client `opensearch-java` 2.13.0 on Apache HttpClient5 |
| Frontend | React + TypeScript + Vite | React 18.3, react-router-dom 6.x, recharts 3.x, vite 5.x, TS 5.x |
| Tests | Testcontainers | `testcontainers:postgresql`, `org.opensearch:opensearch-testcontainers` 2.x, `spring-boot-testcontainers` |
| Packaging | Multi-stage Docker | `maven:3.9-eclipse-temurin-21` build → `eclipse-temurin:21-jre` runtime; frontend built by `frontend-maven-plugin` and copied to `classes/static` |

## A2. Architecture

- **Package-by-feature**, base package `com.thesis.workout`. Feature packages: `auth, exercise,
  routine, gym, template, session, marketplace, analytics, history, analyzer, search, coaching,
  benchmark, demo, health, shared`.
- Within each feature, **layered**: `domain/model` (JPA entities + invariants), `application`
  (services, access gates), `infrastructure/repository` (Spring Data + native SQL), `web` (controllers + DTOs).
- **CQRS-flavoured reads:** heavy reads bypass the aggregate and use purpose-built read models —
  native SQL projections (history, analytics, marketplace) and the OpenSearch derived index (search).
- **Frontend** mirrors the same feature folders under `frontend/src/features/*`, each with `api.ts` + `types.ts`.

## A3. Database schema (Flyway)

Migrations in `backend/src/main/resources/db/migration/`:
- **V1__full_initial_schema.sql** — the *entire* schema in one authoritative migration (28 tables).
- **V2__seed_official_exercises.sql** — seeds the 15 canonical muscle groups + official exercises
  (fixed UUIDs `…000000000101`–`…000000000114`).
- **V3__one_active_workout_per_user.sql** — adds the partial unique index
  `ux_workout_sessions_one_active_per_user ON workout_sessions (user_id) WHERE status = 'IN_PROGRESS'`.

**Cross-cutting schema conventions (all verified in V1):**
- **Soft delete** via `deleted_at timestamptz` on `exercises, routines, gyms, equipment,
  workout_templates, coach_session_comments`; every uniqueness rule is a **partial unique index
  `… WHERE deleted_at IS NULL`** so a deleted name can be reused.
- **Snapshot columns + nullable FKs** for historical truth (see A5 Session). Live FKs use
  `ON DELETE SET NULL`; the `_snapshot` column is authoritative.
- **CHECK constraints everywhere** (enum domains, `days_per_week BETWEEN 1 AND 7`, `rpe BETWEEN 1.0
  AND 10.0`, `finished_at >= started_at`, non-negative counters, `coach_user_id <> client_user_id`).
- **Published-visibility invariant** on `workout_templates`: `CHECK ((visibility='PUBLIC' AND
  published_at IS NOT NULL) OR visibility='PRIVATE')`.
- `template_stats` carries a `lock_version int` (optimistic-locking column) and non-negative counter CHECKs.
- Marketplace filtering uses **aggregated array columns** on `workout_templates`
  (`aggregated_muscle_groups varchar[]`, `aggregated_official_exercise_ids uuid[]`,
  `aggregated_exercise_names text[]`) each with a **GIN index**, recomputed Postgres-side.

**Table inventory (28):** `app_users, refresh_tokens, coach_profiles, coach_client_relationships,
muscle_groups, exercises, exercise_muscle_groups, routines, gyms, equipment, workout_templates,
template_stats, template_days, template_day_exercises, template_day_exercise_muscle_groups,
template_day_routines, workout_sessions, session_exercises, session_exercise_muscle_groups,
session_routines, workout_sets, coach_session_comments, template_votes, template_saves,
template_use_events, coach_template_assignments, template_analysis_results, outbox_events`.

**🧩 Four tables exist in V1 but are NOT referenced by any Java code yet** (verified by grep). They
are deliberate scaffolding and make several Part-B features "schema-ready":
- `outbox_events` (id, aggregate_type, aggregate_id, event_type, payload jsonb, status
  {PENDING/PROCESSING/PROCESSED/FAILED}, attempts, available_at, processed_at, last_error;
  index `(status, available_at, created_at)`) → **transactional outbox (B2)**.
- `template_analysis_results` (score, category, positives/warnings/notes jsonb) → **persisted analyzer output (B7)**.
- `coach_session_comments`, `coach_template_assignments` → **deferred coach features (B8)**.

## A4. Security model ✅

- **JWT access tokens:** HS256 (`jjwt`), claims `iss=workout-thesis, sub=userId, email, role,
  type=access`; **access TTL 15m, refresh TTL 14d** (configurable in `application.yml`
  `app.security.jwt.*`, secret from `JWT_SECRET`). `JwtAuthenticationFilter` sets authority
  `ROLE_<role>`; `UserPrincipal(id,email,role)` injected via `@AuthenticationPrincipal`.
- **Refresh tokens:** opaque 32-byte SecureRandom value, Base64url, **stored only as a SHA-256 hash**
  (`refresh_tokens.token_hash`, unique). **Rotated on every refresh** in one transaction (old revoked,
  new issued); a reused/stolen token hashes to a row whose `revoked_at IS NOT NULL` → rejected.
- **Password hashing:** `BCryptPasswordEncoder()` (default strength 10) — `SecurityConfig` line 81.
- **Stateless** (`SessionCreationPolicy.STATELESS`). **CSRF disabled deliberately**: the access token
  rides the `Authorization` header (not a cookie), so API calls aren't CSRF-able; the only
  credential cookie is the refresh token (**HttpOnly + SameSite=Strict + Path=/api/auth**).
- **Authorization ladder** (`SecurityConfig`, order matters): public `POST /api/auth/{register,login,
  refresh,logout}` + `GET /api/health`; `/api/admin/** → hasRole(ADMIN)`; `/api/coach/** →
  hasRole(COACH)`; `/api/** → authenticated`; everything else (SPA) permitted.
- **IDOR-safe by design:** every owned resource is fetched with `…ByIdAndUserId…` and a miss yields a
  **404, never 403** (existence is not leaked). Nested resources chain ownership up to the root.
- **CORS** restricted to configured origins with credentials (for the Vite dev origin); same-origin in prod.

## A5. Domain features ✅

- **Auth** (`/api/auth/*`, `/api/auth/me`): register/login/refresh/logout, see A4. `Role{USER,COACH,ADMIN}`.
- **Exercise** (`/api/exercises*`): OFFICIAL (owner=null, seeded) vs CUSTOM (owner set). CHECK enforces
  `(OFFICIAL ∧ owner NULL) ∨ (CUSTOM ∧ owner NOT NULL)`. Muscle groups with role PRIMARY/SECONDARY.
- **Routine** (`/api/routines`): user-owned reusable warm-up/cooldown text blocks (type START/END).
- **Gym + Equipment** (`/api/gyms*`, `/api/equipment/*`): user-owned; equipment denormalizes `user_id`.
- **Template** (`/api/templates*`): program → days → day-exercises (+ muscle groups) → day-routines.
  Snapshots (`exercise_name_snapshot`, etc.) so a template survives edits/deletes of source exercises.
  `publish`/`unpublish` toggle marketplace visibility (enforced by the published-visibility CHECK).
  `TemplateAccess` is the ownership gate. Aggregated arrays recomputed Postgres-side via one native
  `array_agg(DISTINCT … ORDER BY …)` query (no JPA array binding; survives `ddl-auto: validate`).
- **Session** (`/api/workouts/*`, `/api/session-exercises/*`, `/api/sets/*`): **immutable history.**
  `WorkoutSession`/`SessionExercise`/`WorkoutSet` snapshot template/day/gym/exercise/equipment/muscle
  **names** at the moment of logging; the live FKs (`template_id, template_day_id, gym_id,
  original_exercise_id, equipment_id`) are nullable with `ON DELETE SET NULL` and are **never read back**
  for the session view. **One active workout per user** is guaranteed by the V3 partial unique index;
  the losing concurrent insert raises a unique violation mapped to **409 `ACTIVE_WORKOUT_EXISTS`**
  (the service pre-check is only a friendly fast path).
- **History** (`/api/history`): paged finished/cancelled sessions; one batch summary query computes
  per-page exerciseCount/setCount/totalVolume (avoids N+1). **OFFSET pagination** (`PageRequest`).
- **Marketplace** (`/api/marketplace/*`): browse PUBLIC templates (sort newest/top-rated/trending,
  **native SQL `LIMIT/OFFSET`**, optional `savedOnly` + filters); `vote` (UP/DOWN, no self-vote,
  toggle/switch), `save`/`unsave`, `use` (deep-copies into a PRIVATE owned copy, keeping only OFFICIAL
  exercise FKs, nulling routine FKs, retaining snapshots). Counters live in `template_stats` and are
  updated with **atomic clamped SQL deltas** (no read-modify-write race, no negative counts);
  `template_use_events` records each copy with name snapshots.
- **Analytics** (`/api/analytics/overview`): fully Postgres-side (see A6).
- **Coaching** (see A7).

## A6. Analytics read model ✅

Native SQL in `analytics/.../AnalyticsQueryRepository`:
- **volumeOverTime:** `SUM(weight*reps)` per `started_at::date`.
- **workoutsPerWeek:** `count(*)` per `date_trunc('week', started_at)` (Monday-anchored).
- **primaryMuscleSetDistribution:** count of logged sets where `role_snapshot='PRIMARY'`, grouped by muscle.
- **bestSetsPerExercise:** `DISTINCT ON (exercise_name_snapshot)` ordered to pick the top set, with
  estimated 1RM = **Epley**: `round(weight * (1 + reps/30.0), 1)` (only weighted, repped sets qualify).
- **oneRepMaxOverTime:** daily best Epley 1RM per exercise → multi-line strength-progression chart (Recharts).

## A7. Coaching: RBAC + ReBAC ✅

- **Relationship lifecycle** (`coach_client_relationships`, status PENDING/ACTIVE/REVOKED/REJECTED):
  coach invites by email → PENDING → client `accept`→ACTIVE / `reject`→REJECTED; either side `revoke`
  an ACTIVE one. Partial unique index allows only one *live* (PENDING|ACTIVE) pair; CHECK forbids self-coaching.
- **Two-layer authorization:** **RBAC** (`/api/coach/** → ROLE_COACH`) is necessary but not
  sufficient; **ReBAC** is the `CoachAccess.requireActiveClient(coach, client)` gate that demands an
  **ACTIVE** relationship and otherwise throws → **404 (not 403)** so a coach can't probe which users exist.
- **Read-only coach access**: history, analytics, single session, and **full-text search over the
  client's workouts** (`GET /api/coach/clients/{id}/search/workouts`) — the one place the workout
  search runs with a non-caller owner id, safe because it passes `CoachAccess` first and then reuses
  the same `WorkoutSearchService` with the client's id (the mandatory owner filter + PG re-validation
  still apply). There is **no coach endpoint that mutates** client data.
- **Client side** (`/api/coaching/*`, any authed user): list invites, accept/reject, list coaches, revoke.
- **Token caveat:** promoting a user to COACH requires re-login/refresh to mint a token with the new role claim.

## A8. Evidence-informed template analyzer ✅ (thesis centerpiece)

`analyzer/application/TemplateAnalyzerService.java`, exposed at **`GET /api/templates/{id}/analysis`**.
Deterministic, rule-based, explainable — **no ML, no personalization, no medical claims** — with an
explicit DISCLAIMER and a `limitations` list. Also invoked during search indexing to populate the
index's `templateStructureScore / analysisCategory / warningCodes / missingMajorMuscles`.

- **Model:** counts only `STRENGTH` exercises; **weighted weekly sets** per muscle bucket with
  `PRIMARY = 1.0`, `SECONDARY = 0.5` (max per exercise). 15 muscle codes folded into 11 buckets;
  6 **major** buckets = {CHEST, BACK, SHOULDERS, QUADS, HAMSTRINGS, GLUTES}.
- **Score = sum of five sub-scores, each capped at 0 (total /100):**
  - **Volume & coverage (35):** penalises missing region (no lower body −12, no posterior −8, no pull
    −12, no push −12), missing major (−6) / minor (−2) muscle, and per-muscle volume bands
    (≤3 very low, ≤7 low for ≥intermediate, >20 high, >25 excessive).
  - **Frequency (20):** penalises ≥10 weekly sets for a muscle all on one day (−4 each).
  - **Balance (20):** ratio checks — pull/push (BACK+BICEPS vs CHEST+SHOULDERS+TRICEPS),
    posterior/quads, lower/upper — with HIGH/MEDIUM thresholds (e.g. pull/push <0.5 HIGH −8, <0.7 MEDIUM −4).
  - **Session design (15):** per-day set count (>30 excessive −5, >20 long −2, ≤8 short −2) and
    single-muscle concentration ≥10 sets/day (−3).
  - **Specificity & rest (10):** rest-interval heuristics (short rest on compound/isolation) and
    rep-range skew (≥90% sets ≤5 reps strength-biased, or ≥30 reps endurance-biased).
- **Difficulty heuristics:** BEGINNER-specific warnings (total volume >70, per-muscle >18, >6
  sessions/week, dense sessions, no compounds); advanced-only note for per-muscle >25.
- **Category thresholds:** **≥80 WELL_STRUCTURED, ≥55 DECENT_STRUCTURE, else NEEDS_REVIEW.**
- **Output:** overall score, category, sub-scores, per-muscle weekly volumes + frequencies, the three
  balance ratios, a list of explainable `warnings` (code, severity INFO/LOW/MEDIUM/HIGH, title,
  heuristic explanation, affected muscles, suggested fix), de-duplicated suggestions, strengths, and
  the honest `limitations` (can't see RIR/RPE; incomplete if planned sets missing; rear-delt not
  separable; difficulty defaults to INTERMEDIATE if unset).

## A9. Search: OpenSearch derived read model ✅

- **Client:** `opensearch-java` 2.13.0 over Apache HttpClient5, connect 2s / socket 5s; every search
  bean is gated by `@ConditionalOnProperty(app.search.enabled)` (default **false** — local `mvn` and
  the Postgres test suite stay OpenSearch-free).
- **Two indexes** (`resources/search/templates_v1.json`, `workout_sessions_v1.json`), versioned names
  behind stable aliases `templates` / `workout_sessions`.
- **Analyzers:** index-time `workout_text` = standard + lowercase + asciifolding; search-time
  `workout_text_search` adds a **synonym_graph** (e.g. `pecs→chest`, `ohp→overhead press`,
  `rdl→romanian deadlift`, `quads/quad/quadriceps`, `lats/lat`, …).
- **Boosts** (best_fields multi_match): templates `name^4, exerciseNames^3, muscleGroupsText^2,
  dayNames^1.5, description^1, routine*^0.5`; workouts `exerciseNameSnapshots^4, templateNameSnapshot^2.5,
  gymNameSnapshot^2, equipment^1.5, muscleGroupsText^1.5, dayName^1, notes^1`.
- **Typo tolerance:** `fuzziness=AUTO, prefix_length=1`, **guarded** so queries <3 chars aren't fuzzed.
- **Marketplace popularity:** the text query is wrapped in a `function_score` with two `log1p`
  `field_value_factor` terms (`savesCount`×1.0, `usesCount`×0.7), summed onto relevance.
- **Filters / facets / highlighting:** exact-match `filter` clauses (don't affect score); `terms`
  aggregations + a per-month `date_histogram` for workouts; unified highlighter wraps `<mark>…</mark>`.
- **Indexing pipeline (write path):** `@TransactionalEventListener(AFTER_COMMIT)` in
  `search/infrastructure/event/SearchIndexEventListener` consumes `TemplateIndexEvent{STRUCTURAL,STATS,
  REMOVE}` and `WorkoutSessionIndexEvent{UPSERT,REMOVE}` published by the domain services. **Two upsert
  paths encode the consistency contract:** a **structural** reindex rebuilds the full document and
  recomputes analyzer-derived fields; a **stats-only partial update** touches only popularity counters
  (vote/save/use) and never recomputes analysis. All writes are **idempotent full-document upserts by
  id**. Failures are logged (WARN) and swallowed — Postgres stays authoritative; rebuild recovers.
  This is **intentionally not a transactional outbox** (see B2; the table even exists).
- **Two-layer security:** (1) a **mandatory JWT-derived scope filter** params can't override
  (`marketplace ⇒ visibility=PUBLIC`, `my ⇒ ownerUserId=caller`, `workouts ⇒ ownerUserId=caller`);
  (2) **PostgreSQL defense-in-depth**: returned ids are re-validated against Postgres with the same
  rule, dropping any stale hit (just-unpublished/deleted), and merging live vote/save + author name
  for marketplace. So `items.length ≤ totalHits`.
- **Rebuild:** `SearchRebuildService` builds a fresh timestamped index, bulk-indexes from Postgres,
  refreshes, then **atomically swaps the alias** (zero read downtime) and deletes the old index; on
  failure the new index is dropped and the old one keeps serving. Manual trigger
  **`POST /api/admin/search/reindex` (ROLE_ADMIN only)**; the demo seeder runs the same rebuild
  in-process on boot so raw-SQL-seeded marketplace data gets indexed without an admin.
- **Resilience:** `app.search.enabled=false` or OpenSearch down ⇒ `/api/search/**` returns **503
  SEARCH_UNAVAILABLE** and the SPA falls back to the SQL list; a not-yet-built index
  (`ignore_unavailable`+`allow_no_indices`, 404 caught) ⇒ **empty results, not 503**.

## A10. Empirical OpenSearch-vs-PostgreSQL benchmark ✅

`benchmark/*` (Spring profile `benchmark`), documented in `docs/SEARCH_BENCHMARK.md`.
- **Fair PG baseline** (`PostgresSearchBaseline`): weighted `tsvector` (`setweight` A/B/C/D) + GIN,
  ranked by `ts_rank_cd`; fuzzy via `pg_trgm` + GIN (`word_similarity`); GROUP BY facets; bulk-load-
  then-index. **OpenSearch side** mirrors the production mapping and `TemplateSearchService` queries.
- **Corpus** (`SyntheticCorpus`, seed 42, **streamed in batches** so memory is constant): realistic
  lifting vocabulary, skewed popularity, 90/10 PUBLIC/PRIVATE, 200 owners; sizes **1k → 2M** (7 points).
- **5 query scenarios:** full-text "bench press", full-text "squat", **fuzzy "benhc"**, filtered
  (press + INTERMEDIATE + PPL), facet-by-difficulty. Single-threaded, warm-up + budgeted iterations,
  p50/p95/p99 (nearest-rank).
- **Headline results (documented):** **crossover ≈ 10k docs** (PG faster below, OpenSearch above);
  PG full-text grows ~linearly (~1.5 ms → ~1.86 s, 1k→2M) while OpenSearch stays flat (~3–64 ms);
  **fuzzy is the decisive gap** — `pg_trgm` degrades 14 ms → 622 ms → 7.8 s → **15.8 s** (1k→2M) vs
  **~4 ms flat** on OpenSearch (**~4000× at 2M**); OpenSearch ~**3.6× smaller on disk** and ~**2×
  faster to build** at 2M. *(Full per-size tables in `docs/SEARCH_BENCHMARK.md`.)*

## A11. Testing ✅

- **Real infrastructure, no mocks/H2:** `AbstractPostgresIntegrationTest` (singleton Postgres
  container) and `AbstractSearchIntegrationTest` (adds a real OpenSearch container + flips
  `app.search.enabled=true`). The Postgres suite stays OpenSearch-free.
- **128 tests** across auth, exercise, routine, gym, equipment, template, session (20), history,
  analytics, marketplace, analyzer, coaching (9), search (11, incl. coach search) + unit tests
  (JWT, health). Tests assert **DB-level guarantees** (e.g. raw insert rejected by the active-workout
  index; aggregates read back via JdbcTemplate), not just service behaviour.

## A12. Demo data ✅

Profile `demo` (`DemoDataSeeder`): seeds `demo@workout.app` with **deep multi-year history
(2020→now, ~600 sessions** incl. a 2020 COVID-lockdown session, a Bali trip, a PR day, strongman work)
so date-filtered "find a specific 2020 workout" search demos are real; a COACH (`coach@workout.app`)
with two ACTIVE clients + a pending invite; a 50-template marketplace; then an in-process search-index
rebuild. All demo passwords `demo12345`. A persistent `opensearch-data` Docker volume keeps the index
across restarts. **Always start the stack with `SPRING_PROFILES_ACTIVE=demo`** so the rebuild runs.

## A13. Known limitations (honest, as documented)

Redis caching not yet built; no transactional outbox (best-effort after-commit indexing); no NL query
parsing; OpenSearch container runs security-disabled (local/demo only); no auth rate limiting;
OFFSET (not keyset) pagination; analyzer can't see effort/RIR; the demo `seedMarketplace` step is slow.

---

# PART B — DESIGNED, NOT YET IMPLEMENTED

> Write about these as "designed / in progress". For each: the problem, the exact design, **where it
> plugs into the current code**, what to claim, rough effort, and the academic concept to name.

## B1. 🔧 Redis cache-aside for the marketplace (flagship)

- **Problem.** `GET /api/marketplace/templates` hits Postgres on every browse with native `LIMIT/OFFSET`
  + GROUP BY counters; hot lists recompute repeatedly.
- **Design.** Cache-aside (lazy) with Spring Data Redis. Key classes by access pattern with different
  TTLs: hot marketplace list pages short TTL, template-detail longer. **Event-driven invalidation
  reusing the existing `@TransactionalEventListener(AFTER_COMMIT)` events** (publish/unpublish/vote/
  save/use already fire `TemplateIndexEvent`) → evict the affected keys. **Stampede / thundering-herd
  protection** via single-flight locks *or* probabilistic early expiration (**XFetch**, Vattani et al. 2015).
- **Plugs into.** `MarketplaceQueryService` (read path) + the existing index-event listeners (eviction).
  Redis is already on the roadmap ("Cache later: Redis").
- **Write.** cache-aside vs read-through vs write-through; per-key-class TTL; event-driven
  invalidation; XFetch with a citation; a *third benchmark axis* (cached vs uncached p99 + hit ratio).
- **Effort.** Moderate.

## B2. 🔧 Transactional outbox for OpenSearch indexing (the `outbox_events` table already exists 🧩)

- **Problem.** Current indexing is best-effort after-commit — the classic **dual-write problem**
  (commit Postgres, then index; a crash in between drifts the index).
- **Design.** In the *same transaction* as the domain change, insert an `outbox_events` row
  (the table is already in V1: `aggregate_type, aggregate_id, event_type, payload jsonb, status,
  attempts, available_at, processed_at, last_error`). A `@Scheduled` **relay** polls
  `WHERE status='PENDING' AND available_at<=now()` (the index `outbox_events_processing_idx
  (status, available_at, created_at)` is already built — pairs perfectly with **`FOR UPDATE SKIP
  LOCKED`**, see B11), ships to the indexer with **at-least-once delivery + retry/backoff**, and marks
  PROCESSED. The consumer is **already idempotent** (full-document upsert by id) — you can state this
  truthfully. Discuss ordering and compare against **CDC/Debezium** as the alternative.
- **Plugs into.** Replaces the publish side in the domain services + the listener in
  `search/infrastructure/event`; reuses the existing indexers unchanged.
- **Write.** dual-write problem → outbox → relay → idempotent consumer → CDC trade-off. The most
  "CS-respectable" addition; emphasise that **the schema was provisioned for it from V1**.
- **Effort.** Moderate (entity + repository + relay; table already there).

## B3. 🔧 Relevance-quality evaluation (precision@k / nDCG / MRR)

- **Problem.** The benchmark measures *speed*, not *result quality*. "Fast" ≠ "good".
- **Design.** A small labelled judgment set (query → relevant template ids over the synthetic corpus),
  then standard IR metrics — **precision@k, nDCG@k (with the standard DCG = Σ relᵢ/log₂(i+1))**, MRR —
  comparing **PostgreSQL `ts_rank_cd` vs OpenSearch BM25**, plus an **ablation** (synonyms on/off,
  fuzziness on/off, boosted vs flat fields). Expectation: BM25 + synonyms/fuzzy wins recall/nDCG.
- **Plugs into.** Reuses the `benchmark` profile harness + `SyntheticCorpus`; read-only, **no schema
  change, no new infra** → lowest-risk, highest thesis payoff.
- **Write.** methodology, the nDCG formula, ground-truth construction, expected outcome. Few bachelor
  theses do real IR evaluation — this turns "I used a search engine" into "I evaluated retrieval quality."
- **Effort.** Low–moderate.

## B4. 🔧 Keyset (cursor) pagination

- **Problem.** Current deep pagination is **OFFSET** (verified: marketplace native `LIMIT/OFFSET`,
  history `PageRequest`). `OFFSET n` scans and discards n rows → O(n) for deep pages.
- **Design.** Seek method: `WHERE (started_at, id) < (:lastStarted, :lastId) ORDER BY started_at DESC,
  id DESC LIMIT :n`, backed by the existing composite index `sessions_user_started_idx (user_id,
  started_at DESC)`. O(log n) index seek + range scan.
- **Plugs into.** `HistoryService` / `MarketplaceQueryService`. Can fold into the benchmark as a
  deep-page latency curve (offset@page-10,000 vs keyset@page-10,000).
- **Write.** complexity argument O(n)→O(log n); the stable composite sort key. **Effort.** Low–moderate.

## B5. 🔧 Rate limiting on auth (Redis token bucket)

- **Problem.** No rate limiting on `/api/auth/*` (documented limitation) → credential-stuffing/brute force.
- **Design.** **Token-bucket** per IP+account on login/register/refresh, **Redis-backed with an atomic
  Lua script** for correctness under concurrency. Discuss token-bucket vs leaky-bucket vs
  sliding-window; distributed counting in Redis. Pairs with B1's Redis.
- **Plugs into.** A filter in front of the auth controllers. **Write.** algorithm comparison +
  distributed rate limiting. **Effort.** Low–moderate.

## B6. 🔧 Argon2id password hashing + transparent rehash-on-login

- **Problem.** Currently `BCryptPasswordEncoder` (verified). **Argon2id** is the current OWASP
  recommendation (memory-hard, GPU-resistant).
- **Design.** Switch the `PasswordEncoder` to a `DelegatingPasswordEncoder` (prefix-tagged hashes,
  e.g. `{argon2}…` / `{bcrypt}…`) so both verify; on a **successful login**, if the stored hash is
  bcrypt, **re-hash with Argon2id and persist** — a zero-downtime, no-forced-reset migration.
- **Plugs into.** `SecurityConfig.passwordEncoder()` + a re-hash hook in `AuthService.login`.
- **Write.** memory-hard KDFs, Argon2id parameters (memory/iterations/parallelism), transparent
  upgrade pattern. **Effort.** Small, elegant.

## B7. 🔧 Persisted analyzer results (the `template_analysis_results` table already exists 🧩)

- **Problem.** The analyzer recomputes on every call and during every structural reindex.
- **Design.** Persist each analysis run into `template_analysis_results` (score, category,
  positives/warnings/notes jsonb, created_at — table already in V1, indexed `(template_id, created_at
  DESC)`); serve the latest cached row and recompute only on template change. Enables **analysis
  history / trend over time** as a bonus.
- **Plugs into.** `TemplateAnalyzerService` (write-through after compute) + the search assembler (read
  cached instead of recompute). **Effort.** Low. **Note in the thesis:** schema was provisioned for this in V1.

## B8. 🔧 Coach session comments + template assignments (tables already exist 🧩)

- **Problem.** Coach access is read-only; the natural next step is collaborative feedback + programming.
- **Design.** `coach_session_comments` (coach comments on a client session) and
  `coach_template_assignments` (coach assigns a template to a client; status ASSIGNED/COMPLETED/REVOKED)
  — **both tables already in V1**. All writes gated by the existing `CoachAccess` ACTIVE-relationship check.
- **Plugs into.** New endpoints under `/api/coach/clients/{id}/…`, reusing `CoachAccess`.
- **Write.** extends ReBAC from read-only to scoped collaborative write. **Effort.** Moderate; schema-ready.

## B9. 🔧 Observability: distributed tracing + metrics (OpenTelemetry → Prometheus/Grafana/Tempo)

- **Design.** Micrometer (already transitively present) + OTel exporter; trace `HTTP → SearchService →
  OpenSearch → PG defense-in-depth post-filter`; RED metrics + p50/p95/p99 histograms; Grafana dashboard.
- **Write.** SLOs, where latency actually goes (a trace of the coach search or the PG re-validation
  step), capacity. **Amplifies every other chapter** with hard numbers. Best paired with a k6/Gatling
  load test → a "system capacity characterization" chapter. **Effort.** Moderate.

## B10. 🔧 Time-based partitioning of `workout_sessions`

- **Design.** Range-partition by year/month (declarative partitioning); show **partition pruning** in
  `EXPLAIN` for a date-filtered history query; latency before/after; retention story. Ties directly to
  the deep 2020→now demo history and the benchmark theme. **Effort.** Moderate–high (one migration).

## B11. 🔧 PostgreSQL `FOR UPDATE SKIP LOCKED` job queue

- **Design.** Infra-free concurrent work queue; multiple workers pull jobs without blocking. Natural
  engine for the **B2 outbox relay** (the `outbox_events_processing_idx` already suits it) or async
  analytics recompute. **Write.** Postgres concurrency depth, no Kafka/Redis needed. **Effort.** Moderate.

## B12. 🔧 Idempotency keys for mutating endpoints

- **Design.** Stripe-style `Idempotency-Key` header on `POST /api/workouts/start`, set logging, publish,
  use — dedup via a stored-key table so retries are exactly-once under network failure. **Write.** safe
  retries, at-most-once side effects. **Effort.** Low–moderate.

## B13. 🔧 Precomputed analytics (materialized view, `REFRESH … CONCURRENTLY`)

- **Design.** Move the heavy aggregations (A6) into a materialized view / incrementally-maintained
  summary table refreshed concurrently. **Write.** query latency before/after; staleness/refresh
  trade-off. **Effort.** Moderate.

---

## Appendix — exact identifiers worth quoting verbatim

- Active-workout guard: `ux_workout_sessions_one_active_per_user … WHERE status='IN_PROGRESS'` → 409 `ACTIVE_WORKOUT_EXISTS`.
- Published invariant: `CHECK ((visibility='PUBLIC' AND published_at IS NOT NULL) OR visibility='PRIVATE')`.
- Anti-self-coach: `CHECK (coach_user_id <> client_user_id)`; live-relationship uniqueness `WHERE status IN ('PENDING','ACTIVE')`.
- Epley 1RM: `weight * (1 + reps/30.0)`.
- Analyzer category cutoffs: `≥80 WELL_STRUCTURED`, `≥55 DECENT_STRUCTURE`, else `NEEDS_REVIEW`; weighted sets PRIMARY 1.0 / SECONDARY 0.5; sub-scores 35/20/20/15/10.
- Search scope filters (non-overridable): marketplace→`visibility=PUBLIC`, my→`ownerUserId=caller`, workouts→`ownerUserId=caller`.
- Endpoints: `GET /api/templates/{id}/analysis`, `GET /api/search/{templates,workouts}`, `POST /api/admin/search/reindex`, `GET /api/coach/clients/{id}/search/workouts`.
- Scaffold tables present in V1 but unwired: `outbox_events`, `template_analysis_results`, `coach_session_comments`, `coach_template_assignments`.

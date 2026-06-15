# SEARCH_FEATURE.md — OpenSearch search, end to end (as built)

This is the detailed, authoritative reference for the search feature (Phase 11). It documents what
the code actually does, where every piece lives, how to use it from the UI, and how to run/operate
it. The higher-level plan/strategy lives in `SEARCH_AND_CACHING.md`; this file is the implementation
deep-dive. The empirical OpenSearch-vs-PostgreSQL evaluation (latency / scale / build time / disk
size, with a fair PG baseline) lives in `SEARCH_BENCHMARK.md`, produced by the benchmark kit in
`com.thesis.workout.benchmark`.

---

## 1. What it is, in one paragraph

PostgreSQL is the source of truth. OpenSearch is a **derived, eventually-consistent, rebuildable**
read model used only for search. Two indexes — `templates` (your programs + the public marketplace)
and `workout_sessions` (your finished/cancelled workouts) — hold flat, **denormalized** documents so
a query needs no joins. Writes in Postgres publish an **after-commit event**; a listener rebuilds the
affected document. Queries do boosted multi-field full-text with **typo tolerance, synonyms,
structured filters, facets, highlighting**, and (for the marketplace) a **popularity boost**.
Security is a mandatory JWT-derived filter plus a **defense-in-depth re-check against Postgres**.
If search is disabled or OpenSearch is down, `/api/search/**` returns 503 and the UI falls back to
the normal SQL list; if the index simply hasn't been built yet, search returns **empty results**.

---

## 2. How to use it on the frontend

The SPA is served at `http://localhost:8080/`. Demo login: `demo@workout.app` / `demo12345`
(seeded only under the `demo` profile). There is a search box on three pages. Typing a query
switches that page into "search mode"; clearing the box returns to the default SQL list.

### 2.1 Marketplace page (`/marketplace`) — scope = marketplace (PUBLIC templates)
- Search box at the top: type and press Enter / click **Search**.
- **It searches more than the name.** Matched fields (highest weight first):
  `name`, `exerciseNames`, `muscleGroups` (text), `dayNames`, `description`, warm-up/cool-down
  `routine` text. So `bench` finds programs that *contain* the bench press even if the program is
  called "Full Body Program 12".
- Results show the matched text **highlighted** (`<mark>`), the author, structure category badge,
  saves/uses, and **View / Use** actions.
- **Facet chips** (Difficulty / Split / Days/week / Muscle group / Structure) appear with counts;
  click one to filter the current query, click again to clear it.
- Ordering blends **text relevance + popularity** (more saves/uses rank higher).
- **Clear** returns to the Newest / Top / Trending SQL browse.
- Demo queries to try: `full`, `programm` (typo → still matches), `push` (popular first),
  `bench` / `chest` / `pecs` (by exercise/muscle, not name).

### 2.2 Templates page (`/templates`) — scope = my (your own programs)
- Search box searches **only your templates** (private + public you own).
- Same field coverage as marketplace. Try `bench`, `chest`, or `pecs` → finds **"Demo Full Body"**
  (the synonym `pecs → chest` matches its bench press even though "pecs" appears nowhere).
- Results link to the builder; facet chips + **Clear** behave as above.

### 2.3 History page (`/history`) — your finished/cancelled workouts
- Search box searches the workout **snapshots**: `exerciseNameSnapshots`, `templateNameSnapshot`,
  `gymNameSnapshot`, `equipmentNameSnapshots`, `muscleGroups`, `templateDayNameSnapshot`, and your
  `notes`.
- Try `bench` / `squat` (exercise), a gym name, a piece of equipment, or words from a note.
- Facets: **Status**, **Gym**, **Muscle group**, plus a read-only **By month** histogram.
- **Clear** returns to the paged history list.

### 2.4 What it is *not*
Keyword/field search, not natural language. `bench last month at my gym` won't parse "last month" —
type `bench` and use the date/gym facets to narrow. (Terminology: a "routine" here is a
warm-up/cool-down block; the thing you build and search is a **template/program**.)

---

## 3. Architecture & consistency model

```
            write (POST/PUT/DELETE)                read (GET /api/search/**)
  client ───────────────► Spring service ─┐        client ──► SearchController
                              commit (PG)  │                      │
                                           ▼                      ▼
                    @TransactionalEventListener(AFTER_COMMIT)   TemplateSearchService /
                                           │                    WorkoutSearchService
                           reads committed PG state                   │  (OpenSearch query)
                                           ▼                          ▼
                                Indexer ──► OpenSearch ◄────────── search hits
                                                                      │
                                                          re-validate IDs against PG
                                                                      ▼
                                                              response to client
```

- **Authoritative store:** PostgreSQL. OpenSearch can be wiped and rebuilt at any time.
- **Eventually consistent:** indexing happens *after commit*, so the index never reflects
  rolled-back work; there can be a brief lag, handled by the defense-in-depth re-check (§7).
- **Not an outbox:** if OpenSearch is briefly down, the indexing failure is logged and swallowed;
  recovery is a rebuild (§8). This is a deliberate trade-off (simplicity over guaranteed delivery)
  appropriate for a derived search index.
- **Read-after-write for the demo/tests:** single-document writes use `refresh=wait_for`, so a
  change is searchable as soon as the write call returns.

---

## 4. The two indexes

Versioned concrete indexes (`templates_v1`, `workout_sessions_v1`) are addressed only through stable
**aliases** (`templates`, `workout_sessions`) so a rebuild can atomically swap to a fresh index.
Mappings live in `backend/src/main/resources/search/{templates_v1,workout_sessions_v1}.json`.

### 4.1 `templates` document (`TemplateDocument`)
| field | type | purpose |
|---|---|---|
| `templateId` | keyword | doc id |
| `ownerUserId` | keyword | security filter (scope=my) |
| `visibility` | keyword | security filter (scope=marketplace ⇒ PUBLIC) |
| `name` | text (+`.keyword`) | full-text, boost ^4 |
| `description` | text | full-text, boost ^1 |
| `splitType`, `difficulty` | keyword | filter + facet |
| `daysPerWeek`, `estimatedDurationMinutes` | integer | filter/facet |
| `publishedAt`, `createdAt` | date (epoch millis) | sort/recency |
| `copiedFromTemplateId` | keyword | provenance |
| `exerciseNames` | text (+`.keyword`) | full-text ^3 |
| `muscleGroups` | keyword | filter + facet |
| `muscleGroupsText` | text | full-text ^2 (enables `chest`/`pecs`) |
| `dayNames` | text | full-text ^1.5 |
| `dayFocuses` | keyword | facet/filter |
| `routineNameSnapshots`, `routineContentSnapshots` | text | full-text ^0.5 |
| `ratingScore` | double | (available) |
| `upvotesCount`, `downvotesCount`, `savesCount`, `usesCount` | integer | popularity boost + display |
| `templateStructureScore` | integer | analyzer-derived; filter (`minScore`) |
| `analysisCategory` | keyword | analyzer-derived; facet/filter |
| `warningCodes`, `missingMajorMuscles` | keyword | analyzer-derived; display/filter |

### 4.2 `workout_sessions` document (`WorkoutSessionDocument`)
Built entirely from immutable session snapshots, so search reflects historical truth.
| field | type | purpose |
|---|---|---|
| `sessionId` | keyword | doc id |
| `ownerUserId` | keyword | mandatory owner filter |
| `status` | keyword | filter + facet (FINISHED/CANCELLED) |
| `startedAt`, `finishedAt` | date (epoch millis) | date-range filter, month facet |
| `durationSeconds` | long | duration filter |
| `templateNameSnapshot` | text (+`.keyword`) | full-text ^2.5 |
| `templateDayNameSnapshot` | text | full-text ^1 |
| `gymNameSnapshot` | text (+`.keyword`) | full-text ^2, gym facet/filter |
| `exerciseNameSnapshots` | text (+`.keyword`) | full-text ^4, exercise filter |
| `muscleGroups` | keyword | filter + facet |
| `muscleGroupsText` | text | full-text ^1.5 |
| `equipmentNameSnapshots` | text (+`.keyword`) | full-text ^1.5, equipment filter |
| `notes` | text | full-text ^1 |
| `totalVolume`, `setCount`, `exerciseCount` | numeric | volume filter + display |

Only FINISHED/CANCELLED sessions are indexed (an in-progress workout is not history yet).

---

## 5. Analyzers & synonyms

Defined in both index mapping JSONs:
- `workout_text` (index time): `standard` tokenizer + `lowercase` + `asciifolding`.
- `workout_text_search` (search time): the above **plus a `synonym_graph` filter**.

Applying synonyms only at **search time** means new synonyms don't require a reindex. Synonym groups:
```
pecs, pec, chest
quads, quad, quadriceps
hams, hammies, hamstring, hamstrings
glutes, glute, butt
delts, delt, shoulders, shoulder
lats, lat
abs, ab, core, abdominals
bis, bicep, biceps
tris, tricep, triceps
calf, calves
ohp, overhead press
rdl, romanian deadlift
bench, bench press
```
Text fields carry a `.keyword` sub-field so the same field can be used for exact-match filters and
facets (e.g. filter workouts by an exact `gymNameSnapshot.keyword`).

---

## 6. Indexing pipeline (write path)

Events are published from the services and consumed after commit.

- Events: `search/application/event/TemplateIndexEvent` (`STRUCTURAL` | `STATS` | `REMOVE`) and
  `WorkoutSessionIndexEvent` (`UPSERT` | `REMOVE`).
- Listener: `search/infrastructure/event/SearchIndexEventListener`
  (`@TransactionalEventListener(phase = AFTER_COMMIT)`, try/catch + log, conditional on
  `app.search.enabled`).
- Document assembly (in a read transaction): `TemplateDocumentAssembler`,
  `WorkoutSessionDocumentAssembler`. The template assembler also runs the rule-based analyzer to
  populate the structural fields.
- Writers: `TemplateIndexer`, `WorkoutSessionIndexer` (index / partial-update / remove / bulk).

**Two template write paths encode the consistency contract:**
- **STRUCTURAL** — rebuild the whole document *including* the analyzer fields.
- **STATS** — partial update of the popularity counters only; the analyzer is **not** recomputed.
  If the document is missing, it falls back to a full structural index.

### 6.1 Which action triggers what
| Action (service) | Event |
|---|---|
| Create / update template (`TemplateService`) | template STRUCTURAL |
| Soft-delete template (`TemplateService`) | template REMOVE |
| Day create/update/delete (`TemplateDayService`) | template STRUCTURAL |
| Day-exercise add/update/delete (`TemplateDayExerciseService`) | template STRUCTURAL |
| Day-routine attach/remove (`TemplateDayRoutineService`) | template STRUCTURAL |
| Publish / unpublish (`TemplatePublishingService`) | template STRUCTURAL |
| Copy/use (`TemplateCopyService`) | new copy STRUCTURAL + source STATS |
| Vote / clear-vote / save / unsave (`TemplateInteractionService`) | template STATS |
| Workout finish / cancel / notes (`WorkoutSessionService`) | session UPSERT |

---

## 7. Query path (read path) & security

Built in `TemplateSearchService` / `WorkoutSearchService`.

1. **Boosted `multi_match`** over the fields/boosts in §4 (`best_fields`).
   - Templates: `name^4, exerciseNames^3, muscleGroupsText^2, dayNames^1.5, description^1, routine*^0.5`
   - Workouts: `exerciseNameSnapshots^4, templateNameSnapshot^2.5, gymNameSnapshot^2,
     equipmentNameSnapshots^1.5, muscleGroupsText^1.5, templateDayNameSnapshot^1, notes^1`
2. **Typo tolerance:** `fuzziness=AUTO`, `prefix_length=1`, **guarded** so queries < 3 chars are not
   fuzzed. (`benhc`→`bench`, `programm`→`program`.)
3. **Synonyms:** applied by the search analyzer (§5).
4. **Structured filters** (exact-match `filter` clauses, don't affect score): templates —
   `difficulty`, `splitType`, `daysPerWeek`, `muscleGroup`, `analysisCategory`, `minScore`
   (range on `templateStructureScore`); workouts — `status`, date range on `startedAt`,
   `muscleGroup`, `exercise`, `gym`, `equipment`, volume range, duration range.
5. **Popularity boost (marketplace only):** the text query is wrapped in a `function_score` that adds
   a `log1p` boost from `savesCount` and `usesCount` on top of relevance.
6. **Facets:** terms aggregations (difficulty/split/days/muscle/category for templates;
   status/gym/muscle for workouts) + a per-month `date_histogram` for workouts → `SearchAggregations`.
7. **Highlighting:** unified highlighter wraps matches in `<mark>…</mark>` on the main text fields.
8. **Empty-index tolerance:** the request sets `ignore_unavailable` + `allow_no_indices`, and a 404
   from OpenSearch is mapped to an **empty result set** (not 503), so "index not built yet" looks
   like "no results", not an outage.

**Security — two layers:**
- **Mandatory scope filter from the JWT (cannot be overridden by params):**
  `scope=marketplace` ⇒ `visibility=PUBLIC`; `scope=my` ⇒ `ownerUserId = caller`;
  `/workouts` ⇒ always `ownerUserId = caller`.
- **Defense-in-depth:** after OpenSearch returns a page of IDs, the service re-validates them against
  PostgreSQL with the same ownership/visibility rule and drops any stale hit (e.g. just
  unpublished/deleted). For marketplace it also merges the viewer's live vote/save state and the
  author name from PostgreSQL. (`items.length` may therefore be ≤ `totalHits`.)

**Coach-scoped workout search (the one delegated owner id).** A coach can full-text search a *client's*
workout history via `GET /api/coach/clients/{clientId}/search/workouts`. This is the only place the
workout search runs with an owner id other than the caller's, and it is safe because `CoachSearchService`
first passes the **`CoachAccess` gate** (an ACTIVE coach→client relationship, else a 404 — IDOR-safe, not
403) and only then reuses the very same `WorkoutSearchService` with the **client's** id. The mandatory
`ownerUserId = clientId` scope filter and the PostgreSQL defense-in-depth re-validation therefore apply
unchanged, scoped to the client. RBAC (`/api/coach/** ⇒ ROLE_COACH`) rejects non-coaches with 403 before
the gate even runs.

---

## 8. Resilience & rebuild

- **Feature flag:** `app.search.enabled` (default **false**) gates every OpenSearch bean. When off
  (or OpenSearch unreachable) `/api/search/**` returns **503 `SEARCH_UNAVAILABLE`** and the SPA falls
  back to the SQL list. A not-yet-built index returns empty results, not 503 (§7.8).
- **Rebuild:** `SearchRebuildService` builds a fresh timestamped index, bulk-indexes from PostgreSQL,
  and **atomically swaps the alias** (zero read downtime), then deletes the old index. If population
  fails, the new index is dropped and the previous one keeps serving.
- **Manual trigger:** `POST /api/admin/search/reindex` — **ROLE_ADMIN only** (`/api/admin/**` in
  `SecurityConfig`). Registration never creates an admin, so ordinary users get **403**.
- **Demo seeding:** the demo seeder runs the same rebuild **in-process** on boot, so the 50
  raw-SQL-seeded marketplace templates (which bypass the event-publishing services) get indexed
  without an admin account. Look for the log line:
  `Demo search index rebuilt: 51 templates, 16 sessions`.

---

## 9. HTTP API

```
GET  /api/search/templates?scope=my|marketplace&q=&difficulty=&splitType=&daysPerWeek=
        &muscleGroup=&analysisCategory=&minScore=&page=&size=
GET  /api/search/workouts?q=&status=&dateFrom=&dateTo=&muscleGroup=&exercise=&gym=
        &equipment=&minVolume=&maxVolume=&minDuration=&maxDuration=&page=&size=
GET  /api/coach/clients/{clientId}/search/workouts?<same workout params>  # ROLE_COACH + ACTIVE relationship
POST /api/admin/search/reindex          # ROLE_ADMIN only
```
Response envelope:
```json
{
  "items": [ /* TemplateSearchItemResponse | WorkoutSearchItemResponse, each with highlights */ ],
  "facets": [ { "field": "difficulty", "buckets": [ { "key": "BEGINNER", "count": 17 } ] } ],
  "page": 0, "size": 20, "totalHits": 123
}
```
Template items also carry `authorDisplayName`, `myVote`, `saved` (merged from PostgreSQL for the
marketplace scope) and `relevanceScore`. 401 if unauthenticated; 503 if search unavailable.

Example:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"demo@workout.app","password":"demo12345"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
curl -s "http://localhost:8080/api/search/templates?scope=marketplace&q=programm" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/search/workouts?q=bench" -H "Authorization: Bearer $TOKEN"
```

---

## 10. Configuration

`backend/src/main/resources/application.yml`:
```yaml
app:
  search:
    enabled: ${APP_SEARCH_ENABLED:false}   # default OFF (local mvn + the 108 PG tests stay OS-free)
    uri: ${APP_SEARCH_URI:http://localhost:9200}
    connect-timeout-ms: 2000
    socket-timeout-ms: 5000
    templates-index: templates_v1
    templates-alias: templates
    sessions-index: workout_sessions_v1
    sessions-alias: workout_sessions
```

`docker-compose.yml` runs a single-node **OpenSearch 2.13** (security disabled for local/demo) and
turns search on for the app:
```yaml
opensearch:
  image: opensearchproject/opensearch:2.13.0
  environment:
    - discovery.type=single-node
    - DISABLE_SECURITY_PLUGIN=true
    - DISABLE_INSTALL_DEMO_CONFIG=true
    - DISABLE_PERFORMANCE_ANALYZER_AGENT_CLI=true   # PA agent CLI crashes on aarch64; not needed
    - OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m
app:
  environment:
    APP_SEARCH_ENABLED: "true"
    APP_SEARCH_URI: http://opensearch:9200
```
OpenSearch Dashboards is intentionally omitted to keep the stack light.

Client: official `opensearch-java` 2.13.0 on the Apache HttpClient5 transport (not Spring Data
Elasticsearch); bean created only when `app.search.enabled=true` (`OpenSearchClientConfig`).

---

## 11. File map

Backend — `backend/src/main/java/com/thesis/workout/search/`:
```
infrastructure/SearchProperties.java          # @ConfigurationProperties app.search.*
infrastructure/OpenSearchClientConfig.java     # OpenSearchClient bean (conditional)
infrastructure/OpenSearchAdmin.java            # create index from JSON, alias swap, exists/delete
infrastructure/TemplateIndexer.java            # structural / stats / remove / bulk
infrastructure/WorkoutSessionIndexer.java
infrastructure/event/SearchIndexEventListener.java   # AFTER_COMMIT
infrastructure/read/TemplateSearchReadRepository.java   # active template ids
infrastructure/read/SessionSearchReadRepository.java    # session snapshot fields + indexable ids
application/document/TemplateDocument.java
application/document/WorkoutSessionDocument.java
application/TemplateDocumentAssembler.java     # builds doc + analyzer fields (read tx)
application/WorkoutSessionDocumentAssembler.java
application/TemplateSearchService.java         # scope my/marketplace query
application/WorkoutSearchService.java          # owner-scoped query
application/SearchRebuildService.java          # alias-swap rebuild
application/SearchAggregations.java            # aggregate -> facet DTO
application/event/{TemplateIndexEvent,WorkoutSessionIndexEvent}.java
application/exception/SearchUnavailableException.java   # 503
web/SearchController.java                       # GET /api/search/templates|workouts
web/AdminSearchController.java                  # POST /api/admin/search/reindex
web/dto/{SearchResultsResponse,SearchFacetResponse,TemplateSearchItemResponse,WorkoutSearchItemResponse}.java
```
Index mappings: `backend/src/main/resources/search/{templates_v1,workout_sessions_v1}.json`.

Wiring touched in existing services (to publish events): `template/application/TemplateService`,
`TemplateDayService`, `TemplateDayExerciseService`, `TemplateDayRoutineService`;
`marketplace/application/{TemplatePublishingService,TemplateCopyService,TemplateInteractionService}`;
`session/application/WorkoutSessionService`; `auth/.../SecurityConfig` (`/api/admin/**` ⇒ ADMIN);
`demo/DemoDataSeeder` (in-process rebuild); `template/domain/model/Template` (added `getCreatedAt()`).

Frontend — `frontend/src/features/search/`:
```
types.ts          # SearchResults/TemplateSearchItem/WorkoutSearchItem/params
api.ts            # searchTemplates(), searchWorkouts()
Highlight.tsx     # renders <mark> fragments
SearchFacets.tsx  # clickable facet chips (double as filters)
```
Integrated into `features/templates/TemplatesPage.tsx`, `features/marketplace/MarketplacePage.tsx`,
`features/history/HistoryPage.tsx`; styles in `src/index.css` (`.search-facets`, `mark`, search input).

---

## 12. Testing

- `backend/src/test/java/com/thesis/workout/AbstractSearchIntegrationTest.java` — boots a real
  OpenSearch 2.11 Testcontainer + the shared PostgreSQL container, sets `app.search.enabled=true`.
- `backend/src/test/java/com/thesis/workout/search/web/SearchFlowIntegrationTest.java` — 10 tests:
  full-text match + analyzer fields + highlighting, typo tolerance, synonyms, marketplace
  scope/private isolation, unpublish removal, filters + facets, owner-scoped workout search +
  cross-user privacy, structured filters, admin reindex 403, unauthenticated 401.
- `AbstractPostgresIntegrationTest` sets `app.search.enabled=false`, so the original 108 PostgreSQL
  tests run with no OpenSearch dependency. Full suite: **118 tests green** under `mvn verify`.

---

## 13. Operating / demo walkthrough

```bash
# fresh demo stack (data + index)
SPRING_PROFILES_ACTIVE=demo docker compose up --build
#   wait for: DemoDataSeeder : Demo search index rebuilt: 51 templates, 16 sessions
#   (the demo seedMarketplace step takes a few minutes)

# manual rebuild from PostgreSQL (needs an ADMIN account; demo seeder does it automatically)
# POST /api/admin/search/reindex

docker compose down       # add -v to wipe the DB and re-seed next boot
```
Demo data: `demo@workout.app` / `demo12345`; 50 marketplace templates across 12 authors; 16 finished
workouts (squat/bench/row/incline DB press) at "Home Garage Gym".

---

## 14. Known limitations / not yet built

- **Redis caching** is not implemented; the marketplace list is served from PostgreSQL.
- **Natural-language queries** are not parsed; search is full-text + structured filters/facets.
- The OpenSearch container in `docker-compose` runs **security disabled** (no TLS/auth) — local/demo
  only, never production.
- Indexing is best-effort after-commit (no outbox/retry); recover via the rebuild endpoint.
- The demo `seedMarketplace` step is slow (~3–4 min for 50 templates via JdbcTemplate); the final
  rebuild only runs after it, so marketplace search is empty until the "rebuilt" log line appears.

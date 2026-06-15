# SEARCH_AND_CACHING.md

# Search and Caching Strategy

## Principle

PostgreSQL is the source of truth.

Redis and OpenSearch are derived systems.

If Redis/OpenSearch fail, the app should still work using PostgreSQL, just slower or with reduced search quality.

---

# Template Search

## Phase 1: PostgreSQL Structured Search

Use `workout_templates` aggregate arrays:

```text
aggregated_muscle_groups
aggregated_official_exercise_ids
aggregated_exercise_names
```

Supported filters:

- visibility = PUBLIC
- split type
- days per week
- difficulty
- includes exercise
- excludes exercise
- includes muscle group
- text query over name/description/exercise names
- sort by popular/trending/newest

Use GIN indexes on aggregate arrays.

## Phase 2: OpenSearch Template Index

Index name:

```text
templates_v1
```

Document fields:

```json
{
  "templateId": "...",
  "ownerUserId": "...",
  "name": "...",
  "description": "...",
  "splitType": "UPPER_LOWER",
  "daysPerWeek": 4,
  "difficulty": "INTERMEDIATE",
  "visibility": "PUBLIC",
  "muscleGroups": ["CHEST", "BACK"],
  "exerciseNames": ["Bench Press", "Squat"],
  "officialExerciseIds": ["..."],
  "upvotes": 120,
  "downvotes": 10,
  "saves": 45,
  "uses": 80,
  "ratingScore": 0.91,
  "trendingScore": 42.5,
  "publishedAt": "..."
}
```

---

# Workout History Search

## OpenSearch Index

Index name:

```text
workout_sessions_v1
```

Document shape:

```json
{
  "sessionId": "...",
  "userId": "...",
  "templateName": "Upper Lower 4 Days",
  "templateDayName": "Upper 1",
  "gymName": "World Class",
  "startedAt": "...",
  "finishedAt": "...",
  "notes": "...",
  "exercises": [
    {
      "name": "Bench Press",
      "muscleGroups": ["CHEST", "TRICEPS"],
      "sets": [
        {
          "weight": 80,
          "reps": 8,
          "equipmentName": "Bench 1",
          "note": "felt strong"
        }
      ]
    }
  ]
}
```

Search examples:

```text
bench press last month
sessions at World Class with dumbbells
tooth pain note
squat PR
workouts with shoulder pain
```

---

# Redis Caching

Use Redis for marketplace list caches.

Suggested keys:

```text
marketplace:popular:global
marketplace:trending:7d
marketplace:newest
marketplace:popular:split:UPPER_LOWER
marketplace:popular:days:4
marketplace:search:{hashOfFilters}
```

TTL:

```text
popular/trending: 5-15 minutes
search result lists: 2-5 minutes
newest: 1-3 minutes
```

Do not cache private user data first. Start with marketplace public data.

---

# Cache Invalidation

Events that should invalidate marketplace caches:

- template published
- template unpublished
- template updated
- template voted
- template saved
- template used

Implementation path:

1. Write DB transaction.
2. Insert outbox event.
3. Background worker processes event.
4. Recompute stats/index/cache.

For Phase 1, invalidate directly in service if no worker yet.

---

# Ranking

Do not rank by raw upvotes only.

Use:

```text
rating_score = quality score from up/down ratio
trending_score = rating + recency + usage/saves
```

Simple initial ranking:

```text
popular = upvotes - downvotes + saves * 0.5 + uses * 0.25
```

Better later:

```text
Wilson lower bound for up/down votes
```

For thesis, explaining why raw votes are naive is a good CS/product point.

---

# As Built (Phase 11 — OpenSearch derived read model)

This section documents what is actually implemented, which refines the plan above. Redis caching
is **not** implemented yet; OpenSearch search **is**. The SQL browse/list paths remain the default
and the fallback, so the application is fully usable with OpenSearch disabled or down.

## Where things live

- Backend feature package: `com.thesis.workout.search` (`infrastructure`, `application`, `web`).
- Index mappings: `backend/src/main/resources/search/templates_v1.json` and
  `workout_sessions_v1.json` (settings + analysis + mappings).
- Client: official `opensearch-java` 2.13.0 on the Apache HttpClient5 transport (not Spring Data
  Elasticsearch). Bean is created only when `app.search.enabled=true`.

## Consistency model

PostgreSQL is authoritative. OpenSearch is an **eventually-consistent, rebuildable** projection:

1. A write commits in PostgreSQL inside the normal service transaction.
2. The service publishes a domain event (`TemplateIndexEvent` / `WorkoutSessionIndexEvent`).
3. A `@TransactionalEventListener(phase = AFTER_COMMIT)` listener indexes the committed state, so
   the index never reflects rolled-back work. This is intentionally **not** a transactional outbox:
   if OpenSearch is briefly unavailable the failure is logged and swallowed, and the index can be
   rebuilt from PostgreSQL at any time.
4. Writes use `refresh=wait_for`, so a change is searchable as soon as the call returns
   (read-after-write), which keeps the demo and the tests deterministic.

Two distinct template upsert paths encode the rule that analyzer-derived fields are refreshed only
on **structural** change:

- **Structural reindex** (create/update template, day/exercise/routine change, publish/unpublish,
  copy): rebuilds the whole document, including the analyzer fields
  (`templateStructureScore`, `analysisCategory`, `warningCodes`, `missingMajorMuscles`).
- **Stats-only update** (vote/save/use): a partial update of the popularity counters only — it
  never recomputes the structural analysis. Falls back to a full reindex if the document is absent.

Workout sessions are indexed when they reach a terminal status (FINISHED/CANCELLED) and are built
entirely from the session's **immutable snapshots** (exercise/gym/equipment/muscle names captured at
training time), so search reflects historical truth.

## Indexes, aliases, analyzers

- Versioned indexes `templates_v1` / `workout_sessions_v1` addressed through stable aliases
  `templates` / `workout_sessions`. A rebuild builds a fresh timestamped index and **atomically
  swaps the alias** (zero read downtime), then drops the old index.
- Custom analysis: `workout_text` (index-time: standard + lowercase + asciifolding) and
  `workout_text_search` (search-time: adds a `synonym_graph` filter). Gym/lifting synonyms are
  applied at **search time** (e.g. `pecs → chest`, `quads → quadriceps`, `ohp → overhead press`),
  so adding synonyms does not require a reindex.
- Text fields carry a `.keyword` sub-field for exact-match structured filters and facets.

## Query design

- **Templates**: boosted `multi_match` over `name^4`, `exerciseNames^3`, `muscleGroupsText^2`,
  `dayNames^1.5`, `description^1`, `routine*^0.5`, `best_fields`. Marketplace scope wraps the text
  query in a `function_score` that adds a `log1p` popularity boost on `savesCount`/`usesCount`.
- **Workouts**: boosted `multi_match` over `exerciseNameSnapshots^4`, `templateNameSnapshot^2.5`,
  `gymNameSnapshot^2`, `equipmentNameSnapshots^1.5`, `muscleGroupsText^1.5`, `notes^1`.
- **Typo tolerance**: `fuzziness=AUTO` with `prefix_length=1`, guarded so queries under 3 chars are
  not fuzzed (avoids noise). e.g. `benhc` matches `bench`.
- **Highlighting**: `<mark>` fragments on the main text fields, surfaced in the UI.
- **Facets**: terms aggregations (difficulty / split / days-per-week / muscle group / analysis
  category for templates; status / gym / muscle group for workouts) plus a `date_histogram`
  (per-month) for workouts. Facet chips double as structured filters.

## Security

- The owner/visibility predicate is a **mandatory filter clause derived from the JWT**, never from
  request params: `scope=marketplace` only matches `visibility=PUBLIC`, `scope=my` only matches the
  caller's `ownerUserId`, and workout search is always filtered to the caller's `ownerUserId`.
- **Defense in depth**: after OpenSearch returns a page of ids, the results are re-validated against
  PostgreSQL with the same rule before anything is returned, so a hit that became stale after
  indexing (e.g. unpublished/deleted) is dropped. For marketplace, the viewer's vote/save state and
  the author name are merged in from PostgreSQL.

## Endpoints

```text
GET  /api/search/templates?scope=my|marketplace&q=&difficulty=&splitType=&daysPerWeek=
        &muscleGroup=&analysisCategory=&minScore=&page=&size=
GET  /api/search/workouts?q=&status=&dateFrom=&dateTo=&muscleGroup=&exercise=&gym=
        &equipment=&minVolume=&maxVolume=&minDuration=&maxDuration=&page=&size=
POST /api/admin/search/reindex      # ROLE_ADMIN only
```

Response envelope: `{ items: [...], facets: [{field, buckets:[{key,count}]}], page, size, totalHits }`.

## Resilience & rebuild

- `app.search.enabled` (default **false**) gates all OpenSearch beans. When off, `/api/search/**`
  returns **503** and the SPA falls back to the SQL list. docker-compose turns it on.
- Manual rebuild: `POST /api/admin/search/reindex` is locked to `ROLE_ADMIN`. Registration never
  creates an admin, so ordinary users get 403; the **demo seeder** runs the same rebuild in-process
  on boot so the index (including the 50 raw-SQL-seeded marketplace templates) is populated without
  an admin account.

## Running OpenSearch

`docker compose up` starts a single-node OpenSearch 2.13 (security disabled, 512m heap). OpenSearch
Dashboards is intentionally omitted to keep the stack light; to add a query console, run
`docker run --rm -p 5601:5601 -e OPENSEARCH_HOSTS='["http://host.docker.internal:9200"]'
opensearchproject/opensearch-dashboards:2.13.0`.

## Not yet implemented

- Redis caches (the marketplace list is served from PostgreSQL).
- Natural-language query parsing (queries are full-text + structured filters, not parsed sentences).

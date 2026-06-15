# API_CONTRACT.md

# REST API Contract

Base URL:

```text
/api
```

All protected endpoints require JWT access token.

---

# Auth

```http
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/logout
GET  /auth/me
```

---

# Exercises

```http
GET    /exercises
GET    /exercises/official
GET    /exercises/custom
POST   /exercises/custom
PUT    /exercises/custom/{exerciseId}
DELETE /exercises/custom/{exerciseId}
GET    /muscle-groups
```

Rules:

- official exercises readable by all authenticated users
- custom exercises owned by current user
- admin manages official exercises later

---

# Routines

```http
GET    /routines
POST   /routines
PUT    /routines/{routineId}
DELETE /routines/{routineId}
```

---

# Gyms / Equipment

```http
GET    /gyms
POST   /gyms
GET    /gyms/{gymId}
PUT    /gyms/{gymId}
DELETE /gyms/{gymId}

GET    /gyms/{gymId}/equipment
POST   /gyms/{gymId}/equipment
PUT    /equipment/{equipmentId}
DELETE /equipment/{equipmentId}
```

Rules:

- gyms and equipment are owned by the current user; cross-user access returns 404
- `GET /gyms/{gymId}` is owner-scoped (used for deep-link reload of a gym's equipment page)
- deleting a gym soft-deletes the gym and its active equipment
- equipment name is unique per gym (case-insensitive, active only); gym name is unique per user

---

# Templates

```http
GET    /templates
POST   /templates
GET    /templates/{templateId}
PUT    /templates/{templateId}
DELETE /templates/{templateId}

POST   /templates/{templateId}/days
PUT    /template-days/{templateDayId}
DELETE /template-days/{templateDayId}

POST   /template-days/{templateDayId}/exercises
PUT    /template-day-exercises/{templateDayExerciseId}
DELETE /template-day-exercises/{templateDayExerciseId}

POST   /template-days/{templateDayId}/routines
DELETE /template-day-routines/{templateDayRoutineId}
```

---

# Workout Sessions

```http
POST   /workouts/start
GET    /workouts/active
GET    /workouts/{sessionId}
POST   /workouts/{sessionId}/finish
POST   /workouts/{sessionId}/cancel

POST   /session-exercises/{sessionExerciseId}/sets
PUT    /sets/{setId}
DELETE /sets/{setId}

POST   /workouts/{sessionId}/extra-exercises
```

Start workout request:

```json
{
  "templateDayId": "uuid",
  "gymId": "uuid"
}
```

The backend must create snapshots transactionally.

---

# History + Analytics

```http
GET /history                 # paged list of the caller's sessions (all statuses), newest first
GET /workouts/{sessionId}     # session detail/summary (reused from Phase 5; no separate /history/{id})
GET /analytics/overview       # bundled analytics over FINISHED sessions
```

`GET /history` params: `page` (default 0), `size` (default 20, max 100). Returns
`{ items, page, size, totalItems, hasNext }`; each item carries summary numbers
(`exerciseCount`, `setCount`, `totalVolume`, `durationSeconds` — null while IN_PROGRESS).

`GET /analytics/overview` returns `{ totalWorkouts, totalVolume, volumeOverTime[],
workoutsPerWeek[], primaryMuscleSetDistribution[], bestSets[], oneRepMaxOverTime[] }`. Best sets
are ranked by estimated 1RM (Epley); `oneRepMaxOverTime` is a per-exercise time series of each
day's best estimated 1RM (strength progression). Analytics count FINISHED sessions only.

## Search — Phase 11 (OpenSearch) — IMPLEMENTED

OpenSearch-backed search over a derived, eventually-consistent read model. PostgreSQL stays
authoritative; when `app.search.enabled=false` (default) or OpenSearch is down, these return
**503 `SEARCH_UNAVAILABLE`** and the client falls back to the SQL browse/list. See
`docs/SEARCH_AND_CACHING.md` (As Built) for the design.

```http
GET /api/search/templates?scope=my|marketplace&q=&difficulty=&splitType=&daysPerWeek=
        &muscleGroup=&analysisCategory=&minScore=&page=&size=
GET /api/search/workouts?q=&status=&dateFrom=&dateTo=&muscleGroup=&exercise=&gym=
        &equipment=&minVolume=&maxVolume=&minDuration=&maxDuration=&page=&size=
POST /api/admin/search/reindex      # ROLE_ADMIN only (ordinary users -> 403)
```

- `scope` security is enforced from the JWT (cannot be overridden): `marketplace` ⇒ PUBLIC only,
  `my` ⇒ caller's own; `/workouts` is always the caller's own history.
- Response envelope:
  `{ items: [...], facets: [{ field, buckets: [{ key, count }] }], page, size, totalHits }`.
  Each item carries per-field `highlights` (`<mark>`); marketplace template items also carry
  `authorDisplayName`, `myVote`, `saved`. Returned items are re-validated against PostgreSQL
  (defense in depth), so `items.length` may be ≤ `totalHits`.
- Behaviour: boosted multi-field full-text, `fuzziness=AUTO` typo tolerance (guarded for short
  queries), search-time synonyms, structured filters, terms + date-histogram facets, marketplace
  popularity boost via `function_score`.

The earlier `GET /history/search` placeholder is superseded by `GET /api/search/workouts`.

---

# Marketplace

```http
GET  /marketplace/templates
GET  /marketplace/templates/{templateId}

POST /templates/{templateId}/publish
POST /templates/{templateId}/unpublish

POST /marketplace/templates/{templateId}/vote
DELETE /marketplace/templates/{templateId}/vote

POST /marketplace/templates/{templateId}/save
DELETE /marketplace/templates/{templateId}/save

POST /marketplace/templates/{templateId}/use
```

Vote request:

```json
{
  "voteType": "UP"
}
```

Phase 7 behavior (PostgreSQL only; no Redis/OpenSearch yet):

- All marketplace endpoints require authentication (401 for anonymous).
- Browse (`GET /marketplace/templates`) lists only PUBLIC, non-deleted templates; params: `sort=newest|top|trending`, `splitType`, `difficulty`, `daysPerWeek`, `savedOnly`, `page`, `size`. Private/foreign templates are never exposed (404 on detail).
- `publish` requires the owner and at least one day with at least one exercise (else 400 `TEMPLATE_NOT_PUBLISHABLE`).
- Vote: same vote toggles off, opposite switches, `DELETE` clears; self-vote → 400 `CANNOT_VOTE_OWN_TEMPLATE`. `rating_score = upvotes − downvotes`; `trending_score` is a write-time approximation (not a real-time decaying rank).
- `use` deep-copies the public template into a new PRIVATE template owned by the copier, linked only via `copiedFromTemplateId`. Exercise references are kept only for OFFICIAL active exercises; publisher-custom/foreign/deleted refs and all routine refs are set NULL, with name/type/planned/muscle-group and routine snapshots preserved — so the copy is fully usable from snapshots and independent of the source. Author is shown as `display_name` (fallback "Community user"); emails are never exposed.

or:

```json
{
  "voteType": "DOWN"
}
```

---

# Analyzer

```http
GET /templates/{templateId}/analysis
```

Phase 9 (PostgreSQL only, deterministic, rule-based, no ML, no persistence):

- Computed live on a read-only GET. Access: owner (private OK) or PUBLIC template → 200; private non-owned → 404; unauthenticated → 401.
- Returns `{ templateId, overallScore (0–100, "Template Structure Score"), category (WELL_STRUCTURED|DECENT_STRUCTURE|NEEDS_REVIEW), summary, subScores{volumeCoverage,frequency,balance,sessionDesign,specificityRest}, muscleGroupVolumes[{muscleGroup,weeklyWeightedSets,volumeDataIncomplete}], frequencyByMuscleGroup[], balanceRatios{pullToPush,posteriorToQuads,lowerToUpper}, warnings[{code,severity,title,explanation,affectedMuscleGroups,suggestedFix}], suggestions[], strengths[], limitations[], disclaimer }`.
- Severity ∈ CRITICAL|HIGH|MEDIUM|LOW|INFO. Templates store no RIR/RPE, so proximity-to-failure is always an INFO limitation. Exercises with no planned set count are excluded from volume (flagged `VOLUME_DATA_INCOMPLETE`). See `docs/ROUTINE_ANALYZER.md` for the full rule set and deduction table. (The older `POST /analyze` + `GET /analysis/latest` sketch is dropped in favor of this single GET.)

---

# Coach

Admin:

```http
POST /admin/coaches
GET  /admin/coaches
POST /admin/coaches/{coachUserId}/deactivate
```

Coach/client:

```http
POST /coaching/invites
GET  /coaching/invites
POST /coaching/invites/{relationshipId}/accept
POST /coaching/invites/{relationshipId}/reject
POST /coaching/relationships/{relationshipId}/revoke
```

Coach dashboard:

```http
GET  /coach/clients
GET  /coach/clients/{clientUserId}/history
GET  /coach/clients/{clientUserId}/analytics
POST /coach/clients/{clientUserId}/sessions/{sessionId}/comments
POST /coach/clients/{clientUserId}/assignments
```

---

# Error Format

Use consistent API errors:

```json
{
  "timestamp": "2026-06-14T12:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Name is required",
  "path": "/api/templates"
}
```

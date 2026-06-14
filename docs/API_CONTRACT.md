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

## Deferred to Phase 11 (OpenSearch)

```http
GET /history/search
```

Filters: `dateFrom, dateTo, exercise, gym, equipment, template, textQuery`.
Full-text / filtered history search is implemented later with OpenSearch, not in Phase 6.

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

or:

```json
{
  "voteType": "DOWN"
}
```

---

# Analyzer

```http
POST /templates/{templateId}/analyze
GET  /templates/{templateId}/analysis/latest
```

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

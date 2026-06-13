# TASKS.md

# Implementation Roadmap

## Rule

Do not build everything at once.

Build one vertical slice at a time.

---

# Phase 0 — Project Control

Deliverables:

- `CLAUDE.md`
- `AGENTS.md`
- docs
- Git repo
- basic README

Status target:

```text
Claude Code has clear instructions and cannot freely invent architecture.
```

---

# Phase 1 — Infrastructure + Database

Deliverables:

- Spring Boot backend
- React TypeScript frontend
- Docker Compose
- PostgreSQL 16
- Flyway
- `V1__full_initial_schema.sql`
- health endpoint
- frontend served by Spring Boot

Acceptance:

```text
docker compose up works
backend starts
Flyway applies migration
GET /api/health works
frontend opens at localhost:8080
```

---

# Phase 2 — Auth

Deliverables:

- register
- login
- refresh token
- logout
- current user endpoint
- JWT security config
- frontend auth screen

Acceptance:

```text
user can register/login
protected endpoints reject unauthenticated requests
```

---

# Phase 3 — Planning Data

Deliverables:

- exercises
- muscle groups
- custom exercises
- routines
- gyms
- equipment

Acceptance:

```text
user can create and manage planning data
ownership security works
```

---

# Phase 4 — Templates

Deliverables:

- workout templates
- template days
- template exercises
- template routines
- aggregate search arrays
- template builder UI

Acceptance:

```text
user can build a multi-day workout program
```

---

# Phase 5 — Live Workout Execution

Deliverables:

- start workout from template day
- create session snapshots
- show start routines
- live workout screen
- log sets
- add extra exercises
- select/quick-add equipment
- finish workout
- summary screen

Acceptance:

```text
user can complete a real workout end-to-end
old workout history remains correct after editing source template/exercises
```

This is the minimum serious thesis demo.

---

# Phase 6 — History + Basic Analytics

Deliverables:

- workout history
- session detail page
- total volume over time
- best set per exercise
- workouts per week
- muscle group distribution

Acceptance:

```text
user can inspect progress from real logged workouts
```

---

# Phase 7 — Marketplace

Deliverables:

- publish/unpublish
- public template browsing
- upvote/downvote
- save
- use/copy template
- template stats

Acceptance:

```text
user can discover, vote, save, and copy public templates
copy is private and independent
```

---

# Phase 8 — Redis Caching

Deliverables:

- cache popular templates
- cache trending templates
- cache search result lists
- TTL
- invalidation after vote/publish/use

Acceptance:

```text
cache hit/miss visible in logs
latency comparison possible
```

---

# Phase 9 — Template Structure Analyzer

Deliverables:

- analyzer service
- explainable score
- positives/warnings/notes
- analyzer UI

Acceptance:

```text
user can analyze a template and see evidence-informed structural feedback
```

---

# Phase 10 — Coach Mode

Deliverables:

- admin creates coach
- coach invites client
- client accepts/revokes
- coach dashboard
- client history/statistics read access
- coach comments
- coach assigned templates

Acceptance:

```text
coach can see only active clients, not all users
```

---

# Phase 11 — OpenSearch

Deliverables:

- index workout sessions
- index public templates
- history search
- template search
- outbox-based indexing

Acceptance:

```text
user can search old workouts by notes/exercises/gyms/equipment/text
```

---

# If Time Collapses

Minimum serious final thesis:

```text
Phases 1-6
```

Strong final thesis:

```text
Phases 1-9
```

Very impressive final thesis:

```text
Phases 1-11
```

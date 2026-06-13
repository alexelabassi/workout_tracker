# CODEX_PROMPTS.md

# Reusable Implementation Prompts

## Prompt 1 — Initial Foundation

```text
Task:
Create the initial project foundation.

Context:
This is a Spring Boot + React TypeScript thesis-grade workout platform. Read CLAUDE.md, AGENTS.md, docs/PROJECT_SPEC.md, docs/ARCHITECTURE.md, docs/DATABASE_SCHEMA.md, and backend/src/main/resources/db/migration/V1__full_initial_schema.sql before editing.

Requirements:
1. Create backend Spring Boot 3.x structure with Java 21.
2. Configure PostgreSQL and Flyway.
3. Add Docker Compose with PostgreSQL 16 and pgAdmin.
4. Ensure Flyway runs V1__full_initial_schema.sql successfully.
5. Create GET /api/health.
6. Create React + TypeScript frontend.
7. Serve frontend through Spring Boot at http://localhost:8080/.
8. Do not implement auth yet.
9. Do not change schema unless migration fails due to a real SQL bug.
10. Run backend build/tests and frontend build.

Output:
- changed files
- commands run
- whether Flyway migration succeeded
- remaining issues
```

## Prompt 2 — Auth

```text
Task:
Implement JWT authentication only.

Requirements:
- register
- login
- refresh
- logout
- current user endpoint
- BCrypt password hashing
- refresh tokens stored as hashes
- Spring Security stateless config
- frontend auth screen
- protected route support
- tests for auth service/controller

Do not implement exercises/templates yet.
```

## Prompt 3 — Exercises

```text
Task:
Implement exercises and muscle groups.

Requirements:
- list official + current user's custom exercises
- create custom exercise
- update custom exercise
- soft-delete custom exercise
- muscle group mapping
- ownership checks
- admin-only placeholder/service method for official exercises, but no admin UI yet
- frontend exercises page with loading/empty/error/toast states
```

## Prompt 4 — Routines

```text
Task:
Implement reusable START/END routines.

Requirements:
- CRUD routines
- routine_type START/END
- ownership checks
- frontend routines page
- clean forms and validation
```

## Prompt 5 — Gyms and Equipment

```text
Task:
Implement gyms and equipment.

Requirements:
- CRUD gyms
- CRUD equipment inside gyms
- quick-add equipment endpoint usable during workout later
- ownership checks
- frontend gyms/equipment page
```

## Prompt 6 — Templates

```text
Task:
Implement workout templates, template days, template exercises, and template routines.

Requirements:
- template CRUD
- template day CRUD
- add/remove/reorder exercises
- snapshot exercise name/type and muscle groups at template level
- attach START/END routines to template days with routine snapshots
- recompute aggregated_muscle_groups, aggregated_official_exercise_ids, aggregated_exercise_names
- frontend template builder
```

## Prompt 7 — Start Workout

```text
Task:
Implement starting a workout from a template_day.

Requirements:
- POST /api/workouts/start
- validates current user owns template or template is public/copyable
- validates gym ownership
- creates workout_sessions row
- copies template day exercises into session_exercises
- copies template_day_exercise_muscle_groups into session_exercise_muscle_groups
- copies template_day_routines into session_routines
- transactionally creates all snapshots
- tests prove history remains stable after source edits
```

## Prompt 8 — Live Workout

```text
Task:
Implement live workout set logging.

Requirements:
- active workout endpoint
- add/update/delete sets
- add extra live exercise
- select equipment
- quick-add equipment
- frontend live workout page
- finish workout
- summary page
```

## Prompt 9 — Marketplace

```text
Task:
Implement free template marketplace.

Requirements:
- publish/unpublish template
- browse public templates
- filters by split/days/difficulty/muscles/exercises
- upvote/downvote
- save/unsave
- use/copy template into private account
- template_stats updates
- frontend marketplace page
```

## Prompt 10 — Analyzer

```text
Task:
Implement evidence-informed template structure analyzer.

Requirements:
- rule-based only
- no fake AI
- score 0-100
- category NEEDS_REVIEW/DECENT_STRUCTURE/WELL_STRUCTURED
- positives/warnings/notes
- analyzes volume, frequency, push-pull balance, upper-lower balance, missing major muscles, missing routines
- frontend analyzer panel on template detail
```

## Prompt 11 — Coach Mode

```text
Task:
Implement coach mode.

Requirements:
- admin creates coach profile
- coach invites client
- client accepts/rejects/revokes
- coach dashboard
- coach can view only active clients
- coach can view client history/analytics
- coach can comment on sessions
- coach can assign templates
- strict authorization tests
```

## Prompt 12 — OpenSearch

```text
Task:
Implement OpenSearch read model for workout history search.

Requirements:
- index finished workout sessions
- search by text/exercise/gym/equipment/notes/date
- PostgreSQL remains source of truth
- use outbox_events for reliable indexing if feasible
- frontend history search page
```

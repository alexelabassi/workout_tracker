# CLAUDE.md

You are implementing a serious bachelor's thesis project: an evidence-informed workout tracking, template discovery, coaching, and training-history search platform.

## Absolute Rules

1. Do not invent architecture. Follow `docs/PROJECT_SPEC.md`, `docs/ARCHITECTURE.md`, and `docs/DATABASE_SCHEMA.md`.
2. Do not change the database schema unless the current task explicitly asks for a schema migration.
3. Do not use H2, HTTP Basic, in-memory fake repositories, mock persistence, or temporary MVP shortcuts.
4. Use PostgreSQL, Flyway, Docker Compose, JWT auth, ownership checks, and final-quality foundations from the beginning.
5. Do not implement vague fake AI coaching. The analyzer is rule-based, evidence-informed, explainable, and conservative.
6. All user-owned resources must enforce ownership/security checks. Avoid IDOR vulnerabilities.
7. Coach access must be limited to active coach-client relationships.
8. Public marketplace templates are readable by everyone, but copying creates a private user-owned copy.
9. Workout sessions must preserve historical truth through snapshots. Never make old workout history depend on live templates/exercises/gyms/equipment.
10. Do not output truncated files or comments like `// rest of code`.
11. After every implementation task, run relevant tests/builds and report commands run.
12. Keep changes scoped. Do not touch unrelated files.

## Preferred Stack

- Backend: Spring Boot 3.x, Java 21
- Frontend: React + TypeScript
- DB: PostgreSQL 16
- Migrations: Flyway
- Auth: JWT access + refresh tokens
- Docker: Docker Compose
- Cache later: Redis
- Search later: OpenSearch
- Frontend served by Spring Boot at `http://localhost:8080/`

## Implementation Style

Use package-by-feature, not a giant technical-layer dump.

Suggested backend structure:

```text
com.thesis.workout
  auth/
    application/
    domain/
    infrastructure/
    web/
  exercise/
  routine/
  gym/
  template/
  session/
  marketplace/
  analytics/
  search/
  coaching/
  shared/
```

## Workflow

For each feature:

1. Read relevant docs.
2. Propose a short plan.
3. Implement only the requested feature.
4. Add tests.
5. Run tests/build.
6. Report changed files and remaining issues.

## Quality Bar

This should look like a strong portfolio/backend systems project, not a CRUD school app.

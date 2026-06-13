# ARCHITECTURE.md

# System Architecture

## High-Level Architecture

```text
React + TypeScript Frontend
        |
        v
Spring Boot REST API
        |
        +--> PostgreSQL source of truth
        +--> Redis cache for marketplace/search result lists
        +--> OpenSearch read model for workout history/template search
        +--> Outbox table for async sync/cache invalidation
```

## Backend Architecture

Use package-by-feature with clean internal layering:

```text
feature/
  domain/
    model/
    service/
  application/
    dto/
    command/
    query/
    usecase/
  infrastructure/
    repository/
    mapper/
  web/
    controller/
```

Example:

```text
template/
  domain/
  application/
  infrastructure/
  web/
```

Do not create one massive `service` package and one massive `controller` package.

---

## Core Modules

### Auth Module

Responsibilities:

- registration
- login
- refresh tokens
- JWT generation/validation
- current user extraction
- password hashing

### Exercise Module

Responsibilities:

- official exercises
- custom exercises
- muscle groups
- custom exercise ownership

### Routine Module

Responsibilities:

- start/end routines
- reusable content
- template routine snapshots

### Gym Module

Responsibilities:

- user gyms
- gym equipment
- equipment quick-add during workout

### Template Module

Responsibilities:

- workout templates/programs
- template days
- template day exercises
- template day routines
- aggregate arrays for search
- template copying

### Session Module

Responsibilities:

- start workout
- create session snapshots
- live set logging
- add extra exercise
- finish workout
- history

### Marketplace Module

Responsibilities:

- publish/unpublish templates
- browse public templates
- vote/save/use templates
- ranking
- stats updates

### Analyzer Module

Responsibilities:

- rule-based template structure analysis
- explainable scoring
- no fake AI

### Coaching Module

Responsibilities:

- admin creates coaches
- coach-client relationships
- coach can view active clients
- coach comments
- coach template assignments

### Search Module

Responsibilities:

- PostgreSQL structured search
- OpenSearch indexing
- workout history search
- template search

### Cache Module

Responsibilities:

- Redis cached marketplace lists
- TTL strategy
- invalidation after votes/saves/publishes

---

## Data Source Rules

PostgreSQL is always source of truth.

Redis and OpenSearch are derived/read models.

If Redis/OpenSearch are empty or stale:

```text
PostgreSQL still works.
```

---

## Outbox Pattern

Use the `outbox_events` table for future async reliability.

Events:

- TEMPLATE_PUBLISHED
- TEMPLATE_UPDATED
- TEMPLATE_VOTED
- TEMPLATE_USED
- WORKOUT_FINISHED
- SESSION_UPDATED
- COACH_COMMENT_CREATED

Consumers later:

- update Redis cache
- update OpenSearch index
- recompute template stats
- recompute analytics snapshots

For Phase 1, outbox can exist unused. Later it gives a strong thesis systems angle.

---

## Frontend Architecture

Suggested structure:

```text
src/
  app/
  shared/
    api/
    auth/
    components/
    hooks/
    types/
  features/
    auth/
    dashboard/
    exercises/
    routines/
    gyms/
    templates/
    workouts/
    history/
    marketplace/
    analyzer/
    coaching/
```

UI style:

- dark mode first
- light mode toggle
- responsive dashboard
- cards
- tables where needed
- skeleton/loading states
- empty states
- error states
- toast notifications

---

## Non-Functional Requirements

### Security

- JWT access token
- refresh token stored securely
- BCrypt password hashing
- ownership checks
- coach-client access checks
- admin-only endpoints

### Performance

- indexes on ownership/history/filter fields
- Redis cache for marketplace top lists
- OpenSearch for history/template text search
- pagination everywhere

### Reliability

- Flyway migrations
- database constraints
- transactional snapshot creation
- outbox for eventually consistent async work

### Maintainability

- feature modules
- DTO boundaries
- no entity leakage if avoidable
- service-level authorization
- tests for critical flows

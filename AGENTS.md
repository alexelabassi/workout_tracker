# AGENTS.md

## Project Mission

Build a thesis-grade gym training platform with:

- secure user accounts
- official/custom exercises
- reusable routines
- gyms and equipment
- multi-day workout templates/programs
- live workout tracking
- immutable session snapshots
- workout history
- free template marketplace
- advanced template search
- Redis cached marketplace rankings
- rule-based evidence-informed routine/template analyzer
- coach/client mode
- OpenSearch-powered workout history search

## Non-Negotiables

- PostgreSQL only.
- Flyway migrations only.
- JWT auth from the start.
- No H2.
- No HTTP Basic.
- No fake repositories.
- No temporary architecture.
- No fake AI coach.
- No unscoped admin access.
- No direct set -> template relationship.
- Snapshot history must remain correct after edits/deletes.

## Data Integrity Rules

Planning data is editable. Session data is historical.

Correct:

```text
workout_sets -> session_exercises -> workout_sessions
```

Wrong:

```text
workout_sets -> template_day_exercises
```

## Security Rules

Every endpoint must enforce one of:

- current user owns the resource
- resource is public
- current user is admin
- current user is active coach of the resource owner

Never trust IDs from the frontend.

## Frontend Rules

- React + TypeScript
- modern SaaS/dashboard UI
- dark mode first
- light mode toggle
- persisted theme
- cards, forms, loading states, empty states, error states, toasts
- no ugly default HTML CRUD

## Output Rules For Coding Agents

- No truncated code.
- No placeholders.
- No unrelated rewrites.
- Run builds/tests.
- Explain files changed.

# THESIS_ANGLE.md

# Thesis Technical Angle

## Recommended Thesis Framing

This project should not be framed as "just a workout tracker."

Frame it as:

> A secure training platform that combines immutable workout-history snapshots, multi-day template modeling, community template discovery, evidence-informed structure analysis, coach-client access control, caching, and search indexing.

---

## Main CS Concepts

### 1. Temporal Data Modeling

Problem:

```text
Workout history must remain correct after future edits.
```

Solution:

```text
Snapshot templates, exercises, routines, gyms, equipment, and muscle groups into session tables.
```

### 2. Clean Architecture

Package-by-feature with clear boundaries.

### 3. Security

- JWT
- refresh tokens
- RBAC
- ownership checks
- coach-client relationship authorization
- public/private marketplace access

### 4. Search

- PostgreSQL structured search
- aggregate arrays
- OpenSearch read model
- history search over nested workout data

### 5. Caching

- Redis marketplace caches
- TTLs
- invalidation
- cached vs uncached latency comparison

### 6. Ranking

- upvotes/downvotes
- saves/uses
- rating score
- trending score
- avoid raw upvote ranking

### 7. Rule-Based Analyzer

- deterministic
- explainable
- evidence-informed
- conservative output

### 8. Outbox Pattern

Reliable async event handling for:

- indexing
- cache invalidation
- stats recomputation

### 9. Coach Mode

Relationship-based access control and scoped data sharing.

---

## Evaluation Ideas

You can evaluate:

1. Cache performance:
   - cached vs uncached marketplace queries

2. Search performance:
   - PostgreSQL search vs OpenSearch query behavior

3. Snapshot correctness:
   - edit/delete template after session
   - old history remains unchanged

4. Security correctness:
   - user cannot access another user's workout
   - coach can access only active clients

5. Analyzer explainability:
   - same template produces deterministic warnings
   - scoring breakdown is transparent

---

## What To Say In Defense

Strong sentence:

> The core technical challenge is not storing workouts, but preserving the historical truth of performed workouts while allowing all planning data to remain editable.

Another strong sentence:

> Redis and OpenSearch are used as derived read-optimization layers, while PostgreSQL remains the source of truth.

Another:

> The analyzer is intentionally rule-based and explainable, avoiding unverifiable AI-generated fitness advice.

Another:

> Coach mode solves the subjectivity problem by allowing a real human coach to interpret user data rather than pretending the system can prescribe universally optimal training.

# PROJECT_SPEC.md

# Thesis Project: Evidence-Informed Workout Template Discovery and Coaching Platform

## One-Line Description

A secure web platform for planning workouts, executing live gym sessions, preserving historically correct workout data, discovering community templates, analyzing routine structure, searching training history, and enabling coach-client monitoring.

## Strong Thesis Title Options

1. **Evidence-Informed Workout Template Discovery and Training History Platform**
2. **A Secure Workout Tracking Platform with Immutable Session Snapshots, Template Search, and Coach Analytics**
3. **Design and Implementation of a Training Platform with Temporal Workout History, Search, Caching, and Rule-Based Routine Analysis**

Recommended title:

> **Evidence-Informed Workout Template Discovery and Training History Platform**

It sounds serious, technical, and not like a fake AI fitness coach.

---

## Product Goals

The app should help users:

- create exercises, routines, gyms, equipment
- create reusable multi-day workout templates
- start live workouts from templates
- log sets, reps, weight, notes, equipment
- finish workouts and view accurate history
- browse free public templates
- vote/save/use public templates
- search templates with structured filters and text
- analyze routine/template structure
- search personal workout history
- optionally connect with a coach who can inspect progress

---

## What Makes This Thesis-Grade

This is not just CRUD. The technical pillars are:

1. **Temporal data integrity**
   - sessions snapshot templates/exercises/routines/gyms/equipment
   - old history remains correct after edits/deletes

2. **Multi-day template modeling**
   - templates are programs
   - template days are executable workouts

3. **Marketplace discovery**
   - public/private templates
   - voting
   - saving
   - use/copy semantics
   - ranking

4. **Search**
   - structured SQL search first
   - OpenSearch read model later for history/template search

5. **Caching**
   - Redis caching for popular/trending template lists
   - TTL and invalidation strategy

6. **Rule-based analyzer**
   - conservative evidence-informed scoring
   - explainable warnings/positives
   - no fake medical claims

7. **Coach mode**
   - admin-created coaches
   - active coach-client relationships
   - coach read access to clients' workouts/statistics
   - optional coach comments and assigned templates

8. **Security**
   - JWT
   - RBAC + ownership checks
   - coach-client authorization
   - public/private access control

9. **Clean architecture**
   - package-by-feature
   - domain/application/infrastructure/web separation

10. **Event-driven readiness**
    - outbox table for search indexing/cache invalidation/stat updates

---

## Main User Roles

### Normal User

Can:

- manage their own exercises/routines/gyms/equipment
- create templates
- start and finish workouts
- view own history/statistics
- publish templates
- browse/use marketplace templates
- accept/revoke coach access

### Coach

Can:

- view assigned clients
- view client workout history/statistics
- comment on client sessions
- assign templates to clients
- cannot edit client history directly

### Admin

Can:

- create/activate/deactivate coach accounts
- manage official exercises
- moderate public templates if needed
- view system-level admin pages

---

## Main App Flow

1. User registers/logs in.
2. User sees official exercises and creates custom exercises.
3. User creates reusable start/end routines.
4. User creates gyms and equipment.
5. User creates a workout template/program.
6. User adds template days.
7. User adds exercises/routines to each template day.
8. User starts a workout from a template day and gym.
9. App creates workout session snapshots.
10. User performs start routine.
11. User logs live sets.
12. User optionally selects equipment or quick-adds equipment.
13. User can add extra live exercises.
14. User finishes workout.
15. User sees summary.
16. Workout appears in history.
17. User can search/filter history later.

---

## Marketplace Flow

1. User publishes a private template.
2. Template appears in marketplace.
3. Other users browse/search/filter.
4. Users can upvote/downvote/save.
5. Users click "Use Template."
6. App creates a private copy of the public template.
7. Copy is independent from original.

Marketplace is free. No payments.

---

## Search Direction

### Template Search

Users should search templates by:

- text query
- split type
- days per week
- difficulty
- included exercises
- excluded exercises
- muscle groups
- equipment requirements
- popularity/trending score

Example:

> "I want upper lower, 4 days, includes bench press, no lat pulldown."

Initial implementation:

- UI filters/chips
- PostgreSQL arrays and joins

Later implementation:

- OpenSearch index for better text/fuzzy search
- optional natural language parsing into filters

### Workout History Search

Users should search old workouts by:

- exercise
- gym
- equipment
- notes
- date range
- weight/reps
- PRs
- template used

OpenSearch should be a read model, not the source of truth.

---

## Analyzer Direction

Do not call it:

- "scientifically perfect"
- "scientifically good"
- "bad routine"
- "AI coach"

Use:

> **Template Structure Analyzer**

or

> **Evidence-Informed Routine Analyzer**

Output categories:

- NEEDS_REVIEW
- DECENT_STRUCTURE
- WELL_STRUCTURED

It should explain:

- positives
- warnings
- neutral notes

It should not prescribe medical advice.

---

## Scope Cuts

Avoid:

- paid marketplace
- social media feed
- chat between users
- blockchain
- gamification fluff
- fake AI-generated workout plans
- pretending the app knows the user's recovery/medical status
- complex mobile app
- computer vision feature for this thesis unless everything else is done

The impressive direction is systems/data/search/security, not gimmicks.

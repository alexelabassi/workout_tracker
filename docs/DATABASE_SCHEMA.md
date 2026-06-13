# DATABASE_SCHEMA.md

# Final Database Schema

This is the final practical schema for the thesis platform.

The source-of-truth SQL migration is:

```text
backend/src/main/resources/db/migration/V1__full_initial_schema.sql
```

## Global Rules

- UUID primary keys.
- `timestamptz` everywhere.
- PostgreSQL 16.
- Flyway migrations.
- `varchar + check constraints`, not native Postgres enums.
- Soft delete planning data.
- Snapshot session data.
- Redis/OpenSearch are derived systems, not source of truth.
- Coach access is modeled explicitly.

---

## Table Groups

### Auth / Users

- `app_users`
- `refresh_tokens`

### Coach Mode

- `coach_profiles`
- `coach_client_relationships`
- `coach_session_comments`
- `coach_template_assignments`

### Exercises

- `exercises`
- `muscle_groups`
- `exercise_muscle_groups`

### Routines

- `routines`

### Gyms

- `gyms`
- `equipment`

### Templates

- `workout_templates`
- `template_stats`
- `template_days`
- `template_day_exercises`
- `template_day_exercise_muscle_groups`
- `template_day_routines`

### Sessions / History

- `workout_sessions`
- `session_exercises`
- `session_exercise_muscle_groups`
- `session_routines`
- `workout_sets`

### Marketplace

- `template_votes`
- `template_saves`
- `template_use_events`

### Analyzer

- `template_analysis_results`

### Async / Search / Cache Reliability

- `outbox_events`

---

# Auth

## `app_users`

```text
id uuid pk
email varchar not null
password_hash varchar not null
display_name varchar
role varchar not null default USER
created_at timestamptz not null
updated_at timestamptz not null
```

Roles:

```text
USER
COACH
ADMIN
```

## `refresh_tokens`

```text
id uuid pk
user_id uuid fk app_users(id)
token_hash varchar not null
expires_at timestamptz not null
revoked_at timestamptz null
created_at timestamptz not null
```

---

# Coach Mode

## `coach_profiles`

```text
coach_user_id uuid pk fk app_users(id)
created_by_admin_id uuid null fk app_users(id)
bio text null
active boolean not null default true
created_at timestamptz not null
updated_at timestamptz not null
```

Only users with role `COACH` should have coach profiles.

## `coach_client_relationships`

```text
id uuid pk
coach_user_id uuid fk app_users(id)
client_user_id uuid fk app_users(id)
status varchar not null
created_by_user_id uuid null fk app_users(id)
accepted_at timestamptz null
revoked_at timestamptz null
created_at timestamptz not null
updated_at timestamptz not null
```

Statuses:

```text
PENDING
ACTIVE
REVOKED
REJECTED
```

Security rule:

```text
coach can access client data only when relationship.status = ACTIVE
```

## `coach_session_comments`

```text
id uuid pk
coach_user_id uuid fk app_users(id)
client_user_id uuid fk app_users(id)
session_id uuid fk workout_sessions(id)
comment text not null
created_at timestamptz not null
updated_at timestamptz not null
deleted_at timestamptz null
```

## `coach_template_assignments`

```text
id uuid pk
coach_user_id uuid fk app_users(id)
client_user_id uuid fk app_users(id)
template_id uuid fk workout_templates(id)
note text null
status varchar not null
assigned_at timestamptz not null
completed_at timestamptz null
revoked_at timestamptz null
```

Statuses:

```text
ASSIGNED
COMPLETED
REVOKED
```

---

# Exercises

## `muscle_groups`

```text
code varchar pk
display_name varchar not null
```

Seed values include:

```text
CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, QUADS, HAMSTRINGS,
GLUTES, CALVES, CORE, FOREARMS, TRAPS, LATS, FULL_BODY, CARDIO
```

## `exercises`

```text
id uuid pk
owner_user_id uuid null fk app_users(id)
name varchar not null
description text null
exercise_type varchar not null
visibility varchar not null
created_at timestamptz not null
updated_at timestamptz not null
deleted_at timestamptz null
```

Exercise types:

```text
STRENGTH
CARDIO
MOBILITY
OTHER
```

Visibility:

```text
OFFICIAL
CUSTOM
```

Rules:

```text
OFFICIAL => owner_user_id is null
CUSTOM   => owner_user_id is not null
```

Unique indexes:

```text
official lower(name), where owner_user_id is null and deleted_at is null
custom owner_user_id + lower(name), where owner_user_id is not null and deleted_at is null
```

## `exercise_muscle_groups`

```text
exercise_id uuid fk exercises(id)
muscle_group_code varchar fk muscle_groups(code)
role varchar not null
primary key (exercise_id, muscle_group_code)
```

Roles:

```text
PRIMARY
SECONDARY
```

---

# Routines

## `routines`

```text
id uuid pk
user_id uuid fk app_users(id)
name varchar not null
routine_type varchar not null
content text not null
created_at timestamptz not null
updated_at timestamptz not null
deleted_at timestamptz null
```

Routine types:

```text
START
END
```

---

# Gyms

## `gyms`

```text
id uuid pk
user_id uuid fk app_users(id)
name varchar not null
location varchar null
created_at timestamptz not null
updated_at timestamptz not null
deleted_at timestamptz null
```

## `equipment`

```text
id uuid pk
user_id uuid fk app_users(id)
gym_id uuid fk gyms(id)
name varchar not null
equipment_type varchar null
notes text null
created_at timestamptz not null
updated_at timestamptz not null
deleted_at timestamptz null
```

Equipment types:

```text
BARBELL
DUMBBELL
MACHINE
CABLE
BENCH
BODYWEIGHT
CARDIO_MACHINE
OTHER
```

---

# Templates

## `workout_templates`

Represents a reusable workout program/template.

```text
id uuid pk
user_id uuid fk app_users(id)
name varchar not null
description text null
split_type varchar null
days_per_week int null
difficulty varchar null
estimated_duration_minutes int null
visibility varchar not null default PRIVATE
published_at timestamptz null
copied_from_template_id uuid null fk workout_templates(id)

aggregated_muscle_groups varchar[] not null default '{}'
aggregated_official_exercise_ids uuid[] not null default '{}'
aggregated_exercise_names text[] not null default '{}'

created_at timestamptz not null
updated_at timestamptz not null
deleted_at timestamptz null
```

Visibility:

```text
PRIVATE
PUBLIC
```

Split types:

```text
FULL_BODY
UPPER_LOWER
PPL
BRO_SPLIT
CUSTOM
```

Difficulty:

```text
BEGINNER
INTERMEDIATE
ADVANCED
```

## `template_stats`

```text
template_id uuid pk fk workout_templates(id)
upvotes_count int not null default 0
downvotes_count int not null default 0
saves_count int not null default 0
uses_count int not null default 0
rating_score numeric(12,4) not null default 0
trending_score numeric(12,4) not null default 0
lock_version int not null default 0
updated_at timestamptz not null
```

Use `rating_score` for up/down ranking and `trending_score` for recency-aware trending.

## `template_days`

```text
id uuid pk
template_id uuid fk workout_templates(id)
day_number int not null
name varchar not null
focus varchar null
estimated_duration_minutes int null
notes text null
created_at timestamptz not null
updated_at timestamptz not null
```

Focus:

```text
UPPER
LOWER
PUSH
PULL
LEGS
FULL_BODY
CUSTOM
```

Unique:

```text
(template_id, day_number)
```

## `template_day_exercises`

```text
id uuid pk
template_day_id uuid fk template_days(id)
exercise_id uuid null fk exercises(id)
exercise_name_snapshot varchar not null
exercise_type_snapshot varchar not null
position int not null
planned_sets int null
planned_reps varchar null
planned_weight numeric(8,2) null
rest_seconds int null
note text null
created_at timestamptz not null
updated_at timestamptz not null
```

Unique:

```text
(template_day_id, position)
```

## `template_day_exercise_muscle_groups`

```text
template_day_exercise_id uuid fk template_day_exercises(id)
muscle_group_code varchar not null
role varchar not null
primary key (template_day_exercise_id, muscle_group_code)
```

This snapshots muscle-group mapping at template level.

## `template_day_routines`

```text
id uuid pk
template_day_id uuid fk template_days(id)
routine_id uuid null fk routines(id)
routine_type varchar not null
routine_name_snapshot varchar not null
routine_content_snapshot text not null
position int not null
```

Unique:

```text
(template_day_id, routine_type, position)
```

---

# Workout Sessions

## `workout_sessions`

```text
id uuid pk
user_id uuid fk app_users(id)
template_id uuid null fk workout_templates(id)
template_day_id uuid null fk template_days(id)
template_name_snapshot varchar null
template_day_name_snapshot varchar null
gym_id uuid null fk gyms(id)
gym_name_snapshot varchar null
status varchar not null
started_at timestamptz not null
finished_at timestamptz null
notes text null
created_at timestamptz not null
updated_at timestamptz not null
```

Statuses:

```text
IN_PROGRESS
FINISHED
CANCELLED
```

## `session_exercises`

```text
id uuid pk
session_id uuid fk workout_sessions(id)
original_template_day_exercise_id uuid null fk template_day_exercises(id)
original_exercise_id uuid null fk exercises(id)
exercise_name_snapshot varchar not null
exercise_type_snapshot varchar not null
position int not null
planned_sets_snapshot int null
planned_reps_snapshot varchar null
planned_weight_snapshot numeric(8,2) null
rest_seconds_snapshot int null
template_note_snapshot text null
is_extra_exercise boolean not null default false
created_at timestamptz not null
updated_at timestamptz not null
```

`position` is indexed but intentionally not unique because live workout reordering can temporarily duplicate positions.

## `session_exercise_muscle_groups`

```text
session_exercise_id uuid fk session_exercises(id)
muscle_group_code_snapshot varchar not null
role_snapshot varchar not null
primary key (session_exercise_id, muscle_group_code_snapshot)
```

## `session_routines`

```text
id uuid pk
session_id uuid fk workout_sessions(id)
original_routine_id uuid null fk routines(id)
routine_type varchar not null
routine_name_snapshot varchar not null
routine_content_snapshot text not null
position int not null
started_at timestamptz null
ended_at timestamptz null
```

## `workout_sets`

```text
id uuid pk
session_exercise_id uuid fk session_exercises(id)
set_number int not null
set_type varchar not null default WORKING
weight numeric(8,2) null
reps int null
duration_seconds int null
distance_meters numeric(10,2) null
rpe numeric(3,1) null
note text null
equipment_id uuid null fk equipment(id)
equipment_name_snapshot varchar null
completed_at timestamptz null
created_at timestamptz not null
updated_at timestamptz not null
```

Set types:

```text
WARMUP
WORKING
DROP
FAILURE
```

---

# Marketplace

## `template_votes`

```text
user_id uuid fk app_users(id)
template_id uuid fk workout_templates(id)
vote_type varchar not null
created_at timestamptz not null
primary key (user_id, template_id)
```

Vote types:

```text
UP
DOWN
```

## `template_saves`

```text
user_id uuid fk app_users(id)
template_id uuid fk workout_templates(id)
created_at timestamptz not null
primary key (user_id, template_id)
```

## `template_use_events`

```text
id uuid pk
user_id uuid fk app_users(id)
source_template_id uuid null fk workout_templates(id)
copied_template_id uuid null fk workout_templates(id)
source_template_name_snapshot varchar not null
copied_template_name_snapshot varchar not null
created_at timestamptz not null
```

---

# Analyzer

## `template_analysis_results`

```text
id uuid pk
template_id uuid fk workout_templates(id)
score int not null
category varchar not null
positives jsonb not null default []
warnings jsonb not null default []
notes jsonb not null default []
created_at timestamptz not null
```

Categories:

```text
NEEDS_REVIEW
DECENT_STRUCTURE
WELL_STRUCTURED
```

---

# Outbox

## `outbox_events`

```text
id uuid pk
aggregate_type varchar not null
aggregate_id uuid not null
event_type varchar not null
payload jsonb not null
status varchar not null default PENDING
attempts int not null default 0
available_at timestamptz not null
processed_at timestamptz null
created_at timestamptz not null
last_error text null
```

Used for:

- OpenSearch indexing
- Redis invalidation
- template stats recomputation
- async analytics

Statuses:

```text
PENDING
PROCESSING
PROCESSED
FAILED
```

---

# Critical Snapshot Chain

When starting a workout:

```text
template_day_exercises -> session_exercises
template_day_exercise_muscle_groups -> session_exercise_muscle_groups
template_day_routines -> session_routines
```

Never make workout history depend on editable planning data.

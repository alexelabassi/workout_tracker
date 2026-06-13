# SEARCH_AND_CACHING.md

# Search and Caching Strategy

## Principle

PostgreSQL is the source of truth.

Redis and OpenSearch are derived systems.

If Redis/OpenSearch fail, the app should still work using PostgreSQL, just slower or with reduced search quality.

---

# Template Search

## Phase 1: PostgreSQL Structured Search

Use `workout_templates` aggregate arrays:

```text
aggregated_muscle_groups
aggregated_official_exercise_ids
aggregated_exercise_names
```

Supported filters:

- visibility = PUBLIC
- split type
- days per week
- difficulty
- includes exercise
- excludes exercise
- includes muscle group
- text query over name/description/exercise names
- sort by popular/trending/newest

Use GIN indexes on aggregate arrays.

## Phase 2: OpenSearch Template Index

Index name:

```text
templates_v1
```

Document fields:

```json
{
  "templateId": "...",
  "ownerUserId": "...",
  "name": "...",
  "description": "...",
  "splitType": "UPPER_LOWER",
  "daysPerWeek": 4,
  "difficulty": "INTERMEDIATE",
  "visibility": "PUBLIC",
  "muscleGroups": ["CHEST", "BACK"],
  "exerciseNames": ["Bench Press", "Squat"],
  "officialExerciseIds": ["..."],
  "upvotes": 120,
  "downvotes": 10,
  "saves": 45,
  "uses": 80,
  "ratingScore": 0.91,
  "trendingScore": 42.5,
  "publishedAt": "..."
}
```

---

# Workout History Search

## OpenSearch Index

Index name:

```text
workout_sessions_v1
```

Document shape:

```json
{
  "sessionId": "...",
  "userId": "...",
  "templateName": "Upper Lower 4 Days",
  "templateDayName": "Upper 1",
  "gymName": "World Class",
  "startedAt": "...",
  "finishedAt": "...",
  "notes": "...",
  "exercises": [
    {
      "name": "Bench Press",
      "muscleGroups": ["CHEST", "TRICEPS"],
      "sets": [
        {
          "weight": 80,
          "reps": 8,
          "equipmentName": "Bench 1",
          "note": "felt strong"
        }
      ]
    }
  ]
}
```

Search examples:

```text
bench press last month
sessions at World Class with dumbbells
tooth pain note
squat PR
workouts with shoulder pain
```

---

# Redis Caching

Use Redis for marketplace list caches.

Suggested keys:

```text
marketplace:popular:global
marketplace:trending:7d
marketplace:newest
marketplace:popular:split:UPPER_LOWER
marketplace:popular:days:4
marketplace:search:{hashOfFilters}
```

TTL:

```text
popular/trending: 5-15 minutes
search result lists: 2-5 minutes
newest: 1-3 minutes
```

Do not cache private user data first. Start with marketplace public data.

---

# Cache Invalidation

Events that should invalidate marketplace caches:

- template published
- template unpublished
- template updated
- template voted
- template saved
- template used

Implementation path:

1. Write DB transaction.
2. Insert outbox event.
3. Background worker processes event.
4. Recompute stats/index/cache.

For Phase 1, invalidate directly in service if no worker yet.

---

# Ranking

Do not rank by raw upvotes only.

Use:

```text
rating_score = quality score from up/down ratio
trending_score = rating + recency + usage/saves
```

Simple initial ranking:

```text
popular = upvotes - downvotes + saves * 0.5 + uses * 0.25
```

Better later:

```text
Wilson lower bound for up/down votes
```

For thesis, explaining why raw votes are naive is a good CS/product point.

# ROUTINE_ANALYZER.md

# Evidence-Informed Template Structure Analyzer

## Naming Decision

Do not call this feature:

- "scientifically good"
- "scientifically bad"
- "perfect routine"
- "AI coach"

Use:

> **Template Structure Analyzer**

or

> **Evidence-Informed Routine Analyzer**

The analyzer is rule-based, conservative, and explainable.

---

## Output Categories

```text
NEEDS_REVIEW
DECENT_STRUCTURE
WELL_STRUCTURED
```

Avoid insulting wording. Say the structure needs review, not that the user's routine is "bad".

---

## Analyzer Output

```json
{
  "score": 74,
  "category": "DECENT_STRUCTURE",
  "positives": [
    "Most major muscle groups are trained at least twice weekly.",
    "Push and pull volume are reasonably balanced."
  ],
  "warnings": [
    "Quadriceps volume is much higher than hamstring volume.",
    "No END routine/cooldown attached."
  ],
  "notes": [
    "This is a structure analysis, not medical or individualized coaching advice."
  ]
}
```

---

## Data Used

From templates:

- template days
- template exercises
- planned sets
- planned reps
- muscle group mappings
- routine attachments
- exercise types
- estimated duration
- equipment requirements

From users later:

- workout completion history
- skipped exercises
- actual sets completed
- performance trends

---

## Initial Rule Categories

### 1. Muscle Group Coverage

Checks whether major muscle groups appear in the template:

- chest
- back/lats
- shoulders
- quads
- hamstrings
- glutes
- calves
- core
- arms

Warnings:

```text
Missing lower-body work
Missing posterior-chain work
Missing pulling movements
Missing core work
```

### 2. Weekly Volume

Estimate planned weekly hard sets per muscle group.

Simple estimate:

```text
primary muscle = 1.0 set credit
secondary muscle = 0.5 set credit
```

Example:

```text
Bench Press, 3 sets:
CHEST primary => +3 chest sets
TRICEPS secondary => +1.5 triceps sets
SHOULDERS secondary => +1.5 shoulders sets
```

Conservative warning bands:

```text
0 sets/week          => missing
1-4 sets/week        => low
5-20 sets/week       => normal broad range
21+ sets/week        => high, needs recovery attention
```

Do not claim 5-20 is universally optimal. It is a practical analysis band.

### 3. Frequency

Count how many template days train each muscle group.

Warnings:

```text
Major muscle trained only once weekly
Same muscle trained on too many consecutive days
```

### 4. Push/Pull Balance

Compare push volume:

```text
CHEST + SHOULDERS + TRICEPS
```

vs pull volume:

```text
BACK + LATS + BICEPS + TRAPS
```

Warning if ratio is extreme.

### 5. Upper/Lower Balance

Compare upper-body volume vs lower-body volume.

Warning if one dominates heavily.

### 6. Movement Diversity

Check for presence of:

- horizontal push
- horizontal pull
- vertical push
- vertical pull
- squat/lunge pattern
- hip hinge pattern
- core

This requires exercises to have optional movement pattern metadata later. If absent, use muscle groups only.

### 7. Routine Completeness

Warnings:

```text
No START routine attached
No END routine attached
Template day has exercises but no warmup/cooldown instructions
```

### 8. Equipment Requirements

Warn if template requires many gym-specific machines.

Useful for marketplace filtering.

---

## Scoring

Start from 100.

Example deductions:

```text
missing major muscle group: -8
very low weekly sets for major muscle: -5
extreme push/pull imbalance: -8
extreme upper/lower imbalance: -8
no START routine: -3
no END routine: -3
too many consecutive days hitting same muscle: -8
very high planned volume for several muscles: -8
```

Categories:

```text
0-49   NEEDS_REVIEW
50-79  DECENT_STRUCTURE
80-100 WELL_STRUCTURED
```

Keep scoring transparent.

---

## Scientific/Evidence Framing

Use conservative language:

Good:

```text
"Evidence-informed structural warning"
"Common programming heuristic"
"May require review"
"Compared with common resistance-training programming principles"
```

Bad:

```text
"scientifically proven bad"
"optimal"
"guaranteed results"
"you should deload"
```

---

## References To Mention In Thesis

1. ACSM 2009 Position Stand: progression models in resistance training; includes frequency recommendations for novice/intermediate/advanced trainees.
2. Schoenfeld et al. 2016 systematic review/meta-analysis: training frequency and hypertrophy; evidence suggests training muscle groups at least twice weekly can be beneficial compared to once weekly.
3. Schoenfeld et al. 2021 loading recommendations: loading strategies differ for strength/hypertrophy; strength tends to benefit from heavier loading while hypertrophy can be achieved across a range of loads.
4. Currier et al. 2023 BJSM network meta-analysis: resistance training prescriptions outperform control for strength/hypertrophy; heavier loads maximize strength, while multiple prescriptions can promote hypertrophy.

The thesis should cite sources carefully and avoid turning population-level evidence into individualized medical advice.

---

## Implemented Rules (Phase 9)

The analyzer is **deterministic and rule-based** (no ML, no personalization). It is read-only,
computed live on `GET /api/templates/{templateId}/analysis`, and never persisted. It evaluates a
single template's **structure** using general resistance-training heuristics.

### Model construction (assumptions)
- **Weekly = sum over all authored days, each counted once.** If `days_per_week` exceeds the
  number of authored days, an INFO limitation notes the assumption (no fabricated schedule).
- **Weighted sets** = `plannedSets × roleWeight` with PRIMARY = 1.0, SECONDARY = 0.5 (no TERTIARY
  in schema). Per exercise, each bucket takes its strongest role (no double counting).
- **Missing `plannedSets` is NOT defaulted.** Such exercises are excluded from volume (they still
  count for coverage/frequency presence) and a `VOLUME_DATA_INCOMPLETE` finding + limitation make
  clear that volume figures are incomplete. Unknown sets are never assumed to be a number.
- **Only `STRENGTH` exercises** count toward volume/balance; CARDIO/MOBILITY/OTHER are excluded
  (noted as a limitation if present).
- **Muscle buckets** (15 codes folded): Back = BACK+LATS+TRAPS; others 1:1; FULL_BODY/CARDIO codes
  excluded from per-muscle thresholds. Raw per-bucket volumes are reported.
- **Compound vs isolation** heuristic: ≥3 distinct muscle group codes = compound (for rest rules).
- **Difficulty** null → treated as INTERMEDIATE (limitation noted).
- Rep rules are **goal-agnostic and extreme-only** (templates have no goal field).

### Rules
1. **Volume per bucket** (weighted weekly sets): ≤3 very low; 4–7 low for intermediates; 8–20 good;
   21–25 very high; >25 excessive.
2. **Major coverage** (presence-based): no lower body (Quads+Hams+Glutes), no pulling (Back), no
   pushing (Chest+Shoulders), no posterior chain (Hams+Glutes) → HIGH. Minor groups
   (biceps/triceps/calves/core/forearms) cannot mask a missing major region.
3. **Frequency / concentration**: a bucket with ≥10 weekly weighted sets all on one day →
   `VOLUME_CONCENTRATED`.
4. **Per-session**: ≥10 weighted sets for one muscle in a session → `MUSCLE_CONCENTRATION_HIGH`;
   session total >30 excessive, >20 long, 1–8 short (skipped when the day has unknown set counts).
5. **Balance** (structural heuristics, worst tier only): pull/push <0.5 HIGH / <0.7 MEDIUM;
   posterior/quads <0.3 HIGH / <0.5 MEDIUM; lower/upper and upper/lower <0.5 MEDIUM unless the
   template's day focuses indicate it is intentionally specialized. Rear deltoids are not separable
   from SHOULDERS, so all shoulder work is treated as pushing (documented limitation).
6. **Rest**: compound <60s or isolation <30s → LOW. Missing rest → INFO limitation, no penalty.
7. **Rep range** (extreme-only): ~all sets ≤5 reps → LOW (strength-biased); ~all ≥30 → MEDIUM.
8. **Proximity to failure**: always an INFO limitation — templates do not store RIR/RPE.
9. **Difficulty suitability**: BEGINNER warnings (total weekly sets >70, any muscle >18, >6
   sessions, >8 exercises/session, isolation-only with no compounds); ADVANCED note when any muscle
   exceeds 25 weekly sets.

### Scoring — "Template Structure Score" (100)
Each sub-score starts at its max and deducts per fired finding, floored at 0:

| Sub-score | Max | Deductions |
|---|---|---|
| Volume coverage | 35 | NO_LOWER_BODY/NO_PULL/NO_PUSH −12; NO_POSTERIOR_CHAIN −8; major missing −6; minor missing −2; very low −3; low −1; very high −2; excessive −5 |
| Frequency | 20 | VOLUME_CONCENTRATED −4 each |
| Balance | 20 | push/pull, posterior/quads HIGH −8 / MEDIUM −4; lower/upper or upper/lower MEDIUM −4 |
| Session design | 15 | session excessive −5; long −2; short −2; muscle concentration −3 |
| Specificity / rest | 10 | each short-rest −2; rep endurance −3; rep low −2 |

`overallScore` = sum of sub-scores. Category: ≥80 WELL_STRUCTURED, 55–79 DECENT_STRUCTURE,
<55 NEEDS_REVIEW. `VOLUME_DATA_INCOMPLETE` and all INFO items carry **no** score deduction.

### Limitations (always surfaced)
- Cannot assess effort/proximity to failure (no RIR/RPE in templates).
- Volume excludes exercises without planned set counts (figures may be incomplete).
- Rest heuristics skipped when no planned rest is set; non-numeric reps cannot be analyzed.
- Difficulty assumed INTERMEDIATE when unset; rear delts not separable from shoulders.
- CARDIO/MOBILITY/OTHER excluded from resistance volume.

### Disclaimer (returned with every analysis)
> This analysis uses general evidence-informed training heuristics. It is not medical advice, does
> not account for injury history, recovery, technique, proximity to failure, sleep, nutrition, or
> individual response, and should be interpreted as structural feedback on the template.

This is **evidence-informed and deterministic structural feedback**, not personalized medical or
training advice, and makes no claim of optimality or 100% scientific correctness.

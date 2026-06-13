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

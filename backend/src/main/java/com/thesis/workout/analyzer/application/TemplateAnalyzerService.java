package com.thesis.workout.analyzer.application;

import com.thesis.workout.analyzer.web.dto.AnalysisResponse;
import com.thesis.workout.analyzer.web.dto.AnalysisWarning;
import com.thesis.workout.analyzer.web.dto.BalanceRatiosResponse;
import com.thesis.workout.analyzer.web.dto.MuscleFrequencyResponse;
import com.thesis.workout.analyzer.web.dto.MuscleVolumeResponse;
import com.thesis.workout.analyzer.web.dto.Severity;
import com.thesis.workout.analyzer.web.dto.SubScoresResponse;
import com.thesis.workout.exercise.domain.model.ExerciseType;
import com.thesis.workout.exercise.domain.model.MuscleRole;
import com.thesis.workout.template.application.TemplateAnalysisSource;
import com.thesis.workout.template.application.analysis.TemplateAnalysisModel;
import com.thesis.workout.template.domain.model.DayFocus;
import com.thesis.workout.template.domain.model.Difficulty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic, rule-based, evidence-informed structural analyzer for workout templates. No ML,
 * no personalization, no medical claims — only general resistance-training heuristics over the
 * template's structure. Produces a "Template Structure Score" with explainable warnings.
 */
@Service
public class TemplateAnalyzerService {

    static final String DISCLAIMER = "This analysis uses general evidence-informed training "
            + "heuristics. It is not medical advice, does not account for injury history, recovery, "
            + "technique, proximity to failure, sleep, nutrition, or individual response, and should "
            + "be interpreted as structural feedback on the template.";

    // Analysis buckets (the 15 seeded codes folded into movement-relevant groups).
    private static final String CHEST = "CHEST";
    private static final String BACK = "BACK";
    private static final String SHOULDERS = "SHOULDERS";
    private static final String BICEPS = "BICEPS";
    private static final String TRICEPS = "TRICEPS";
    private static final String QUADS = "QUADS";
    private static final String HAMSTRINGS = "HAMSTRINGS";
    private static final String GLUTES = "GLUTES";
    private static final String CALVES = "CALVES";
    private static final String CORE = "CORE";
    private static final String FOREARMS = "FOREARMS";

    private static final Map<String, String> CODE_TO_BUCKET = Map.ofEntries(
            Map.entry("CHEST", CHEST), Map.entry("BACK", BACK), Map.entry("LATS", BACK), Map.entry("TRAPS", BACK),
            Map.entry("SHOULDERS", SHOULDERS), Map.entry("BICEPS", BICEPS), Map.entry("TRICEPS", TRICEPS),
            Map.entry("QUADS", QUADS), Map.entry("HAMSTRINGS", HAMSTRINGS), Map.entry("GLUTES", GLUTES),
            Map.entry("CALVES", CALVES), Map.entry("CORE", CORE), Map.entry("FOREARMS", FOREARMS));
    // FULL_BODY and CARDIO codes are intentionally excluded from per-muscle thresholds.

    private static final List<String> MAJOR_BUCKETS = List.of(CHEST, BACK, SHOULDERS, QUADS, HAMSTRINGS, GLUTES);
    private static final List<String> ALL_BUCKETS = List.of(
            CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, QUADS, HAMSTRINGS, GLUTES, CALVES, CORE, FOREARMS);
    private static final Pattern INT_PATTERN = Pattern.compile("\\d+");

    private final TemplateAnalysisSource analysisSource;

    public TemplateAnalyzerService(TemplateAnalysisSource analysisSource) {
        this.analysisSource = analysisSource;
    }

    @Transactional(readOnly = true)
    public AnalysisResponse analyze(UUID userId, UUID templateId) {
        TemplateAnalysisModel.Root template = analysisSource.loadForAnalysis(userId, templateId);
        return new Analysis(template).run();
    }

    /** Per-bucket weekly accumulation. */
    private static final class Bucket {
        double weeklyWeightedSets;
        int presentExerciseCount;
        boolean hasUnknownSets;
        final Set<UUID> daysTrained = new java.util.HashSet<>();
    }

    /** One analysis pass over a single template; instance state keeps the rule code readable. */
    private final class Analysis {
        private final TemplateAnalysisModel.Root template;
        private final Difficulty difficulty;
        private final Map<String, Bucket> buckets = new LinkedHashMap<>();
        private final Map<UUID, Map<String, Double>> perDayBucketSets = new LinkedHashMap<>();
        private final Map<UUID, Double> perDayKnownSets = new LinkedHashMap<>();
        private final Map<UUID, Integer> perDayExerciseCount = new LinkedHashMap<>();

        private int strengthExercises;
        private int strengthExercisesMissingSets;
        private boolean anyRestData;
        private boolean anyCompound;
        private int repExercisesParsed;
        private int repExercisesLow;   // <= 5 reps
        private int repExercisesHigh;  // >= 30 reps
        private int unparsedReps;

        private final List<AnalysisWarning> warnings = new ArrayList<>();
        private final List<String> strengths = new ArrayList<>();
        private final List<String> suggestions = new ArrayList<>();
        private final List<String> limitations = new ArrayList<>();

        private Analysis(TemplateAnalysisModel.Root template) {
            this.template = template;
            this.difficulty = template.difficulty();
            ALL_BUCKETS.forEach(b -> buckets.put(b, new Bucket()));
        }

        AnalysisResponse run() {
            accumulate();

            int volumeCoverage = scoreVolumeAndCoverage();
            int frequency = scoreFrequency();
            int balance = scoreBalance();
            int sessionDesign = scoreSessionDesign();
            int specificityRest = scoreSpecificityAndRest();

            applyDifficultyHeuristics();
            collectStrengths();
            collectLimitations();
            deriveSuggestions();

            int overall = volumeCoverage + frequency + balance + sessionDesign + specificityRest;
            String category = category(overall);

            return new AnalysisResponse(template.templateId(), overall, category, summary(overall, category),
                    new SubScoresResponse(volumeCoverage, frequency, balance, sessionDesign, specificityRest),
                    muscleVolumes(), frequencies(), ratios(), warnings, suggestions, strengths,
                    limitations, DISCLAIMER);
        }

        // --- accumulation -------------------------------------------------------------------

        private void accumulate() {
            for (TemplateAnalysisModel.Day day : template.days()) {
                perDayBucketSets.put(day.id(), new LinkedHashMap<>());
                perDayKnownSets.put(day.id(), 0.0);
                perDayExerciseCount.put(day.id(), 0);
                for (TemplateAnalysisModel.Exercise exercise : day.exercises()) {
                    if (exercise.type() != ExerciseType.STRENGTH) {
                        continue; // only resistance-training work counts toward volume/balance
                    }
                    accumulateExercise(day, exercise);
                }
            }
        }

        private void accumulateExercise(TemplateAnalysisModel.Day day, TemplateAnalysisModel.Exercise exercise) {
            strengthExercises++;
            perDayExerciseCount.merge(day.id(), 1, Integer::sum);

            long distinctCodes = exercise.muscles().stream().map(TemplateAnalysisModel.Muscle::code).distinct().count();
            if (distinctCodes >= 3) {
                anyCompound = true;
            }

            Integer plannedSets = exercise.plannedSets();
            boolean known = plannedSets != null;
            if (!known) {
                strengthExercisesMissingSets++;
            } else {
                perDayKnownSets.merge(day.id(), plannedSets.doubleValue(), Double::sum);
            }

            Map<String, Double> bucketRole = new LinkedHashMap<>();
            for (TemplateAnalysisModel.Muscle muscle : exercise.muscles()) {
                String bucket = CODE_TO_BUCKET.get(muscle.code());
                if (bucket == null) {
                    continue;
                }
                double weight = muscle.role() == MuscleRole.PRIMARY ? 1.0 : 0.5;
                bucketRole.merge(bucket, weight, Math::max);
            }
            for (Map.Entry<String, Double> entry : bucketRole.entrySet()) {
                Bucket bucket = buckets.get(entry.getKey());
                bucket.presentExerciseCount++;
                bucket.daysTrained.add(day.id());
                if (known) {
                    double weighted = plannedSets * entry.getValue();
                    bucket.weeklyWeightedSets += weighted;
                    perDayBucketSets.get(day.id()).merge(entry.getKey(), weighted, Double::sum);
                } else {
                    bucket.hasUnknownSets = true;
                }
            }

            if (exercise.restSeconds() != null) {
                anyRestData = true;
                boolean compound = distinctCodes >= 3;
                if (compound && exercise.restSeconds() < 60) {
                    add("REST_SHORT_COMPOUND", Severity.LOW, "Short rest on a compound lift",
                            "Heuristic: heavy compound lifts usually recover better with 90–180s rest; "
                                    + exercise.name() + " plans " + exercise.restSeconds() + "s.",
                            List.of(), "Consider 90–180s rest on heavy compounds.");
                } else if (!compound && exercise.restSeconds() < 30) {
                    add("REST_SHORT_ISOLATION", Severity.LOW, "Very short rest on an isolation lift",
                            "Heuristic: isolation work is commonly done with 30–120s rest; "
                                    + exercise.name() + " plans " + exercise.restSeconds() + "s.",
                            List.of(), "Consider at least 30–60s rest.");
                }
            }

            Integer reps = parseReps(exercise.plannedReps());
            if (exercise.plannedReps() != null && !exercise.plannedReps().isBlank() && reps == null) {
                unparsedReps++;
            } else if (reps != null) {
                repExercisesParsed++;
                if (reps <= 5) {
                    repExercisesLow++;
                } else if (reps >= 30) {
                    repExercisesHigh++;
                }
            }
        }

        // --- scoring ------------------------------------------------------------------------

        private int scoreVolumeAndCoverage() {
            int score = 35;

            boolean lowerBody = present(QUADS) + present(HAMSTRINGS) + present(GLUTES) > 0;
            boolean pulling = present(BACK) > 0;
            boolean pushing = present(CHEST) + present(SHOULDERS) > 0;
            boolean posterior = present(HAMSTRINGS) + present(GLUTES) > 0;

            if (!lowerBody) {
                add("NO_LOWER_BODY", Severity.HIGH, "No lower-body work",
                        "Heuristic: the template trains no quads, hamstrings or glutes.",
                        List.of(QUADS, HAMSTRINGS, GLUTES), "Add squat/hinge/lunge-pattern work.");
                score -= 12;
            } else if (!posterior) {
                add("NO_POSTERIOR_CHAIN", Severity.HIGH, "No posterior-chain work",
                        "Heuristic: quads are trained but hamstrings/glutes are not, which is a common imbalance.",
                        List.of(HAMSTRINGS, GLUTES), "Add a hip-hinge (RDL/deadlift) or glute movement.");
                score -= 8;
            }
            if (!pulling) {
                add("NO_PULL", Severity.HIGH, "No back/pulling work",
                        "Heuristic: the template trains no back/lats/upper-back; biceps alone do not cover pulling.",
                        List.of(BACK), "Add rows and/or vertical pulls.");
                score -= 12;
            }
            if (!pushing) {
                add("NO_PUSH", Severity.HIGH, "No pushing/chest work",
                        "Heuristic: the template trains no chest or shoulders; triceps alone do not cover pushing.",
                        List.of(CHEST, SHOULDERS), "Add a horizontal and/or vertical press.");
                score -= 12;
            }

            for (String bucket : ALL_BUCKETS) {
                Bucket data = buckets.get(bucket);
                boolean major = MAJOR_BUCKETS.contains(bucket);
                if (data.presentExerciseCount == 0) {
                    if (major && coveredByRegionWarning(bucket, lowerBody, pulling, pushing)) {
                        continue; // already reported by a region-level warning
                    }
                    add(major ? "MAJOR_MUSCLE_MISSING" : "MUSCLE_GROUP_MISSING",
                            major ? Severity.MEDIUM : Severity.LOW,
                            (major ? "Missing major muscle group: " : "Untrained muscle group: ") + bucket,
                            "Heuristic: no exercise targets " + bucket + ".",
                            List.of(bucket), "Consider adding direct or indirect " + bucket + " work.");
                    score -= major ? 6 : 2;
                    continue;
                }
                double volume = data.weeklyWeightedSets;
                if (volume == 0) {
                    continue; // present but all sets unknown — covered by the incompleteness limitation
                }
                if (volume <= 3) {
                    add("MUSCLE_VOLUME_VERY_LOW", major ? Severity.HIGH : Severity.MEDIUM,
                            "Very low weekly volume for " + bucket,
                            "Heuristic: " + fmt(volume) + " weighted sets/week is low for most goals.",
                            List.of(bucket), "Aim for roughly 8+ weekly weighted sets if growth is a goal.");
                    score -= 3;
                } else if (volume <= 7 && difficultyAtLeastIntermediate()) {
                    add("MUSCLE_VOLUME_LOW", Severity.LOW, "Low weekly volume for " + bucket,
                            "Heuristic: " + fmt(volume) + " weighted sets/week is on the low side for intermediates.",
                            List.of(bucket), "Consider increasing toward 8–14 weekly sets.");
                    score -= 1;
                } else if (volume > 25) {
                    add("MUSCLE_VOLUME_EXCESSIVE", Severity.HIGH, "Excessive weekly volume for " + bucket,
                            "Heuristic: " + fmt(volume) + " weighted sets/week is very high and risks junk volume/fatigue.",
                            List.of(bucket), "Consider trimming toward ~20 weekly sets.");
                    score -= 5;
                } else if (volume > 20) {
                    add("MUSCLE_VOLUME_HIGH", Severity.MEDIUM, "Very high weekly volume for " + bucket,
                            "Heuristic: " + fmt(volume) + " weighted sets/week is high; watch for diminishing returns.",
                            List.of(bucket), "This is reasonable only for advanced, well-recovered lifters.");
                    score -= 2;
                }
            }
            return Math.max(0, score);
        }

        private int scoreFrequency() {
            int score = 20;
            for (String bucket : ALL_BUCKETS) {
                Bucket data = buckets.get(bucket);
                if (data.weeklyWeightedSets >= 10 && data.daysTrained.size() == 1) {
                    add("VOLUME_CONCENTRATED", Severity.MEDIUM, "Weekly volume concentrated in one session for " + bucket,
                            "Heuristic: " + fmt(data.weeklyWeightedSets) + " weighted sets all on one day; "
                                    + "splitting across 2 sessions often improves quality.",
                            List.of(bucket), "Distribute " + bucket + " volume across two training days.");
                    score = Math.max(0, score - 4);
                }
            }
            return score;
        }

        private int scoreBalance() {
            int score = 20;
            Double pullPush = ratio(volume(BACK) + volume(BICEPS), pushVolume());
            if (pullPush != null) {
                if (pullPush < 0.5) {
                    add("PUSH_PULL_IMBALANCE", Severity.HIGH, "Pulling volume much lower than pushing",
                            "Structural heuristic: pull/push ratio is " + fmt(pullPush) + " (< 0.5).",
                            List.of(BACK, BICEPS), "Add pulling volume to approach a ~1:1 push/pull balance.");
                    score -= 8;
                } else if (pullPush < 0.7) {
                    add("PUSH_PULL_IMBALANCE", Severity.MEDIUM, "Pulling volume lower than pushing",
                            "Structural heuristic: pull/push ratio is " + fmt(pullPush) + " (< 0.7).",
                            List.of(BACK, BICEPS), "Consider more pulling volume for balance.");
                    score -= 4;
                }
            }
            Double postQuad = ratio(volume(HAMSTRINGS) + volume(GLUTES), volume(QUADS));
            if (postQuad != null) {
                if (postQuad < 0.3) {
                    add("POSTERIOR_QUAD_IMBALANCE", Severity.HIGH, "Posterior chain much lower than quads",
                            "Structural heuristic: posterior/quads ratio is " + fmt(postQuad) + " (< 0.3).",
                            List.of(HAMSTRINGS, GLUTES), "Add hinge/glute work to balance the lower body.");
                    score -= 8;
                } else if (postQuad < 0.5) {
                    add("POSTERIOR_QUAD_IMBALANCE", Severity.MEDIUM, "Posterior chain lower than quads",
                            "Structural heuristic: posterior/quads ratio is " + fmt(postQuad) + " (< 0.5).",
                            List.of(HAMSTRINGS, GLUTES), "Consider more hamstring/glute volume.");
                    score -= 4;
                }
            }
            Double lowerUpper = ratio(lowerVolume(), upperVolume());
            if (lowerUpper != null && lowerUpper < 0.5 && !isUpperFocused()) {
                add("LOWER_UPPER_IMBALANCE", Severity.MEDIUM, "Lower-body volume much lower than upper",
                        "Structural heuristic: lower/upper ratio is " + fmt(lowerUpper) + " (< 0.5).",
                        List.of(QUADS, HAMSTRINGS, GLUTES), "Add lower-body volume unless this is intentionally upper-focused.");
                score -= 4;
            }
            Double upperLower = ratio(upperVolume(), lowerVolume());
            if (upperLower != null && upperLower < 0.5 && !isLowerFocused()) {
                add("UPPER_LOWER_IMBALANCE", Severity.MEDIUM, "Upper-body volume much lower than lower",
                        "Structural heuristic: upper/lower ratio is " + fmt(upperLower) + " (< 0.5).",
                        List.of(CHEST, BACK, SHOULDERS), "Add upper-body volume unless this is intentionally lower-focused.");
                score -= 4;
            }
            return Math.max(0, score);
        }

        private int scoreSessionDesign() {
            int score = 15;
            for (TemplateAnalysisModel.Day day : template.days()) {
                double total = perDayKnownSets.getOrDefault(day.id(), 0.0);
                boolean dayHasUnknown = day.exercises().stream()
                        .anyMatch(e -> e.type() == ExerciseType.STRENGTH && e.plannedSets() == null);

                if (total > 30) {
                    add("SESSION_EXCESSIVE", Severity.MEDIUM, "Very high set count in one session (" + day.name() + ")",
                            "Heuristic: " + fmt(total) + " sets in a single session is excessive for most users.",
                            List.of(), "Split this day or reduce volume.");
                    score -= 5;
                } else if (total > 20) {
                    add("SESSION_LONG", Severity.LOW, "Long session (" + day.name() + ")",
                            "Heuristic: " + fmt(total) + " sets is a long, high-volume session.",
                            List.of(), "Fine if recovery allows; otherwise trim.");
                    score -= 2;
                } else if (total > 0 && total <= 8 && !dayHasUnknown) {
                    add("SESSION_SHORT", Severity.LOW, "Short session (" + day.name() + ")",
                            "Heuristic: " + fmt(total) + " sets is a short session; it may be incomplete.",
                            List.of(), "Add work if this day is meant to be a full session.");
                    score -= 2;
                }

                for (Map.Entry<String, Double> entry : perDayBucketSets.get(day.id()).entrySet()) {
                    if (entry.getValue() >= 10) {
                        add("MUSCLE_CONCENTRATION_HIGH", Severity.MEDIUM,
                                "Heavy single-session load on " + entry.getKey() + " (" + day.name() + ")",
                                "Heuristic: " + fmt(entry.getValue()) + " sets for one muscle in a session risks junk volume.",
                                List.of(entry.getKey()), "Spread this muscle's volume across days.");
                        score -= 3;
                    }
                }
            }
            return Math.max(0, score);
        }

        private int scoreSpecificityAndRest() {
            int score = 10;
            for (AnalysisWarning warning : warnings) {
                if (warning.code().startsWith("REST_SHORT")) {
                    score -= 2;
                }
            }
            if (repExercisesParsed > 0) {
                if (repExercisesHigh * 10 >= repExercisesParsed * 9) {
                    add("REP_RANGE_ENDURANCE", Severity.MEDIUM, "Almost all sets are very high-rep",
                            "Heuristic: ~all parsed sets are 30+ reps, which is endurance-biased.",
                            List.of(), "Add lower-rep work unless endurance/conditioning is the intent.");
                    score -= 3;
                } else if (repExercisesLow * 10 >= repExercisesParsed * 9) {
                    add("REP_RANGE_LOW", Severity.LOW, "Almost all sets are very low-rep",
                            "Heuristic: ~all parsed sets are ≤5 reps (strength-biased); higher-rep volume is limited.",
                            List.of(), "Add some 6–15 rep work if hypertrophy is also a goal.");
                    score -= 2;
                }
            }
            return Math.max(0, score);
        }

        private void applyDifficultyHeuristics() {
            double totalKnown = perDayKnownSets.values().stream().mapToDouble(Double::doubleValue).sum();
            int sessions = Math.max(template.authoredDayCount(),
                    template.daysPerWeek() != null ? template.daysPerWeek() : 0);
            int maxExercisesPerDay = perDayExerciseCount.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            double maxBucket = buckets.values().stream().mapToDouble(b -> b.weeklyWeightedSets).max().orElse(0);

            if (difficultyOrDefault() == Difficulty.BEGINNER) {
                if (totalKnown > 70) {
                    add("BEGINNER_VOLUME_HIGH", Severity.MEDIUM, "High total volume for a beginner template",
                            "Heuristic: " + fmt(totalKnown) + " weekly sets is a lot for a beginner.",
                            List.of(), "Start lower and progress over time.");
                }
                if (maxBucket > 18) {
                    add("BEGINNER_MUSCLE_VOLUME_HIGH", Severity.MEDIUM, "High per-muscle volume for a beginner",
                            "Heuristic: a muscle exceeds 18 weekly sets, which is high for a beginner.",
                            List.of(), "Beginners progress well on less volume.");
                }
                if (sessions > 6) {
                    add("BEGINNER_FREQUENCY_HIGH", Severity.MEDIUM, "Very high training frequency for a beginner",
                            "Heuristic: more than 6 sessions/week is demanding for a beginner.",
                            List.of(), "3–4 days/week is usually plenty to start.");
                }
                if (maxExercisesPerDay > 8) {
                    add("BEGINNER_SESSION_DENSE", Severity.LOW, "Dense sessions for a beginner",
                            "Heuristic: more than 8 exercises in a session is a lot for a beginner.",
                            List.of(), "Fewer, well-chosen exercises are easier to execute.");
                }
                if (strengthExercises > 0 && !anyCompound) {
                    add("BEGINNER_NO_COMPOUNDS", Severity.MEDIUM, "No compound exercises detected",
                            "Heuristic: the template appears isolation-only; beginners benefit from basic compounds.",
                            List.of(), "Add squat/hinge/press/row-pattern compounds.");
                }
            } else if (maxBucket > 25) {
                // already flagged EXCESSIVE; no extra deduction here.
                add("HIGH_VOLUME_ADVANCED_ONLY", Severity.INFO, "Very high per-muscle volume",
                        "Heuristic: some muscles exceed 25 weekly sets — only appropriate for advanced, recovered lifters.",
                        List.of(), "Ensure recovery supports this volume.");
            }
        }

        // --- assembly -----------------------------------------------------------------------

        private void collectStrengths() {
            boolean allMajorsCovered = MAJOR_BUCKETS.stream().allMatch(b -> buckets.get(b).presentExerciseCount > 0);
            if (allMajorsCovered) {
                strengths.add("All major muscle regions (chest, back, shoulders, quads, hamstrings, glutes) are trained.");
            }
            long wellDosed = ALL_BUCKETS.stream()
                    .filter(b -> buckets.get(b).weeklyWeightedSets >= 8 && buckets.get(b).weeklyWeightedSets <= 20)
                    .count();
            if (wellDosed >= 4) {
                strengths.add(wellDosed + " muscle groups fall in a solid 8–20 weekly-set range.");
            }
            Double pullPush = ratio(volume(BACK) + volume(BICEPS), pushVolume());
            if (pullPush != null && pullPush >= 0.8 && pullPush <= 1.3) {
                strengths.add("Push and pull volume are well balanced (pull/push ≈ " + fmt(pullPush) + ").");
            }
            if (anyCompound) {
                strengths.add("The template includes compound movements.");
            }
        }

        private void collectLimitations() {
            limitations.add("Cannot assess effort/proximity to failure: templates do not store RIR/RPE.");
            if (strengthExercisesMissingSets > 0) {
                String note = strengthExercisesMissingSets + " exercise(s) have no planned set count, so they are "
                        + "excluded from volume scoring — volume figures are therefore incomplete.";
                limitations.add(note);
                add("VOLUME_DATA_INCOMPLETE",
                        strengthExercisesMissingSets * 2 > strengthExercises ? Severity.MEDIUM : Severity.INFO,
                        "Volume scoring is incomplete", note, List.of(),
                        "Add planned set counts to every exercise for a complete volume analysis.");
            }
            if (!anyRestData) {
                limitations.add("No planned rest times are set, so rest-interval heuristics were skipped.");
            }
            if (unparsedReps > 0) {
                limitations.add(unparsedReps + " exercise(s) have non-numeric planned reps that could not be analyzed.");
            }
            if (template.difficulty() == null) {
                limitations.add("Template difficulty is unset; difficulty heuristics assume INTERMEDIATE.");
            }
            limitations.add("Rear deltoids are not separable from the shoulders muscle group, so push/pull "
                    + "balance treats all shoulder work as pushing.");
        }

        private void deriveSuggestions() {
            warnings.stream()
                    .filter(w -> w.severity() == Severity.HIGH || w.severity() == Severity.MEDIUM)
                    .map(AnalysisWarning::suggestedFix)
                    .filter(fix -> fix != null && !fix.isBlank())
                    .distinct()
                    .forEach(suggestions::add);
        }

        // --- response pieces ----------------------------------------------------------------

        private List<MuscleVolumeResponse> muscleVolumes() {
            List<MuscleVolumeResponse> list = new ArrayList<>();
            for (String bucket : ALL_BUCKETS) {
                Bucket data = buckets.get(bucket);
                list.add(new MuscleVolumeResponse(bucket, round(data.weeklyWeightedSets), data.hasUnknownSets));
            }
            return list;
        }

        private List<MuscleFrequencyResponse> frequencies() {
            List<MuscleFrequencyResponse> list = new ArrayList<>();
            for (String bucket : ALL_BUCKETS) {
                list.add(new MuscleFrequencyResponse(bucket, buckets.get(bucket).daysTrained.size()));
            }
            return list;
        }

        private BalanceRatiosResponse ratios() {
            return new BalanceRatiosResponse(
                    round(ratio(volume(BACK) + volume(BICEPS), pushVolume())),
                    round(ratio(volume(HAMSTRINGS) + volume(GLUTES), volume(QUADS))),
                    round(ratio(lowerVolume(), upperVolume())));
        }

        private String summary(int score, String category) {
            long majorsMissing = MAJOR_BUCKETS.stream().filter(b -> buckets.get(b).presentExerciseCount == 0).count();
            long highWarnings = warnings.stream().filter(w -> w.severity() == Severity.HIGH).count();
            String base = switch (category) {
                case "WELL_STRUCTURED" -> "Well-structured template covering the major movement patterns.";
                case "DECENT_STRUCTURE" -> "Reasonable template structure with some areas to improve.";
                default -> "This template structure needs review before it is well-rounded.";
            };
            return base + " Structure score " + score + "/100"
                    + (majorsMissing > 0 ? "; " + majorsMissing + " major muscle region(s) untrained" : "")
                    + (highWarnings > 0 ? "; " + highWarnings + " high-severity finding(s)." : ".");
        }

        // --- helpers ------------------------------------------------------------------------

        private void add(String code, Severity severity, String title, String explanation,
                List<String> affected, String fix) {
            warnings.add(new AnalysisWarning(code, severity, title, explanation, affected, fix));
        }

        private int present(String bucket) {
            return buckets.get(bucket).presentExerciseCount;
        }

        private double volume(String bucket) {
            return buckets.get(bucket).weeklyWeightedSets;
        }

        private double pushVolume() {
            return volume(CHEST) + volume(SHOULDERS) + volume(TRICEPS);
        }

        private double lowerVolume() {
            return volume(QUADS) + volume(HAMSTRINGS) + volume(GLUTES) + volume(CALVES);
        }

        private double upperVolume() {
            return volume(CHEST) + volume(BACK) + volume(SHOULDERS) + volume(BICEPS) + volume(TRICEPS) + volume(FOREARMS);
        }

        private boolean coveredByRegionWarning(String bucket, boolean lowerBody, boolean pulling, boolean pushing) {
            return switch (bucket) {
                case BACK -> !pulling;
                case CHEST, SHOULDERS -> !pushing;
                case QUADS, HAMSTRINGS, GLUTES -> !lowerBody;
                default -> false;
            };
        }

        private boolean isUpperFocused() {
            return !template.days().isEmpty() && template.days().stream()
                    .allMatch(d -> d.focus() == DayFocus.UPPER || d.focus() == DayFocus.PUSH || d.focus() == DayFocus.PULL);
        }

        private boolean isLowerFocused() {
            return !template.days().isEmpty() && template.days().stream()
                    .allMatch(d -> d.focus() == DayFocus.LOWER || d.focus() == DayFocus.LEGS);
        }

        private boolean difficultyAtLeastIntermediate() {
            return difficultyOrDefault() != Difficulty.BEGINNER;
        }

        private Difficulty difficultyOrDefault() {
            return difficulty != null ? difficulty : Difficulty.INTERMEDIATE;
        }

        private Double ratio(double numerator, double denominator) {
            return denominator <= 0 ? null : numerator / denominator;
        }

        private Double round(Double value) {
            return value == null ? null : Math.round(value * 100.0) / 100.0;
        }

        private double round(double value) {
            return Math.round(value * 100.0) / 100.0;
        }

        private String fmt(double value) {
            return String.valueOf(round(value));
        }
    }

    private static String category(int score) {
        if (score >= 80) {
            return "WELL_STRUCTURED";
        }
        return score >= 55 ? "DECENT_STRUCTURE" : "NEEDS_REVIEW";
    }

    private static Integer parseReps(String plannedReps) {
        if (plannedReps == null || plannedReps.isBlank()) {
            return null;
        }
        Matcher matcher = INT_PATTERN.matcher(plannedReps);
        List<Integer> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        if (numbers.isEmpty()) {
            return null;
        }
        if (numbers.size() == 1) {
            return numbers.get(0);
        }
        return (numbers.get(0) + numbers.get(1)) / 2; // range midpoint
    }
}

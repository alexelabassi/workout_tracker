package com.thesis.workout.benchmark;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Deterministic generator of synthetic template search documents for the benchmark. Given a seed
 * and a size it always produces the same corpus, so OpenSearch and PostgreSQL are measured over
 * identical data and runs are reproducible. The text is drawn from a realistic lifting vocabulary
 * so full-text / fuzzy queries have meaningful signal (e.g. queries for "bench press", "squat",
 * "chest" match a predictable fraction of the corpus).
 */
final class SyntheticCorpus {

    private static final long SEED = 42L;

    /** Exercise name -> the muscle groups it trains (used to derive each template's muscle set). */
    private static final Map<String, List<String>> EXERCISES = Map.ofEntries(
            Map.entry("Barbell Bench Press", List.of("CHEST", "TRICEPS", "SHOULDERS")),
            Map.entry("Incline Dumbbell Press", List.of("CHEST", "SHOULDERS", "TRICEPS")),
            Map.entry("Cable Fly", List.of("CHEST")),
            Map.entry("Barbell Back Squat", List.of("QUADS", "GLUTES", "HAMSTRINGS")),
            Map.entry("Front Squat", List.of("QUADS", "CORE")),
            Map.entry("Leg Press", List.of("QUADS", "GLUTES")),
            Map.entry("Leg Extension", List.of("QUADS")),
            Map.entry("Romanian Deadlift", List.of("HAMSTRINGS", "GLUTES")),
            Map.entry("Lying Leg Curl", List.of("HAMSTRINGS")),
            Map.entry("Hip Thrust", List.of("GLUTES")),
            Map.entry("Conventional Deadlift", List.of("BACK", "HAMSTRINGS", "GLUTES")),
            Map.entry("Barbell Row", List.of("BACK", "LATS", "BICEPS")),
            Map.entry("Pull-up", List.of("LATS", "BICEPS")),
            Map.entry("Lat Pulldown", List.of("LATS", "BICEPS")),
            Map.entry("Overhead Press", List.of("SHOULDERS", "TRICEPS")),
            Map.entry("Lateral Raise", List.of("SHOULDERS")),
            Map.entry("Bicep Curl", List.of("BICEPS")),
            Map.entry("Tricep Pushdown", List.of("TRICEPS")),
            Map.entry("Standing Calf Raise", List.of("CALVES")),
            Map.entry("Plank", List.of("CORE")));

    private static final List<String> EXERCISE_NAMES = List.copyOf(EXERCISES.keySet());

    private static final String[] ADJECTIVES = {
        "Explosive", "Hypertrophy", "Strength", "Foundational", "Athletic", "Minimalist",
        "High-Volume", "Powerbuilding", "Classic", "Advanced", "Beginner-Friendly", "Elite"
    };
    private static final String[] SPLIT_TYPES = {"FULL_BODY", "UPPER_LOWER", "PPL", "BRO_SPLIT", "CUSTOM"};
    private static final Map<String, String> SPLIT_LABELS = Map.of(
            "FULL_BODY", "Full Body", "UPPER_LOWER", "Upper Lower", "PPL", "Push Pull Legs",
            "BRO_SPLIT", "Bro Split", "CUSTOM", "Custom");
    private static final String[] DIFFICULTIES = {"BEGINNER", "INTERMEDIATE", "ADVANCED"};
    private static final int OWNER_POOL = 200;

    private SyntheticCorpus() {
    }

    /**
     * Streams the corpus in fixed-size batches with a single sequential RNG, so memory stays
     * constant (only one batch is held at a time) and the output is identical to {@link #generate}
     * for the same prefix — which is what lets the benchmark scale to 1M docs.
     */
    static void forEachBatch(int size, int batchSize, Consumer<List<SearchDoc>> consumer) {
        Random rng = new Random(SEED);
        List<String> owners = owners();
        List<SearchDoc> batch = new ArrayList<>(Math.min(batchSize, Math.max(size, 1)));
        for (int i = 0; i < size; i++) {
            batch.add(generateOne(rng, owners, i));
            if (batch.size() >= batchSize) {
                consumer.accept(batch);
                batch = new ArrayList<>(batchSize);
            }
        }
        if (!batch.isEmpty()) {
            consumer.accept(batch);
        }
    }

    /** Materialises the whole corpus (small sizes / convenience). Uses the same sequential RNG. */
    static List<SearchDoc> generate(int size) {
        List<SearchDoc> all = new ArrayList<>(size);
        forEachBatch(size, 10_000, all::addAll);
        return all;
    }

    private static List<String> owners() {
        List<String> owners = new ArrayList<>(OWNER_POOL);
        for (int i = 0; i < OWNER_POOL; i++) {
            owners.add(new UUID(SEED, i).toString());
        }
        return owners;
    }

    private static SearchDoc generateOne(Random rng, List<String> owners, int i) {
        String split = SPLIT_TYPES[rng.nextInt(SPLIT_TYPES.length)];
        String difficulty = DIFFICULTIES[rng.nextInt(DIFFICULTIES.length)];
        int daysPerWeek = 2 + rng.nextInt(5); // 2..6
        String adjective = ADJECTIVES[rng.nextInt(ADJECTIVES.length)];

        int exerciseCount = 4 + rng.nextInt(5); // 4..8
        Set<String> picked = new LinkedHashSet<>();
        while (picked.size() < exerciseCount) {
            picked.add(EXERCISE_NAMES.get(rng.nextInt(EXERCISE_NAMES.size())));
        }
        List<String> exercises = new ArrayList<>(picked);
        Set<String> muscles = new LinkedHashSet<>();
        exercises.forEach(ex -> muscles.addAll(EXERCISES.get(ex)));

        String name = adjective + " " + SPLIT_LABELS.get(split) + " Program " + (i + 1);
        String description = "A " + difficulty.toLowerCase() + " " + SPLIT_LABELS.get(split).toLowerCase()
                + " program training " + String.join(", ", muscles).toLowerCase() + ".";

        int structureScore = 30 + rng.nextInt(71); // 30..100
        String category = structureScore >= 80 ? "WELL_STRUCTURED"
                : structureScore >= 55 ? "DECENT_STRUCTURE" : "NEEDS_REVIEW";

        // Skewed popularity: most templates low, a few very high (cubic of a uniform).
        double s = rng.nextDouble();
        int saves = (int) (s * s * s * 600);
        int uses = (int) (rng.nextDouble() * rng.nextDouble() * 400);

        // 90% public so the marketplace corpus (visibility=PUBLIC) is large.
        String visibility = rng.nextInt(10) == 0 ? "PRIVATE" : "PUBLIC";
        String owner = owners.get(rng.nextInt(owners.size()));

        return new SearchDoc(new UUID(SEED ^ 0x5DEECE66DL, i).toString(), owner, visibility, name,
                description, exercises, new ArrayList<>(muscles), split, difficulty, daysPerWeek,
                saves, uses, structureScore, category);
    }
}

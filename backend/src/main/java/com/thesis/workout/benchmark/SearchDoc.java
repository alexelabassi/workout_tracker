package com.thesis.workout.benchmark;

import java.util.List;

/**
 * One synthetic searchable template used by the OpenSearch-vs-PostgreSQL benchmark. It mirrors the
 * fields of the production {@code templates} document so both engines index an identical corpus and
 * the comparison is apples-to-apples. Benchmark-only (profile {@code benchmark}).
 */
public record SearchDoc(
        String id,
        String ownerUserId,
        String visibility,
        String name,
        String description,
        List<String> exerciseNames,
        List<String> muscleGroups,
        String splitType,
        String difficulty,
        int daysPerWeek,
        int savesCount,
        int usesCount,
        int structureScore,
        String analysisCategory) {

    /** Space-joined exercise names (full-text + trigram source). */
    public String exerciseNamesText() {
        return String.join(" ", exerciseNames);
    }

    /** Space-joined muscle codes (full-text). */
    public String muscleGroupsText() {
        return String.join(" ", muscleGroups);
    }
}

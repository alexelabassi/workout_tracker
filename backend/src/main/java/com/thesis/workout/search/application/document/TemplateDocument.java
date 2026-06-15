package com.thesis.workout.search.application.document;

import java.util.List;

/**
 * Denormalised template document stored in the {@code templates_v1} index. Built from the
 * authoritative PostgreSQL state at index time so a single OpenSearch document carries everything
 * the template/marketplace search needs (full-text fields, structured filter fields, popularity
 * counters and the analyzer-derived structural fields) without any join at query time.
 *
 * <p>Dates are stored as epoch milliseconds (the {@code date} mapping accepts {@code epoch_millis}),
 * which sidesteps Jackson temporal-serialization configuration.</p>
 */
public record TemplateDocument(
        String templateId,
        String ownerUserId,
        String visibility,
        String name,
        String description,
        String splitType,
        String difficulty,
        Integer daysPerWeek,
        Integer estimatedDurationMinutes,
        Long publishedAt,
        Long createdAt,
        String copiedFromTemplateId,
        List<String> exerciseNames,
        List<String> muscleGroups,
        String muscleGroupsText,
        List<String> dayNames,
        List<String> dayFocuses,
        List<String> routineNameSnapshots,
        List<String> routineContentSnapshots,
        Double ratingScore,
        Integer upvotesCount,
        Integer downvotesCount,
        Integer savesCount,
        Integer usesCount,
        Integer templateStructureScore,
        String analysisCategory,
        List<String> warningCodes,
        List<String> missingMajorMuscles) {
}

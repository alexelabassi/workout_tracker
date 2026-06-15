package com.thesis.workout.search.web.dto;

import java.util.List;
import java.util.Map;

/**
 * One workout-history search hit, built from the session's immutable snapshots. Carries the
 * relevance score and per-field highlight fragments. Always owner-scoped — a session is only ever
 * returned to the user who trained it.
 */
public record WorkoutSearchItemResponse(
        String sessionId,
        String status,
        Long startedAt,
        Long finishedAt,
        Long durationSeconds,
        String templateNameSnapshot,
        String templateDayNameSnapshot,
        String gymNameSnapshot,
        List<String> exerciseNameSnapshots,
        List<String> muscleGroups,
        List<String> equipmentNameSnapshots,
        Double totalVolume,
        Integer setCount,
        Integer exerciseCount,
        Double relevanceScore,
        Map<String, List<String>> highlights) {
}

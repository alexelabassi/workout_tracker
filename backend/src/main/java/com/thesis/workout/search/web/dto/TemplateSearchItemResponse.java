package com.thesis.workout.search.web.dto;

import java.util.List;
import java.util.Map;

/**
 * One template search hit. Carries the denormalised display fields, the analyzer-derived structural
 * score/category, the relevance score, and the per-field highlight fragments. For marketplace
 * results the viewer's vote/save state and the author name are merged in from PostgreSQL.
 */
public record TemplateSearchItemResponse(
        String templateId,
        String name,
        String description,
        String visibility,
        String splitType,
        String difficulty,
        Integer daysPerWeek,
        Integer estimatedDurationMinutes,
        List<String> muscleGroups,
        List<String> exerciseNames,
        Double ratingScore,
        Integer savesCount,
        Integer usesCount,
        Integer templateStructureScore,
        String analysisCategory,
        String authorDisplayName,
        String myVote,
        boolean saved,
        Double relevanceScore,
        Map<String, List<String>> highlights) {
}

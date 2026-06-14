package com.thesis.workout.analyzer.web.dto;

import java.util.List;

public record AnalysisWarning(
        String code,
        Severity severity,
        String title,
        String explanation,
        List<String> affectedMuscleGroups,
        String suggestedFix) {
}

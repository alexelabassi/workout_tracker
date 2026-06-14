package com.thesis.workout.analyzer.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * Evidence-informed structural analysis of a workout template. The score is a "Template Structure
 * Score", not a measure of scientific or medical quality. Always carries the disclaimer.
 */
public record AnalysisResponse(
        UUID templateId,
        int overallScore,
        String category,
        String summary,
        SubScoresResponse subScores,
        List<MuscleVolumeResponse> muscleGroupVolumes,
        List<MuscleFrequencyResponse> frequencyByMuscleGroup,
        BalanceRatiosResponse balanceRatios,
        List<AnalysisWarning> warnings,
        List<String> suggestions,
        List<String> strengths,
        List<String> limitations,
        String disclaimer) {
}

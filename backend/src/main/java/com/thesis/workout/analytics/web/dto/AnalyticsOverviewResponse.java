package com.thesis.workout.analytics.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bundled analytics over the user's FINISHED sessions. {@code primaryMuscleSetDistribution} is a
 * count of sets per PRIMARY-role muscle group — a training-emphasis proxy, not a biological
 * stimulus score.
 */
public record AnalyticsOverviewResponse(
        long totalWorkouts,
        BigDecimal totalVolume,
        List<VolumePointResponse> volumeOverTime,
        List<WeeklyWorkoutsResponse> workoutsPerWeek,
        List<MuscleDistributionResponse> primaryMuscleSetDistribution,
        List<BestSetResponse> bestSets,
        List<OneRepMaxSeriesResponse> oneRepMaxOverTime) {
}

package com.thesis.workout.analytics.application;

import com.thesis.workout.analytics.infrastructure.repository.AnalyticsQueryRepository;
import com.thesis.workout.analytics.infrastructure.repository.AnalyticsQueryRepository.BestSetRow;
import com.thesis.workout.analytics.infrastructure.repository.AnalyticsQueryRepository.OneRepMaxPoint;
import com.thesis.workout.analytics.web.dto.AnalyticsOverviewResponse;
import com.thesis.workout.analytics.web.dto.BestSetResponse;
import com.thesis.workout.analytics.web.dto.MuscleDistributionResponse;
import com.thesis.workout.analytics.web.dto.OneRepMaxPointResponse;
import com.thesis.workout.analytics.web.dto.OneRepMaxSeriesResponse;
import com.thesis.workout.analytics.web.dto.VolumePointResponse;
import com.thesis.workout.analytics.web.dto.WeeklyWorkoutsResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the analytics overview from the Postgres-side aggregations. Totals are derived from
 * the already-computed series so no extra round trips are needed.
 */
@Service
public class AnalyticsService {

    private final AnalyticsQueryRepository analyticsQueryRepository;

    public AnalyticsService(AnalyticsQueryRepository analyticsQueryRepository) {
        this.analyticsQueryRepository = analyticsQueryRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse overview(UUID userId) {
        List<VolumePointResponse> volumeOverTime = analyticsQueryRepository.volumeOverTime(userId).stream()
                .map(point -> new VolumePointResponse(point.getDay(), point.getVolume()))
                .toList();

        List<WeeklyWorkoutsResponse> workoutsPerWeek = analyticsQueryRepository.workoutsPerWeek(userId).stream()
                .map(week -> new WeeklyWorkoutsResponse(week.getWeekStart(), week.getWorkouts()))
                .toList();

        List<MuscleDistributionResponse> muscleDistribution =
                analyticsQueryRepository.primaryMuscleSetDistribution(userId).stream()
                        .map(muscle -> new MuscleDistributionResponse(muscle.getCode(), muscle.getSetCount()))
                        .toList();

        List<BestSetResponse> bestSets = analyticsQueryRepository.bestSetsPerExercise(userId).stream()
                .sorted(Comparator.comparing(BestSetRow::getEstimatedOneRepMax).reversed())
                .map(row -> new BestSetResponse(
                        row.getExerciseName(), row.getWeight(), row.getReps(),
                        row.getEstimatedOneRepMax(), row.getSessionId(), row.getPerformedAt()))
                .toList();

        List<OneRepMaxSeriesResponse> oneRepMaxOverTime = groupOneRepMax(
                analyticsQueryRepository.oneRepMaxOverTime(userId));

        long totalWorkouts = workoutsPerWeek.stream().mapToLong(WeeklyWorkoutsResponse::workouts).sum();
        BigDecimal totalVolume = volumeOverTime.stream()
                .map(VolumePointResponse::volume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AnalyticsOverviewResponse(totalWorkouts, totalVolume, volumeOverTime, workoutsPerWeek,
                muscleDistribution, bestSets, oneRepMaxOverTime);
    }

    /** Groups the flat (day, exercise, 1RM) rows into per-exercise, date-ordered progression series. */
    private static List<OneRepMaxSeriesResponse> groupOneRepMax(List<OneRepMaxPoint> rows) {
        Map<String, List<OneRepMaxPointResponse>> byExercise = new LinkedHashMap<>();
        for (OneRepMaxPoint row : rows) {
            byExercise.computeIfAbsent(row.getExerciseName(), key -> new ArrayList<>())
                    .add(new OneRepMaxPointResponse(row.getDay(), row.getEstimatedOneRepMax()));
        }
        return byExercise.entrySet().stream()
                .map(entry -> new OneRepMaxSeriesResponse(entry.getKey(), entry.getValue()))
                .toList();
    }
}

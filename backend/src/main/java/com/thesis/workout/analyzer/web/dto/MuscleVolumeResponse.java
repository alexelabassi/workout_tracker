package com.thesis.workout.analyzer.web.dto;

public record MuscleVolumeResponse(String muscleGroup, double weeklyWeightedSets, boolean volumeDataIncomplete) {
}

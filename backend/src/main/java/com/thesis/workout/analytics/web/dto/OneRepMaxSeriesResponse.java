package com.thesis.workout.analytics.web.dto;

import java.util.List;

/** Estimated-1RM progression for a single exercise, chronological. */
public record OneRepMaxSeriesResponse(String exerciseName, List<OneRepMaxPointResponse> points) {
}

package com.thesis.workout.analyzer.web.dto;

/** Structural balance ratios; null when the denominator region has no counted volume. */
public record BalanceRatiosResponse(Double pullToPush, Double posteriorToQuads, Double lowerToUpper) {
}

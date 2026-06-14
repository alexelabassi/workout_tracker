package com.thesis.workout.analyzer.web.dto;

public record SubScoresResponse(
        int volumeCoverage,
        int frequency,
        int balance,
        int sessionDesign,
        int specificityRest) {
}

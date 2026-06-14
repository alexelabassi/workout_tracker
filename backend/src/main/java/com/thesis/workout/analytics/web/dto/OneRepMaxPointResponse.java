package com.thesis.workout.analytics.web.dto;

import java.math.BigDecimal;

public record OneRepMaxPointResponse(String date, BigDecimal estimatedOneRepMax) {
}

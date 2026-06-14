package com.thesis.workout.history.web.dto;

import com.thesis.workout.session.domain.model.SessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HistoryItemResponse(
        UUID sessionId,
        SessionStatus status,
        Instant startedAt,
        Instant finishedAt,
        String templateName,
        String templateDayName,
        String gymName,
        long exerciseCount,
        long setCount,
        BigDecimal totalVolume,
        Long durationSeconds) {
}

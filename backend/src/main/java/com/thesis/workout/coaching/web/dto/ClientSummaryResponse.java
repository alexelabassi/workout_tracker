package com.thesis.workout.coaching.web.dto;

import java.time.Instant;
import java.util.UUID;

/** A coach's view of one active client. */
public record ClientSummaryResponse(
        UUID clientId,
        String displayName,
        String email,
        Instant activeSince) {
}

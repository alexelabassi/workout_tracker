package com.thesis.workout.marketplace.web.dto;

import com.thesis.workout.marketplace.domain.model.VoteType;
import java.util.UUID;

/** A marketplace browse card: public template info + stats + the current user's vote/save state. */
public record MarketplaceTemplateSummaryResponse(
        UUID id,
        String name,
        String description,
        String splitType,
        String difficulty,
        Integer daysPerWeek,
        Integer estimatedDurationMinutes,
        String publishedAt,
        String authorDisplayName,
        MarketplaceStatsResponse stats,
        VoteType myVote,
        boolean saved) {
}

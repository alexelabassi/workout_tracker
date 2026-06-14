package com.thesis.workout.marketplace.web.dto;

import com.thesis.workout.marketplace.domain.model.VoteType;

/** Returned after a vote/save action so the client can refresh that template's card precisely. */
public record MarketplaceInteractionResponse(MarketplaceStatsResponse stats, VoteType myVote, boolean saved) {
}

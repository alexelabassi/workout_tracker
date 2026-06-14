package com.thesis.workout.marketplace.web.dto;

import com.thesis.workout.marketplace.domain.model.VoteType;
import com.thesis.workout.template.web.dto.TemplateDetailResponse;

/** Read-only public template detail: the full day tree plus marketplace stats and viewer state. */
public record MarketplaceTemplateDetailResponse(
        TemplateDetailResponse template,
        String authorDisplayName,
        MarketplaceStatsResponse stats,
        VoteType myVote,
        boolean saved) {
}

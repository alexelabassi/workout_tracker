package com.thesis.workout.marketplace.web.dto;

import java.math.BigDecimal;

public record MarketplaceStatsResponse(
        long upvotes,
        long downvotes,
        long saves,
        long uses,
        BigDecimal ratingScore) {
}

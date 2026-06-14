package com.thesis.workout.marketplace.web.dto;

import com.thesis.workout.marketplace.domain.model.VoteType;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull VoteType voteType) {
}

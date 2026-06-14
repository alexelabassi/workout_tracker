package com.thesis.workout.marketplace.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** A user's UP/DOWN vote on a public template. Maps the existing template_votes table. */
@Entity
@Table(name = "template_votes")
public class TemplateVote {

    @EmbeddedId
    private TemplateVoteId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 30)
    private VoteType voteType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TemplateVote() {
    }

    public static TemplateVote of(UUID userId, UUID templateId, VoteType voteType) {
        TemplateVote vote = new TemplateVote();
        vote.id = new TemplateVoteId(userId, templateId);
        vote.voteType = voteType;
        return vote;
    }

    public void changeType(VoteType voteType) {
        this.voteType = voteType;
    }

    public TemplateVoteId getId() {
        return id;
    }

    public VoteType getVoteType() {
        return voteType;
    }
}

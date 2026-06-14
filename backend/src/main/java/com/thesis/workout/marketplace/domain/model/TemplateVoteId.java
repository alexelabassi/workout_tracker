package com.thesis.workout.marketplace.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TemplateVoteId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    protected TemplateVoteId() {
    }

    public TemplateVoteId(UUID userId, UUID templateId) {
        this.userId = userId;
        this.templateId = templateId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TemplateVoteId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(templateId, that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, templateId);
    }
}

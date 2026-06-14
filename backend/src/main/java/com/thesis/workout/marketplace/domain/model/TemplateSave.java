package com.thesis.workout.marketplace.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** A user's bookmark of a public template. Maps the existing template_saves table. */
@Entity
@Table(name = "template_saves")
public class TemplateSave {

    @EmbeddedId
    private TemplateSaveId id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TemplateSave() {
    }

    public static TemplateSave of(UUID userId, UUID templateId) {
        TemplateSave save = new TemplateSave();
        save.id = new TemplateSaveId(userId, templateId);
        return save;
    }

    public TemplateSaveId getId() {
        return id;
    }
}

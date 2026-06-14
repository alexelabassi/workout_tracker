package com.thesis.workout.marketplace.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Records that a user copied a public template into their own library. Name snapshots preserve
 * the event even if either template is later deleted. Maps the existing template_use_events table.
 */
@Entity
@Table(name = "template_use_events")
public class TemplateUseEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_template_id")
    private UUID sourceTemplateId;

    @Column(name = "copied_template_id")
    private UUID copiedTemplateId;

    @Column(name = "source_template_name_snapshot", nullable = false, length = 180)
    private String sourceTemplateNameSnapshot;

    @Column(name = "copied_template_name_snapshot", nullable = false, length = 180)
    private String copiedTemplateNameSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TemplateUseEvent() {
    }

    public static TemplateUseEvent record(UUID userId, UUID sourceTemplateId, UUID copiedTemplateId,
            String sourceTemplateNameSnapshot, String copiedTemplateNameSnapshot) {
        TemplateUseEvent event = new TemplateUseEvent();
        event.id = UUID.randomUUID();
        event.userId = userId;
        event.sourceTemplateId = sourceTemplateId;
        event.copiedTemplateId = copiedTemplateId;
        event.sourceTemplateNameSnapshot = sourceTemplateNameSnapshot;
        event.copiedTemplateNameSnapshot = copiedTemplateNameSnapshot;
        return event;
    }

    public UUID getId() {
        return id;
    }
}

package com.thesis.workout.marketplace.infrastructure.repository;

import com.thesis.workout.marketplace.domain.model.TemplateUseEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateUseEventRepository extends JpaRepository<TemplateUseEvent, UUID> {
}

package com.thesis.workout.template.infrastructure.repository;

import com.thesis.workout.template.domain.model.TemplateDay;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateDayRepository extends JpaRepository<TemplateDay, UUID> {

    List<TemplateDay> findByTemplateIdOrderByDayNumberAsc(UUID templateId);

    Optional<TemplateDay> findByTemplateIdAndDayNumber(UUID templateId, int dayNumber);

    long countByTemplateId(UUID templateId);
}

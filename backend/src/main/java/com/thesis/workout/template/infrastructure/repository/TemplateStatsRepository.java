package com.thesis.workout.template.infrastructure.repository;

import com.thesis.workout.template.domain.model.TemplateStats;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateStatsRepository extends JpaRepository<TemplateStats, UUID> {
}

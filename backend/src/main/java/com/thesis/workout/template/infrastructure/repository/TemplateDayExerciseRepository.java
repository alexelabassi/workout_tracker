package com.thesis.workout.template.infrastructure.repository;

import com.thesis.workout.template.domain.model.TemplateDayExercise;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateDayExerciseRepository extends JpaRepository<TemplateDayExercise, UUID> {

    List<TemplateDayExercise> findByTemplateDayIdOrderByPositionAsc(UUID templateDayId);

    List<TemplateDayExercise> findByTemplateDayIdInOrderByPositionAsc(List<UUID> templateDayIds);

    Optional<TemplateDayExercise> findFirstByTemplateDayIdOrderByPositionDesc(UUID templateDayId);
}

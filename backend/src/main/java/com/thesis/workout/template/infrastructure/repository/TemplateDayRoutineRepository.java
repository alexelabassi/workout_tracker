package com.thesis.workout.template.infrastructure.repository;

import com.thesis.workout.routine.domain.model.RoutineType;
import com.thesis.workout.template.domain.model.TemplateDayRoutine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateDayRoutineRepository extends JpaRepository<TemplateDayRoutine, UUID> {

    List<TemplateDayRoutine> findByTemplateDayIdOrderByRoutineTypeAscPositionAsc(UUID templateDayId);

    List<TemplateDayRoutine> findByTemplateDayIdInOrderByRoutineTypeAscPositionAsc(List<UUID> templateDayIds);

    Optional<TemplateDayRoutine> findFirstByTemplateDayIdAndRoutineTypeOrderByPositionDesc(
            UUID templateDayId, RoutineType routineType);
}

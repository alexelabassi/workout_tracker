package com.thesis.workout.session.infrastructure.repository;

import com.thesis.workout.session.domain.model.SessionRoutine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRoutineRepository extends JpaRepository<SessionRoutine, UUID> {

    List<SessionRoutine> findBySessionIdOrderByRoutineTypeAscPositionAsc(UUID sessionId);
}

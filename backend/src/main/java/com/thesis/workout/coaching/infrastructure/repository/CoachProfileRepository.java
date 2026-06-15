package com.thesis.workout.coaching.infrastructure.repository;

import com.thesis.workout.coaching.domain.model.CoachProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachProfileRepository extends JpaRepository<CoachProfile, UUID> {
}

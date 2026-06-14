package com.thesis.workout.gym.infrastructure.repository;

import com.thesis.workout.gym.domain.model.Gym;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymRepository extends JpaRepository<Gym, UUID> {

    List<Gym> findByUserIdAndDeletedAtIsNullOrderByNameAsc(UUID userId);

    Optional<Gym> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Gym> findByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name);
}

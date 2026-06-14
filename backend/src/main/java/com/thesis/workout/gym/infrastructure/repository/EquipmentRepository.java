package com.thesis.workout.gym.infrastructure.repository;

import com.thesis.workout.gym.domain.model.Equipment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    List<Equipment> findByGymIdAndDeletedAtIsNullOrderByNameAsc(UUID gymId);

    Optional<Equipment> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Equipment> findByGymIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID gymId, String name);

    /**
     * Soft-deletes every active piece of equipment in a gym in one statement, used when the gym
     * itself is deleted. Sets {@code updatedAt} explicitly because a bulk update bypasses the
     * {@code @UpdateTimestamp} generator.
     */
    @Modifying
    @Query("update Equipment e set e.deletedAt = :now, e.updatedAt = :now "
            + "where e.gymId = :gymId and e.deletedAt is null")
    int softDeleteActiveByGym(@Param("gymId") UUID gymId, @Param("now") Instant now);
}

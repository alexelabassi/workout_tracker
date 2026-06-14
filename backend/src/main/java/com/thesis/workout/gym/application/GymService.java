package com.thesis.workout.gym.application;

import com.thesis.workout.gym.application.exception.GymNameTakenException;
import com.thesis.workout.gym.application.exception.GymNotFoundException;
import com.thesis.workout.gym.domain.model.Gym;
import com.thesis.workout.gym.infrastructure.repository.EquipmentRepository;
import com.thesis.workout.gym.infrastructure.repository.GymRepository;
import com.thesis.workout.gym.web.dto.GymResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gym use cases. Every read/write loads the target by {@code (id, userId)} so a request for
 * another user's gym is indistinguishable from a missing one (404, no IDOR signal). Deleting a
 * gym soft-deletes it and cascades the soft delete to its active equipment in the same
 * transaction. Name uniqueness is checked up front and defended by the DB's partial unique index.
 */
@Service
public class GymService {

    private final GymRepository gymRepository;
    private final EquipmentRepository equipmentRepository;

    public GymService(GymRepository gymRepository, EquipmentRepository equipmentRepository) {
        this.gymRepository = gymRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional(readOnly = true)
    public List<GymResponse> list(UUID userId) {
        return gymRepository.findByUserIdAndDeletedAtIsNullOrderByNameAsc(userId).stream()
                .map(GymResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GymResponse get(UUID userId, UUID gymId) {
        Gym gym = gymRepository.findByIdAndUserIdAndDeletedAtIsNull(gymId, userId)
                .orElseThrow(GymNotFoundException::new);
        return GymResponse.from(gym);
    }

    @Transactional
    public GymResponse create(UUID userId, GymCommand command) {
        String name = command.name().trim();
        ensureNameAvailable(userId, name, null);

        Gym gym = Gym.createFor(userId, name, normalizeLocation(command.location()));
        return GymResponse.from(save(gym));
    }

    @Transactional
    public GymResponse update(UUID userId, UUID gymId, GymCommand command) {
        Gym gym = gymRepository.findByIdAndUserIdAndDeletedAtIsNull(gymId, userId)
                .orElseThrow(GymNotFoundException::new);
        String name = command.name().trim();
        ensureNameAvailable(userId, name, gymId);

        gym.updateDetails(name, normalizeLocation(command.location()));
        return GymResponse.from(save(gym));
    }

    @Transactional
    public void delete(UUID userId, UUID gymId) {
        Gym gym = gymRepository.findByIdAndUserIdAndDeletedAtIsNull(gymId, userId)
                .orElseThrow(GymNotFoundException::new);
        Instant now = Instant.now();
        gym.softDelete(now);
        equipmentRepository.softDeleteActiveByGym(gymId, now);
    }

    private Gym save(Gym gym) {
        try {
            return gymRepository.saveAndFlush(gym);
        } catch (DataIntegrityViolationException ex) {
            throw new GymNameTakenException();
        }
    }

    private void ensureNameAvailable(UUID userId, String name, UUID excludeId) {
        gymRepository.findByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(userId, name)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new GymNameTakenException();
                });
    }

    private static String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        return location.trim();
    }
}

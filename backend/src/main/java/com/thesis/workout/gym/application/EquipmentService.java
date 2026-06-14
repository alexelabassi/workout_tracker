package com.thesis.workout.gym.application;

import com.thesis.workout.gym.application.exception.EquipmentNameTakenException;
import com.thesis.workout.gym.application.exception.EquipmentNotFoundException;
import com.thesis.workout.gym.application.exception.GymNotFoundException;
import com.thesis.workout.gym.domain.model.Equipment;
import com.thesis.workout.gym.infrastructure.repository.EquipmentRepository;
import com.thesis.workout.gym.infrastructure.repository.GymRepository;
import com.thesis.workout.gym.web.dto.EquipmentResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Equipment use cases. Create/list are gym-scoped and first verify the gym is owned by the user
 * and active (404 otherwise — no IDOR signal, and you can't add equipment to a foreign gym).
 * Update/delete load the equipment owner-scoped by {@code userId}, then re-verify the parent gym
 * is still owned and active as defence in depth on top of the gym-delete cascade.
 */
@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final GymRepository gymRepository;

    public EquipmentService(EquipmentRepository equipmentRepository, GymRepository gymRepository) {
        this.equipmentRepository = equipmentRepository;
        this.gymRepository = gymRepository;
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> list(UUID userId, UUID gymId) {
        requireOwnedGym(userId, gymId);
        return equipmentRepository.findByGymIdAndDeletedAtIsNullOrderByNameAsc(gymId).stream()
                .map(EquipmentResponse::from)
                .toList();
    }

    @Transactional
    public EquipmentResponse create(UUID userId, UUID gymId, EquipmentCommand command) {
        requireOwnedGym(userId, gymId);
        String name = command.name().trim();
        ensureNameAvailable(gymId, name, null);

        Equipment equipment = Equipment.createFor(
                userId, gymId, name, command.equipmentType(), normalizeNotes(command.notes()));
        return EquipmentResponse.from(save(equipment));
    }

    @Transactional
    public EquipmentResponse update(UUID userId, UUID equipmentId, EquipmentCommand command) {
        Equipment equipment = requireOwnedEquipment(userId, equipmentId);
        String name = command.name().trim();
        ensureNameAvailable(equipment.getGymId(), name, equipmentId);

        equipment.updateDetails(name, command.equipmentType(), normalizeNotes(command.notes()));
        return EquipmentResponse.from(save(equipment));
    }

    @Transactional
    public void delete(UUID userId, UUID equipmentId) {
        Equipment equipment = requireOwnedEquipment(userId, equipmentId);
        equipment.softDelete(Instant.now());
    }

    /** Loads equipment owner-scoped and confirms its parent gym is still owned and active. */
    private Equipment requireOwnedEquipment(UUID userId, UUID equipmentId) {
        Equipment equipment = equipmentRepository.findByIdAndUserIdAndDeletedAtIsNull(equipmentId, userId)
                .orElseThrow(EquipmentNotFoundException::new);
        gymRepository.findByIdAndUserIdAndDeletedAtIsNull(equipment.getGymId(), userId)
                .orElseThrow(EquipmentNotFoundException::new);
        return equipment;
    }

    private void requireOwnedGym(UUID userId, UUID gymId) {
        gymRepository.findByIdAndUserIdAndDeletedAtIsNull(gymId, userId)
                .orElseThrow(GymNotFoundException::new);
    }

    private Equipment save(Equipment equipment) {
        try {
            return equipmentRepository.saveAndFlush(equipment);
        } catch (DataIntegrityViolationException ex) {
            throw new EquipmentNameTakenException();
        }
    }

    private void ensureNameAvailable(UUID gymId, String name, UUID excludeId) {
        equipmentRepository.findByGymIdAndNameIgnoreCaseAndDeletedAtIsNull(gymId, name)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new EquipmentNameTakenException();
                });
    }

    private static String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.trim();
    }
}

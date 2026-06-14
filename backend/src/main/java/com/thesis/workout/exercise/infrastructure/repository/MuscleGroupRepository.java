package com.thesis.workout.exercise.infrastructure.repository;

import com.thesis.workout.exercise.domain.model.MuscleGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MuscleGroupRepository extends JpaRepository<MuscleGroup, String> {

    List<MuscleGroup> findAllByOrderByDisplayNameAsc();
}

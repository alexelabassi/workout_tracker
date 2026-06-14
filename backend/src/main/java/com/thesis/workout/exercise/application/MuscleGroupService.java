package com.thesis.workout.exercise.application;

import com.thesis.workout.exercise.infrastructure.repository.MuscleGroupRepository;
import com.thesis.workout.exercise.web.dto.MuscleGroupResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MuscleGroupService {

    private final MuscleGroupRepository muscleGroupRepository;

    public MuscleGroupService(MuscleGroupRepository muscleGroupRepository) {
        this.muscleGroupRepository = muscleGroupRepository;
    }

    @Transactional(readOnly = true)
    public List<MuscleGroupResponse> list() {
        return muscleGroupRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(MuscleGroupResponse::from)
                .toList();
    }
}

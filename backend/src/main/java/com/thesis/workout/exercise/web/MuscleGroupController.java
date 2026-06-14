package com.thesis.workout.exercise.web;

import com.thesis.workout.exercise.application.MuscleGroupService;
import com.thesis.workout.exercise.web.dto.MuscleGroupResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/muscle-groups")
public class MuscleGroupController {

    private final MuscleGroupService muscleGroupService;

    public MuscleGroupController(MuscleGroupService muscleGroupService) {
        this.muscleGroupService = muscleGroupService;
    }

    @GetMapping
    public List<MuscleGroupResponse> list() {
        return muscleGroupService.list();
    }
}

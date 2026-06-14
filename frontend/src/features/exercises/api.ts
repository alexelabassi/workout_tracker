import { apiDelete, apiGet, apiPost, apiPut } from "../../shared/api/client";
import type { CustomExercisePayload, Exercise, MuscleGroup } from "./types";

export function fetchVisibleExercises(): Promise<Exercise[]> {
  return apiGet<Exercise[]>("/exercises");
}

export function fetchOfficialExercises(): Promise<Exercise[]> {
  return apiGet<Exercise[]>("/exercises/official");
}

export function fetchCustomExercises(): Promise<Exercise[]> {
  return apiGet<Exercise[]>("/exercises/custom");
}

export function fetchMuscleGroups(): Promise<MuscleGroup[]> {
  return apiGet<MuscleGroup[]>("/muscle-groups");
}

export function createCustomExercise(payload: CustomExercisePayload): Promise<Exercise> {
  return apiPost<Exercise>("/exercises/custom", payload);
}

export function updateCustomExercise(id: string, payload: CustomExercisePayload): Promise<Exercise> {
  return apiPut<Exercise>(`/exercises/custom/${id}`, payload);
}

export function deleteCustomExercise(id: string): Promise<void> {
  return apiDelete<void>(`/exercises/custom/${id}`);
}

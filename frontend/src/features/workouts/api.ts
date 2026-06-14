import { apiDelete, apiGet, apiPost, apiPut } from "../../shared/api/client";
import type { SessionExerciseView, SetPayload, WorkoutDetail, WorkoutSetView } from "./types";

export async function fetchActiveWorkout(): Promise<WorkoutDetail | null> {
  // The backend returns 204 (no body) when there is no active workout; the client yields undefined.
  const data = await apiGet<WorkoutDetail | undefined>("/workouts/active");
  return data ?? null;
}

export function fetchWorkout(sessionId: string): Promise<WorkoutDetail> {
  return apiGet<WorkoutDetail>(`/workouts/${sessionId}`);
}

export function startWorkout(templateDayId: string, gymId: string): Promise<WorkoutDetail> {
  return apiPost<WorkoutDetail>("/workouts/start", { templateDayId, gymId });
}

export function finishWorkout(sessionId: string): Promise<WorkoutDetail> {
  return apiPost<WorkoutDetail>(`/workouts/${sessionId}/finish`);
}

export function cancelWorkout(sessionId: string): Promise<WorkoutDetail> {
  return apiPost<WorkoutDetail>(`/workouts/${sessionId}/cancel`);
}

export function addExtraExercise(sessionId: string, exerciseId: string): Promise<SessionExerciseView> {
  return apiPost<SessionExerciseView>(`/workouts/${sessionId}/extra-exercises`, { exerciseId });
}

export function addSet(sessionExerciseId: string, payload: SetPayload): Promise<WorkoutSetView> {
  return apiPost<WorkoutSetView>(`/session-exercises/${sessionExerciseId}/sets`, payload);
}

export function updateSet(setId: string, payload: SetPayload): Promise<WorkoutSetView> {
  return apiPut<WorkoutSetView>(`/sets/${setId}`, payload);
}

export function deleteSet(setId: string): Promise<void> {
  return apiDelete<void>(`/sets/${setId}`);
}

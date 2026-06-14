import { apiDelete, apiGet, apiPost, apiPut } from "../../shared/api/client";
import type { Routine, RoutinePayload } from "./types";

export function fetchRoutines(): Promise<Routine[]> {
  return apiGet<Routine[]>("/routines");
}

export function createRoutine(payload: RoutinePayload): Promise<Routine> {
  return apiPost<Routine>("/routines", payload);
}

export function updateRoutine(id: string, payload: RoutinePayload): Promise<Routine> {
  return apiPut<Routine>(`/routines/${id}`, payload);
}

export function deleteRoutine(id: string): Promise<void> {
  return apiDelete<void>(`/routines/${id}`);
}

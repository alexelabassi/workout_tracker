import type { MuscleRole } from "../exercises/types";
import type { RoutineType } from "../routines/types";

export type SessionStatus = "IN_PROGRESS" | "FINISHED" | "CANCELLED";
export type SetType = "WARMUP" | "WORKING" | "DROP" | "FAILURE";

export const SET_TYPES: SetType[] = ["WARMUP", "WORKING", "DROP", "FAILURE"];

export interface WorkoutSetView {
  id: string;
  setNumber: number;
  setType: SetType;
  weight: number | null;
  reps: number | null;
  durationSeconds: number | null;
  distanceMeters: number | null;
  rpe: number | null;
  note: string | null;
  equipmentId: string | null;
  equipmentName: string | null;
  completedAt: string | null;
}

export interface SessionMuscleGroup {
  code: string;
  role: MuscleRole;
}

export interface SessionExerciseView {
  id: string;
  exerciseName: string;
  exerciseType: string;
  position: number;
  extraExercise: boolean;
  plannedSets: number | null;
  plannedReps: string | null;
  plannedWeight: number | null;
  restSeconds: number | null;
  templateNote: string | null;
  muscleGroups: SessionMuscleGroup[];
  sets: WorkoutSetView[];
}

export interface SessionRoutineView {
  id: string;
  routineType: RoutineType;
  routineName: string;
  routineContent: string;
  position: number;
}

export interface WorkoutDetail {
  id: string;
  status: SessionStatus;
  templateId: string | null;
  templateName: string | null;
  templateDayId: string | null;
  templateDayName: string | null;
  gymId: string | null;
  gymName: string | null;
  startedAt: string;
  finishedAt: string | null;
  notes: string | null;
  routines: SessionRoutineView[];
  exercises: SessionExerciseView[];
}

export interface SetPayload {
  setType: SetType;
  weight: number | null;
  reps: number | null;
  durationSeconds: number | null;
  distanceMeters: number | null;
  rpe: number | null;
  note: string | null;
  equipmentId: string | null;
}

import type { MuscleRole } from "../exercises/types";

export type SplitType = "FULL_BODY" | "UPPER_LOWER" | "PPL" | "BRO_SPLIT" | "CUSTOM";
export type Difficulty = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export type DayFocus = "UPPER" | "LOWER" | "PUSH" | "PULL" | "LEGS" | "FULL_BODY" | "CUSTOM";
export type RoutineType = "START" | "END";

export const SPLIT_TYPES: SplitType[] = ["FULL_BODY", "UPPER_LOWER", "PPL", "BRO_SPLIT", "CUSTOM"];
export const DIFFICULTIES: Difficulty[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];
export const DAY_FOCUSES: DayFocus[] = ["UPPER", "LOWER", "PUSH", "PULL", "LEGS", "FULL_BODY", "CUSTOM"];

export interface TemplateSummary {
  id: string;
  name: string;
  description: string | null;
  splitType: SplitType | null;
  daysPerWeek: number | null;
  difficulty: Difficulty | null;
  estimatedDurationMinutes: number | null;
  visibility: "PRIVATE" | "PUBLIC";
  dayCount: number;
  updatedAt: string;
}

export interface TemplateMuscleGroupSnapshot {
  code: string;
  role: MuscleRole;
}

export interface TemplateDayExercise {
  id: string;
  exerciseId: string | null;
  exerciseName: string;
  exerciseType: string;
  position: number;
  plannedSets: number | null;
  plannedReps: string | null;
  plannedWeight: number | null;
  restSeconds: number | null;
  note: string | null;
  muscleGroups: TemplateMuscleGroupSnapshot[];
}

export interface TemplateDayRoutine {
  id: string;
  routineId: string | null;
  routineType: RoutineType;
  routineName: string;
  routineContent: string;
  position: number;
}

export interface TemplateDay {
  id: string;
  dayNumber: number;
  name: string;
  focus: DayFocus | null;
  estimatedDurationMinutes: number | null;
  notes: string | null;
  exercises: TemplateDayExercise[];
  routines: TemplateDayRoutine[];
}

export interface TemplateDetail {
  id: string;
  name: string;
  description: string | null;
  splitType: SplitType | null;
  daysPerWeek: number | null;
  difficulty: Difficulty | null;
  estimatedDurationMinutes: number | null;
  visibility: "PRIVATE" | "PUBLIC";
  updatedAt: string;
  muscleGroups: string[];
  days: TemplateDay[];
}

export interface TemplatePayload {
  name: string;
  description: string | null;
  splitType: SplitType | null;
  daysPerWeek: number | null;
  difficulty: Difficulty | null;
  estimatedDurationMinutes: number | null;
}

export interface DayPayload {
  dayNumber: number;
  name: string;
  focus: DayFocus | null;
  estimatedDurationMinutes: number | null;
  notes: string | null;
}

export interface ExercisePayload {
  exerciseId: string;
  plannedSets: number | null;
  plannedReps: string | null;
  plannedWeight: number | null;
  restSeconds: number | null;
  note: string | null;
}

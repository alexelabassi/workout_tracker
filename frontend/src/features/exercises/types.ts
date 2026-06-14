export type ExerciseType = "STRENGTH" | "CARDIO" | "MOBILITY" | "OTHER";
export type Visibility = "OFFICIAL" | "CUSTOM";
export type MuscleRole = "PRIMARY" | "SECONDARY";

export interface MuscleGroup {
  code: string;
  displayName: string;
}

export interface ExerciseMuscleGroup {
  code: string;
  displayName: string;
  role: MuscleRole;
}

export interface Exercise {
  id: string;
  name: string;
  description: string | null;
  exerciseType: ExerciseType;
  visibility: Visibility;
  muscleGroups: ExerciseMuscleGroup[];
}

export interface MuscleGroupAssignment {
  code: string;
  role: MuscleRole;
}

export interface CustomExercisePayload {
  name: string;
  description: string | null;
  exerciseType: ExerciseType;
  muscleGroups: MuscleGroupAssignment[];
}

export const EXERCISE_TYPES: ExerciseType[] = ["STRENGTH", "CARDIO", "MOBILITY", "OTHER"];

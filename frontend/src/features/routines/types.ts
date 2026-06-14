export type RoutineType = "START" | "END";

export interface Routine {
  id: string;
  name: string;
  routineType: RoutineType;
  content: string;
  updatedAt: string;
}

export interface RoutinePayload {
  name: string;
  routineType: RoutineType;
  content: string;
}

export const ROUTINE_TYPES: RoutineType[] = ["START", "END"];

export const ROUTINE_TYPE_LABELS: Record<RoutineType, string> = {
  START: "Warm-up",
  END: "Cool-down",
};

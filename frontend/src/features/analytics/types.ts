export interface VolumePoint {
  date: string;
  volume: number;
}

export interface WeeklyWorkouts {
  weekStart: string;
  workouts: number;
}

export interface MuscleDistribution {
  code: string;
  setCount: number;
}

export interface BestSet {
  exerciseName: string;
  weight: number;
  reps: number;
  estimatedOneRepMax: number;
  sessionId: string;
  performedAt: string | null;
}

export interface OneRepMaxPoint {
  date: string;
  estimatedOneRepMax: number;
}

export interface OneRepMaxSeries {
  exerciseName: string;
  points: OneRepMaxPoint[];
}

export interface AnalyticsOverview {
  totalWorkouts: number;
  totalVolume: number;
  volumeOverTime: VolumePoint[];
  workoutsPerWeek: WeeklyWorkouts[];
  primaryMuscleSetDistribution: MuscleDistribution[];
  bestSets: BestSet[];
  oneRepMaxOverTime: OneRepMaxSeries[];
}

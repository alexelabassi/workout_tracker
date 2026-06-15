export interface FacetBucket {
  key: string;
  count: number;
}

export interface SearchFacet {
  field: string;
  buckets: FacetBucket[];
}

export interface SearchResults<T> {
  items: T[];
  facets: SearchFacet[];
  page: number;
  size: number;
  totalHits: number;
}

export type TemplateScope = "my" | "marketplace";

export interface TemplateSearchItem {
  templateId: string;
  name: string;
  description: string | null;
  visibility: "PRIVATE" | "PUBLIC";
  splitType: string | null;
  difficulty: string | null;
  daysPerWeek: number | null;
  estimatedDurationMinutes: number | null;
  muscleGroups: string[];
  exerciseNames: string[];
  ratingScore: number | null;
  savesCount: number | null;
  usesCount: number | null;
  templateStructureScore: number | null;
  analysisCategory: string | null;
  authorDisplayName: string | null;
  myVote: "UP" | "DOWN" | null;
  saved: boolean;
  relevanceScore: number | null;
  highlights: Record<string, string[]>;
}

export interface WorkoutSearchItem {
  sessionId: string;
  status: string;
  startedAt: number | null;
  finishedAt: number | null;
  durationSeconds: number | null;
  templateNameSnapshot: string | null;
  templateDayNameSnapshot: string | null;
  gymNameSnapshot: string | null;
  exerciseNameSnapshots: string[];
  muscleGroups: string[];
  equipmentNameSnapshots: string[];
  totalVolume: number | null;
  setCount: number | null;
  exerciseCount: number | null;
  relevanceScore: number | null;
  highlights: Record<string, string[]>;
}

export interface TemplateSearchParams {
  scope: TemplateScope;
  q?: string;
  difficulty?: string;
  splitType?: string;
  daysPerWeek?: number;
  muscleGroup?: string;
  analysisCategory?: string;
  minScore?: number;
  page?: number;
  size?: number;
}

export interface WorkoutSearchParams {
  q?: string;
  status?: string;
  dateFrom?: string;
  dateTo?: string;
  muscleGroup?: string;
  exercise?: string;
  gym?: string;
  equipment?: string;
  minVolume?: number;
  maxVolume?: number;
  minDuration?: number;
  maxDuration?: number;
  page?: number;
  size?: number;
}

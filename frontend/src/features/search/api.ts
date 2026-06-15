import { apiGet } from "../../shared/api/client";
import type {
  SearchResults,
  TemplateSearchItem,
  TemplateSearchParams,
  WorkoutSearchItem,
  WorkoutSearchParams,
} from "./types";

function queryString(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  }
  return search.toString();
}

export function searchTemplates(params: TemplateSearchParams): Promise<SearchResults<TemplateSearchItem>> {
  return apiGet<SearchResults<TemplateSearchItem>>(
    `/search/templates?${queryString(params as unknown as Record<string, string | number | undefined>)}`,
  );
}

export function searchWorkouts(params: WorkoutSearchParams): Promise<SearchResults<WorkoutSearchItem>> {
  return apiGet<SearchResults<WorkoutSearchItem>>(
    `/search/workouts?${queryString(params as unknown as Record<string, string | number | undefined>)}`,
  );
}

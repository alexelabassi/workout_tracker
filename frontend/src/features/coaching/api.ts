import { apiDelete, apiGet, apiPost } from "../../shared/api/client";
import type { AnalyticsOverview } from "../analytics/types";
import type { HistoryPage } from "../history/types";
import type { SearchResults, WorkoutSearchItem } from "../search/types";
import type { WorkoutDetail } from "../workouts/types";
import type { ClientSummary, CoachRelationship } from "./types";

export interface ClientWorkoutSearchParams {
  q?: string;
  status?: string;
  muscleGroup?: string;
  gym?: string;
}

// --- coach side (ROLE_COACH; each per-client read is gated by an ACTIVE relationship) ---

export function fetchClients(): Promise<ClientSummary[]> {
  return apiGet<ClientSummary[]>("/coach/clients");
}

export function inviteClient(clientEmail: string): Promise<ClientSummary> {
  return apiPost<ClientSummary>("/coach/clients/invite", { clientEmail });
}

export function revokeClient(clientId: string): Promise<void> {
  return apiDelete<void>(`/coach/clients/${clientId}`);
}

export function fetchClientHistory(clientId: string, page: number, size: number): Promise<HistoryPage> {
  return apiGet<HistoryPage>(`/coach/clients/${clientId}/history?page=${page}&size=${size}`);
}

export function fetchClientAnalytics(clientId: string): Promise<AnalyticsOverview> {
  return apiGet<AnalyticsOverview>(`/coach/clients/${clientId}/analytics`);
}

export function fetchClientSession(clientId: string, sessionId: string): Promise<WorkoutDetail> {
  return apiGet<WorkoutDetail>(`/coach/clients/${clientId}/sessions/${sessionId}`);
}

/** Relationship-gated full-text search over a client's workout history. */
export function searchClientWorkouts(
  clientId: string,
  params: ClientWorkoutSearchParams,
): Promise<SearchResults<WorkoutSearchItem>> {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  }
  return apiGet<SearchResults<WorkoutSearchItem>>(
    `/coach/clients/${clientId}/search/workouts?${search.toString()}`,
  );
}

// --- client side (any authenticated user) ---

export function fetchInvites(): Promise<CoachRelationship[]> {
  return apiGet<CoachRelationship[]>("/coaching/invites");
}

export function acceptInvite(relationshipId: string): Promise<void> {
  return apiPost<void>(`/coaching/invites/${relationshipId}/accept`);
}

export function rejectInvite(relationshipId: string): Promise<void> {
  return apiPost<void>(`/coaching/invites/${relationshipId}/reject`);
}

export function fetchCoaches(): Promise<CoachRelationship[]> {
  return apiGet<CoachRelationship[]>("/coaching/coaches");
}

export function revokeCoach(relationshipId: string): Promise<void> {
  return apiDelete<void>(`/coaching/coaches/${relationshipId}`);
}

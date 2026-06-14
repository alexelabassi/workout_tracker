import { apiGet } from "../../shared/api/client";
import type { AnalyticsOverview } from "./types";

export function fetchAnalyticsOverview(): Promise<AnalyticsOverview> {
  return apiGet<AnalyticsOverview>("/analytics/overview");
}

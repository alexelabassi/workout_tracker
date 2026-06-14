import { apiGet } from "../../shared/api/client";
import type { HistoryPage } from "./types";

export function fetchHistory(page: number, size: number): Promise<HistoryPage> {
  return apiGet<HistoryPage>(`/history?page=${page}&size=${size}`);
}

import type { SessionStatus } from "../workouts/types";

export interface HistoryItem {
  sessionId: string;
  status: SessionStatus;
  startedAt: string;
  finishedAt: string | null;
  templateName: string | null;
  templateDayName: string | null;
  gymName: string | null;
  exerciseCount: number;
  setCount: number;
  totalVolume: number;
  durationSeconds: number | null;
}

export interface HistoryPage {
  items: HistoryItem[];
  page: number;
  size: number;
  totalItems: number;
  hasNext: boolean;
}

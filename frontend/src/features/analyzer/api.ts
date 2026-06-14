import { apiGet } from "../../shared/api/client";
import type { TemplateAnalysis } from "./types";

export function fetchAnalysis(templateId: string): Promise<TemplateAnalysis> {
  return apiGet<TemplateAnalysis>(`/templates/${templateId}/analysis`);
}

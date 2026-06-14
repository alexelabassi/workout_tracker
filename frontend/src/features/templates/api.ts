import { apiDelete, apiGet, apiPost, apiPut } from "../../shared/api/client";
import type {
  DayPayload,
  ExercisePayload,
  TemplateDay,
  TemplateDayExercise,
  TemplateDayRoutine,
  TemplateDetail,
  TemplatePayload,
  TemplateSummary,
} from "./types";

export function fetchTemplates(): Promise<TemplateSummary[]> {
  return apiGet<TemplateSummary[]>("/templates");
}

export function fetchTemplate(id: string): Promise<TemplateDetail> {
  return apiGet<TemplateDetail>(`/templates/${id}`);
}

export function createTemplate(payload: TemplatePayload): Promise<TemplateDetail> {
  return apiPost<TemplateDetail>("/templates", payload);
}

export function updateTemplate(id: string, payload: TemplatePayload): Promise<TemplateDetail> {
  return apiPut<TemplateDetail>(`/templates/${id}`, payload);
}

export function deleteTemplate(id: string): Promise<void> {
  return apiDelete<void>(`/templates/${id}`);
}

export function createDay(templateId: string, payload: DayPayload): Promise<TemplateDay> {
  return apiPost<TemplateDay>(`/templates/${templateId}/days`, payload);
}

export function updateDay(dayId: string, payload: DayPayload): Promise<TemplateDay> {
  return apiPut<TemplateDay>(`/template-days/${dayId}`, payload);
}

export function deleteDay(dayId: string): Promise<void> {
  return apiDelete<void>(`/template-days/${dayId}`);
}

export function addExercise(dayId: string, payload: ExercisePayload): Promise<TemplateDayExercise> {
  return apiPost<TemplateDayExercise>(`/template-days/${dayId}/exercises`, payload);
}

export function updateExercise(exerciseRowId: string, payload: ExercisePayload): Promise<TemplateDayExercise> {
  return apiPut<TemplateDayExercise>(`/template-day-exercises/${exerciseRowId}`, payload);
}

export function deleteExercise(exerciseRowId: string): Promise<void> {
  return apiDelete<void>(`/template-day-exercises/${exerciseRowId}`);
}

export function attachRoutine(dayId: string, routineId: string): Promise<TemplateDayRoutine> {
  return apiPost<TemplateDayRoutine>(`/template-days/${dayId}/routines`, { routineId });
}

export function removeRoutine(dayRoutineId: string): Promise<void> {
  return apiDelete<void>(`/template-day-routines/${dayRoutineId}`);
}

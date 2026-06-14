import { apiDelete, apiGet, apiPost, apiPut } from "../../shared/api/client";
import type { Equipment, EquipmentPayload, Gym, GymPayload } from "./types";

export function fetchGyms(): Promise<Gym[]> {
  return apiGet<Gym[]>("/gyms");
}

export function fetchGym(id: string): Promise<Gym> {
  return apiGet<Gym>(`/gyms/${id}`);
}

export function createGym(payload: GymPayload): Promise<Gym> {
  return apiPost<Gym>("/gyms", payload);
}

export function updateGym(id: string, payload: GymPayload): Promise<Gym> {
  return apiPut<Gym>(`/gyms/${id}`, payload);
}

export function deleteGym(id: string): Promise<void> {
  return apiDelete<void>(`/gyms/${id}`);
}

export function fetchEquipment(gymId: string): Promise<Equipment[]> {
  return apiGet<Equipment[]>(`/gyms/${gymId}/equipment`);
}

export function createEquipment(gymId: string, payload: EquipmentPayload): Promise<Equipment> {
  return apiPost<Equipment>(`/gyms/${gymId}/equipment`, payload);
}

export function updateEquipment(equipmentId: string, payload: EquipmentPayload): Promise<Equipment> {
  return apiPut<Equipment>(`/equipment/${equipmentId}`, payload);
}

export function deleteEquipment(equipmentId: string): Promise<void> {
  return apiDelete<void>(`/equipment/${equipmentId}`);
}

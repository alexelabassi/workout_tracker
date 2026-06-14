export type EquipmentType =
  | "BARBELL"
  | "DUMBBELL"
  | "MACHINE"
  | "CABLE"
  | "BENCH"
  | "BODYWEIGHT"
  | "CARDIO_MACHINE"
  | "OTHER";

export interface Gym {
  id: string;
  name: string;
  location: string | null;
  updatedAt: string;
}

export interface GymPayload {
  name: string;
  location: string | null;
}

export interface Equipment {
  id: string;
  gymId: string;
  name: string;
  equipmentType: EquipmentType | null;
  notes: string | null;
  updatedAt: string;
}

export interface EquipmentPayload {
  name: string;
  equipmentType: EquipmentType | null;
  notes: string | null;
}

export const EQUIPMENT_TYPES: EquipmentType[] = [
  "BARBELL",
  "DUMBBELL",
  "MACHINE",
  "CABLE",
  "BENCH",
  "BODYWEIGHT",
  "CARDIO_MACHINE",
  "OTHER",
];

export const EQUIPMENT_TYPE_LABELS: Record<EquipmentType, string> = {
  BARBELL: "Barbell",
  DUMBBELL: "Dumbbell",
  MACHINE: "Machine",
  CABLE: "Cable",
  BENCH: "Bench",
  BODYWEIGHT: "Bodyweight",
  CARDIO_MACHINE: "Cardio machine",
  OTHER: "Other",
};

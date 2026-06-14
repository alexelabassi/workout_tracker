export type Severity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | "INFO";

export interface AnalysisWarning {
  code: string;
  severity: Severity;
  title: string;
  explanation: string;
  affectedMuscleGroups: string[];
  suggestedFix: string | null;
}

export interface MuscleVolume {
  muscleGroup: string;
  weeklyWeightedSets: number;
  volumeDataIncomplete: boolean;
}

export interface MuscleFrequency {
  muscleGroup: string;
  daysPerWeek: number;
}

export interface BalanceRatios {
  pullToPush: number | null;
  posteriorToQuads: number | null;
  lowerToUpper: number | null;
}

export interface SubScores {
  volumeCoverage: number;
  frequency: number;
  balance: number;
  sessionDesign: number;
  specificityRest: number;
}

export interface TemplateAnalysis {
  templateId: string;
  overallScore: number;
  category: "WELL_STRUCTURED" | "DECENT_STRUCTURE" | "NEEDS_REVIEW";
  summary: string;
  subScores: SubScores;
  muscleGroupVolumes: MuscleVolume[];
  frequencyByMuscleGroup: MuscleFrequency[];
  balanceRatios: BalanceRatios;
  warnings: AnalysisWarning[];
  suggestions: string[];
  strengths: string[];
  limitations: string[];
  disclaimer: string;
}

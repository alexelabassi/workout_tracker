import type { Difficulty, SplitType, TemplateDetail } from "../templates/types";

export type VoteType = "UP" | "DOWN";
export type MarketplaceSort = "newest" | "top" | "trending";

export interface MarketplaceStats {
  upvotes: number;
  downvotes: number;
  saves: number;
  uses: number;
  ratingScore: number;
}

export interface MarketplaceSummary {
  id: string;
  name: string;
  description: string | null;
  splitType: SplitType | null;
  difficulty: Difficulty | null;
  daysPerWeek: number | null;
  estimatedDurationMinutes: number | null;
  publishedAt: string | null;
  authorDisplayName: string;
  stats: MarketplaceStats;
  myVote: VoteType | null;
  saved: boolean;
}

export interface MarketplacePageData {
  items: MarketplaceSummary[];
  page: number;
  size: number;
  totalItems: number;
  hasNext: boolean;
}

export interface MarketplaceDetail {
  template: TemplateDetail;
  authorDisplayName: string;
  stats: MarketplaceStats;
  myVote: VoteType | null;
  saved: boolean;
}

export interface InteractionResult {
  stats: MarketplaceStats;
  myVote: VoteType | null;
  saved: boolean;
}

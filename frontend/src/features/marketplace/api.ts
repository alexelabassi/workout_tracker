import { apiDelete, apiGet, apiPost } from "../../shared/api/client";
import type { TemplateDetail } from "../templates/types";
import type {
  InteractionResult,
  MarketplaceDetail,
  MarketplacePageData,
  MarketplaceSort,
  VoteType,
} from "./types";

export function fetchMarketplace(
  sort: MarketplaceSort,
  savedOnly: boolean,
  page: number,
  size: number,
): Promise<MarketplacePageData> {
  const params = new URLSearchParams({
    sort,
    savedOnly: String(savedOnly),
    page: String(page),
    size: String(size),
  });
  return apiGet<MarketplacePageData>(`/marketplace/templates?${params.toString()}`);
}

export function fetchMarketplaceDetail(id: string): Promise<MarketplaceDetail> {
  return apiGet<MarketplaceDetail>(`/marketplace/templates/${id}`);
}

export function voteTemplate(id: string, voteType: VoteType): Promise<InteractionResult> {
  return apiPost<InteractionResult>(`/marketplace/templates/${id}/vote`, { voteType });
}

export function clearVote(id: string): Promise<InteractionResult> {
  return apiDelete<InteractionResult>(`/marketplace/templates/${id}/vote`);
}

export function saveTemplate(id: string): Promise<InteractionResult> {
  return apiPost<InteractionResult>(`/marketplace/templates/${id}/save`);
}

export function unsaveTemplate(id: string): Promise<InteractionResult> {
  return apiDelete<InteractionResult>(`/marketplace/templates/${id}/save`);
}

export function useTemplate(id: string): Promise<TemplateDetail> {
  return apiPost<TemplateDetail>(`/marketplace/templates/${id}/use`);
}

import type { CreditRole } from "@/shared/api/types";

// POST /api/suggestions/places. 빈 선택 항목은 보내지 않는다(서버는 빈 링크를 받지 않는다).
export type PlaceSuggestion = {
  name: string;
  region: string;
  link?: string;
  reason: string;
  makerHint?: string;
};

// POST /api/suggestions/credits
export type CreditSuggestion = {
  placeId: number;
  role: CreditRole | null;
  makerName: string;
  sourceUrl?: string;
  isSelf: boolean;
};

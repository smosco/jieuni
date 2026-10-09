import { apiFetch } from "@/shared/api/client";
import type { CreditSuggestion, PlaceSuggestion } from "./types";

type Created = { id: number; status: string };

export function suggestPlace(body: PlaceSuggestion): Promise<Created> {
  return apiFetch("/api/suggestions/places", { method: "POST", body: JSON.stringify(body) });
}

export function suggestCredit(body: CreditSuggestion): Promise<Created> {
  return apiFetch("/api/suggestions/credits", { method: "POST", body: JSON.stringify(body) });
}

// 선택 항목은 비었으면 뺀다.
export function optional(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed === "" ? undefined : trimmed;
}

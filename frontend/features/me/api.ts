import { ApiError, apiFetch } from "@/shared/api/client";
import type { CursorPage } from "@/shared/api/types";
import type { FollowingMakers, FollowingPlace, Me, MeState, MySuggestions } from "./types";

// 비로그인(401)은 오류가 아니라 null.
export async function getMe(): Promise<Me | null> {
  try {
    return await apiFetch<Me>("/api/me");
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      return null;
    }
    throw e;
  }
}

export function getMeState(places: number[], makers: number[]): Promise<MeState> {
  const params = new URLSearchParams();
  if (places.length > 0) params.set("places", places.join(","));
  if (makers.length > 0) params.set("makers", makers.join(","));
  return apiFetch(`/api/me/state?${params}`);
}

export function changeNickname(nickname: string): Promise<Me> {
  return apiFetch("/api/me", { method: "PATCH", body: JSON.stringify({ nickname }) });
}

export function logout(): Promise<void> {
  return apiFetch("/api/auth/logout", { method: "POST" });
}

export function deleteMe(): Promise<void> {
  return apiFetch("/api/me", { method: "DELETE" });
}

export function fetchFollowingPlacesPage(cursor: string | null): Promise<CursorPage<FollowingPlace>> {
  return apiFetch(cursor ? `/api/me/following/places?cursor=${encodeURIComponent(cursor)}` : "/api/me/following/places");
}

export function getFollowingMakers(): Promise<FollowingMakers> {
  return apiFetch("/api/me/following/makers");
}

export function markFollowingSeen(): Promise<void> {
  return apiFetch("/api/me/following/seen", { method: "POST" });
}

export function getMySuggestions(): Promise<MySuggestions> {
  return apiFetch("/api/me/suggestions");
}

import { apiFetch } from "@/shared/api/client";
import { serverFetch } from "@/shared/api/server";
import type { CursorPage, MakerSummary, PlaceKind, PlaceTile } from "@/shared/api/types";

function placesPath(kinds: PlaceKind[], cursor: string | null): string {
  const params = new URLSearchParams();
  if (kinds.length > 0) params.set("kind", kinds.join(","));
  if (cursor) params.set("cursor", cursor);
  const query = params.toString();
  return query ? `/api/places?${query}` : "/api/places";
}

function makersPath(cursor: string | null): string {
  return cursor ? `/api/makers?cursor=${encodeURIComponent(cursor)}` : "/api/makers";
}

// 첫 페이지는 서버가 태그 캐시(browse)로 받고, 다음 페이지부터는 브라우저가 받는다.
export function getPlacesFirstPage(kinds: PlaceKind[]) {
  return serverFetch<CursorPage<PlaceTile>>(placesPath(kinds, null), { tags: ["browse"] });
}

export function fetchPlacesPage(kinds: PlaceKind[], cursor: string | null) {
  return apiFetch<CursorPage<PlaceTile>>(placesPath(kinds, cursor));
}

export function getMakersFirstPage() {
  return serverFetch<CursorPage<MakerSummary>>(makersPath(null), { tags: ["browse"] });
}

export function fetchMakersPage(cursor: string | null) {
  return apiFetch<CursorPage<MakerSummary>>(makersPath(cursor));
}

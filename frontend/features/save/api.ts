import { apiFetch } from "@/shared/api/client";
import type { CursorPage, PlaceTile } from "@/shared/api/types";
import type { SaveStatus } from "./types";

export function putSave(placeId: number, status: SaveStatus): Promise<{ placeId: number; status: SaveStatus }> {
  return apiFetch(`/api/me/saves/${placeId}`, { method: "PUT", body: JSON.stringify({ status }) });
}

export function deleteSave(placeId: number): Promise<void> {
  return apiFetch(`/api/me/saves/${placeId}`, { method: "DELETE" });
}

export function fetchSavesPage(status: SaveStatus, cursor: string | null): Promise<CursorPage<PlaceTile>> {
  const params = new URLSearchParams({ status });
  if (cursor) params.set("cursor", cursor);
  return apiFetch(`/api/me/saves?${params}`);
}

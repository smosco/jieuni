import { apiFetch } from "@/shared/api/client";
import type { CursorPage } from "@/shared/api/types";
import type { AdminPlace, AdminPlaceStatus, AdminPlaceSummary, PlaceEdit } from "./types";

export function fetchAdminPlacesPage(
  status: AdminPlaceStatus | null,
  q: string,
  cursor: string | null,
): Promise<CursorPage<AdminPlaceSummary>> {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  if (q) params.set("q", q);
  if (cursor) params.set("cursor", cursor);
  const query = params.toString();
  return apiFetch(query ? `/api/admin/places?${query}` : "/api/admin/places");
}

export function fetchAdminPlace(id: number): Promise<AdminPlace> {
  return apiFetch(`/api/admin/places/${id}`);
}

export function createAdminPlace(body: { name: string; region: string | null }): Promise<AdminPlace> {
  return apiFetch("/api/admin/places", { method: "POST", body: JSON.stringify(body) });
}

export function editAdminPlace(id: number, body: PlaceEdit): Promise<AdminPlace> {
  return apiFetch(`/api/admin/places/${id}`, { method: "PATCH", body: JSON.stringify(body) });
}

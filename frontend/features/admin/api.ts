import { apiFetch } from "@/shared/api/client";
import type { CursorPage } from "@/shared/api/types";
import type {
  AdminMaker,
  AdminMakerRow,
  AdminPlace,
  AdminPlaceStatus,
  AdminPlaceSummary,
  MakerEdit,
  PlaceEdit,
  PlaceLists,
} from "./types";

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

export function changePlaceStatus(id: number, to: AdminPlaceStatus): Promise<AdminPlace> {
  return apiFetch(`/api/admin/places/${id}/status`, { method: "POST", body: JSON.stringify({ to }) });
}

export function replacePlaceList<K extends keyof PlaceLists>(id: number, name: K, body: PlaceLists[K]): Promise<AdminPlace> {
  return apiFetch(`/api/admin/places/${id}/${name}`, { method: "PUT", body: JSON.stringify(body) });
}

export function fetchAdminMakersPage(q: string, cursor: string | null): Promise<CursorPage<AdminMakerRow>> {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (cursor) params.set("cursor", cursor);
  const query = params.toString();
  return apiFetch(query ? `/api/admin/makers?${query}` : "/api/admin/makers");
}

export function fetchAdminMaker(id: number): Promise<AdminMaker> {
  return apiFetch(`/api/admin/makers/${id}`);
}

export function createAdminMaker(body: MakerEdit): Promise<AdminMaker> {
  return apiFetch("/api/admin/makers", { method: "POST", body: JSON.stringify(body) });
}

export function editAdminMaker(id: number, body: MakerEdit): Promise<AdminMaker> {
  return apiFetch(`/api/admin/makers/${id}`, { method: "PATCH", body: JSON.stringify(body) });
}

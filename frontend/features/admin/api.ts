import { apiFetch } from "@/shared/api/client";
import type { CursorPage } from "@/shared/api/types";
import type {
  AdminDashboard,
  AdminMaker,
  AdminMakerRow,
  AdminPick,
  AdminPlace,
  AdminPlaceStatus,
  AdminPlaceSummary,
  CreditAccept,
  CreditSuggestion,
  MakerEdit,
  PhotoBody,
  PhotoEdit,
  PlaceEdit,
  PlaceLists,
  PlaceSuggestion,
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

// 사진 파일은 서버를 거치지 않는다: 서명 URL을 받아 브라우저가 저장소에 직접 PUT하고, 키를 등록한다.
// 두 파일의 바이트 수가 서명에 들어가 서버가 크기 한도를 본다.
export function requestUpload(
  placeId: number,
  sizes: { w1600: number; w800: number },
): Promise<{ objectKey: string; uploadUrls: { w1600: string; w800: string } }> {
  return apiFetch("/api/admin/uploads", {
    method: "POST",
    body: JSON.stringify({ placeId, contentType: "image/webp", sizes }),
  });
}

// 저장소(R2)는 다른 출처라 apiFetch(쿠키·XSRF)를 쓰지 않는다. Content-Type이 서명에 들어 있어 같은 값을 보내야 한다.
// Content-Length도 서명에 들어 있고, 브라우저가 blob 크기로 채운다.
export async function putToStorage(url: string, blob: Blob): Promise<void> {
  const res = await fetch(url, { method: "PUT", headers: { "Content-Type": "image/webp" }, body: blob });
  if (!res.ok) throw new Error(`upload ${res.status}`);
}

export function addPhoto(id: number, body: PhotoBody): Promise<AdminPlace> {
  return apiFetch(`/api/admin/places/${id}/photos`, { method: "POST", body: JSON.stringify(body) });
}

export function editPhoto(id: number, photoId: number, body: PhotoEdit): Promise<AdminPlace> {
  return apiFetch(`/api/admin/places/${id}/photos/${photoId}`, { method: "PATCH", body: JSON.stringify(body) });
}

export function reorderPhotos(id: number, photoIds: number[]): Promise<AdminPlace> {
  return apiFetch(`/api/admin/places/${id}/photos/order`, { method: "PUT", body: JSON.stringify({ photoIds }) });
}

export function deletePhoto(id: number, photoId: number): Promise<void> {
  return apiFetch(`/api/admin/places/${id}/photos/${photoId}`, { method: "DELETE" });
}

export function fetchAdminDashboard(): Promise<AdminDashboard> {
  return apiFetch("/api/admin/dashboard");
}

export function putPick(date: string, body: { placeId: number; line: string }): Promise<AdminPick> {
  return apiFetch(`/api/admin/picks/${date}`, { method: "PUT", body: JSON.stringify(body) });
}

export function deletePick(date: string): Promise<void> {
  return apiFetch(`/api/admin/picks/${date}`, { method: "DELETE" });
}

export function fetchPlaceSuggestions(): Promise<{ items: PlaceSuggestion[] }> {
  return apiFetch("/api/admin/suggestions?type=places");
}

export function fetchCreditSuggestions(): Promise<{ items: CreditSuggestion[] }> {
  return apiFetch("/api/admin/suggestions?type=credits");
}

export function startSuggestedPlace(placeId: number): Promise<AdminPlace> {
  return apiFetch(`/api/admin/suggestions/places/${placeId}/start`, { method: "POST" });
}

export function rejectSuggestedPlace(placeId: number): Promise<void> {
  return apiFetch(`/api/admin/suggestions/places/${placeId}/reject`, { method: "POST" });
}

export function acceptCreditSuggestion(id: number, body: CreditAccept): Promise<AdminPlace> {
  return apiFetch(`/api/admin/suggestions/credits/${id}/accept`, { method: "POST", body: JSON.stringify(body) });
}

export function declineCreditSuggestion(id: number): Promise<void> {
  return apiFetch(`/api/admin/suggestions/credits/${id}/decline`, { method: "POST" });
}

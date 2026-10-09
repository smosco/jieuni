import { apiFetch } from "@/shared/api/client";
import type { CursorPage } from "@/shared/api/types";
import type { AdminPlaceStatus, AdminPlaceSummary } from "./types";

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

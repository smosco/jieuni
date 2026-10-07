import { apiFetch } from "@/shared/api/client";
import type { SaveStatus } from "./types";

export function putSave(placeId: number, status: SaveStatus): Promise<{ placeId: number; status: SaveStatus }> {
  return apiFetch(`/api/me/saves/${placeId}`, { method: "PUT", body: JSON.stringify({ status }) });
}

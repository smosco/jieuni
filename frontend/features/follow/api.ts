import { apiFetch } from "@/shared/api/client";

export function putFollow(makerId: number): Promise<void> {
  return apiFetch(`/api/me/follows/${makerId}`, { method: "PUT" });
}

export function deleteFollow(makerId: number): Promise<void> {
  return apiFetch(`/api/me/follows/${makerId}`, { method: "DELETE" });
}

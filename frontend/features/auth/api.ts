import { apiFetch } from "@/shared/api/client";
import type { Me } from "@/features/me/types";

// 백엔드 dev·test 프로필에만 있다.
export function devLogin(email: string): Promise<Me> {
  return apiFetch<Me>("/api/dev/login", { method: "POST", body: JSON.stringify({ email }) });
}

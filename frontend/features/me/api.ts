import { ApiError, apiFetch } from "@/shared/api/client";
import type { Me } from "./types";

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

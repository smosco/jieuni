import { readCookie } from "./cookie";

// API.md 0절 오류 본문 { message, fieldErrors? }
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly fieldErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

// 브라우저는 상대 경로(/api/…)를 쓰고 next.config.ts의 rewrites가 백엔드로 넘긴다(같은 출처, DECISIONS 6절).
// 서버 컴포넌트는 API_URL로 백엔드를 직접 부른다.
function baseUrl(): string {
  if (typeof window !== "undefined") {
    return "";
  }
  return process.env.API_URL ?? "http://localhost:8080";
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const method = (init.method ?? "GET").toUpperCase();
  if (init.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  // CSRF: GET 외 요청은 XSRF-TOKEN 쿠키를 헤더로 돌려보낸다(API.md 0절).
  if (typeof document !== "undefined" && method !== "GET" && method !== "HEAD") {
    const token = readCookie(document.cookie, "XSRF-TOKEN");
    if (token) {
      headers.set("X-XSRF-TOKEN", token);
    }
  }

  const res = await fetch(baseUrl() + path, { ...init, headers });
  if (!res.ok) {
    throw await toApiError(res);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

async function toApiError(res: Response): Promise<ApiError> {
  try {
    const body = (await res.json()) as { message?: string; fieldErrors?: Record<string, string> };
    return new ApiError(res.status, body.message ?? res.statusText, body.fieldErrors);
  } catch {
    return new ApiError(res.status, res.statusText);
  }
}

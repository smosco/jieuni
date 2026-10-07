import type { Pending, PendingAction } from "./types";

// 구글 화면에서 오래 머물렀거나 다른 일을 하다 돌아온 뒤에 예전 동작이 실행되지 않게 한다.
export const PENDING_TTL_MS = 10 * 60 * 1000;

// fragment는 넣지 않는다. 서버가 잘라내고, 남으면 뒤에 붙는 ?login=error가 그 안으로 들어간다.
export function loginUrl(pathname: string, search: string): string {
  return `/oauth2/authorization/google?redirect=${encodeURIComponent(pathname + search)}`;
}

// login 파라미터만 뺀 쿼리. 다른 쿼리는 그대로 둔다.
export function stripLoginError(search: string): string {
  const params = new URLSearchParams(search);
  params.delete("login");
  const rest = params.toString();
  return rest ? `?${rest}` : "";
}

// 모양이 어긋나거나 만료된 값은 없는 것으로 본다.
export function parsePending(raw: string | null, now: number): Pending | null {
  if (!raw) return null;
  let value: unknown;
  try {
    value = JSON.parse(raw);
  } catch {
    return null;
  }
  if (typeof value !== "object" || value === null) return null;
  const v = value as Record<string, unknown>;
  if (typeof v.returnTo !== "string" || typeof v.savedAt !== "number") return null;
  if (now - v.savedAt > PENDING_TTL_MS || v.savedAt > now) return null;
  const action = toAction(v);
  return action && { ...action, returnTo: v.returnTo, savedAt: v.savedAt };
}

function toAction(v: Record<string, unknown>): PendingAction | null {
  switch (v.action) {
    case "save":
      return typeof v.placeId === "number" && (v.status === "want" || v.status === "been")
        ? { action: "save", placeId: v.placeId, status: v.status }
        : null;
    case "follow":
      return typeof v.makerId === "number" ? { action: "follow", makerId: v.makerId } : null;
    case "suggest":
      return { action: "suggest" };
    default:
      return null;
  }
}

// returnTo의 경로 부분. 다른 화면에서 로그인했으면 이어서 하지 않는다.
export function pathOf(returnTo: string): string {
  const end = returnTo.search(/[?#]/);
  return end < 0 ? returnTo : returnTo.slice(0, end);
}

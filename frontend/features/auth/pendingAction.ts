import { parsePending } from "./login";
import type { Pending, PendingAction } from "./types";

const KEY = "jieuni:pending-action";

// 탭 단위로 남긴다. 구글 로그인은 페이지를 떠나므로 메모리에는 둘 수 없다.
export function savePending(action: PendingAction, returnTo: string): void {
  const pending: Pending = { ...action, returnTo, savedAt: Date.now() };
  sessionStorage.setItem(KEY, JSON.stringify(pending));
}

// 꺼내면서 지운다. 실행이 실패해도 두 번 돌지 않게.
export function takePending(): Pending | null {
  const pending = parsePending(sessionStorage.getItem(KEY), Date.now());
  sessionStorage.removeItem(KEY);
  return pending;
}

export function clearPending(): void {
  sessionStorage.removeItem(KEY);
}

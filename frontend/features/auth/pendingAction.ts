import type { QueryClient } from "@tanstack/react-query";
import { putFollow } from "@/features/follow/api";
import { putSave } from "@/features/save/api";
import { parsePending } from "./login";
import type { Pending, PendingAction } from "./types";

const KEY = "jieuni:pending-action";

export const FAILURE_MESSAGE: Record<PendingAction["action"], string> = {
  save: "저장하지 못했어요. 다시 시도해 주세요",
  follow: "팔로우하지 못했어요. 다시 시도해 주세요",
  suggest: "소개하지 못했어요. 다시 시도해 주세요",
};

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

export async function runAction(action: PendingAction, queryClient: QueryClient): Promise<void> {
  switch (action.action) {
    case "save":
      await putSave(action.placeId, action.status);
      break;
    case "follow":
      await putFollow(action.makerId);
      break;
    case "suggest":
      // 소개하기는 돌아온 화면이 곧 이어서 할 일이다.
      return;
  }
  await queryClient.invalidateQueries({ queryKey: ["me"] });
}

import type { SaveStatus } from "@/features/save/types";

// 로그인이 필요한 동작. 시트 제목을 고르고, 남길 때는 로그인하고 돌아와 한 번 이어서 실행한다.
// 남기는 것은 "켜기"(저장·팔로우)뿐이다. 끄려다 세션이 끝난 경우는 남기지 않는다:
// 로그인 뒤 다시 받은 상태를 보고 사용자가 다시 고르는 편이 안전하다.
// suggest·me는 돌아온 화면이 곧 이어서 할 일이라 실행할 것이 없다.
export type PendingAction =
  | { action: "save"; placeId: number; status: SaveStatus }
  | { action: "follow"; makerId: number }
  | { action: "suggest" }
  | { action: "me" };

export type Pending = PendingAction & { returnTo: string; savedAt: number };

// resume: 로그인 뒤 이어서 할지. 끄기에서 401이 난 경우만 false.
// onLeave: 구글 로그인으로 페이지를 떠나기 직전에 부른다(피드가 보던 카드를 남긴다).
export type LoginRequest = { action: PendingAction; onLeave?: () => void; resume?: boolean };

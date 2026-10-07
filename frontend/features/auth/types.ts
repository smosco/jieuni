import type { SaveStatus } from "@/features/save/types";

// 로그인 전에 누른 동작. 로그인하고 돌아오면 한 번 이어서 실행한다.
export type PendingAction =
  | { action: "save"; placeId: number; status: SaveStatus }
  | { action: "follow"; makerId: number }
  | { action: "suggest" };

export type Pending = PendingAction & { returnTo: string; savedAt: number };

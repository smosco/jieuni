import type { CreditRole, MakerSummary, PlaceStatus, PlaceTile } from "@/shared/api/types";
import type { SaveStatus } from "@/features/save/types";

// GET /api/me
export type Me = {
  id: number;
  nickname: string;
  profileImageUrl: string | null;
  role: "user" | "admin";
  hasNewFromFollowing: boolean;
};

// GET /api/me/state. 저장·팔로우하지 않은 id는 빠진다.
export type MeState = {
  saves: Record<string, SaveStatus>;
  follows: number[];
};

export type FollowingPlace = PlaceTile & { isNew: boolean };

export type FollowingMakers = { items: MakerSummary[] };

// GET /api/me/suggestions. 공간 제보는 공간의 상태, 크레딧 제보는 제보의 상태를 그대로 준다.
export type MySuggestions = {
  places: { id: number; name: string; region: string; status: PlaceStatus | "suggested" | "draft" | "rejected"; createdAt: string }[];
  credits: { id: number; placeName: string; role: CreditRole; makerName: string; status: "pending" | "accepted" | "declined" }[];
};

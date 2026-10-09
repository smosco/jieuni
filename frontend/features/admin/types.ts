import type { PlaceKind } from "@/shared/api/types";

// 관리자는 공개되지 않은 상태까지 전부 본다.
export type AdminPlaceStatus = "suggested" | "draft" | "published" | "paused" | "closed" | "rejected";

// GET /api/admin/places 한 줄
export type AdminPlaceSummary = {
  id: number;
  slug: string | null;
  name: string;
  region: string | null;
  kind: PlaceKind | null;
  status: AdminPlaceStatus;
  updatedAt: string;
  hasPhoto: boolean;
};

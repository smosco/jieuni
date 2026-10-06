import type { CreditRole, PlaceTile } from "@/shared/api/types";

// API.md 1절 GET /api/feed. 시간 계산은 서버가 하고 프론트는 이 값을 그린다.
export type Weekday = "MON" | "TUE" | "WED" | "THU" | "FRI" | "SAT" | "SUN";

export type FeedPick = {
  date: string;
  weekday: Weekday;
  line: string;
  place: PlaceTile;
  firstMaker: { id: number; slug: string | null; name: string; role: CreditRole } | null;
};

export type FeedResponse = {
  weekLabel: string;
  picks: FeedPick[];
  openedCount: number;
  nextOpensAt: string;
  nextWeekday: Weekday;
  nextRelative: "today" | "tomorrow" | "later";
};

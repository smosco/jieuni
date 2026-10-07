import type { PlaceKind } from "@/shared/api/types";

// 칩 순서. URL의 kind도 이 순서로 정렬해 같은 조건이 같은 주소가 되게 한다.
export const KIND_ORDER: PlaceKind[] = [
  "garden", "stay", "cafe", "museum", "bar", "gallery", "bookstore", "restaurant", "shop", "religious", "public", "other",
];

// ?kind=garden,cafe → ["garden", "cafe"]. 모르는 값은 버린다(백엔드는 모르는 값에 400을 준다).
export function parseKinds(param: string | string[] | undefined): PlaceKind[] {
  const values = new Set((Array.isArray(param) ? param.join(",") : param ?? "").split(","));
  return KIND_ORDER.filter((kind) => values.has(kind));
}

export function placesHref(kinds: PlaceKind[]): string {
  return kinds.length === 0 ? "/places" : `/places?kind=${kinds.join(",")}`;
}

export function toggleKind(kinds: PlaceKind[], kind: PlaceKind): PlaceKind[] {
  return kinds.includes(kind) ? kinds.filter((k) => k !== kind) : KIND_ORDER.filter((k) => k === kind || kinds.includes(k));
}

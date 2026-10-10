import type { Credit, Embed, Source } from "@/features/place/types";
import type { Access, Photo, PlaceKind, PlaceTile } from "@/shared/api/types";

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

export type AdminPhoto = Photo & { id: number; objectKey: string; position: number };

// GET /api/admin/places/{id}. 공개 상세의 하위 목록에 관리자 값이 붙는다.
export type AdminPlace = {
  id: number;
  slug: string | null;
  name: string;
  kind: PlaceKind | null;
  region: string | null;
  address: string | null;
  lat: number | null;
  lng: number | null;
  access: Access | null;
  visitNote: string | null;
  summary: string | null;
  impression: string | null;
  senses: Senses<string | null>;
  essay: string | null;
  visitedByEditorOn: string | null;
  status: AdminPlaceStatus;
  publishedAt: string | null;
  credits: (Credit & { makerId: number })[];
  sources: Source[];
  photos: AdminPhoto[];
  embeds: Embed[];
  mapLinks: { naver: string; kakao: string };
  relatedPlaces: PlaceTile[];
  suggestedBy: { id: number; nickname: string } | null;
  suggestionReason: string | null;
  suggestionLink: string | null;
  suggestionMakerHint: string | null;
  createdAt: string;
  updatedAt: string;
  slugLocked: boolean;
};

export type Senses<T> = { light: T; sound: T; scent: T; touch: T };

// PATCH /api/admin/places/{id}: 스칼라 필드 전부. 빈 값은 null.
export type PlaceEdit = Pick<
  AdminPlace,
  | "slug"
  | "name"
  | "kind"
  | "region"
  | "address"
  | "lat"
  | "lng"
  | "access"
  | "visitNote"
  | "summary"
  | "impression"
  | "senses"
  | "essay"
  | "visitedByEditorOn"
>;

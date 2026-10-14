import type { Credit, Source, SourceType } from "@/features/place/types";
import type { Access, CreditRole, Embed, MakerKind, Photo, PhotoLicense, PlaceKind, PlaceTile } from "@/shared/api/types";

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
  // 편성된 날. 공간은 한 번만 편성한다.
  pickedOn: string | null;
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
  | "access"
  | "visitNote"
  | "summary"
  | "impression"
  | "senses"
  | "essay"
  | "visitedByEditorOn"
>;

// PUT /api/admin/places/{id}/credits|sources|embeds: 통째 교체, 배열 순서가 position.
export type CreditBody = { makerId: number; role: CreditRole; verified: boolean; sourceUrl: string | null };
export type SourceBody = { title: string; url: string; type: SourceType };
export type PlaceLists = { credits: CreditBody[]; sources: SourceBody[]; embeds: Embed[] };

// POST /api/admin/places/{id}/photos와 PATCH 본문(네 칸 전부). 빈 값은 null.
export type PhotoEdit = { alt: string | null; credit: string | null; license: PhotoLicense; sourceUrl: string | null };
export type PhotoBody = { objectKey: string; width: number; height: number } & PhotoEdit;

// GET /api/admin/makers 한 줄
export type AdminMakerRow = {
  id: number;
  slug: string | null;
  name: string;
  kind: MakerKind;
  region: string | null;
  placeCount: number;
  updatedAt: string;
};

// GET /api/admin/makers/{id}
export type AdminMaker = {
  id: number;
  slug: string | null;
  name: string;
  kind: MakerKind;
  region: string | null;
  intro: string | null;
  website: string | null;
  instagram: string | null;
  affiliation: { id: number; slug: string | null; name: string } | null;
  places: PlaceTile[];
  slugLocked: boolean;
  createdAt: string;
  updatedAt: string;
};

// POST·PATCH /api/admin/makers: 필드 전부. 빈 값은 null.
export type MakerEdit = {
  slug: string | null;
  name: string;
  kind: MakerKind;
  region: string | null;
  intro: string | null;
  website: string | null;
  instagram: string | null;
  affiliationId: number | null;
};

// 편성 한 칸. 빈 칸은 place·line이 null. locked = 이미 열린 칸(서버가 계산한다).
export type AdminPick = {
  date: string;
  weekday: "MON" | "TUE" | "WED" | "THU" | "FRI";
  line: string | null;
  locked: boolean;
  place: { id: number; slug: string | null; name: string; photo: Photo | null } | null;
};

// GET /api/admin/dashboard
export type AdminDashboard = {
  picks: { thisWeek: AdminPick[]; nextWeek: AdminPick[] };
  pendingSuggestions: number;
  draftCount: number;
  llmCostThisMonthUsd: number;
};

type Reviewer = { id: number; nickname: string } | null;

// GET /api/admin/suggestions?type=places
export type PlaceSuggestion = {
  id: number;
  name: string;
  region: string | null;
  link: string | null;
  reason: string | null;
  makerHint: string | null;
  suggestedBy: Reviewer;
  createdAt: string;
};

// GET /api/admin/suggestions?type=credits
export type CreditSuggestion = {
  id: number;
  place: { id: number; slug: string | null; name: string };
  role: CreditRole;
  makerName: string;
  sourceUrl: string | null;
  isSelf: boolean;
  user: Reviewer;
  status: "pending" | "accepted" | "declined";
  createdAt: string;
};

export type SuggestionType = "places" | "credits";

// POST /api/admin/suggestions/credits/{id}/accept
export type CreditAccept = { makerId: number; verified: boolean; sourceUrl: string | null };

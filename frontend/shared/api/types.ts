// 여러 API가 같이 쓰는 응답 모양. 값은 백엔드 enum과 같다.

export type PlaceKind =
  | "cafe"
  | "bar"
  | "restaurant"
  | "stay"
  | "garden"
  | "museum"
  | "gallery"
  | "bookstore"
  | "shop"
  | "religious"
  | "public"
  | "other";

export type PlaceStatus = "published" | "paused" | "closed";

export type Access = "walk_in" | "reservation" | "ticket" | "guests_only";

export type EmbedType = "youtube" | "instagram";

export type Embed = { type: EmbedType; url: string };

export type PhotoLicense = "own" | "press" | "credited";

export type Photo = {
  url: { w1600: string; w800: string };
  width: number;
  height: number;
  alt: string;
  credit: string;
  license: PhotoLicense;
  sourceUrl: string | null;
};

export type PlaceTile = {
  id: number;
  slug: string;
  name: string;
  region: string;
  kind: PlaceKind;
  status: PlaceStatus;
  photo: Photo | null;
  caption: string | null;
};

export type MakerKind = "person" | "studio" | "owner";

export type CreditRole =
  | "planning"
  | "branding"
  | "architecture"
  | "interior"
  | "landscape"
  | "lighting"
  | "furniture"
  | "scent"
  | "photography"
  | "owner"
  | "other";

export type MakerSummary = {
  id: number;
  slug: string;
  name: string;
  kind: MakerKind;
  region: string | null;
  photo: Photo | null;
};

export type CursorPage<T> = {
  items: T[];
  nextCursor: string | null;
};

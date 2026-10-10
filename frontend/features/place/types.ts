import type { Access, CreditRole, MakerKind, Photo, PlaceKind, PlaceTile } from "@/shared/api/types";

// GET /api/places/{slug}
export type SourceType = "article" | "video" | "instagram" | "official";
export type EmbedType = "youtube" | "instagram";

export type Credit = {
  position: number;
  role: CreditRole;
  verified: boolean;
  sourceUrl: string | null;
  maker: { id: number; slug: string | null; name: string; kind: MakerKind };
};

export type Embed = { type: EmbedType; url: string };

export type Source = { title: string; url: string; type: SourceType };

export type PlaceDetail = {
  id: number;
  slug: string;
  name: string;
  kind: PlaceKind;
  region: string;
  address: string | null;
  lat: number | null;
  lng: number | null;
  access: Access | null;
  visitNote: string | null;
  summary: string | null;
  impression: string | null;
  senses: { light: string | null; sound: string | null; scent: string | null; touch: string | null };
  essay: string | null;
  visitedByEditorOn: string | null;
  status: "published" | "paused" | "closed";
  publishedAt: string;
  credits: Credit[];
  sources: Source[];
  photos: Photo[];
  embeds: Embed[];
  mapLinks: { naver: string; kakao: string };
  relatedPlaces: PlaceTile[];
};

import type { MakerKind, PlaceTile } from "@/shared/api/types";

// GET /api/makers/{slug}
export type MakerProfile = {
  id: number;
  slug: string;
  name: string;
  kind: MakerKind;
  region: string | null;
  intro: string | null;
  website: string | null;
  instagram: string | null;
  affiliation: { id: number; slug: string | null; name: string } | null;
  places: PlaceTile[];
};

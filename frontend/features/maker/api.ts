import { serverFetch } from "@/shared/api/server";
import type { MakerProfile } from "./types";

export function getMaker(slug: string): Promise<MakerProfile> {
  return serverFetch<MakerProfile>(`/api/makers/${encodeURIComponent(slug)}`, { tags: [`maker:${slug}`] });
}

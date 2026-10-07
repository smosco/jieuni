import { serverFetch } from "@/shared/api/server";
import type { PlaceDetail } from "./types";

export function getPlace(slug: string): Promise<PlaceDetail> {
  return serverFetch<PlaceDetail>(`/api/places/${encodeURIComponent(slug)}`, { tags: [`place:${slug}`] });
}

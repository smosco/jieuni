import { serverFetch } from "@/shared/api/server";
import type { FeedResponse } from "./types";

export function getFeed(): Promise<FeedResponse> {
  return serverFetch<FeedResponse>("/api/feed", { tags: ["feed"] });
}

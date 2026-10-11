import { serverFetch } from "@/shared/api/server";

// GET /api/sitemap
export type SitemapData = {
  places: { slug: string; updatedAt: string }[];
  makers: { slug: string; updatedAt: string }[];
};

export function getSitemap(): Promise<SitemapData> {
  return serverFetch<SitemapData>("/api/sitemap", { tags: ["sitemap"] });
}

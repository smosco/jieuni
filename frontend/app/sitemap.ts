import type { MetadataRoute } from "next";
import { getSitemap } from "@/features/seo/api";
import { SITE_URL } from "@/shared/lib/site";

// 요청마다 만들되 백엔드 응답은 태그 캐시(sitemap)에서 받는다. 빌드 때 prerender되면 백엔드 없이 빌드가 깨진다.
export const revalidate = 0;

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const { places, makers } = await getSitemap();
  return [
    { url: SITE_URL },
    { url: `${SITE_URL}/places` },
    { url: `${SITE_URL}/makers` },
    { url: `${SITE_URL}/about` },
    ...places.map((p) => ({ url: `${SITE_URL}/places/${p.slug}`, lastModified: p.updatedAt })),
    ...makers.map((m) => ({ url: `${SITE_URL}/makers/${m.slug}`, lastModified: m.updatedAt })),
  ];
}

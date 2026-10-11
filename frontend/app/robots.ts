import type { MetadataRoute } from "next";
import { SITE_URL } from "@/shared/lib/site";

// 로그인한 사람만 의미 있는 화면과 프록시 경로는 검색에서 뺀다.
export default function robots(): MetadataRoute.Robots {
  return {
    rules: { userAgent: "*", allow: "/", disallow: ["/admin", "/me", "/suggest", "/api/", "/oauth2/", "/login/"] },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}

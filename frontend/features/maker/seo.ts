import type { Metadata } from "next";
import { subjectParticle } from "@/shared/lib/josa";
import { pageMetadata, SITE_NAME, SITE_URL } from "@/shared/lib/site";
import type { MakerProfile } from "./types";

function description(maker: MakerProfile): string {
  if (maker.intro) {
    return maker.intro;
  }
  const places = maker.places.slice(0, 5).map((p) => p.name);
  return `${maker.name}${subjectParticle(maker.name)} 만든 공간: ${places.join(", ")}`;
}

export function makerMetadata(maker: MakerProfile): Metadata {
  return pageMetadata({
    title: `${maker.name}의 공간들 — ${SITE_NAME}`,
    description: description(maker),
    path: `/makers/${maker.slug}`,
    ownCard: true,
  });
}

// 개인만 Person, 스튜디오와 주인(가게·회사)은 Organization.
export function makerJsonLd(maker: MakerProfile): Record<string, unknown> {
  const sameAs = [maker.website, maker.instagram && `https://www.instagram.com/${maker.instagram}/`].filter(Boolean);
  return {
    "@type": maker.kind === "person" ? "Person" : "Organization",
    name: maker.name,
    description: description(maker),
    url: `${SITE_URL}/makers/${maker.slug}`,
    ...(sameAs.length > 0 && { sameAs }),
    ...(maker.affiliation?.slug && {
      memberOf: { "@type": "Organization", name: maker.affiliation.name, url: `${SITE_URL}/makers/${maker.affiliation.slug}` },
    }),
  };
}

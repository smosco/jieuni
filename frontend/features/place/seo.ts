import type { Metadata } from "next";
import { KIND_LABEL } from "@/shared/lib/labels";
import { pageMetadata, SITE_NAME, SITE_URL } from "@/shared/lib/site";
import type { PlaceDetail } from "./types";

// 공간 이름 검색에서 만든 사람 이름도 같이 걸리게 설명에 넣는다.
function description(place: PlaceDetail): string {
  const base = place.summary ?? `${place.region}의 ${KIND_LABEL[place.kind]}`;
  const makers = [...new Set(place.credits.map((c) => c.maker.name))];
  return makers.length > 0 ? `${base} · 만든 사람 ${makers.join(", ")}` : base;
}

export function placeMetadata(place: PlaceDetail): Metadata {
  return pageMetadata({
    title: `${place.name} — ${SITE_NAME}`,
    description: description(place),
    path: `/places/${place.slug}`,
    ownCard: true,
  });
}

export function placeJsonLd(place: PlaceDetail): Record<string, unknown> {
  return {
    "@type": "Place",
    name: place.name,
    description: description(place),
    url: `${SITE_URL}/places/${place.slug}`,
    ...(place.address && {
      address: { "@type": "PostalAddress", streetAddress: place.address, addressRegion: place.region, addressCountry: "KR" },
    }),
    ...(place.lat !== null && place.lng !== null && {
      geo: { "@type": "GeoCoordinates", latitude: place.lat, longitude: place.lng },
    }),
    ...(place.photos.length > 0 && { image: place.photos.map((p) => p.url.w1600) }),
  };
}

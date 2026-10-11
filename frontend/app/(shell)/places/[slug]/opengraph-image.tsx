import { loadPlace } from "@/features/place/api";
import { SHARE_CARD_SIZE, shareCard } from "@/features/seo/shareCard";
import { KIND_LABEL } from "@/shared/lib/labels";

// 사진은 넣지 않는다. 사진이 모두 WebP인데 ImageResponse(satori)는 WebP를 그리지 못한다.
export const alt = "공간 공유 카드";
export const size = SHARE_CARD_SIZE;
export const contentType = "image/png";

export default async function Image({ params }: { params: Promise<{ slug: string }> }) {
  const place = await loadPlace((await params).slug);
  return shareCard({ kicker: `${place.region} · ${KIND_LABEL[place.kind]}`, title: place.name });
}

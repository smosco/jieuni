import { loadPlace } from "@/features/place/api";
import type { PlaceDetail } from "@/features/place/types";
import { SHARE_CARD_SIZE, shareCard } from "@/features/seo/shareCard";
import { KIND_LABEL } from "@/shared/lib/labels";
import { shareCardAlt } from "@/shared/lib/site";

// 사진은 넣지 않는다. 사진이 모두 WebP인데 ImageResponse(satori)는 WebP를 그리지 못한다.
function card(place: PlaceDetail) {
  return { kicker: `${place.region} · ${KIND_LABEL[place.kind]}`, title: place.name };
}

// 대체 글에 공간 이름을 넣으려고 고정 alt 대신 쓴다. 카드는 하나다.
// 빌드 때 카드 주소를 모으려고 slug 없이({}) 한 번 불린다. 그때는 백엔드를 부르지 않는다(백엔드 없이 빌드).
export async function generateImageMetadata({ params }: { params: { slug?: string } }) {
  const alt = params.slug ? shareCardAlt(card(await loadPlace(params.slug))) : "공간 공유 카드";
  return [{ id: "card", alt, size: SHARE_CARD_SIZE, contentType: "image/png" }];
}

export default async function Image({ params }: { params: Promise<{ slug: string }> }) {
  return shareCard(card(await loadPlace((await params).slug)));
}

import { loadMaker } from "@/features/maker/api";
import type { MakerProfile } from "@/features/maker/types";
import { SHARE_CARD_SIZE, shareCard } from "@/features/seo/shareCard";
import { makerLabel } from "@/shared/lib/labels";
import { shareCardAlt } from "@/shared/lib/site";

function card(maker: MakerProfile) {
  return { kicker: makerLabel(maker.kind, maker.region), title: `${maker.name}의 공간들` };
}

// 대체 글에 이름을 넣으려고 고정 alt 대신 쓴다. 카드는 하나다.
// 빌드 때 카드 주소를 모으려고 slug 없이({}) 한 번 불린다. 그때는 백엔드를 부르지 않는다(백엔드 없이 빌드).
export async function generateImageMetadata({ params }: { params: { slug?: string } }) {
  const alt = params.slug ? shareCardAlt(card(await loadMaker(params.slug))) : "만든 사람 공유 카드";
  return [{ id: "card", alt, size: SHARE_CARD_SIZE, contentType: "image/png" }];
}

export default async function Image({ params }: { params: Promise<{ slug: string }> }) {
  return shareCard(card(await loadMaker((await params).slug)));
}

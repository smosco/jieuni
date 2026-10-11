import { loadMaker } from "@/features/maker/api";
import { SHARE_CARD_SIZE, shareCard } from "@/features/seo/shareCard";
import { makerLabel } from "@/shared/lib/labels";

export const alt = "만든 사람 공유 카드";
export const size = SHARE_CARD_SIZE;
export const contentType = "image/png";

export default async function Image({ params }: { params: Promise<{ slug: string }> }) {
  const maker = await loadMaker((await params).slug);
  return shareCard({ kicker: makerLabel(maker.kind, maker.region), title: `${maker.name}의 공간들` });
}

import { SHARE_CARD_SIZE, shareCard } from "@/features/seo/shareCard";
import { HOME_CARD, shareCardAlt } from "@/shared/lib/site";

// 공간·만든 사람 페이지가 아닌 곳(홈·둘러보기)의 공유 카드. 데이터가 없어 빌드 때 한 번 만든다.
export const alt = shareCardAlt(HOME_CARD);
export const size = SHARE_CARD_SIZE;
export const contentType = "image/png";

export default function Image() {
  return shareCard(HOME_CARD);
}

import { getMakersFirstPage } from "@/features/browse/api";
import { BrowseHead } from "@/features/browse/components/BrowseHead";
import { MakerBrowseList } from "@/features/browse/components/MakerBrowseList";

// 요청마다 그리되 첫 페이지는 태그 캐시(browse)에서 받는다. 정적 prerender가 아니라서 빌드 때 백엔드가 없어도 된다.
export const revalidate = 0;

export default async function MakersPage() {
  const first = await getMakersFirstPage();
  return (
    <>
      <BrowseHead active="makers" />
      <MakerBrowseList initial={first} />
    </>
  );
}

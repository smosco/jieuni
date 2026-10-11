import type { Metadata } from "next";
import { getMakersFirstPage } from "@/features/browse/api";
import { BrowseHead } from "@/features/browse/components/BrowseHead";
import { MakerBrowseList } from "@/features/browse/components/MakerBrowseList";
import { pageMetadata, SITE_NAME } from "@/shared/lib/site";

// 요청마다 그리되 첫 페이지는 태그 캐시(browse)에서 받는다. 정적 prerender가 아니라서 빌드 때 백엔드가 없어도 된다.
export const revalidate = 0;

export const metadata: Metadata = pageMetadata({
  title: `만든 사람 둘러보기 — ${SITE_NAME}`,
  description: "공간을 지은 건축가, 디자이너, 스튜디오. 그들이 만든 공간을 모아 봐요.",
  path: "/makers",
});

export default async function MakersPage() {
  const first = await getMakersFirstPage();
  return (
    <>
      <BrowseHead active="makers" />
      <MakerBrowseList initial={first} />
    </>
  );
}

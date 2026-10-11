import type { Metadata } from "next";
import { getPlacesFirstPage } from "@/features/browse/api";
import { BrowseHead } from "@/features/browse/components/BrowseHead";
import { KindChips } from "@/features/browse/components/KindChips";
import { PlaceBrowseList } from "@/features/browse/components/PlaceBrowseList";
import { parseKinds } from "@/features/browse/kinds";
import { pageMetadata, SITE_NAME } from "@/shared/lib/site";

// 요청마다 그리되 첫 페이지는 태그 캐시(browse)에서 받는다. 정적 prerender가 아니라서 빌드 때 백엔드가 없어도 된다.
export const revalidate = 0;

export const metadata: Metadata = pageMetadata({
  title: `공간 둘러보기 — ${SITE_NAME}`,
  description: "카페, 스테이, 정원, 미술관. 누가 지었는지와 함께 기록한 공간들이에요.",
  path: "/places",
});

export default async function PlacesPage({ searchParams }: PageProps<"/places">) {
  const kinds = parseKinds((await searchParams).kind);
  const first = await getPlacesFirstPage(kinds);
  return (
    <>
      <BrowseHead active="places" />
      <KindChips selected={kinds} />
      <PlaceBrowseList kinds={kinds} initial={first} />
    </>
  );
}

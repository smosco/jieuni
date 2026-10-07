import { getPlacesFirstPage } from "@/features/browse/api";
import { BrowseHead } from "@/features/browse/components/BrowseHead";
import { KindChips } from "@/features/browse/components/KindChips";
import { PlaceBrowseList } from "@/features/browse/components/PlaceBrowseList";
import { parseKinds } from "@/features/browse/kinds";

// 요청마다 그리되 첫 페이지는 태그 캐시(browse)에서 받는다. 정적 prerender가 아니라서 빌드 때 백엔드가 없어도 된다.
export const revalidate = 0;

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

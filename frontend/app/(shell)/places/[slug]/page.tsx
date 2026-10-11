import { loadPlace } from "@/features/place/api";
import { PlaceDetailView } from "@/features/place/components/PlaceDetailView";
import { placeJsonLd, placeMetadata } from "@/features/place/seo";
import { JsonLd } from "@/shared/ui/JsonLd";

// 빌드 때는 아무 공간도 만들지 않고(백엔드 없이 빌드), 첫 요청 때 만들어 태그 place:{slug}로 캐시한다.
export const dynamicParams = true;

export function generateStaticParams() {
  return [];
}

export async function generateMetadata({ params }: PageProps<"/places/[slug]">) {
  return placeMetadata(await loadPlace((await params).slug));
}

export default async function PlacePage({ params }: PageProps<"/places/[slug]">) {
  const place = await loadPlace((await params).slug);
  return (
    <>
      <JsonLd data={placeJsonLd(place)} />
      <PlaceDetailView place={place} />
    </>
  );
}

import { notFound } from "next/navigation";
import { getPlace } from "@/features/place/api";
import { PlaceDetailView } from "@/features/place/components/PlaceDetailView";
import { ApiError } from "@/shared/api/client";
import { assertSlug } from "@/shared/lib/slug";

// 빌드 때는 아무 공간도 만들지 않고(백엔드 없이 빌드), 첫 요청 때 만들어 태그 place:{slug}로 캐시한다.
export const dynamicParams = true;

export function generateStaticParams() {
  return [];
}

export default async function PlacePage({ params }: PageProps<"/places/[slug]">) {
  const { slug } = await params;
  assertSlug(slug);
  const place = await getPlace(slug).catch((e: unknown) => {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  });
  return <PlaceDetailView place={place} />;
}

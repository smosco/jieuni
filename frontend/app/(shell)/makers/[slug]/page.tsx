import { notFound } from "next/navigation";
import { getMaker } from "@/features/maker/api";
import { MakerProfileView } from "@/features/maker/components/MakerProfileView";
import { ApiError } from "@/shared/api/client";
import { assertSlug } from "@/shared/lib/slug";

// 빌드 때는 아무 프로필도 만들지 않고(백엔드 없이 빌드), 첫 요청 때 만들어 태그 maker:{slug}로 캐시한다.
export const dynamicParams = true;

export function generateStaticParams() {
  return [];
}

export default async function MakerPage({ params }: PageProps<"/makers/[slug]">) {
  const { slug } = await params;
  assertSlug(slug);
  const maker = await getMaker(slug).catch((e: unknown) => {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  });
  return <MakerProfileView maker={maker} />;
}

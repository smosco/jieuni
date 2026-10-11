import { loadMaker } from "@/features/maker/api";
import { MakerProfileView } from "@/features/maker/components/MakerProfileView";
import { makerJsonLd, makerMetadata } from "@/features/maker/seo";
import { JsonLd } from "@/shared/ui/JsonLd";

// 빌드 때는 아무 프로필도 만들지 않고(백엔드 없이 빌드), 첫 요청 때 만들어 태그 maker:{slug}로 캐시한다.
export const dynamicParams = true;

export function generateStaticParams() {
  return [];
}

export async function generateMetadata({ params }: PageProps<"/makers/[slug]">) {
  return makerMetadata(await loadMaker((await params).slug));
}

export default async function MakerPage({ params }: PageProps<"/makers/[slug]">) {
  const maker = await loadMaker((await params).slug);
  return (
    <>
      <JsonLd data={makerJsonLd(maker)} />
      <MakerProfileView maker={maker} />
    </>
  );
}

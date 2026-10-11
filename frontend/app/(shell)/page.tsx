import type { Metadata } from "next";
import { getFeed } from "@/features/feed/api";
import { Feed } from "@/features/feed/components/Feed";
import { pageMetadata, SITE_DESCRIPTION, SITE_NAME } from "@/shared/lib/site";

// 요청마다 그리되 백엔드 응답은 태그 캐시(feed)에서 받는다. 정적 prerender가 아니라서 빌드 때 백엔드가 없어도 된다.
export const revalidate = 0;

export const metadata: Metadata = pageMetadata({
  title: `${SITE_NAME} — 오늘의 공간`,
  description: SITE_DESCRIPTION,
  path: "/",
});

export default async function Home() {
  const feed = await getFeed();
  return (
    <>
      <h1 className="visually-hidden">오늘의 공간</h1>
      <Feed feed={feed} />
    </>
  );
}

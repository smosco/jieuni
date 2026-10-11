import type { Metadata } from "next";

// 검색·공유에 쓰는 절대 주소의 바탕. 빌드 때 굳는다(NEXT_PUBLIC_). 로컬은 비워 두면 된다.
export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || "http://localhost:3000";

export const SITE_NAME = "공간의 지은이";

export const SITE_DESCRIPTION = "다녀온 뒤에도 오래 생각나는 공간과, 그 공간을 지은 사람들을 소개해요. 평일 아침마다 한 곳씩.";

// 자식 경로의 openGraph·twitter는 부모 것(루트 공유 카드 포함)을 통째로 덮어쓴다.
// 그래서 사이트 이름·언어·기본 카드까지 같이 넣는다. 경로에 자기 opengraph-image가 있으면 ownCard로 기본 카드를 뺀다
// (여기 넣은 이미지가 경로의 카드보다 앞서 나왔고, images 키가 undefined로라도 있으면 경로의 카드가 사라졌다).
const DEFAULT_CARD = { url: "/opengraph-image", width: 1200, height: 630, alt: SITE_NAME };

export function pageMetadata({
  title,
  description,
  path,
  ownCard = false,
}: {
  title: string;
  description: string;
  path: string;
  ownCard?: boolean;
}): Metadata {
  const card = ownCard ? {} : { images: DEFAULT_CARD };
  return {
    title,
    description,
    alternates: { canonical: path },
    openGraph: { type: "website", siteName: SITE_NAME, locale: "ko_KR", url: path, title, description, ...card },
    twitter: { card: "summary_large_image", title, description, ...card },
  };
}

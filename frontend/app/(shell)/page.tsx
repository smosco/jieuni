import { getFeed } from "@/features/feed/api";

// 요청마다 그리되 백엔드 응답은 태그 캐시(feed)에서 받는다. 정적 prerender가 아니라서 빌드 때 백엔드가 없어도 된다.
export const revalidate = 0;

// 임시 화면: 피드 카드를 만들 때 바꾼다.
export default async function Home() {
  const feed = await getFeed();
  return (
    <main>
      <ul>
        {feed.picks.map((pick) => (
          <li key={pick.date} className="body-md">
            {pick.place.name}
          </li>
        ))}
      </ul>
    </main>
  );
}

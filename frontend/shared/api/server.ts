import { apiFetch } from "./client";

// 서버 컴포넌트 전용. 태그 캐시에 들어가고 1시간마다도 다시 받는다(API.md 6절).
// 사용자 쿠키는 붙이지 않는다: 공개 데이터만 받고, 사용자별 값은 클라이언트 섬이 /api/me/state로 따로 받는다.
export function serverFetch<T>(path: string, { tags }: { tags: string[] }): Promise<T> {
  return apiFetch<T>(path, { next: { tags, revalidate: 3600 } });
}

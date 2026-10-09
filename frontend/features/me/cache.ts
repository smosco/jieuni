import type { QueryClient } from "@tanstack/react-query";

// 로그아웃·탈퇴·세션 만료 때 이전 사용자의 값(저장·팔로우 상태, 내 목록)이 화면에 남지 않게 지운다.
export function forgetUser(queryClient: QueryClient): void {
  queryClient.setQueryData(["me"], null);
  queryClient.removeQueries({ predicate: (query) => query.queryKey[0] === "me" && query.queryKey.length > 1 });
}

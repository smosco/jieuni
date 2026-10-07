import { useInfiniteQuery } from "@tanstack/react-query";
import type { CursorPage, PlaceKind } from "@/shared/api/types";
import { fetchMakersPage, fetchPlacesPage } from "./api";

// 첫 페이지는 서버가 그린 값을 그대로 쓴다. 다시 받으면 같은 값을 두 번 받는 셈이라 staleTime을 끝없이 둔다.
function firstPageOptions<T>(initial: CursorPage<T>) {
  return {
    initialData: { pages: [initial], pageParams: [null] },
    initialPageParam: null as string | null,
    getNextPageParam: (last: CursorPage<T>) => last.nextCursor,
    staleTime: Infinity,
  };
}

export function usePlacesPages(kinds: PlaceKind[], initial: Awaited<ReturnType<typeof fetchPlacesPage>>) {
  return useInfiniteQuery({
    queryKey: ["browse", "places", kinds],
    queryFn: ({ pageParam }) => fetchPlacesPage(kinds, pageParam),
    ...firstPageOptions(initial),
  });
}

export function useMakersPages(initial: Awaited<ReturnType<typeof fetchMakersPage>>) {
  return useInfiniteQuery({
    queryKey: ["browse", "makers"],
    queryFn: ({ pageParam }) => fetchMakersPage(pageParam),
    ...firstPageOptions(initial),
  });
}

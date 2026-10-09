"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { useMe } from "@/features/me/queries";
import { fetchAdminPlacesPage } from "./api";
import type { AdminPlaceStatus } from "./types";

// 관리자 셸 안에서만 쓰지만, 역할이 확인되기 전에 403을 부르지 않게 한 번 더 막는다.
export function useAdminPlacesPages(status: AdminPlaceStatus | null, q: string) {
  const { data: me } = useMe();
  return useInfiniteQuery({
    queryKey: ["admin", "places", status, q],
    queryFn: ({ pageParam }) => fetchAdminPlacesPage(status, q, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    enabled: me?.role === "admin",
  });
}
